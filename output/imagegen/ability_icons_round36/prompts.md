# Ability Icons Round 36

The three production candidates were generated with the built-in image-generation tool. The supplied screenshots were design references rather than edit targets. Sources were generated on flat chroma fields and preserved before 64x64 RGBA processing.

## Plasma Shot — gun variant

```text
Use case: stylized-concept
Asset type: production Minecraft mod ability icon source for Teeny Craft
Input images: Images 1 and 2 are design references for the exact chunky green sci-fi gun; Image 3 is the reference for the pink plasma beam colors and white edging. Generate a new original sprite, not a crop or direct copy.
Primary request: exactly one isolated green plasma gun actively firing exactly one piercing pink beam. No hands and no character.
Style/medium: authentic hand-placed Minecraft-style pixel art, deliberate square pixel clusters, hard jagged edges, limited palette, moderately detailed 96x96-style pixel-art design intended for clean nearest-neighbor reduction to a final 64x64 Minecraft texture.
Composition/framing: square canvas. The gun's rear body begins in the lower-right, the gun runs diagonally about 45 degrees toward the center, and its muzzle points toward the upper-left. The gun occupies roughly the lower half of the diagonal. A compact five-point irregular muzzle burst sits at the center. One long straight beam continues from the burst into the upper-left, occupying the other half. Generous clear padding; nothing clipped.
Subject details: faithfully retain the reference gun's boxy silhouette, dark forest-green rear and underside, brighter lime-green forward casing, black upper recess, black side vent panel with four or five vertical slots, small neon-pink indicator, and neon-pink muzzle opening. Slight three-quarter view showing top and side planes. Beam is hot pink with a pale-pink inner core and crisp white side edging, mostly straight with a slight outward taper. Add only a few deep-plum edge pixels for contrast.
Scene/backdrop: perfectly flat solid #00FFFF cyan chroma-key background for removal, one completely uniform color.
Constraints: exactly one gun, exactly one beam, no hands, no person, no target, no extra projectile, no text, no letters, no numbers, no watermark, no border, no frame, no UI button, no floor, no cast shadow, no reflection. Background must have no gradient, texture, shadows, haze, or lighting variation. Do not use #00FFFF anywhere in the sprite.
Avoid: smooth vector curves, soft glow haze, blur, antialiasing, realistic rendering, tiny noisy mechanical detail, background scenery.
```

## Laser Eyes — Dr. Light

The initial generated source is retained as `laser_eyes_drlight_initial_source.png`. Its dark center read too much like an eye, so the final source was produced with this targeted revision prompt:

```text
Use case: precise-object-edit
Asset type: production Minecraft mod ability icon source for Teeny Craft
Input images: Image 1 is the edit target and establishes the approved pixel-art style, diagonal composition, right hand, cuff, and connected white-yellow beam. Image 2 is only a loose design reference for the hand forming a circular light charge.
Primary request: revise only the firing origin and hand clarity so this cannot be mistaken for an eye. Remove the dark navy/black pupil-like central circle completely. Replace it with a solid radiant near-white and pale-yellow palm-light core whose center merges directly into the beam. Keep exactly one right hand with its palm/front facing the viewer, fingers visibly curling around the outer side of the light disk rather than making an eyelid shape.
Preserve: the lower-right to upper-left 45-degree composition, one right forearm entering lower-right, white/light-gray cuff, warm peach and coral pixel-art hand, one straight connected beam, near-white beam core, crisp lemon-yellow/gold outline, two or three small angular sparks, flat green chroma background, generous padding, authentic deliberate square pixel clusters, and final 64x64 reduction intent.
Composition refinement: make the circular charge a little smaller and clearly mounted in the palm, with a pale-yellow center and white-hot beam connection. The beam remains slightly narrower than the circle. Separate the upper and lower finger silhouettes enough to read as a hand, not an eye surround.
Scene/backdrop: perfectly flat solid #00FF00 green chroma-key background, exactly uniform.
Constraints: change only the palm-light origin and finger readability; no dark central dot, no black circle, no pupil, no iris, no eye symbol, no eye, no face, no head, no second hand, no extra beam, no text, no watermark, no border, no frame, no shadows or gradient in the background, and do not use #00FF00 in the sprite. Hard pixel-art edges, no blur, no antialiasing, no soft glow haze.
```

## Boulder Toss

```text
Use case: stylized-concept
Asset type: production Minecraft mod ability icon source for Teeny Craft
Input images: Image 1 is the palette and low-poly faceting reference for the boulder. Generate a new original sprite, not a crop or direct copy.
Primary request: exactly one enormous heavy boulder being thrown through the air, shedding exactly three small rock fragments.
Style/medium: authentic hand-placed Minecraft-style pixel art, deliberate square pixel clusters, hard jagged edges, limited palette, moderately detailed 96x96-style pixel-art design intended for clean nearest-neighbor reduction to a final 64x64 Minecraft texture.
Composition/framing: square canvas. The dominant boulder travels diagonally from lower-left toward upper-right and is centered slightly above and right of canvas center. It fills roughly 60 to 65 percent of the canvas width. The silhouette is irregularly round and massive, not a perfect circle. Exactly three much smaller angular fragments trail behind toward the lower-left, plus exactly two short blocky motion/dust marks. Generous clear padding; nothing clipped.
Subject details: five or six large readable angular stone planes. Deep purple and plum shadow on the lower-left, dark brick-red middle tones, reddish clay midtones, coral and salmon highlights on the upper-right, matching the reference palette. One restrained crack on the trailing lower-left face. The fragments share the same palette and remain clearly subordinate. The motion marks are muted dusty coral, not an arrow or circular swoosh.
Scene/backdrop: perfectly flat solid #00FF00 green chroma-key background for removal, one completely uniform color.
Constraints: exactly one main boulder, exactly three small rock fragments, exactly two short motion marks, no hand, no arm, no character, no ground, no target, no impact explosion, no extra full boulder, no curved UI arrow, no text, no letters, no numbers, no watermark, no border, no frame, no UI button, no floor, no cast shadow, no reflection. Background must have no gradient, texture, shadows, haze, or lighting variation. Do not use #00FF00 anywhere in the sprite.
Avoid: smooth perfect sphere, photorealistic stone texture, tiny noisy cracks, soft glow, blur, antialiasing, realistic rendering, background scenery.
```

## Processing

- Built-in ImageGen, one independent source per texture, with one targeted Dr. Light revision.
- Plasma Shot used cyan chroma because both green and magenta appear in the subject; the other two used green chroma.
- Original and transparent processing masters are retained in this folder.
- Production candidates are exact 64x64 RGBA images with binary alpha and nearest-neighbor reduction.
- Dr. Light uses a four-pixel inset because its generated source reached both diagonal canvas edges.
- QA includes enlarged checkerboard previews, individual 32x32 views, and a combined 64x64/32x32 sheet.
