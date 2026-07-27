from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parent
SIZE = 64


def is_magenta_fringe(red: int, green: int, blue: int) -> bool:
    return red > 165 and blue > 135 and green < min(red, blue) * 0.68


def fit_subject(
    source_name: str,
    target_extent: int,
    center: tuple[int, int] = (32, 32),
) -> Image.Image:
    source = Image.open(ROOT / source_name).convert("RGBA")
    cleaned = Image.new("RGBA", source.size, (0, 0, 0, 0))
    source_pixels = source.load()
    target_pixels = cleaned.load()

    for y in range(source.height):
        for x in range(source.width):
            red, green, blue, alpha = source_pixels[x, y]
            if alpha < 128 or is_magenta_fringe(red, green, blue):
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
    return hard_alpha(final)


def hard_alpha(image: Image.Image) -> Image.Image:
    result = Image.new("RGBA", image.size, (0, 0, 0, 0))
    source = image.convert("RGBA")
    source_pixels = source.load()
    target_pixels = result.load()

    for y in range(source.height):
        for x in range(source.width):
            red, green, blue, alpha = source_pixels[x, y]
            if alpha >= 128:
                target_pixels[x, y] = (red, green, blue, 255)
    return result


def normalize_soul_fist(source: Image.Image, fill: tuple[int, int, int]) -> Image.Image:
    alpha = source.getchannel("A")
    source_pixels = source.load()
    result = Image.new("RGBA", source.size, (0, 0, 0, 0))
    target_pixels = result.load()

    for y in range(source.height):
        for x in range(source.width):
            if alpha.getpixel((x, y)) == 0:
                continue

            red, green, blue, _ = source_pixels[x, y]
            light_detail = min(red, green, blue) >= 118 and max(red, green, blue) - min(red, green, blue) <= 55
            if not light_detail:
                target_pixels[x, y] = (*fill, 255)
                continue

            target_pixels[x, y] = (255, 255, 255, 255)

    return result


def limited_palette(source: Image.Image, colors: int) -> Image.Image:
    alpha = source.getchannel("A")
    quantized = source.quantize(
        colors=colors,
        method=Image.Quantize.FASTOCTREE,
        dither=Image.Dither.NONE,
    ).convert("RGBA")
    quantized.putalpha(alpha)
    return hard_alpha(quantized)


def lift_dark_readability(source: Image.Image) -> Image.Image:
    result = Image.new("RGBA", source.size, (0, 0, 0, 0))
    source_pixels = source.load()
    target_pixels = result.load()

    for y in range(source.height):
        for x in range(source.width):
            red, green, blue, alpha = source_pixels[x, y]
            if alpha == 0:
                continue

            if red >= 150 and red > green * 1.7 and red > blue * 1.7:
                target_pixels[x, y] = (red, green, blue, 255)
                continue

            peak = max(red, green, blue)
            if peak < 20:
                target_pixels[x, y] = (8, 9, 12, 255)
                continue

            factor = 1.65 if peak < 70 else 1.32 if peak < 125 else 1.12
            target_pixels[x, y] = (
                min(255, round(red * factor)),
                min(255, round(green * factor)),
                min(255, round(blue * factor)),
                255,
            )

    return result


def add_batman_edge_highlight(source: Image.Image) -> Image.Image:
    result = source.copy()
    alpha = source.getchannel("A")
    source_pixels = source.load()
    target_pixels = result.load()

    for y in range(source.height):
        for x in range(source.width):
            red, green, blue, pixel_alpha = source_pixels[x, y]
            if pixel_alpha == 0 or max(red, green, blue) >= 105:
                continue

            touches_transparency = any(
                nx < 0
                or ny < 0
                or nx >= source.width
                or ny >= source.height
                or alpha.getpixel((nx, ny)) == 0
                for ny in range(y - 1, y + 2)
                for nx in range(x - 1, x + 2)
                if not (nx == x and ny == y)
            )
            if touches_transparency:
                target_pixels[x, y] = (55, 65, 78, 255)

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


def qa_sheet(images: list[Image.Image]) -> None:
    cell = 256
    gap = 16
    canvas = Image.new(
        "RGBA",
        (gap + len(images) * (cell + gap), gap + 3 * (cell + gap)),
        (38, 38, 42, 255),
    )

    for column, image in enumerate(images):
        left = gap + column * (cell + gap)
        for row, qa_size in enumerate((64, 32, 16)):
            top = gap + row * (cell + gap)
            backing = checkerboard((cell, cell), 16)
            reduced = image.resize((qa_size, qa_size), Image.Resampling.NEAREST)
            enlarged = reduced.resize((cell, cell), Image.Resampling.NEAREST)
            backing.alpha_composite(enlarged)
            canvas.alpha_composite(backing, (left, top))

    canvas.convert("RGB").save(ROOT / "ability_icons_qa.png", optimize=True)


soul_source = fit_subject(
    "soul_punch_raven_transparent_source_v2.png",
    target_extent=56,
)
soul_raven = normalize_soul_fist(soul_source, (0, 0, 0))
soul_trigon = normalize_soul_fist(soul_source, (239, 16, 28))
grappling_hook = limited_palette(
    add_batman_edge_highlight(
        lift_dark_readability(
            fit_subject(
                "grappling_hook_transparent_source.png",
                target_extent=59,
            )
        )
    ),
    colors=20,
)
jiu_jitsu = limited_palette(
    fit_subject(
        "jiu_jitsu_transparent_source.png",
        target_extent=60,
    ),
    colors=12,
)

outputs = {
    "ability_soul_punch.png": soul_raven,
    "ability_soul_punch_trigon.png": soul_trigon,
    "ability_grappling_hook.png": grappling_hook,
    "ability_jiu_jitsu.png": jiu_jitsu,
}

for filename, image in outputs.items():
    image.save(ROOT / filename, optimize=True)

for filename, image in (
    ("soul_punch_raven_preview.png", soul_raven),
    ("soul_punch_trigon_preview.png", soul_trigon),
    ("grappling_hook_preview.png", grappling_hook),
    ("jiu_jitsu_preview.png", jiu_jitsu),
):
    checkerboard_preview(image, filename)

qa_sheet([soul_raven, soul_trigon, grappling_hook, jiu_jitsu])

for filename, image in outputs.items():
    alpha = image.getchannel("A")
    bbox = alpha.getbbox()
    partial = sum(1 for value in alpha.get_flattened_data() if 0 < value < 255)
    print(f"{filename}: mode={image.mode}, size={image.size}, bbox={bbox}, partial_alpha={partial}")

print(
    "soul geometry identical:",
    soul_raven.getchannel("A").tobytes() == soul_trigon.getchannel("A").tobytes(),
)
