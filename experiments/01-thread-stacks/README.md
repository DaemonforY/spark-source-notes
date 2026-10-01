# 实验 01：Job 运行中的真实线程栈

在 Job 运行到一半时，抓取 JVM 中相关线程的调用栈。

```bash
./run-experiment.sh experiments/01-thread-stacks/thread-stacks.scala
```

## 你会看到

| 线程 | 正在做什么 |
|---|---|
| `main` | 卡在 `DAGScheduler.runJob` 里等待结果 |
| `dag-scheduler-event-loop` | 已处理完提交事件，处于空闲等待 |
| `Executor task launch worker ...` | 正在执行 `map` 函数（每个 Task 一个线程） |

**结论**：一个 Job 横跨多个线程，线程之间通过事件队列、RPC 消息、线程池交接，所以单看任何一个线程栈都是断的。

> 行号、Lambda 类名可能因运行环境略有不同，线程和调用层次应与 `expected-output.txt` 一致。
