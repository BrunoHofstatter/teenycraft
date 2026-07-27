# Ability Icons Round 22

Built-in image generation produced the three source images. Each source used a
flat `#ff00ff` chroma background, removed locally before nearest-neighbor
reduction to the 64x64 production textures.

## `lightning_mace`

```text
Use case: stylized-concept
Asset type: production Minecraft Forge mod ability-icon source, intended for
nearest-neighbor reduction to a 64x64 transparent item texture and recognition
at about 32x32.

Create exactly one isolated Minecraft-style hand-placed pixel-art mace running
from bottom-left to upper-right at approximately 45 degrees. The slightly dark
steel-gray rod is moderately thin and straight. Its large spherical steel-gray
head is the upper-right focal point and has exactly four raised hemispherical
studs with shaded bases and lighter caps. Add four or five short jagged
lightning fragments tightly wrapping the head and upper shaft. Match Lightning
Fists with a near-white pale-yellow core, acid-lime/chartreuse body, and darker
olive-lime edge pixels.

Use deliberately chunky square clusters, hard stair-stepped edges, limited
detail, generous padding, and a perfectly flat uniform `#ff00ff` chroma
background. No hand, character, second mace, spikes, holes, rings, blue
lightning, impact burst, frame, text, soft glow, blur, antialiasing, or scene.
```

## `double_slam`

The generated Lightning Mace source was supplied as the exact weapon-design
reference.

```text
Use case: precise-object-edit
Asset type: production Minecraft Forge mod ability-icon source, intended for
nearest-neighbor reduction to a 64x64 transparent item texture and recognition
at about 32x32.

Transform the referenced Lightning Mace into Double Slam while preserving the
same steel material, spherical head, exactly four raised hemispherical studs,
rod proportions, shading, and chunky Minecraft pixel-art construction. Show
exactly one mace flying bottom-left to upper-right at approximately 35 to 40
degrees above horizontal, with the head at upper-right.

Remove all lightning. Add exactly two separated, restrained cool-white or
pale-gray curved speed sweeps trailing toward lower-left, plus no more than
three tiny direction pixels. The two main sweeps suggest two hits without a
duplicate mace, ghost mace, impact explosion, or target. Use a perfectly flat
uniform `#ff00ff` chroma background. No hand, character, electricity, frame,
text, glow, blur, antialiasing, or scene.
```

## `flight`

The selected source followed several targeted silhouette revisions. The final
revision prompt was:

```text
Use case: precise-object-edit
Asset type: final targeted correction of a Teeny Craft Flight ability-icon
source.

Widen the upper half of the red cape so its broad horizontal top is about 85
percent as wide as the maximum bottom width. Keep the sides nearly vertical
with only a slight flare, producing a restrained trapezoid rather than a
triangle or shield. Replace curled punctuation-like marks with exactly four
subtle upward-speed streaks: two short stair-stepped rising lines beside the
upper half and two shorter rising lines near the lower outer corners.

Preserve exactly one centered front-facing red cape, one centered chunky
golden upward arrow fully inside it, a slight blocky bottom fabric wave,
crimson/red hard-edged pixel shading, a flat uniform `#ff00ff` chroma
background, crisp Minecraft-style square clusters, and generous padding. No
character, clasp, emblem, wings, split tails, clouds, extra marks, text,
punctuation, watermark, blur, antialiasing, gradient, or glow.
```

## Processing

- Chroma removal: installed `remove_chroma_key.py` helper with border
  auto-keying, soft matte, despill, transparent threshold `12`, and opaque
  threshold `220`.
- Final sizing: each complete subject fit to a maximum extent of 58 pixels on a
  64x64 canvas.
- Scaling: nearest-neighbor.
- Alpha: binary in production textures.
- QA: individual enlarged checkerboard previews, individual 32x32 views, and a
  combined 64x64/32x32 comparison sheet.
