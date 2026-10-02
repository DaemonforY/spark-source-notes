#!/bin/bash
# 运行某个实验脚本
# 用法：SPARK_HOME=<Spark 源码目录> [MASTER=local[N]] ./run-experiment.sh <实验脚本> [--kryo] [其他 spark-shell 参数]
set -e
: "${SPARK_HOME:?请先设置 SPARK_HOME 为 Spark 源码目录}"
SCRIPT="$(cd "$(dirname "$1")" && pwd)/$(basename "$1")"
shift
# 其余参数原样传给 spark-shell；--kryo 是 "--conf spark.serializer=...KryoSerializer" 的简写
# 默认 --master local[4]，可用环境变量 MASTER 覆盖，例如 MASTER="local[1]"
EXTRA=()
for a in "$@"; do
  if [ "$a" = "--kryo" ]; then EXTRA+=(--conf spark.serializer=org.apache.spark.serializer.KryoSerializer); else EXTRA+=("$a"); fi
done
if [ -z "$JAVA_HOME" ] && [ -x /usr/libexec/java_home ]; then
  export JAVA_HOME=$(/usr/libexec/java_home -v 17)
fi
# 只打印实验输出（从第一行 "###" 或 "=====" 开始），隐藏启动横幅和日志
"$SPARK_HOME/bin/spark-shell" --master "${MASTER:-local[4]}" "${EXTRA[@]}" -I "$SCRIPT" < /dev/null 2>/dev/null \
  | awk 'f || /^(###|=====)/ { f = 1; print }'
