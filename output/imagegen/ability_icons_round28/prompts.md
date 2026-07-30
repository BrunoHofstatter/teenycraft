# Ability Icons Round 28

Built-in image generation was used once per distinct ability texture. Each
source was generated on a flat magenta chroma background, preserved unchanged,
processed to binary alpha, and reduced with nearest-neighbor sampling to the
64x64 production texture.

The Riddler's figure-equipped ability id is `good_luck`; `luck_up` is the
effect applied by that ability. The production texture therefore uses
`ability_good_luck.png`.

## Trash Can Toss

> Use case: stylized-concept
> Asset type: production Minecraft Forge mod ability icon source sprite for
> Teeny Craft; final deliverable will be a transparent 64x64 texture
> Primary request: Create the Trash Can Toss ability icon for Sticky Joe. Show
> exactly one large galvanized metal trash can actively flying through the
> air. The trash can travels from the lower-right toward the upper-left on a
> steep rising diagonal, approximately 55–60 degrees above horizontal—
> noticeably more upward than a standard 45-degree item diagonal. The lid
> leads toward the upper-left and remains firmly attached; the base trails
> toward the lower-right. Add only two or three short blocky motion marks
> immediately behind the base to make the upward toss unmistakable.
> Style/medium: authentic hand-placed Minecraft-style pixel art, strong
> readable silhouette, deliberate square pixel clusters, hard stair-stepped
> edges, limited palette, colored pixel-art outline, moderately detailed
> 96x96-style pixel-art design intended for clean nearest-neighbor reduction
> to a final 64x64 Minecraft texture
> Composition/framing: one dominant trash can filling most of the square while
> retaining generous clear padding; centered slightly toward the upper-left to
> leave room for the short lower-right motion trail
> Color palette: pale blue-gray galvanized metal, icy cyan highlights, dark
> teal-gray shadows, and at most a tiny restrained dirty olive accent; do not
> use magenta in the subject
> Scene/backdrop: perfectly flat solid #ff00ff chroma-key background for local
> removal; one uniform color only
> Constraints: exactly one trash can; attached lid; no character, hands,
> targets, second trash can, detached lid, garbage pile, ground, impact burst,
> text, watermark, frame, circular UI plate, green background circle, cast
> shadow, reflection, gradient, blur, antialiasing, smooth vector curves, soft
> glow, or haze. The background must have no shadows, gradients, texture,
> floor plane, or lighting variation. Keep the sprite fully separated from the
> background with crisp edges and generous padding.

## Batarang Storm

> Use case: stylized-concept
> Asset type: production Minecraft Forge mod ability icon source sprite for
> Teeny Craft; final deliverable will be a transparent 64x64 texture
> Primary request: Create a new Batarang Storm ability icon for Batman Beyond.
> Show exactly three futuristic bat-shaped batarangs flying in a loose curved
> formation matching this placement: one batarang near the upper-right, one
> around the left-middle, and one slightly larger primary batarang near the
> lower-center/right. All three must point and travel toward the bottom-right,
> never upward. Their leading tips face bottom-right, with only a few short
> blocky motion marks trailing behind toward the upper-left. The formation
> should feel like a coordinated downward-right storm, not a straight evenly
> spaced line.
> Style/medium: authentic hand-placed Minecraft-style pixel art, strong
> bat-wing silhouettes, deliberate square pixel clusters, crisp hard
> stair-stepped edges, limited palette, moderately detailed 96x96-style
> pixel-art design intended for clean nearest-neighbor reduction to a final
> 64x64 Minecraft texture
> Composition/framing: exactly three clearly separated batarangs, each
> modestly large but with generous outer padding; the lower-center/right
> batarang is only slightly dominant; avoid overlaps and visual noise
> Color palette: near-black and charcoal bodies, subtle cool blue-gray edge
> highlights, restrained dark-crimson red panels on the Batman Beyond
> batarangs; do not use magenta in the subjects
> Scene/backdrop: perfectly flat solid #ff00ff chroma-key background for local
> removal; one uniform color only
> Constraints: exactly three batarangs; all face and fly bottom-right; no
> upward flight direction, no Batman figure, hands, gun, arrows, yellow Batman
> logo, text, watermark, frame, circular UI plate, floor, cast shadow,
> reflection, gradient, blur, antialiasing, smooth vector curves, soft glow, or
> haze. The background must have no shadows, gradients, texture, floor plane,
> or lighting variation. Keep every batarang fully separated from the
> background with crisp edges.

The first generated composition preserved the requested direction and count,
but its thin blade-like silhouettes degraded into streaks in the 32x32 QA
view. A targeted revision removed the detached motion pixels and broadened the
batarangs:

> Use case: stylized-concept
> Asset type: production Minecraft Forge mod ability icon source sprite for
> Teeny Craft; final deliverable will be a transparent 64x64 texture
> Primary request: Revise the Batarang Storm icon for Batman Beyond so the
> three projectiles have much broader, chunkier, immediately recognizable
> bat-shaped boomerang silhouettes instead of thin blade-like streaks. Show
> exactly three batarangs in a compact loose curved formation: one near the
> upper-right, one near the left-middle, and one only slightly larger near the
> lower-center/right. All three face and travel toward the bottom-right, never
> upward. Each batarang should resemble a bold compact crescent/bat-wing shape
> with a deep central notch, broad wings, thick body mass, and clear pointed
> tips, closer in silhouette to a classic batarang than a narrow flying knife.
> Style/medium: authentic hand-placed Minecraft-style pixel art, deliberately
> chunky square pixel clusters, crisp hard stair-stepped edges, strong filled
> silhouettes, limited palette, moderately detailed 96x96-style pixel-art
> design intended for clean nearest-neighbor reduction to a final 64x64
> Minecraft texture
> Composition/framing: exactly three clearly separated broad batarangs;
> compact cluster occupying roughly three quarters of the square; generous
> outer padding; lower-center/right batarang is only about 10 percent larger
> than the others; no overlaps
> Color palette: near-black and charcoal bodies with restrained dark-crimson
> red lower-edge panels and subtle cool blue-gray edge highlights; do not use
> magenta in the subjects
> Scene/backdrop: perfectly flat solid #ff00ff chroma-key background for local
> removal; one uniform color only
> Constraints: exactly three batarangs; all face and fly bottom-right; make
> every silhouette broad and chunky, not thin, spindly, spear-like, bird-like,
> or knife-like. No detached motion pixels or trails, no Batman figure, hands,
> gun, arrows, yellow Batman logo, text, watermark, frame, circular UI plate,
> floor, cast shadow, reflection, gradient, blur, antialiasing, smooth vector
> curves, soft glow, or haze. The background must have no shadows, gradients,
> texture, floor plane, or lighting variation. Keep every batarang fully
> separated from the background with crisp edges and substantial visible black
> body area.

## Good Luck

> Use case: stylized-concept
> Asset type: production Minecraft Forge mod ability icon source sprite for
> Teeny Craft; final deliverable will be a transparent 64x64 texture
> Primary request: Create the Good Luck ability icon for the Riddler. Show
> exactly three four-leaf clovers as a compact luck-buff emblem: one large
> dominant four-leaf clover in the upper-center, one medium four-leaf clover at
> the lower-left, and one smaller but still unmistakably four-leaf clover at
> the lower-right. Preserve four distinct rounded blocky leaves on every
> clover, with short simple stems. The three-clover hierarchy should closely
> evoke a classic lucky-clover ability icon without copying a circular UI
> frame.
> Style/medium: authentic hand-placed Minecraft-style pixel art, deliberate
> square pixel clusters, crisp hard stair-stepped edges, limited palette, dark
> colored outlines, moderately detailed 96x96-style pixel-art design intended
> for clean nearest-neighbor reduction to a final 64x64 Minecraft texture
> Composition/framing: centered action-emblem composition with exactly three
> separated clovers; large upper-center, medium lower-left, small lower-right;
> enough space between silhouettes to read clearly, generous transparent outer
> padding
> Color palette: vivid lime-green highlights, saturated leaf green midtones,
> and deep dark Riddler-green outlines and shadows; do not use magenta in the
> clovers
> Scene/backdrop: perfectly flat solid #ff00ff chroma-key background for local
> removal; one uniform color only
> Constraints: exactly three clovers; every clover has exactly four leaves; no
> yellow ring, no dark-green circular background, no question mark, no Riddler
> figure, no hands, no additional leaves, no flowers, no text, watermark,
> frame, circular UI plate, ground, cast shadow, reflection, gradient, blur,
> antialiasing, smooth vector curves, soft glow, haze, or floating sparkles.
> The background must have no shadows, gradients, texture, floor plane, or
> lighting variation. Keep the clovers fully separated from the background
> with crisp edges.
