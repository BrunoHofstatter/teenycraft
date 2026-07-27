from __future__ import annotations

import colorsys
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parent
SIZE = 64


def clean_and_resize() -> Image.Image:
    source = Image.open(ROOT / "construct_beam_45deg_yellow_transparent.png").convert("RGBA")
    resized = source.resize((SIZE, SIZE), Image.Resampling.NEAREST)
    output = Image.new("RGBA", resized.size, (0, 0, 0, 0))
    source_pixels = resized.load()
    output_pixels = output.load()

    for y in range(SIZE):
        for x in range(SIZE):
            red, green, blue, alpha = source_pixels[x, y]
            chroma_fringe = green > 90 and green > red * 1.28 and green > blue * 1.28
            if alpha >= 128 and not chroma_fringe:
                output_pixels[x, y] = (red, green, blue, 255)

    return output


def derive_green(source: Image.Image) -> Image.Image:
    output = Image.new("RGBA", source.size, (0, 0, 0, 0))
    source_pixels = source.load()
    output_pixels = output.load()

    for y in range(source.height):
        for x in range(source.width):
            red, green, blue, alpha = source_pixels[x, y]
            if alpha == 0:
                continue

            hue, saturation, value = colorsys.rgb_to_hsv(red / 255, green / 255, blue / 255)
            yellow_energy = red >= 120 and green >= 75 and red > blue * 1.12 and green > blue * 1.08
            if yellow_energy and saturation >= 0.16:
                nr, ng, nb = colorsys.hsv_to_rgb(0.34, min(1.0, saturation * 1.05), value)
                output_pixels[x, y] = (round(nr * 255), round(ng * 255), round(nb * 255), 255)
            else:
                output_pixels[x, y] = (red, green, blue, 255)

    return output


def checkerboard(image: Image.Image, size: int) -> Image.Image:
    result = Image.new("RGBA", (size, size), (0, 0, 0, 255))
    draw = ImageDraw.Draw(result)
    tile = max(2, size // 8)
    for y in range(0, size, tile):
        for x in range(0, size, tile):
            shade = (70, 70, 74, 255) if ((x // tile) + (y // tile)) % 2 == 0 else (106, 106, 112, 255)
            draw.rectangle((x, y, x + tile - 1, y + tile - 1), fill=shade)
    result.alpha_composite(image.resize((size, size), Image.Resampling.NEAREST))
    return result


yellow = clean_and_resize()
green = derive_green(yellow)

yellow.save(ROOT / "ability_construct_beam.png", optimize=True)
green.save(ROOT / "ability_construct_beam_green.png", optimize=True)

for name, image in (("yellow", yellow), ("green", green)):
    checkerboard(image, 512).convert("RGB").save(ROOT / f"construct_beam_45deg_{name}_preview.png")

qa = Image.new("RGB", (304, 88), (44, 44, 48))
for column, image in enumerate((yellow, green)):
    for index, qa_size in enumerate((64, 32, 16)):
        left = 12 + column * 152 + index * 44
        top = (88 - qa_size) // 2
        qa.paste(checkerboard(image, qa_size).convert("RGB"), (left, top))
qa.save(ROOT / "construct_beam_45deg_qa.png")

