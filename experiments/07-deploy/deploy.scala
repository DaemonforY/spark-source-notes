// 实验：RPC 端点、Executor 独立进程、动态资源分配的扩容与回收
// 对应讲义：第 6 讲《RPC 与部署》
// 运行（local-cluster 会在本机启动一个真正的 Standalone 集群，Executor 是独立的 JVM 进程）：
//   SPARK_HOME=<Spark 源码目录> MASTER="local-cluster[4,1,1024]" ./run-experiment.sh experiments/07-deploy/deploy.scala \
//     --conf spark.dynamicAllocation.enabled=true --conf spark.dynamicAllocation.initialExecutors=0 \
//     --conf spark.dynamicAllocation.executorIdleTimeout=5s
// 注意：local-cluster 需要已编译好的 Spark（assembly 目录下的 jar），只用于测试
import java.lang.management.ManagementFactory
import org.apache.spark.SparkEnv
import org.apache.spark.scheduler._
import scala.collection.mutable
import scala.jdk.CollectionConverters._

val t0 = System.currentTimeMillis
def ts = f"${(System.currentTimeMillis - t0) / 1000.0}%5.1fs"
val events = mutable.ArrayBuffer[String]()
sc.addSparkListener(new SparkListener {
  override def onExecutorAdded(e: SparkListenerExecutorAdded): Unit = events.synchronized { events += s"  [$ts] + Executor ${e.executorId} 注册（${e.executorInfo.totalCores} 核）" }
  override def onExecutorRemoved(e: SparkListenerExecutorRemoved): Unit = events.synchronized { events += s"  [$ts] - Executor ${e.executorId} 被移除：${e.reason}" }
})
def executors() = sc.statusTracker.getExecutorInfos.length - 1   // 去掉 Driver 自己

// ---------- A. Driver 上注册了哪些 RPC 端点 ----------
println("### A. Driver 上的 RPC 端点")
// rpcEnv 是 private[spark]，用反射读取
val rpcEnv = SparkEnv.get.getClass.getMethod("rpcEnv").invoke(SparkEnv.get)
println("  RpcEnv 实现：" + rpcEnv.getClass.getSimpleName + "，地址：" + rpcEnv.getClass.getMethod("address").invoke(rpcEnv))
val dispatcher = { val f = rpcEnv.getClass.getDeclaredFields.find(_.getName.endsWith("dispatcher")).get; f.setAccessible(true); f.get(rpcEnv) }
val endpointsField = dispatcher.getClass.getDeclaredFields.find(_.getName.endsWith("endpoints")).get
endpointsField.setAccessible(true)
val names = endpointsField.get(dispatcher).asInstanceOf[java.util.concurrent.ConcurrentMap[String, _]].keySet.asScala.toSeq.sorted
names.foreach(n => println("  · " + n))

// ---------- B. 动态资源分配：从 0 个 Executor 开始 ----------
println("### B. 动态资源分配")
println(s"  [$ts] 启动时 Executor 数：${executors()}（initialExecutors=0）")
// 后台线程：每 100ms 读取 ExecutorAllocationManager 的"目标 Executor 数"，记录变化
// （executorAllocationManager 和目标值字段都是 private，用反射读取）
val eam = sc.getClass.getMethod("executorAllocationManager").invoke(sc).asInstanceOf[Option[AnyRef]].get
val targetField = eam.getClass.getDeclaredFields.find(_.getName.endsWith("numExecutorsTargetPerResourceProfileId")).get
targetField.setAccessible(true)
def target() = targetField.get(eam).asInstanceOf[scala.collection.mutable.HashMap[Int, Int]].getOrElse(0, 0)
val targets = mutable.ArrayBuffer[String]()
@volatile var polling = true
new Thread(() => { var last = -1; while (polling) { val t = target(); if (t != last) { targets.synchronized { targets += s"  [$ts] 目标 Executor 数 = $t" }; last = t }; Thread.sleep(100) } }).start()
// 提交 16 个 Task，每个 Task 睡 3 秒，制造"积压"
val pids = sc.parallelize(1 to 16, 16).map { _ => Thread.sleep(3000); ManagementFactory.getRuntimeMXBean.getName }.collect().distinct
println(s"  [$ts] Job 完成，期间 Executor 数：${executors()}")
// ---------- C. Executor 是独立进程 ----------
println("### C. Executor 是独立的 JVM 进程")
println("  Driver 进程：" + ManagementFactory.getRuntimeMXBean.getName)
pids.sorted.foreach(p => println("  Task 运行所在进程：" + p))
// ---------- D. 空闲回收 ----------
Thread.sleep(15000)
polling = false
println("### D. Driver 端的目标 Executor 数变化（ExecutorAllocationManager）")
targets.synchronized { targets.foreach(println) }
println("### E. Executor 注册与移除事件")
events.synchronized { events.foreach(println) }
println(s"  [$ts] 空闲 15 秒后 Executor 数：${executors()}")
System.exit(0)
