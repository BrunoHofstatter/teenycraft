# Effects

## Purpose
Document battle effects, their lifecycle, and how effect application should be extended.

## Current Status
The mod already has a centralized battle effect model with registries and support classes for periodic effects and application behavior.

## Player-Facing Behavior
- Effects can buff, debuff, damage over time, heal over time, lock actions, or modify combat flow.
- Effects live in the battle runtime state and are usually cleared on figure defeat/swap reset conditions.

## Source Of Truth
- [`src/main/java/bruhof/teenycraft/battle/effect/EffectApplierRegistry.java`](../../src/main/java/bruhof/teenycraft/battle/effect/EffectApplierRegistry.java)
- [`src/main/java/bruhof/teenycraft/battle/effect/EffectRegistry.java`](../../src/main/java/bruhof/teenycraft/battle/effect/EffectRegistry.java)
- [`src/main/java/bruhof/teenycraft/battle/effect/PeriodicBattleEffect.java`](../../src/main/java/bruhof/teenycraft/battle/effect/PeriodicBattleEffect.java)
- [`src/main/java/bruhof/teenycraft/battle/effect/EffectInstance.java`](../../src/main/java/bruhof/teenycraft/battle/effect/EffectInstance.java)
- [`src/main/resources/data/teenycraft/abilities/pure_effects`](../../src/main/resources/data/teenycraft/abilities/pure_effects)

## Design Notes
- New effect logic should prefer the existing registry model.
- Shared effect concepts should become reusable registry entries rather than custom branches per ability.
- Lockout-style effects must stay explicit about what they block: movement, casting, swapping, buffs, or incoming damage handling.

## Open Questions
- whether long-term non-battle effects should ever share this system
- whether effect categories need a stronger schema or enum-driven validation layer

## Planned Additions
- status glossary for all named combat effects
- clearer stacking and refresh rules
