# 实验 06：存储体系

```bash
./run-experiment.sh experiments/06-storage/storage.scala --driver-memory 2g
```

| 实验 | 结论 |
|---|---|
| A 存储级别 | 100 万个对象：`MEMORY_ONLY` 87.7 MB，`MEMORY_ONLY_SER` 39.0 MB（默认 Java 序列化），相差 2.25 倍 |
| B cache() 默认级别 | `RDD.cache()` = `MEMORY_ONLY`；`Dataset.cache()` = `MEMORY_AND_DISK`，可由 `spark.sql.defaultCacheStorageLevel`（4.0.0+）修改 |
| C 缓存命中 | 第二次 Action 时 map 函数执行 0 次 |
| D 广播分块 | 10MB 随机字节按 `spark.broadcast.blockSize=4m` 切成 3 块 |
| E 磁盘目录 | `<本地目录>/blockmgr-<UUID>/<两位十六进制子目录>/<块名>` |

> 每次运行时 RDD 编号、广播编号、`blockmgr-<UUID>` 会不同；大小和块数应与 `expected-output.txt` 一致。
