# 02 面试拆解 01：Job、Stage、Task 到底是什么关系？—— 小红书配图


def _tasks(n, color):
    return "".join(
        f'<span style="display:inline-block;width:2.3em;padding:0.2em 0;margin:0.15em;border-radius:0.25em;'
        f'background:{color};color:#fff;font-size:0.62em;text-align:center;font-weight:600">T{i}</span>'
        for i in range(n))


def _box(title, inner, border, bg, pad="0.6em"):
    return (f'<div style="border:0.08em solid {border};background:{bg};border-radius:0.4em;padding:{pad};margin-top:0.4em">'
            f'<div style="font-size:0.72em;font-weight:700;color:{border};margin-bottom:0.3em">{title}</div>{inner}</div>')


_stage0 = _box("Stage 0 · ShuffleMapStage", _tasks(4, "#4A7BA7"), "#4A7BA7", "#fff")
_stage1 = _box("Stage 1 · ResultStage", _tasks(3, "#E8711A"), "#E8711A", "#fff")
_arrow = ('<div style="display:flex;align-items:center;justify-content:center;font-size:0.6em;color:#C2410C;'
          'font-weight:700;padding:0 0.3em;white-space:nowrap;writing-mode:vertical-rl">Shuffle →</div>')
_stages = f'<div style="display:flex;align-items:stretch">{_stage0}{_arrow}{_stage1}</div>'
_job = _box("Job · 由 Action 触发（如 collect）", _stages, "#1F4E79", "#F3F6FA")
DIAGRAM = _box("Application · 一次 spark-submit", _job + _box("Job …", "", "#8A94A3", "#F3F6FA", "0.4em 0.6em"),
               "#1E2A3A", "#FBF8F3")

SLIDES = [
    {"type": "cover",
     "title": "Job / Stage / Task\n**的关系**",
     "subtitle": "Spark 面试必问\n7 个实验 · 源码级答案",
     "badge": "面试拆解 01"},

    {"type": "content",
     "heading": "面试官：说说 Job、Stage、Task 的关系",
     "items": [
         "🙂 及格回答：一个 Action 一个 Job，宽依赖切 Stage，一个分区一个 Task",
         "😰 面试官追问：",
         "  一个 Action **一定**只有一个 Job 吗？",
         "  Stage 从前往后切，还是**从后往前**？",
         "  Task 数**一定**等于分区数吗？",
         "  再执行一次 Action，Stage 会重算吗？",
     ]},

    {"type": "html",
     "heading": "三层关系一张图",
     "html": DIAGRAM,
     "note": "Job ← Action 调用 `sc.runJob`　Stage ← 宽依赖切分　Task ← 每个**需要计算的分区**一个"},

    {"type": "content",
     "heading": "反直觉 ①：sortByKey 会触发 Job 😲",
     "items": [
         "sortByKey 明明是**转换算子**",
         "实验：没调用任何 Action，却出现了 1 个 Job",
     ],
     "code": "Job5: Stage9(4 tasks, sortByKey)",
     "items_after": [
         "原因：RangePartitioner 要先**抽样**，确定每个分区的边界",
         "抽样里调用了 `.collect()` → **触发 Job**",
         "| 源码：`Partitioner.scala:345`",
     ]},

    {"type": "content",
     "heading": "反直觉 ②：take(1)\u00a0只有\u00a01\u00a0个\u00a0Task",
     "items": ["RDD 有 4 个分区，take(1) 只启动 **1 个 Task**"],
     "code": "Job4: Stage8(**1 tasks**, take)",
     "items_after": [
         "take 先扫 **1 个分区**，够了就返回",
         "不够再扩大范围，**每次扩大都提交新 Job**",
         "👉 Task 数 = **需要计算的**分区数",
     ]},

    {"type": "content",
     "heading": "反直觉 ③：Stage 会被跳过",
     "items": ["同一个 RDD 第二次执行 Action："],
     "code": "Job2: Stage3(4 tasks) | Stage4(3 tasks)\n  -> 实际执行 Stage4: 3 tasks\n     **Stage3 没有执行！**",
     "items_after": [
         "上游 Stage 显示 **skipped**",
         "原因：上次 Shuffle 的输出还在，直接复用",
         "👉 Shuffle 输出相当于一种**隐式缓存**",
     ]},

    {"type": "content",
     "heading": "Stage 怎么切？",
     "items": [
         "**从后往前切**：从最后一个 RDD 出发，反向遍历依赖",
         "  遇到窄依赖 → 合并到当前 Stage",
         "  遇到宽依赖 → 切开，创建新 Stage",
         "**从前往后跑**：先提交父 Stage，完成后再提交子 Stage",
         "join 的下游 Stage 有**两个父 Stage**",
     ],
     "note": "所以 Stage 之间是 **DAG（有向无环图）**，不是一条直线"},

    {"type": "content",
     "heading": "满分回答（背这段）",
     "items": [
         "**基本关系**：Action → Job；宽依赖切 Stage；每个要计算的分区一个 Task",
         "**切分方式**：从后往前切，从前往后执行，Stage 构成 DAG",
         "**加分细节**：",
         "  take 可能多次提交 Job；sortByKey 抽样会触发 Job",
         "  take(1) 可能只有 1 个 Task",
         "  Shuffle 输出还在时，Stage 会被跳过",
     ]},

    {"type": "card",
     "heading": "速记卡（建议收藏）",
     "card": """Job   ← Action 调用 sc.runJob
Stage ← 宽依赖切分
        后往前切，前往后跑
Task  ← 每个“需要计算的分区”一个

⭐ 一个 Action ≠ 一定一个 Job
⭐ Task 数 ≠ 一定等于分区数
⭐ **Shuffle 输出在 → Stage skipped**
⭐ Stage 之间是 DAG""",
     "note": "你还被问过哪些 Spark 面试题？评论区告诉我"},
]
