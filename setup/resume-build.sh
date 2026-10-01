#!/bin/bash
# 编译被中断后，从指定模块继续编译（不 clean，复用已编译的模块）
# 用法：SPARK_HOME=<Spark 源码目录> ./setup/resume-build.sh spark-sql_2.13
# 模块名查法：grep -E "Building .*\[[0-9]+/[0-9]+\]" $SPARK_HOME/build.log | tail -1
#            再到该模块的 pom.xml 里查看 <artifactId>
set -e
: "${SPARK_HOME:?请先设置 SPARK_HOME 为 Spark 源码目录}"
MODULE="${1:?请指定要续编的模块 artifactId，例如 spark-sql_2.13}"
SETTINGS="$(cd "$(dirname "$0")" && pwd)/aliyun-settings.xml"

if [ -z "$JAVA_HOME" ] && [ -x /usr/libexec/java_home ]; then
  export JAVA_HOME=$(/usr/libexec/java_home -v 17)
fi
export MAVEN_OPTS="${MAVEN_OPTS:--Xss64m -Xmx6g -XX:ReservedCodeCacheSize=1g}"

cd "$SPARK_HOME"
./build/mvn -s "$SETTINGS" -T 2 -ntp -DskipTests -Phive -Phive-thriftserver package -rf ":$MODULE" 2>&1 | tee build-resume.log
