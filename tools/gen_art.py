"""Generates Caregotchi art with the Codex CLI image tool (pattern from Pumpling).

Codex saves images into ~/.codex/generated_images/<session>/; the image of the run's own session
is copied to art/sheets/<id>.png. Already present sheets are skipped.

    python tools/gen_art.py            # everything missing
    python tools/gen_art.py fox room   # only these
"""
import re
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "art" / "sheets"
GEN = Path.home() / ".codex" / "generated_images"

STYLE = (
    "Style: detailed 16-bit pixel art like a modern cozy indie game, crisp pixels, clean dark outline, "
    "3-4 tone shading ramps, soft muted warm palette (oatmeal, honey, sage, dusty rose, cocoa), "
    "warm and cozy."
)
GRID = (
    "Layout: 3 columns x 3 rows of equal square cells on a flat solid pure magenta (#FF00FF) background. "
    "One creature per cell, centered, with clear margin; nothing touches the cell borders. No text, no grid lines, "
    "no shadows on the background, no other objects except those listed."
)

PETS = {
    "fox": "a small red fox kit with a cream belly, wearing soft striped blue pyjamas",
    "bunny": "a round oatmeal-coloured bunny with floppy ears and a knitted sage-green scarf",
    "hedgehog": "a plump hedgehog with honey-brown spines and a tiny white nurse cap with a pink cross",
    "owl": "a round grey owlet with big amber eyes and a small knitted nightcap",
    "newt": "a small blue-teal newt with a cream belly, little orange cheek spots and a tiny raincoat hood",
    "turtle": "a little green turtle with a honey-brown patterned shell and a knitted sage scarf",
    "phoenix": "a small phoenix chick with soft orange-gold feathers, a flame-like crest and warm glowing tail feathers",
    "sloth": "a fluffy beige-brown sloth with a sleepy smile and a striped lilac sleeping mask pushed up on its head",
    "raccoon": "a chubby grey raccoon with a dark eye mask, a ringed tail and a small checkered apron",
    "mole": "a velvety dark-brown mole with pink paws, a pink nose and tiny round spectacles",
    "moth": "a fluffy cream-and-lilac moth with big feathery antennae, soft patterned wings and a round fuzzy body",
    "frog": "a round green frog with dark spots, a pale belly and a tiny straw hat with a flower",
}


def pet_prompt(desc):
    return f"""Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a sprite sheet for a cozy self-care tamagotchi app pet: {desc}.
{STYLE} The pet is cute with big shiny eyes.
{GRID}
Cells, left to right, top to bottom:
1) baby: tiny, round dumpling-shaped body, stubby limbs, very cute
2) teen: a bit bigger
3) adult: standing happy and healthy, cute
4) adult eating porridge from a small bowl with a spoon
5) adult drinking from a big mug of water held in both paws
6) adult holding up a small round pill like a treasure, proud
7) adult cheering, jumping with joy, paws up
8) adult feeling unwell: wrapped in a patchwork blanket, a thermometer in its mouth, droopy eyes
9) adult asleep, curled up, peaceful face
The same character and palette in every cell, facing three-quarters right."""


OTHER = {
    "room": """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a background scene for a cozy self-care tamagotchi app: the inside of a small cozy home inside the hollow of an old tree, seen from the front, side-on like a 2D game room.
""" + STYLE + """ Portrait 2:3.
The room: curved walls of warm honey wood with visible rings, a round window in the upper middle with an empty wide windowsill (sky visible through it, keep the sky simple light blue), a soft wooden floor with a big round braided rug in the middle.
Left side: a small kitchen corner with a little round table, one stool and a tiny stove with a kettle. Upper left on the wall: a small wooden medicine cabinet with a few glass jars.
Right side: a small bed with a patchwork quilt and a pillow, a bedside table with a candle and a water jug.
IMPORTANT: no people, no animals, no text, no letters. The rug in the centre and the windowsill must be clear, free space (a pet and pots will be added later).""",
}


SHEET = """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: {what} for a cozy self-care tamagotchi app (a pet lives in a hollow of an old tree).
""" + STYLE + """
Layout: {cols} columns x {rows} rows of equal square cells on a flat solid pure magenta (#FF00FF) background. One object per cell, centered, with clear margin; nothing touches the cell borders. No text, no letters, no numbers, no grid lines, no shadows on the background.
Objects in order (left to right, top to bottom):
{items}
IMPORTANT: these are ordinary lifeless objects – NO faces, NO eyes, NO mouths, NO smiles, NO blush on any object, food, plant, pot or egg. Only living creatures (people's heads in the health icons) may show a calm closed-eye face."""

BANNER = """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a wide header banner illustration for a page of a cozy self-care tamagotchi app (a pet lives in a hollow of an old tree). Landscape 3:2.
""" + STYLE + """
IMPORTANT: put all the important content in the middle horizontal band (the top and bottom quarters will be cropped away). No text, no letters, no numbers, no people.
Scene: {scene}
IMPORTANT: objects, plants, pots, eggs and food have NO faces, NO eyes, NO mouths."""


def sheet(what, cols, rows, items):
    return SHEET.format(what=what, cols=cols, rows=rows, items="\n".join(f"{i + 1} {t}" for i, t in enumerate(items)))


OTHER["icons"] = sheet("a sheet of 16 small game UI icons, chunky and readable at small size, each a single object", 4, 4, [
    "a round wooden door in a tree trunk with a tiny warm window (home)",
    "a terracotta pot with a sprouting green herb (windowsill)",
    "a closed diary book with a patchwork quilt pattern on the cover and a ribbon bookmark",
    "a brass magnifying glass over a small pinned paper note",
    "a speckled egg in a small straw nest",
    "a little wicker basket with a striped cloth (shop)",
    "a ceramic bowl of warm porridge with steam",
    "a mug of water with a drop above it",
    "a two-colour pill capsule, honey and cream",
    "a crescent moon resting on a soft pillow",
    "a golden autumn leaf coin, shiny",
    "a small warm flame (streak)",
    "a rolled paper scroll tied with a red ribbon (quest)",
    "a small wooden treasure chest with brass corners, slightly open with glow",
    "a five-pointed golden star",
    "a red heart with a white plaster cross on it (health)",
])
OTHER["items"] = sheet("a sheet of 16 furniture and decor objects for a tiny tree-hollow room, each seen from the front, side-on like a 2D game room", 4, 4, [
    "a hammock with a striped blanket", "a rocking chair with a knitted throw", "a string of warm fairy lights (horizontal)", "a small bookshelf full of books",
    "a round fish bowl with a goldfish on a little stand", "a red hot water bottle in a knitted cover", "a potted fern", "a hanging paper lantern",
    "a wind-up record player", "a round wall clock", "a teapot with two cups on a small tray", "a framed painting of misty mountains",
    "a soft plush bear toy", "a little music box", "a pile of soft floor cushions", "a cosy armchair with a reading lamp",
])
OTHER["pots"] = sheet("a sheet of potted herbs in identical small terracotta pots, showing growth stages", 4, 3, [
    "mint: tiny sprout", "mint: growing, a few leaves", "mint: lush and blooming with small lilac flowers", "mint: wilting, droopy yellowed leaves",
    "chamomile: tiny sprout", "chamomile: growing", "chamomile: lush with many white daisy flowers", "chamomile: wilting, droopy yellowed leaves",
    "lavender: tiny sprout", "lavender: growing", "lavender: lush with purple flower spikes", "lavender: wilting, droopy greyish leaves",
])
OTHER["badges"] = sheet("a sheet of 16 round embroidered fabric patches (badges) with stitched borders, like scout patches, each with a single symbol", 4, 4, [
    "a pill on honey background", "a porridge bowl on sage", "a crescent moon on night blue", "a water drop on light blue",
    "a small storm cloud with a soft rainbow on dusty rose (an attack survived)", "a heart on cream", "a snowflake on pale blue", "a sun on yellow",
    "a flame on orange", "a turtle on green (course completed)", "a star on navy", "a leaf on olive",
    "a key on brown", "a crown on purple", "a feather on teal", "a magnifying glass on beige",
])
OTHER["eggs"] = sheet("a sheet of 12 pet eggs, all the same size and egg-shaped, each with its own pattern and colours hinting at the creature inside", 4, 3, [
    "orange with cream tail stripe (fox)", "oatmeal with soft ear spots (bunny)", "honey with tiny spikes (hedgehog)", "grey with feather scallops (owl)",
    "blue with water ripples (newt)", "green hexagon shell (turtle)", "red-gold with flame pattern, glowing (phoenix)", "mossy grey-green with leaves (sloth)",
    "grey with a black mask band (raccoon)", "dark velvet with pink speckles (mole)", "pale lilac with moth wing patterns (moth)", "green with dark spots (frog)",
])
OTHER["pots2"] = sheet("a sheet of potted herbs in identical small terracotta pots, showing growth stages", 4, 3, [
    "rosemary: tiny sprout", "rosemary: growing", "rosemary: lush with small pale-blue flowers", "rosemary: wilting, droopy yellowed needles",
    "sage: tiny sprout", "sage: growing, soft grey-green leaves", "sage: lush with violet flower spikes", "sage: wilting, droopy yellowed leaves",
    "aloe: tiny sprout", "aloe: growing", "aloe: lush with a tall orange flower stalk", "aloe: wilting, droopy brownish leaves",
])
OTHER["icons2"] = sheet("a sheet of 16 small game UI icons, chunky and readable at small size, each a single object", 4, 4, [
    "a pair of small brown walking boots (a walk outside)",
    "a neatly folded patchwork quilt (a sick day blanket)",
    "a cracking egg with a little light coming out",
    "a small brass bell (reminder)",
    "a glass thermometer",
    "a round brass barometer dial with a needle",
    "a rising sun over a small hill (morning)",
    "a wall calendar page with a red ribbon",
    "a sealed paper envelope with a red wax seal (letter)",
    "a doctor's paper document with a red cross stamp",
    "a small golden trophy cup",
    "a small brass padlock",
    "a small green seedling sprouting from soil",
    "a small brass cogwheel (settings)",
    "a cold compress: a folded blue cloth with a few ice cubes",
    "a small pink pill box with seven compartments",
])
OTHER["appicon"] = """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: an app icon for a cozy self-care tamagotchi app. """ + STYLE + """ Square 1:1.
A small red fox kit in striped blue pyjamas peeking happily out of a round wooden door in an old tree trunk, holding a tiny pill like a treasure; warm light from inside the hollow; a couple of green leaves around. Big, simple, readable shapes centred with generous margin, a plain warm honey-coloured background filling the whole square. No text, no letters, no border."""
OTHER["icons3"] = sheet("a sheet of 16 food and sport icons, chunky and readable at small size, each a single object", 4, 4, [
    "breakfast: a sunny-side-up egg with a slice of toast on a small plate, a little sun above",
    "lunch: a steaming bowl of soup with a spoon",
    "dinner: a plate with baked fish and vegetables, a small candle beside it",
    "snack: an apple and a cookie on a napkin",
    "wholesome: a bowl of fresh green salad with tomatoes and seeds, a small sparkle",
    "ordinary: a plate of pasta with a fork",
    "fast food: a burger with fries and a pink donut",
    "vegetables: a carrot, a broccoli floret and a cucumber",
    "fruit: a red apple, a banana and an orange",
    "protein: a boiled egg, a piece of salmon and a small cup of yogurt",
    "sweets: a wrapped candy, a chocolate bar and a lollipop",
    "running: a single colourful running shoe with motion lines",
    "gym: a small dumbbell and a kettlebell",
    "yoga: a rolled purple yoga mat with a small lotus",
    "bike: a small city bicycle with a basket",
    "swimming: swim goggles and a swimming cap with a splash",
])
OTHER["icons4"] = sheet("a sheet of 16 small game UI icons about feeling unwell and time, chunky and readable at small size, each a single object", 4, 4, [
    "dancing: two dance shoes with music notes",
    "home workout: a jump rope coiled",
    "other sport: a ball",
    "migraine: a head silhouette in profile with a small lightning bolt and a dark cloud over it",
    "headache: a head in profile with a cold wet cloth on the forehead",
    "stomach: a round tummy with a swirl on it and a hot water bottle",
    "back pain: a curved back with a plaster on it",
    "anxiety: a small grey cloud with a scribbled swirl inside",
    "allergy: a box of tissues with a sneezing flower beside it",
    "blood pressure: a tonometer with a cuff and a round gauge",
    "a cold: a woolly scarf and a cup of tea with lemon",
    "period: a calendar page with a red drop and a small flower",
    "an alarm clock with two bells",
    "a doctor's prescription paper with a pill drawn on it and a stamp",
    "a brown pill bottle with a white label",
    "an hourglass with golden sand",
])
for _k, _s in {
    "banner_sill": "a wide wooden windowsill inside a tree hollow with three small terracotta herb pots, a watering can and a pill box, morning sunlight and a soft green forest outside the window",
    "banner_diary": "a cosy wooden desk with an open diary, a folded patchwork quilt, a candle and a cup of tea, warm evening light",
    "banner_detective": "a cork board on a wooden wall with pinned paper notes connected by red string, a brass magnifying glass and a tiny detective cap on a shelf below, warm lamp light",
    "banner_nursery": "a warm nest shelf with several patterned eggs in straw nests under a glowing lamp, a little hand-knitted blanket, soft golden light",
    "banner_shop": "a tiny cozy forest shop counter inside a tree, shelves with jars, folded quilts, lanterns and plush toys, a small brass bell on the counter, a friendly badger shopkeeper in an apron behind the counter",
    "banner_quests": "a wooden notice board in a forest glade with pinned paper scrolls and a small lantern, fireflies, soft dusk light",
}.items():
    OTHER[_k] = BANNER.format(scene=_s)


def image_of(output):
    m = re.search(r"session id:\s*([0-9a-f-]+)", output)
    if not m:
        return None
    pngs = sorted((GEN / m.group(1)).glob("*.png"), key=lambda p: p.stat().st_mtime)
    return pngs[-1] if pngs else None


def run(name, prompt):
    dst = OUT / f"{name}.png"
    if dst.exists():
        print("skip", name)
        return
    r = subprocess.run([shutil.which("codex.cmd") or shutil.which("codex") or "codex", "exec", "--skip-git-repo-check",
                        "--sandbox", "read-only", "-"],
                       input=prompt, text=True, encoding="utf-8", capture_output=True, cwd=str(ROOT))
    p = image_of(r.stdout + r.stderr)
    if p is None:
        print("FAIL", name, r.stdout[-400:], r.stderr[-400:], flush=True)
        return
    shutil.copy(p, dst)
    print("ok", name, p, flush=True)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    want = sys.argv[1:]
    jobs = [(k, pet_prompt(v)) for k, v in PETS.items()] + list(OTHER.items())
    for name, prompt in jobs:
        if want and name not in want:
            continue
        run(name, prompt)


if __name__ == "__main__":
    main()
