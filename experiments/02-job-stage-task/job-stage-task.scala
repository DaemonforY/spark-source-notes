// 实验：Job、Stage、Task 的关系（7 个用例）
// 对应文章：《面试拆解 01｜Job、Stage、Task 到底是什么关系？》
// 运行：SPARK_HOME=<Spark 源码目录> ./run-experiment.sh experiments/02-job-stage-task/job-stage-task.scala
// 注意：Stage 编号、执行顺序在不同运行中可能略有差异，Stage 数、Task 数和是否 skipped 应与 expected-output.txt 一致

import org.apache.spark.scheduler._
import scala.collection.mutable
// 监听器：记录每个 Job 包含的 Stage，以及每个 Stage 的 Task 数、是否被跳过
class Rec extends SparkListener {
  val log = mutable.ArrayBuffer[String]()
  override def onJobStart(e: SparkListenerJobStart): Unit = synchronized {
    val ss = e.stageInfos.sortBy(_.stageId).map(s => s"Stage${s.stageId}(${s.numTasks} tasks, ${s.name.split(" at ")(0)}, parents=${s.parentIds.mkString(",")})")
    log += s"  Job${e.jobId}: " + ss.mkString(" | ")
  }
  override def onStageCompleted(e: SparkListenerStageCompleted): Unit = synchronized {
    log += s"    -> 实际执行 Stage${e.stageInfo.stageId}: ${e.stageInfo.numTasks} tasks"
  }
}
def run(title: String)(f: => Any): Unit = {
  val r = new Rec; sc.addSparkListener(r)
  val res = f; Thread.sleep(1500); sc.removeSparkListener(r)
  println(s"\n### $title  => 结果: $res"); r.log.foreach(println)
}
val words = sc.parallelize(Seq("a","b","c","a","b","a","d","e"), 4)
run("例1 窄依赖 map+filter+count") { words.map(_ * 2).filter(_ != "dd").count() }
val counts = words.map(w => (w, 1)).reduceByKey(_ + _, 3)
run("例2 reduceByKey(3) + collect") { counts.collect().sorted.mkString(",") }
run("例3 同一个 counts 再 count") { counts.count() }
run("例4 两次 shuffle: reduceByKey 后 groupBy") { counts.map{case (w,c)=>(c,w)}.groupByKey(2).collect().length }
run("例5 take(1)") { words.take(1).mkString }
run("例6 sortByKey(transformation)") { val s = words.map(w=>(w,1)).sortByKey(); "已定义" }
run("例7 join 两个 RDD") { words.map((_,1)).join(words.map((_,2)), 2).count() }
System.exit(0)
