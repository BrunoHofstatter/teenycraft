from __future__ import annotations

import colorsys
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

            # Remove any residual green-key fringe before enforcing binary alpha.
            looks_like_key = green > 90 and green > red * 1.28 and green > blue * 1.28
            if alpha < 128 or looks_like_key:
                target_pixels[x, y] = (0, 0, 0, 0)
            else:
                target_pixels[x, y] = (red, green, blue, 255)

    return cleaned


def yellow_to_green(source: Image.Image) -> Image.Image:
    result = Image.new("RGBA", source.size, (0, 0, 0, 0))
    source_pixels = source.load()
    target_pixels = result.load()

    for y in range(source.height):
        for x in range(source.width):
            red, green, blue, alpha = source_pixels[x, y]
            if alpha == 0:
                continue

            hue, saturation, value = colorsys.rgb_to_hsv(red / 255, green / 255, blue / 255)
            is_yellow_energy = red >= 120 and green >= 75 and red > blue * 1.12 and green > blue * 1.08
            if is_yellow_energy and saturation >= 0.16:
                # Preserve the exact alpha and luminance structure while replacing
                # the yellow/gold hue family with a Green Lantern hue family.
                green_hue = 0.34
                new_saturation = min(1.0, saturation * 1.05)
                nr, ng, nb = colorsys.hsv_to_rgb(green_hue, new_saturation, value)
                target_pixels[x, y] = (round(nr * 255), round(ng * 255), round(nb * 255), 255)
            else:
                target_pixels[x, y] = (red, green, blue, 255)

    return result


def checkerboard_preview(image: Image.Image, output_name: str, scale: int = 8) -> None:
    width = image.width * scale
    height = image.height * scale
    checker = Image.new("RGBA", (width, height), (0, 0, 0, 255))
    draw = ImageDraw.Draw(checker)
    tile = scale * 2
    colors = ((70, 70, 74, 255), (106, 106, 112, 255))
    for y in range(0, height, tile):
        for x in range(0, width, tile):
            parity = ((x // tile) + (y // tile)) % 2
            draw.rectangle((x, y, x + tile - 1, y + tile - 1), fill=colors[parity])

    enlarged = image.resize((width, height), Image.Resampling.NEAREST)
    checker.alpha_composite(enlarged)
    checker.convert("RGB").save(ROOT / output_name)


def qa_strip(images: list[tuple[str, Image.Image]]) -> None:
    cell_width = 64
    gap = 12
    row_height = 88
    canvas = Image.new("RGBA", (len(images) * (cell_width + gap) + gap, row_height * 3), (44, 44, 48, 255))

    for column, (_, image) in enumerate(images):
        left = gap + column * (cell_width + gap)
        for row, qa_size in enumerate((64, 32, 16)):
            reduced = image.resize((qa_size, qa_size), Image.Resampling.NEAREST)
            top = row * row_height + (64 - qa_size) // 2 + 8
            checker = Image.new("RGBA", (qa_size, qa_size), (0, 0, 0, 0))
            checker_draw = ImageDraw.Draw(checker)
            tile = max(2, qa_size // 8)
            for y in range(0, qa_size, tile):
                for x in range(0, qa_size, tile):
                    shade = (76, 76, 82, 255) if ((x // tile) + (y // tile)) % 2 == 0 else (112, 112, 118, 255)
                    checker_draw.rectangle((x, y, x + tile - 1, y + tile - 1), fill=shade)
            checker.alpha_composite(reduced)
            canvas.alpha_composite(checker, (left + (64 - qa_size) // 2, top))

    canvas.convert("RGB").save(ROOT / "ability_icons_qa.png")


counterattack = clean_and_resize("counterattack_transparent.png")
kiss = clean_and_resize("kiss_shared_transparent.png")
beam_yellow = clean_and_resize("construct_beam_yellow_transparent.png")
beam_green = yellow_to_green(beam_yellow)

outputs = {
    "ability_counterattack.png": counterattack,
    "ability_puddin_pucker.png": kiss,
    "ability_deadly_kiss.png": kiss,
    "ability_macho_smooch.png": kiss,
    "ability_construct_beam.png": beam_yellow,
    "ability_construct_beam_green.png": beam_green,
}

for filename, image in outputs.items():
    image.save(ROOT / filename, optimize=True)

for filename, image in (
    ("counterattack_preview.png", counterattack),
    ("kiss_shared_preview.png", kiss),
    ("construct_beam_yellow_preview.png", beam_yellow),
    ("construct_beam_green_preview.png", beam_green),
):
    checkerboard_preview(image, filename)

qa_strip([
    ("counterattack", counterattack),
    ("kiss", kiss),
    ("beam_yellow", beam_yellow),
    ("beam_green", beam_green),
])

