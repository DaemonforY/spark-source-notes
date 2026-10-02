# 实验 11：AQE、DataSource V2 与 Spark Connect

## 一、AQE 与 DataSource V2

```bash
./run-experiment.sh experiments/11-aqe-dsv2-connect/aqe-dsv2.scala --driver-memory 2g
```

| 实验 | 结果 | 结论 |
|---|---|---|
| A 合并分区 | AQE 关闭：最后一个 Stage 200 个 Task；开启：1 个（`AQEShuffleRead coalesced`） | 小数据时 AQE 把大量空分区合并 |
| B 动态切换 Join | 初始计划 `SortMergeJoin` → 最终计划 `BroadcastHashJoin` | 运行时拿到真实大小后**重新生成物理计划**（`reOptimize`），由普通的 `JoinSelection` 选中广播；不是 `DynamicJoinSelection` 做的 |
| C 倾斜 Join | 开启后 Join 阶段 Task 数 20 → 23，计划出现 `SortMergeJoin(skew=true)`、`AQEShuffleRead skewed` | 倾斜分区被拆成多份 |
| D V1 vs V2 | V1：`FileScan`，无聚合下推；V2：`BatchScan`，`PushedAggregation: [MAX(id), MIN(id), COUNT(*)]` | V2 支持聚合下推；**内置 Parquet 默认仍走 V1**（`spark.sql.sources.useV1SourceList`） |

预期输出：[`aqe-dsv2.expected-output.txt`](aqe-dsv2.expected-output.txt)

## 二、Spark Connect

需要先在本机启动一个 Connect 服务（**只监听 127.0.0.1**），做完实验后停止：

```bash
# 启动（服务端用 local[2]）
$SPARK_HOME/sbin/start-connect-server.sh --master "local[2]" --conf spark.connect.grpc.binding.address=127.0.0.1

# 运行客户端（客户端所在的 spark-shell 用 local[1]）
MASTER="local[1]" ./run-experiment.sh experiments/11-aqe-dsv2-connect/connect-client.scala

# 停止
$SPARK_HOME/sbin/stop-connect-server.sh
```

| 实验 | 结论 |
|---|---|
| E1 | 经典会话是 `classic.SparkSession`，Connect 会话是 `connect.SparkSession` |
| E2 | 通过 Connect 执行查询，结果正确 |
| E3 | Connect 会话上访问 `sparkContext` 报 `UNSUPPORTED_CONNECT_FEATURE.SESSION_SPARK_CONTEXT` |
| E4 | 客户端发出的是 protobuf 未解析计划，过滤条件仍是字符串 `"id > 50"` |
| E5 | 物理计划 `splits=2` 来自服务端的 `local[2]`，说明查询在服务端执行 |

> `spark-shell --remote sc://...` 启动的 Connect REPL 基于 Ammonite，需要真实终端；这里改为在经典 spark-shell 中用代码创建 Connect 客户端会话。

预期输出：[`connect-client.expected-output.txt`](connect-client.expected-output.txt)（临时目录路径已替换为占位符）
