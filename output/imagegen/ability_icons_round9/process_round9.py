from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parent
SIZE = 64
FLAME_FILL = (14, 139, 238, 255)
FLAME_OUTLINE = (0, 66, 172, 255)


def looks_like_magenta_fringe(red: int, green: int, blue: int) -> bool:
    return red > 180 and blue > 140 and green < min(red, blue) * 0.55


def is_flame_blue(red: int, green: int, blue: int) -> bool:
    return blue - red >= 55 and blue - green >= 25


def clean_and_resize(source_name: str, quantize_flame: bool = False) -> Image.Image:
    source = Image.open(ROOT / source_name).convert("RGBA")
    resized = source.resize((SIZE, SIZE), Image.Resampling.NEAREST)
    cleaned = Image.new("RGBA", resized.size, (0, 0, 0, 0))

    source_pixels = resized.load()
    target_pixels = cleaned.load()
    for y in range(SIZE):
        for x in range(SIZE):
            red, green, blue, alpha = source_pixels[x, y]
            if alpha < 128 or looks_like_magenta_fringe(red, green, blue):
                target_pixels[x, y] = (0, 0, 0, 0)
                continue

            if quantize_flame and is_flame_blue(red, green, blue):
                target_pixels[x, y] = FLAME_OUTLINE if green < 100 else FLAME_FILL
            else:
                target_pixels[x, y] = (red, green, blue, 255)

    return cleaned


def checkerboard(size: tuple[int, int], tile: int) -> Image.Image:
    image = Image.new("RGBA", size, (0, 0, 0, 255))
    draw = ImageDraw.Draw(image)
    colors = ((66, 66, 72, 255), (106, 106, 114, 255))
    for y in range(0, size[1], tile):
        for x in range(0, size[0], tile):
            color = colors[((x // tile) + (y // tile)) % 2]
            draw.rectangle((x, y, x + tile - 1, y + tile - 1), fill=color)
    return image


def checkerboard_preview(image: Image.Image, output_name: str, scale: int = 8) -> None:
    canvas = checkerboard((image.width * scale, image.height * scale), scale * 2)
    canvas.alpha_composite(image.resize(canvas.size, Image.Resampling.NEAREST))
    canvas.convert("RGB").save(ROOT / output_name, optimize=True)


def qa_sheet(images: list[tuple[str, Image.Image]]) -> None:
    cell = 256
    gap = 16
    canvas = Image.new(
        "RGBA",
        (gap + len(images) * (cell + gap), gap + 3 * (cell + gap)),
        (38, 38, 42, 255),
    )

    for column, (_, image) in enumerate(images):
        left = gap + column * (cell + gap)
        for row, qa_size in enumerate((64, 32, 16)):
            top = gap + row * (cell + gap)
            backing = checkerboard((cell, cell), 16)
            reduced = image.resize((qa_size, qa_size), Image.Resampling.NEAREST)
            enlarged = reduced.resize((cell, cell), Image.Resampling.NEAREST)
            backing.alpha_composite(enlarged)
            canvas.alpha_composite(backing, (left, top))

    canvas.convert("RGB").save(ROOT / "ability_icons_qa.png", optimize=True)


images = {
    "tea_time": clean_and_resize("tea_time_transparent_source_v2.png"),
    "tea_chi": clean_and_resize("tea_chi_transparent_source_v2.png", quantize_flame=True),
    "tea_toss": clean_and_resize("tea_toss_transparent_source.png"),
}

for ability_id, image in images.items():
    image.save(ROOT / f"ability_{ability_id}.png", optimize=True)
    checkerboard_preview(image, f"{ability_id}_preview.png")

qa_sheet(list(images.items()))

for ability_id, image in images.items():
    alpha = image.getchannel("A")
    bbox = alpha.getbbox()
    opaque = sum(1 for value in alpha.getdata() if value == 255)
    partial = sum(1 for value in alpha.getdata() if 0 < value < 255)
    print(f"{ability_id}: bbox={bbox}, opaque={opaque}, partial={partial}")

tea_chi_colors = set(images["tea_chi"].getdata())
flame_colors = sorted(color for color in tea_chi_colors if color in {FLAME_FILL, FLAME_OUTLINE})
print(f"tea_chi flame colors: {flame_colors}")
