from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parent
SIZE = 64


def clean_and_resize(source_name: str) -> Image.Image:
    source = Image.open(ROOT / source_name).convert("RGBA")
    resized = source.resize((SIZE, SIZE), Image.Resampling.NEAREST)
    cleaned = Image.new("RGBA", resized.size, (0, 0, 0, 0))

    source_pixels = resized.load()
    target_pixels = cleaned.load()
    for y in range(SIZE):
        for x in range(SIZE):
            red, green, blue, alpha = source_pixels[x, y]

            # Enforce the production binary-alpha contract and remove any
            # remaining green-key fringe after the large-source despill pass.
            looks_like_key = green > 80 and green > red * 1.20 and green > blue * 1.20
            if alpha < 128 or looks_like_key:
                target_pixels[x, y] = (0, 0, 0, 0)
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
    enlarged = image.resize(canvas.size, Image.Resampling.NEAREST)
    canvas.alpha_composite(enlarged)
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
    "hooded_barrage": clean_and_resize("hooded_barrage_transparent_source.png"),
    "hooded_void": clean_and_resize("hooded_void_transparent_source.png"),
    "mind_control": clean_and_resize("mind_control_transparent_source_v2.png"),
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
