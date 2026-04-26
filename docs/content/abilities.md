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

## Open Questions
- whether ability schema should be formalized in a dedicated reference doc
- how much of golden ability behavior should stay data-driven versus code-driven

## Planned Additions
- schema examples for common ability patterns
- clearer per-category validation expectations
- links to icon/model naming conventions as the content library grows
