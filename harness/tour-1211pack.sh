#!/bin/bash
# 1211 带包 tour 格（harness-cell-1211pack-2612base）：iris 1.8.12 + sodium 0.6.13 + BSL 光影包
# 端到端验收 neoforge-detect 卡遗留观察——packInUse=true 时 NativeModelRenderer 走
# CPU vanilla 管线（cpuBufferFallback），GpuRenderPath 被跳过，16 屏走查全过。
#
# 用法: sh harness/tour-1211pack.sh [screen ...]   （透传给 tour.sh，缺省全 16 屏）
#   环境变量同 tour.sh（DISPLAY_NUM/HARNESS_PORT）；产物在 harness/out/1.21.1-neoforge/。
#
# 构成（全部 Modrinth 实拉+sha1 校验，缓存在 harness/out/dlcache/——gitignored）：
#   iris-neoforge-1.8.12+mc1.21.1.jar   /v2/project/iris/version ga（2026-09-17 实拉）
#   sodium-neoforge-0.6.13+mc1.21.1.jar /v2/project/sodium/version ga（iris 硬依赖）
#   BSL_v10.1.5.zip                     /v2/project/bsl-shaders（1.1MB 小体积包，
#                                       能触发 IrisApi.isShaderPackInUse=true 即可）
# 装配面（跑前置入、跑后卸回，既有无包 tour 不受污染）：
#   run/client/mods/{iris,sodium}.jar   ← MDG dev runClient 扫 gameDir mods 文件夹
#   run/client/shaderpacks/BSL_v10.1.5.zip + config/iris.properties（shaderPack= 预启用，
#                                       IrisConfig.load 实证键名：shaderPack/enableShaders）
#   run/client/ysm-debug-hide.flag      ← hide-matrix 路径自证打点（path=cpu 行，
#                                       NativeModelRenderer shouldLogHideMatrix 文件通道）
# 验收判据（脚本自动 grep，全部日志数值，禁看图判好）：
#   ① client.log 有 "[YSM] Iris/Oculus shader compat active" 且同行 packInUse=true
#   ② path=cpu 存在且模型非空（totalQuads/visibleQuads>0）= CPU 缓冲管线带包持续产出；
#      最后一个预览屏心跳之后的纯世界相位零 path=gpu/path=simd（GpuRenderPath/SIMD
#      直写被 cpuBufferFallback 砍除；预览屏内 GPU 属 isPreview 设计保留）；全程零 path=simd
#   ③ tour exit 0 + 16 屏 png 零 *-failed + worldshot≥1（HARNESS_WORLDSHOT=1）+零崩溃标记
set -u

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT" || exit 1
LINE="1.21.1-neoforge"
CLIENT_DIR="$ROOT/versions/$LINE/run/client"
CACHE="$ROOT/harness/out/dlcache"
OUT="$ROOT/harness/out/$LINE"

IRIS_NAME="iris-neoforge-1.8.12+mc1.21.1.jar"
IRIS_URL="https://cdn.modrinth.com/data/YL57xq9U/versions/t3ruzodq/iris-neoforge-1.8.12%2Bmc1.21.1.jar"
IRIS_SHA1="a3e6355915c7d3b2bc392724795113e51d289378"
SODIUM_NAME="sodium-neoforge-0.6.13+mc1.21.1.jar"
SODIUM_URL="https://cdn.modrinth.com/data/AANobbMI/versions/Pb3OXVqC/sodium-neoforge-0.6.13%2Bmc1.21.1.jar"
SODIUM_SHA1="38af70fa4dc4b2aaac636e92fdba3bedd5a025e1"
PACK_NAME="BSL_v10.1.5.zip"
PACK_URL="https://cdn.modrinth.com/data/Q1vvjJYV/versions/yFTiE1Nc/BSL_v10.1.5.zip"
PACK_SHA1="49bed4894881b22fa680b97504f0c3265bafbcbc"

fail() { echo "[1211pack] FAIL: $*" >&2; exit 1; }

fetch() { # $1=url $2=出文件名 $3=期望sha1
  local f="$CACHE/$2"
  if [ -f "$f" ] && echo "$3  $f" | sha1sum -c --status 2>/dev/null; then
    echo "[1211pack] cached ok: $2"
    return 0
  fi
  echo "[1211pack] downloading $2"
  mkdir -p "$CACHE"
  curl -fsSL --retry 3 -o "$f" "$1" || fail "download failed: $1"
  echo "$3  $f" | sha1sum -c --quiet || fail "sha1 mismatch: $2"
}

# ---- 0. 实拉 + 校验 ----
fetch "$IRIS_URL"   "$IRIS_NAME"   "$IRIS_SHA1"
fetch "$SODIUM_URL" "$SODIUM_NAME" "$SODIUM_SHA1"
fetch "$PACK_URL"   "$PACK_NAME"   "$PACK_SHA1"

# ---- 1. 装配（幂等）----
mkdir -p "$CLIENT_DIR/mods" "$CLIENT_DIR/shaderpacks" "$CLIENT_DIR/config"
cp -f "$CACHE/$IRIS_NAME" "$CACHE/$SODIUM_NAME" "$CLIENT_DIR/mods/"
cp -f "$CACHE/$PACK_NAME" "$CLIENT_DIR/shaderpacks/"
IRIS_CFG="$CLIENT_DIR/config/iris.properties"
touch "$IRIS_CFG"
if grep -q '^shaderPack=' "$IRIS_CFG" 2>/dev/null; then
  sed -i "s|^shaderPack=.*|shaderPack=$PACK_NAME|" "$IRIS_CFG"
else
  printf 'shaderPack=%s\n' "$PACK_NAME" >> "$IRIS_CFG"
fi
if grep -q '^enableShaders=' "$IRIS_CFG" 2>/dev/null; then
  sed -i 's/^enableShaders=.*/enableShaders=true/' "$IRIS_CFG"
else
  printf 'enableShaders=true\n' >> "$IRIS_CFG"
fi
: > "$CLIENT_DIR/ysm-debug-hide.flag"
echo "[1211pack] installed: mods/{iris,sodium} shaderpacks/$PACK_NAME iris.properties hide.flag"

# ---- 2. tour（worldshot 开：世界内第三人称 renderMesh 路径证据）----
HARNESS_WORLDSHOT=1
export HARNESS_WORLDSHOT
sh harness/tour.sh "$LINE" "$@"
TOUR_EXIT=$?

# ---- 3. 卸载（防污染后续无包格）----
mkdir -p "$CLIENT_DIR/mods-disabled-1211pack"
mv -f "$CLIENT_DIR/mods/$IRIS_NAME" "$CLIENT_DIR/mods/$SODIUM_NAME" "$CLIENT_DIR/mods-disabled-1211pack/" 2>/dev/null
rm -f "$CLIENT_DIR/ysm-debug-hide.flag"
echo "[1211pack] uninstalled (jars -> mods-disabled-1211pack, hide.flag removed)"

# ---- 4. 数值验收 ----
# 世界内分派证据（walk 流程内本地玩家不绑 YSM 头像=世界内零 renderMesh，1201pack 首跑
# 实证）→ 取可辩护不变量：最后一个预览屏心跳（screen=com.elfmcys…Screen）之后=纯世界
# 相位，该相位 path=gpu/path=simd 必须零行（带包下 GpuRenderPath/SIMD 直写均被
# cpuBufferFallback 砍除，NativeModelRenderer:152/163）；预览屏内 path=gpu 属
# isPreview 设计保留（:152「原语义保留」）不算违约。path=cpu + totalQuads>0+
# visibleQuads>0 = CPU 缓冲管线在 Iris 帧内持续产出模型几何（43 行/6min 实证）。
CLOG="$OUT/client.log"
echo "=== acceptance checks ==="
A1=0; grep -q "\[YSM\] Iris/Oculus shader compat active" "$CLOG" && A1=1
A2=0; grep -q "Iris/Oculus shader compat active.*packInUse=true" "$CLOG" && A2=1
CPU_N=$(grep -c "\[ysm-hide-matrix\] path=cpu" "$CLOG" 2>/dev/null || true)
QUAD_N=$(grep "\[ysm-hide-matrix\] path=cpu" "$CLOG" | grep -c "totalQuads=[1-9]" || true)
VIS_N=$(grep "\[ysm-hide-matrix\] path=cpu" "$CLOG" | grep -c "visibleQuads=[1-9]" || true)
SIMD_N=$(grep -c "\[ysm-hide-matrix\] path=simd" "$CLOG" 2>/dev/null || true)
GPU_N=$(grep -c "\[ysm-hide-matrix\] path=gpu" "$CLOG" 2>/dev/null || true)
LAST_PREV=$(grep -n "beat joined=true.*screen=com\.elfmcys" "$CLOG" | tail -1 | cut -d: -f1)
# 边界帧宽限 3 行：最后一次预览心跳后仍可能落 1 帧预览 GPU 渲染（二轮实证 gpu 行=LAST_PREV+1）
WORLD_VIOL_N=$(awk -v n="$(( ${LAST_PREV:-0} + 3 ))" 'NR>n && /path=(gpu|simd)/ {c++} END {print c+0}' "$CLOG")
CRASH_N=$(grep -cE "Fatal|SIGSEGV|malloc\(\)" "$CLOG" 2>/dev/null || true)
PNG_N=$(find "$OUT" -maxdepth 1 -name "*.png" ! -name "worldshot*" ! -name "*failed*" | wc -l)
FAIL_N=$(find "$OUT" -maxdepth 1 -name "*-failed.png" | wc -l)
WORLD_N=$(find "$OUT" -maxdepth 1 -name "worldshot*.png" ! -name "*failed*" | wc -l)

echo "check1 compat-active-line       = $A1 (expect 1)"
echo "check2 packInUse=true           = $A2 (expect 1)"
echo "check2 path=cpu lines           = $CPU_N (expect >0) non-empty-model=$QUAD_N visible-model=$VIS_N (expect >0)"
echo "check2 world-phase gpu/simd rows= $WORLD_VIOL_N (expect 0, after last preview heartbeat L$LAST_PREV)"
echo "check2 whole-run simd/gpu       = $SIMD_N (expect 0) / $GPU_N (preview-only, >=0)"
echo "check3 tour exit                = $TOUR_EXIT (expect 0)"
echo "check3 screens png              = $PNG_N (expect 16) failed=$FAIL_N (expect 0) worldshot=$WORLD_N (expect >=1)"
echo "check3 crash markers            = $CRASH_N (expect 0)"

if [ "$A1" = 1 ] && [ "$A2" = 1 ] && [ "$CPU_N" -gt 0 ] && [ "$QUAD_N" -gt 0 ] && [ "$VIS_N" -gt 0 ] \
   && [ "$WORLD_VIOL_N" = 0 ] && [ "$SIMD_N" = 0 ] && [ "$TOUR_EXIT" = 0 ] && [ "$CRASH_N" = 0 ] \
   && [ "$PNG_N" -ge 16 ] && [ "$FAIL_N" = 0 ] && [ "$WORLD_N" -ge 1 ]; then
  echo "=== 1211pack cell: PASS ==="
  grep "Iris/Oculus shader compat active" "$CLOG" | head -1
  grep "\[ysm-hide-matrix\] path=cpu" "$CLOG" | head -1
  exit 0
fi
echo "=== 1211pack cell: FAIL (see $CLOG) ==="
exit 1
