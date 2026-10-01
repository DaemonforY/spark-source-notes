# 03 宽窄依赖：Stage 到底在哪里切开？—— 小红书配图


def _panel(ox, oy, title, sub, parents, children, edges, wide=False):
    """画一个依赖关系小图：parents / children 为 (中心x, 标签, 颜色)，edges 为 (父序号, 子序号)。"""
    py, cy, bw, bh = oy + 70, oy + 210, 64, 40
    color = "#E8711A" if wide else "#1F4E79"
    s = [f'<rect x="{ox}" y="{oy}" width="430" height="300" rx="16" fill="#fff" stroke="#E3E8EF" stroke-width="2"/>',
         f'<text x="{ox + 215}" y="{oy + 42}" text-anchor="middle" font-size="26" font-weight="700" fill="{color}">{title}</text>',
         f'<text x="{ox + 215}" y="{oy + 285}" text-anchor="middle" font-size="20" fill="#5B6878">{sub}</text>']
    for pi, ci in edges:
        x1, x2 = ox + parents[pi][0], ox + children[ci][0]
        s.append(f'<line x1="{x1}" y1="{py + bh}" x2="{x2}" y2="{cy}" stroke="{color}" stroke-width="3" opacity="0.85"/>')
    for (x, label, fill), y in [(p, py) for p in parents] + [(c, cy) for c in children]:
        s.append(f'<rect x="{ox + x - bw / 2}" y="{y}" width="{bw}" height="{bh}" rx="8" fill="{fill}"/>')
        s.append(f'<text x="{ox + x}" y="{y + 28}" text-anchor="middle" font-size="20" font-weight="600" fill="#fff">{label}</text>')
    return "".join(s)


_B, _G, _O = "#1F4E79", "#4A7BA7", "#C2410C"
DIAGRAM = (
    '<svg viewBox="0 0 900 640" width="100%" style="font-family:PingFang SC" xmlns="http://www.w3.org/2000/svg">'
    + _panel(0, 0, "一对一", "map / filter",
             [(95, "P0", _B), (215, "P1", _B), (335, "P2", _B)],
             [(95, "C0", _G), (215, "C1", _G), (335, "C2", _G)],
             [(0, 0), (1, 1), (2, 2)])
    + _panel(470, 0, "按区间", "union",
             [(65, "A0", _B), (165, "A1", _B), (265, "B0", "#6B7A8C"), (365, "B1", "#6B7A8C")],
             [(65, "C0", _G), (165, "C1", _G), (265, "C2", _G), (365, "C3", _G)],
             [(0, 0), (1, 1), (2, 2), (3, 3)])
    + _panel(0, 340, "多对一", "coalesce（仍是窄依赖）",
             [(65, "P0", _B), (165, "P1", _B), (265, "P2", _B), (365, "P3", _B)],
             [(115, "C0", _G), (315, "C1", _G)],
             [(0, 0), (1, 0), (2, 1), (3, 1)])
    + _panel(470, 340, "宽依赖", "按 key 打散 → Shuffle → 切 Stage",
             [(95, "P0", _O), (215, "P1", _O), (335, "P2", _O)],
             [(95, "C0", "#F59E0B"), (215, "C1", "#F59E0B"), (335, "C2", "#F59E0B")],
             [(i, j) for i in range(3) for j in range(3)], wide=True)
    + "</svg>"
)

SLIDES = [
    {"type": "cover",
     "title": "宽窄依赖\n**3 个误区**",
     "subtitle": "Spark 面试 / 性能优化必懂\n10 组实验验证",
     "badge": "基于 Spark 4.2.0"},

    {"type": "content",
     "heading": "先问你 3 个问题",
     "items": [
         "❓ 窄依赖就是“**一对一**”吗？",
         "❓ reduceByKey、join **一定**会 Shuffle 吗？",
         "❓ map 和 mapValues **只是写法不同**吗？",
     ],
     "note": "👉 答案都是：**不是**。往后翻，每一条都有实验和源码为证"},

    {"type": "html",
     "heading": "一张图看懂宽窄依赖",
     "html": DIAGRAM,
     "note": "判断标准：不看“一对几”，看**父分区的数据要不要拆开**分给多个子分区"},

    {"type": "content",
     "heading": "误区 ①：窄依赖 ≠ 一对一",
     "items": [
         "一对一：map、filter",
         "按区间：union（分区直接拼接）",
         "**多对一**：coalesce",
         "实验：coalesce(4→2) 后，**子分区 0 依赖父分区 0 和 1**",
         "依然只有 **1 个 Stage**",
     ],
     "code": "coalesce 子分区0 依赖的父分区: ArraySeq(0, 1)\n规划的Stage: S2(2t) | 实际执行: 1 个",
     "note": "父分区 0 的数据**整个**交给子分区 0，不用拆开，所以不需要 Shuffle"},

    {"type": "content",
     "heading": "误区 ②：reduceByKey 不一定 Shuffle",
     "items": [
         "数据**已按相同分区器分好区** → 直接在分区内聚合",
         "同一个 key 已经在同一个分区里了，不用再打散",
     ],
     "code": "// PairRDDFunctions.scala:92\nif (self.partitioner == Some(partitioner)) {\n  self.mapPartitions(...)    // **不 Shuffle**\n} else {\n  new ShuffledRDD(...)       // Shuffle\n}",
     "items_after": [
         "实验：先 partitionBy，再 reduceByKey",
         "  结果：**只 Shuffle 1 次**",
         "join 同理：预分区后 join，Stage **3 个 → 2 个**",
     ]},

    {"type": "content",
     "heading": "误区 ③：一个 map 多 Shuffle 一次 ⚠️",
     "code": "partitionBy → **map(identity)** → reduceByKey\n规划的Stage: S9 S10 S11   → **3 个 Stage**\n\npartitionBy → **mapValues** → reduceByKey\n规划的Stage: S12 S13      → 2 个 Stage",
     "items_after": [
         "`map(identity)` 什么都没改",
         "却**多了一次 Shuffle**！",
     ]},

    {"type": "content",
     "heading": "为什么？",
     "code": "// MapPartitionsRDD.scala:52\noverride val partitioner =\n  if (preservesPartitioning) firstParent[T].partitioner\n  else None",
     "items_after": [
         "map 可能**改 key** → Spark 无法判断 → **丢掉分区器**",
         "mapValues 保证**只改 value** → **保留分区器**",
     ],
     "note": "✅ 键值对只改 value 时，用 **mapValues / flatMapValues**"},

    {"type": "content",
     "heading": "小技巧：一眼看出 Stage 边界",
     "items": ["调用 `rdd.toDebugString`："],
     "code": "(3) ShuffledRDD[47] at reduceByKey\n **+-**(3) MapPartitionsRDD[46] at map\n    |  ShuffledRDD[45] at partitionBy\n    **+-**(4) MapPartitionsRDD[44] at map\n       |  ParallelCollectionRDD[43]",
     "items_after": [
         "每个 **`+-`** = 一个 Shuffle 边界 = Stage 切开处",
         "括号里的数字 = 分区数 = Task 数",
         "这里有 2 个 `+-` → **3 个 Stage**",
     ]},

    {"type": "card",
     "heading": "速记卡（建议收藏）",
     "card": """窄依赖：父分区数据不拆开 → 同 Stage
  · 一对一  map / filter
  · 区间   union
  · 多对一  coalesce
宽依赖：按 key 打散 → **切 Stage**

⭐ 窄依赖 ≠ 一对一
⭐ 分区器相同 → reduceByKey/join 不 Shuffle
⭐ **map 丢分区器，mapValues 保留**
⭐ toDebugString 的 +- 就是 Stage 边界""",
     "note": "关注 X老师读源码，一行一行读懂 Spark"},
]
