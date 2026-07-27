from __future__ import annotations

import colorsys
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parent
SIZE = 64


def is_magenta_fringe(red: int, green: int, blue: int) -> bool:
    return red > 170 and blue > 140 and green < min(red, blue) * 0.62


def is_green_fringe(red: int, green: int, blue: int) -> bool:
    return green > 90 and green > red * 1.25 and green > blue * 1.20


def fit_subject(
    source_name: str,
    target_extent: int,
    key_family: str,
    center: tuple[int, int] = (32, 32),
) -> Image.Image:
    source = Image.open(ROOT / source_name).convert("RGBA")
    cleaned = Image.new("RGBA", source.size, (0, 0, 0, 0))
    source_pixels = source.load()
    target_pixels = cleaned.load()

    for y in range(source.height):
        for x in range(source.width):
            red, green, blue, alpha = source_pixels[x, y]
            fringe = (
                is_magenta_fringe(red, green, blue)
                if key_family == "magenta"
                else is_green_fringe(red, green, blue)
            )
            if alpha < 128 or fringe:
                continue
            target_pixels[x, y] = (red, green, blue, 255)

    bbox = cleaned.getchannel("A").getbbox()
    if bbox is None:
        raise ValueError(f"No opaque subject found in {source_name}")

    subject = cleaned.crop(bbox)
    scale = target_extent / max(subject.size)
    resized_size = (
        max(1, round(subject.width * scale)),
        max(1, round(subject.height * scale)),
    )
    subject = subject.resize(resized_size, Image.Resampling.NEAREST)

    final = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    left = center[0] - subject.width // 2
    top = center[1] - subject.height // 2
    final.alpha_composite(subject, (left, top))
    return final


def blue_whale_to_green(source: Image.Image) -> Image.Image:
    result = Image.new("RGBA", source.size, (0, 0, 0, 0))
    source_pixels = source.load()
    target_pixels = result.load()

    for y in range(source.height):
        for x in range(source.width):
            red, green, blue, alpha = source_pixels[x, y]
            if alpha == 0:
                continue

            hue, saturation, value = colorsys.rgb_to_hsv(
                red / 255.0,
                green / 255.0,
                blue / 255.0,
            )

            if 0.50 <= hue <= 0.72 and saturation >= 0.18:
                new_hue = 0.34
                new_saturation = min(1.0, max(0.38, saturation * 0.95))
                nr, ng, nb = colorsys.hsv_to_rgb(new_hue, new_saturation, value)
                target_pixels[x, y] = (
                    round(nr * 255),
                    round(ng * 255),
                    round(nb * 255),
                    255,
                )
            elif 0.07 <= hue <= 0.20 and saturation >= 0.10:
                new_hue = 0.25
                new_saturation = min(0.58, max(0.24, saturation * 0.90))
                nr, ng, nb = colorsys.hsv_to_rgb(new_hue, new_saturation, value)
                target_pixels[x, y] = (
                    round(nr * 255),
                    round(ng * 255),
                    round(nb * 255),
                    255,
                )
            else:
                target_pixels[x, y] = (red, green, blue, 255)

    return result


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


starfish = fit_subject(
    "starfish_chuck_transparent_source.png",
    target_extent=38,
    key_family="magenta",
)
whale_blue = fit_subject(
    "whale_drop_blue_transparent_source.png",
    target_extent=52,
    key_family="magenta",
)
whale_green = blue_whale_to_green(whale_blue)
curse = fit_subject(
    "curse_transparent_source.png",
    target_extent=51,
    key_family="green",
)

outputs = {
    "ability_starfish_chuck.png": starfish,
    "ability_whale_drop.png": whale_blue,
    "ability_whale_drop_green.png": whale_green,
    "ability_curse.png": curse,
}

for filename, image in outputs.items():
    image.save(ROOT / filename, optimize=True)

for filename, image in (
    ("starfish_chuck_preview.png", starfish),
    ("whale_drop_blue_preview.png", whale_blue),
    ("whale_drop_green_preview.png", whale_green),
    ("curse_preview.png", curse),
):
    checkerboard_preview(image, filename)

qa_sheet(
    [
        ("starfish_chuck", starfish),
        ("whale_drop", whale_blue),
        ("whale_drop_green", whale_green),
        ("curse", curse),
    ]
)

for filename, image in outputs.items():
    alpha = image.getchannel("A")
    bbox = alpha.getbbox()
    partial = sum(1 for value in alpha.getdata() if 0 < value < 255)
    print(f"{filename}: mode={image.mode}, size={image.size}, bbox={bbox}, partial_alpha={partial}")

print(
    "whale geometry identical:",
    whale_blue.getchannel("A").tobytes() == whale_green.getchannel("A").tobytes(),
)
