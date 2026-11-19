"""Cuts art/sheets into design/img for the HTML prototype.

pets 3×3 → <pet>_<0..8>.png; icons/items/badges 4×4, pots/eggs 4×3 → <kind>_<name>.png;
banner_* → centre band 2.4:1 as .jpg; room → room.jpg.
"""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SHEETS = ROOT / "art" / "sheets"
OUT = ROOT / "design" / "img"

PETS = ["fox", "bunny", "hedgehog", "owl"]
GRIDS = {
    "icons": (4, 4, ["home", "sill", "diary", "detective", "nest", "shop", "eat", "water", "pill", "sleep",
                     "leaf", "flame", "scroll", "chest", "star", "heart"]),
    "items": (4, 4, ["hammock", "rocker", "lights", "books", "fishbowl", "hotwater", "fern", "lantern",
                     "record", "clock", "tea", "painting", "plush", "musicbox", "cushions", "armchair"]),
    "badges": (4, 4, [str(i) for i in range(16)]),
    "pots": (4, 3, [f"{h}_{s}" for h in ["mint", "chamomile", "lavender"] for s in range(4)]),
    "eggs": (4, 3, ["fox", "bunny", "hedgehog", "owl", "newt", "turtle", "phoenix", "sloth", "raccoon", "mole", "moth", "frog"]),
}


def key(im):
    im = im.convert("RGBA")
    px = im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            m = min(r, b) - g
            if m > 90:
                px[x, y] = (0, 0, 0, 0)
            elif m > 40:
                k = (m - 40) / 50
                px[x, y] = (int(r - (r - g) * k), g, int(b - (b - g) * k), int(255 * (1 - k)))
    return im


def cells(path, cols, rows, size):
    im = key(Image.open(path))
    w, h = im.size
    cw, ch = w / cols, h / rows
    for n in range(cols * rows):
        i, j = n % cols, n // cols
        cell = im.crop((int(i * cw), int(j * ch), int((i + 1) * cw), int((j + 1) * ch)))
        box = cell.getchannel("A").point(lambda v: 255 if v > 30 else 0).getbbox()
        cell = cell.crop(box) if box else cell
        cell.thumbnail((size, size))
        yield n, cell


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for f in sorted(SHEETS.glob("*.png")):
        s = f.stem
        dst = OUT / (s + ".jpg")
        if s == "room":
            im = Image.open(f).convert("RGB")
            im.thumbnail((720, 1080))
            im.save(OUT / "room.jpg", quality=88)
        elif s.startswith("banner_"):
            im = Image.open(f).convert("RGB")
            w, h = im.size
            bh = int(w / 2.4)
            im = im.crop((0, (h - bh) // 2, w, (h - bh) // 2 + bh))
            im.thumbnail((800, 800))
            im.save(dst, quality=86)
        elif s in PETS:
            for n, c in cells(f, 3, 3, 256):
                c.save(OUT / f"{s}_{n}.png")
        elif s in GRIDS:
            cols, rows, names = GRIDS[s]
            kind = s.rstrip("s") if s != "items" else "item"
            for n, c in cells(f, cols, rows, 160):
                c.save(OUT / f"{kind}_{names[n]}.png")
        else:
            continue
        print("cut", s)


if __name__ == "__main__":
    main()
