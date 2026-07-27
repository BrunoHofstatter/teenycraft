# Ability Item Displays

## Purpose

Define how custom ability textures are positioned in first person and third person, how each ability selects a shared display category, and which presentation work remains planned.

## Current Status

The custom ability-icon pipeline has five client-side item display categories:

- `tool`
- `action`
- `fist`
- `throwable`
- `aimed`

Each category owns a shared Minecraft item-model parent under `assets/teenycraft/models/item/ability_display/`. The Gradle `generateAbilityIconModels` task reads the central category manifest and generates each custom ability model with the appropriate shared parent. The three battle-slot wrapper items, numeric ability predicates, model-bake safety wrapper, golden glint, and battle behavior are unchanged.

The display system currently changes baked item-model transforms only. It does not apply custom player arm poses.

## Source Of Truth

- Category assignments: [`src/main/resources/assets/teenycraft/ability_display_categories.json`](../../src/main/resources/assets/teenycraft/ability_display_categories.json)
- Shared model profiles: [`src/main/resources/assets/teenycraft/models/item/ability_display`](../../src/main/resources/assets/teenycraft/models/item/ability_display)
- Model generation and validation: [`build.gradle`](../../build.gradle)
- Dynamic ability model selection: [`src/main/java/bruhof/teenycraft/client/model/AbilityModelWrapper.java`](../../src/main/java/bruhof/teenycraft/client/model/AbilityModelWrapper.java)
- Ability item NBT and behavior: [`src/main/java/bruhof/teenycraft/item/custom/battle/ItemAbility.java`](../../src/main/java/bruhof/teenycraft/item/custom/battle/ItemAbility.java)
- Icon discovery and vanilla fallbacks: [`src/main/java/bruhof/teenycraft/client/AbilityIconManager.java`](../../src/main/java/bruhof/teenycraft/client/AbilityIconManager.java)

## Why Categories Are Centralized

Display category is client presentation metadata rather than damage, targeting, effect, or balance data. It therefore lives in one asset-side manifest instead of inside every gameplay ability JSON.

The central file makes the full art classification reviewable in one place, avoids modifying gameplay records for visual-only changes, and lets the build validate that every finished custom icon has exactly one presentation category.

The manifest groups ability ids into arrays:

```json
{
  "tool": ["axe_to_grind"],
  "action": ["around_the_world"],
  "fist": ["amazonian_beatdown"],
  "throwable": ["bat_mine", "birdarang"],
  "aimed": [],
  "state_overrides": {
    "bat_mine_button": "action"
  }
}
```

An ability may be categorized before its custom texture exists. However, a finished `ability_<id>.png` cannot enter the generated model pipeline until its id appears in exactly one category.

## Display Categories

### Tool

Reference ability: `axe_to_grind`.

Use for a physical weapon or long implement that the player appears to grip, including axes, swords, staffs, and mallets. First person shows most of the weapon while keeping the hand relationship believable. Third person uses a larger-than-vanilla handheld presentation with compensating rotation and translation.

Profile: `teenycraft:item/ability_display/tool`.

### Action

Reference ability: `around_the_world`.

Use when the texture depicts an action, event, buff, debuff, status, or other concept that is not literally an object in the player's hand. First person presents almost the whole texture on a plane close to parallel with the screen, similar in spirit to viewing a map. Third person keeps the icon readable and larger than a normal generated item.

Profile: `teenycraft:item/ability_display/action`.

### Fist

Reference ability: `amazonian_beatdown`.

Use for punches, fists, gauntlets, and similar unarmed attacks. The third-person transform places the sprite beside the player arm so it reads as wrapping or augmenting the real hand instead of as a detached fist being held. First person retains a hand-associated attack presentation.

Profile: `teenycraft:item/ability_display/fist`.

### Throwable

Reference abilities: `birdarang` and `bat_mine`.

Use for a discrete object that the player appears ready to throw, such as a batarang, mine, or bomb. The same finalized profile worked for both the upright Birdarang composition and the diagonally oriented Bat Mine composition, so no per-texture transform override is currently needed.

Profile: `teenycraft:item/ability_display/throwable`.

### Aimed

Reserve for a physical ranged weapon that the player should eventually appear to aim, such as a blaster or cannon. Its model profile is already independent, but its current first-person and third-person values intentionally duplicate `tool` until the broader texture library is ready for pose testing.

When practical, compose aimed textures at approximately 45 degrees from a lower corner toward the opposite upper corner. That diagonal generally sits most naturally in the shared aimed transform. It is a preference rather than a validation rule: a different angle is appropriate when it produces a clearer silhouette, a more faithful ability concept, or better reduced-size readability.

Profile: `teenycraft:item/ability_display/aimed`.

## Current Assignments

| Category | Ability ids |
| --- | --- |
| `tool` | `axe_to_grind`, `harleys_mallet`, `scarab_swords`, `staff_slam` |
| `action` | `around_the_world`, `arrow_storm`, `bang`, `batarang_storm`, `battery_drain`, `black_hole`, `break_the_bat`, `break_you`, `burp_shield`, `counterattack`, `curse`, `dance`, `deadly_kiss`, `evil_laugh`, `freeze_breath`, `hooded_void`, `heroic_pose`, `jiu_jitsu`, `laser_eyes_manta`, `laser_sneeze`, `light_shield`, `lightning_bubble`, `macho_smooch`, `mind_control`, `puddin_pucker`, `tea_chi`, `tea_time`, `venom`, `waffles`, `whale_drop`, `raspberry`, `science` |
| `fist` | `amazonian_beatdown`, `butterfly`, `cat_scratch`, `fear`, `lightning_fists`, `soul_punch`, `punchies`, `quick_punch_stun` |
| `throwable` | `bat_mine`, `birdarang`, `hooded_barrage`, `journalism`, `starfish_chuck`, `tea_toss`, `ultimate_batarang` |
| `aimed` | `camera_flash`, `construct_beam`, `energy_cannon`, `grappling_hook`, `lightning_shot`, `missile_barrage`, `plasma_shot`, `sonic_cannon`, `trident_throw` |

## Generation Flow

1. An approved texture is added as `assets/teenycraft/textures/item/ability_<id>.png`.
2. The ability id is added to exactly one array in `ability_display_categories.json`.
3. `generateAbilityIconModels` validates the ability, category, texture index, and figure usage.
4. The task generates `ability_<id>.json` with the selected shared display parent and the ability texture as `layer0`.
5. The task regenerates the model overrides for `ability_1`, `ability_2`, and `ability_3`.
6. At runtime, the held stack's `AbilityID` selects the generated child model; that model inherits its first-person and third-person transforms from the category profile.

Example generated child model:

```json
{
  "parent": "teenycraft:item/ability_display/tool",
  "textures": {
    "layer0": "teenycraft:item/ability_axe_to_grind"
  }
}
```

Do not edit files under `build/generated/ability-icon-resources`; they are deleted and recreated by the generation task.

## Validation Contract

`generateAbilityIconModels` fails when:

- the manifest is missing or is not a JSON object
- one of the five required category arrays is missing or malformed
- the manifest contains an unknown top-level key
- a required shared profile model is missing
- an ability id is blank or padded with whitespace
- an ability appears in more than one category
- a category references a missing ability
- a category references an internal/non-figure-equipped ability
- a finished custom icon has no category assignment
- a state override uses an unknown category or unsupported generated state

This validation applies to custom icon textures. Abilities that still use `AbilityIconManager.FALLBACKS` retain the selected vanilla item's own model transforms until a custom texture is added.

## Special Visual States

Special generated states inherit the base ability's display category unless `state_overrides` explicitly selects another category.

Bat Mine is the current example. The throwable mine uses the `throwable` category, while `ability_bat_mine_button.png` explicitly uses the `action` category so the remote button is presented as a readable action icon:

```json
{
  "state_overrides": {
    "bat_mine_button": "action"
  }
}
```

The generator accepts overrides only for states it knows how to generate. Golden abilities are not separate states here; they reuse the normal model and add the runtime enchantment glint.

## Display Test Items

Eight inert items remain in the Teeny Craft creative tab as a visual authoring harness:

- Axe to Grind for `tool`
- Around the World for `action`
- Amazonian Beatdown for `fist`
- Birdarang and Bat Mine for `throwable`
- Trident Throw, Construct Beam, and Grappling Hook for `aimed`

Their committed `display_test_*.json` files inherit the same shared profiles as real generated abilities. Editing a shared profile therefore updates both actual battle abilities and the corresponding creative test item. The tests have no battle behavior.

The test items may be removed after the profiles have been verified across the full texture library, or retained as development tools.

## Editing And Reloading

To change a category's transforms, edit its shared model under `assets/teenycraft/models/item/ability_display/`. To reclassify an ability, move its id between arrays in `ability_display_categories.json`.

During development:

1. Run `./gradlew.bat processResources` after editing a shared profile or the category manifest.
2. In the running client, press `F3 + T` to reload models and textures.
3. If an IDE-launched client does not see the processed resource copy, restart it through `./gradlew.bat runClient`.

Changing only model JSON does not require a Java recompile. Adding or removing registered test items does require restarting the game because item registries are established at startup.

Verify main-hand first person, the player's left-handed setting, own-player `F5`, another player's view, attack swings, GUI/hotbar rendering, golden glint, and special-state transitions.

## Model Transform Notes

Each hand context may define:

- `rotation: [x, y, z]` in degrees
- `translation: [x, y, z]` in model space
- `scale: [x, y, z]` as multipliers

First-person and third-person right/left-hand transforms are independent. Model-space translation does not always map directly to screen direction after rotation, so visual iteration remains necessary. Keep scale axes equal unless deliberate stretching is desired.

Minecraft automatically mirrors the X translation and negates the configured Y and Z rotations when applying a left-hand item transform. When a profile should appear as a true visual mirror, start with the same source rotation values for both first-person hands and let the renderer perform that conversion once. Manually negating Y and Z in the left-hand JSON can double-invert a non-cardinal rotation; exact 90-degree flat-sprite profiles may visually conceal that mistake.

Third-person transforms control the item relative to the rendered hand, including how other players see it and how the local player sees it in `F5`. They do not change the arm pose itself.

## Planned Aimed Arm Poses

The aimed category currently uses normal held-item arm behavior. A future client-side extension should add optional arm-pose metadata separately from the item display category rather than baking player animation concerns into model JSON.

Forge 1.20.1 exposes `IClientItemExtensions.getArmPose`. The intended implementation is:

1. Register client item extensions for `ItemAbility`.
2. Read `ItemAbility.TAG_ID` from the held stack.
3. Resolve that ability's presentation metadata on the client.
4. Return a custom `HumanoidModel.ArmPose` for abilities that request an aiming pose.
5. Let the held item follow the transformed hand while leaving first-person item-model transforms independent.

Likely future pose values are:

- `normal`
- `aim_one_handed`
- `aim_two_handed`
- a bow-specific path using Minecraft's active bow-use animation

One-handed blasters may use a straight-forward holding arm while leaving the other arm normal. Large cannons may require a two-handed pose. Bows should not reuse a generic straight-arm pose because drawing a bow depends on both arms and active item-use state.

This pose work is intentionally deferred until more aimed-weapon textures exist. The separate `aimed` model category is already in place so those abilities can be classified without later restructuring the display generator.

## Planned Follow-Up

- classify each new custom icon as it is approved
- decide whether the five creative test items remain permanent development tools
- implement and test aimed arm-pose metadata after the relevant weapon textures exist
- consider optional per-ability transform overrides only if shared profiles prove insufficient; none are currently required

## Golden Held-Item Visual Prototypes

Amazonian Beatdown and Birdarang remain as inert hand-only tests for the
selected balanced golden halo. `HandOnlyModelWrapper` selects the experimental
model only for first-person and third-person hand contexts. The hotbar,
inventory, dropped-item, and item-frame views continue using the original
ability texture.

The balanced halo extends up to five pixels from the source alpha silhouette
with a translucent gold falloff. Every original opaque pixel is preserved.
The other six display-test items retain only their normal display-category
models and no longer participate in golden-visual testing.

The derived comparison textures are reproducible through
`tools/generate-golden-display-test-textures.ps1`. They are intentionally
separate from the real `ability_<id>.png` files and do not affect battle items.

The prototype tests a static texture treatment only. Applying the selected
treatment to real golden battle abilities exclusively while held would reuse
the same hand-context model switch, then resolve the golden state from the
ability stack. A subtle animated sheen would require a custom render pass or
shader and is therefore materially more complex than any option in this set.

The deferred production rollout is specified in
[`../roadmap/golden-ability-held-halo-plan.md`](../roadmap/golden-ability-held-halo-plan.md).
