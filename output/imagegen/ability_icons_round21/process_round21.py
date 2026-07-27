from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter


ROOT = Path(__file__).resolve().parent
SIZE = 64


def fit_subject(
    source_name: str,
    target_extent: int = 58,
    center: tuple[int, int] = (32, 32),
) -> Image.Image:
    source = Image.open(ROOT / source_name).convert("RGBA")

    # Chroma removal is deliberately completed before reduction. The final
    # pixel grid uses a hard subject silhouette; intentional material alpha is
    # applied later and is not inherited from antialiased source edges.
    cleaned = Image.new("RGBA", source.size, (0, 0, 0, 0))
    source_pixels = source.load()
    cleaned_pixels = cleaned.load()
    for y in range(source.height):
        for x in range(source.width):
            red, green, blue, alpha = source_pixels[x, y]
            is_magenta = (
                red > 120
                and blue > 120
                and green < min(red, blue) * 0.6
            )
            if alpha >= 128 and not is_magenta:
                cleaned_pixels[x, y] = (red, green, blue, 255)

    bbox = cleaned.getchannel("A").getbbox()
    if bbox is None:
        raise ValueError(f"No subject found in {source_name}")

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


def apply_shield_alpha(
    image: Image.Image,
    minimum_alpha: int,
    maximum_alpha: int,
) -> Image.Image:
    output = image.copy()
    alpha = image.getchannel("A")

    # Preserve an opaque, approximately four-pixel outer ring. Everything
    # farther inside becomes deliberately translucent while retaining broad
    # generated highlight and shade clusters through luminance-based alpha.
    eroded = alpha.filter(ImageFilter.MinFilter(9))
    source_pixels = image.load()
    output_pixels = output.load()
    eroded_pixels = eroded.load()

    for y in range(SIZE):
        for x in range(SIZE):
            red, green, blue, source_alpha = source_pixels[x, y]
            if source_alpha == 0:
                continue
            if eroded_pixels[x, y] == 0:
                output_pixels[x, y] = (red, green, blue, 255)
                continue

            luminance = (red * 299 + green * 587 + blue * 114) // 1000
            material_alpha = minimum_alpha + (
                (maximum_alpha - minimum_alpha) * luminance // 255
            )
            output_pixels[x, y] = (red, green, blue, material_alpha)

    return output


def apply_science_alpha(image: Image.Image) -> Image.Image:
    output = image.copy()
    source_pixels = image.load()
    output_pixels = output.load()

    for y in range(SIZE):
        for x in range(SIZE):
            red, green, blue, alpha = source_pixels[x, y]
            if alpha == 0:
                continue

            # Neon liquid and all three green bubbles remain opaque.
            if green > red * 1.25 and green > blue * 1.25:
                output_pixels[x, y] = (red, green, blue, 255)
                continue

            luminance = (red * 299 + green * 587 + blue * 114) // 1000

            # The darker teal glass outline remains opaque.
            if luminance < 150:
                output_pixels[x, y] = (red, green, blue, 255)
            # Bright reflections remain strong but still read as glass.
            elif luminance > 235:
                output_pixels[x, y] = (red, green, blue, 220)
            else:
                glass_alpha = 88 + (luminance - 150) * 52 // 85
                output_pixels[x, y] = (red, green, blue, glass_alpha)

    return output


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


light_shield = apply_shield_alpha(
    fit_subject("light_shield_revision_source.png"),
    minimum_alpha=68,
    maximum_alpha=132,
)
burp_shield = apply_shield_alpha(
    fit_subject("burp_shield_transparent_source.png"),
    minimum_alpha=118,
    maximum_alpha=190,
)
science = apply_science_alpha(
    fit_subject("science_transparent_source.png", target_extent=58),
)

outputs = {
    "ability_light_shield.png": light_shield,
    "ability_burp_shield.png": burp_shield,
    "ability_science.png": science,
}

for filename, image in outputs.items():
    image.save(ROOT / filename, optimize=True)

for stem, image in (
    ("light_shield", light_shield),
    ("burp_shield", burp_shield),
    ("science", science),
):
    checkerboard_preview(image, f"{stem}_preview.png")
    qa32_preview(image, f"{stem}_qa32.png")

qa_sheet(
    [
        ("Light Shield", light_shield),
        ("Burp Shield", burp_shield),
        ("Science", science),
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
