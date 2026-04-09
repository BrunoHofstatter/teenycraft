# Battle AI

## Purpose
Document the intended opponent decision-making model for Teeny battles and the current placeholder state in the codebase.

## Current Status
- Mostly planned.
- The repo does not yet contain a full opponent battle AI that mirrors player decision-making.
- Current debug battles use `EntityTeenyDummy` as a placeholder target, with battle state attached but no real combat brain.
- NPC team loading exists, which means authored opponent rosters are already supported even though authored AI behavior is not.

## Player-Facing Behavior
- The intended opponent should behave like a battle participant, not like a normal Minecraft mob.
- AI should eventually make decisions around spacing, mana usage, cooldown timing, and figure swapping.
- Right now, this layer is not implemented as a mature gameplay system.

## Source Of Truth
- [`src/main/java/bruhof/teenycraft/entity/custom/EntityTeenyDummy.java`](../../src/main/java/bruhof/teenycraft/entity/custom/EntityTeenyDummy.java)
- [`src/main/java/bruhof/teenycraft/command/CommandTeeny.java`](../../src/main/java/bruhof/teenycraft/command/CommandTeeny.java)
- [`src/main/java/bruhof/teenycraft/capability/BattleState.java`](../../src/main/java/bruhof/teenycraft/capability/BattleState.java)
- [`src/main/java/bruhof/teenycraft/util/NPCTeamLoader.java`](../../src/main/java/bruhof/teenycraft/util/NPCTeamLoader.java)
- [`docs/systems/battle-engine.md`](battle-engine.md)

## Implemented Behavior
- `EntityTeenyDummy` has only minimal idle/look goals.
- The dummy can hold battle state and act as the opponent-side runtime container during debug battles.
- Current battle code already supports many shared combat mechanics on runtime state, which gives future AI a player-like system to drive.
- Decision logic for movement, target selection, ability choice, mana planning, and tactical swapping is not implemented yet.

## Design Notes
- AI should drive the same combat runtime systems players use wherever possible.
- Opponents should make decisions based on figure role, mana thresholds, cooldowns, and current battle context.
- Swap behavior is part of battle identity and should eventually be handled as a real tactical choice.
- AI design should stay compatible with authored NPC teams and future challenger encounter rules.

## Planned Additions
- Movement logic for closing distance, kiting, or maintaining preferred range.
- Ability selection logic based on mana, cooldowns, and current tactical state.
- Swap logic based on low health, matchup state, or authored encounter rules.
- Support for different AI personalities or encounter archetypes.
- Better parity between player-side battle options and opponent-side decision-making.

## Open Questions
- How deterministic versus expressive the AI should feel.
- Whether some bosses or story fights should use custom scripts instead of only general AI heuristics.
- How much of the AI should be authored in data versus hardcoded decision layers.
