// 实验：AQE 的三大优化 + DataSource V2 的下推
// 对应讲义：第 10 讲《AQE、DataSource V2 与 Spark Connect》
// 运行：SPARK_HOME=<Spark 源码目录> ./run-experiment.sh experiments/11-aqe-dsv2-connect/aqe-dsv2.scala --driver-memory 2g
import org.apache.spark.scheduler._
import org.apache.spark.sql.execution.adaptive.AdaptiveSparkPlanExec
import scala.collection.mutable

// 记录每个 Stage 实际执行的 Task 数
val stageTasks = mutable.ArrayBuffer[Int]()
sc.addSparkListener(new SparkListener {
  override def onStageCompleted(e: SparkListenerStageCompleted): Unit = stageTasks.synchronized { stageTasks += e.stageInfo.numTasks }
})
def runAndCount(df: org.apache.spark.sql.DataFrame): Seq[Int] = {
  stageTasks.synchronized(stageTasks.clear()); df.collect(); Thread.sleep(800); stageTasks.synchronized(stageTasks.toList)
}
def planLines(df: org.apache.spark.sql.DataFrame, keys: String*) =
  df.queryExecution.executedPlan.toString.split("\n").filter(l => keys.exists(l.contains)).map(l => "      " + l.trim.take(150))
def finalPlan(df: org.apache.spark.sql.DataFrame) = df.queryExecution.executedPlan match {
  case a: AdaptiveSparkPlanExec => a.finalPhysicalPlan; case p => p }

// ---------- A. 合并 Shuffle 分区 ----------
println("### A. 合并 Shuffle 分区（spark.sql.shuffle.partitions = 200，数据很小）")
for (aqe <- Seq("false", "true")) {
  spark.conf.set("spark.sql.adaptive.enabled", aqe)
  val df = spark.range(0, 100000, 1, 8).groupBy((org.apache.spark.sql.functions.col("id") % 10).as("k")).count()
  val tasks = runAndCount(df)
  println(s"  AQE=$aqe：各 Stage 的 Task 数 = ${tasks.mkString(", ")}")
  if (aqe == "true") planLines(df, "AQEShuffleRead").foreach(println)
}

// ---------- B. 动态切换 Join 策略 ----------
println("### B. 动态切换 Join 策略：静态估计很大，运行时发现很小")
spark.conf.set("spark.sql.adaptive.enabled", "true")
val big   = spark.range(0, 5000000).selectExpr("id", "id % 1000 AS k")
val small = spark.range(0, 5000000).filter("id < 100").selectExpr("id AS k2")   // 过滤后只剩 100 行，但静态估计不知道
val joined = big.join(small, big("k") === small("k2"))
println("  执行前（初始计划）：")
planLines(joined, "Join").foreach(println)
joined.collect()
println("  执行后（最终计划）：")
finalPlan(joined).toString.split("\n").filter(_.contains("Join")).foreach(l => println("      " + l.trim.take(150)))

// ---------- C. 处理倾斜 Join ----------
println("### C. 处理倾斜 Join：90% 的数据 key = 0")
spark.conf.set("spark.sql.autoBroadcastJoinThreshold", "-1")                    // 禁止广播，强制 SortMergeJoin
spark.conf.set("spark.sql.shuffle.partitions", "20")
spark.conf.set("spark.sql.adaptive.coalescePartitions.enabled", "false")
spark.conf.set("spark.sql.adaptive.skewJoin.skewedPartitionThresholdInBytes", "1MB")   // 小数据也能触发
spark.conf.set("spark.sql.adaptive.advisoryPartitionSizeInBytes", "1MB")
val skewed = spark.range(0, 2000000).selectExpr("CASE WHEN id < 1800000 THEN 0 ELSE id END AS k", "id AS v")
val other  = spark.range(0, 2000000).selectExpr("id AS k", "id AS w")
for (skew <- Seq("false", "true")) {
  spark.conf.set("spark.sql.adaptive.skewJoin.enabled", skew)
  val j = skewed.join(other, "k").selectExpr("sum(v + w)")
  val tasks = runAndCount(j)
  println(s"  skewJoin.enabled=$skew：各 Stage 的 Task 数 = ${tasks.mkString(", ")}")
  finalPlan(j).toString.split("\n").filter(l => l.contains("SortMergeJoin") || l.contains("AQEShuffleRead")).foreach(l => println("      " + l.trim.take(150)))
}
Seq("spark.sql.autoBroadcastJoinThreshold", "spark.sql.shuffle.partitions", "spark.sql.adaptive.coalescePartitions.enabled",
    "spark.sql.adaptive.skewJoin.skewedPartitionThresholdInBytes", "spark.sql.adaptive.advisoryPartitionSizeInBytes",
    "spark.sql.adaptive.skewJoin.enabled").foreach(spark.conf.unset)

// ---------- D. DataSource V1 vs V2：聚合下推 ----------
println("### D. DataSource V1 vs V2：聚合下推到 Parquet")
val dir = java.nio.file.Files.createTempDirectory("dsv2").toString + "/t"
spark.range(0, 1000000).selectExpr("id", "id % 100 AS g").write.parquet(dir)
spark.conf.set("spark.sql.parquet.aggregatePushdown", "true")
for ((label, v1List) <- Seq(("V1（默认，parquet 在 useV1SourceList 中）", "avro,csv,json,kafka,orc,parquet,text"), ("V2（把 parquet 从 useV1SourceList 移除）", "avro,csv,json,kafka,orc,text"))) {
  spark.conf.set("spark.sql.sources.useV1SourceList", v1List)
  val df = spark.read.parquet(dir).selectExpr("max(id)", "min(id)", "count(*)")
  df.collect()
  println(s"  $label：")
  finalPlan(df).toString.split("\n").filter(l => l.contains("Scan")).foreach { l =>
    val t = l.trim
    println("      " + t.take(60) + " ...")
    "PushedAggregation: \\[[^\\]]*\\]".r.findFirstIn(t).foreach(m => println("        " + m))
    "PushedFilters: \\[[^\\]]*\\]".r.findFirstIn(t).foreach(m => println("        " + m))
  }
}
System.exit(0)
