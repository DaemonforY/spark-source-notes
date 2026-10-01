// 实验：在 Job 运行中途抓取线程栈，观察一个 Job 横跨哪些线程
// 运行：$SPARK_HOME/bin/spark-shell --master "local[2]" -I thread-stacks.scala
import scala.jdk.CollectionConverters._

// 后台线程：4 秒后（Job 正在运行时）打印相关线程的调用栈，只保留 Spark 和用户代码的帧
new Thread(() => {
  Thread.sleep(4000)
  val keep = Set("main", "dag-scheduler-event-loop")
  Thread.getAllStackTraces.asScala.foreach { case (t, st) =>
    if (keep(t.getName) || t.getName.startsWith("Executor task launch")) {
      println(s"\n===== THREAD: ${t.getName} =====")
      st.filter(f => f.getClassName.startsWith("org.apache.spark") || f.getClassName.contains("$line"))
        .take(25).foreach(f => println("  at " + f))
    }
  }
}).start()

// 与官方 SparkPi 示例结构相同，只是让每个 Task 多睡 8 秒，方便抓栈
val count = sc.parallelize(1 until 200000, 2).map { i =>
  if (i == 1 || i == 100000) Thread.sleep(8000)
  val x = math.random() * 2 - 1
  val y = math.random() * 2 - 1
  if (x * x + y * y <= 1) 1 else 0
}.reduce(_ + _)
println(s"\nPi is roughly ${4.0 * count / 199999}")
System.exit(0)
