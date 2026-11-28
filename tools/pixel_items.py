"""Puts the bought things on the room's own pixel grid (#25), so they don't stand out from the hollow.

    python tools/pixel_items.py            # writes res/drawable-nodpi/room_item_*.webp from item_*.webp
    python tools/pixel_items.py --preview out.png   # the room with every thing in it, as on a 1080 px screen

The room and the pet are drawn in chunky pixels – about 140 across the screen – while the things came finer and
brighter, with no shadow. Each thing is shrunk to as many pixels as it covers on that grid, its colours cut to a
small palette and warmed a little towards the room, given a one-pixel dark outline, and blown back up with sharp
pixels. Things on the floor get a soft pixel shadow under them. The shop keeps the fine item_*.webp; the room
draws room_item_*.webp, so this can be run again after tools/cut.py.
"""
import re
import sys
from pathlib import Path

from PIL import Image, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"
GAME = ROOT / "app" / "src" / "main" / "java" / "app" / "caregotchi" / "domain" / "Game.kt"
GRID = 240          # room pixels across the screen (room_bg is ~250 art pixels wide)
SCALE = 4           # stored pixel size
FLOOR = 0.6         # things placed lower than this share of the room's height stand on the floor
OUTLINE = False     # the Codex things already have their own dark outline


def items():
    """id -> (x, y, w) from Shop.cozy."""
    out = {}
    for m in re.finditer(r'Item\("(\w+)", "[^"]*", "[^"]*", \d+, \d+, ([.\d]+)f, ([.\d]+)f, ([.\d]+)f\)', GAME.read_text(encoding="utf-8")):
        out[m.group(1)] = tuple(float(v) for v in m.groups()[1:])
    return out


def warm(px):
    r, g, b, a = px
    # a touch less saturated and a little warmer, like the room's lamplight
    grey = (r + g + b) / 3
    r, g, b = [c * 0.85 + grey * 0.15 for c in (r, g, b)]
    return (min(255, int(r * 1.03 + 4)), int(g * 0.99), int(b * 0.92), a)


def pixelate(im: Image.Image, cells: int, floor: bool) -> Image.Image:
    im = im.convert("RGBA")
    w, h = im.size
    cw = max(6, cells); ch = max(6, round(h * cw / w))
    small = im.resize((cw, ch), Image.LANCZOS)
    # hard alpha: pixel art has no half-transparent edges
    a = small.getchannel("A").point(lambda v: 255 if v > 110 else 0)
    rgb = small.convert("RGB").quantize(colors=20, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE).convert("RGB")
    small = Image.merge("RGBA", (*rgb.split(), a))
    small.putdata([warm(p) if p[3] else (0, 0, 0, 0) for p in small.get_flattened_data()])
    # a one-pixel dark outline around the shape, like the room's furniture
    pad = 2 if floor else 1
    canvas = Image.new("RGBA", (cw + 2, ch + 1 + pad), (0, 0, 0, 0))
    mask = Image.new("L", canvas.size, 0); mask.paste(a, (1, 1))
    ring = mask.filter(ImageFilter.MaxFilter(3))
    outline = Image.new("RGBA", canvas.size, (58, 34, 26, 255))
    if floor:
        # a flat shadow on the boards under the thing
        sh = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
        sw = int(cw * 0.9); sx = (canvas.width - sw) // 2; sy = ch - 1
        for y in range(sy, min(canvas.height, sy + pad + 1)):
            inset = (y - sy) * 2
            for x in range(sx + inset, sx + sw - inset):
                sh.putpixel((x, y), (40, 22, 14, 95))
        canvas = Image.alpha_composite(canvas, sh)
    if OUTLINE: canvas.paste(outline, (0, 0), ring)
    canvas.paste(small, (1, 1), a)
    return canvas.resize((canvas.width * SCALE, canvas.height * SCALE), Image.NEAREST)


def main():
    pos = items()
    built = {}
    for f in sorted(OUT.glob("item_*.webp")):
        id_ = f.stem[5:]
        x, y, w = pos.get(id_, (0, 0, .1))
        cells = round(w * GRID)
        out = pixelate(Image.open(f), cells, y >= FLOOR)
        built[id_] = out
        if "--preview" not in sys.argv:
            out.save(OUT / f"room_{f.name}", "WEBP", lossless=True)
    if "--preview" in sys.argv:
        dst = Path(sys.argv[sys.argv.index("--preview") + 1])
        room = Image.open(OUT / "room_bg.webp").convert("RGBA").resize((1080, 1620), Image.NEAREST)
        before = room.copy()
        for id_, (x, y, w) in sorted(pos.items(), key=lambda kv: kv[1][1]):
            for canvas, pic in ((before, Image.open(OUT / f"item_{id_}.webp")), (room, built[id_])):
                pic = pic.convert("RGBA")
                tw = int(w * 1080); th = int(pic.height * tw / pic.width)
                canvas.alpha_composite(pic.resize((tw, th), Image.NEAREST), (int(x * 1080), int(y * 1620)))
        side = Image.new("RGBA", (2160, 1620)); side.paste(before, (0, 0)); side.paste(room, (1080, 0))
        side.convert("RGB").save(dst)
        print(dst)


if __name__ == "__main__":
    main()
