import re
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MOCKUP = ROOT / "design" / "skanr-mockup.html"
OUT = ROOT / "design" / "store"
CHROME = "google-chrome"

SHOTS = [
    (6, "home", "All your scans, and nothing else."),
    (1, "welcome", "No ads. No account. Ever."),
    (3, "name", "Named from the page itself."),
    (4, "document", "Every page, one swipe away."),
    (5, "share", "PDFs sized for where they're going."),
]

EXTRA_CSS = """
body { margin: 0 !important; padding: 0 !important; overflow: hidden; background: var(--desk); }
.ready { display: none !important; }
.shot { width: 360px; height: 640px; display: flex; flex-direction: column; align-items: center; }
.cap { font-family: var(--display); font-size: 34px; line-height: 1.05; color: var(--graphite); text-align: center; margin: 0; padding: 0 30px 22px; height: 128px; box-sizing: border-box; display: flex; align-items: flex-end; justify-content: center; text-wrap: balance; }
.shot .phone { zoom: .76; box-shadow: 0 40px 80px -30px rgba(0, 0, 0, .7); }
.feature { width: 512px; height: 250px; display: grid; grid-template-columns: 1fr 1fr; align-items: center; background: var(--desk); overflow: hidden; }
.feature .left { padding-left: 40px; display: grid; gap: 14px; justify-items: start; }
.feature .logo { font-family: var(--body); font-weight: 600; font-size: 54px; letter-spacing: -.06em; line-height: .9; color: var(--graphite); padding: 8px 14px 12px; --len: 13px; --w: 2.5px; }
.feature .tag { font-family: var(--display); font-size: 25px; line-height: 1.05; color: var(--pencil); margin: 0; }
.feature .fan { position: relative; height: 250px; }
.feature .fan .page { position: absolute; width: 104px; --lh: 7px; top: 50px; left: 50%; }
.feature .fan .p1 { transform: translateX(-95%) rotate(-11deg); }
.feature .fan .p2 { transform: translateX(-5%) rotate(9deg); }
.feature .fan .front { position: absolute; top: 34px; left: 50%; transform: translateX(-50%); width: 126px; padding: 9px; }
.feature .fan .front .page { position: static; width: 100%; transform: none; box-shadow: 0 0 0 1px var(--rule), 0 16px 30px -14px rgba(0, 0, 0, .7); }
"""


def render(html: str, out: Path, width: int, height: int, scale: int, workdir: Path):
    page = workdir / (out.stem + ".html")
    page.write_text(html)
    subprocess.run(
        [CHROME, "--headless=new", "--disable-gpu", "--hide-scrollbars", f"--force-device-scale-factor={scale}",
         f"--window-size={width},{height}", "--virtual-time-budget=4000", f"--screenshot={out}", page.as_uri()],
        check=True, capture_output=True,
    )
    print(out.relative_to(ROOT))


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    src = MOCKUP.read_text()
    head = src[: src.index('<div class="wrap">')]
    figures = re.findall(r"<figure>(.*?)<figcaption>", src, re.S)
    doc = lambda body: f'<!doctype html><html data-theme="dark"><head><meta charset="utf-8">{head}<style>{EXTRA_CSS}</style></head><body>{body}</body></html>'

    with tempfile.TemporaryDirectory() as tmp:
        work = Path(tmp)
        for n, (index, name, caption) in enumerate(SHOTS, 1):
            phone = figures[index - 1].replace(
                '<span class="mark">skanr</span>',
                '<span class="mark brackets" style="--len:7px;--w:1.5px;padding:3px 7px 5px">skanr</span>',
            )
            render(doc(f'<div class="shot"><p class="cap">{caption}</p>{phone}</div>'), OUT / f"screenshot-{n}-{name}.png", 360, 640, 3, work)

        feature = """<div class="feature">
          <div class="left"><span class="logo brackets">skanr</span><p class="tag">Paper in,<br>clean PDF out.</p></div>
          <div class="fan"><span class="page p1 bill"></span><span class="page p2 sig"></span><span class="front brackets"><span class="page"></span></span></div>
        </div>"""
        render(doc(feature), OUT / "feature-graphic.png", 512, 250, 2, work)

        icon = f'<!doctype html><html><body style="margin:0">{(ROOT / "design" / "icon.svg").read_text()}</body></html>'
        render(icon, OUT / "icon-512.png", 512, 512, 1, work)


if __name__ == "__main__":
    sys.exit(main())
