from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parent
SIZE = 64


def fit_subject(
    source_name: str,
    target_extent: int = 58,
    center: tuple[int, int] = (32, 32),
) -> Image.Image:
    source = Image.open(ROOT / source_name).convert("RGBA")
    source_pixels = source.load()
    for y in range(source.height):
        for x in range(source.width):
            red, green, blue, alpha = source_pixels[x, y]
            is_magenta_fringe = (
                alpha > 0
                and red > 120
                and blue > 120
                and green < min(red, blue) * 0.65
            )
            if is_magenta_fringe:
                source_pixels[x, y] = (0, 0, 0, 0)

    bbox = source.getchannel("A").getbbox()
    if bbox is None:
        raise ValueError(f"No subject found in {source_name}")

    subject = source.crop(bbox)
    scale = target_extent / max(subject.size)
    resized_size = (
        max(1, round(subject.width * scale)),
        max(1, round(subject.height * scale)),
    )
    subject = subject.resize(resized_size, Image.Resampling.NEAREST)

    # Production ability icons use a crisp binary alpha silhouette.
    alpha = subject.getchannel("A").point(lambda value: 255 if value >= 128 else 0)
    subject.putalpha(alpha)

    final = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    left = center[0] - subject.width // 2
    top = center[1] - subject.height // 2
    final.alpha_composite(subject, (left, top))
    return final


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
            draw.text(
                (left, top),
                f"{label} - {qa_size}x{qa_size}",
                fill=(235, 235, 238, 255),
            )
            backing = checkerboard((cell, cell), 16)
            reduced = image.resize((qa_size, qa_size), Image.Resampling.NEAREST)
            backing.alpha_composite(
                reduced.resize((cell, cell), Image.Resampling.NEAREST)
            )
            canvas.alpha_composite(backing, (left, top + label_height))

    canvas.convert("RGB").save(ROOT / "ability_icons_qa.png", optimize=True)


outputs = {
    "ability_flight.png": fit_subject("flight_transparent_source.png"),
    "ability_double_slam.png": fit_subject("double_slam_transparent_source.png"),
    "ability_lightning_mace.png": fit_subject(
        "lightning_mace_transparent_source.png"
    ),
}

for filename, image in outputs.items():
    image.save(ROOT / filename, optimize=True)

for stem, image in (
    ("flight", outputs["ability_flight.png"]),
    ("double_slam", outputs["ability_double_slam.png"]),
    ("lightning_mace", outputs["ability_lightning_mace.png"]),
):
    checkerboard_preview(image, f"{stem}_preview.png")
    qa32_preview(image, f"{stem}_qa32.png")

qa_sheet(
    [
        ("Flight", outputs["ability_flight.png"]),
        ("Double Slam", outputs["ability_double_slam.png"]),
        ("Lightning Mace", outputs["ability_lightning_mace.png"]),
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
