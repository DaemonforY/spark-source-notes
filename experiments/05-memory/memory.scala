// 实验：统一内存管理的内存划分与借用规则
// 对应讲义：第 4 讲《内存管理》
// 运行（用 1 个线程、1g 内存，结果更容易对照）：
//   SPARK_HOME=<Spark 源码目录> MASTER="local[1]" ./run-experiment.sh experiments/05-memory/memory.scala --driver-memory 1g
// 说明：local 模式下 Driver 兼任 Executor，所以 --driver-memory 就是这个"Executor"的堆内存
import org.apache.spark.SparkEnv
import org.apache.spark.scheduler._
import org.apache.spark.storage.StorageLevel

val MB = 1024.0 * 1024
// memoryManager 是 private[spark]，在 REPL 中通过反射读取（Scala 的包私有成员在字节码里是 public）
val mm = SparkEnv.get.getClass.getMethod("memoryManager").invoke(SparkEnv.get)
def call(name: String): Long = mm.getClass.getMethod(name).invoke(mm).asInstanceOf[Long]

// ---------- 实验 A：内存划分是否符合公式 ----------
val sys = Runtime.getRuntime.maxMemory
val unified = call("maxOnHeapStorageMemory") + call("onHeapExecutionMemoryUsed")
println(s"### A. 内存划分（driver-memory=${sc.getConf.get("spark.driver.memory", "1g")}）")
println(f"  JVM Runtime.maxMemory          = ${sys / MB}%8.1f MB")
println(f"  公式 (系统内存 - 300MB) × 0.6  = ${(sys - 300 * MB) * 0.6 / MB}%8.1f MB")
println(f"  实际统一内存（Storage + Execution）= ${unified / MB}%8.1f MB")
println(f"  其中 Storage 区域（× 0.5）      = ${unified * 0.5 / MB}%8.1f MB")

// ---------- 实验 B：借用与收回 ----------
@volatile var peak = 0L
@volatile var spill = 0L
sc.addSparkListener(new SparkListener {
  override def onTaskEnd(e: SparkListenerTaskEnd): Unit = if (e.taskMetrics != null) {
    peak = math.max(peak, e.taskMetrics.peakExecutionMemory)
    spill += e.taskMetrics.memoryBytesSpilled
  }
})
// B1. 缓存约 70% 统一内存的数据，超过 Storage 区域（50%）
val n = (unified * 0.7 / (8 * MB)).toInt
val cached = sc.parallelize(0 until n, n).map(_ => new Array[Byte](8 * 1024 * 1024)).persist(StorageLevel.MEMORY_ONLY)
cached.count()
println("### B. 借用与收回")
println(f"  [B1] 缓存 $n 个 8MB 分区：Storage 已用 ${call("storageMemoryUsed") / MB}%.1f MB（Storage 区域只有 ${unified * 0.5 / MB}%.1f MB）")
// B2. 运行一个需要大量 Execution 内存的聚合（ExternalAppendOnlyMap 会不断申请内存）
sc.parallelize(0 until 3000000, 1).map(i => (i, "x" * 50)).groupByKey(1).count()
Thread.sleep(1500)
val left = sc.getRDDStorageInfo.find(_.id == cached.id).map(i => (i.numCachedPartitions, i.memSize)).getOrElse((0, 0L))
println(f"  [B2] 大聚合之后：缓存剩 ${left._1} 个分区，Storage 已用 ${left._2 / MB}%.1f MB")
println(f"       该聚合的峰值 Execution 内存 ${peak / MB}%.1f MB，溢写 ${spill / MB}%.1f MB")
System.exit(0)
