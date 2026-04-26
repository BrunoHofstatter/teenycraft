# Battle Refactor Plan

## Purpose
Turn the battle refactor audit into a phased implementation sequence with one bounded thread at a time.
This is still a refactor plan, not a redesign plan.

## Working Rules
- No balance tuning during the refactor unless a mechanic is already objectively broken.
- Do not rewrite `BattleState`, `AbilityExecutor`, effects, accessories, and battle pairing in one thread.
- Each phase must end with the mod compiling, docs updated, and battle still manually playable.
- Prefer additive safety work before structural replacement work.

## Current Reality Check
- Battle bootstrap is already more centralized than the audit suggests through `ArenaBattleManager.startBattle`.
- The defeat cleanup bug called out in the audit has partially moved since `BattleState.tick` now consumes `winnerPlayer` directly instead of checking `battleWon`. Phase 2 re-verifies that live path before changing it further, but Forge GameTest mock `ServerPlayer` instances still do not expose a full network channel for directly exercising the packet-and-teleport exit path.
- Phase 1 now has live Forge GameTests for the battle harness scenarios plus JUnit validation coverage for reload-time content checks.
- Phase 3 now stores authoritative opponent identity on `BattleState`, initializes both sides through one paired bootstrap flow, and removes the main proximity-based opponent lookups from correctness-critical battle runtime paths.
- Phase 4 now moves figure-local combat state onto `BattleFigure`, keeps participant resources on `BattleState`, and fixes duplicate bench targeting with authoritative tagged team indices without widening into the Phase 5 mutation refactor.
- Phase 5 now replaces the small Phase 2 HP seam with a `BattleState` combat-mutation flow that owns correctness-critical battle HP deltas, faint or kill hooks, and reset or defeat handoff without widening into the later executor split.
- Phase 6 now splits `AbilityExecutor` into smaller execution helpers for validation, targeting, mutation invocation, and follow-up ordering while keeping the existing entry points, pairing, ownership split, and Phase 5 mutation authority intact.
- Phase 7 now completes the refactor by parsing `golden_bonus` on load, tightening gameplay-content effect/trait contracts around the real runtime registries, and removing gameplay-critical generic fallback behavior from validated battle-content paths.

## Phase Checklist
- [x] Phase 1: Safety harness
- [x] Phase 2: Correctness fixes
- [x] Phase 3: Explicit battle pairing
- [x] Phase 4: State ownership cleanup
- [x] Phase 5: Central combat mutation
- [x] Phase 6: Executor split
- [x] Phase 7: Content contract cleanup

## Phase 1
### Goal
Freeze current battle behavior behind characterization tests and reload-time validation before architecture changes begin.

### Non-Goals
- No session or opponent-pairing work yet.
- No state-ownership moves between `BattleState` and `BattleFigure`.
- No cooldown redesign.
- No balance changes.
- No cleanup-only refactors unless they directly enable tests or validation.

### Files This Phase May Touch
- `build.gradle`
- `src/test/java/**`
- `src/test/resources/**`
- `src/main/java/bruhof/teenycraft/util/AbilityLoader.java`
- `src/main/java/bruhof/teenycraft/util/FigureLoader.java`
- `src/main/java/bruhof/teenycraft/util/NPCTeamLoader.java`
- `src/main/java/bruhof/teenycraft/battle/effect/EffectApplierRegistry.java`
- `src/main/java/bruhof/teenycraft/battle/effect/EffectRegistry.java`
- `src/main/java/bruhof/teenycraft/battle/trait/TraitRegistry.java`
- `src/main/java/bruhof/teenycraft/battle/AbilityExecutor.java`
- `docs/systems/battle-engine.md`
- `docs/content/abilities.md`
- `docs/content/effects.md`

### Safety Harness Deliverables
1. Add battle characterization tests.
2. Add reload-time content validation.
3. Leave current mechanics intentionally unchanged unless a test cannot observe them otherwise.

### Characterization Coverage
Prefer Forge GameTests for live battle flow coverage because the current engine depends on entities, capabilities, inventory replacement, and tick events.

Minimum scenarios:
- battle start
  Verify `ArenaBattleManager.startBattle` or the thinnest equivalent test seam initializes both participants, sets battling state, and rebuilds player controls.
- swap
  Verify `BattleState.swapFigure` changes active figure, applies swap cooldown, and clears flight on swap.
- faint and round reset
  Verify `BattleState.checkFaint` swaps to the next alive figure, clears current effects, and applies `reset_lock`.
- defeat cleanup
  Verify an all-fainted team enters cleanup and that arena battle shutdown still completes.
- poison tick kill
  Verify poison damage can drive HP to zero and that the current faint path still executes.
- charge-up
  Verify mana is committed up front, slot lock starts, and `AbilityExecutor.finishCharge` resolves the pending cast.
- blue channel
  Verify channel start, repeated tick resolution, and cleanup at the end of the channel.
- accessory activation
  Verify battery threshold gating and active/inactive transitions.
- one remote-mine path
  Verify placement of a slot-specific mine and one detonation path.
- one golden bonus path
  Pick a currently working golden bonus path such as `trait:undodgeable` or a self/opponent effect bonus. Do not use `trait:instant_cast` as the characterization target.

### Validation Scope
Validation in this phase should fail on bad content references during reload with resource-path-specific errors.

Validate at minimum:
- figure ability ids
- NPC team figure ids
- NPC team golden ability ids
- ability effect ids
- ability trait ids
- golden bonus contracts

Validation must use the real runtime contracts:
- effect references must be checked against supported ids from `EffectApplierRegistry` and any directly stored effect ids from `EffectRegistry`
- trait references must be checked against supported runtime trait hooks, not only the current `TraitRegistry` map if there are ad hoc paths elsewhere
- golden bonus parsing must validate scope and parameter shape, not just the referenced id

### Known Validation Blockers
The current data already contains contract drift that Phase 1 needs to reconcile explicitly:
- `trait:instant_cast` appears in multiple golden bonuses but has no runtime implementation in `TraitRegistry`
- `trait:tofu_chance` appears in golden bonuses and is handled ad hoc in `AbilityExecutor.rollTofu`, not through `TraitRegistry`
- helper ids such as `remote_mine`, `disable`, `waffle_chance`, `pets`, `self_damage`, and `flight` are valid ability-effect inputs through `EffectApplierRegistry` even when they are not one-to-one `EffectRegistry` entries

Phase 1 should document and validate against the current contract as it really exists.
It should not silently "fix" these mismatches by inventing new mechanics.

### Recommended Execution Order
1. Add small validation helpers and expose supported ids from the existing registries.
2. Add strict reload validation with precise errors.
3. Add GameTests for the minimum characterization scenarios.
4. Update the battle, abilities, and effects docs with the new validation/testing truth.
5. Run `./gradlew.bat build`.
6. Manually smoke-test one real battle after the test pass.

### Exit Criteria
- Phase 1 tests exist and cover the minimum scenario list.
- Reload validation reports bad references clearly and blocks broken content from loading.
- `./gradlew.bat build` passes if the environment allows it.
- No intentional gameplay rebalance shipped as part of this phase.
- Docs describe the current safety harness and any known intentionally unchanged engine weaknesses.

### Implemented Result
- Forge GameTests now cover battle start, swap, faint reset, defeat cleanup, poison tick kill, charge-up, blue channel, accessory activation, remote mine, and one golden bonus path.
- Reload-time validation now checks figure ability ids, NPC team figure ids, NPC team golden ids, ability effect ids, trait ids, and golden bonus contracts.
- The safety-harness phase initially kept legacy `trait:instant_cast` golden bonuses warning-only and behavior-neutral; Phase 7 later resolves that drift by making the id an explicit validated compatibility alias without adding new mechanics.

## Phase 2
### Goal
Fix obvious battle correctness bugs without broad architecture refactors.

### Non-Goals
- No explicit battle pairing work yet.
- No state-ownership move from `BattleState` onto per-figure runtime state yet.
- No cooldown redesign.
- No full combat-mutation centralization pass yet.
- No new mechanics or balance tuning unless a mechanic is objectively broken.

### Files This Phase May Touch
- `src/main/java/bruhof/teenycraft/capability/BattleState.java`
- `src/main/java/bruhof/teenycraft/capability/IBattleState.java`
- `src/main/java/bruhof/teenycraft/battle/AbilityExecutor.java`
- `src/main/java/bruhof/teenycraft/battle/effect/EffectRegistry.java`
- `src/main/java/bruhof/teenycraft/battle/effect/EffectApplierRegistry.java`
- `src/main/java/bruhof/teenycraft/gametest/BattleGameTests.java`
- `src/test/java/**`
- `docs/systems/battle-engine.md`
- `docs/roadmap/battle-refactor-plan.md`

### Exit Criteria
- Poison or a similar HP-to-zero path resolves faint and defeat consistently.
- The live defeat cleanup path is re-verified in runtime code, with supported GameTests still covering the battle-state timer cleanup path.
- Characterization tests now assert the corrected behavior instead of the old broken behavior.
- The mod still compiles and battle remains manually playable.
- No broad pairing, state-ownership, or executor refactor leaks into this phase.

### Implemented Result
- `BattleState` now exposes a small HP-delta seam for effect-driven battle HP changes. If that path drops the active figure to 0 HP, it routes back through normal faint and defeat handling.
- Poison, self-shock, and the other direct effect/chip HP edits touched in this phase now use that `BattleState` seam instead of mutating `BattleFigure` HP in isolation.
- The poison characterization GameTest now asserts defeat cleanup instead of documenting the old stuck-at-0-HP behavior.
- Forge GameTests now also cover an instant self-shock kill path alongside the corrected poison kill path.
- The `ArenaBattleManager.finishBattleForPlayer` defeat cleanup flow was re-verified in runtime code, but a direct GameTest for that packet-and-teleport path remains blocked because mock `ServerPlayer` instances do not have a real Netty channel.
- The broader end-state cleanup redesign is still intentionally unchanged. `battleWon` remains legacy state for now because Phase 2 does not widen into the planned structural refactor.

## Phase 3
### Goal
Introduce explicit authoritative opponent pairing for live battle runtime without widening into the later state-ownership or combat-mutation refactors.

### Non-Goals
- No move of effects, cooldowns, slot progress, or other figure-local state off `BattleState` yet.
- No full HP-mutation centralization beyond the small Phase 2 seam that already exists.
- No cooldown redesign.
- No new multi-battle session architecture beyond the local pairing identity needed by current runtime.
- No balance or content-contract changes.

### Files This Phase May Touch
- `src/main/java/bruhof/teenycraft/world/arena/ArenaBattleManager.java`
- `src/main/java/bruhof/teenycraft/capability/BattleState.java`
- `src/main/java/bruhof/teenycraft/capability/IBattleState.java`
- `src/main/java/bruhof/teenycraft/battle/AbilityExecutor.java`
- `src/main/java/bruhof/teenycraft/event/ModEvents.java`
- `src/main/java/bruhof/teenycraft/accessory/AccessoryExecutor.java`
- `src/main/java/bruhof/teenycraft/chip/ChipExecutor.java`
- `src/main/java/bruhof/teenycraft/gametest/BattleGameTests.java`
- `docs/systems/battle-engine.md`
- `docs/roadmap/battle-refactor-plan.md`

### Exit Criteria
- Active battles store an authoritative opponent identity.
- Battle bootstrap initializes both sides with that pairing before first-appearance hooks or HUD sync rely on opponent state.
- Correctness-critical runtime paths stop using nearest-battler scans for opponent authority.
- Tests prove pairing stays correct even when another battle-capable entity is nearby.
- The mod still compiles and battle remains manually playable.
- No Phase 4 or Phase 5 structural cleanup leaks into this phase.

### Implemented Result
- `BattleState` now stores the paired opponent entity id and exposes paired-opponent helpers through `IBattleState`.
- `ArenaBattleManager.startBattle` now initializes the player and dummy through one paired bootstrap flow instead of giving the opponent its own ad hoc setup path.
- `AbilityExecutor`, `AccessoryExecutor`, `BattleState.checkFaint`, first-appearance chip hooks, and `ModEvents` HUD sync now resolve opponent authority from the paired battle state instead of scanning nearby battle-capable entities.
- `BattleState.hasActiveMine` now keys mine ownership off the owning battler entity, not only the player field, so paired-opponent HUD mine state remains symmetric.
- Forge GameTests now assert paired-opponent correctness in the presence of a nearby distractor for faint reset, accessory opponent effects, and remote-mine placement/detonation.
- A non-critical proximity lookup still remains in the debug-only `CommandTeeny` helper paths. That exact spot is intentionally unchanged in Phase 3 because it is not live battle-runtime authority and cleaning it up is not required for the pairing fix.

## Phase 4
### Goal
Separate participant-owned battle state from figure-owned combat state without widening into the later central combat-mutation refactor.

### Non-Goals
- No Phase 5 central damage and healing mutation redesign yet.
- No cooldown mechanic redesign or balance tuning.
- No new broad battle-session architecture beyond the Phase 3 pairing already in place.
- No content JSON churn unless ownership cleanup strictly requires it.

### Files This Phase May Touch
- `src/main/java/bruhof/teenycraft/battle/BattleFigure.java`
- `src/main/java/bruhof/teenycraft/capability/BattleState.java`
- `src/main/java/bruhof/teenycraft/capability/IBattleState.java`
- `src/main/java/bruhof/teenycraft/battle/AbilityExecutor.java`
- `src/main/java/bruhof/teenycraft/event/ModEvents.java`
- `src/main/java/bruhof/teenycraft/gametest/BattleGameTests.java`
- `docs/systems/battle-engine.md`
- `docs/roadmap/battle-refactor-plan.md`

### Exit Criteria
- Figure-specific temporary combat state no longer lives as one shared participant bucket.
- Swaps no longer move buffs, debuffs, slot progress, or cast state onto the wrong figure.
- Duplicate figures on the same team can be swapped authoritatively.
- Phase 2 HP-to-zero correctness and Phase 3 pairing remain intact.
- The mod still compiles and battle remains manually playable.

### Implemented Result
- `BattleFigure` now owns active effects, internal cooldowns, per-slot progress, slot lock, and charge or blue-channel runtime for that figure.
- `BattleState` now keeps participant resources and pairing on the participant, while delegating figure-owned combat lookups to the active figure. `reset_lock` remains a narrow participant-level exception because it gates the whole side during round reset.
- `BattleState.tick` now advances each figure's owned effect map and cooldown buckets instead of one shared participant effect bucket.
- Bench figure swap items now carry an authoritative tagged team index, and `ModEvents` uses that path first so duplicate species no longer collapse to first-match swap resolution.
- `AbilityExecutor` remote-mine detonation and HUD mine checks now respect mines that stay on the figure that was mined, even if that figure is no longer the active slot.
- Forge GameTests now cover figure-owned effect and progress ownership across swaps, duplicate bench targeting, and non-leaking charge state across active-figure changes.
- At the end of Phase 4, direct combat mutation paths were still intentionally unchanged and the small Phase 2 HP seam was still in place.

## Phase 5
### Goal
Centralize correctness-critical combat mutation on one authoritative `BattleState` path without widening into the later executor split or content-contract cleanup phases.

### Non-Goals
- No Phase 6 executor decomposition yet.
- No typed effect-contract or `golden_bonus` cleanup yet.
- No broad new battle-session or end-state redesign beyond the current mutation and faint handoff.
- No balance tuning or content JSON churn unless the mutation fix strictly requires it.

### Files This Phase May Touch
- `src/main/java/bruhof/teenycraft/capability/BattleState.java`
- `src/main/java/bruhof/teenycraft/capability/IBattleState.java`
- `src/main/java/bruhof/teenycraft/battle/AbilityExecutor.java`
- `src/main/java/bruhof/teenycraft/battle/effect/EffectRegistry.java`
- `src/main/java/bruhof/teenycraft/battle/effect/EffectApplierRegistry.java`
- `src/main/java/bruhof/teenycraft/chip/ChipExecutor.java`
- `src/main/java/bruhof/teenycraft/gametest/BattleGameTests.java`
- `docs/systems/battle-engine.md`
- `docs/roadmap/battle-refactor-plan.md`

### Exit Criteria
- Correctness-critical battle HP changes route through one authoritative combat-mutation flow.
- Standard hit damage, periodic HP deltas, self-damage, healing, and chip-driven battle HP changes no longer rely on scattered direct battle HP edits.
- Faint, kill, reset, and defeat handling no longer depend on ad hoc caller-local mutation.
- Phase 4 figure ownership and Phase 3 pairing remain intact.
- The mod still compiles and the GameTest battle harness still passes.

### Implemented Result
- `IBattleState` now exposes `CombatMutationSource`, `CombatMutationResult`, and `BattleState`-owned combat mutation helpers. Callers can either resolve immediately or preserve the current hit ordering by mutating first and finalizing faint or kill handling afterward through the same state-owned flow.
- `AbilityExecutor.applyDamageToFigure` now routes standard hit damage through that `BattleState` mutation flow instead of mutating `BattleFigure` HP directly, while keeping the existing pet, reflect, announce, and opponent-effect ordering intact.
- `EffectRegistry` periodic poison, health radio, heal, self-shock, and group-heal paths now use the same mutation helpers, and `ChipExecutor` self-heal hooks do too.
- Forge GameTests now cover standard-hit kill healing, poison kill healing, and self-shock faint battery gain so the important damage and heal cases are verified through the unified mutation path.
- `.\gradlew.bat build` passes.
- `.\gradlew.bat runGameTestServer` passes.
- The delayed-effect original-source limitation that remained during Phase 5 was later fixed as a post-refactor bug fix by persisting source figure identity alongside the caster participant UUID for later combat-source hooks.

## Phase 6
### Goal
Split battle execution into smaller validation, targeting, mutation, and follow-up stages without widening into Phase 7 content-contract cleanup or broader battle-session redesign.

### Non-Goals
- No typed `golden_bonus` redesign yet.
- No effect-contract cleanup or schema redesign yet.
- No HUD, packet, or inventory-control presentation extraction yet.
- No broader battle-session or end-state redesign beyond the existing Phase 3, 4, and 5 runtime shape.
- No balance changes or content JSON churn unless the executor split strictly requires it.

### Files This Phase May Touch
- `src/main/java/bruhof/teenycraft/battle/AbilityExecutor.java`
- `src/main/java/bruhof/teenycraft/battle/executor/**`
- `src/main/java/bruhof/teenycraft/battle/effect/EffectApplierRegistry.java`
- `src/main/java/bruhof/teenycraft/battle/trait/TraitRegistry.java`
- `src/main/java/bruhof/teenycraft/capability/BattleState.java`
- `src/main/java/bruhof/teenycraft/capability/IBattleState.java`
- `src/main/java/bruhof/teenycraft/gametest/BattleGameTests.java`
- `docs/systems/battle-engine.md`
- `docs/roadmap/battle-refactor-plan.md`

### Exit Criteria
- `AbilityExecutor` reads as orchestration instead of one giant mixed logic blob.
- Validation, target resolution, mutation invocation, and follow-up responsibilities are locally separated.
- Phase 5 `BattleState` combat mutation stays the only authoritative HP/faint/kill correctness path.
- Phase 4 ownership and Phase 3 pairing remain intact.
- Existing gameplay ordering stays stable for attack, action, charge, blue-channel, projectile, tofu, mine, reflect, pet, and opponent-effect paths.
- The mod still compiles and the GameTest battle harness still passes.

### Implemented Result
- `AbilityExecutor` is now a thin public facade. Phase 6 moves most runtime logic into `battle.executor.BattleAbilityExecution`, `BattleTargeting`, and `BattleDamageResolver` instead of keeping validation, targeting, damage, and follow-up branches in one class.
- `BattleAbilityExecution` now owns action validation, target resolution, remote-mine scheduling and detonation, projectile scheduling, charge completion, blue-channel interval work, self-effect staging, mana commitment timing, and tofu handling.
- `BattleDamageResolver` now owns outgoing damage package construction plus the standard hit mutation path, including pet follow-up damage, reflect follow-up damage, opponent-effect application, and the final handoff back into the Phase 5 `BattleState` mutation helpers.
- `BattleTargeting` now owns the paired-opponent and ranged-cone targeting helpers used by immediate actions, charge completion, blue-channel ticks, projectile resolution, tofu, and mine lookup.
- Forge GameTests now also cover the delayed-projectile split seam directly: mana is still spent on fire, the projectile still queues first, and `resolveProjectile` still owns resolution-time flight cancellation.
- `.\gradlew.bat build` passes.
- `.\gradlew.bat runGameTestServer` passes.
- Intentional non-goals remain explicit after Phase 6: `golden_bonus` parsing is still string-based, and presentation or packet cleanup is still deferred.

## Phase 7
### Goal
Tighten battle content contracts around `golden_bonus`, effect inputs, and related runtime parsing without widening into presentation cleanup or broader battle-session redesign.

### Non-Goals
- No HUD/presentation cleanup, packet-model cleanup, or inventory-control extraction.
- No broad effect-system redesign or `EffectInstance` payload rewrite.
- No broader battle-session, arena, accessory, chip, or cooldown redesign.
- No balance changes.
- No fix for the delayed-effect original-caster-figure identity limitation within Phase 7 itself; that correctness bug stays separate from the content-contract cleanup scope.

### Files This Phase May Touch
- `src/main/java/bruhof/teenycraft/util/AbilityLoader.java`
- `src/main/java/bruhof/teenycraft/battle/damage/DamagePipeline.java`
- `src/main/java/bruhof/teenycraft/battle/executor/**`
- `src/main/java/bruhof/teenycraft/battle/effect/EffectApplierRegistry.java`
- `src/main/java/bruhof/teenycraft/battle/trait/TraitRegistry.java`
- `src/main/java/bruhof/teenycraft/battle/validation/**`
- `src/main/java/bruhof/teenycraft/gametest/BattleGameTests.java`
- `src/test/java/**`
- `docs/systems/battle-engine.md`
- `docs/content/abilities.md`
- `docs/content/effects.md`
- `docs/roadmap/battle-refactor-plan.md`

### Exit Criteria
- Ability `golden_bonus` contracts are parsed on load and reused by runtime plus validation.
- Ability-effect ids and trait ids use the same explicit runtime contracts in validation and execution.
- Validated battle gameplay no longer relies on generic unknown-effect or unknown-trait fallback behavior.
- Phase 3 pairing, Phase 4 ownership, Phase 5 mutation authority, and the Phase 6 executor split remain intact.
- `.\gradlew.bat build` passes.
- `.\gradlew.bat runGameTestServer` passes.

### Implemented Result
- `AbilityLoader` now parses `golden_bonus` into structured scope/id/param records at reload time, and executor, damage, trait, and preview helpers consume those parsed records instead of reparsing raw strings.
- `EffectApplierRegistry` now exposes an explicit validated gameplay-effect lookup, while generic fallback behavior is separated behind `getOrFallback(...)` for non-validated/debug use.
- `TraitRegistry` now exposes explicit validated trait lookup, golden trait parameter overrides consume parsed loader data, and legacy `trait:instant_cast` is now a validated compatibility alias instead of a warning-only contract exception.
- `BattleContentValidation` now validates ability effect ids against the same explicit applier input contract used by runtime, validates traits against the live trait registry, and parses `golden_bonus` through the same loader helper used at runtime.
- Forge GameTests now cover parsed self and opponent golden bonus application, and JUnit validation coverage now checks parsed golden bonus shape plus rejection of stored-only effect ids as gameplay-content inputs.
- `.\gradlew.bat build` passes.
- `.\gradlew.bat runGameTestServer` passes.

## Closeout
The planned battle refactor phases are complete in this repo.

Intentionally unchanged non-goals:
- the debug-only proximity helper fallback in `CommandTeeny` remains out of scope for the refactor itself
- HUD/presentation cleanup, packet-model cleanup, and broader battle-session redesign are still separate work, not part of this refactor
