# Teeny Craft ability icon prompts — round 11

Generated with the built-in image-generation path on a flat magenta chroma background. Production textures were reduced to 64x64 with nearest-neighbor sampling and binary alpha.

## Soul Punch — initial Raven master (superseded)

Use case: stylized-concept

Asset type: production Minecraft Forge mod ability icon source, intended for nearest-neighbor reduction to a 64x64 transparent RGBA texture and readable at 32x32.

Primary request: Create the Raven Soul Punch icon as exactly one very large solid black clenched fist punching diagonally from the bottom-right toward the upper-left. This is a side-directed punch, NOT a fist punching toward the viewer. The short wrist enters from the bottom-right, and the fist points toward the upper-left. The palm-side of the clenched hand faces the viewer, so the viewer sees the folded fingers across the palm side.

Subject details: The fist occupies most of the canvas. Show only a slightly narrower, very short wrist. The cropped end of the wrist is not a straight line; it ends in exactly three adjacent rounded triangular lobes. Use thin, consistent white interior pixel lines to separate the clenched fingers and depict simple fingernails. Add exactly two short white lines across the upper portions of the top fingers. The fist fill is essentially one pure black color with no internal shading. Add only a very thin very-light-gray exterior readability outline around the black silhouette, consistent in thickness.

Style/medium: authentic hand-placed Minecraft-style pixel art, deliberate chunky square pixel clusters, hard edges, limited palette, strong silhouette, no smooth vector curves.

Composition: diagonal bottom-right to upper-left, generous transparent-intent padding, no clipping.

Scene/backdrop: perfectly flat solid `#ff00ff` chroma-key background for removal, uniform edge-to-edge, no texture, gradients, floor, shadows, or lighting variation. Do not use magenta in the subject.

Constraints: exactly one fist; palm-side folded fingers visible; bottom-right to upper-left direction; only black, white, and very light gray; readable at 32x32.

Avoid: fist aimed toward camera, upright fist, sleeve, arm beyond the short wrist, aura, magic wisps, particles, impact burst, extra hands, text, frame, watermark, blur, antialiasing, gradients, soft glow, cast shadow.

### Trigon variant processing

The Trigon texture is not a separate generation. It is a deterministic palette swap of the finalized Raven 64x64 texture: black fill `(0, 0, 0)` becomes saturated red `(239, 16, 28)`. Alpha, silhouette, light outline, white finger divisions, nails, and highlight pixels remain identical.

## Soul Punch — final Raven revision

Use case: precise-object-edit

Asset type: revised production Minecraft Forge mod ability icon source, intended for nearest-neighbor reduction to a 64x64 transparent RGBA texture and readable at 32x32.

Input image: edit target. Replace the hand anatomy and orientation while preserving the isolated black-and-white pixel-art concept and flat magenta chroma-key backdrop.

Primary change: Redraw the subject as exactly one clenched RIGHT hand punching diagonally from the bottom-right toward the upper-left. The short narrow wrist enters at the bottom-right. The clenched fist is at the upper-left. The palm-side of the fist faces the viewer.

Critical anatomy and orientation:

- The four curled fingers must be grouped side-by-side in one CROSSWISE row at the end opposite the wrist.
- The finger row is perpendicular to the wrist/forearm axis; do NOT draw four long finger columns parallel to the wrist.
- Think of rotating the previous finger arrangement 90 degrees counterclockwise while keeping the punch traveling lower-right to upper-left.
- This is a right hand. The thumb is clearly visible on the upper-right side of the fist, crossing/resting over the curled fingers.
- The fist is broad and large; the wrist is noticeably thinner, approximately half the fist width, and only a short section is visible.
- Retain the three adjacent rounded triangular lobes at the cropped lower-right wrist end.

Line treatment:

- Solid pure-black hand with no internal black shading.
- Absolutely NO white, gray, or colored exterior outline around the hand silhouette.
- Use thin, consistently thick white pixel lines only INSIDE the hand to separate the four curled fingers, define the thumb, and suggest simple nails.
- Include exactly two short white highlight lines on the upper finger surfaces.
- Make the outer silhouette slightly rounder and more cartoon-like than the input, using restrained stepped pixel curves; not bubbly or overly soft.

Style: authentic hand-placed Minecraft-style pixel art, chunky deliberate square clusters, crisp hard edges, limited palette, strong readable silhouette.

Composition: one large fist with enough padding, diagonal lower-right to upper-left, no clipping.

Backdrop: perfectly flat solid `#ff00ff` chroma-key background, uniform edge-to-edge. Do not use magenta in the subject.

Preserve: black hand, white internal linework, isolated sprite, pixel-art scale.

Avoid: exterior outline, fist pointing toward the camera, left hand, thumb on lower-left, longitudinal finger columns, open palm, sleeve, long forearm, aura, particles, impact burst, text, frame, gradients, antialiasing, blur, soft glow, watermark.

### Final Trigon variant processing

The final red variant is derived from this revised 64x64 Raven texture. Only pure-black fill pixels change to saturated red `(239, 16, 28)`; the alpha mask and all white internal linework remain identical.

## Grappling Hook

Use case: stylized-concept

Asset type: production Minecraft Forge mod aimed ability icon source, intended for nearest-neighbor reduction to a 64x64 transparent RGBA Minecraft item texture and readable at 32x32.

Primary request: Create Batman's Grappling Hook ability icon as exactly one compact physical grapnel launcher firing exactly one three-pronged grappling hook. The launcher is held from the bottom-right and aims diagonally toward the upper-left. A taut pale steel-gray cable leaves the muzzle and connects visibly to the launched hook near the upper-left.

Subject details: The black launcher occupies the lower-right half, with a readable pistol grip, compact cylindrical cable housing, pointed bat-ear-like rear sight, and a restrained bat-wing contour around the muzzle. The three-pronged steel grappling hook is large enough to read at 32x32; its two outer hooks spread slightly like bat wings. Include only two or three short hard-edged motion pixels at the muzzle. One tiny restrained red technology indicator may appear on the launcher.

Style/medium: authentic hand-placed Minecraft-style pixel art, deliberate chunky square clusters, crisp hard edges, strong diagonal silhouette, limited palette, no soft rendering.

Color palette: near-black, charcoal, cool blue-gray highlights, steel gray, pale gray cable, exactly one tiny red indicator.

Composition: bottom-right to upper-left aim; launcher is dominant but the hook and cable remain unmistakable; sufficient padding.

Scene/backdrop: perfectly flat solid `#ff00ff` chroma-key background for removal, uniform edge-to-edge, no texture, gradients, floor, shadows, or lighting variation. Do not use magenta in the subject.

Constraints: exactly one launcher, exactly one connected cable, exactly one three-pronged hook; physical weapon, no character.

Avoid: Batman figure, Batman-shaped ghost, translucent apparition, fishing rod, rope coil, target, enemy, defense-down badge, text, frame, watermark, blur, antialiasing, gradients, soft glow, cast shadow.

## Jiu Jitsu

Use case: stylized-concept

Asset type: production Minecraft Forge mod action-display ability icon source, intended for nearest-neighbor reduction to a 64x64 transparent RGBA texture and readable at 32x32.

Primary request: Create a cartoon combat impact icon whose exact visible text is `kaPow!`, spelled k-a-P-o-w-exclamation-mark. The uppercase P is slightly taller and wider than every other letter. All other capitalization must remain exact: lowercase k, lowercase a, uppercase P, lowercase o, lowercase w, then `!`.

Subject details: The saturated blue word `kaPow!` dominates almost the entire horizontal canvas. Use chunky hand-lettered pixel-art characters with a dark navy one-pixel edge or shadow so adjacent letters remain distinct. Let the letters bounce slightly along the baseline. Behind the word is exactly one compressed white comic impact burst. The text is larger than the burst, so the burst is visible mainly as pointed white spikes above and below the letters and as a few small white areas between letters. Add only a few deliberate light-gray 2x2 pixel dot clusters on the visible white burst areas.

Style/medium: authentic hand-placed Minecraft-style pixel art, chunky square clusters, hard edges, limited palette, energetic comic-book impact lettering.

Composition: wide horizontal action emblem, centered, generous padding, text fully legible at 32x32.

Text (verbatim): `kaPow!`

Scene/backdrop: perfectly flat solid `#ff00ff` chroma-key background for removal, uniform edge-to-edge, no texture, gradients, floor, shadows, or lighting variation. Do not use magenta in the subject.

Constraints: exact spelling and capitalization `kaPow!`; P slightly bigger; saturated blue lettering; one white burst partially hidden by oversized text.

Avoid: different spelling, all caps, speech balloon, rectangular comic panel, fists, characters, Batman, extra words, background scene, frame, watermark, blur, antialiasing, gradients, soft glow, cast shadow.

## Processing notes

- Flat magenta was removed with the installed chroma-key helper.
- Sources were cropped and reduced to 64x64 using nearest-neighbor sampling.
- Final alpha is binary; all corners are transparent.
- Grappling Hook received a restrained charcoal/blue-gray readability lift after the first 32x32 QA check.
- The final four textures were reviewed at 64x64, 32x32, and 16x16 in `ability_icons_qa.png`.
