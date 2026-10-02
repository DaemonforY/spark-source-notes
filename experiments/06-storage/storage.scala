// 实验：存储体系——存储级别、cache 默认级别、缓存命中、广播分块、磁盘目录
// 对应讲义：第 5 讲《存储体系》
// 运行：SPARK_HOME=<Spark 源码目录> ./run-experiment.sh experiments/06-storage/storage.scala --driver-memory 2g
import org.apache.spark.SparkEnv
import org.apache.spark.storage.StorageLevel

val MB = 1024.0 * 1024
case class User(id: Long, name: String, city: String)
def memOf(rddId: Int) = sc.getRDDStorageInfo.find(_.id == rddId).map(i => (i.memSize, i.diskSize, i.numCachedPartitions)).get

// ---------- A. 同一份数据，不同存储级别占多少内存 ----------
println("### A. 同一份数据（100 万个 User 对象）在不同存储级别下的大小")
def users() = sc.parallelize(0L until 1000000L, 4).map(i => User(i, s"user_$i", if (i % 2 == 0) "Beijing" else "Shanghai"))
for (level <- Seq(StorageLevel.MEMORY_ONLY, StorageLevel.MEMORY_ONLY_SER, StorageLevel.DISK_ONLY)) {
  val r = users().persist(level); r.count()
  val (m, d, n) = memOf(r.id)
  println(f"  ${level.description}%-38s 内存 ${m / MB}%7.1f MB  磁盘 ${d / MB}%7.1f MB  （$n 个分区）")
  r.unpersist(blocking = true)
}

// ---------- B. cache() 的默认存储级别 ----------
println("### B. cache() 的默认存储级别")
println("  RDD.cache()     -> " + sc.parallelize(1 to 10).cache().getStorageLevel.description)
println("  Dataset.cache() -> " + spark.range(10).cache().storageLevel.description)
spark.conf.set("spark.sql.defaultCacheStorageLevel", "MEMORY_ONLY")
println("  设置 spark.sql.defaultCacheStorageLevel=MEMORY_ONLY 后 Dataset.cache() -> " + spark.range(20).cache().storageLevel.description)

// ---------- C. 缓存命中：第二次 Action 不再重新计算 ----------
println("### C. 缓存命中")
val computed = sc.longAccumulator("computed")
val c = sc.parallelize(1 to 1000, 4).map { x => computed.add(1); x * 2 }.cache()
c.count(); val first = computed.value
c.count(); val second = computed.value - first
println(s"  第 1 次 count：map 函数执行了 $first 次（计算并写入缓存）")
println(s"  第 2 次 count：map 函数执行了 $second 次（直接读缓存块 rdd_${c.id}_0 ~ rdd_${c.id}_3）")

// ---------- D. 广播变量按 spark.broadcast.blockSize（默认 4m）切块 ----------
println("### D. 广播变量分块")
val rnd = new scala.util.Random(42)
val payload = Array.fill[Byte](10 * 1024 * 1024)(rnd.nextInt(256).toByte)   // 10MB 随机字节，压缩不掉
val b = sc.broadcast(payload)
val f = b.getClass.getDeclaredField("numBlocks"); f.setAccessible(true)
println(s"  广播 10MB 随机字节，blockSize=${sc.getConf.get("spark.broadcast.blockSize", "4m")}：被切成 ${f.get(b)} 块（broadcast_${b.id}_piece0 ...）")
println(s"  Executor 端读到的长度：${sc.parallelize(1 to 2, 2).map(_ => b.value.length).collect().mkString(", ")}")

// ---------- E. 块在磁盘上的目录结构 ----------
println("### E. 块在磁盘上的位置")
val d = sc.parallelize(1 to 1000, 2).persist(StorageLevel.DISK_ONLY); d.count()
val bm = SparkEnv.get.blockManager
val dbm = bm.getClass.getMethod("diskBlockManager").invoke(bm)
val file = dbm.getClass.getMethod("getFile", classOf[String]).invoke(dbm, s"rdd_${d.id}_0").asInstanceOf[java.io.File]
val p = file.getPath.split("/")
println(s"  rdd_${d.id}_0 存放在：.../${p.takeRight(3).mkString("/")}")
println(s"  即 <spark.local.dir>/blockmgr-<UUID>/<两位十六进制子目录>/<块名>，文件存在：${file.exists}")
System.exit(0)
