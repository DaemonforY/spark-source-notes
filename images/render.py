#!/usr/bin/env python3
"""
小红书配图渲染器：读取 slides/sXX.py 中的 SLIDES 定义，生成 HTML，再用 Chrome 无头模式截图为 PNG。

用法：
    python3 images/render.py          # 生成全部
    python3 images/render.py 03       # 只生成 s03.py

幻灯片类型（每页一个 dict）：
    {"type": "cover", "title": ..., "subtitle": ..., "badge": ...}
    {"type": "content", "heading": ..., "items": [...], "code": ..., "note": ...}
    {"type": "html", "heading": ..., "html": ..., "note": ...}       # 自定义图示
    {"type": "card", "heading": ..., "card": ..., "note": ...}       # 速记卡 / 清单
    {"type": "cta", "title": ..., "subtitle": ...}                   # 互动页

文字中可使用：**加粗高亮**、`行内代码`
"""
import html
import importlib.util
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent
SLIDES_DIR = ROOT / "slides"
BUILD_DIR = ROOT / "_build"
OUT_DIR = ROOT / "out"
CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
W, H = 1080, 1440
BRAND = "X老师读源码"
FOOTER = "基于 Apache Spark 4.2.0 · 实验验证"

CSS = """
* { margin: 0; padding: 0; box-sizing: border-box; }
html, body { width: %(W)dpx; height: %(H)dpx; overflow: hidden; }
body {
  font-family: "PingFang SC", "Hiragino Sans GB", "STHeiti", sans-serif;
  background: #FBF8F3; color: #1E2A3A;
  display: flex; flex-direction: column; padding: 70px 84px 60px;
}
.top { display: flex; justify-content: space-between; align-items: center;
       font-size: 28px; color: #8A94A3; letter-spacing: 1px; }
.top .brand { color: #1F4E79; font-weight: 600; }
.top .brand::before { content: ""; display: inline-block; width: 14px; height: 14px;
       background: #E8711A; border-radius: 3px; margin-right: 12px; vertical-align: 1px; }
.main { flex: 1; display: flex; flex-direction: column; justify-content: center; overflow: hidden; margin-top: 40px; }
.fit { font-size: 46px; word-break: keep-all; overflow-wrap: anywhere; }
h1 { font-size: 1.5em; line-height: 1.3; font-weight: 700; color: #1F4E79; margin-bottom: 0.9em;
     padding-left: 0.5em; border-left: 0.22em solid #E8711A; }
ul { list-style: none; }
li { line-height: 1.55; margin-bottom: 0.62em; padding-left: 1.1em; position: relative; }
li::before { content: ""; position: absolute; left: 0.1em; top: 0.68em; width: 0.38em; height: 0.38em;
             border-radius: 50%%; background: #E8711A; }
li.sub { margin-left: 1.2em; font-size: 0.88em; color: #3D4B5C; }
li.sub::before { background: #B8C2CF; }
li.plain { padding-left: 0; } li.plain::before { display: none; }
b { color: #C2410C; font-weight: 700; }
code { font-family: "SF Mono", Menlo, monospace; font-size: 0.86em; background: #EEF2F7; color: #1F4E79;
       padding: 0.05em 0.3em; border-radius: 0.2em; }
pre { font-family: "SF Mono", Menlo, monospace; font-size: 0.66em; line-height: 1.55;
      background: #1E2A3A; color: #E6EDF5; padding: 0.9em 1em; border-radius: 0.5em;
      margin: 0.4em 0 0.8em; white-space: pre; }
pre b { color: #FDBA74; }
.note { margin-top: 0.9em; font-size: 0.8em; line-height: 1.5; color: #3D4B5C; background: #FDEBD8;
        border-radius: 0.4em; padding: 0.7em 0.9em; }
.card { font-family: "SF Mono", Menlo, "PingFang SC", monospace; font-size: 0.8em; line-height: 1.75;
        background: #fff; border: 3px solid #1F4E79; border-radius: 0.6em; padding: 1em 1.1em;
        white-space: pre; box-shadow: 0.35em 0.35em 0 #FDEBD8; }
.foot { display: flex; justify-content: space-between; font-size: 24px; color: #A0A9B5; margin-top: 30px; }
/* 封面 */
body.cover { background: #1F4E79; color: #fff; }
body.cover .top, body.cover .top .brand { color: #fff; }
body.cover .main { justify-content: center; }
.cover-title { font-size: 2.6em; line-height: 1.25; font-weight: 800; }
.cover-title b { color: #FDBA74; }
.cover-line { width: 3.2em; height: 0.3em; background: #E8711A; margin: 0.9em 0; border-radius: 0.15em; }
.cover-sub { font-size: 1.05em; line-height: 1.6; color: #D6E2F0; }
.badge { display: inline-block; margin-top: 1.6em; font-size: 0.8em; background: #E8711A; color: #fff;
         padding: 0.35em 0.9em; border-radius: 2em; font-weight: 600; }
body.cover .foot { color: #9FB6D0; }
/* 互动页 */
body.cta .main { justify-content: center; align-items: center; text-align: center; }
.cta-title { font-size: 2.1em; line-height: 1.4; font-weight: 800; color: #1F4E79; }
.cta-title b { color: #E8711A; }
.cta-sub { margin-top: 1em; font-size: 1em; color: #3D4B5C; line-height: 1.6; }
.cta-arrow { margin-top: 1.2em; font-size: 2em; }
""" % {"W": W, "H": H}

# 文字放不下时自动缩小字号（同时检查高度和宽度）
FIT_JS = """
<script>
(function () {
  var main = document.querySelector('.main'), fit = document.querySelector('.fit');
  var size = parseFloat(getComputedStyle(fit).fontSize);
  function over() {
    if (main.scrollHeight > main.clientHeight + 1) return true;
    // 代码块 / 卡片：直接测量文字宽度，与扣除左右内边距后的可用宽度比较
    var boxes = fit.querySelectorAll('pre, .card');
    for (var i = 0; i < boxes.length; i++) {
      var el = boxes[i], cs = getComputedStyle(el);
      var avail = el.clientWidth - parseFloat(cs.paddingLeft) - parseFloat(cs.paddingRight);
      var r = document.createRange(); r.selectNodeContents(el);
      if (r.getBoundingClientRect().width > avail + 1) return true;
    }
    return false;
  }
  while (over() && size > 18) { size -= 1; fit.style.fontSize = size + 'px'; }
  document.title = 'fontsize=' + size;
})();
</script>
"""


NBSP = "\u00a0"
EMOJI = "[\U0001F300-\U0001FAFF\u2600-\u27BF\u2B50]"


def keep_together(text):
    """避免难看的断行：数字和后面的单位、表情和前面的文字不拆到两行。"""
    text = re.sub(r"(\d) ", lambda m: m.group(1) + NBSP, text)
    text = re.sub(" (?=" + EMOJI + ")", NBSP, text)
    return text


def inline(text):
    """转义 HTML，并处理 **加粗** 和 `代码`。"""
    t = html.escape(keep_together(text))
    t = re.sub(r"`([^`]+)`", r"<code>\1</code>", t)
    t = re.sub(r"\*\*(.+?)\*\*", r"<b>\1</b>", t)
    return t


def code_block(text):
    """代码块：转义后保留 **高亮**。"""
    t = html.escape(text.strip("\n"))
    t = re.sub(r"\*\*(.+?)\*\*", r"<b>\1</b>", t)
    return f"<pre>{t}</pre>"


def render_items(items):
    out = []
    for it in items:
        cls = ""
        if it.startswith("  "):
            cls, it = "sub", it.strip()
        elif it.startswith("| "):
            cls, it = "plain", it[2:]
        out.append(f'<li class="{cls}">{inline(it)}</li>')
    return "<ul>" + "".join(out) + "</ul>"


def br(text):
    return inline(text).replace("\n", "<br>")


def body_html(s):
    t = s["type"]
    if t == "cover":
        return f"""
        <div class="cover-title">{br(s["title"])}</div>
        <div class="cover-line"></div>
        <div class="cover-sub">{br(s.get("subtitle", ""))}</div>
        {f'<div><span class="badge">{inline(s["badge"])}</span></div>' if s.get("badge") else ""}"""
    if t == "cta":
        return f"""
        <div class="cta-title">{br(s["title"])}</div>
        <div class="cta-sub">{br(s.get("subtitle", ""))}</div>
        <div class="cta-arrow">👇</div>"""
    parts = [f'<h1>{inline(s["heading"])}</h1>']
    if s.get("items"):
        parts.append(render_items(s["items"]))
    if s.get("code"):
        parts.append(code_block(s["code"]))
    if s.get("items_after"):
        parts.append(render_items(s["items_after"]))
    if s.get("html"):
        parts.append(s["html"])
    if s.get("card"):
        c = html.escape(s["card"].strip("\n"))
        c = re.sub(r"\*\*(.+?)\*\*", r"<b>\1</b>", c)
        parts.append(f'<div class="card">{c}</div>')
    if s.get("note"):
        parts.append(f'<div class="note">{inline(s["note"])}</div>')
    return "".join(parts)


def page_html(s, idx, total):
    cls = {"cover": "cover", "cta": "cta"}.get(s["type"], "")
    return f"""<!doctype html><html><head><meta charset="utf-8"><style>{CSS}</style></head>
<body class="{cls}">
  <div class="top"><span class="brand">{BRAND}</span><span>{idx}/{total}</span></div>
  <div class="main"><div class="fit">{body_html(s)}</div></div>
  <div class="foot"><span>{FOOTER}</span><span>{BRAND}</span></div>
  {FIT_JS}
</body></html>"""


def load(path):
    spec = importlib.util.spec_from_file_location(path.stem, path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod.SLIDES


def render(path):
    slides = load(path)
    key = path.stem[1:]  # s03 -> 03
    build, out = BUILD_DIR / key, OUT_DIR / key
    build.mkdir(parents=True, exist_ok=True)
    out.mkdir(parents=True, exist_ok=True)
    for old in out.glob("*.png"):
        old.unlink()
    for i, s in enumerate(slides, 1):
        name = f"{key}-P{i:02d}"
        hf = build / f"{name}.html"
        hf.write_text(page_html(s, i, len(slides)), encoding="utf-8")
        subprocess.run(
            [CHROME, "--headless=new", "--disable-gpu", "--hide-scrollbars",
             "--force-device-scale-factor=1", f"--window-size={W},{H}",
             "--virtual-time-budget=2000", f"--screenshot={out / (name + '.png')}",
             hf.as_uri()],
            check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        # 读取自动缩放后的字号，过小时提示（说明这一页内容太多）
        dom = subprocess.run(
            [CHROME, "--headless=new", "--disable-gpu", f"--window-size={W},{H}",
             "--virtual-time-budget=2000", "--dump-dom", hf.as_uri()],
            capture_output=True, text=True).stdout
        m = re.search(r"<title>fontsize=([\d.]+)</title>", dom)
        size = float(m.group(1)) if m else -1
        warn = "  ⚠️ 内容偏多，字号已缩小" if 0 < size < 34 else ""
        print(f"  {name}.png  字号 {size:g}px{warn}")


def main():
    keys = sys.argv[1:]
    files = sorted(SLIDES_DIR.glob("s*.py"))
    if keys:
        files = [f for f in files if f.stem[1:] in keys]
    for f in files:
        print(f"[{f.stem}]")
        render(f)


if __name__ == "__main__":
    main()
