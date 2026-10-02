# X老师读源码 · Spark 源码笔记

> 一个大数据老师冲击 Apache Spark Committer 的公开学习记录。
> 所有内容基于 **Apache Spark 4.2.0**，每个结论都尽量给出源码位置和可复现的实验。

本仓库是「X老师读源码」系列文章 / 视频的配套代码和资料。

## 📚 文章目录

| # | 文章 | 配套实验 |
|---|---|---|
| 00 | 开篇：我为什么要冲击 Apache Spark Committer | — |
| 01 | 源码通关 01｜从零编译 Spark 4.2.0 源码，国内踩坑全记录 | [`setup/`](setup/) |
| 02 | 宽窄依赖：Stage 到底在哪里切开？ | [`experiments/03-dependency`](experiments/03-dependency/) |
| 03 | 面试拆解 01｜Job、Stage、Task 到底是什么关系？ | [`experiments/02-job-stage-task`](experiments/02-job-stage-task/) |
| — | L1 练习：27 个断点跟踪一个 Job 的一生 | [`labs/`](labs/)、[`experiments/01-thread-stacks`](experiments/01-thread-stacks/) |
| — | 第 3 讲：Shuffle 原理（三种 Writer 的选择条件） | [`experiments/04-shuffle-writer`](experiments/04-shuffle-writer/) |
| — | 第 4 讲：内存管理（内存划分与借用规则） | [`experiments/05-memory`](experiments/05-memory/) |
| — | 第 5 讲：存储体系（存储级别、cache、广播分块） | [`experiments/06-storage`](experiments/06-storage/) |

<!-- 文章发布后，把标题替换成各平台的文章链接 -->

## 🚀 快速开始

### 1. 准备环境

- JDK 17（Spark 4.2.0 官方支持 17 / 21 / 25）
- Git
- 磁盘预留 15 GB 以上，内存建议 16 GB 以上

### 2. 拉取并编译 Spark 源码

```bash
git clone --filter=blob:none https://github.com/apache/spark.git
cd spark
git checkout -b learn-v4.2.0 v4.2.0
export SPARK_HOME=$(pwd)
cd -

git clone <本仓库地址> spark-source-notes
cd spark-source-notes
./setup/build-spark.sh
```

编译被中断时，从中断的模块继续（以 `spark-sql_2.13` 为例）：

```bash
./setup/resume-build.sh spark-sql_2.13
```

详细说明和踩坑记录见文章 01。

### 3. 运行实验

```bash
./run-experiment.sh experiments/02-job-stage-task/job-stage-task.scala

# 指定线程数、透传 spark-shell 参数
MASTER="local[1]" ./run-experiment.sh experiments/05-memory/memory.scala --driver-memory 1g
```

每个实验目录下都有 `expected-output.txt`，可以对照自己的运行结果。

## 📁 目录结构

```
spark-source-notes/
├── setup/                 编译与调试脚本
│   ├── aliyun-settings.xml    阿里云 Maven 镜像（只给 Spark 用，不改全局配置）
│   ├── build-spark.sh         完整编译
│   ├── resume-build.sh        断点续编
│   └── debug-sparkpi.sh       以远程调试模式启动 SparkPi
├── experiments/           可复现的实验（spark-shell 脚本 + 预期输出）
│   ├── 01-thread-stacks/      Job 运行中的真实线程栈
│   ├── 02-job-stage-task/     Job / Stage / Task 的关系
│   ├── 03-dependency/         宽窄依赖与 Stage 切分
│   ├── 04-shuffle-writer/     三种 Shuffle Writer 的选择条件
│   ├── 05-memory/             统一内存管理的划分与借用
│   └── 06-storage/            存储级别、cache 默认级别、广播分块
├── labs/                  动手练习（IDEA 断点调试指南）
├── roadmap/               从入门到 Committer 的学习路线
├── images/                文章配图
└── run-experiment.sh      实验运行脚本
```

## ✅ 内容承诺

1. **版本锁定**：所有内容标注基于的 Spark 版本，目前是 4.2.0
2. **源码为证**：结论尽量给出 `文件:行号`
3. **运行为证**：关键结论配有可复现的实验
4. **区分事实与观点**：源码行为是事实；"为什么这样设计"如果是推测会明确说明
5. **公开勘误**：发现错误欢迎提 Issue，确认后会更正并致谢

## 🐛 勘误与反馈

发现错误或有疑问，欢迎提 [Issue](../../issues)。

## 📄 许可

本仓库采用**双许可**：

| 内容 | 许可证 | 文件 |
|---|---|---|
| **代码**：`setup/`、`experiments/`、`images/render.py`、`images/slides/`、`*.sh` | [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0) | [`LICENSE`](LICENSE) |
| **文字与图片**：文章、`labs/`、`roadmap/`、`images/out/` 及其他文档 | [CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/deed.zh-hans)（署名-非商业性使用-相同方式共享） | [`LICENSE-CONTENT`](LICENSE-CONTENT) |

- 代码可以自由使用、修改，包括商业用途，保留版权和许可声明即可
- 文字和图片转载请**注明出处「X老师读源码」**，不得用于商业用途，改编后须以相同许可发布

本仓库中引用的 Apache Spark 源码片段，版权归 Apache 软件基金会所有，遵循 [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)。

*Apache Spark 和 Spark 是 Apache 软件基金会的商标。本仓库为个人学习记录，与 Apache 软件基金会无关，也未获得其背书。*
