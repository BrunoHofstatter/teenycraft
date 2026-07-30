# Ability Icons Round 23

Built-in image generation produced one large source for each ability. Luthorbot
and Cash Blaster used flat cyan chroma backgrounds; Kryptonite Beam used a flat
magenta chroma background because its subject is predominantly green.

## `luthorbot`

The supplied Luthorbot screenshot was used only as a character silhouette and
palette reference.

```text
Use case: stylized-concept
Asset type: production Minecraft Forge mod ability icon, later reduced to a
64x64 transparent PNG and required to remain readable at 32x32

Create exactly one hovering Luthorbot being summoned. It is a compact armored
robot with a dark navy body, angular green armor and shoulder plates, a red
visor, oversized magenta-purple mechanical fists, and a simple dark chest
emblem shape. Show the full robot centered in a slightly dynamic three-quarter
frontal pose with both fists visible. Add only a few blocky deployment or hover
pixels directly beneath it.

Use authentic hand-placed Minecraft-style pixel art, a strong silhouette,
deliberately chunky square pixel clusters, a limited palette, crisp hard edges,
and colored highlights and shadows. Fill about 70 percent of a square canvas
with generous padding.

Use a perfectly flat uniform solid #00ffff chroma-key background. Do not copy
the reference's circular UI frame, background, pose, or exact artwork. No
second robot, full Lex Luthor, text, watermark, frame, soft glow, blur,
antialiasing, smooth vector curves, scenery, floor, or shadow.
```

## `kryptonite_beam`

The supplied green crop was used only as a beam-palette reference. The
Luthorbot screenshot was used only for Lex's armor colors and angular
construction.

```text
Use case: stylized-concept
Asset type: production Minecraft Forge mod ability icon, later reduced to a
64x64 transparent PNG and required to remain readable at 32x32

Create exactly one thick chest-fired kryptonite energy beam. The beam
originates from a small bright emitter in a cropped angular green armored chest
fragment at the lower-right corner and blasts diagonally toward the upper-left
at approximately 45 degrees. The armor fragment is only the firing origin, not
a full robot or character. Give the beam a deep forest-teal outer outline,
saturated kryptonite-green body, narrow vivid lime inner core, and a few
restrained angular discharge pixels at the emitter and leading end.

Use authentic hand-placed Minecraft-style pixel art, a strong silhouette,
deliberately chunky square pixel clusters, a limited palette, and crisp hard
edges. The beam should occupy most of the diagonal and roughly two-thirds of
the canvas.

Use a perfectly flat uniform solid #ff00ff chroma-key background. Do not copy
the references' circular UI frames, backgrounds, poses, or exact artwork. No
held weapon, full Lex Luthor, face, shield icon, text, watermark, frame, soft
glow haze, blur, antialiasing, smooth vector curves, scenery, floor, or shadow.
```

## `cash_blaster`

The supplied Luthorbot screenshot was used only for the saturated
purple-magenta and darker purple/navy palette. The supplied coin screenshot
was used only for the chunky gold palette and rim treatment.

```text
Use case: stylized-concept
Asset type: production Minecraft Forge mod ability icon, later reduced to a
64x64 transparent PNG and required to remain readable at 32x32

Create exactly one futuristic triangular-prism Cash Blaster cannon firing a
large stream of coins. The cannon is a chunky angular wedge occupying the
lower-right half and aimed diagonally toward the upper-left at approximately
45 degrees. Its body has a saturated purple-magenta main face, deep plum and
dark navy side facets and outline, restrained lilac highlights, and a strongly
defined dark triangular muzzle.

From the muzzle, show exactly six distinct chunky gold coins through the
upper-left half: three larger primary coins and three smaller secondary coins,
plus a few blocky golden motion pixels. Use bright golden centers, orange-gold
shadows, pale-yellow highlights, and thick amber rims, with no letters or
currency symbols.

Use authentic hand-placed Minecraft-style pixel art, a strong silhouette,
deliberately chunky square pixel clusters, a limited palette, and crisp hard
edges. Use a perfectly flat uniform solid #00ffff chroma-key background. Do
not copy the references' circular UI frames, lilac background, poses, or exact
artwork. No character, hand, arm, coin pile, money bills, text, watermark,
frame, soft glow, blur, antialiasing, smooth vector curves, scenery, floor, or
shadow.
```

## Processing

- Chroma removal: installed `remove_chroma_key.py` helper with border
  auto-keying, soft matte, transparent threshold `12`, and opaque threshold
  `220`.
- Despill was used for the cyan sources. It was deliberately disabled for the
  magenta beam source so the opaque purple armor panels remained intact; the
  processing script removes only partially transparent magenta edge pixels.
- Final sizing: each complete subject fits within a maximum extent of 58
  pixels on a 64x64 canvas.
- Scaling: nearest-neighbor.
- Alpha: binary in production textures.
- QA: individual enlarged checkerboard previews, individual 32x32 views, and a
  combined 64x64/32x32 comparison sheet.
