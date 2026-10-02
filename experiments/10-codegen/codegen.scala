// 实验：全阶段代码生成与 Tungsten
// 对应讲义：第 9 讲《代码生成与 Tungsten》
// 运行（用 1 个线程，性能对比更稳定）：
//   SPARK_HOME=<Spark 源码目录> MASTER="local[1]" ./run-experiment.sh experiments/10-codegen/codegen.scala --driver-memory 2g
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.expressions.{GenericInternalRow, UnsafeProjection, UnsafeRow}
import org.apache.spark.sql.execution.debug._
import org.apache.spark.sql.types._
import org.apache.spark.unsafe.types.UTF8String

spark.conf.set("spark.sql.adaptive.enabled", "false")

// ---------- A. 生成的代码长什么样 ----------
println("### A. range → filter → project 生成的代码")
val dfA = spark.range(0, 1000).filter("id % 3 = 0").selectExpr("id * 2 AS x")
println("  物理计划：")
dfA.queryExecution.executedPlan.treeString.split("\n").foreach(l => println("    " + l))
val stages = codegenStringSeq(dfA.queryExecution.executedPlan)
println(s"  代码生成阶段数：${stages.size}")
val code = stages.head._2
val out = new java.io.File("experiments/10-codegen/generated-code.java")
if (out.getParentFile.exists) { val w = new java.io.PrintWriter(out); w.write(code); w.close() }
println(s"  生成的 Java 代码共 ${code.split("\n").length} 行（完整代码已写入 experiments/10-codegen/generated-code.java）")
println("  processNext() 中的核心循环（节选）：")
val lines = code.split("\n")
val start = lines.indexWhere(_.contains("protected void processNext()"))
lines.slice(start, start + 45).filter(_.trim.nonEmpty).foreach(l => println("    " + l))

// ---------- B. 性能对比 ----------
println("### B. 性能对比：sum(id * 2) WHERE id % 3 = 0，id 从 0 到 3 亿")
def query() = spark.range(0, 300000000L).filter("id % 3 = 0").selectExpr("sum(id * 2)").collect()(0).getLong(0)
def bench(label: String, conf: Map[String, String]): Unit = {
  conf.foreach { case (k, v) => spark.conf.set(k, v) }
  query()                                                    // 预热
  val ts = (1 to 3).map { _ => val t = System.nanoTime; val r = query(); ((System.nanoTime - t) / 1e6, r) }
  val ms = ts.map(_._1).sorted
  println(f"  $label%-44s 中位数 ${ms(1)}%8.0f ms  （3 次：${ms.map(m => f"$m%.0f").mkString(", ")}）结果 ${ts.head._2}")
}
bench("① 全阶段代码生成（默认）",                  Map("spark.sql.codegen.wholeStage" -> "true",  "spark.sql.codegen.factoryMode" -> "FALLBACK"))
bench("② 关闭全阶段代码生成（表达式仍单独代码生成）", Map("spark.sql.codegen.wholeStage" -> "false", "spark.sql.codegen.factoryMode" -> "FALLBACK"))
bench("③ 全部关闭（纯解释执行，火山模型）",          Map("spark.sql.codegen.wholeStage" -> "false", "spark.sql.codegen.factoryMode" -> "NO_CODEGEN"))
spark.conf.set("spark.sql.codegen.wholeStage", "true"); spark.conf.set("spark.sql.codegen.factoryMode", "FALLBACK")

// ---------- C. 字段太多时回退 ----------
println("### C. 输出字段数超过 spark.sql.codegen.maxFields（默认 100）时不做全阶段代码生成")
for (n <- Seq(50, 120)) {
  val df = spark.range(10).selectExpr((1 to n).map(i => s"id + $i AS c$i"): _*)
  val top = df.queryExecution.executedPlan.treeString.split("\n").head
  println(f"  $n%3d 列：${top.take(60)}...   ${if (top.trim.startsWith("*")) "← 有 *，参与了全阶段代码生成" else "← 没有 *，回退为普通执行"}")
}

// ---------- D. UnsafeRow 的内存布局 ----------
println("### D. UnsafeRow 的二进制布局：[null 位图][每个字段 8 字节][变长数据]")
val proj = UnsafeProjection.create(Array[DataType](IntegerType, LongType, StringType))
def dump(values: Array[Any]): Unit = {
  val row: UnsafeRow = proj(new GenericInternalRow(values))
  val bytes = row.getBytes
  println(s"  输入 ${values.map(v => if (v == null) "null" else v.toString).mkString("(", ", ", ")")}：共 ${row.getSizeInBytes} 字节")
  val names = Seq("null 位图", "字段 0 (int)", "字段 1 (long)", "字段 2 (string：偏移 << 32 | 长度)", "变长区：字符串内容")
  bytes.grouped(8).zipWithIndex.foreach { case (w, i) =>
    val hex = w.reverse.map(b => f"$b%02x").mkString          // 小端存储，倒过来按数值读
    println(f"    word $i%d  0x$hex   ${names.lift(i).getOrElse("")}")
  }
}
dump(Array[Any](7, 100L, UTF8String.fromString("spark")))
dump(Array[Any](null, 100L, UTF8String.fromString("spark")))
System.exit(0)
