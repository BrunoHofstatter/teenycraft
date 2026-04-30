# Battle AI

## Purpose
Document the implemented opponent decision-making model for Teeny battles, the limits of the current v1 behavior, and where AI tuning now lives.

## Current Status
- Partially implemented.
- `EntityTeenyDummy` now runs a first-pass battle AI goal during arena battles instead of acting as a pure idle target.
- The v1 AI uses the shared battle runtime for movement, action execution, and swapping rather than a separate cheat system.
- NPC team JSON can now optionally include an AI profile with direct timing, swap, and reasoning controls, with `difficulty` acting only as an optional preset source for omitted fields.
- Arena pickups, wall tactics, and scripted encounter logic are not part of v1.

## Player-Facing Behavior
- The current opponent behaves like a battle participant, not like a vanilla mob.
- The AI keeps moving, tries to face the player before ranged casts, and chooses between melee pressure, ranged pressure, stalling, self-maintenance, and swapping.
- Swapping is a real tactical behavior in v1 and is influenced by class advantage, current HP, and whether the current figure can pressure well.
- Ability choice is still heuristic rather than fully authored scripting, but pure effect abilities now also use effect-specific family rules for healing, setup buffs, pressure-reactive defense, cleanse, mines, and other support actions.

## Source Of Truth
- [`src/main/java/bruhof/teenycraft/battle/ai/BattleAiGoal.java`](../../src/main/java/bruhof/teenycraft/battle/ai/BattleAiGoal.java)
- [`src/main/java/bruhof/teenycraft/battle/ai/BattleAiProfile.java`](../../src/main/java/bruhof/teenycraft/battle/ai/BattleAiProfile.java)
- [`src/main/java/bruhof/teenycraft/entity/custom/EntityTeenyDummy.java`](../../src/main/java/bruhof/teenycraft/entity/custom/EntityTeenyDummy.java)
- [`src/main/java/bruhof/teenycraft/command/CommandTeeny.java`](../../src/main/java/bruhof/teenycraft/command/CommandTeeny.java)
- [`src/main/java/bruhof/teenycraft/capability/BattleState.java`](../../src/main/java/bruhof/teenycraft/capability/BattleState.java)
- [`src/main/java/bruhof/teenycraft/util/NPCTeamLoader.java`](../../src/main/java/bruhof/teenycraft/util/NPCTeamLoader.java)
- [`src/main/java/bruhof/teenycraft/world/arena/ArenaBattleManager.java`](../../src/main/java/bruhof/teenycraft/world/arena/ArenaBattleManager.java)
- [`docs/systems/battle-engine.md`](battle-engine.md)
- [`../world/npc-ai-controls.md`](../world/npc-ai-controls.md)

## Implemented Behavior
- `EntityTeenyDummy` now has a dedicated `BattleAiGoal` above its normal idle look goals.
- The AI evaluates one short-lived intent at a time and then lets movement support that intent.
- The AI now treats close-range contact and recent damage as generic pressure, which makes it reevaluate passive plans faster and prefer immediate combat actions more often.
- Action choice is generic by ability family instead of hardcoded per ability:
  - melee damage
  - ranged damage
  - heal
  - cleanse
  - buff
  - control or debuff
  - utility
- Pure effect abilities now also layer more specific rules on top of that generic role scoring:
  - self-heal and group-heal scale more sharply with real HP loss
  - setup buffs such as `power_up`, `dance`, `luck_up`, pets, and radios prefer low-pressure windows
  - defensive self-buffs such as `cuteness`, `shield`, `defense_up`, and `dodge_smoke` prefer a mid-range threat band instead of full pressure or full disengage
  - `flight` and `reflect` now act as pressure reactions
  - `cleanse` now scores from self debuff count only
  - pure opponent effect moves such as `poison`, `curse`, `waffle`, `defense_down`, `freeze`, and `remote_mine` now have their own situational gates
- Valid actions are scored, then chosen with weighted randomness from the best-scoring window so the AI is not perfectly deterministic.
- The scorer now also applies an anti-repetition failsafe so the same slot is not spammed forever when another valid option exists.
- If a usable melee action is already in range, the AI now strongly prefers taking that melee hit instead of idling on a passive or ranged plan.
- Movement is role-aware:
  - melee figures close distance and stay threatening
  - ranged figures try to hold a usable band
  - support or maintenance actions try to create space first
- Buff, debuff, and control actions now lose score when they would mostly reapply effects that are already active on the same target.
- Swap decisions are checked before normal action selection and currently focus on:
  - class advantage
  - current HP preservation
  - whether another bench figure fits the matchup better
- Opponent-side battle runtime now supports AI-driven swaps, speed changes, delayed projectile resolution, and dummy presentation updates without requiring a player owner.
- Dummy accessories are still out of scope for v1.

## Design Notes
- AI should drive the same combat runtime systems players use wherever possible.
- Difficulty should tune competence, timing, and reasoning depth, while NPC data should tune personality.
- For the exact NPC AI JSON control surface and authoring guidance, use [npc-ai-controls.md](../world/npc-ai-controls.md).
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
