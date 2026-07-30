from __future__ import annotations

from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parent
ORIGINAL = ROOT / "smoke_bomb_transparent_source.png"
EDIT = ROOT / "smoke_bomb_orange_edit_transparent.png"
OUTPUT = ROOT / "smoke_bomb_orange_composited_source.png"


original = Image.open(ORIGINAL).convert("RGBA")
edit = Image.open(EDIT).convert("RGBA")
if original.size != edit.size:
    raise ValueError(f"Source size mismatch: {original.size} != {edit.size}")

original_pixels = original.load()
edit_pixels = edit.load()
changed = 0

for y in range(original.height):
    for x in range(original.width):
        red, green, blue, alpha = original_pixels[x, y]

        # The plume occupies the upper portion of the source. Restricting the
        # transfer to warm-colored opaque pixels above the can guarantees that
        # the metal body and its red X emblem remain unchanged.
        is_smoke = (
            y < 620
            and alpha >= 128
            and red > 80
            and red > green * 1.45
            and red > blue * 1.45
        )
        if not is_smoke:
            continue

        edit_red, edit_green, edit_blue, edit_alpha = edit_pixels[x, y]
        if (
            edit_alpha >= 128
            and edit_red > 80
            and edit_red > edit_green
            and edit_green > edit_blue
        ):
            new_color = (edit_red, edit_green, edit_blue, alpha)
        else:
            # Preserve the original pixel when the edited source moved an edge,
            # but warm its hue using the same red-orange balance.
            new_green = min(red - 1, max(green, round(green + red * 0.18)))
            new_blue = round(blue * 0.65)
            new_color = (red, new_green, new_blue, alpha)

        if new_color != original_pixels[x, y]:
            original_pixels[x, y] = new_color
            changed += 1

original.save(OUTPUT, optimize=True)
print(f"Wrote {OUTPUT}")
print(f"Smoke pixels recolored: {changed}")
