// 实验：Spark Connect 客户端与服务端
// 对应讲义：第 10 讲《AQE、DataSource V2 与 Spark Connect》
// 需要先启动一个只监听本机的 Connect 服务（见 README），再运行：
//   SPARK_HOME=<Spark 源码目录> MASTER="local[1]" ./run-experiment.sh experiments/11-aqe-dsv2-connect/connect-client.scala
// 说明：Connect 版 REPL（spark-shell --remote）需要真实终端；这里改为在经典 spark-shell 中用代码创建 Connect 客户端会话

val remote = org.apache.spark.sql.connect.SparkSession.builder().remote("sc://127.0.0.1:15002").getOrCreate()
println("### E1 经典模式的 spark：" + spark.getClass.getName)
println("### E1 Connect 客户端会话：" + remote.getClass.getName)
println("### E2 通过 Connect 执行查询：sum(0..99) = " + remote.range(100).selectExpr("sum(id)").collect()(0).getLong(0))
println("### E3 在 Connect 会话上访问 sparkContext：" + scala.util.Try(remote.sparkContext).fold(e => "失败 → " + e.getClass.getSimpleName + ": " + Option(e.getMessage).getOrElse("").take(200), _ => "成功"))
println("### E4 Connect 客户端发出的未解析计划（protobuf 的文本形式，节选）：")
println(remote.range(100).filter("id > 50").plan.toString.split("\n").take(30).map("  " + _).mkString("\n"))
println("### E5 服务端执行时的物理计划：")
remote.range(100).filter("id > 50").explain()
System.exit(0)
