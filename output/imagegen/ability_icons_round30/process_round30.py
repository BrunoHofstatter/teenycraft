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

    # Chroma removal leaves a soft high-resolution matte. Collapse it before
    # nearest-neighbor reduction so the production sprite uses crisp alpha.
    alpha = source.getchannel("A").point(lambda value: 255 if value >= 128 else 0)
    source.putalpha(alpha)

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
    # Match the established base Shuriken's compact in-hand footprint while
    # preserving this variant's distinct long four-point silhouette.
    "ability_shuriken__variant_red.png": fit_subject(
        "shuriken_red_transparent_source.png",
        target_extent=46,
        center=(32, 31),
    ),
    # All Super Dash treatments share the same 58-pixel authoring footprint.
    "ability_super_dash.png": fit_subject(
        "super_dash_purple_transparent_source.png",
        target_extent=58,
    ),
    "ability_super_dash__variant_red.png": fit_subject(
        "super_dash_red_transparent_source.png",
        target_extent=58,
    ),
    "ability_super_dash__variant_black.png": fit_subject(
        "super_dash_black_transparent_source.png",
        target_extent=58,
    ),
}


def lock_alpha_to_canonical(
    image: Image.Image,
    canonical: Image.Image,
) -> Image.Image:
    """Use one exact silhouette for every Super Dash color treatment."""
    result = image.copy()
    source_alpha = image.getchannel("A")
    canonical_alpha = canonical.getchannel("A")

    for y in range(SIZE):
        for x in range(SIZE):
            if canonical_alpha.getpixel((x, y)) == 0:
                result.putpixel((x, y), (0, 0, 0, 0))
                continue
            if source_alpha.getpixel((x, y)) != 0:
                r, g, b, _ = result.getpixel((x, y))
                result.putpixel((x, y), (r, g, b, 255))
                continue

            # Edits can differ from the master by a pixel or two after chroma
            # removal. Fill those pixels from the nearest colored neighbor.
            for radius in range(1, SIZE):
                candidates: list[tuple[int, int]] = []
                for dy in range(-radius, radius + 1):
                    dx = radius - abs(dy)
                    candidates.append((x - dx, y + dy))
                    if dx:
                        candidates.append((x + dx, y + dy))
                found = None
                for sample_x, sample_y in candidates:
                    if not (0 <= sample_x < SIZE and 0 <= sample_y < SIZE):
                        continue
                    if source_alpha.getpixel((sample_x, sample_y)):
                        found = result.getpixel((sample_x, sample_y))[:3]
                        break
                if found is not None:
                    result.putpixel((x, y), (*found, 255))
                    break
    return result


canonical_dash = outputs["ability_super_dash.png"]
outputs["ability_super_dash__variant_red.png"] = lock_alpha_to_canonical(
    outputs["ability_super_dash__variant_red.png"], canonical_dash
)
outputs["ability_super_dash__variant_black.png"] = lock_alpha_to_canonical(
    outputs["ability_super_dash__variant_black.png"], canonical_dash
)

for filename, image in outputs.items():
    image.save(ROOT / filename, optimize=True)

for stem, filename in (
    ("shuriken_red", "ability_shuriken__variant_red.png"),
    ("super_dash_purple", "ability_super_dash.png"),
    ("super_dash_red", "ability_super_dash__variant_red.png"),
    ("super_dash_black", "ability_super_dash__variant_black.png"),
):
    image = outputs[filename]
    checkerboard_preview(image, f"{stem}_preview.png")
    qa32_preview(image, f"{stem}_qa32.png")

qa_sheet(
    [
        ("Shuriken Red", outputs["ability_shuriken__variant_red.png"]),
        ("Dash Purple", outputs["ability_super_dash.png"]),
        ("Dash Red", outputs["ability_super_dash__variant_red.png"]),
        ("Dash Black", outputs["ability_super_dash__variant_black.png"]),
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
