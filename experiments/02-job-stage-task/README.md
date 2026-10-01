# 实验 02：Job、Stage、Task 的关系

用 `SparkListener` 记录每个 Job 规划了哪些 Stage、每个 Stage 的 Task 数，以及实际执行了哪些 Stage。

```bash
./run-experiment.sh experiments/02-job-stage-task/job-stage-task.scala
```

| 用例 | 现象 | 说明 |
|---|---|---|
| 例 1 | 1 个 Stage | 只有窄依赖 |
| 例 2 | 2 个 Stage，Task 数 4 和 3 | 每个 Stage 的 Task 数由各自的分区数决定 |
| 例 3 ⭐ | 上游 Stage 未执行（skipped） | Shuffle 输出已存在，直接复用 |
| 例 4 | 3 个 Stage | 两次 Shuffle |
| 例 5 ⭐ | `take(1)` 只有 1 个 Task | Task 数 = 需要计算的分区数 |
| 例 6 ⭐ | `sortByKey` 未调用 Action 却出现 Job | `RangePartitioner` 抽样时调用了 `collect()` |
| 例 7 | 一个 Stage 有两个父 Stage | Stage 之间构成 DAG |
