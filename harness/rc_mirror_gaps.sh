#!/bin/bash
# RC 绑定情境旋转镜像数值格（rc-closure-mirror-gaps）。
#
# 目的：bindRootFrame 补 climbable/isShaking/Dinnerbone 三情境旋转镜像前后的
# 绑定-渲染一致性数值取证。四段（每段状态由 rc-probe-diff 行内
# st=climb:{}/frozen:{}/flip:{} 布尔自证，分桶零切片歧义）：
#   stand    —— 默认路径（三态皆否），镜像前后必须逐位零差（零差走查基准段）
#   ladder   —— climbable 诱导：脚下方块 setblock ladder[facing=south]（对面 stone 支撑）
#               +tp 锁 yaw=0。镜像后根 yaw=180-180=0 → 脸法线 fwdZ≈-1（镜像缺失 ≈+1）
#   freeze   —— isShaking 诱导：3x3x3 powder_snow 埋脚（peaceful 冻 140t=7s 后 fully frozen），
#               根 yaw 叠加 cos(tickCount*3.25)*PI*0.4F ±1.257° 微抖；镜像缺失时
#               dStruct 高于仪器地板且随 tick 振荡
#   dinnerbone—— GuiTourDriver dinnerbone 命令（armed 下反射改写本地玩家 gameProfile 名；
#               Player.getName 读 profile，setCustomName 对玩家无效）。镜像后 upY≈-1（缺失 +1）
# 一致性断言核心=dStruct（探针真渲染顶点 vs B1 预测）≤仪器地板（fix-rc-probe-diff 实证
# 地板≈1.5px：UV 插值 vs 顶点流面内偏移）、dFwdAngle/dUpAngle≈0。
#
# 进程卫生（沿 rc_probe_diff.sh 硬纪律）：禁 pkill/pgrep/killall/--stop/遍历 /proc；
# gradle 一律 --no-daemon + setsid（PID=整树 PGID）；cleanup 阶梯=优雅→TERM 整组→KILL 整组。
# 端口隔离：默认 25599（错开 rc_probe_diff 25575 等）。
set -u
trap '' PIPE   # 服务端死亡后 FIFO 写不炸 driver（继续走存活检查路径）

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT" || exit 1

VERSION="${VERSION:-1.20.1-forge}"
PORT="${PORT:-25599}"
SUFFIX="${SUFFIX:-run}"
DISPLAY_NUM="${DISPLAY_NUM:-98}"
CLIENT_DIR="$ROOT/versions/$VERSION/run/client"
SERVER_DIR="$ROOT/versions/$VERSION/run/server"
RUN_DIR="$ROOT/harness/run/rc-mirror-gaps"
OUT="$ROOT/harness/out/rc-mirror-gaps-$SUFFIX"
TABLE="$OUT/rc-mirror-gaps.table.txt"

mkdir -p "$SERVER_DIR" "$CLIENT_DIR/config" "$RUN_DIR" "$OUT"
: > "$TABLE"

HOST=localhost
XVFB_PID=""
FIFO="$RUN_DIR/server-stdin.fifo"
PIDFILE="$RUN_DIR/tour.pids"
: > "$PIDFILE"

cleanup() {
  echo "[mirror-gaps] cleanup: graceful first"
  echo "quit" > "$CLIENT_DIR/cmd.txt" 2>/dev/null
  timeout 3 sh -c 'echo stop > "$1"' fifo "$FIFO" 2>/dev/null
  sleep 5
  if [ -s "$PIDFILE" ]; then
    while read -r p; do [ -n "$p" ] && kill -TERM -- "-$p" 2>/dev/null; done < "$PIDFILE"
    sleep 4
    while read -r p; do [ -n "$p" ] && kill -KILL -- "-$p" 2>/dev/null; done < "$PIDFILE"
  fi
  rm -f "$FIFO" "$CLIENT_DIR/harness.armed" "$CLIENT_DIR/cmd.txt" "$CLIENT_DIR/harness.ready" "$PIDFILE"
  echo "[mirror-gaps] cleanup done"
}
trap cleanup EXIT

fail() { echo "[mirror-gaps] FAIL: $*" >&2; exit 1; }

# ---- 端口属主预检（泄漏的上轮服务端会让 bind 失败，fail fast 带归属提示） ----
if ss -tln 2>/dev/null | grep -q ":$PORT "; then
  fail "port $PORT already occupied (leaked prior run? clean its PGID first)"
fi

# ---- 0. run 目录供给（幂等；模型/RC 配置沿主仓 run 目录共享） ----
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
grep -q 'yes_steve_model:textures/' "$CLIENT_DIR/config/realcamera.json" || fail "realcamera.json not prefix variant (L2 noMatchingTarget)"
mkdir -p "$CLIENT_DIR/mods"
[ -f "$MAIN_RUN/mods/realcamera-dev.jar" ] && [ ! -f "$CLIENT_DIR/mods/realcamera-dev.jar" ] && cp "$MAIN_RUN/mods/realcamera-dev.jar" "$CLIENT_DIR/mods/"
# server 侧模型库供给：worktree server run 全新无 custom 模型 → 服务端没有 Trissy →
# 客户端选中被拒落默认模型 → UV 布局不符 → tryUvBind 静默回退骨轴（rc-probe-diff 行零产出，
# 2026-09-19 首跑实证）。软链主仓 server 的 custom 模型库。
MAIN_SERVER_DIR="/home/brokestar/workspace/ModernYSM/versions/$VERSION/run/server"
if [ -d "$MAIN_SERVER_DIR/config/yes_steve_model/custom" ] && [ ! -L "$SERVER_DIR/config/yes_steve_model/custom" ]; then
  rm -rf "$SERVER_DIR/config/yes_steve_model/custom"
  ln -s "$MAIN_SERVER_DIR/config/yes_steve_model/custom" "$SERVER_DIR/config/yes_steve_model/custom"
fi
printf '%s\n' "$PORT" > "$CLIENT_DIR/harness.port"
echo "[mirror-gaps] run dir provisioned (port=$PORT suffix=$SUFFIX)"

# ---- 0.5 兼容渲染器门（探针捕获需 CPU 路径顶点进 MultiVertexCatcher） ----
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
echo "[mirror-gaps] waiting for server Done (log: $OUT/server.log)"
DONE=no
for i in $(seq 1 600); do
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
echo "op Dev" >&3
sleep 2

# ---- 3. client（armed + harness.port 文件） ----
OPT="$CLIENT_DIR/options.txt"
touch "$OPT"
grep -q '^version:' "$OPT" 2>/dev/null || echo 'version:3465' >> "$OPT"
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

echo "[mirror-gaps] waiting for client title/world"
await_ready "title" 300 || fail "client never reached title"
await_ready "world" 300 || fail "client never joined world"
rm -f "$CLIENT_DIR/harness.ready"
# world 每轮清空 → playerdata 的模型指派被抹 → Dev 落服务端默认模型 → Trissy UV(54.5,69.5)
# 打不中 → tryUvBind 静默回退骨轴、rc-probe-diff 零行（08:39/08:41 两轮实证）。
# join 后 console 指派 Trissy（S2C 推送，下方 sleep 20 内完成下载/解析）。
execute_console 'ysm model set Dev "custom|Trissy_特莉丝" textures/NekoWhite.png true'
sleep 20   # 模型懒加载+首绑稳定期（rc_probe_diff 同款）

seg_mark() { echo "SEG $1 $(date +%s)" >> "$TABLE"; }
logt() { echo "[mirror-gaps $(date +%H:%M:%S)] $*"; }
server_alive() {
  if grep -q "Stopping the server" "$OUT/server.log" 2>/dev/null; then
    fail "server died unexpectedly (check $OUT/server.log tail before this line)"
  fi
}
execute_console() { echo "$1" >&3; sleep 1; }

# ---- 4.1 stand 段（默认路径基准） ----
logt "segment stand"
seg_mark stand
sleep 10

# ---- 4.2 ladder 段（climbable：脚 cell 换 ladder[facing=south]，tp 锁 yaw=0） ----
server_alive
logt "segment ladder"
execute_console "execute at @p run setblock ~ ~ ~-1 minecraft:smooth_stone"
execute_console "execute at @p run setblock ~ ~ ~ minecraft:ladder[facing=south]"
execute_console "execute at @p run tp @p ~ ~ ~ 0 0"
sleep 3   # onClimbable/getLastClimbablePos 建立瞬态
seg_mark ladder
sleep 10

# ---- 4.3 freeze 段（isShaking：powder_snow 埋脚，peaceful 冻 140t 后 fully frozen） ----
server_alive
logt "segment freeze"
execute_console "execute at @p run fill ~1 ~ ~1 ~-1 ~-2 ~-1 minecraft:powder_snow"
sleep 10  # 冻结累计 140t=7s
seg_mark freeze
sleep 10
execute_console "execute at @p run fill ~1 ~ ~1 ~-1 ~-1 ~-1 minecraft:air"
execute_console "execute at @p run fill ~1 ~-2 ~1 ~-1 ~-2 ~-1 minecraft:smooth_stone"
sleep 10  # 解冻衰减（140t）后才进 dinnerbone 段，两态不混

# ---- 4.4 dinnerbone 段（倒置：armed 反射改写 gameProfile 名） ----
server_alive
logt "segment dinnerbone"
echo "dinnerbone" > "$CLIENT_DIR/cmd.txt"
sleep 3
grep -q "ok dinnerbone" "$CLIENT_DIR/harness.ready" 2>/dev/null \
  || logt "WARN dinnerbone cmd not ok: $(cat "$CLIENT_DIR/harness.ready" 2>/dev/null)"
seg_mark dinnerbone
sleep 10

# ---- 5. 提取（行内 st= 布尔分桶 + 逐桶统计） ----
echo "[mirror-gaps] extracting"
grep '\[compat\]\[rc-probe-diff\]' "$OUT/client.log" > "$OUT/rc-mirror-gaps.rows.log" || true
ROWS=$(wc -l < "$OUT/rc-mirror-gaps.rows.log")
python3 - "$OUT/rc-mirror-gaps.rows.log" >> "$TABLE" <<'PYEOF'
import re, sys
rows = []
pat = re.compile(
    r'dStruct=probe-b1After.*?\|([0-9.]+)\|blk'
    r'.*?dFwdAngle=(-?[0-9.]+|NaN) deg'
    r'.*?dUpAngle=(-?[0-9.]+|NaN) deg'
    r'.*?st=climb:(true|false)/frozen:(true|false)/flip:(true|false)'
    r'.*?fwdZ=(-?[0-9.]+|NaN) upY=(-?[0-9.]+|NaN)')
total = 0
for line in open(sys.argv[1], encoding='utf-8', errors='replace'):
    total += 1
    m = pat.search(line)
    if not m:
        continue
    climb, frozen, flip = m.group(4) == 'true', m.group(5) == 'true', m.group(6) == 'true'
    fnum = lambda x: float('nan') if x == 'NaN' else float(x)
    rows.append((climb, frozen, flip, float(m.group(1)), fnum(m.group(2)), fnum(m.group(3)), fnum(m.group(7)), fnum(m.group(8))))
def bucket(name, pred):
    sel = [r for r in rows if pred(r)]
    if not sel:
        print("# %s n=0" % name); return
    fin = lambda a: sorted(x for x in a if x == x)
    ds = fin(r[3] for r in sel); df = fin(r[4] for r in sel); du = fin(r[5] for r in sel)
    fz = fin(r[6] for r in sel); uy = fin(r[7] for r in sel)
    if not ds or not fz or not uy:
        print("# %s n=%d (no finite rows)" % (name, len(sel))); return
    med = lambda a: a[len(a)//2]
    dfm = '%.2f/%.2f' % (med(df), df[-1]) if df else 'n/a'
    dum = '%.2f/%.2f' % (med(du), du[-1]) if du else 'n/a'
    print("# %s n=%d dStruct p50=%.4f max=%.4f (blk) | dFwd p50/max=%s | dUp p50/max=%s (deg) | fwdZ med=%.3f | upY med=%.3f"
          % (name, len(sel), med(ds), ds[-1], dfm, dum, med(fz), med(uy)))
bucket("stand(default)", lambda r: not r[0] and not r[1] and not r[2])
bucket("ladder(climbable)", lambda r: r[0])
bucket("freeze(shaking)", lambda r: r[1])
bucket("dinnerbone(flip)", lambda r: r[2] and not r[1])
print("# total rows=%d (unmatched=%d)" % (len(rows), total - len(rows)))
PYEOF
echo "[mirror-gaps] table summary:"
grep '^#' "$TABLE" | tail -8

# ---- 6. 健康门（0 Binding failed / 0 EMPTY） ----
BF=$(grep -c "Binding failed" "$OUT/client.log" || true)
EP=$(grep -c "bone bind EMPTY" "$OUT/client.log" || true)
{
  echo "# health: Binding failed=$BF EMPTY=$EP"
  grep -m3 "bind function registered" "$OUT/client.log" || true
} >> "$TABLE"
echo "[mirror-gaps] health: Binding failed=$BF EMPTY=$EP"

# ---- 7. 收尾 ----
echo "quit" > "$CLIENT_DIR/cmd.txt"
sleep 6
echo "[mirror-gaps] done: $OUT/rc-mirror-gaps.rows.log"
