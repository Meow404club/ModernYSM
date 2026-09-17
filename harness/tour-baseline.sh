#!/bin/bash
# 版本线 tour 基线 runner（可复跑记录：跑完打数值判据）。
# 用法: sh harness/tour-baseline.sh [--judge-only] <version-line> [screen ...]
#   --judge-only：不起游戏，只对在案 harness/out/<line>/ 产物重跑判据段
#   （审查复核用；exit 项以 020b 收尾屏在案佐证完整走查，不作独立判据源）。
# 判据：server 日志 "Done (" ≥1 行、16 屏 png 零 *-failed、world join ≥1、tour exit 0。
# 屏名口径：12 循环屏 png 无 0 前缀（texture.png…modern_roulette.png），必须 *.png
# 全量计——0*.png 只命中 4 件带编号屏（000/010/020/020b），"[ 4 -ge 16 ]"永假=
# PASS 分支不可达（审查实证根因）；排除 worldshot*（HARNESS_WORLDSHOT 环境泄漏防混入，
# 与 tour-1211pack.sh 同口径）。
# 基线在案（2026-09-17，work/harness-cell-1211pack-2612base @ 570a961）：
#   26.1.2-neoforge  Done + 16 png + 0 failed + world join（joined=true）+ exit 0
set -u
JUDGE_ONLY=0
if [ "${1:-}" = "--judge-only" ]; then
  JUDGE_ONLY=1
  shift
fi
VERSION="${1:?usage: sh harness/tour-baseline.sh [--judge-only] <version-line> [screen ...]}"
shift || true
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/harness/out/$VERSION"

if [ "$JUDGE_ONLY" = 1 ]; then
  EXIT=0
else
  sh "$(dirname "$0")/tour.sh" "$VERSION" "$@"
  EXIT=$?
fi
DONE_N=$(grep -c "Done (" "$OUT/server.log" 2>/dev/null || true)
PNG_N=$(find "$OUT" -maxdepth 1 -name "*.png" ! -name "worldshot*" ! -name "*failed*" | wc -l)
FAIL_N=$(find "$OUT" -maxdepth 1 -name "*-failed.png" | wc -l)
JOIN_N=$(grep -c "beat joined=true player=true level=true" "$OUT/client.log" 2>/dev/null || true)
echo "=== baseline $VERSION ==="
echo "exit=$EXIT (expect 0)  Done=$DONE_N (expect >=1)  png=$PNG_N (expect 16)  failed=$FAIL_N (expect 0)  worldjoin=$JOIN_N (expect >=1)"
[ "$EXIT" = 0 ] && [ "$DONE_N" -ge 1 ] && [ "$PNG_N" -ge 16 ] && [ "$FAIL_N" = 0 ] && [ "$JOIN_N" -ge 1 ] && { echo "PASS"; exit 0; }
echo "FAIL"
exit 1
