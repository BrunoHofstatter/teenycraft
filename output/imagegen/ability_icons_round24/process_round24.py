from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parent
SIZE = 64


def remove_magenta_fringe(image: Image.Image) -> Image.Image:
    cleaned = image.copy()
    pixels = cleaned.load()

    for y in range(cleaned.height):
        for x in range(cleaned.width):
            red, green, blue, alpha = pixels[x, y]
            if alpha == 0:
                continue

            # None of this round's subjects contains purple or magenta.
            # Remove both partially transparent matte pixels and opaque
            # chroma remnants before the nearest-neighbor reduction.
            if (
                red > 110
                and blue > 105
                and green < min(red, blue) * 0.72
                and abs(red - blue) < 105
            ):
                pixels[x, y] = (0, 0, 0, 0)

    return cleaned


def fit_subject(
    source_name: str,
    target_extent: int = 58,
    center: tuple[int, int] = (32, 32),
) -> Image.Image:
    source = Image.open(ROOT / source_name).convert("RGBA")
    source = remove_magenta_fringe(source)

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
    "ability_natures_wrath.png": fit_subject(
        "natures_wrath_transparent_source.png",
        target_extent=58,
    ),
    "ability_root.png": fit_subject(
        "root_transparent_source.png",
        target_extent=58,
    ),
    "ability_shockwave_stomp.png": fit_subject(
        "shockwave_stomp_transparent_source.png",
        target_extent=60,
        center=(32, 33),
    ),
}

for filename, image in outputs.items():
    image.save(ROOT / filename, optimize=True)

for stem, image in (
    ("natures_wrath", outputs["ability_natures_wrath.png"]),
    ("root", outputs["ability_root.png"]),
    ("shockwave_stomp", outputs["ability_shockwave_stomp.png"]),
):
    checkerboard_preview(image, f"{stem}_preview.png")
    qa32_preview(image, f"{stem}_qa32.png")

qa_sheet(
    [
        ("Nature's Wrath", outputs["ability_natures_wrath.png"]),
        ("Root", outputs["ability_root.png"]),
        ("Shockwave Stomp", outputs["ability_shockwave_stomp.png"]),
    ]
)

for filename, image in outputs.items():
    alpha = image.getchannel("A")
    bbox = alpha.getbbox()
    partial = sum(
        1 for value in alpha.get_flattened_data() if 0 < value < 255
    )
    magenta_pixels = sum(
        1
        for red, green, blue, value in image.get_flattened_data()
        if value > 0
        and red > 110
        and blue > 105
        and green < min(red, blue) * 0.72
        and abs(red - blue) < 105
    )
    corners = (
        image.getpixel((0, 0))[3],
        image.getpixel((63, 0))[3],
        image.getpixel((0, 63))[3],
        image.getpixel((63, 63))[3],
    )
    print(
        f"{filename}: mode={image.mode}, size={image.size}, bbox={bbox}, "
        f"partial_alpha={partial}, magenta_pixels={magenta_pixels}, "
        f"corner_alpha={corners}"
    )
