#!/bin/bash
# 运行某个实验脚本
# 用法：SPARK_HOME=<Spark 源码目录> ./run-experiment.sh <实验脚本> [--kryo]
set -e
: "${SPARK_HOME:?请先设置 SPARK_HOME 为 Spark 源码目录}"
SCRIPT="$(cd "$(dirname "$1")" && pwd)/$(basename "$1")"
EXTRA=()
# 第二个参数 --kryo：使用 Kryo 序列化器运行
if [ "$2" = "--kryo" ]; then EXTRA=(--conf spark.serializer=org.apache.spark.serializer.KryoSerializer); fi
if [ -z "$JAVA_HOME" ] && [ -x /usr/libexec/java_home ]; then
  export JAVA_HOME=$(/usr/libexec/java_home -v 17)
fi
# 只打印实验输出（从第一行 "###" 或 "=====" 开始），隐藏启动横幅和日志
"$SPARK_HOME/bin/spark-shell" --master "local[4]" "${EXTRA[@]}" -I "$SCRIPT" < /dev/null 2>/dev/null \
  | awk 'f || /^(###|=====)/ { f = 1; print }'
