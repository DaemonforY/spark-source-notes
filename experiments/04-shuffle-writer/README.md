# 实验 04：三种 Shuffle Writer 的选择条件

直接打印 `ShuffleDependency.shuffleHandle` 的类型，观察 `SortShuffleManager.registerShuffle` 选择了哪种 Writer。

```bash
./run-experiment.sh experiments/04-shuffle-writer/shuffle-writer.scala          # 默认 Java 序列化器
./run-experiment.sh experiments/04-shuffle-writer/shuffle-writer.scala --kryo   # Kryo 序列化器
```

| Handle | 对应的 Writer |
|---|---|
| `BypassMergeSortShuffleHandle` | `BypassMergeSortShuffleWriter` |
| `SerializedShuffleHandle` | `UnsafeShuffleWriter`（tungsten-sort） |
| `BaseShuffleHandle` | `SortShuffleWriter` |

## 三个反直觉的发现

| 用例 | 现象 | 原因 |
|---|---|---|
| 3 ⭐ | 没配 Kryo，K/V 都是 Int 时也走了 tungsten-sort，序列化器显示为 `KryoSerializer` | `SerializerManager.getSerializer`：K 和 V 都是基本类型 / String 时自动使用 Kryo |
| 6 ⭐ | value 是自定义类时，默认配置下只能走 `SortShuffleWriter`；换成 Kryo 后才走 tungsten-sort | `JavaSerializer` 不支持重定位（`supportsRelocationOfSerializedObjects = false`） |
| 8、10 ⭐ | Spark SQL 默认配置下走的是 Bypass，不是 tungsten-sort | `spark.sql.shuffle.partitions` 默认 200，正好等于 `spark.shuffle.sort.bypassMergeThreshold` 默认值 200 |

> SQL 部分关闭了 AQE，便于直接在执行计划里找到 `ShuffleExchangeExec`。
