# 实验 10：全阶段代码生成与 Tungsten

```bash
MASTER="local[1]" ./run-experiment.sh experiments/10-codegen/codegen.scala --driver-memory 2g
```

| 实验 | 结果 | 结论 |
|---|---|---|
| A 生成的代码 | range → filter → project 只有 1 个代码生成阶段，生成 142 行 Java；三个算子被融合进**同一个 for 循环** | 全阶段代码生成消除了算子之间的迭代器调用。完整代码见 [`generated-code.java`](generated-code.java) |
| B 性能（3 亿行，单线程，3 次取中位数） | ① 全阶段代码生成 **347 ms**；② 关闭全阶段代码生成 **5943 ms**（约 17 倍）；③ 纯解释执行 **25999 ms**（约 75 倍） | 具体数字取决于机器，倍数关系才是重点 |
| C 字段数上限 | 50 列有 `*`，120 列没有 `*` | 输出字段超过 `spark.sql.codegen.maxFields`（默认 100）时不做全阶段代码生成 |
| D UnsafeRow 布局 | `(7, 100, "spark")` 共 40 字节：null 位图 / int / long / `偏移 32 << 32 \| 长度 5` / 字符串字节 | 与 `UnsafeRow.java` 的注释完全一致；字段为 null 时位图对应位为 1 |

> 生成的代码中有 `if (3L == 0) throw remainderByZeroError`：4.0 起 ANSI 模式默认开启，除零会报错而不是返回 null。
