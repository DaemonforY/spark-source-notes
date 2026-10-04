# 04 Committer 之路 01：我给 Apache Spark 提的第一个 PR —— 小红书配图
# 第 8 张为"进展"页，PR 有新结果后请更新这里的内容再重新渲染

SLIDES = [
    {"type": "cover",
     "title": "Apache\u00a0Spark\n我的第一个 **PR**",
     "subtitle": "只改了一个单词\n这个笔误藏了 11 年",
     "badge": "Committer 之路 01"},

    {"type": "content",
     "heading": "在哪发现的",
     "items": ["读 Shuffle 源码时，`PackedRecordPointer` 的注释写着："],
     "code": "This implies that the maximum addressable\npage size is 2^27 **bits** = 128 megabytes",
     "items_after": [
         "2^27 **位** = 16 MB",
         "2^27 **字节** = 128 MB",
         "等号两边对不上 → 应该是 **bytes**",
     ]},

    {"type": "content",
     "heading": "先用源码证明它真的错了",
     "items": [
         "① 紧挨着的常量：`MAXIMUM_PAGE_SIZE_BYTES = 1 << 27`，单位就是**字节**",
         "② 注释说偏移量\"**不按 8 字节对齐**\"，即按字节寻址",
         "③ 注释说按字对齐时最大 **1 GB**：2^27 × 8 字节 = 1 GB",
     ],
     "note": "查了提交历史：这一行写于 **2015 年 5 月**，此后 **11 年**没改过"},

    {"type": "content",
     "heading": "提 PR 之前，先做 4 件事",
     "items": [
         "① 最新 **master** 上还是错的吗？→ 是",
         "② **查重**：GitHub PR + JIRA 都搜一遍 → 没人提过",
         "③ 全库搜**同类错误** → 只有这一处",
         "④ 读**贡献指南**：",
         "  小改动**不需要建 JIRA**",
         "  标题用 `[MINOR][CORE]` 开头",
     ],
     "note": "查重是对社区最基本的尊重：重复的 PR 会浪费 Reviewer 的时间"},

    {"type": "content",
     "heading": "动手：改一个单词",
     "code": "- ... page size is 2^27 **bits** = 128 megabytes\n+ ... page size is 2^27 **bytes** = 128 megabytes",
     "items_after": [
         "改完这一行 **99** 个字符，没超过规范的 100",
         "用 `git worktree` 另开目录，不打扰学习环境",
         "署名用**真名** + GitHub **隐私邮箱**",
     ],
     "note": "提交的作者信息会**永久**留在 Spark 的历史里"},

    {"type": "content",
     "heading": "踩坑：fork 的 Actions 默认是关的",
     "items": [
         "Spark 的 CI 跑在**你自己 fork 的仓库**里",
         "GitHub 出于安全考虑，fork 里的工作流**默认不运行**",
         "要去 Actions 页面手动点：",
         "  **I understand my workflows, go ahead and enable them**",
         "😅 我第一次找不到按钮：浏览器**没登录**",
         "启用后还要**重新推送一次**才会触发构建",
     ]},

    {"type": "content",
     "heading": "PR 模板里的 AI 声明",
     "items": [
         "Spark 的 PR 模板专门问：是否用了**生成式 AI 工具**？",
         "按 ASF 指引写上 `Generated-by:`",
         "我读源码、整理材料用了 AI 辅助，所以**如实声明**",
     ],
     "note": "**用 AI 辅助不丢人，隐瞒才丢人。** 每一处改动我都亲自用源码验证过"},

    {"type": "content",
     "heading": "目前的进展（持续更新）",
     "items": [
         "✅ PR 已提交，自动检查通过",
         "✅ 几小时内收到**自动审查**：**0 个问题**",
         "⚠️ 一个和改动无关的 Python 测试**偶发失败**（CI 网络问题），已重跑",
         "⏳ 等待 Committer 合并",
     ],
     "note": "结果出来后更新，关注不迷路"},

    {"type": "card",
     "heading": "第一个 PR 流程（收藏）",
     "card": """发现问题 → 用源码验证
→ 确认最新 master 仍存在
→ 查重（GitHub + JIRA）
→ 读贡献指南
→ 基于 master 修改 → 过代码规范
→ 设置提交身份（真名 + 隐私邮箱）
→ fork → **启用 Actions** → 推送
→ 按模板写 PR（含 **AI 声明**）
→ 提交 → 等待 Review""",
     "note": "你第一次提 PR 踩过什么坑？评论区聊聊"},
]
