# Ability Icons Round 35

All three sources were generated independently with the built-in image-generation tool. The supplied screenshots and Martian face crop were design references rather than edit targets. Sources were generated on flat removable chroma fields and preserved before 64x64 RGBA processing.

## Soopah Laser

```text
Use case: stylized-concept
Asset type: production Minecraft mod ability icon source for "Soopah Laser"
Input images: Image 1 is a concept reference for the separate charged red energy orb and sharp white electrical accents; Image 2 is a concept reference for the immense red beam with a strong white rim. Use them only as subject and scale references, not as a scene to copy.
Scene/backdrop: perfectly flat solid #ff00ff chroma-key background for later removal, one uniform color, no shadows, gradient, texture, floor, or lighting variation.
Subject: exactly one compact charged red energy orb near the lower-right and exactly one enormous red laser beam firing diagonally toward the upper-left. The orb and beam are separate elements with a clearly visible band of chroma background between them; their outlines never touch. Two or three small angular white electrical accents may surround the orb.
Style/medium: authentic hand-placed Minecraft-style pixel art, strong readable silhouette, deliberate square pixel clusters, limited palette, hard crisp edges. Design at a moderately detailed 96x96 pixel-art level intended for clean nearest-neighbor reduction to a final 64x64 Minecraft texture.
Composition/framing: square isolated sprite with generous padding. Orb is much smaller than the beam. Beam begins narrower near the orb and widens dramatically toward the upper-left. The far upper-left end is a broad straight flat cap perpendicular to the beam axis, never a triangle or point.
Color palette: vivid red core, darker crimson edge shading, bright white or very pale cool energy rim. Do not use #ff00ff in the subject.
Constraints: no hand, no character, no weapon, no extra projectile, no impact target, no scene, no UI frame, no circular badge, no text, no watermark, no blur, no antialiasing, no smooth vector curves, no glow haze, no cast shadow. Keep the orb and beam visibly disconnected.
```

## Umbrella Shot

```text
Use case: stylized-concept
Asset type: production Minecraft mod ability icon source for "Umbrella Shot"
Input images: Image 1 is a concept reference for the single pink projectile with a thick dark-purple rim and tiny pale tail; Image 2 is a concept reference for a compact thick-canopied purple umbrella weapon. Use them only as subject references, not as circular framed icons to copy.
Scene/backdrop: perfectly flat solid #00ff00 chroma-key background for later removal, one uniform color, no shadows, gradient, texture, floor, or lighting variation.
Subject: exactly one compact partially opened umbrella weapon and exactly one small fired energy projectile. The umbrella runs diagonally from a black crook handle at the lower-right toward a thick rounded canopy and small white-silver muzzle cap at the center-left/upper-left. The projectile sits farther toward the upper-left, visibly separated from the muzzle, with a pink core, thick dark-purple rim, and at most a very short pale tail.
Style/medium: authentic hand-placed Minecraft-style pixel art, strong readable silhouette, deliberate square pixel clusters, limited palette, hard crisp edges. Design at a moderately detailed 96x96 pixel-art level intended for clean nearest-neighbor reduction to a final 64x64 Minecraft texture.
Composition/framing: square isolated sprite with generous padding. Umbrella occupies about two-thirds of the lower-right to upper-left diagonal; the projectile is only 8-10 percent of the canvas width in the remaining upper-left area and leaves visible empty space. The canopy is thick and compact, not a thin closed stick and not a fully open circular umbrella.
Color palette: black and charcoal handle/shaft, dark aubergine outer canopy, saturated purple panels, small white-silver muzzle, pink projectile with deep violet rim. Do not use #00ff00 anywhere in the subject.
Constraints: no character, no hand, no extra projectile, no impact explosion, no scene, no built-in circular background, no UI frame, no text, no watermark, no blur, no antialiasing, no smooth vector curves, no soft glow haze, no cast shadow.
```

## Telekinesis

```text
Use case: stylized-concept
Asset type: production Minecraft mod action ability icon source for "Telekinesis"
Input images: Image 1 is a concept reference for psychic rings centered on a green forehead; Image 2 is the authoritative Teeny Craft Martian face and color-palette reference. Preserve the red eyes, green palette, strong brow, and centered facial structure from Image 2 while making the face more dimensional and expressive than Image 1.
Scene/backdrop: perfectly flat solid #ff00ff chroma-key background for later removal, one uniform color, no shadows, gradient, texture, floor, or lighting variation.
Subject: exactly one centered front-facing green Martian head with a stern focused expression and red eyes. The head has an angular jaw, pronounced brow, cheek planes, nose bridge, restrained blocky shading, and no body. Exactly three thick concentric psychic rings originate at the center of the forehead; their lower portions overlap the forehead slightly and their upper portions extend above the head. The rings are concentric waves, not a spiral.
Style/medium: authentic hand-placed Minecraft-style pixel art, strong readable silhouette, deliberate square pixel clusters, limited palette, hard crisp edges. Design at a moderately detailed 96x96 pixel-art level intended for clean nearest-neighbor reduction to a final 64x64 Minecraft texture.
Composition/framing: square isolated action icon with generous transparent-intent padding. The face fills roughly the lower two-thirds while the psychic rings remain clearly visible in the upper area.
Color palette: primary green close to #2D8B55, mid and dark greens close to #18793F and #105F2B, deepest near-black green, red-maroon eyes close to #53090A and #621417, rings in pale mint, lime, and yellow-green. Do not use #ff00ff in the subject.
Constraints: no spiral, no hypnosis swirl, no defense-down arrow, no floating objects, no shoulders or torso, no extra face, no scene, no circular badge or background, no UI frame, no text, no watermark, no blur, no antialiasing, no smooth vector curves, no glow haze, no cast shadow.
```

## Processing

- Built-in ImageGen, one independent generation per texture.
- Soopah Laser and Telekinesis used magenta chroma; Umbrella Shot used green chroma.
- Original sources and extracted transparent sources are retained in this folder.
- Production export: exact 64x64 RGBA with binary alpha and nearest-neighbor scaling.
- QA: enlarged checkerboard previews, individual 32x32 views, and a combined 64x64/32x32 sheet.
