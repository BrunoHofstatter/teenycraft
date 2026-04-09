# Battle Engine

## Purpose
Define the runtime combat model, the main execution flow, and where battle behavior should be changed.

## Current Status
The repo already contains a functioning battle foundation centered on:
- `IBattleState` capability attached to living entities
- `BattleFigure` as the active combat snapshot
- `AbilityExecutor` as the main execution path
- effect and trait registries
- custom packet sync for client battle data

## Player-Facing Behavior
- Battles are real-time, not turn-based.
- Active figures spend mana, respect cooldowns, and hold virtual HP separate from vanilla health.
- Buffs and debuffs live in battle state rather than directly mutating the figure item.
- Team defeat occurs when all three virtual figures are defeated.

## Source Of Truth
- [`src/main/java/bruhof/teenycraft/battle/AbilityExecutor.java`](../../src/main/java/bruhof/teenycraft/battle/AbilityExecutor.java)
- [`src/main/java/bruhof/teenycraft/battle/BattleFigure.java`](../../src/main/java/bruhof/teenycraft/battle/BattleFigure.java)
- [`src/main/java/bruhof/teenycraft/battle/damage/DamagePipeline.java`](../../src/main/java/bruhof/teenycraft/battle/damage/DamagePipeline.java)
- [`src/main/java/bruhof/teenycraft/capability/IBattleState.java`](../../src/main/java/bruhof/teenycraft/capability/IBattleState.java)
- [`src/main/java/bruhof/teenycraft/networking/PacketSyncBattleData.java`](../../src/main/java/bruhof/teenycraft/networking/PacketSyncBattleData.java)
- [`src/main/java/bruhof/teenycraft/TeenyBalance.java`](../../src/main/java/bruhof/teenycraft/TeenyBalance.java)

## Design Notes
- Player and NPC battle participants should stay as symmetric as possible.
- Battle runtime state should stay hot and volatile, while figure items remain the persistent long-term storage.
- Traits and effects should hook into existing registries before new one-off battle branches are added.
- Numeric tuning belongs in `TeenyBalance.java`.

## Balance Hooks
- mana generation and consumption
- cooldown timings
- damage multipliers
- crit and dodge rules
- swap reset timings
- effect duration and strength

## Open Questions
- How much of PvP should reuse the exact same runtime flow versus special-case rules
- Whether more battle phases or combat states are needed beyond the current executor/state setup

## Planned Additions
- richer battle accessories and chips integration
- more complete AI behavior parity
- more documented battle event hooks for future mechanics
