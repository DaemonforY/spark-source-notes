# 实验 05：统一内存管理的内存划分与借用规则

```bash
MASTER="local[1]" ./run-experiment.sh experiments/05-memory/memory.scala --driver-memory 1g
```

> local 模式下 Driver 兼任 Executor，所以用 `--driver-memory` 控制堆内存。用 1 个线程，避免多个 Task 分摊内存，结果更容易对照。

## 实验 A：内存划分是否符合公式

`统一内存 = (Runtime.maxMemory - 300MB) × spark.memory.fraction(0.6)`，Storage 区域再 × `spark.memory.storageFraction(0.5)`。

| 配置 | JVM maxMemory | 公式 | 实际统一内存 | Storage 区域 |
|---|---|---|---|---|
| 1g | 1024.0 MB | 434.4 MB | 434.4 MB | 217.2 MB |
| 2g | 2048.0 MB | 1048.8 MB | 1048.8 MB | 524.4 MB |

## 实验 B：不对称的借用规则

| 步骤 | 现象 | 对应规则 |
|---|---|---|
| B1 缓存约 70% 统一内存的数据 | Storage 用到 304 MB，超过 217.2 MB 的 Storage 区域 | Storage 可以借 Execution 的**空闲**内存 |
| B2 运行一个大聚合 | 缓存从 38 个分区降到 27 个，Storage 回到 216 MB | Execution 可以驱逐缓存，**收回借出的部分** |
| B2（续） | Execution 峰值约 223 MB 后转为溢写 446.8 MB，不再继续驱逐 | 收回**只到 Storage 区域边界**，区域内的缓存受保护 |
