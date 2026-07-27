# Ability Icons Round 19

Built-in image generation was used for every source. Green chroma was used for
Camera Flash and Journalism; magenta chroma was used for the green Cat Beast
Boy hand. Sources were generated separately, background-removed locally, and
reduced to 64x64 with nearest-neighbor sampling.

## Camera Flash

```text
Use case: stylized-concept
Asset type: production Minecraft mod ability icon source, intended for nearest-neighbor reduction to a 64x64 transparent item texture and readability at 32x32
Primary request: Camera Flash ability icon — exactly one compact handheld camera actively firing a brilliant flash
Scene/backdrop: perfectly flat solid #00ff00 chroma-key background for local background removal; one uniform color only, no shadows, gradients, texture, floor, reflection, or lighting variation
Subject: a single compact camera in dark charcoal gray, with restrained muted-purple shadow planes and a small dusty-pink ring around the lens; no character and no hand
Style/medium: authentic hand-placed Minecraft-style pixel art, chunky deliberate square pixel clusters, limited palette, hard stair-stepped edges, strong readable silhouette; deliberately simplified for final 64x64 reduction
Composition/framing: camera occupies the lower-right two-thirds of the square and points toward the upper left; three-quarter first-person viewpoint from slightly behind and above-left, showing the top and back plus enough of the lens side for the pink ring to be recognizable; camera is clearly off-center; a large crisp angular white flash erupts forward into the upper-left, with only a few pale lavender and very light pink edge pixels; generous transparent-intent padding and no clipping
Color palette: mostly dark charcoal, restrained desaturated purple shadows, very limited dusty pink lens accent, white flash core with sparse pale lavender/pink edge pixels
Constraints: exactly one camera; the flash and camera must remain the two dominant shapes at 32x32; isolated sprite only; do not use #00ff00 in the subject; no text, watermark, frame, rarity border, circular UI background, full scene, character, hand, stun badge, stars, branding, detailed rear-screen UI, extra props, cast shadow, or reflection
Avoid: smooth vector curves, antialiasing, blur, soft glow haze, gradients, painterly rendering, excessive micro-detail, photorealism
```

## Journalism

The first generated source used a conventional diagonal and is retained as
`journalism_initial_source.png`. The final source came from this targeted
near-vertical revision. Its unusually upright generated result was rotated by
10 degrees with nearest-neighbor sampling during processing to land at the
requested approximately 70-degree final composition:

```text
Use case: stylized-concept
Asset type: production Minecraft mod ability icon source, intended for nearest-neighbor reduction to a 64x64 transparent item texture and readability at 32x32
Primary request: Journalism ability icon — exactly one classic wooden pencil thrown through the air, with an unusually steep near-vertical orientation
Scene/backdrop: perfectly flat solid #00ff00 chroma-key background for local background removal; one uniform color only, no shadows, gradients, texture, floor, reflection, or lighting variation
Subject: a single thick readable yellow wooden pencil with a sharpened nearly-black graphite point, visible tan sharpened wood, cool gray metal ferrule, and a brick-red eraser; two or three darker golden longitudinal facet stripes run along the yellow body
Style/medium: authentic hand-placed Minecraft-style pixel art, chunky deliberate square pixel clusters, limited palette, hard stair-stepped edges, strong readable silhouette; deliberately simplified for final 64x64 reduction
Composition/framing: IMPORTANT — the pencil axis is approximately 70 degrees above the horizontal, only about 20 degrees tilted away from vertical; it starts near x=20,y=57 at the lower-left and ends near x=40,y=7 at the upper-right; graphite point at the lower-left end and red eraser at the upper-right end; the pencil fills most of the square without clipping; exactly two short subordinate motion streaks near the lower-left side; generous transparent-intent padding
Color palette: warm yellow and golden-yellow body, muted cool-gray ferrule, restrained brick-red eraser, tan exposed wood, nearly black graphite
Constraints: exactly one pencil; enforce the near-vertical 70-degree angle, not a common 45-degree diagonal; preserve eraser at upper-right and point at lower-left; isolated sprite only; do not use #00ff00 in the subject; no hand, character, paper, newspaper, notebook, text, letters, target, impact scene, extra pencil, watermark, frame, rarity border, circular UI background, cast shadow, or reflection
Avoid: 45-degree angle, horizontal pencil, smooth vector curves, antialiasing, blur, gradients, painterly rendering, excessive micro-detail, photorealism
```

## Cat Scratch

```text
Use case: stylized-concept
Asset type: production Minecraft mod ability icon source, intended for nearest-neighbor reduction to a 64x64 transparent item texture and readability at 32x32
Primary request: Cat Scratch ability icon — exactly one oversized green Cat Beast Boy paw-hand performing a clawing scratch
Scene/backdrop: perfectly flat solid #ff00ff chroma-key background for local background removal; one uniform color only, no shadows, gradients, texture, floor, reflection, or lighting variation
Subject: one large stylized green cat-like hand attached to a very thin green forearm; exactly four clearly separated digits total, each ending in one long hooked pure-black claw; open curled scratching pose, not a closed human fist
Style/medium: authentic hand-placed Minecraft-style pixel art, chunky deliberate square pixel clusters, limited palette, hard stair-stepped edges, strong readable silhouette; deliberately simplified for final 64x64 reduction
Composition/framing: the very thin forearm enters from the bottom-right corner and widens dramatically into the oversized paw-hand; the four clawed digits rake toward the upper-left; the hand is the dominant subject and fills most of the square without clipping; exactly three short pale-green motion slashes continue beyond the claws toward the upper-left, subordinate to the hand; generous transparent-intent padding
Color palette: saturated Beast Boy green, limited darker forest-green shadows and restrained yellow-green highlights; claws are solid pure black; pale green motion accents
Constraints: exactly one hand, exactly one thin forearm, exactly four visible digits total, exactly four long black claws, exactly three short motion slashes; preserve transparent gaps between claws so they remain separate at 32x32; isolated sprite only; do not use #ff00ff anywhere in the subject; no second hand, enemy, blood, character body, face, impact target, power-down symbol, text, watermark, frame, rarity border, circular UI background, cast shadow, or reflection
Avoid: five fingers, human fist, muscular forearm, thick wrist, short fingernails, smooth vector curves, antialiasing, blur, gradients, painterly rendering, excessive micro-detail, photorealism
```
