# 配图

小红书轮播图（1080×1440，3:4），由 `render.py` 根据 `slides/sXX.py` 生成。

| 目录 | 内容 | 张数 |
|---|---|---|
| `out/00/` | 开篇：我为什么要冲击 Apache Spark Committer | 8 |
| `out/01/` | 源码通关 01：编译踩坑全记录 | 9 |
| `out/02/` | 面试拆解 01：Job、Stage、Task | 9 |
| `out/03/` | 宽窄依赖：Stage 到底在哪里切开 | 9 |
| `out/04/` | Committer 之路 01：我给 Apache Spark 提的第一个 PR | 9 |

## 修改与重新生成

1. 修改 `slides/sXX.py` 中的文字（支持 `**加粗高亮**` 和 `` `代码` ``）
2. 重新生成：

```bash
python3 images/render.py          # 全部
python3 images/render.py 03       # 只生成第 03 篇
```

依赖：macOS + Google Chrome（使用无头模式截图）、Python 3。
内容放不下时会自动缩小字号；脚本输出里出现 ⚠️ 表示该页内容偏多，建议精简。

## 说明

- 封面是纯文字版。真人照片版封面在小红书上通常效果更好，可以用这些封面的配色（深蓝 `#1F4E79` + 橙 `#E8711A`）自己做。
- 配图中引用的源码行号基于 Spark 4.2.0。
