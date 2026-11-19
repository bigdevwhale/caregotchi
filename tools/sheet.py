"""Glues rendered screens side by side: python tools/sheet.py out.png 1-home 2-sill ..."""
import sys
from pathlib import Path
from PIL import Image
src = Path(__file__).resolve().parent.parent / "app" / "build" / "art" / "screens"
ims = [Image.open(src / f"{n}.png").convert("RGB") for n in sys.argv[2:]]
w, h = ims[0].size
S = Image.new("RGB", (w * len(ims), h), "white")
for i, im in enumerate(ims): S.paste(im, (i * w, 0))
S.thumbnail((2400, 1300)); S.save(sys.argv[1])
