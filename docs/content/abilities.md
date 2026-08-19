# Abilities

## Purpose
Capture how abilities are categorized, loaded, executed, and extended.

## Current Status
Abilities are already data-driven and split across multiple categories in resource data. Execution is centered on `AbilityExecutor` with support for traits and effects, and the loader now also supports optional player-facing figure-screen text fields that the figure screen shows through hover tooltips.
Phase 1 of the battle refactor now validates ability references during reload instead of leaving bad ids to fail later at runtime.
Phase 7 now parses `golden_bonus` entries into structured loader records on reload, and validated battle runtime consumes that parsed contract instead of reparsing raw strings at execution time.
Figure-selected color variants are implemented as presentation metadata without duplicating gameplay ability definitions. The initial set covers Whale Drop, Soul Punch, and Construct Beam.
Parameterless `transform` self effects now enter or exit data-defined battle forms according to the executing ability id. Golden transform abilities can use the pre-cast `reduced_mana_cost` trait contract; the current Gorilla transitions pay about 30% of normal mana cost.

## Player-Facing Behavior
- Figures use abilities in real time.
- Abilities can deal damage, apply self effects, apply opponent effects, or add special execution behavior.
- Golden/mastery variants can modify an ability's performance or extra behavior.

## Source Of Truth
- [`src/main/java/bruhof/teenycraft/battle/AbilityExecutor.java`](../../src/main/java/bruhof/teenycraft/battle/AbilityExecutor.java)
- [`src/main/java/bruhof/teenycraft/battle/validation/BattleContentValidation.java`](../../src/main/java/bruhof/teenycraft/battle/validation/BattleContentValidation.java)
- [`src/main/java/bruhof/teenycraft/util/AbilityLoader.java`](../../src/main/java/bruhof/teenycraft/util/AbilityLoader.java)
- [`src/main/resources/data/teenycraft/abilities`](../../src/main/resources/data/teenycraft/abilities)
- [`src/main/java/bruhof/teenycraft/TeenyBalance.java`](../../src/main/java/bruhof/teenycraft/TeenyBalance.java)
- [`abilities-json-reference.md`](abilities-json-reference.md)
- [`ability-icon-workflow.md`](ability-icon-workflow.md)
- [`ability-item-displays.md`](ability-item-displays.md)

## Reload Validation
Battle content reload now validates:

- ability effect ids against the explicit gameplay-content input ids exposed by `EffectApplierRegistry`
- ability trait ids against the explicit runtime trait contract exposed by `TraitRegistry`
- `golden_bonus` entries through the same parsed-on-load scope/id/param contract used by runtime
- figure-form enter/exit abilities, transform-effect shape, counterpart mappings, and effective cost tiers

Current compatibility note:

- legacy `trait:instant_cast` remains a validated no-op compatibility alias so shipped data stays legal without adding new mechanics

## Current Categories
- melee
- ranged
- self effect
- opponent effect
- pure effects and support definitions

## Design Notes
- New abilities should first try to compose existing executor, trait, and effect systems.
- One-off logic is acceptable only when the mechanic truly does not fit the current registries.
- Ability balance numbers should resolve through `TeenyBalance.java` or centralized calculation paths.
- Ability JSON can now optionally expose `description` and `golden_description` for the figure screen tooltip without changing battle execution data.
- `AbilityLoader.AbilityData` now carries parsed `golden_bonus` records for battle runtime and preview helpers, while the raw strings remain only as source-data compatibility.

## Ability Authoring Checklist
When a figure needs a new ability id, update these together:

- Add the ability JSON under [`src/main/resources/data/teenycraft/abilities`](../../src/main/resources/data/teenycraft/abilities) with a unique `id` and a unique `texture_index`.
- Reference that ability id from the figure JSON `abilities` list.
- For a custom icon, add `assets/teenycraft/textures/item/ability_<id>.png` and assign the ability to exactly one category in `assets/teenycraft/ability_display_categories.json`. The Gradle `generateAbilityIconModels` task generates the categorized individual item model and all three slot-wrapper overrides from the current `texture_index`.
- For a figure-specific color treatment of that same gameplay ability, keep the ability JSON unchanged. Add `ability_<id>__variant_<variant>.png` and set the optional figure JSON `ability_icon_variants` entry from the ability id to the lowercase variant id. Variants inherit the base display category and do not receive another `texture_index`.
- For a special visual state, add the state texture and extend the generator and item property contract. Bat Mine currently uses `ability_bat_mine_button.png` with `teenycraft:is_button`.
- If the JSON introduces a brand-new effect id or trait id, implement and register it in the runtime registries before reload validation will accept the content.
- Transform abilities use a parameterless `{ "id": "transform", "params": [] }` self effect. The matching form definition owns the enter/exit relationship rather than repeating a form id in the effect.

Important current icon behavior:

- During model baking, `AbilityIconManager` discovers the available `ability_<id>.png` textures from the active resources. [`src/main/java/bruhof/teenycraft/client/model/AbilityModelWrapper.java`](../../src/main/java/bruhof/teenycraft/client/model/AbilityModelWrapper.java) permits only those exact ids to enter the generated predicate table before consulting `AbilityIconManager.FALLBACKS`. This prevents placeholder abilities from inheriting a nearby custom texture through Minecraft's numeric predicate thresholds while still allowing a fallback entry to remain as a safety net.
- Golden abilities reuse the normal icon. [`ItemAbility.isFoil`](../../src/main/java/bruhof/teenycraft/item/custom/battle/ItemAbility.java) supplies the enchantment glint; there are no separate golden icon models or textures.
- The generator validates that figure-equipped abilities exist and have unique `texture_index` values. Internal effect/support definitions are not treated as renderable ability slots.
- The generator also validates that every color-variant texture is assigned by at least one figure, every figure assignment has a matching texture, the ability belongs to that figure, and a categorized base custom icon exists.

Current integrated custom icons:

- `amazonian_beatdown`
- `around_the_world`
- `arrow_storm`
- `axe_to_grind`
- `bang`
- `bat_mine`, including its active button state
- `batarang_storm`
- `battery_drain`
- `birdarang`
- `black_hole`
- `booster_beatdown`
- `dance`
- `fear`
- `harleys_mallet`
- `hooded_barrage`
- `hooded_void`
- `macho_smooch`
- `mind_control`
- `skeets`

## Technical Reference
- For the field-by-field JSON contract, live effect and trait ids, parameter usage, and golden bonus merge rules, use [abilities-json-reference.md](abilities-json-reference.md).
- For the collaborative icon design, generation, review, and file-output process, use [ability-icon-workflow.md](ability-icon-workflow.md).
- For held-item model categories, shared transforms, validation, special states, and planned aimed arm poses, use [ability-item-displays.md](ability-item-displays.md).

## Open Questions
- how much of golden ability behavior should stay data-driven versus code-driven

## Planned Additions
- schema examples for common ability patterns
- clearer per-category validation expectations
- links to icon/model naming conventions as the content library grows
