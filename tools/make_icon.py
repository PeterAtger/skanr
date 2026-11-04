import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app" / "src" / "main" / "res"

BG = "#121110"
PAGE = "#DAD6D0"
EDGE = "#36312D"
HEADING = "#948C84"
LINE = "#BDB6AE"
BRACKET = "#C9A486"

A4 = 297 / 210
FRONT_W = 25
SIDE_W = 21.5
CENTER = 54
SCALE = 1.1


def rect(x, y, w, h):
    return f"M{x:.2f},{y:.2f}h{w:.2f}v{h:.2f}h{-w:.2f}z"


def page(x, y, w, kind):
    h = w * A4
    heading, lines = [], []
    if kind == "front":
        heading.append(rect(x + .1 * w, y + .08 * h, .46 * w, .045 * h))
        for i in range(9):
            lines.append(rect(x + .1 * w, y + (.18 + i * .072) * h, (.5 if i % 4 == 3 else .8) * w, .026 * h))
    elif kind == "left":
        heading.append(rect(x + .14 * w, y + .1 * h, .36 * w, .05 * h))
        for i in range(5):
            lines.append(rect(x + .14 * w, y + (.42 + i * .075) * h, .72 * w, .028 * h))
    else:
        for i in range(8):
            lines.append(rect(x + .14 * w, y + (.12 + i * .075) * h, .72 * w, .028 * h))
    return rect(x, y, w, h), "".join(heading), "".join(lines)


def brackets(x0, y0, x1, y1, length, width):
    rects = [
        (x0, y0, length, width), (x0, y0, width, length),
        (x1 - length, y0, length, width), (x1 - width, y0, width, length),
        (x0, y1 - width, length, width), (x0, y1 - length, width, length),
        (x1 - length, y1 - width, length, width), (x1 - width, y1 - length, width, length),
    ]
    return "".join(rect(*r) for r in rects)


def layout():
    side_h = SIDE_W * A4
    front_h = FRONT_W * A4
    fx, fy = CENTER - FRONT_W / 2, CENTER - front_h / 2 - 1
    sides = [
        (page(CENTER - 13 - SIDE_W / 2, CENTER - side_h / 2 + 1.5, SIDE_W, "left"), -11, CENTER - 13, CENTER + 1.5),
        (page(CENTER + 13 - SIDE_W / 2, CENTER - side_h / 2 + 1.5, SIDE_W, "right"), 9, CENTER + 13, CENTER + 1.5),
    ]
    pad = 2.6
    return sides, page(fx, fy, FRONT_W, "front"), brackets(fx - pad, fy - pad, fx + FRONT_W + pad, fy + front_h + pad, 5, 1.3)


def vector_drawable(mono=False):
    sides, front, br = layout()

    def paths(p, outline_only):
        body, heading, lines = p
        if outline_only:
            return f'<path android:strokeColor="#000000" android:strokeWidth="1.6" android:pathData="{body}" />'
        out = f'<path android:fillColor="{PAGE}" android:strokeColor="{EDGE}" android:strokeWidth=".5" android:pathData="{body}" />'
        if heading:
            out += f'<path android:fillColor="{HEADING}" android:pathData="{heading}" />'
        return out + f'<path android:fillColor="{LINE}" android:pathData="{lines}" />'

    groups = "".join(
        f'<group android:rotation="{deg}" android:pivotX="{px:.2f}" android:pivotY="{py:.2f}">{paths(p, mono)}</group>'
        for p, deg, px, py in sides
    )
    front_paths = paths(front, False) if not mono else f'<path android:fillColor="#000000" android:pathData="{front[0]}" />'
    color = "#000000" if mono else BRACKET
    return f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <group android:scaleX="{SCALE}" android:scaleY="{SCALE}" android:pivotX="{CENTER}" android:pivotY="{CENTER}">
        {groups}
        {front_paths}
        <path android:fillColor="{color}" android:pathData="{br}" />
    </group>
</vector>
'''


def svg():
    sides, front, br = layout()

    def paths(p):
        body, heading, lines = p
        out = f'<path fill="{PAGE}" stroke="{EDGE}" stroke-width=".5" d="{body}"/>'
        if heading:
            out += f'<path fill="{HEADING}" d="{heading}"/>'
        return out + f'<path fill="{LINE}" d="{lines}"/>'

    groups = "".join(f'<g transform="rotate({deg} {px:.2f} {py:.2f})">{paths(p)}</g>' for p, deg, px, py in sides)
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="512" height="512">'
        f'<rect width="108" height="108" fill="{BG}"/><g transform="translate({CENTER} {CENTER}) scale({SCALE}) translate({-CENTER} {-CENTER})">'
        f'{groups}{paths(front)}<path fill="{BRACKET}" d="{br}"/></g></svg>'
    )


def main():
    (RES / "drawable" / "ic_launcher_fg.xml").write_text(vector_drawable())
    (RES / "drawable" / "ic_launcher_mono.xml").write_text(vector_drawable(mono=True))
    (ROOT / "design" / "icon.svg").write_text(svg())


if __name__ == "__main__":
    sys.exit(main())
