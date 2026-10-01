// 实验：宽窄依赖与 Stage 切分（10 组对比 + toDebugString）
// 对应文章：《宽窄依赖：Stage 到底在哪里切开？》
// 运行：SPARK_HOME=<Spark 源码目录> ./run-experiment.sh experiments/03-dependency/dependency.scala

import org.apache.spark._
import org.apache.spark.rdd.RDD
import org.apache.spark.scheduler._
import scala.collection.mutable
class Rec extends SparkListener {
  val stages = mutable.ArrayBuffer[String](); val ran = mutable.ArrayBuffer[Int]()
  override def onJobStart(e: SparkListenerJobStart): Unit = synchronized {
    stages ++= e.stageInfos.sortBy(_.stageId).map(s => s"S${s.stageId}(${s.numTasks}t)") }
  override def onStageCompleted(e: SparkListenerStageCompleted): Unit = synchronized { ran += e.stageInfo.stageId }
}
def deps(r: RDD[_]): String = r.dependencies.map(d => d.getClass.getName.split('.').last).mkString(",")
def run(title: String, r: RDD[_]): Unit = {
  val rec = new Rec; sc.addSparkListener(rec); r.count(); Thread.sleep(1200); sc.removeSparkListener(rec)
  println(s"### $title"); println(s"    最后一个RDD: ${r.getClass.getSimpleName}, 依赖: ${deps(r)}, partitioner: ${r.partitioner.map(_.getClass.getSimpleName).getOrElse("None")}")
  println(s"    规划的Stage: ${rec.stages.mkString(" ")} | 实际执行: ${rec.ran.size} 个")
}
def base() = sc.parallelize(1 to 100, 4)
def pairs() = base().map(i => (i % 10, i))
val p3 = new HashPartitioner(3)
run("1 map+filter", base().map(_ + 1).filter(_ > 5))
run("2 union", base().union(base()))
val co = base().coalesce(2)
run("3 coalesce(4->2)", co)
println("    coalesce 子分区0 依赖的父分区: " + co.dependencies.head.asInstanceOf[NarrowDependency[_]].getParents(0))
run("4 repartition(2)", base().repartition(2))
run("5 reduceByKey(3)", pairs().reduceByKey(_ + _, 3))
run("6a partitionBy(p3) 再 reduceByKey(p3)", pairs().partitionBy(p3).reduceByKey(p3, _ + _))
run("6b partitionBy(p3) -> map -> reduceByKey(p3)", pairs().partitionBy(p3).map(identity).reduceByKey(p3, _ + _))
run("6c partitionBy(p3) -> mapValues -> reduceByKey(p3)", pairs().partitionBy(p3).mapValues(_ + 1).reduceByKey(p3, _ + _))
run("7a 普通 join", { val a = pairs(); a.join(a.mapValues(_ + 1), p3) })
run("7b 预分区后 join", { val a = pairs().partitionBy(p3); a.join(a.mapValues(_ + 1), p3) })
println("\n### toDebugString（6b）"); println(pairs().partitionBy(p3).map(identity).reduceByKey(p3, _ + _).toDebugString)
System.exit(0)
