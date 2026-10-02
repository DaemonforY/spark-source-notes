# 实验 09：一条 SQL 在 Catalyst 中的四个阶段

```bash
./run-experiment.sh experiments/09-catalyst/catalyst.scala
```

打印同一条查询的四个计划（解析后、分析后、优化后、物理计划），并用 `queryExecution.tracker` 统计真正生效的规则。为了看清静态物理计划，关闭了 AQE。

| 阶段 | 观察到的变化 | 对应规则 / 机制 |
|---|---|---|
| ① 解析后 | 节点带 `'` 前缀，表、列、函数都未绑定；`10 + 20` 原样保留 | ANTLR 语法 + AstBuilder |
| ② 分析后 | 每列有唯一 ID（如 `age#34`），找到了 Parquet 表 | ResolveRelations / ResolveReferences / ResolveFunctions |
| ③ 优化后 | `10 + 20` → `30` | ConstantFolding |
| | Filter 被移到 Join 下面 | PushDownPredicates |
| | 新增 `isnotnull(id)`、`isnotnull(user_id)` | InferFiltersFromConstraints |
| | 只保留用得到的列 | ColumnPruning |
| | `SubqueryAlias`、`View` 消失 | FinishAnalysis（EliminateSubqueryAliases、EliminateView） |
| ④ 物理计划 | 过滤条件下推到 Parquet（`PushedFilters`），`ReadSchema` 中没有未使用的 `bio` 列 | 文件数据源下推 |
| | 小表广播：`BroadcastHashJoin` | JoinSelection（小于 10MB） |
| | 两阶段聚合 + `Exchange hashpartitioning(name, 200)` | Aggregation 策略 + EnsureRequirements |
| | `*(1)`、`*(2)`、`*(3)` | CollapseCodegenStages（全阶段代码生成） |
| 规则统计 | 被调用 242 条，真正生效 8 条 | — |

> 列 ID、`plan_id`、耗时每次运行会不同；`expected-output.txt` 中的临时目录路径已替换为 `<tmp>`。
