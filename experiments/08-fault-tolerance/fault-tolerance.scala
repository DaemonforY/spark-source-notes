// 实验：容错机制——Task 重试、Executor 丢失后的局部重算、Checkpoint
// 对应讲义：第 7 讲《容错机制》
// 运行（三种 master 各跑一次，脚本会根据 master 选择要做的实验）：
//   MASTER="local[2]"                 ./run-experiment.sh experiments/08-fault-tolerance/fault-tolerance.scala
//   MASTER="local[2,3]"               ./run-experiment.sh experiments/08-fault-tolerance/fault-tolerance.scala
//   MASTER="local-cluster[2,1,1024]"  ./run-experiment.sh experiments/08-fault-tolerance/fault-tolerance.scala
import org.apache.spark.TaskContext
import org.apache.spark.scheduler._
import scala.collection.mutable
import scala.util.Try

val master = sc.master
println(s"### master = $master")

if (master.startsWith("local[")) {
  // ---------- A. Task 重试 ----------
  // 注意：失败的 Task 对累加器的更新会被丢弃，所以要统计"尝试了几次"，必须监听 Task 结束事件
  println("### A. Task 重试：每个 Task 的前 2 次尝试都抛异常（共 2 个 Task）")
  val ok = new java.util.concurrent.atomic.AtomicInteger; val failed = new java.util.concurrent.atomic.AtomicInteger
  val listenerA = new SparkListener { override def onTaskEnd(e: SparkListenerTaskEnd): Unit =
    if (e.taskInfo.successful) ok.incrementAndGet() else failed.incrementAndGet() }
  sc.addSparkListener(listenerA)
  val acc = sc.longAccumulator("elements")
  val r = Try(sc.parallelize(1 to 4, 2).map { x =>
    acc.add(1)
    if (TaskContext.get.attemptNumber < 2) throw new RuntimeException(s"故意失败，attempt=${TaskContext.get.attemptNumber}")
    x
  }.count())
  Thread.sleep(800); sc.removeSparkListener(listenerA)
  println(s"  结果：${if (r.isSuccess) "成功，count = " + r.get else "Job 失败"}")
  println(s"  Task 尝试：失败 ${failed.get} 次，成功 ${ok.get} 次")
  println(s"  累加器（每处理一个元素 +1）= ${acc.value}：只有成功的那次尝试被计入，失败尝试的累加被丢弃")

  // ---------- C. Checkpoint ----------
  println("### C. Checkpoint：切断血缘，以及为什么要先 cache")
  sc.setCheckpointDir(java.nio.file.Files.createTempDirectory("ckpt").toString)
  var jobs = 0
  sc.addSparkListener(new SparkListener { override def onJobStart(e: SparkListenerJobStart): Unit = jobs += 1 })
  def deep(acc: org.apache.spark.util.LongAccumulator) =
    (1 to 50).foldLeft(sc.parallelize(1 to 100, 2).map { x => acc.add(1); x })((rdd, _) => rdd.map(_ + 1))
  for (withCache <- Seq(false, true)) {
    val acc = sc.longAccumulator("computed")
    val rdd = deep(acc)
    if (withCache) rdd.cache()
    val before = rdd.toDebugString.split("\n").length
    rdd.checkpoint()
    jobs = 0
    rdd.count(); Thread.sleep(800)
    println(s"  [${if (withCache) "先 cache 再 checkpoint" else "直接 checkpoint"}]")
    println(s"    血缘长度：${before} 层 → checkpoint 后 ${rdd.toDebugString.split("\n").length} 层，父依赖变成 ${rdd.dependencies.head.rdd.getClass.getSimpleName}")
    println(s"    一次 count() 触发了 $jobs 个 Job；源头数据被计算了 ${acc.value} 次（共 100 条）")
  }
}

if (master.startsWith("local-cluster")) {
  // ---------- B. Executor 丢失 → Shuffle 输出丢失 → 只重算丢失的部分 ----------
  println("### B. Executor 丢失后的局部重算")
  val stages = mutable.ArrayBuffer[String]()
  sc.addSparkListener(new SparkListener {
    override def onStageCompleted(e: SparkListenerStageCompleted): Unit = stages.synchronized {
      val i = e.stageInfo
      stages += s"    Stage ${i.stageId}（attempt ${i.attemptNumber()}）: ${i.name.split(" at ")(0)}，执行了 ${i.numTasks} 个 Task"
    }
  })
  while (sc.statusTracker.getExecutorInfos.length < 3) Thread.sleep(500)   // 等 2 个 Executor 都注册
  val reduced = sc.parallelize(1 to 1000, 4).map(i => (i % 10, i)).reduceByKey(_ + _, 2)
  println("  [1] 第一次 count：")
  reduced.count(); Thread.sleep(1000)
  stages.synchronized { stages.foreach(println); stages.clear() }
  val ids = sc.getClass.getMethod("getExecutorIds").invoke(sc).asInstanceOf[Seq[String]].sorted
  println(s"  [2] 杀掉 Executor ${ids.head}（当前 Executor：${ids.mkString(", ")}），它上面的 Shuffle 输出随之丢失")
  sc.killExecutor(ids.head)
  Thread.sleep(5000)
  println("  [3] 第二次 count：")
  reduced.count(); Thread.sleep(1000)
  stages.synchronized { stages.foreach(println) }
  println("  解读：被杀的 Executor 当时是空闲的，DAGScheduler 没有立即收到通知；")
  println("        第二次 count 时 reduce Stage 先跑，读 Shuffle 报 FetchFailed，")
  println("        才去重算 map Stage——只重算丢失的分区，然后 reduce Stage 重试（attempt 1）")
}
System.exit(0)
