from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parent
SIZE = 64


def resize_full_canvas(source_name: str, chroma: str) -> Image.Image:
    source = Image.open(ROOT / source_name).convert("RGBA")

    # Lock the extracted source to binary alpha before nearest-neighbor reduction.
    alpha = source.getchannel("A").point(lambda value: 255 if value >= 128 else 0)
    source.putalpha(alpha)
    result = source.resize((SIZE, SIZE), Image.Resampling.NEAREST)

    # Remove any final chroma-colored edge pixels that survived extraction.
    cleaned = result.copy()
    for y in range(SIZE):
        for x in range(SIZE):
            red, green, blue, value = cleaned.getpixel((x, y))
            if not value:
                continue
            if chroma == "magenta":
                fringe = red > 120 and blue > 120 and red > green * 1.5 and blue > green * 1.5
            elif chroma == "green":
                fringe = green > 100 and green > red * 1.35 and green > blue * 1.35
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
    "ability_soopah_laser.png": resize_full_canvas(
        "soopah_laser_transparent_source.png", "magenta"
    ),
    "ability_umbrella_shot.png": resize_full_canvas(
        "umbrella_shot_transparent_source.png", "green"
    ),
    "ability_telekinesis.png": resize_full_canvas(
        "telekinesis_transparent_source.png", "magenta"
    ),
}

for filename, image in outputs.items():
    image.save(ROOT / filename, optimize=True)

for stem, filename in (
    ("soopah_laser", "ability_soopah_laser.png"),
    ("umbrella_shot", "ability_umbrella_shot.png"),
    ("telekinesis", "ability_telekinesis.png"),
):
    image = outputs[filename]
    checkerboard_preview(image, f"{stem}_preview.png")
    qa32_preview(image, f"{stem}_qa32.png")

qa_sheet(
    [
        ("Soopah Laser", outputs["ability_soopah_laser.png"]),
        ("Umbrella Shot", outputs["ability_umbrella_shot.png"]),
        ("Telekinesis", outputs["ability_telekinesis.png"]),
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
