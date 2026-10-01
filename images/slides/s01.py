# 01 源码通关 01：从零编译 Spark 4.2.0 源码 —— 小红书配图（收藏型）

SLIDES = [
    {"type": "cover",
     "title": "编译 Spark 源码\n**避坑清单**",
     "subtitle": "国内网络 · Spark 4.2.0\n照着做，一次成功",
     "badge": "源码通关 01"},

    {"type": "content",
     "heading": "坑 1：依赖下载只有 20 KB/s 🐢",
     "items": [
         "原因：Spark 的 pom 里配的是**海外镜像**",
         "解决：换成阿里云镜像，实测**快了两百多倍**",
         "⭐ 技巧：**不改全局配置**",
         "  单独写一个 `aliyun-settings.xml`",
         "  编译时用 `-s` 参数指定",
         "  不影响电脑上的其他 Maven 项目",
     ]},

    {"type": "content",
     "heading": "镜像配置文件（截图保存）",
     "code": """<settings>
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <mirrorOf>*</mirrorOf>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>""",
     "note": "`mirrorOf *` 表示**所有仓库**的请求都走阿里云，包括 pom 里配的海外镜像"},

    {"type": "content",
     "heading": "坑 2：JDK 版本",
     "items": [
         "官方支持：Java **17 / 21 / 25**",
         "⚠️ pom 的检查只要求 **≥ 17**",
         "  所以 JDK 23 不会被拦",
         "  但它**不在官方支持列表**里，出问题很难查",
         "✅ 建议直接用 **JDK 17**",
     ],
     "code": "export JAVA_HOME=$(/usr/libexec/java_home -v 17)\njava -version   # 确认是 17"},

    {"type": "content",
     "heading": "坑 3：编译进程被杀了 3 次 💀",
     "items": ["先看懂退出码：**128 + 信号编号**"],
     "code": "137 = 128 + 9   SIGKILL  强制杀死\n143 = 128 + 15  SIGTERM  被要求终止\n130 = 128 + 2   SIGINT   按了 Ctrl+C",
     "items_after": [
         "日志里有没有 **OutOfMemoryError**？",
         "系统内存够不够？",
         "✅ 长任务放在**独立终端 / tmux** 里跑",
     ]},

    {"type": "content",
     "heading": "救命技巧：断点续编 🔥",
     "items": [
         "被中断了，**不用从头来**！",
         "① 去掉 `clean`（否则编译好的全被删）",
         "② 加上 `-rf :模块名`",
     ],
     "code": "./build/mvn -s aliyun-settings.xml \\\n  -DskipTests -Phive -Phive-thriftserver \\\n  package **-rf :spark-sql_2.13**",
     "note": "从哪继续？看日志里最后一个 `Building` 的模块，再去它的 pom.xml 里查 artifactId"},

    {"type": "content",
     "heading": "完整编译命令",
     "code": """export MAVEN_OPTS="-Xss64m -Xmx4g \\
  -XX:ReservedCodeCacheSize=1g"

./build/mvn -s aliyun-settings.xml \\
  -T 4 -ntp -DskipTests \\
  -Phive -Phive-thriftserver \\
  clean package""",
     "items_after": [
         "`build/mvn` 会**自动下载**正确版本的 Maven（3.9.15）",
         "`-DskipTests` 跳过运行测试，**别用** `maven.test.skip`",
     ]},

    {"type": "content",
     "heading": "验证成功 🎉",
     "code": "./bin/spark-shell --master \"local[2]\"\n\nscala> spark.range(1, 101)\n         .selectExpr(\"sum(id)\")\n         .first().getLong(0)\nres0: Long = **5050**\n\nscala> spark.version\nres1: String = **4.2.0**",
     "items_after": ["1 加到 100 = 5050 ✅", "版本号 4.2.0 ✅"]},

    {"type": "card",
     "heading": "避坑清单（收藏这张就够）",
     "card": """✅ JDK 用 17
✅ git clone --filter=blob:none
✅ 基于 tag 建学习分支
✅ 单独的镜像配置 + -s 指定
✅ 用 ./build/mvn
✅ 设置 MAVEN_OPTS
✅ -DskipTests（别用 maven.test.skip）
✅ 独立终端 / tmux 里编译
✅ **中断了用 -rf 续编**
✅ spark-shell 验证 sum = 5050""",
     "note": "下期：设 27 个断点，跟踪一个 Spark Job 的一生"},
]
