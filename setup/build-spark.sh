#!/bin/bash
# 完整编译 Spark 源码（跳过运行测试，开启 Hive 支持）
# 用法：SPARK_HOME=<Spark 源码目录> ./setup/build-spark.sh
set -e
: "${SPARK_HOME:?请先设置 SPARK_HOME 为 Spark 源码目录}"
SETTINGS="$(cd "$(dirname "$0")" && pwd)/aliyun-settings.xml"

# macOS 自动查找 JDK 17；Linux 请自行设置 JAVA_HOME
if [ -z "$JAVA_HOME" ] && [ -x /usr/libexec/java_home ]; then
  export JAVA_HOME=$(/usr/libexec/java_home -v 17)
fi
echo "JAVA_HOME=$JAVA_HOME"
export MAVEN_OPTS="${MAVEN_OPTS:--Xss64m -Xmx4g -XX:ReservedCodeCacheSize=1g}"

cd "$SPARK_HOME"
./build/mvn -s "$SETTINGS" -T 4 -ntp -DskipTests -Phive -Phive-thriftserver clean package 2>&1 | tee build.log
