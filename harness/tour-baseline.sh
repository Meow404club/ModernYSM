#!/bin/bash
# 版本线 tour 基线 runner（可复跑记录：跑完打数值判据）。
# 用法: sh harness/tour-baseline.sh <version-line> [screen ...]
# 判据：server 日志 "Done (" ≥1 行、16 屏 png 零 *-failed、tour exit 0。
# 基线在案（2026-09-17，work/harness-cell-1211pack-2612base @ 570a961）：
#   26.1.2-neoforge  Done + 16 png + 0 failed + world join（joined=true）+ exit 0
set -u
sh "$(dirname "$0")/tour.sh" "$@"
EXIT=$?
VERSION="${1:?}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/harness/out/$VERSION"
DONE_N=$(grep -c "Done (" "$OUT/server.log" 2>/dev/null || true)
PNG_N=$(find "$OUT" -maxdepth 1 -name "0*.png" ! -name "*failed*" | wc -l)
FAIL_N=$(find "$OUT" -maxdepth 1 -name "*-failed.png" | wc -l)
JOIN_N=$(grep -c "beat joined=true player=true level=true" "$OUT/client.log" 2>/dev/null || true)
echo "=== baseline $VERSION ==="
echo "exit=$EXIT (expect 0)  Done=$DONE_N (expect >=1)  png=$PNG_N (expect 16)  failed=$FAIL_N (expect 0)  worldjoin=$JOIN_N (expect >=1)"
[ "$EXIT" = 0 ] && [ "$DONE_N" -ge 1 ] && [ "$PNG_N" -ge 16 ] && [ "$FAIL_N" = 0 ] && [ "$JOIN_N" -ge 1 ] && { echo "PASS"; exit 0; }
echo "FAIL"
exit 1
