# 实验 08：容错机制

脚本会根据 master 选择要做的实验，三种 master 各跑一次：

```bash
MASTER="local[2]"                ./run-experiment.sh experiments/08-fault-tolerance/fault-tolerance.scala
MASTER="local[2,3]"              ./run-experiment.sh experiments/08-fault-tolerance/fault-tolerance.scala
MASTER="local-cluster[2,1,1024]" ./run-experiment.sh experiments/08-fault-tolerance/fault-tolerance.scala
```

| 实验 | 结果 | 结论 |
|---|---|---|
| A `local[2]` | 失败 2 次、成功 0 次，Job 失败 | `local[N]` 模式下 Task 不重试（`MAX_LOCAL_TASK_FAILURES = 1`） |
| A `local[2,3]` | 失败 4 次、成功 2 次，Job 成功 | `local[N,F]` 允许最多失败 F 次 |
| A 累加器 | = 4，只计入最后成功的那次尝试 | **失败尝试对累加器的更新会被丢弃** |
| B Executor 丢失 | 第二次 count 时 map Stage 只重算了丢失的分区 | 局部重算；重算几个分区取决于被杀 Executor 上有几个 map 输出，每次运行可能不同 |
| B 执行顺序 | reduce Stage 先跑 → FetchFailed → 重算 map Stage → reduce 重试 | 被杀的 Executor 当时**空闲**，DAGScheduler 没有立即收到通知，是通过 FetchFailed **被动发现**的（`TaskSchedulerImpl.executorLost` 只在 Executor 上有运行中的 Task 时才通知 DAGScheduler） |
| C Checkpoint | 血缘从 52 层变成 2 层；一次 count() 触发 2 个 Job | Checkpoint 在 Action 结束后**额外提交一个 Job** 写数据 |
| C cache | 不 cache 时源头数据算了 200 次，先 cache 只算 100 次 | **checkpoint 之前先 cache，避免重算** |

> `expected-output.txt` 中的主机名、IP、进程号已替换为占位符。
