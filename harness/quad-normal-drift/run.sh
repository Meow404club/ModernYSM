#!/bin/bash
# quad-normal-drift 离线数值探针（本卡不依赖 gradle/游戏进程，普通 JVM 即跑）。
#
# 目的（定界，非修复）：
#   ①烘焙层跨 run 对账：同输入经真实产线烘焙链
#     folder json --YSMFolderDeserializer--> RawYsmModel --YSMBinarySerializer(32,true)-->
#     bytes --YSMBinaryDeserializer(32)--> RawYsmModel（=ServerModelManager.java:1268 产线参数）
#     跑 3 遍（folder/binary/folder#2）×2 个独立 JVM，sha256(quads) 必须逐位一致。
#   ②同一 bind UV 配置对不同 geo 的命中面法线对账：trissyUV=(0.21289062,0.27148438)/
#     up=(0.013671875,0.27539062)（用户真机与 dev env realcamera.json 同值）分别打
#     Trissy(256px) 与 built/default(128px) 两个 geo，命中面 bone:cube:face+法线不同
#     = 资源层定界证据（读数=(配置 UV × 激活 geo) 的函数）。
#
# 用法：run.sh < classes.txt（每行一个模型目录）
# 依赖：javac + 项目 1.20.1-forge 编译产物 + gradle caches libraries（gson/joml/netty/fastutil）。
set -u
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
MAIN_REPO="${MAIN_REPO:-/home/brokestar/workspace/ModernYSM}"
SRC_MAIN="${SRC_MAIN:-$MAIN_REPO/versions/1.20.1-forge/build/classes/java/main}"
LIBS="${LIBS:-/home/brokestar/.gradle/caches/minecraft/libraries}"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

CP_LIBS=$(find "$LIBS" -name "*.jar" | grep -v "sources\|javadoc\|natives" | tr '\n' ':')
javac -nowarn -cp "$SRC_MAIN:$CP_LIBS" -d "$WORK/classes" "$(dirname "$0")/QuadNormalProbe.java" || exit 1

MODELS=$(cat)
java -cp "$WORK/classes:$SRC_MAIN:$CP_LIBS" QuadNormalProbe $MODELS > "$WORK/runA.txt" || exit 1
java -cp "$WORK/classes:$SRC_MAIN:$CP_LIBS" QuadNormalProbe $MODELS > "$WORK/runB.txt" || exit 1

if diff "$WORK/runA.txt" "$WORK/runB.txt" > /dev/null; then
  echo "PROBE RESULT: runA==runB (cross-JVM bit-identical)"
else
  echo "PROBE RESULT: runA!=runB — DRIFT DETECTED"
  diff "$WORK/runA.txt" "$WORK/runB.txt"
fi
cat "$WORK/runA.txt"
