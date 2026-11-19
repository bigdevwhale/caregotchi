"""Cuts the Codex sheets in art/sheets into app drawables (res/drawable-nodpi, webp).

    python tools/cut.py

pets 3×3 → pet_<id>_<0..8> (baby, teen, adult, eat, drink, pill, cheer, sick, asleep);
icons/icons2/items/badges 4×4, pots/pots2/eggs 4×3; banner_* → centre band 2.4:1; room → room_bg.
The magenta background is keyed out; each frame is cropped to its picture.
"""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SHEETS = ROOT / "art" / "sheets"
OUT = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"

PETS = ["fox", "bunny", "hedgehog", "owl", "newt", "turtle", "phoenix", "sloth", "raccoon", "mole", "moth", "frog"]
GRIDS = {
    "icons": (4, 4, "icon", ["home", "sill", "diary", "detective", "nest", "shop", "eat", "water", "pill", "sleep",
                             "leaf", "flame", "scroll", "chest", "star", "heart"]),
    "icons2": (4, 4, "icon", ["walk", "plaid", "hatch", "bell", "thermo", "baro", "sunrise", "calendar",
                              "letter", "doctor", "trophy", "lock", "seedling", "gear", "compress", "pillbox"]),
    "icons3": (4, 4, "icon", ["breakfast", "lunch", "dinner", "snack", "wholesome", "ordinary", "fastfood", "veg",
                              "fruit", "protein", "sweet", "run", "gym", "yoga", "bike", "swim"]),
    "icons4": (4, 4, "icon", ["dance", "rope", "ball", "migraine", "headache", "stomach", "back", "anxiety",
                              "allergy", "tonometer", "cold", "period", "alarm", "rx", "bottle", "hourglass"]),
    "items": (4, 4, "item", ["hammock", "rocker", "lights", "books", "fishbowl", "hotwater", "fern", "lantern",
                             "record", "clock", "tea", "painting", "plush", "musicbox", "cushions", "armchair"]),
    "badges": (4, 4, "badge", [str(i) for i in range(16)]),
    "pots": (4, 3, "pot", [f"{h}_{s}" for h in ["mint", "chamomile", "lavender"] for s in range(4)]),
    "pots2": (4, 3, "pot", [f"{h}_{s}" for h in ["rosemary", "sage", "aloe"] for s in range(4)]),
    "eggs": (4, 3, "egg", PETS),
}
SIZE = {"pet": 320, "icon": 128, "item": 256, "badge": 160, "pot": 200, "egg": 160}


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
    """Each picture of a grid sheet, whole even when it pokes over its cell's border.

    Pictures are found as connected blobs on the keyed sheet; a blob belongs to the cell its centre lies in,
    and little blobs (sparkles, steam) join the nearest big one in the same cell.
    """
    import numpy as np
    from scipy import ndimage
    im = key(Image.open(path))
    w, h = im.size
    cw, ch = w / cols, h / rows
    a = np.array(im.getchannel("A")) > 30
    lab, n = ndimage.label(ndimage.binary_dilation(a, iterations=3))
    objs = ndimage.find_objects(lab)
    owner = {}
    for k, sl in enumerate(objs, start=1):
        ys, xs = sl
        cy, cx = (ys.start + ys.stop) / 2, (xs.start + xs.stop) / 2
        owner[k] = min(int(cy // ch), rows - 1) * cols + min(int(cx // cw), cols - 1)
    px = np.array(im)
    for cell in range(cols * rows):
        ks = [k for k, c in owner.items() if c == cell]
        if not ks:
            yield cell, Image.new("RGBA", (8, 8))
            continue
        mask = np.isin(lab, ks) & a
        ys, xs = np.nonzero(mask)
        y0, y1, x0, x1 = ys.min(), ys.max() + 1, xs.min(), xs.max() + 1
        part = px[y0:y1, x0:x1].copy()
        part[~mask[y0:y1, x0:x1]] = 0
        img = Image.fromarray(part, "RGBA")
        img.thumbnail((size, size), Image.LANCZOS)
        yield cell, img


def save(im, name, q=90):
    im.save(OUT / f"{name}.webp", "WEBP", quality=q, method=6)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    only = set(__import__("sys").argv[1:])
    for f in sorted(SHEETS.glob("*.png")):
        s = f.stem
        if only and s not in only:
            continue
        if s == "room":
            im = Image.open(f).convert("RGB")
            im.thumbnail((900, 1350), Image.LANCZOS)
            save(im, "room_bg", 88)
        elif s.startswith("banner_"):
            im = Image.open(f).convert("RGB")
            w, h = im.size
            bh = int(w / 2.4)
            im = im.crop((0, (h - bh) // 2, w, (h - bh) // 2 + bh))
            im.thumbnail((1000, 1000), Image.LANCZOS)
            save(im, s, 85)
        elif s in PETS:
            for n, c in cells(f, 3, 3, SIZE["pet"]):
                save(c, f"pet_{s}_{n}")
        elif s in GRIDS:
            cols, rows, kind, names = GRIDS[s]
            if kind == "pot":
                # one scale for the whole sheet, standing on one floor: a sprout stays small next to a bush
                raw = dict(cells(f, cols, rows, 100000))
                mw = max(i.width for i in raw.values()); mh = max(i.height for i in raw.values())
                k = SIZE[kind] / max(mw, mh)
                W, H = round(mw * k), round(mh * k)
                for n, c in raw.items():
                    c = c.resize((max(1, round(c.width * k)), max(1, round(c.height * k))), Image.LANCZOS)
                    canvas = Image.new("RGBA", (W, H))
                    canvas.paste(c, ((W - c.width) // 2, H - c.height), c)
                    save(canvas, f"{kind}_{names[n]}")
            else:
                for n, c in cells(f, cols, rows, SIZE[kind]):
                    save(c, f"{kind}_{names[n]}")
        else:
            continue
        print("cut", s)


if __name__ == "__main__":
    main()
