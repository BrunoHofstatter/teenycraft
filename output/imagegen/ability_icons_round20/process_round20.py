from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parent
SIZE = 64


def fit_subject(
    source_name: str,
    target_extent: int,
    center: tuple[int, int] = (32, 32),
) -> Image.Image:
    source = Image.open(ROOT / source_name).convert("RGBA")

    # The production icons use binary alpha. Chroma removal happens before this
    # script so the preserved RGB pixels can be reduced with nearest-neighbor.
    cleaned = Image.new("RGBA", source.size, (0, 0, 0, 0))
    source_pixels = source.load()
    cleaned_pixels = cleaned.load()
    for y in range(source.height):
        for x in range(source.width):
            red, green, blue, alpha = source_pixels[x, y]
            if alpha >= 128:
                cleaned_pixels[x, y] = (red, green, blue, 255)

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


def draw_pixel_text(
    image: Image.Image,
    text: str,
    origin: tuple[int, int],
    color: tuple[int, int, int, int],
    scale: int = 2,
) -> None:
    glyphs = {
        "A": (
            "010",
            "101",
            "111",
            "101",
            "101",
        ),
        "H": (
            "101",
            "101",
            "111",
            "101",
            "101",
        ),
    }
    draw = ImageDraw.Draw(image)
    cursor_x = origin[0]
    for character in text:
        glyph = glyphs[character]
        for row, line in enumerate(glyph):
            for column, pixel in enumerate(line):
                if pixel == "1":
                    x = cursor_x + column * scale
                    y = origin[1] + row * scale
                    draw.rectangle(
                        (x, y, x + scale - 1, y + scale - 1),
                        fill=color,
                    )
        cursor_x += (len(glyph[0]) + 1) * scale


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


# The revision source composes the cannon in the lower-right half and the
# enlarged moving projectile in the upper-left half. Filling 59 pixels moves
# the cannon close to the corner while retaining a narrow transparent margin.
plasma_shot = fit_subject(
    "plasma_shot_revision_transparent_source.png",
    target_extent=59,
)
missile_barrage = fit_subject(
    "missile_barrage_transparent_source.png",
    target_extent=59,
)
evil_laugh = fit_subject(
    "evil_laugh_transparent_source.png",
    target_extent=45,
    center=(32, 32),
)

# The image generator is deliberately not trusted with exact lettering.
# These two small marks are authored on the final pixel grid.
ha_color = (126, 0, 30, 255)
draw_pixel_text(evil_laugh, "HA", (5, 4), ha_color)
draw_pixel_text(evil_laugh, "HA", (43, 50), ha_color)

outputs = {
    "ability_plasma_shot.png": plasma_shot,
    "ability_missile_barrage.png": missile_barrage,
    "ability_evil_laugh.png": evil_laugh,
}

for filename, image in outputs.items():
    image.save(ROOT / filename, optimize=True)

for stem, image in (
    ("plasma_shot", plasma_shot),
    ("missile_barrage", missile_barrage),
    ("evil_laugh", evil_laugh),
):
    checkerboard_preview(image, f"{stem}_preview.png")
    qa32_preview(image, f"{stem}_qa32.png")

qa_sheet(
    [
        ("Plasma Shot", plasma_shot),
        ("Missile Barrage", missile_barrage),
        ("Evil Laugh", evil_laugh),
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
