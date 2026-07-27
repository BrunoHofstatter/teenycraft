# Ability Icons Round 15

Built-in image generation was used for three separate source images. Each source
used a flat `#ff00ff` chroma background, which was removed locally before
nearest-neighbor reduction to the 64x64 production textures.

## `lightning_shot`

```text
Use case: stylized-concept
Asset type: production Minecraft Forge mod ability-icon source, intended for reduction to a 64x64 transparent item texture and recognition at about 32x32
Primary request: Create the Black Lightning ability icon “Lightning Shot” as a single isolated Minecraft-style hand-placed pixel-art sprite.
Scene/backdrop: perfectly flat, uniform solid #ff00ff chroma-key background for later removal; absolutely no gradient, texture, shadow, floor, lighting variation, or magenta reflections.
Subject and composition: a cropped white-gloved clenched fist and short white cuff enter from the bottom-right and fire one dominant electric bolt toward the upper-left. The glove occupies only roughly the lower-right quarter but remains clearly recognizable: square pixel fingers, fist wider than the wrist, chunky silhouette. The bolt begins thick at the knuckles, makes approximately three strong zigzags whose amplitude and thickness decrease toward the tip, then ends in a shorter, straighter pointed section. Add only three or four short subordinate forks from the main bolt. Exactly one fist, exactly one major bolt.
Style/medium: authentic Minecraft-like pixel art with deliberately chunky square pixel clusters, hard stair-stepped edges, strong silhouette, limited detail, no smooth illustration look. Design for clean nearest-neighbor reduction to 64x64 and readability at 32x32.
Color palette: white glove with restrained pale-gray block shadows and subtle chartreuse reflected pixels. Lightning has a near-white pale-yellow core, vivid electric chartreuse / acid-lime yellow main body, and sparse darker lime-olive edge steps. The energy is neon yellow leaning green, not pure golden yellow.
Constraints: generous transparent-intent padding; keep all subject parts separated clearly from the chroma background; no #ff00ff anywhere in the sprite; no impact explosion, target, orb, second fist, character, frame, circular badge, text, watermark, cast shadow, blur, antialiasing, smooth vector curves, soft glow, glow haze, or background scene.
```

## `lightning_bubble`

```text
Use case: stylized-concept
Asset type: production Minecraft Forge mod ability-icon source, intended for reduction to a 64x64 transparent item texture and recognition at about 32x32
Primary request: Create the Black Lightning ability icon “Lightning Bubble” as one isolated Minecraft-style hand-placed pixel-art sprite.
Scene/backdrop: perfectly flat, uniform solid #ff00ff chroma-key background for later removal; absolutely no gradient, texture, shadow, floor, lighting variation, or magenta reflections.
Subject and composition: a large centered circular electrical shield bubble occupying most of the square canvas. The bubble is an outline only: a chunky, slightly irregular, blocky circular ring with a completely empty chroma-background interior. Exactly five separate jagged lightning spokes originate at five well-spaced points on the perimeter and travel inward, connecting together at one small irregular junction slightly off the exact center. The five spokes must remain individually readable and should not become a dense web. No character and no filled glass sphere.
Style/medium: authentic Minecraft-like pixel art with deliberately chunky square pixel clusters, hard stair-stepped edges, strong silhouette, limited detail, no smooth illustration look. Design for clean nearest-neighbor reduction to 64x64 and readability at 32x32.
Color palette: lightning and ring have a near-white pale-yellow core on selected strongest segments, vivid electric chartreuse / acid-lime yellow main body, and sparse darker lime-olive outside steps. Neon yellow leaning green, not pure golden yellow. The center of the bubble remains transparent-intent.
Constraints: generous padding around the outer ring; no #ff00ff anywhere in the sprite; exactly one ring and exactly five inward bolts; no background fill, aura cloud, glass tint, radial glow, extra emblem, person, fist, frame, circular UI badge behind it, text, watermark, cast shadow, blur, antialiasing, smooth vector curves, soft glow, glow haze, or scene.
```

## `lightning_fists`

```text
Use case: stylized-concept
Asset type: production Minecraft Forge mod ability-icon source, intended for reduction to a 64x64 transparent item texture and recognition at about 32x32
Primary request: Create the Black Lightning ability icon “Lightning Fists” as one isolated Minecraft-style hand-placed pixel-art sprite.
Scene/backdrop: perfectly flat, uniform solid #ff00ff chroma-key background for later removal; absolutely no gradient, texture, shadow, floor, lighting variation, or magenta reflections.
Subject and composition: exactly one large white-gloved clenched fist thrusting diagonally from the bottom-right toward the upper-left. The broad square knuckle block is the dominant focal point in the upper-left/center; a narrower white wrist and short forearm exit through the bottom-right. Show simplified square finger divisions and make the fist clearly larger than the forearm. Add exactly five short jagged electrical fragments tightly wrapping around the knuckles, sides of the fist, and wrist. Some fragments pass visually in front and some behind, like coiled electricity hugging the glove; they must not radiate far outward. Three of the strongest wraps may subtly suggest the ability’s three hits. Exactly one fist, no duplicate or echo fists.
Style/medium: authentic Minecraft-like pixel art with deliberately chunky square pixel clusters, hard stair-stepped edges, strong silhouette, limited detail, no smooth illustration look. Design for clean nearest-neighbor reduction to 64x64 and readability at 32x32.
Color palette: glove and sleeve are white with restrained pale-gray block shadows, sparse charcoal edge accents only where needed for silhouette, and subtle chartreuse reflected pixels. Electricity has a near-white pale-yellow core, vivid electric chartreuse / acid-lime yellow main body, and sparse darker lime-olive outer steps. Neon yellow leaning green, not pure golden yellow.
Constraints: generous transparent-intent padding; keep the electricity visually attached to and wrapping the glove; no #ff00ff anywhere in the sprite; no second fist, long projectile bolt, impact explosion, outward aura, character, frame, badge, text, watermark, cast shadow, blur, antialiasing, smooth vector curves, soft glow, glow haze, or background scene.
```

## Processing

- Chroma removal: installed `remove_chroma_key.py` helper with border
  auto-keying, soft matte, despill, threshold `12`, and opaque threshold `220`.
- Final sizing: subject fit to a maximum extent of 58 pixels on a 64x64 canvas.
- Scaling: nearest-neighbor.
- Alpha: binary.
- QA: enlarged checkerboard previews plus combined 64x64, 32x32, and 16x16
  comparison sheet.
- A few isolated warm-colored fringe pixels were replaced with neighboring
  in-palette pixels after reduction.

## `lightning_bubble` revision 2

The original source and exports remain saved with the `_v1` suffix where
needed. Revision 2 replaces the production Bubble texture.

```text
Use case: stylized-concept
Asset type: revised production Minecraft Forge mod ability-icon source, intended for reduction to a 64x64 transparent item texture and recognition at about 32x32
Primary request: Regenerate the Black Lightning ability icon “Lightning Bubble.” Keep the electrical shield-bubble concept, but make the five lightning streaks inside the circle much thicker, much more irregular, and clearly more zigzagged than the previous version.
Scene/backdrop: perfectly flat, uniform solid #ff00ff chroma-key background for later removal; absolutely no gradient, texture, shadow, floor, lighting variation, or magenta reflections.
Subject and composition: one large centered, slightly irregular blocky circular electrical outline with a completely empty chroma-background interior. Exactly five separate thick lightning streaks begin at five unevenly spaced points around the perimeter and converge into one irregular junction slightly off-center. Each inner streak must have multiple abrupt angular bends, unequal segment lengths, visibly different paths, variable thickness, and small direction changes; none may look like a straight radial spoke. The interior streaks should be noticeably thicker and visually stronger than the outer ring, about one-and-a-half to two times the ring thickness. Make the center junction energetic and asymmetrical, not a neat star, wheel, pie chart, or peace symbol. Keep five paths individually readable without forming a dense web.
Style/medium: authentic Minecraft-like hand-placed pixel art with deliberately chunky square clusters, hard stair-stepped edges, strong silhouette, limited detail, and no smooth illustration look. Design for clean nearest-neighbor reduction to 64x64 and clear recognition at 32x32.
Color palette: near-white pale-yellow cores on the strongest portions, vivid electric chartreuse / acid-lime yellow main body, and sparse darker lime-olive outer steps. Neon yellow leaning green, not pure golden yellow.
Constraints: generous padding around the ring; exactly one ring and exactly five thick crooked inward lightning streaks; no #ff00ff anywhere in the sprite; no straight spokes, geometric radial symmetry, filled background, aura cloud, glass tint, character, fist, emblem, frame, UI badge, text, watermark, cast shadow, blur, antialiasing, smooth vector curves, soft glow, glow haze, or scene.
```
