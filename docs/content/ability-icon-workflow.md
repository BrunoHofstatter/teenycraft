# Ability Icon Generation Workflow

## Purpose

This document defines the collaborative workflow for designing and generating Teeny Craft ability icons. It is written so a new chat can continue producing icons without having to reconstruct the visual philosophy, technical requirements, or approval process.

The normal working unit is a batch of three abilities. An ability may require more than one texture when it has multiple visual states, such as the throwable Bat Mine and its activation button.

## Current Visual Philosophy

Ability icons should show **the ability happening**, not necessarily a literal weapon or object that the Minecraft player is holding.

The ability items are battle controls. Minecraft renders them in the player's hand, but that technical presentation should not force every ability into the shape of a conventional tool. This matters for moves such as Around the World, Heroic Pose, Dance, healing, fear, team attacks, and other actions that cannot be represented honestly as held objects.

The design priority is:

1. Communicate the ability immediately.
2. Use the additional room of a 64x64 production texture without depending on fine detail for recognition.
3. Remain readable when displayed at approximately 32x32 in the UI.
4. Look natural enough when Minecraft renders the item in hand.
5. Match the visual family of the other Teeny Craft ability icons.

Physical attacks and weapons can use a Minecraft-tool diagonal, usually bottom-left to top-right. Forward punches or thrusts may work better from bottom-right to top-left when that makes the arm appear to enter naturally from the player's side. Symbolic abilities can use a centered emblem or a snapshot of the action.

Do not bake a button frame, rarity frame, or circular UI background into the texture. The icon should be an isolated sprite with transparent space around it.

## Relationship To Teeny Titans Go Figure

Teeny Craft is inspired by Teeny Titans Go Figure, and many abilities already have a recognizable idea or icon in the original game.

Use the original game as a **design reference**, not as an instruction to copy an icon pixel for pixel. Preserve the concept that makes the move recognizable, then reinterpret its composition, silhouette, motion, and shading as Minecraft-style pixel art.

Examples:

- Amazonian Beatdown keeps the repeated-fist concept, but uses a strong diagonal fist and bracer composition suitable for a Minecraft item.
- Around the World keeps Earth and the speed orbit because they are the clearest description of the move, but uses a blocky globe and a new pixel-art trail composition.
- Arrow Storm uses a distinct bow releasing several arrows instead of copying the vanilla bow or the original game's exact icon.

The user often knows how an icon appears in the original game. Treat their description as authoritative creative direction. Suggestions should improve clarity and Minecraft presentation without discarding the recognizable original-game idea.

## Message And Approval Flow

### 1. User supplies a rough batch

The user normally provides three ability ids and explains whatever they already know about each icon. Useful information includes:

- which figure uses the ability;
- what physically happens during the move;
- how the original-game icon represents it;
- important objects, colors, symbols, or character details;
- preferred direction or hand presentation;
- whether the ability has multiple visual states;
- anything that must not appear.

The description can be informal or voice-transcribed. Infer obvious wording mistakes from context instead of demanding perfect terminology.

### 2. Chat checks the runtime context

Before proposing the art, read each ability JSON under:

`src/main/resources/data/teenycraft/abilities/`

Use the JSON to confirm the description, hit type, multi-hit count, group damage, effects, and other mechanics that might deserve visual treatment. Check the relevant figure JSON when character ownership or palette is unclear.

The Google Sheet `Teeny Craft - Abilities` is optional. Prefer the local JSON when it contains enough information. Use the sheet only when the JSON lacks important design context or the user specifically requests it.

Do not invent new gameplay behavior from the icon. Mechanics inform the picture; the picture does not change the mechanics.

### 3. Chat polishes the ideas into a concrete plan

For each ability, respond with a specific proposal covering:

- the dominant subject and silhouette;
- its direction and position inside the 64x64 production canvas;
- how motion, multiple hits, group damage, status, or impact will be shown;
- the main palette and recognizable character-specific details;
- how the result will remain readable in the UI and in hand;
- details to simplify or omit because they would become noise when the texture is displayed at 32x32.

This stage is collaborative design, not generation. Explain disagreements when a requested detail is likely to become unreadable, and suggest the closest readable alternative.

### 4. User reviews the plan

Wait for the user to approve or adjust the concrete proposals. Do not generate the batch before this approval unless the user explicitly asks to skip the planning stage.

### 5. Chat generates one source per texture

After approval, generate each distinct texture separately. Three abilities may therefore require four or more image-generation calls when an ability has multiple states.

Use the built-in image-generation path by default. Request an isolated Minecraft-style pixel-art sprite on a perfectly flat removable chroma background. Use a chroma color that does not conflict with the subject; `#ff00ff` works well for most Teeny Craft icons.

Prompts should state:

- production Minecraft mod ability icon;
- authentic hand-placed pixel-art appearance;
- strong silhouette and deliberate square pixel clusters;
- precise subject count and composition;
- limited palette and hard edges;
- a moderately detailed 96x96 pixel-art design intended for clean nearest-neighbor reduction to a final 64x64 Minecraft texture;
- transparent final intent;
- no text, watermark, frame, scene, blur, antialiasing, smooth vector curves, or soft glow haze.

Do not ask the image generator to optimize for 32x32 readability. That is a
local QA requirement after generation and reduction, not part of the
generation prompt.

Use words such as `exactly one`, `exactly two`, or `exactly three` when object count matters.

### 6. Chat processes and inspects the textures

For every generated source:

1. Copy the source into `output/imagegen/ability_icons_roundN/`.
2. Remove the flat chroma background locally.
3. Remove any remaining magenta or chroma-colored edge pixels.
4. Resize to exactly 64x64 with nearest-neighbor sampling.
5. Enforce a real alpha channel with transparent corners.
6. Create an enlarged nearest-neighbor preview on a checkerboard for review.
7. Inspect the enlarged preview, the true 64x64 texture, and a reduced 32x32 QA view.
8. Regenerate or make a targeted revision if the subject becomes unclear at any required review size.
9. Record the final prompt set in the round's `prompts.md`.

Generated art often looks good while enlarged but fails after reduction. The 64x64 texture is the production asset, but readability at its smaller rendered sizes remains the deciding test.

### Generated source resolution versus production resolution

The image generator does not create a literal 96x96 pixel grid. It currently returns large raster sources, commonly 1254x1254, and interprets pixel-art and target-size instructions only approximately. These sources can contain substantially more visual information than the final 64x64 texture. Their large dimensions are not merely a nearest-neighbor enlargement of a finished low-resolution sprite.

Prompting for a moderately detailed 96x96 pixel-art design is an intentional,
simple nudge toward finer effective detail than asking directly for a chunky
64x64 icon. Prompts should say that the 96x96-style design is intended for
clean nearest-neighbor reduction to the final 64x64 texture. Do not mention
32x32 readability in the generation prompt; inspect that size locally during
QA instead.

The saved large source is the reusable art master for processing purposes, not a production texture. Keep the unmodified generated source and the background-removed transparent source so later exports can be produced without recreating the inspiration, composition, or prompt. If the production resolution changes or a cleaner reduction becomes useful, process these saved sources first and regenerate only the icons whose sources do not reduce cleanly.

### 7. Save approved-direction textures in the project

The production textures belong in:

`src/main/resources/assets/teenycraft/textures/item/`

Use these names:

- normal icon: `ability_<ability_id>.png`
- figure-selected color variant: `ability_<ability_id>__variant_<variant>.png`
- extra state: `ability_<ability_id>_<state>.png`

Examples: `ability_whale_drop__variant_green.png` and `ability_bat_mine_button.png`.

Color variant ids use lowercase snake case and describe the visual treatment, such as `green`, `red`, or `yellow`. The normal `ability_<ability_id>.png` remains the default and requires no figure assignment. To select an alternate texture, add the optional figure JSON map:

```json
"ability_icon_variants": {
  "whale_drop": "green"
}
```

That is the complete authoring contract for an ability whose base custom icon is already integrated: add the correctly named variant PNG and assign it from each figure that uses it. Do not create another ability JSON, `texture_index`, display-category entry, or model JSON. The generated variant inherits the base ability's display category automatically.

Current assignments are:

- Beast Boy: green `whale_drop`; Aquaman uses the default blue icon.
- Trigon and Argyle Trigon: red `soul_punch`; Raven uses the default black icon.
- Jessica Cruz and John Stewart: green `construct_beam`; Sinestro uses the default yellow icon.

Golden abilities reuse the normal texture and receive Minecraft's enchantment glint at runtime. Do not generate a separate golden texture.

After generation and QA, place the textures in the assets folder and complete required category and figure-variant assignments without waiting for another approval; do not modify item models, wrapper overrides, fallback mappings, predicates, or state-switching logic unless explicitly requested.

### 8. User reviews the generated batch

Show enlarged checkerboard previews and link the actual 64x64 files. Include reduced 32x32 QA views when they materially help review. The user can approve the batch, request a focused change, or provide the next three abilities.

## Technical Art Specification

- Production canvas: 64x64 pixels.
- Format: PNG with RGBA color.
- Background: fully transparent.
- Alpha: preferably binary, with pixels either fully opaque or fully transparent.
- Scaling: nearest-neighbor only.
- Edges: crisp, intentional, and free from chroma-colored fringe.
- Style: Minecraft-like, hand-placed pixel art rather than a small smooth illustration.
- Palette: limited enough to feel authored, with colored shadows and highlights where useful.
- Silhouette: one dominant read at a glance.
- Padding: enough transparent space to prevent clipping in the UI or hand.
- Text: none.
- Built-in border: none.
- Antialiasing and blur: none.

The icon must still be recognizable when displayed at approximately 32x32, even though the production texture is 64x64.

The 64x64 choice is an art-quality decision, not a performance requirement. A 64x64 RGBA texture contains four times as many texels as a 32x32 texture, but ability icons remain extremely small relative to the rest of Minecraft's resources. For the expected number of abilities, the additional disk, atlas, and memory cost is negligible. Prefer 64x64 over 48x48 because 64x64 is a conventional power-of-two texture size and is more predictable for Minecraft texture atlases, mipmaps, scaling, and tooling.

## Composition Guidelines

### Physical melee and held constructs

- Prefer the same broad diagonal used by Minecraft tools when appropriate.
- Let the handle, arm, or shaft approach from a lower corner.
- Keep the attacking end large enough to be the focal point.
- Use a restrained arc, echo, or impact pixels to show motion.

### Multi-hit attacks

- Use subordinate echoes, repeated projectiles, or separated motion marks.
- Keep one primary subject stronger than the echoes.
- Avoid several equally detailed overlapping objects, which become noise after reduction.

### Ranged and group attacks

- Show spread, orbit, repeated projectiles, or a broad action path.
- Group damage does not require a special universal badge; communicate it naturally when the move supports it.

### Aimed abilities

- Prefer an approximately 45-degree composition from a lower corner toward the opposite upper corner when the subject supports it. This generally aligns well with the shared aimed-item display transforms and helps the sprite appear to project naturally from the player's hand.
- Treat 45 degrees as a useful default, not a requirement. Use another angle when the ability's silhouette, original concept, hand relationship, or reduced-size readability is clearly better that way.
- Keep the firing origin or grip near the lower corner and the attacking end large and distinct enough to remain readable after reduction.

### Buffs, debuffs, healing, and symbolic actions

- Use a centered emblem or a snapshot of the effect.
- Favor a memorable shape over literal hand compatibility.
- Do not force an abstract ability to become a weapon.

### Multi-state abilities

- Treat each state as its own texture with a related palette and visual identity.
- The active-state icon should prioritize instant recognition over decorative detail.
- Example: the Bat Mine throwable is a thin black bat-shaped mine with a red light, while its activation state is a large simple red button.
- Bat Mine is the only current multi-state ability. Its button path is separate from color variants, and Bat Mine does not need variant-plus-state support.

## Character And Color Identity

Use character-specific materials and colors when they are essential to recognizing the move:

- Wonder Woman: warm skin, crimson accents, and a bright gold bracer.
- Flash: red-and-gold speed trails and lightning shapes.
- Sinestro: an entirely yellow hard-light construct rather than a wooden or metal weapon.
- Harley Quinn: playful danger motifs such as a rough red heart painted on a black bomb.
- Batman: black, charcoal edge highlights, precise silhouettes, and restrained red technology lights.

Do not add a full character when a smaller signature element communicates the ability more clearly.

## Repository Integration Notes

An approved `ability_<id>.png` texture and a display-category assignment enter the standard custom-icon pipeline together. The Gradle `generateAbilityIconModels` task reads the current `texture_index` and `assets/teenycraft/ability_display_categories.json`, generates a categorized `ability_<id>.json`, and generates matching overrides for all three ability-slot items during resource processing.

For an existing figure ability, integration therefore requires only:

1. Place the 64x64 RGBA texture at `assets/teenycraft/textures/item/ability_<id>.png` using the exact ability JSON id.
2. Add the ability id to exactly one display category in `assets/teenycraft/ability_display_categories.json`.
3. Run a normal Gradle build or launch task. Resource processing runs `generateAbilityIconModels` automatically.

For a color variant of an already integrated icon, use the `ability_<id>__variant_<variant>.png` filename and add `"<ability_id>": "<variant>"` under the relevant figure's optional `ability_icon_variants` object. The generator validates both sides and creates the inherited model and wrapper predicates.

Do not edit `AbilityIconManager.FALLBACKS`, create a model JSON, or edit the three slot wrappers manually. Model baking discovers the exact texture id from the active resources and allows only discovered ids into the generated custom-model table, so the custom texture automatically takes precedence without leaking into placeholder abilities.

Generated custom models take priority over `AbilityIconManager.FALLBACKS`. The fallback entry may remain in place and will be used if the custom model is unavailable. Abilities without approved custom textures continue to use their vanilla placeholders, with `ability_default` as the final fallback.

The unique Bat Mine state still uses its explicit item property and generator entry: `ability_bat_mine_button.png` is selected by `teenycraft:is_button` while a mine is active. Special states inherit the base ability display category unless `state_overrides` selects another supported category. This state path is not part of the color-variant contract.

Golden abilities always use the normal icon model plus the runtime enchantment glint. The icon pipeline does not use `_golden.png`, `_golden.json`, or an `is_golden` model predicate.

### Display transform test items

The Teeny Craft creative tab currently includes inert display-test items whose committed models are named `display_test_*.json` under `assets/teenycraft/models/item/`. They inherit the same shared display parents as generated battle abilities, so editing a profile updates its real abilities and test items together without changing battle behavior. The reference textures are Axe to Grind for `tool`, Around the World for `action`, Amazonian Beatdown for `fist`, and Birdarang plus Bat Mine for `throwable`.

See [ability-item-displays.md](ability-item-displays.md) for the complete category contract, current assignments, shared model paths, and planned aimed arm poses.

## Final Quality Checklist

Before reporting a batch complete, verify:

- [ ] The icon matches the user's description and original-game intent.
- [ ] The actual ability mechanic was checked in JSON.
- [ ] The ability is shown happening, unless a deliberately simple state icon is clearer.
- [ ] The sprite looks acceptable in a Minecraft hand.
- [ ] The production texture reads at 64x64 and its silhouette remains clear at 32x32.
- [ ] Object counts are correct.
- [ ] Character-specific colors and objects are correct.
- [ ] No unwanted character, text, frame, background scene, or extra prop appeared.
- [ ] Canvas is exactly 64x64 RGBA PNG.
- [ ] Corners are transparent and chroma fringe is gone.
- [ ] Nearest-neighbor scaling was used.
- [ ] Final texture is in `assets/teenycraft/textures/item/`.
- [ ] Source, checkerboard preview, and prompt notes are saved under `output/imagegen/`.
- [ ] Models and code were left untouched unless integration was explicitly requested.

## Copy-Paste Brief For A New Chat

Use this message to start or re-establish the workflow in another chat:

> We are creating 64x64 transparent pixel-art ability icons for Teeny Craft, a Minecraft Forge mod inspired by Teeny Titans Go Figure. Read `docs/content/ability-icon-workflow.md` and the JSON files for the abilities I name. Icons should show the ability happening, not necessarily a literal held weapon, but physical attacks should still be composed so they look good in a Minecraft hand. The original game's icon concept is useful inspiration, but do not copy its exact artwork. I will give rough ideas for about three abilities. First, polish my ideas and propose a concrete composition, palette, motion treatment, and simplifications for each. Wait for my review. After I approve, generate one large source image per required texture, remove the chroma background, reduce it to a clean 64x64 RGBA PNG with nearest-neighbor sampling, inspect it at 64x64 and in a reduced 32x32 QA view, and place it in `src/main/resources/assets/teenycraft/textures/item/`. Treat the large generated source as a reusable processing master: it contains more information than the final texture and should be retained rather than recreated if another export is needed. Generation prompts should request a moderately detailed 96x96 pixel-art design intended for clean nearest-neighbor reduction to the final 64x64 texture. Do not mention 32x32 readability in generation prompts; check it only during local QA. Save sources, enlarged checkerboard previews, QA views, and final prompts under `output/imagegen/ability_icons_roundN/`. Do not change models, fallback mappings, or ability logic unless I explicitly ask.
