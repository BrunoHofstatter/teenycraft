# Abilities

## Purpose
Capture how abilities are categorized, loaded, executed, and extended.

## Current Status
Abilities are already data-driven and split across multiple categories in resource data. Execution is centered on `AbilityExecutor` with support for traits and effects, and the loader now also supports optional player-facing figure-screen text fields that the figure screen shows through hover tooltips.

## Player-Facing Behavior
- Figures use abilities in real time.
- Abilities can deal damage, apply self effects, apply opponent effects, or add special execution behavior.
- Golden/mastery variants can modify an ability's performance or extra behavior.

## Source Of Truth
- [`src/main/java/bruhof/teenycraft/battle/AbilityExecutor.java`](../../src/main/java/bruhof/teenycraft/battle/AbilityExecutor.java)
- [`src/main/java/bruhof/teenycraft/util/AbilityLoader.java`](../../src/main/java/bruhof/teenycraft/util/AbilityLoader.java)
- [`src/main/resources/data/teenycraft/abilities`](../../src/main/resources/data/teenycraft/abilities)
- [`src/main/java/bruhof/teenycraft/TeenyBalance.java`](../../src/main/java/bruhof/teenycraft/TeenyBalance.java)

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

## Open Questions
- whether ability schema should be formalized in a dedicated reference doc
- how much of golden ability behavior should stay data-driven versus code-driven

## Planned Additions
- schema examples for common ability patterns
- clearer per-category validation expectations
- links to icon/model naming conventions as the content library grows
