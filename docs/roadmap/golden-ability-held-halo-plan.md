# Golden Ability Held-Halo Rollout Plan

## Status

Deferred. Do not apply this presentation to real battle abilities until every
figure-equipped ability has an approved custom texture.

At the time this plan was written, the repository contains 124 distinct
figure-equipped ability ids and 51 matching custom textures. The readiness
count must use figure-equipped ids rather than all ability JSON records,
because internal effect and support definitions are not rendered as items.

## Approved Direction

- Golden abilities use the balanced static gold halo while held.
- The halo appears only in first-person and third-person hand contexts.
- The normal icon remains unchanged in the hotbar, inventory, menus, dropped
  item rendering, and item frames.
- The halo is generated from the texture's alpha silhouette. It extends up to
  five pixels, uses a translucent gold falloff, and does not change any
  original opaque pixel.
- The vanilla enchantment glint is removed from golden abilities when this
  rollout ships.
- GUI golden identification remains a separate presentation layer, such as a
  small gold slot treatment. It must not be baked into held-item textures.

## Readiness Gate

Do not begin the production conversion until all of these are true:

1. Every distinct ability id referenced by figure JSON has an approved
   `ability_<id>.png`.
2. Every finished texture is assigned to exactly one category in
   `ability_display_categories.json`.
3. `generateAbilityIconModels` succeeds without a vanilla fallback for any
   figure-equipped ability.
4. Every source texture has enough transparent margin for a five-pixel halo,
   or the team has approved an explicit exception policy for edge clipping.
5. Special render states, currently Bat Mine's remote button, have approved
   halo behavior.

The build should expose a coverage report and fail the production-halo task if
the equipped-id texture count is not complete. Internal abilities must not
block the gate.

## Implementation Phases

### 1. Freeze And Validate The Visual Contract

- Keep Amazonian Beatdown and Birdarang as the reference test pair.
- Confirm the current balanced radius, opacity, color, and falloff in both
  first-person hands and both third-person hands.
- Add a transparent-margin validation report. A texture touching the halo
  safety margin should be listed explicitly rather than silently producing a
  clipped aura.
- Decide whether special states inherit the base ability's halo settings or
  generate their own alpha-derived halo. The recommended default is to derive
  the halo from each state's actual texture.

### 2. Generate Halos During The Build

Extend `generateAbilityIconModels` or add a task it depends on:

- read each approved `ability_<id>.png`
- generate `ability_<id>_golden_halo.png` under
  `build/generated/ability-icon-resources`, not beside the source artwork
- preserve all opaque source pixels exactly
- add halo pixels only where the source alpha is zero
- generate a held golden model using the same display category as the normal
  ability model
- generate equivalent assets for supported state textures
- delete and recreate the generated output directory on every run so removed
  abilities cannot leave stale assets

Generated halo PNGs should not be committed individually. The source icon,
generator, and fixed visual constants should be the reproducible source of
truth.

### 3. Select The Halo Only For Golden Held Stacks

Extend the existing model-bake and `AbilityModelWrapper` path:

- resolve the normal per-ability model as it does today
- inspect `ItemAbility.TAG_GOLDEN`
- for a golden stack, return a cached hand-context wrapper containing the
  normal and generated halo models
- use the halo model only for first-person and third-person left/right hand
  contexts
- use the normal model for `GUI`, `GROUND`, `FIXED`, and other contexts
- cache model pairs on resource reload; do not allocate wrappers every frame
- preserve the active Bat Mine button model and other future state overrides

A plain item-property predicate is not sufficient because it would select the
halo in GUI rendering as well as hand rendering.

### 4. Remove Vanilla Glint And Add GUI Identification

- Change `ItemAbility.isFoil` so golden ability stacks no longer request the
  vanilla enchantment glint.
- Add the separately approved golden slot treatment to the vanilla hotbar and
  Teeny Craft HUD/screens.
- Verify that removing glint does not affect golden gameplay data, tooltips,
  names, ability bonuses, or synchronization.

### 5. Automated Validation

Add generation checks for:

- exact equality of every original nontransparent pixel
- halo output dimensions and alpha format
- halo pixels appearing only outside the source silhouette
- one normal and one halo model for every finished renderable ability
- identical display-category parents between each normal/halo model pair
- complete coverage of all figure-equipped ability ids
- complete special-state coverage
- no generated golden models for internal-only ability records

### 6. Visual QA

Create generated contact sheets grouped by `tool`, `action`, `fist`,
`throwable`, and `aimed`, then review every ability at reduced size.

In-game verification must cover:

- first-person right and left hands
- third-person right and left hands
- attack/use animation
- normal and golden versions of the same ability
- hotbar, inventory, Figure Screen, Silkie Station, and battle HUD
- dark and bright environments
- Bat Mine's normal and remote-button states
- resource reload and figure swapping during battle

Pay special attention to thin silhouettes, icons near texture boundaries, and
already-yellow artwork. Exceptions should adjust source padding before adding
per-ability halo strengths.

### 7. Retire The Harness

After the production path passes full-library QA:

- remove the two balanced-halo display-test models and derived textures
- remove `GoldenDisplayTestClientEvents` if no other visual experiments use it
- keep the generic hand-context wrapper only if the production renderer reuses
  it
- update `docs/content/ability-item-displays.md` from prototype status to
  implemented behavior

## Expected Work

- One medium-sized client rendering change to combine golden NBT, ability model
  resolution, special states, and hand-only context switching.
- One medium-sized Gradle/image-generation change with strict validation.
- A small GUI decoration change after its exact appearance is approved.
- Minimal routine work per ability because halo assets are generated
  automatically.
- The largest cost is full-library visual QA and correcting source textures
  that lack five pixels of transparent margin.

The design intentionally avoids authoring and maintaining a separate handmade
golden texture for every ability.
