"""App icon from art/sheets/appicon.png: adaptive foreground (the picture at 82% on its own background colour) and docs/logo.png."""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
src = Image.open(ROOT / "art" / "sheets" / "appicon.png").convert("RGB")
bg = src.getpixel((6, 6))
S = 432
fg = Image.new("RGB", (S, S), bg)
inner = src.resize((int(S * .82), int(S * .82)), Image.LANCZOS)
fg.paste(inner, ((S - inner.width) // 2, (S - inner.height) // 2))
out = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"
fg.save(out / "ic_launcher_fg.webp", "WEBP", quality=92)
(ROOT / "docs").mkdir(exist_ok=True)
src.resize((512, 512), Image.LANCZOS).save(ROOT / "docs" / "logo.png")
print("bg #%02X%02X%02X" % bg)
