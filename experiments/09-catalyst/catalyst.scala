// 实验：一条 SQL 在 Catalyst 中的四个阶段，以及哪些规则真正生效
// 对应讲义：第 8 讲《Spark SQL 与 Catalyst》
// 运行：SPARK_HOME=<Spark 源码目录> ./run-experiment.sh experiments/09-catalyst/catalyst.scala
// 说明：关闭 AQE，便于直接看到静态的物理计划（AQE 在第 10 讲讲）
spark.conf.set("spark.sql.adaptive.enabled", "false")
import spark.implicits._

// 准备两张 Parquet 表
val dir = java.nio.file.Files.createTempDirectory("catalyst").toString
// bio 列在查询中不会被用到，用来观察列裁剪是否作用到了数据读取层
(1 to 1000).map(i => (i, s"user_$i", i % 60, if (i % 3 == 0) "BJ" else "SH", "x" * 100)).toDF("id", "name", "age", "city", "bio")
  .write.parquet(s"$dir/people")
(1 to 200).map(i => (i % 100 + 1, i * 1.5)).toDF("user_id", "amount").write.parquet(s"$dir/orders")
spark.read.parquet(s"$dir/people").createOrReplaceTempView("people")
spark.read.parquet(s"$dir/orders").createOrReplaceTempView("orders")

val sql = """SELECT p.name, sum(o.amount) AS total
            |FROM people p JOIN orders o ON p.id = o.user_id
            |WHERE p.age > 10 + 20 AND p.city = 'BJ'
            |GROUP BY p.name""".stripMargin
println("### SQL\n" + sql)
val df = spark.sql(sql)
val qe = df.queryExecution
df.collect()

def show(title: String, plan: String) = { println(s"\n### $title"); println(plan.split("\n").map("  " + _).mkString("\n")) }
show("① 解析后（Unresolved Logical Plan）：表名、列名、函数都还没有绑定", qe.logical.treeString)
show("② 分析后（Analyzed Logical Plan）：绑定了表、列的类型和唯一 ID", qe.analyzed.treeString)
show("③ 优化后（Optimized Logical Plan）", qe.optimizedPlan.treeString)
show("④ 物理计划（Executed Physical Plan）", qe.executedPlan.treeString)

// 哪些规则真正改变了计划（numEffectiveInvocations > 0）
println("\n### 真正生效的规则（按所属模块分组）")
val effective = qe.tracker.rules.filter(_._2.numEffectiveInvocations > 0).keys.toSeq.sorted
def group(prefix: String) = effective.filter(_.startsWith(prefix)).map(_.split('.').last)
println("  分析器 analysis.* ：" + group("org.apache.spark.sql.catalyst.analysis").mkString(", "))
println("  优化器 optimizer.*：" + group("org.apache.spark.sql.catalyst.optimizer").mkString(", "))
val others = effective.filterNot(r => r.startsWith("org.apache.spark.sql.catalyst.analysis") || r.startsWith("org.apache.spark.sql.catalyst.optimizer"))
if (others.nonEmpty) println("  其他：" + others.map(_.split('.').last).mkString(", "))
println("  （物理计划的准备规则如 EnsureRequirements、CollapseCodegenStages 不经过 RuleExecutor，不会出现在这里）")
println(s"  规则总数：被调用过 ${qe.tracker.rules.size} 条，真正生效 ${effective.size} 条")

println("\n### 各阶段耗时")
qe.tracker.phases.toSeq.sortBy(_._2.startTimeMs).foreach { case (p, s) => println(f"  $p%-12s ${s.durationMs}%5d ms") }
System.exit(0)
