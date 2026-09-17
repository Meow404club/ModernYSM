#!/bin/bash
# RC 探针差分 harness（fix-rc-probe-diff）。
#
# 目的：站/蹲×2/趴爬四姿势下，逐帧对比 B1 预测表面点 vs RC 探针实际捕获顶点
#（RealCameraApiBinder.runProbeDiff 的 [compat][rc-probe-diff] 日志行），产出逐姿势差分表。
#
# 进程卫生（沿 tour.sh 硬纪律）：禁 pkill/pgrep/killall/--stop/遍历 /proc；
# gradle 一律 --no-daemon + setsid（PID=整树 PGID）；cleanup 阶梯=优雅→TERM 整组→KILL 整组。
# 端口隔离：server 走 server.properties server-port，client 走 gameDir/harness.port 文件
#（GuiTourDriver resolvePort 文件通道；MDG runClient 不透传 launcher -D）。默认 25575。
# 姿势诱导：客户端 KeyMapping.setDown（GuiTourDriver pose 命令）；
# 趴爬需 1 格高隧道——经 server 控制台 FIFO 注入 execute at @p run fill 预建（四向）。
set -u

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT" || exit 1

VERSION="${VERSION:-1.20.1-forge}"
PORT="${PORT:-25575}"
DISPLAY_NUM="${DISPLAY_NUM:-96}"
CLIENT_DIR="$ROOT/versions/$VERSION/run/client"
SERVER_DIR="$ROOT/versions/$VERSION/run/server"
RUN_DIR="$ROOT/harness/run/rc-probe-diff"
OUT="$ROOT/harness/out/rc-probe-diff"
TABLE="$OUT/rc-probe-diff.table.txt"

mkdir -p "$SERVER_DIR" "$CLIENT_DIR/config" "$RUN_DIR" "$OUT"
: > "$TABLE"

HOST=localhost
XVFB_PID=""
FIFO="$RUN_DIR/server-stdin.fifo"
PIDFILE="$RUN_DIR/tour.pids"
: > "$PIDFILE"

cleanup() {
  echo "[probe-diff] cleanup: graceful first"
  echo "quit" > "$CLIENT_DIR/cmd.txt" 2>/dev/null
  timeout 3 sh -c 'echo stop > "$1"' fifo "$FIFO" 2>/dev/null
  sleep 5
  if [ -s "$PIDFILE" ]; then
    while read -r p; do [ -n "$p" ] && kill -TERM -- "-$p" 2>/dev/null; done < "$PIDFILE"
    sleep 4
    while read -r p; do [ -n "$p" ] && kill -KILL -- "-$p" 2>/dev/null; done < "$PIDFILE"
  fi
  rm -f "$FIFO" "$CLIENT_DIR/harness.armed" "$CLIENT_DIR/cmd.txt" "$CLIENT_DIR/harness.ready" "$PIDFILE"
  echo "[probe-diff] cleanup done"
}
trap cleanup EXIT

fail() { echo "[probe-diff] FAIL: $*" >&2; exit 1; }

# ---- 0. run 目录供给（幂等；模型/RC 配置沿主仓 run 目录符号链接共享） ----
# 世界存档必须每轮清空：残留的前轮 fill 改形（隧道顶盖）会让玩家刷在顶下=强制
# CROUCHING/shift=false（round4 实证），且地形漂移污染差分基准。
MAIN_RUN="/home/brokestar/workspace/ModernYSM/versions/$VERSION/run/client"
rm -rf "$SERVER_DIR/world" "$SERVER_DIR/world_nether" "$SERVER_DIR/world_the_end"
mkdir -p "$CLIENT_DIR/config/yes_steve_model" "$CLIENT_DIR/mods"
[ -d "$MAIN_RUN/config/yes_steve_model" ] || fail "main run model dir missing: $MAIN_RUN"
if [ ! -e "$CLIENT_DIR/config/yes_steve_model" ]; then
  ln -s "$MAIN_RUN/config/yes_steve_model" "$CLIENT_DIR/config/yes_steve_model"
fi
for f in realcamera.json yes_steve_model-client.toml firstperson.json; do
  [ -f "$MAIN_RUN/config/$f" ] && [ ! -f "$CLIENT_DIR/config/$f" ] && cp "$MAIN_RUN/config/$f" "$CLIENT_DIR/config/$f"
done
mkdir -p "$CLIENT_DIR/mods"
[ -f "$MAIN_RUN/mods/realcamera-dev.jar" ] && [ ! -f "$CLIENT_DIR/mods/realcamera-dev.jar" ] && cp "$MAIN_RUN/mods/realcamera-dev.jar" "$CLIENT_DIR/mods/"
printf '%s\n' "$PORT" > "$CLIENT_DIR/harness.port"
echo "[probe-diff] run dir provisioned (port=$PORT)"

# ---- 0.5 兼容渲染器门（探针捕获需 CPU 路径顶点进 MultiVertexCatcher；GPU/SIMD 直写不可捕） ----
TOML="$CLIENT_DIR/config/yes_steve_model-client.toml"
if [ -f "$TOML" ] && grep -q '^	UseCompatibilityRenderer' "$TOML"; then
  sed -i 's/^	UseCompatibilityRenderer = .*/	UseCompatibilityRenderer = true/' "$TOML"
fi

# ---- 1. Xvfb ----
setsid Xvfb ":$DISPLAY_NUM" -screen 0 640x480x24 -nolisten tcp >/dev/null 2>&1 &
XVFB_PID=$!
echo "$XVFB_PID" >> "$PIDFILE"
sleep 2
export DISPLAY=":$DISPLAY_NUM"

# ---- 2. server ----
printf 'eula=true\n' > "$SERVER_DIR/eula.txt"
cat > "$SERVER_DIR/server.properties" <<PROPS
server-port=$PORT
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
GRADLE_SERVER=":$VERSION:runServer"
GRADLE_CLIENT=":$VERSION:runClient"
rm -f "$FIFO" && mkfifo "$FIFO"
: > "$OUT/server.log"
setsid sh gradlew $GRADLE_SERVER --no-daemon --no-configuration-cache < "$FIFO" > "$OUT/server.log" 2>&1 &
SERVER_PID=$!
echo "$SERVER_PID" >> "$PIDFILE"
exec 3>"$FIFO"
echo "[probe-diff] waiting for server Done (log: $OUT/server.log)"
DONE=no
for i in $(seq 1 300); do
  if grep -q "Done (" "$OUT/server.log" 2>/dev/null; then DONE=yes; break; fi
  if grep -qE "BUILD FAILED|FATAL" "$OUT/server.log" 2>/dev/null; then break; fi
  sleep 1
done
[ "$DONE" = yes ] || fail "server did not reach Done"
echo "difficulty peaceful" >&3
echo "gamerule doDaylightCycle false" >&3
echo "time set day" >&3
echo "gamerule doWeatherCycle false" >&3
echo "weather clear" >&3
echo "op Dev" >&3   # crawl2 隧道由客户端 sendCommand 自建（服务端控制台 fill 曾疑似未落）
sleep 2

# ---- 3. client（armed + harness.port 文件） ----
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

await_ready() { # $1=期望内容 $2=超时秒
  for i in $(seq 1 "$2"); do
    [ -f "$CLIENT_DIR/harness.ready" ] && grep -q "^$1\$" "$CLIENT_DIR/harness.ready" 2>/dev/null && return 0
    grep -qE "Fatal|SIGSEGV|malloc\(\)" "$OUT/client.log" 2>/dev/null && fail "client native crash"
    sleep 1
  done
  return 1
}

echo "[probe-diff] waiting for client title/world"
await_ready "title" 300 || fail "client never reached title"
await_ready "world" 300 || fail "client never joined world"
rm -f "$CLIENT_DIR/harness.ready"
# 模型懒加载+首绑稳定期：world 标记后模型仍未激活（round9 stand 段仅 2 行=加载未完）
sleep 20

# ---- 4. 姿势格（先站 8s 稳态，再逐姿势切换；趴爬先经控制台建四向 1 格隧道） ----
pose_seg() { # $1=pose $2=秒
  echo "[probe-diff] pose $1 for ${2}s"
  echo "pose $1" > "$CLIENT_DIR/cmd.txt"
  sleep 3   # 姿态建立+过渡收尾（分析侧丢弃每段前 ~2s 瞬态）
  echo "SEG $1 $(date +%s)" >> "$TABLE"
  sleep "$2"
}

pose_seg stand 8
pose_seg crouch 8
pose_seg crouchmove 12
# 趴爬 v2：客户端 op 后 sendCommand 自建四向 1 格隧道（tunnel-check 方块日志自证），
# 持续疾跑前进 → SWIMMING → climbing 趴姿动画
pose_seg crawl2 12
pose_seg crawl 12

# ---- 5. 差分表提取（按日志 pose/shift/位移分组；SEG 时间轴备用） ----
echo "[probe-diff] extracting diff rows"
grep '\[compat\]\[rc-probe-diff\]' "$OUT/client.log" > "$OUT/rc-probe-diff.rows.log" || true
ROWS=$(wc -l < "$OUT/rc-probe-diff.rows.log")
{
  echo "# rc-probe-diff rows=$ROWS seg-timeline:"
  grep '^SEG' "$TABLE" || true
  echo "# 分组统计（pose × shift × probeAvail）："
  awk '{
    pose=""; shift=""; avail="";
    if (match($0, /pose=[A-Z]+/)) pose=substr($0, RSTART+5, RLENGTH-5);
    if (match($0, /shift=(true|false)/)) shift=substr($0, RSTART+6, RLENGTH-6);
    if (match($0, /probeAvail=(true|false)/)) avail=substr($0, RSTART+11, RLENGTH-11);
    print pose"/shift="shift"/avail="avail;
  }' "$OUT/rc-probe-diff.rows.log" | sort | uniq -c
} >> "$TABLE"
echo "[probe-diff] table summary:"
tail -20 "$TABLE"

# ---- 6. 收尾（cleanup 阶梯见 trap） ----
echo "quit" > "$CLIENT_DIR/cmd.txt"
sleep 6
echo "[probe-diff] done: $OUT/rc-probe-diff.rows.log"
