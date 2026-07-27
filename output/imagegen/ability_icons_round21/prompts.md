# Teeny Craft ability icons — round 21

Built-in image generation produced three separate source masters on flat
magenta chroma backgrounds. The installed chroma-removal helper extracted each
master before nearest-neighbor reduction to the 64x64 production grid.

The two shields deliberately keep partially transparent interiors. Their outer
rings remain opaque, and Burp Shield's interior uses greater opacity than Light
Shield's interior. Science keeps its neon liquid and teal glass outline opaque
while using partial alpha for the pale glass body.

## `light_shield`

```text
Use case: stylized-concept
Asset type: production Minecraft Forge mod action-display ability-icon source, intended for final nearest-neighbor reduction to a 64x64 transparent RGBA item texture and recognition at approximately 32x32.
Input image: Image 1 is a geometry and pixel-art style reference only. Match its centered, slightly irregular blocky circular shield footprint, outer diameter, ring weight, padding, and chunky hand-placed pixel-art language. Do not copy its lightning spokes or its colors.
Primary request: Create Dr. Light's ability icon “Light Shield” as exactly one filled luminous yellow shield bubble.
Scene/backdrop: perfectly flat, uniform solid #ff00ff chroma-key background for later removal; absolutely no gradient, texture, shadow, floor, lighting variation, or magenta reflections.
Subject and composition: one large centered nearly circular shield bubble occupying the same proportion of the square as Image 1. It has a chunky, slightly irregular golden-yellow outer ring and a clearly filled pale-yellow interior. No lightning spokes and no central emblem. The interior should suggest translucent hard light through broad low-detail pixel clusters: exactly one large near-white yellow reflection along the upper-left interior, one restrained warm-yellow shaded region toward the lower-right, and two or three broad angular light facets. Keep the center calm and readable.
Style/medium: authentic Minecraft-like hand-placed pixel art with deliberately chunky square clusters, hard stair-stepped edges, limited palette, strong silhouette, no smooth illustration look. The shield may feel gently three-dimensional, but all depth must come from a few broad pixel-art regions rather than fine detail or soft rendering.
Color palette: opaque master colors only for clean chroma removal; golden-yellow ring, bright lemon accents, very pale cream-yellow interior, one subdued amber shadow. Do not use green, electric chartreuse, or magenta in the subject.
Constraints: exactly one filled circular shield; match Image 1’s footprint and outline format; generous clean padding; no internal lightning, spokes, center junction, character, hand, star, symbol, text, frame, badge, scene, cast shadow, particles outside the ring, blur, antialiasing, smooth vector curves, soft glow haze, watermark, or #ff00ff in the sprite. Final intent is a translucent interior, which will be applied during local processing; render the master interior as flat opaque pale yellow.
```

### `light_shield` closed-body revision

The first source introduced a large unintended lower-right cutout. It remains
saved as the initial master, but the revision below replaces it in production.

```text
Use case: precise-object-edit
Asset type: revised production Minecraft Forge mod action-display ability-icon source, intended for final nearest-neighbor reduction to a 64x64 transparent RGBA item texture and recognition at approximately 32x32.
Input image: Image 1 is the geometry reference and edit target. Preserve its complete closed circular shield silhouette, centered scale, outer footprint, irregular stair-stepped pixel outline, ring thickness, and padding exactly. The shield must remain a fully filled closed disk with no hole, missing wedge, gap, opening, cutout, or transparent region anywhere inside the outer outline.
Primary request: Transform the green Burp Shield into Dr. Light’s luminous yellow Light Shield. Change the entire shield palette from green to golden yellow and pale luminous yellow. Remove all internal round bubbles and remove the green cloudy swirl. Replace them with a calm clean pale-yellow filled interior, exactly one broad near-white yellow reflection on the upper-left, two or three broad angular light facets, and one restrained warm golden shaded region toward the lower-right.
Scene/backdrop: keep a perfectly flat, uniform solid #ff00ff chroma-key background; no gradient, texture, shadow, floor, lighting variation, or magenta reflections.
Style/medium: preserve the authentic Minecraft-like hand-placed pixel-art construction, deliberately chunky square clusters, hard stair-stepped edges, limited palette, strong silhouette, and low detail. Depth comes only from broad pixel regions.
Color palette: opaque master colors only; golden-yellow ring with a slightly darker amber outer edge, very pale cream-yellow filled interior, bright lemon highlight, subdued warm-gold shadow. No green, teal, or magenta inside the shield.
Constraints: change only the palette and internal decoration described above; preserve the complete closed circular geometry. Exactly one fully filled circular shield. Absolutely no hole, wedge, gap, opening, cutout, hollow area, internal transparency, bubbles, lightning, spokes, center junction, character, symbol, text, frame, scene, cast shadow, particles outside the ring, blur, antialiasing, smooth vector curves, glow haze, watermark, or #ff00ff in the sprite. Final translucency will be applied locally after chroma removal; the generated master must be fully opaque throughout its complete circular interior.
```

## `burp_shield`

```text
Use case: stylized-concept
Asset type: production Minecraft Forge mod action-display ability-icon source, intended for final nearest-neighbor reduction to a 64x64 transparent RGBA item texture and recognition at approximately 32x32.
Input image: Image 1 is a geometry and pixel-art style reference only. Match its centered, slightly irregular blocky circular shield footprint, outer diameter, ring weight, padding, and chunky hand-placed pixel-art language. Do not copy its lightning spokes or colors.
Primary request: Create the ability icon “Burp Shield” as exactly one filled green gaseous bubble shield.
Scene/backdrop: perfectly flat, uniform solid #ff00ff chroma-key background for later removal; absolutely no gradient, texture, shadow, floor, lighting variation, or magenta reflections.
Subject and composition: one large centered nearly circular shield bubble occupying the same proportion of the square as Image 1. It has a chunky, slightly irregular darker fresh-green outer ring and a clearly filled light-green interior. No lightning spokes and no central emblem. Make the interior look somewhat thicker and less transparent than a normal clean light bubble through broad low-detail pixel clusters: exactly one pale yellow-green reflection on the upper-left, one darker green shaded area toward the lower-right, exactly three or four simple chunky round bubble shapes contained fully inside the shield, and one restrained broad cloudy curved green swirl. The bubbles must remain sparse and readable and must not escape outside the ring.
Style/medium: authentic Minecraft-like hand-placed pixel art with deliberately chunky square clusters, hard stair-stepped edges, limited palette, strong silhouette, no smooth illustration look. The shield should feel gently three-dimensional and gaseous, but all depth must come from a few broad pixel-art regions rather than fine detail or soft rendering.
Color palette: opaque master colors only for clean chroma removal; medium leaf-green ring with a dark forest-green edge, fresh bright-green interior, pale chartreuse highlight, one restrained deeper-green shadow. Do not use magenta in the subject.
Constraints: exactly one filled circular shield; match Image 1’s footprint and outline format; exactly three or four internal bubbles; generous clean padding; no internal lightning, spokes, center junction, slime drips, poison skull, character, mouth, hand, text, frame, badge, scene, cast shadow, particles outside the ring, blur, antialiasing, smooth vector curves, soft glow haze, watermark, or #ff00ff in the sprite. Final intent is a semi-transparent interior, which will be applied during local processing; render the master interior as flat opaque green.
```

## `science`

```text
Use case: stylized-concept
Asset type: production Minecraft Forge mod action-display ability-icon source, intended for final nearest-neighbor reduction to a 64x64 transparent RGBA item texture and recognition at approximately 32x32.
Primary request: Create Gorilla Grodd’s ability icon “Science!” as exactly one cartoon Erlenmeyer flask made of glass, filled with bubbling neon-green liquid.
Scene/backdrop: perfectly flat, uniform solid #ff00ff chroma-key background for later removal; absolutely no gradient, texture, shadow, floor, lighting variation, or magenta reflections.
Subject and composition: one large isolated Erlenmeyer flask centered on the square and tilted only slightly, about 8 to 12 degrees clockwise toward the right. Preserve an unmistakable silhouette: broad triangular flask body, flat-ish bottom, sloped shoulders, short narrow neck, and clearly flared glass rim. The flask fills most of the canvas while retaining generous padding. Neon-green liquid fills approximately the lower 35 to 40 percent of the flask. Keep the liquid surface almost horizontal with a small raised slosh toward the right. Exactly three simple bubbles rise upward: one still inside the neck and exactly two separated bubbles escaping above the rim toward the upper-right.
Style/medium: very cartoonish authentic Minecraft-like hand-placed pixel art with deliberately chunky square clusters, hard stair-stepped edges, strong silhouette, limited detail, and no smooth illustration look. Design for clean nearest-neighbor reduction to 64x64 and clear recognition at 32x32.
Color palette and materials: the glass has a visible darker desaturated teal/cyan colored outline instead of black, a pale aqua interior tint, and one broad white-aqua reflection on the upper-left. The liquid is vivid neon green with a deeper emerald lower edge and a small pale yellow-green highlight. Keep the glass and liquid as opaque master colors for clean chroma removal; intentional glass translucency will be applied during local processing. No magenta in the subject.
Constraints: exactly one flask and exactly three bubbles total; no hand, gorilla, face, atom symbol, label, text, table, stopper, second container, explosion, large smoke cloud, laboratory scene, frame, circular badge, cast shadow, reflection on the background, blur, antialiasing, smooth vector curves, soft glow haze, watermark, or #ff00ff in the sprite.
```

## Processing

- Chroma background: flat generated magenta, removed with the installed
  `remove_chroma_key.py` helper using border auto-keying, soft matte, despill,
  transparent threshold `12`, and opaque threshold `220`.
- The Light Shield revision's extremely pale body produced an excessively soft
  matte from the general helper, so its production reduction uses a hard
  magenta-dominance key directly from the preserved opaque source.
- Production canvas: 64x64 RGBA.
- Subject fit: maximum extent of 58 pixels.
- Scaling: nearest-neighbor.
- Light Shield interior alpha: approximately 27–52%, depending on highlight.
- Burp Shield interior alpha: approximately 46–75%, depending on highlight.
- Shield outline alpha: fully opaque.
- QA: individual enlarged checkerboard previews, individual 32x32 QA previews,
  and one combined 64x64/32x32 review sheet.
