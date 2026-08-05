# Ability Icons Round 32

The built-in image-generation path produced three distinct large sources from
the supplied Teeny Titans references. Each reference guided concept,
proportions, and palette without being copied pixel for pixel. Sources were
generated on flat `#ff00ff` chroma backgrounds for local removal before
nearest-neighbor export to 64x64 RGBA.

## `robo_buddy`

Image 1 was the design and composition reference. Generate exactly one compact
gray remote controller in three-quarter view, centered and occupying only about
55 to 60 percent of the square canvas. Preserve the recognizable broad base,
narrower upper assembly, dark-green panel seams, oversized lime button, and
short antenna with one green orb. Add only a very small, subtle, low-contrast
pale-green lightning notch to the button. Surround and partly overlap the
controller with exactly eight thick blocky binary glyphs using only `0` and
`1`, alternating lime-filled/dark-outlined and dark-filled/lime-outlined
treatments.

Authentic hand-placed Minecraft-style pixel art; moderately detailed 96x96-style
design intended for nearest-neighbor reduction to 64x64. Limited gray,
charcoal, forest-green, and lime palette with crisp stair-stepped edges on a
flat `#ff00ff` background. No robot character, poison symbol, hands, frame,
circular plate, scene, shadow, watermark, blur, antialiasing, vector curves, or
soft glow.

## `burp_surprise`

Image 2 was the design and composition reference. Generate exactly one large
pink Silkie head in left-facing side profile. Make the enormous black open
mouth the dominant shape, with exactly one simple white question mark inside
and one small curled pink tongue line along the lower edge. Show exactly one
round closed eye with a heavy curved eyelid and exactly two long angular pink
antennae rising toward the upper-right. Preserve the cute, strange, surprised
expression while creating new pixel art.

Authentic hand-placed Minecraft-style pixel art; moderately detailed 96x96-style
design intended for nearest-neighbor reduction to 64x64. Salmon pink, pale-pink
highlights, burgundy outlines, near-black mouth, and warm-white question mark
on flat `#ff00ff`. No thrown object, full body, extra face or eye, teeth, hands,
frame, circular plate, scene, shadow, watermark, blur, antialiasing, vector
curves, or soft glow.

## `battery_drain` eel variant

Image 3 was the design and composition reference. Generate exactly one electric
eel in a broad S-shaped wave, with its rounded head at lower-left and tapering
tail sweeping toward upper-right. Give it exactly two protruding white eyes and
one brighter yellow inset body stripe following the upper contour. Behind it,
place exactly one large jagged cyan, pale-aqua, and white electrical burst with
visible negative-space gaps; keep the burst behind the eel's face and body.

Authentic hand-placed Minecraft-style pixel art; moderately detailed 96x96-style
design intended for nearest-neighbor reduction to 64x64. Lime/yellow-green body,
warm-yellow stripe, deep teal/olive outline, and cyan-white electricity on flat
`#ff00ff`. No battery, arrow, meter, Black Manta helmet, character, orange
circle, ocean scene, hands, frame, shadow, watermark, blur, antialiasing, vector
curves, or soft glow.

## Processing

- Built-in ImageGen; one independent generation per texture.
- Green-heavy sprites used the installed chroma-removal helper with border
  auto-keying, soft matte, and despill.
- Silkie used a local hard magenta mask so its warm pink pixels stayed fully
  saturated and opaque.
- Production export: exact 64x64 RGBA, binary alpha, nearest-neighbor scaling.
- QA: enlarged checkerboard previews, individual 32x32 views, and a combined
  64x64/32x32 comparison sheet.
