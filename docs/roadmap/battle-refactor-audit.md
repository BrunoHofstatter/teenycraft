# Battle Refactor Audit

## Purpose
This document is an implementation brief for refactoring the current battle engine in safe phases.
It is not a design spec for new mechanics.
It is a review of the current code, where the architecture is weak, and what should be improved first.

## Scope
Reviewed areas:

- battle bootstrapping and pairing
- hot battle state creation
- ability execution
- damage and mitigation
- mana, battery, tofu, and accessory runtime
- effects and periodic logic
- traits and chips where they cross into battle behavior
- client sync and battle inventory control surface

Primary code paths reviewed:

- `src/main/java/bruhof/teenycraft/command/CommandTeeny.java`
- `src/main/java/bruhof/teenycraft/capability/BattleState.java`
- `src/main/java/bruhof/teenycraft/capability/IBattleState.java`
- `src/main/java/bruhof/teenycraft/battle/AbilityExecutor.java`
- `src/main/java/bruhof/teenycraft/battle/BattleFigure.java`
- `src/main/java/bruhof/teenycraft/battle/damage/DamagePipeline.java`
- `src/main/java/bruhof/teenycraft/battle/effect/EffectApplierRegistry.java`
- `src/main/java/bruhof/teenycraft/battle/effect/EffectRegistry.java`
- `src/main/java/bruhof/teenycraft/battle/effect/EffectCalculator.java`
- `src/main/java/bruhof/teenycraft/battle/trait/TraitRegistry.java`
- `src/main/java/bruhof/teenycraft/accessory/AccessoryExecutor.java`
- `src/main/java/bruhof/teenycraft/accessory/AccessoryRegistry.java`
- `src/main/java/bruhof/teenycraft/util/AbilityLoader.java`
- `src/main/java/bruhof/teenycraft/util/FigureLoader.java`
- `src/main/java/bruhof/teenycraft/util/NPCTeamLoader.java`
- `src/main/java/bruhof/teenycraft/event/ModEvents.java`

## Current Runtime Shape
The current engine has the right high-level intention:

- cold data lives on figure items and JSON
- hot battle state lives in `BattleState` and `BattleFigure`
- abilities, traits, effects, damage, accessories, and chips all try to plug into the same runtime loop

The main problem is that the runtime model is not strongly separated.
State ownership is mixed, combat pairing is implicit, and mutation paths are spread across many classes.
The result is a system that works by accumulated exceptions instead of one explicit combat model.

## Current Pipeline
### Cold data creation
- Figures are loaded as raw `JsonObject`s in `FigureLoader`.
- Abilities are loaded into `AbilityLoader.AbilityData`.
- NPC teams are loaded through `NPCTeamLoader`.
- Persistent figure state is stored on `ItemFigure` NBT.

### Hot battle creation
- Player battles currently start through `CommandTeeny.startBattle`.
- Player team figures come from `TitanManager.getTeamStack`.
- Player hot state is built with `BattleState.initializeBattle`.
- Opponent hot state is built manually on a spawned dummy instead of using one shared session bootstrap path.

### Battle loop
- `ModEvents.onLivingTick` calls `BattleState.tick` for any living entity with the capability.
- `ModEvents.onPlayerTick` forces held-slot lock behavior and sends a full HUD sync packet every tick.
- `BattleState.tick` handles mana regen, battery logic, accessory ticking, charge/channel timers, figure cooldown ticking, active effects, and projectile resolution.

### Ability execution
- `ItemAbility` and `AttackEntityEvent` route into `AbilityExecutor`.
- `AbilityExecutor` validates state, resolves targets, handles special branches, runs traits, applies damage, then applies effects.
- Damage output is built in `DamagePipeline.calculateOutput`.
- Mitigation is applied in `DamagePipeline.calculateMitigation`.
- Effect application is routed through `EffectApplierRegistry` into `BattleState.applyEffect` and `EffectRegistry`.

### Faint and battle end
- HP is virtual on `BattleFigure`.
- Standard hit resolution eventually calls `BattleState.checkFaint`.
- `checkFaint` does round reset on swap-faint or victory/defeat setup when no figures remain.

## Critical Findings
### 1. There is no explicit battle session or explicit opponent pairing
Relevant code:

- `ModEvents.java:187-191`
- `AbilityExecutor.java:303-304`
- `AbilityExecutor.java:606-609`
- `AccessoryExecutor.java:219-225`
- `BattleState.java:487-490`
- `BattleState.java:1117-1128`

The engine usually finds an opponent by scanning nearby entities and taking the first match.
That is the core reason many systems feel fragile.

Consequences:

- target resolution can pick the wrong entity if more than one battle-capable entity is nearby
- the HUD can sync against the wrong opponent
- chip hooks, accessories, mine ownership checks, victory logic, and swap reset visuals all rely on proximity instead of an authoritative pair
- the current system cannot scale cleanly to multiple nearby battles, PvP, summon-heavy battles, or proper NPC encounters

General fix direction:

- create an explicit `BattleSession` or equivalent pairing object
- each participant should know its enemy participant or session id directly
- remove all `findFirst nearby living entity with battle capability` logic from core combat flow

### 2. State ownership is wrong: too much lives on `BattleState`, not on the figure that actually owns it
Relevant code:

- `BattleState.java:65-67`
- `BattleState.java:172-210`
- `BattleState.java:718-799`
- `BattleFigure.java:92-119`
- `TraitRegistry.java:149-160`

`BattleState` owns `activeEffects`, `internalCooldowns`, `slotProgress`, charge state, blue-channel state, and other temporary combat flags.
But many of those are really figure-specific, not participant-specific.

Examples:

- buffs and debuffs live on the participant, so they follow the active slot instead of staying with the figure that received them
- `activate` progress uses `slotProgress` on `BattleState`, so a slot can keep charge-like progress across swaps
- `lockedSlot` is participant-wide even when it is caused by one ability or one effect on one figure

Consequences:

- swaps preserve state that should probably stay on the old figure
- effect semantics become inconsistent and hard to reason about
- refactoring one mechanic risks breaking several unrelated ones because they share the same state bucket

General fix direction:

- keep participant resources like mana, battery, tofu, and equipped accessory on participant state
- move figure HP, cooldowns, effects, slot progress, pending casts, and figure-only temporary flags onto a per-figure combat state

### 3. Ability cooldowns are effectively not implemented
Relevant code:

- `BattleFigure.java:27`
- `BattleFigure.java:67-71`
- `BattleFigure.java:123-148`
- `BattleState.java:943-949`
- `AbilityExecutor.java`

`BattleFigure` has `abilityCooldowns`.
The tick loop decrements them.
The HUD sync sends them.
But nothing ever calls `BattleFigure.setCooldown`, and no execution path checks cooldown before letting an ability fire.

Consequences:

- docs and HUD imply a mechanic that is not actually active
- future balance work will be misleading because cooldowns appear to exist but do not affect gameplay

General fix direction:

- decide whether cooldowns are real or should be removed
- if real, add one central cooldown policy inside the combat resolver and remove ad hoc assumptions elsewhere

### 4. HP mutation is not centralized
Relevant code:

- `AbilityExecutor.java:769-772`
- `AbilityExecutor.java:859`
- `EffectRegistry.java:171-176`
- `EffectRegistry.java:371-372`
- `ChipExecutor.java:127-129`

Standard hits route through `AbilityExecutor.applyDamageToFigure`.
But other paths directly change HP on `BattleFigure`.

Examples:

- poison directly calls `victimFigure.modifyHp(-mit.finalDamage)`
- `self_shock` directly calls `target.modifyHp(-magnitude)`
- some chip healing directly calls `ownerFigure.modifyHp(healAmount)`

Consequences:

- faint checks and battle-end checks are not guaranteed to run
- kill credit, chip triggers, and future death hooks can be skipped
- there is no single place to enforce invariants around HP changes

General fix direction:

- route all HP changes through one combat-state mutation API
- that API should be responsible for damage application, healing, faint checks, and follow-up hooks

### 5. Defeat cleanup logic is asymmetric and currently broken
Relevant code:

- `BattleState.java:339-355`
- `BattleState.java:505-526`

The victory timer is only advanced when `battleWon` is `true`.
On player defeat, `checkFaint` sets `battleWon = false`, `victoryTimer = 40`, and `winnerPlayer = sp`.
That means the timer is set, but the battle-state tick path that consumes it never runs.

Consequences:

- defeat cleanup can get stuck
- battle state may remain active after all figures are gone

General fix direction:

- stop encoding end-of-battle flow as `battleWon + victoryTimer`
- create an explicit end state such as `NONE`, `WIN_PENDING`, `LOSS_PENDING`

## Major Structural Findings
### 6. `BattleState` and `AbilityExecutor` are god objects
Relevant code:

- `BattleState.java:282-333`
- `BattleState.java:336-445`
- `BattleState.java:449-528`
- `AbilityExecutor.java:51-217`
- `AbilityExecutor.java:311-603`
- `AbilityExecutor.java:721-893`

`BattleState` currently mixes:

- participant state
- figure state
- inventory presentation
- player movement changes
- accessory lifecycle
- effect ticking
- projectiles
- victory and teleport cleanup

`AbilityExecutor` currently mixes:

- input validation
- target selection
- special-case mechanics
- mana-spend logic
- trait execution
- damage application
- opponent effect application
- self-effect application
- particles
- debug messages

This makes the code hard to test and hard to refactor in small pieces.

General fix direction:

- split execution into smaller stages with stable interfaces
- examples: `ActionValidator`, `TargetResolver`, `CostCommitter`, `DamageResolver`, `EffectResolver`, `CombatNotifier`

### 7. Opponent bootstrap logic is duplicated and bypasses the main initializer
Relevant code:

- `CommandTeeny.java:302-357`
- `BattleState.java:120-148`

The player uses `initializeBattle`.
The dummy opponent does not.
The dummy path manually sets `setBattling(true)`, clears the team, injects figures, sets owner, and sets the active figure.

Consequences:

- battle startup rules are not guaranteed to stay consistent between participants
- future initialization changes can silently only affect one side

General fix direction:

- one bootstrap path should create both participants
- command/debug entry should call the same service as future NPC/world encounters

### 8. Identity is weak; duplicate figures on a team are ambiguous
Relevant code:

- `BattleState.java:306-317`
- `ModEvents.java:283-295`

Bench icons only preserve `FigureID`.
Swap resolution finds the first team slot with the same figure id.
If the player has duplicate figures of the same type, there is no stable per-instance identity.

Consequences:

- swap can choose the wrong instance
- any future per-figure runtime state will become unsafe without runtime ids

General fix direction:

- each battle figure needs a runtime instance id
- bench controls should target runtime ids or slot ids, not figure species id

## Ability Execution Findings
### 9. Mana spend timing is inconsistent
Relevant code:

- `AbilityExecutor.java:185-210`
- `AbilityExecutor.java:459-577`
- `TraitRegistry.java:120-126`

Different mechanics commit mana at different times:

- normal abilities usually spend mana at the end of execution
- charge-up spends mana up front
- delayed projectiles spend mana before projectile resolution
- some no-target failures still spend mana

Consequences:

- cost semantics are hard to reason about
- future interruption or refund logic will be difficult to implement cleanly

General fix direction:

- define clear stages: validate, reserve/commit cost, schedule/execute, resolve outcome
- use one policy for when mana is consumed and when it is refunded or not refunded

### 10. Some extension points are dead, partial, or misleading
Relevant code:

- `TraitRegistry.java:247-255`
- `DamagePipeline.java:23-24`
- `BattleFigure.java:146`

Examples:

- `TraitRegistry.triggerMitigationHooks` exists but is never called
- `DamageResult.knockback` and `DamageResult.effects` are unused
- cooldown arrays exist but are not wired into gameplay

These partial abstractions make the engine look more generic than it really is.

General fix direction:

- remove dead extension points or implement them fully
- prefer fewer, real extension points over many placeholders

### 11. Golden bonus parsing is ad hoc and brittle
Relevant code:

- `AbilityExecutor.java:236-243`
- `AbilityExecutor.java:560-567`
- `AbilityExecutor.java:869-879`
- `TraitRegistry.java:181-239`

`golden_bonus` strings are parsed with `split(":")` and custom rules in several places.
There is no typed schema for what a golden bonus actually is.

Consequences:

- bonus syntax is hard to validate
- behavior depends on where the string is parsed
- adding new bonus types makes the parser complexity spread further

General fix direction:

- replace `List<String> goldenBonus` with structured data objects
- validate them at reload time

### 12. Some ability data already references behavior that does not exist
Relevant code:

- `src/main/resources/data/teenycraft/abilities/pure_effects/bar_deplete.json:16`
- `src/main/resources/data/teenycraft/abilities/pure_effects/poison.json:16`
- `TraitRegistry.java:98-131`

`trait:instant_cast` appears in ability JSON, but the runtime only implements `instant_cast_chance`.

Consequences:

- content can imply mechanics that do nothing
- debugging balance and mastery bonuses becomes confusing

General fix direction:

- add reload-time validation for effect ids, trait ids, and golden bonus ids
- fail loudly instead of silently accepting unknown content contracts

## Damage And RNG Findings
### 13. The shuffle-bag model is being bypassed by ad hoc random rolls
Relevant code:

- `BattleFigure.java:165-217`

The current dodge and crit systems use shuffle bags.
But when modifiers are added, the code often falls back to `Math.random()` alongside the bag instead of modifying the bag model itself.

Consequences:

- probability behavior is no longer truly bag-based
- luck and dodge modifiers are harder to reason about and test

General fix direction:

- either commit to shuffle bags and implement modifiers inside that model
- or replace shuffle bags with one explicit RNG policy that supports modifiers cleanly

### 14. Reflect is overloaded and currently mixes several meanings
Relevant code:

- `EffectApplierRegistry.java:292-304`
- `DamagePipeline.java:231-233`
- `BattleFigure.java:118`
- `AbilityExecutor.java:806-837`
- `EffectRegistry.java:257-270`

The `reflect` effect currently stores:

- damage reduction in `magnitude`
- reflection strength in `power`
- extra self-stun and movement freeze as separate side effects

Then runtime uses it in multiple incompatible ways:

- mitigation treats `reflect.magnitude` as percent of damage kept
- `BattleFigure.getEffectiveStat(REFLECT_PERCENT)` also adds `getEffectMagnitude("reflect")`, which makes the same value act like a generic reflect-percent stat
- `applyDamageToFigure` then performs both generic reflect damage and special reflect damage

That means reflect is not just overloaded in data storage.
It is overloaded in combat meaning too.

General fix direction:

- split reflect into typed fields and one clear runtime meaning
- do not let one effect id represent both a reduction factor and a generic reflect stat contribution

### 15. Source linkage between composed effects is missing
Relevant code:

- `EffectApplierRegistry.java:298-302`
- `EffectRegistry.java:265-270`

`reflect` applies separate `stun` and `freeze_movement`.
When `reflect` is removed, its `onRemove` removes `stun` and `freeze_movement` globally.

Consequences:

- removing reflect can remove stun or freeze that came from another source
- there is no ownership model for "child effects" created by another effect

General fix direction:

- effects that create other effects should track source linkage
- alternatively, multi-part mechanics should be represented as one typed status object instead of multiple loose effect ids

### 16. Poison stores caster UUID but does not really use it
Relevant code:

- `EffectInstance.java:8-9`
- `EffectApplierRegistry.java:372`
- `EffectRegistry.java:160-172`

Poison stores `casterUUID`, but `PoisonEffect.onPeriodicTick` does not resolve the attacker state or attacker figure.
It passes `null` attacker context into `calculatePoisonTick`.

Consequences:

- periodic damage does not correctly use the original attacker context
- future ownership-sensitive mechanics like kill credit, chip hooks, crit scaling, or threat logic will be wrong

General fix direction:

- periodic effects that need attacker context must resolve attacker through the explicit battle session or participant map

## Effect System Findings
### 17. `EffectInstance` is too generic for the jobs it is doing
Relevant code:

- `EffectInstance.java:3-25`
- `PeriodicBattleEffect.java:7-46`
- `BattleState.java:737-752`

Current overloaded meanings:

- `magnitude` can mean stacks, interval, charges, blocked slot, or mine stage
- `power` can mean total periodic payload, reflect payload, mana snapshot, or flight duration snapshot
- generic merge rules in `BattleState.applyEffect` decide how reapplication behaves for all effects

Consequences:

- effect behavior is difficult to understand by reading a single effect implementation
- generic reapply behavior is likely wrong for some mechanics

General fix direction:

- replace the generic payload pair with typed effect state per effect family
- at minimum, move stacking and refresh policy into the effect definition instead of hardcoding one generic merge algorithm

### 18. `EffectApplierRegistry` and `EffectRegistry` duplicate behavior
Relevant code:

- `EffectApplierRegistry.java`
- `EffectRegistry.java`

`EffectApplierRegistry` computes values and also re-checks gates like `kiss` or `cleanse_immunity`.
`EffectRegistry` also re-checks many of the same gates inside `onApply`.

Consequences:

- rules are duplicated
- future edits can drift between applier-time and effect-time behavior

General fix direction:

- appliers should mostly translate data into typed application requests
- effect definitions should own acceptance, stacking, refresh, tick, and removal rules

### 19. Unknown effect ids silently fall back to a generic default path
Relevant code:

- `EffectApplierRegistry.java:23-36`

If an effect id is not registered, the registry returns a generic fallback applier instead of failing.

Consequences:

- content typos can silently produce strange runtime behavior instead of a load-time failure
- the engine becomes harder to trust as data grows

General fix direction:

- remove the silent fallback from gameplay-critical content loading
- validate all effect ids during data reload

### 20. Instant effects are not consistently treated as instant
Relevant code:

- `EffectRegistry.java:467-483`
- `BattleState.java:728-752`

`CleanseEffect` is declared as `INSTANT`, but its `onApply` returns `true`.
That means `BattleState.applyEffect` stores it in `activeEffects` after applying its instant behavior.

Consequences:

- the runtime meaning of `EffectType.INSTANT` is not actually enforced
- some instant effects can remain as active entries if their `onApply` path returns `true`

General fix direction:

- the battle-state layer should enforce effect type semantics
- instant effects should not be stored unless explicitly modeled as two separate effects

### 21. Category removals are mutation-sensitive and unsafe
Relevant code:

- `BattleState.java:813-823`
- `EffectRegistry.java:265-270`
- `EffectRegistry.java:493-495`

`removeEffectsByCategory` iterates the active map with `removeIf` and calls `onRemove`.
Some `onRemove` paths call `removeEffect` again.

Consequences:

- category cleanup logic is vulnerable to concurrent modification problems
- complex effect interactions are hard to guarantee safely

General fix direction:

- category removals should work on a snapshot and then remove one effect at a time through the same safe removal path

### 22. Faint cleanup bypasses effect teardown
Relevant code:

- `BattleState.java:466-471`
- `BattleState.java:1036`

Round reset and end battle both use raw map clearing in places.
That bypasses `onRemove` for active effects.

Consequences:

- movement, gravity, slot locks, or future effect-side cleanup can leak or fail to run

General fix direction:

- clear effects through one unified effect-removal path

## Accessory And Chip Findings
### 23. Accessories are only half data-driven
Relevant code:

- `AccessorySpec.java`
- `AccessoryRegistry.java`
- `AccessoryExecutor.java`

The registry supports a few generic forms, but actual behavior still contains id-specific branches like:

- `bat_signal`
- `krypto_the_superdog`
- `supermans_underpants`
- `birdarang`

Consequences:

- accessory growth will continue to enlarge `AccessoryExecutor`
- battle engine behavior is partially hidden in content and partially hidden in hardcoded runtime branches

General fix direction:

- either commit to a typed accessory behavior model
- or explicitly classify accessories as code-defined runtime plugins instead of pretending they are mostly data-driven

### 24. Accessory logic bypasses parts of the regular ability/effect pipeline
Relevant code:

- `AccessoryExecutor.java:113-160`
- `AccessoryExecutor.java:178-187`

Some accessories directly apply effects or direct damage without going through the same validation and resolution path as abilities.

Consequences:

- runtime behavior consistency is lower
- interaction bugs are more likely when battle rules change

General fix direction:

- define accessory actions as first-class combat actions in the shared combat resolver

### 25. Chip hooks strengthen the need for explicit pairing
Relevant code:

- `ChipExecutor.java:76-98`
- `BattleState.java:1104-1109`

Chip hooks are event-driven and use opponent state/entity references passed in from nearby lookups.
That is manageable only while battles are simple and isolated.

General fix direction:

- chips should receive combat context from the authoritative session, not from "nearest likely opponent" queries

## UI And Networking Findings
### 26. The battle control surface is implemented by rebuilding the player inventory
Relevant code:

- `BattleState.java:282-333`
- `ItemAbility.java`
- `ItemTofu.java`
- `ItemAccessory.java`

This works, but it couples gameplay input to vanilla inventory state very tightly.

Consequences:

- battle UI logic is mixed into server inventory mutation
- future battle-control changes will stay expensive and invasive

General fix direction:

- keep this for now if needed
- but treat it as a presentation adapter, not as the battle model itself

### 27. HUD sync is authoritative only by convention, not by explicit battle link
Relevant code:

- `ModEvents.java:183-250`
- `PacketSyncBattleData.java`

The server sends a full snapshot every tick and again finds the nearest opponent by proximity.

Consequences:

- HUD and runtime may disagree if the nearest target is not the actual paired opponent
- packet shape is large and tightly coupled to server internals

General fix direction:

- once session pairing exists, sync should use the session enemy directly
- the packet should represent a stable view model, not a raw scrape of live battle internals

## Lower-Priority But Real Findings
### 28. Damage preview logic duplicates combat math
Relevant code:

- `FigurePreviewHelper.java:74-95`
- `DamagePipeline.java:93-156`

Preview damage is not built from the same resolver as real damage.
That is a maintenance risk.

### 29. Debug and UX messaging is inside core combat code
Relevant code:

- `AbilityExecutor.java`
- `EffectApplierRegistry.java`
- `AccessoryExecutor.java`
- `BattleState.java`

Core logic is filled with `sendSystemMessage`, particles, sounds, and direct entity feedback.
That makes logic noisy and harder to reuse or test.

### 30. Player hit feedback still depends on a vanilla damage hack
Relevant code:

- `AbilityExecutor.java:884-888`

After virtual damage is applied, the target receives `0.01f` real damage and players are reset to max health.
That is a practical workaround, but it is also a signal that engine-side feedback and vanilla-side feedback are not cleanly separated.

## Recommended Refactor Order
### Phase 1: Safety Harness
- Add characterization tests for the current behavior before changing architecture.
- Cover at least: swap, faint, defeat cleanup, poison kill, charge-up, blue channel, mine detonation, accessory activation, and one golden bonus path.
- Add reload-time validation for effect ids, trait ids, and golden bonus ids.

### Phase 2: Session And Pairing
- Introduce explicit battle pairing.
- Remove proximity-based opponent lookup from core battle code.
- Move command-driven battle start onto one bootstrap service that initializes both sides the same way.

### Phase 3: State Ownership Cleanup
- Split participant state from figure combat state.
- Move effects, cooldowns, slot progress, and figure-only temporary mechanics off participant state.
- Give each battle figure a stable runtime identity.

### Phase 4: Centralize Combat Mutation
- Create one mutation path for damage, healing, effect application, swap, and faint resolution.
- Remove direct `modifyHp` calls from scattered systems.
- Make end-of-battle status explicit instead of encoding it through `battleWon`.

### Phase 5: Ability And Effect Contract Cleanup
- Replace string `golden_bonus` parsing with typed data.
- Replace generic `EffectInstance` payload overloading with typed effect state or effect-family-specific state.
- Move stacking and refresh rules into effect definitions.
- Remove the unknown-effect fallback applier.

### Phase 6: Presentation Extraction
- Keep inventory-based controls if needed, but isolate them behind a battle-UI adapter.
- Move chat messages, particles, and sounds out of the resolver layer.
- Make packet sync consume a stable battle view model.

## Suggested First Implementation Thread
If another chat starts implementing this, the safest first thread is:

1. Add tests and validation.
2. Fix the defeat cleanup bug.
3. Implement explicit opponent pairing.
4. Implement real ability cooldown state or delete the fake cooldown mechanic.
5. Centralize HP mutation and faint checks.

That sequence improves correctness immediately without forcing a full rewrite first.
