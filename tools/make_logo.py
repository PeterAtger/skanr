import sys
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.pens.boundsPen import BoundsPen

FONT, OUT = sys.argv[1], sys.argv[2]
TEXT, TRACKING, WEIGHT = "skanr", -0.06, 600
INK, BRACKET, BG = "#ECE8E4", "#C9A486", "#121110"

font = instantiateVariableFont(TTFont(FONT), {"wght": WEIGHT})
glyphs, cmap = font.getGlyphSet(), font.getBestCmap()
upm = font["head"].unitsPerEm

pen = SVGPathPen(glyphs)
bounds = BoundsPen(glyphs)
x = 0
for ch in TEXT:
    name = cmap[ord(ch)]
    t = (1, 0, 0, -1, x, 0)
    glyphs[name].draw(TransformPen(pen, t))
    glyphs[name].draw(TransformPen(bounds, t))
    x += glyphs[name].width + TRACKING * upm
x0, y0, x1, y1 = bounds.bounds
text_w, text_h = x1 - x0, y1 - y0

pad_x, pad_top, pad_bottom = 0.083 * text_w, 0.077 * text_w, 0.097 * text_w
blen, bw = 0.068 * text_w, 0.0125 * text_w
bx0, by0, bx1, by1 = x0 - pad_x, y0 - pad_top, x1 + pad_x, y1 + pad_bottom


def brackets():
    rects = [
        (bx0, by0, blen, bw), (bx0, by0, bw, blen),
        (bx1 - blen, by0, blen, bw), (bx1 - bw, by0, bw, blen),
        (bx0, by1 - bw, blen, bw), (bx0, by1 - blen, bw, blen),
        (bx1 - blen, by1 - bw, blen, bw), (bx1 - bw, by1 - blen, bw, blen),
    ]
    return "".join(f"M{x:.1f},{y:.1f}h{w:.1f}v{h:.1f}h{-w:.1f}z" for x, y, w, h in rects)


text_path = pen.getCommands()
box_w, box_h = bx1 - bx0, by1 - by0

svg = f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{bx0 - 80:.0f} {by0 - 80:.0f} {box_w + 160:.0f} {box_h + 160:.0f}"><rect x="{bx0 - 80:.0f}" y="{by0 - 80:.0f}" width="100%" height="100%" fill="{BG}"/><path fill="{BRACKET}" d="{brackets()}"/><path fill="{INK}" d="{text_path}"/></svg>'
open(OUT, "w").write(svg)
print("box", round(box_w), round(box_h))
