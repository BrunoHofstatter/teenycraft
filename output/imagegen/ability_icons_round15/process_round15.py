from __future__ import annotations

from collections import deque
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parent
SIZE = 64


def is_magenta_fringe(red: int, green: int, blue: int) -> bool:
    return red > 145 and blue > 135 and green < min(red, blue) * 0.72


def remove_small_components(image: Image.Image, minimum_area: int = 200) -> Image.Image:
    pixels = image.load()
    width, height = image.size
    visited = bytearray(width * height)

    for start_y in range(height):
        for start_x in range(width):
            start_index = start_y * width + start_x
            if visited[start_index] or pixels[start_x, start_y][3] == 0:
                continue

            queue = deque([(start_x, start_y)])
            visited[start_index] = 1
            component: list[tuple[int, int]] = []

            while queue:
                x, y = queue.popleft()
                component.append((x, y))
                for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
                    if nx < 0 or nx >= width or ny < 0 or ny >= height:
                        continue
                    index = ny * width + nx
                    if visited[index] or pixels[nx, ny][3] == 0:
                        continue
                    visited[index] = 1
                    queue.append((nx, ny))

            if len(component) < minimum_area:
                for x, y in component:
                    pixels[x, y] = (0, 0, 0, 0)

    return image


def fit_subject(
    source_name: str,
    target_extent: int = 58,
    center: tuple[int, int] = (32, 32),
) -> Image.Image:
    source = Image.open(ROOT / source_name).convert("RGBA")
    cleaned = Image.new("RGBA", source.size, (0, 0, 0, 0))
    source_pixels = source.load()
    cleaned_pixels = cleaned.load()

    for y in range(source.height):
        for x in range(source.width):
            red, green, blue, alpha = source_pixels[x, y]
            if alpha < 128 or is_magenta_fringe(red, green, blue):
                continue
            cleaned_pixels[x, y] = (red, green, blue, 255)

    cleaned = remove_small_components(cleaned)
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


def replace_warm_artifacts(image: Image.Image) -> Image.Image:
    output = image.copy()
    source_pixels = image.load()
    output_pixels = output.load()
    warm_pixels: list[tuple[int, int]] = []

    for y in range(image.height):
        for x in range(image.width):
            red, green, blue, alpha = source_pixels[x, y]
            if alpha and red > green + 10 and red > blue + 10:
                warm_pixels.append((x, y))

    for x, y in warm_pixels:
        replacement: tuple[int, int, int, int] | None = None
        for radius in range(1, 5):
            candidates: list[tuple[int, int, int, int]] = []
            for ny in range(max(0, y - radius), min(image.height, y + radius + 1)):
                for nx in range(max(0, x - radius), min(image.width, x + radius + 1)):
                    if max(abs(nx - x), abs(ny - y)) != radius:
                        continue
                    red, green, blue, alpha = source_pixels[nx, ny]
                    if not alpha or (red > green + 10 and red > blue + 10):
                        continue
                    candidates.append((red, green, blue, alpha))
            if candidates:
                replacement = min(
                    candidates,
                    key=lambda color: abs(color[0] - color[1]),
                )
                break
        output_pixels[x, y] = replacement or (0, 0, 0, 0)

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
            backing.alpha_composite(
                reduced.resize((cell, cell), Image.Resampling.NEAREST)
            )
            canvas.alpha_composite(backing, (left, top))

    canvas.convert("RGB").save(ROOT / "ability_icons_qa.png", optimize=True)


lightning_shot = replace_warm_artifacts(
    fit_subject("lightning_shot_transparent.png")
)
lightning_bubble = replace_warm_artifacts(
    fit_subject("lightning_bubble_transparent_v2.png")
)
lightning_fists = replace_warm_artifacts(
    fit_subject("lightning_fists_transparent.png")
)

outputs = {
    "ability_lightning_shot.png": lightning_shot,
    "ability_lightning_bubble.png": lightning_bubble,
    "ability_lightning_fists.png": lightning_fists,
}

for filename, image in outputs.items():
    image.save(ROOT / filename, optimize=True)

for stem, image in (
    ("lightning_shot", lightning_shot),
    ("lightning_bubble", lightning_bubble),
    ("lightning_fists", lightning_fists),
):
    checkerboard_preview(image, f"{stem}_preview.png")
    qa32_preview(image, f"{stem}_qa32.png")

qa_sheet(
    [
        ("lightning_shot", lightning_shot),
        ("lightning_bubble", lightning_bubble),
        ("lightning_fists", lightning_fists),
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
