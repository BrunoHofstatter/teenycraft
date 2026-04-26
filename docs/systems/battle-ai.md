# Battle AI

## Purpose
Document the implemented opponent decision-making model for Teeny battles, the limits of the current v1 behavior, and where AI tuning now lives.

## Current Status
- Partially implemented.
- `EntityTeenyDummy` now runs a first-pass battle AI goal during arena battles instead of acting as a pure idle target.
- The v1 AI uses the shared battle runtime for movement, action execution, and swapping rather than a separate cheat system.
- NPC team JSON can now optionally include a small AI profile used to tune competence and style.
- Arena pickups, wall tactics, and scripted encounter logic are not part of v1.

## Player-Facing Behavior
- The current opponent behaves like a battle participant, not like a vanilla mob.
- The AI keeps moving, tries to face the player before ranged casts, and chooses between melee pressure, ranged pressure, stalling, self-maintenance, and swapping.
- Swapping is a real tactical behavior in v1 and is influenced by class advantage, current HP, and whether the current figure can pressure well.
- Ability choice is still heuristic rather than authored per-ability scripting, so the AI is functional but not yet highly expressive.

## Source Of Truth
- [`src/main/java/bruhof/teenycraft/battle/ai/BattleAiGoal.java`](../../src/main/java/bruhof/teenycraft/battle/ai/BattleAiGoal.java)
- [`src/main/java/bruhof/teenycraft/battle/ai/BattleAiProfile.java`](../../src/main/java/bruhof/teenycraft/battle/ai/BattleAiProfile.java)
- [`src/main/java/bruhof/teenycraft/entity/custom/EntityTeenyDummy.java`](../../src/main/java/bruhof/teenycraft/entity/custom/EntityTeenyDummy.java)
- [`src/main/java/bruhof/teenycraft/command/CommandTeeny.java`](../../src/main/java/bruhof/teenycraft/command/CommandTeeny.java)
- [`src/main/java/bruhof/teenycraft/capability/BattleState.java`](../../src/main/java/bruhof/teenycraft/capability/BattleState.java)
- [`src/main/java/bruhof/teenycraft/util/NPCTeamLoader.java`](../../src/main/java/bruhof/teenycraft/util/NPCTeamLoader.java)
- [`src/main/java/bruhof/teenycraft/world/arena/ArenaBattleManager.java`](../../src/main/java/bruhof/teenycraft/world/arena/ArenaBattleManager.java)
- [`docs/systems/battle-engine.md`](battle-engine.md)

## Implemented Behavior
- `EntityTeenyDummy` now has a dedicated `BattleAiGoal` above its normal idle look goals.
- The AI evaluates one short-lived intent at a time and then lets movement support that intent.
- Action choice is generic by ability family instead of hardcoded per ability:
  - melee damage
  - ranged damage
  - heal
  - cleanse
  - buff
  - control or debuff
  - utility
- Valid actions are scored, then chosen with weighted randomness from the best-scoring window so the AI is not perfectly deterministic.
- Movement is role-aware:
  - melee figures close distance and stay threatening
  - ranged figures try to hold a usable band
  - support or maintenance actions try to create space first
- Swap decisions are checked before normal action selection and currently focus on:
  - class advantage
  - current HP preservation
  - whether another bench figure fits the matchup better
- Opponent-side battle runtime now supports AI-driven swaps, speed changes, delayed projectile resolution, and dummy presentation updates without requiring a player owner.
- Dummy accessories are still out of scope for v1.

## Design Notes
- AI should drive the same combat runtime systems players use wherever possible.
- Difficulty should tune competence, timing, and reasoning depth, while NPC data should tune personality.
- Current NPC AI profile fields are:
  - `difficulty`
  - `aggression`
  - `swap_bias`
  - `preferred_range`
  - `mana_discipline`
  - `risk_tolerance`
- Because battle effects are participant-owned, swapping is not treated as a way to clear poison, buffs, or debuffs.

## Planned Additions
- Arena-aware behavior for pickups, walls, launch positioning, and cover.
- Stronger counter-awareness against enemy cleanse, spacing, and other answers on higher difficulties.
- Better support timing and more nuanced retreat or stall decisions.
- Optional use of accessories for AI-controlled battlers if enemy accessory runtime is implemented later.
- Broader authored personality data once the first-pass profile fields prove useful in real battles.

## Open Questions
- How deterministic versus expressive the AI should feel.
- How much extra ability-specific behavior is worth adding before the generic family-scoring model becomes too blunt.
- Whether lower difficulties should only weaken tuning or also remove specific reasoning layers entirely.
