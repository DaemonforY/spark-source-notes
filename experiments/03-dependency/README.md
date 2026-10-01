# 实验 03：宽窄依赖与 Stage 切分

打印每种算子产生的依赖类型、分区器，以及 Job 规划的 Stage 数。

```bash
./run-experiment.sh experiments/03-dependency/dependency.scala
```

| 用例 | 依赖 | Stage 数 | 说明 |
|---|---|---|---|
| 1 map + filter | OneToOneDependency | 1 | 一对一 |
| 2 union | RangeDependency | 1 | 分区拼接 |
| 3 coalesce(4→2) ⭐ | 匿名 NarrowDependency | 1 | 多对一，仍是窄依赖 |
| 4 repartition(2) | — | 2 | `coalesce(n, shuffle = true)` |
| 5 reduceByKey(3) | ShuffleDependency | 2 | 普通情况 |
| 6a partitionBy → reduceByKey ⭐ | OneToOneDependency | 2 | 分区器相同，reduceByKey 不 Shuffle |
| 6b partitionBy → map → reduceByKey ⭐ | ShuffleDependency | 3 | `map` 丢失分区器，多一次 Shuffle |
| 6c partitionBy → mapValues → reduceByKey | OneToOneDependency | 2 | `mapValues` 保留分区器 |
| 7a 普通 join | — | 3 | 两侧都要 Shuffle |
| 7b 预分区后 join | — | 2 | join 本身不 Shuffle |
