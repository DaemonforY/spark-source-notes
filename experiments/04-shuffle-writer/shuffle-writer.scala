// 实验：SortShuffleManager 的三种 Writer 分别在什么条件下被选用
// 对应讲义：第 3 讲《Shuffle 原理》4.5 节
// 运行（分别用默认 Java 序列化器和 Kryo 各跑一次）：
//   SPARK_HOME=<Spark 源码目录> ./run-experiment.sh experiments/04-shuffle-writer/shuffle-writer.scala
//   SPARK_HOME=<Spark 源码目录> ./run-experiment.sh experiments/04-shuffle-writer/shuffle-writer.scala --kryo
// 原理：ShuffleDependency 创建时就会调用 registerShuffle 得到 shuffleHandle，Handle 的类型决定 Writer：
//   BypassMergeSortShuffleHandle -> BypassMergeSortShuffleWriter
//   SerializedShuffleHandle      -> UnsafeShuffleWriter（tungsten-sort）
//   BaseShuffleHandle            -> SortShuffleWriter
import org.apache.spark.ShuffleDependency
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.execution.exchange.ShuffleExchangeExec

case class P(a: Int)

def rddHandle(r: RDD[_]): String = r.dependencies.collectFirst { case d: ShuffleDependency[_, _, _] =>
  s"${d.shuffleHandle.getClass.getSimpleName}  (mapSideCombine=${d.mapSideCombine}, 分区数=${d.partitioner.numPartitions}, 序列化器=${d.serializer.getClass.getSimpleName})"
}.get

// SQL 部分关闭 AQE，便于直接在执行计划里找到 ShuffleExchangeExec
spark.conf.set("spark.sql.adaptive.enabled", "false")
def sqlHandle(df: org.apache.spark.sql.Dataset[_]): String = df.queryExecution.executedPlan.collectFirst {
  case e: ShuffleExchangeExec =>
    s"${e.shuffleDependency.shuffleHandle.getClass.getSimpleName}  (分区数=${e.shuffleDependency.partitioner.numPartitions}, 序列化器=${e.shuffleDependency.serializer.getClass.getSimpleName})"
}.get

val pairs = sc.parallelize(1 to 1000, 4).map(i => (i % 50, i))
println("### spark.serializer = " + sc.getConf.get("spark.serializer", "JavaSerializer（默认）"))
println("1  RDD reduceByKey(_+_, 3)              -> " + rddHandle(pairs.reduceByKey(_ + _, 3)))
println("2  RDD groupByKey(3)                    -> " + rddHandle(pairs.groupByKey(3)))
println("3  RDD groupByKey(300)，K/V 都是 Int     -> " + rddHandle(pairs.groupByKey(300)))
println("4  RDD reduceByKey(_+_, 300)            -> " + rddHandle(pairs.reduceByKey(_ + _, 300)))
println("6  RDD groupByKey(300)，value 是自定义类 -> " + rddHandle(sc.parallelize(1 to 1000, 4).map(i => (i % 50, P(i))).groupByKey(300)))
println("7  SQL repartition(300)                 -> " + sqlHandle(spark.range(0, 1000).repartition(300)))
println("8  SQL repartition(200)                 -> " + sqlHandle(spark.range(0, 1000).repartition(200)))
println("9  SQL repartition(201)                 -> " + sqlHandle(spark.range(0, 1000).repartition(201)))
println("10 SQL groupBy().count()（默认分区数）   -> " + sqlHandle(spark.range(0, 1000).groupBy(org.apache.spark.sql.functions.col("id") % 10).count()))
System.exit(0)
