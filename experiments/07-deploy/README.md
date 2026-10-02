# 实验 07：RPC 端点、Executor 独立进程、动态资源分配

使用 `local-cluster` 模式：在本机启动一个真正的 Standalone 集群，**每个 Executor 都是独立的 JVM 进程**，可以观察到真实的 RPC 交互。

```bash
MASTER="local-cluster[4,1,1024]" ./run-experiment.sh experiments/07-deploy/deploy.scala \
  --conf spark.dynamicAllocation.enabled=true \
  --conf spark.dynamicAllocation.initialExecutors=0 \
  --conf spark.dynamicAllocation.executorIdleTimeout=5s
```

> `local-cluster[N,cores,memMB]` 需要已经编译好的 Spark（会使用 `assembly` 下的 jar），仅用于测试。

| 实验 | 结论 |
|---|---|
| A RPC 端点 | Driver 上注册了 `CoarseGrainedScheduler`、`HeartbeatReceiver`、`BlockManagerMaster`、`MapOutputTracker` 等端点，前几讲的组件都通过它们通信 |
| C 独立进程 | 16 个 Task 分布在 4 个与 Driver 不同的 JVM 进程中 |
| D 扩容 | 目标 Executor 数 0 → 1 → 3 → 7 → 15 → 16：每轮新增 1、2、4、8，约每秒一轮；封顶 16 = 16 个 Task ÷ 每个 Executor 1 核 |
| D 实际 | 实际只启动 4 个 Executor：目标只是期望，受集群资源（4 个 Worker）限制 |
| E 缩容 | Job 结束后，Executor 在空闲超时后被 Driver 移除，回到 0 个 |

> 时间、进程号、端口每次运行都不同；`expected-output.txt` 中的主机名、IP、进程号已替换为占位符。
