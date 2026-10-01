#!/bin/bash
# 以调试模式启动 SparkPi：JVM 停在启动处，等待 IDEA 连接 5005 端口后才继续运行
# local[2] 模式下 Driver 和 Executor 在同一个 JVM 里，一个调试会话即可跟踪全链路
# 用法：SPARK_HOME=<Spark 源码目录> ./setup/debug-sparkpi.sh
set -e
: "${SPARK_HOME:?请先设置 SPARK_HOME 为 Spark 源码目录}"
if [ -z "$JAVA_HOME" ] && [ -x /usr/libexec/java_home ]; then
  export JAVA_HOME=$(/usr/libexec/java_home -v 17)
fi
EXAMPLES_JAR=$(ls "$SPARK_HOME"/examples/target/scala-2.13/jars/spark-examples_*.jar | head -1)

"$SPARK_HOME/bin/spark-submit" \
  --master "local[2]" \
  --class org.apache.spark.examples.SparkPi \
  --driver-java-options "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005" \
  --conf spark.network.timeout=10000s \
  --conf spark.executor.heartbeatInterval=1000s \
  "$EXAMPLES_JAR" 2
