# Teeny Craft ability icon prompts — round 10

Generation mode: built-in image generation. Flat chroma backgrounds were removed locally, then the subjects were fitted and reduced to 64x64 with nearest-neighbor sampling and binary alpha.

## Starfish Chuck

Use case: `stylized-concept`

Asset type: production Minecraft Forge mod ability icon source, intended for final nearest-neighbor reduction to a 64x64 transparent RGBA item texture and readability at 32x32.

Primary request: Starfish Chuck ability icon showing exactly one centered orange starfish.

Scene/backdrop: perfectly flat uniform solid `#ff00ff` chroma-key background for later removal, with no gradient, texture, shadow, floor, reflection, or lighting variation.

Subject: exactly one five-armed orange starfish, centered and upright in a classic orientation: one arm points straight up, two arms extend horizontally left and right, and two arms angle toward the lower-left and lower-right. The silhouette occupies about 60 percent of the square canvas in both width and height, leaving generous empty chroma padding. The arms are thick, gently irregular and organic with rounded tips, not a mathematically perfect star. Use a dark burnt-orange edge/shadow, a saturated orange body, and lighter peach-orange circular markings. There is exactly one largest light-orange circle in the center, then light-orange circles along each arm that decrease in size toward each arm tip. The spot rows radiate cleanly from the center.

Style/medium: authentic hand-placed Minecraft-style pixel art, deliberate large square pixel clusters, hard stair-stepped edges, very limited palette, simple colored shading, and a strong clean silhouette.

Constraints: exact five-arm orientation and exactly one starfish. Keep the subject around 60 percent of the canvas, not oversized. No face, eyes, water splash, impact burst, speed lines, character, text, watermark, frame, badge, circular UI background, scene, blur, antialiasing, smooth vector curves, soft glow, or extra objects. Do not use `#ff00ff` anywhere in the subject.

## Whale Drop — blue master

Use case: `stylized-concept`

Asset type: production Minecraft Forge mod action-display ability icon source, intended for final nearest-neighbor reduction to a 64x64 transparent RGBA item texture and readability at 32x32.

Primary request: Whale Drop ability icon showing exactly one compact blue whale falling toward the viewer.

Scene/backdrop: perfectly flat uniform solid `#ff00ff` chroma-key background for later removal, with no gradient, texture, shadow, floor, reflection, or lighting variation.

Subject: exactly one broad chunky blue whale in a near-front three-quarter descending pose. Its body proportions are compact and mildly squared: width and height are fairly close, with a broad upper body, gently flattened sides and rounded corners, but it remains organic and clearly whale-shaped; it is not an extreme circle, sphere, cube, rectangle, or long streamlined whale. A small tail rises behind the top of the body, and two short flippers extend from the sides. The face has two simple eyes and a small friendly smile. From directly below the mouth down across the underside is one broad pale beige/light-yellow belly patch containing exactly four chunky vertical ventral groove stripes. Use a restrained dark ocean-blue edge/shadow, medium ocean-blue body, lighter blue highlight, pale beige-yellow underside, and dark blue facial pixels. Add exactly two short blocky downward motion streaks above the upper sides.

Style/medium: authentic hand-placed Minecraft-style pixel art, deliberate large square pixel clusters, hard stair-stepped edges, limited palette, chunky low-detail shading, and a strong readable silhouette.

Constraints: exactly one whale, exactly two eyes, exactly one small smile, exactly four belly groove stripes, and exactly two fall streaks. The body must be mildly squared in proportion but not box-shaped and not extremely round. No ocean, water splash, impact explosion, victim, character, extra animal, text, watermark, frame, badge, circular UI background, scene, soft glow haze, blur, antialiasing, smooth vector curves, or fine micro-detail. Do not use `#ff00ff` anywhere in the subject.

## Curse

Use case: `stylized-concept`

Asset type: production Minecraft Forge mod action-display debuff ability icon source, intended for final nearest-neighbor reduction to a 64x64 transparent RGBA item texture and readability at 32x32.

Primary request: Curse ability icon consisting of one irregular purple magical blot, one subtle broken darker-purple flame shape, and exactly three simple white skulls.

Scene/backdrop: perfectly flat uniform solid `#00ff00` chroma-key background for later removal, with no gradient, texture, shadow, floor, reflection, or lighting variation.

Subject: one large centered purple blot with an overall circular mass but a very irregular organic perimeter. Inside the blot is one darker-purple flame motif only moderately darker than the surrounding purple. The flame has three broad upward tongues connected only through a shallow shared base near the bottom; through the middle and upper area they are separated with visible purple gaps. Place exactly three generic off-white skulls on the flame tongues: the largest skull in the lower-left, a medium skull in the middle-right, and the smallest skull in the upper-left. All three skulls occupy substantial space. Each skull has a simple blocky cranium, two dark-purple eye sockets, one tiny nose opening, and a compact blocky tooth row.

Style/medium: authentic hand-placed Minecraft-style pixel art, deliberate large square pixel clusters, hard stair-stepped edges, chunky limited-detail forms, no fine anatomy, strong primary skull read, and subtle secondary flame read.

Constraints: exactly one purple blot, one broken flame motif, and exactly three skulls in the specified positions and sizes. No crossbones, extra skulls, full skeletons, character, smoke cloud, haze, particles outside the blot, text, watermark, frame, clean circular border, badge, scene, blur, antialiasing, smooth vector curves, or bright glow. Do not use `#00ff00` anywhere in the subject.

## Derived variant and exports

- `ability_whale_drop_green.png` was derived from the processed blue 64x64 master through deterministic HSV palette substitution.
- The two whale textures have identical dimensions, alpha channels, silhouette, face, grooves, flippers, tail, and motion-mark geometry.
- The blue base texture is the runtime-ready `ability_whale_drop.png`. The green variant remains in this round folder until character-specific variant selection is implemented.
- All four production textures are 64x64 RGBA PNGs with binary alpha and transparent corners.
