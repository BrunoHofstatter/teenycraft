from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parent
SIZE = 64


def resize_source(source_name: str, chroma: str, inset: int = 0) -> Image.Image:
    source = Image.open(ROOT / source_name).convert("RGBA")

    # Generated pixel art is reduced with binary alpha and nearest-neighbor only.
    alpha = source.getchannel("A").point(lambda value: 255 if value >= 128 else 0)
    source.putalpha(alpha)

    target_size = SIZE - inset * 2
    reduced = source.resize((target_size, target_size), Image.Resampling.NEAREST)
    result = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    result.alpha_composite(reduced, (inset, inset))

    # Remove any chroma-colored pixels that survived extraction and reduction.
    cleaned = result.copy()
    for y in range(SIZE):
        for x in range(SIZE):
            red, green, blue, value = cleaned.getpixel((x, y))
            if not value:
                continue
            if chroma == "green":
                fringe = green > 100 and green > red * 1.35 and green > blue * 1.35
            elif chroma == "cyan":
                fringe = (
                    red < 120
                    and green > 110
                    and blue > 110
                    and green > red * 1.35
                    and blue > red * 1.35
                )
            else:
                raise ValueError(f"Unsupported chroma: {chroma}")
            if fringe:
                cleaned.putpixel((x, y), (0, 0, 0, 0))

    return cleaned


def checkerboard(size: tuple[int, int], tile: int) -> Image.Image:
    image = Image.new("RGBA", size, (0, 0, 0, 255))
    draw = ImageDraw.Draw(image)
    colors = ((66, 66, 72, 255), (106, 106, 114, 255))
    for y in range(0, size[1], tile):
        for x in range(0, size[0], tile):
            draw.rectangle(
                (x, y, x + tile - 1, y + tile - 1),
                fill=colors[((x // tile) + (y // tile)) % 2],
            )
    return image


def checkerboard_preview(image: Image.Image, output_name: str) -> None:
    canvas = checkerboard((512, 512), 16)
    canvas.alpha_composite(image.resize((512, 512), Image.Resampling.NEAREST))
    canvas.convert("RGB").save(ROOT / output_name, optimize=True)


def qa32_preview(image: Image.Image, output_name: str) -> None:
    reduced = image.resize((32, 32), Image.Resampling.NEAREST)
    canvas = checkerboard((256, 256), 16)
    canvas.alpha_composite(reduced.resize((256, 256), Image.Resampling.NEAREST))
    canvas.convert("RGB").save(ROOT / output_name, optimize=True)


def qa_sheet(images: list[tuple[str, Image.Image]]) -> None:
    cell = 256
    gap = 16
    label_height = 22
    canvas = Image.new(
        "RGBA",
        (
            gap + len(images) * (cell + gap),
            gap + 2 * (cell + label_height + gap),
        ),
        (38, 38, 42, 255),
    )
    draw = ImageDraw.Draw(canvas)

    for column, (label, image) in enumerate(images):
        left = gap + column * (cell + gap)
        for row, qa_size in enumerate((64, 32)):
            top = gap + row * (cell + label_height + gap)
            draw.text((left, top), f"{label} - {qa_size}x{qa_size}", fill=(235, 235, 238, 255))
            backing = checkerboard((cell, cell), 16)
            reduced = image.resize((qa_size, qa_size), Image.Resampling.NEAREST)
            backing.alpha_composite(reduced.resize((cell, cell), Image.Resampling.NEAREST))
            canvas.alpha_composite(backing, (left, top + label_height))

    canvas.convert("RGB").save(ROOT / "ability_icons_qa.png", optimize=True)


outputs = {
    "ability_plasma_shot__variant_gun.png": resize_source(
        "plasma_shot_gun_transparent_source.png", "cyan"
    ),
    # The source reaches both diagonal corners, so a four-pixel inset preserves
    # transparent hand/UI padding without changing its approved composition.
    "ability_laser_eyes_drlight.png": resize_source(
        "laser_eyes_drlight_transparent_source.png", "green", inset=4
    ),
    "ability_boulder_toss.png": resize_source(
        "boulder_toss_transparent_source.png", "green"
    ),
}

for filename, image in outputs.items():
    image.save(ROOT / filename, optimize=True)

for stem, filename in (
    ("plasma_shot_gun", "ability_plasma_shot__variant_gun.png"),
    ("laser_eyes_drlight", "ability_laser_eyes_drlight.png"),
    ("boulder_toss", "ability_boulder_toss.png"),
):
    image = outputs[filename]
    checkerboard_preview(image, f"{stem}_preview.png")
    qa32_preview(image, f"{stem}_qa32.png")

qa_sheet(
    [
        ("Plasma Gun", outputs["ability_plasma_shot__variant_gun.png"]),
        ("Dr. Light", outputs["ability_laser_eyes_drlight.png"]),
        ("Boulder Toss", outputs["ability_boulder_toss.png"]),
    ]
)

for filename, image in outputs.items():
    alpha = image.getchannel("A")
    bbox = alpha.getbbox()
    partial = sum(1 for value in alpha.get_flattened_data() if 0 < value < 255)
    corners = (
        image.getpixel((0, 0))[3],
        image.getpixel((63, 0))[3],
        image.getpixel((0, 63))[3],
        image.getpixel((63, 63))[3],
    )
    print(
        f"{filename}: mode={image.mode}, size={image.size}, bbox={bbox}, "
        f"partial_alpha={partial}, corner_alpha={corners}"
    )
