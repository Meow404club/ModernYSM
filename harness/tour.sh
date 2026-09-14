#!/bin/bash
# 跨版本 GUI 走查 orchestrator。
#
# 用法:  sh harness/tour.sh <version-line> [screen ...]
#   version-line: stonecutter 版本项目名（1.16.5-forge / 1.20.1-forge；M3 平铺追加行
#                 只需扩展下方 case 的路径推导表）
#   screen:       可选，默认全清单（TOUR_SCREENS）
#
# 进程卫生（硬纪律，开头自检强制）：
#   - 禁止 pkill/pgrep/killall/--stop/遍历 /proc——这些模式会命中并行会话。
#   - gradle 调用一律 --no-daemon + setsid：游戏 JVM 是启动 JVM 的子进程且同进程组，
#     记录的 PID 即整棵树的 PGID（daemon 模式下任务由共享 daemon fork，树被切断——
#     这正是曾被禁的"按名字扫描兜底"存在的唯一原因，修根因后无需任何扫描）。
#   - cleanup 阶梯：优雅先（cmd.txt quit → FIFO stop）→ 超时 kill -TERM -- -PGID
#     （负号=整组，含 fork 出的游戏 JVM）→ 再超时 kill -KILL -- -PGID。
#
# 和平启动：server "Done" 后经 stdin(FIFO) 注入控制台命令（difficulty peaceful /
# gamerule doDaylightCycle false / time set day / gamerule doWeatherCycle false /
# weather clear，全版本一致）。生成期读取的设置（flat/online-mode/difficulty=0/
# gamemode=creative）由 server.properties 预写双保险。
#
# 客户端零 GL 注入：截图全部外部 ffmpeg x11grab（xvfb+llvmpipe 下进程内 glReadPixels
# 会触发 native 堆损坏假崩溃，M2.5 12 轮误报实证）；屏间导航经 cmd.txt/harness.ready
# 文件握手，driver 仅在 <clientGameDir>/harness.armed 存在时激活。
# 注意：不要 export LC_ALL=C——C locale 下 JVM sun.jnu.encoding=ANSI_X3.4-1968，
# processResources 扫内置模型的非 ASCII 文件名（中文 avatar 文件）直接炸。
set -u

# ---- 自检：禁止进程名字扫描模式回潮（本行与被过滤的注释行除外）----
# 禁词串分段书写，防自检匹配自检行本身
if grep -nE 'pkil''l|pgre''p|killal''l|--st''op|/pro''c/' "$0" | grep -v '^[0-9]*:[[:space:]]*#' >/dev/null 2>&1; then
  echo "FATAL: process-scan pattern detected in tour.sh (see lines above)" >&2
  exit 1
fi

VERSION="${1:?usage: sh harness/tour.sh <version-line> [screen ...]}"
shift || true
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT" || exit 1

DISPLAY_NUM="${DISPLAY_NUM:-91}"
OUT="$ROOT/harness/out/$VERSION"
RUN_DIR="$ROOT/harness/run/$VERSION"
SCREENSHOT_DIR="${SCREENSHOT_DIR:-$OUT}"

# ---- 版本线推导表（M3 平铺：新增版本线只改这里；gameDir 约定两线统一为
#      run/server 与 run/client，1.20.1 线已在 build.forge.gradle.kts
#      runs.gameDirectory 显式对齐）----
case "$VERSION" in
  1.16.5-forge|1.20.1-forge)
    SERVER_DIR="$ROOT/versions/$VERSION/run/server"
    CLIENT_DIR="$ROOT/versions/$VERSION/run/client"
    ;;
  # M3 平铺批一/批二 forge 十四线（<1.17 unimined=build.unimined.gradle.kts、1.17~1.20
  # legacyforge=build.forge.gradle.kts；runs.gameDirectory 同约定 run/server、run/client）
  1.16.1-forge|1.16.2-forge|1.16.3-forge|1.16.4-forge|\
1.17.1-forge|1.18-forge|1.18.1-forge|1.18.2-forge|1.19-forge|1.19.1-forge|1.19.2-forge|1.19.3-forge|1.19.4-forge|1.20-forge)
    SERVER_DIR="$ROOT/versions/$VERSION/run/server"
    CLIENT_DIR="$ROOT/versions/$VERSION/run/client"
    ;;
  1.20.4-neoforge|1.20.6-neoforge|1.21.1-neoforge|21.3-neoforge|21.4-neoforge|21.5-neoforge|21.8-neoforge|21.10-neoforge|21.11-neoforge|26.1.2-neoforge|26.2-neoforge)
    SERVER_DIR="$ROOT/versions/$VERSION/run/server"
    CLIENT_DIR="$ROOT/versions/$VERSION/run/client"
    ;;
  # M3 批二 c-2：neoforge 补线五线（build.moddev.gradle.kts 同构）
  1.21-neoforge|21.2-neoforge|21.6-neoforge|21.7-neoforge|21.9-neoforge)
    SERVER_DIR="$ROOT/versions/$VERSION/run/server"
    CLIENT_DIR="$ROOT/versions/$VERSION/run/client"
    ;;
  *)
    echo "[tour] unknown version line: $VERSION" >&2
    exit 1
    ;;
esac
GRADLE_SERVER=":$VERSION:runServer"
GRADLE_CLIENT=":$VERSION:runClient"

TOUR_SCREENS="${*:-disclaimer playermodel texture modern_texture info modern_info upload download folder extrarender extraconfig settings roulette modern_roulette}"

HOST=localhost
PORT=25565
XVFB_PID=""
FIFO="$RUN_DIR/server-stdin.fifo"
PIDFILE="$RUN_DIR/tour.pids"
# 首跑的版本线（如 1.20.1-forge）run 目录尚不存在，必须先建——PIDFILE 截断
# 与 FIFO 创建都落在这里，晚于本行的 mkdir 已来不及（2026-09-13 首跑实证）
mkdir -p "$RUN_DIR"
: > "$PIDFILE"

cleanup() {
  echo "[tour] cleanup: graceful first"
  echo "quit" > "$CLIENT_DIR/cmd.txt" 2>/dev/null
  # FIFO 写端 open() 要等读端：server 已死（失败路径常态）时此 open 无限期阻塞，
  # 吞掉整个 TERM/KILL 清理阶梯并泄漏本会话进程树（审查 P1 实证）。
  # timeout 兜底保证永不卡：有读端=照常送达；无读端=最多 3s 后 124 返回继续阶梯。
  timeout 3 sh -c 'echo stop > "$1"' fifo "$FIFO" 2>/dev/null
  sleep 5
  # 阶梯 2/3：按记录的 PGID 杀整组（setsid 启动，PID 即 PGID；不含共享 daemon）
  if [ -s "$PIDFILE" ]; then
    while read -r p; do [ -n "$p" ] && kill -TERM -- "-$p" 2>/dev/null; done < "$PIDFILE"
    sleep 4
    while read -r p; do [ -n "$p" ] && kill -KILL -- "-$p" 2>/dev/null; done < "$PIDFILE"
  fi
  rm -f "$FIFO" "$CLIENT_DIR/harness.armed" "$CLIENT_DIR/cmd.txt" "$PIDFILE"
  echo "[tour] cleanup done"
}
trap cleanup EXIT

fail() { echo "[tour] FAIL: $*" >&2; exit 1; }

# ---- 0. server 目录准备（eula + 首跑 properties；幂等）----
mkdir -p "$SERVER_DIR" "$CLIENT_DIR" "$RUN_DIR" "$SCREENSHOT_DIR"
printf 'eula=true\n' > "$SERVER_DIR/eula.txt"
if [ ! -f "$SERVER_DIR/server.properties" ]; then
  cat > "$SERVER_DIR/server.properties" <<'PROPS'
online-mode=false
level-type=minecraft\:flat
difficulty=0
gamemode=creative
force-gamemode=true
spawn-protection=0
view-distance=4
generate-structures=false
allow-nether=false
PROPS
  echo "[tour] wrote fresh server.properties (flat/peaceful/creative/offline)"
fi

# ---- 1. Xvfb（独占 :9x 段；PID 入表）----
setsid Xvfb ":$DISPLAY_NUM" -screen 0 640x480x24 -nolisten tcp >/dev/null 2>&1 &
XVFB_PID=$!
echo "$XVFB_PID" >> "$PIDFILE"
sleep 2
export DISPLAY=":$DISPLAY_NUM"

# ---- 2. server（--no-daemon + setsid：树完整可整组清理；stdin=FIFO 注入控制台）----
rm -f "$FIFO" && mkfifo "$FIFO"
: > "$OUT/server.log"
setsid sh gradlew $GRADLE_SERVER --no-daemon --no-configuration-cache < "$FIFO" > "$OUT/server.log" 2>&1 &
SERVER_PID=$!
echo "$SERVER_PID" >> "$PIDFILE"
exec 3>"$FIFO"   # 保持写端，防 EOF 杀 server
echo "[tour] waiting for server Done (log: $OUT/server.log)"
DONE=no
for i in $(seq 1 240); do
  if grep -q "Done (" "$OUT/server.log" 2>/dev/null; then DONE=yes; break; fi
  if grep -qE "BUILD FAILED|FATAL" "$OUT/server.log" 2>/dev/null; then break; fi
  sleep 1
done
[ "$DONE" = yes ] || fail "server did not reach Done (see $OUT/server.log)"
echo "[tour] server up, injecting peaceful/day-locked/clear via stdin"
echo "difficulty peaceful" >&3
echo "gamerule doDaylightCycle false" >&3
echo "time set day" >&3
echo "gamerule doWeatherCycle false" >&3
echo "weather clear" >&3
sleep 2

# ---- 3. client（armed 标记 + 文件握手；driver 侧零 GL）----
# 1.20+ 首启 AccessibilityOnboardingScreen 挡在 TitleScreen 前（driver 等
# TitleScreen 才 mark title），预写 options 跳过 onboarding（幂等：有则改、无则加）
OPT="$CLIENT_DIR/options.txt"
touch "$OPT"
if grep -q '^onboardAccessibility:' "$OPT" 2>/dev/null; then
  sed -i 's/^onboardAccessibility:.*/onboardAccessibility:false/' "$OPT"
else
  echo 'onboardAccessibility:false' >> "$OPT"
fi
rm -f "$CLIENT_DIR/harness.armed" "$CLIENT_DIR/cmd.txt" "$CLIENT_DIR/harness.ready"
: > "$OUT/client.log"
setsid sh gradlew $GRADLE_CLIENT --no-daemon --no-configuration-cache > "$OUT/client.log" 2>&1 &
CLIENT_PID=$!
echo "$CLIENT_PID" >> "$PIDFILE"
touch "$CLIENT_DIR/harness.armed"

grab() { # $1=输出名
  ffmpeg -y -loglevel error -f x11grab -framerate 2 -i ":$DISPLAY_NUM" -frames:v 1 "$SCREENSHOT_DIR/$1.png" 2>/dev/null \
    && echo "[tour] shot $1" || echo "[tour] SHOT FAILED: $1"
}
step() { # $1=screen $2=输出名
  rm -f "$CLIENT_DIR/harness.ready"
  echo "open $1" > "$CLIENT_DIR/cmd.txt"
  if await_ready "ok $1" 90; then
    sleep 2.5
    grab "$2"
  else
    echo "[tour] screen $1 did not open (timeout/crash)"
    grab "$2-failed"
  fi
}
await_ready() { # $1=期望内容 $2=超时秒
  for i in $(seq 1 "$2"); do
    [ -f "$CLIENT_DIR/harness.ready" ] && grep -q "^$1\$" "$CLIENT_DIR/harness.ready" 2>/dev/null && return 0
    grep -qE "Fatal|SIGSEGV|malloc\(\)" "$OUT/client.log" 2>/dev/null && fail "client native crash (see $OUT/client.log)"
    sleep 1
  done
  return 1
}

echo "[tour] waiting for client title"
await_ready "title" 300 || fail "client never reached title"
sleep 1
grab "000-title"
# 注意：此处不得 rm harness.ready！title→world 是 driver 自主跃迁（不由 cmd.txt 驱动），
# 其时刻与 grab 耗时不可预测：实测 join 落在 beat 间隔内，rm 会把已写入的 "world" 吞掉，
# await world 必然 300s 超时（首轮根因，2026-09-12）。握手文件在 client 启动前已清空，
# title 是首个标记、world 是第二个，直接等 "world" 即可，无需中途再清。
echo "[tour] waiting for client world join"
await_ready "world" 300 || fail "client never joined world"
rm -f "$CLIENT_DIR/harness.ready"

step disclaimer 010-disclaimer
step playermodel 020-playermodel
for s in $TOUR_SCREENS; do
  case "$s" in disclaimer|playermodel) continue;; esac
  step "$s" "$s"
done
step playermodel 020b-playermodel-return

# ---- 4. 收尾（cleanup 阶梯见 trap）----
echo "[tour] tour complete, tearing down"
