# Abilities

## Purpose
Capture how abilities are categorized, loaded, executed, and extended.

## Current Status
Abilities are already data-driven and split across multiple categories in resource data. Execution is centered on `AbilityExecutor` with support for traits and effects, and the loader now also supports optional player-facing figure-screen text fields that the figure screen shows through hover tooltips.
Phase 1 of the battle refactor now validates ability references during reload instead of leaving bad ids to fail later at runtime.
Phase 7 now parses `golden_bonus` entries into structured loader records on reload, and validated battle runtime consumes that parsed contract instead of reparsing raw strings at execution time.

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

## Reload Validation
Battle content reload now validates:

- ability effect ids against the explicit gameplay-content input ids exposed by `EffectApplierRegistry`
- ability trait ids against the explicit runtime trait contract exposed by `TraitRegistry`
- `golden_bonus` entries through the same parsed-on-load scope/id/param contract used by runtime

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
- If you want a custom icon, add `assets/teenycraft/models/item/ability_<id>.json` and usually `assets/teenycraft/models/item/ability_<id>_golden.json`.
- Add the matching icon textures under `assets/teenycraft/textures/item/`, typically `ability_<id>.png` and `ability_<id>_golden.png`.
- Add override entries for that `texture_index` to all three wrapper models:
  - `assets/teenycraft/models/item/ability_1.json`
  - `assets/teenycraft/models/item/ability_2.json`
  - `assets/teenycraft/models/item/ability_3.json`
- If the JSON introduces a brand-new effect id or trait id, implement and register it in the runtime registries before reload validation will accept the content.

Important current icon behavior:

- [`src/main/java/bruhof/teenycraft/client/AbilityIconManager.java`](../../src/main/java/bruhof/teenycraft/client/AbilityIconManager.java) is not only a missing-texture fallback. If an ability id is present in `FALLBACKS`, 
`AbilityModelWrapper` will render that vanilla item model instead of your custom ability model.
- So use `FALLBACKS` only when you intentionally want the vanilla-item icon, not in addition to a custom icon pipeline.

## Technical Reference
- For the field-by-field JSON contract, live effect and trait ids, parameter usage, and golden bonus merge rules, use [abilities-json-reference.md](abilities-json-reference.md).

## Open Questions
- how much of golden ability behavior should stay data-driven versus code-driven

## Planned Additions
- schema examples for common ability patterns
- clearer per-category validation expectations
- links to icon/model naming conventions as the content library grows
