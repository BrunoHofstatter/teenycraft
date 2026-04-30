# Battle Engine

## Purpose
Document the implemented runtime combat model: how battles are entered, how team figures become battle participants, how the live battle loop runs, and where combat behavior should be changed in code.

## Current Status
The repo contains a working real-time battle foundation centered on a battle-state capability attached to living entities, `BattleFigure` runtime snapshots, `AbilityExecutor`, the damage pipeline, effect and trait registries, and client sync for the battle overlay.
Phase 1 of the refactor adds a safety harness around that runtime with Forge GameTests for key battle flows and reload-time validation for battle content references.
Phase 2 now fixes the main active-figure HP-to-zero correctness hole without a broad architecture refactor: effect-driven direct HP changes that can kill the active figure now route back through `BattleState` so faint, swap, defeat, and cleanup resolve consistently.
Phase 3 now adds explicit authoritative pairing for active battles: each battler stores its paired opponent identity in `BattleState`, paired battle bootstrap initializes both sides together, and the covered runtime paths no longer scrape the nearest battle-capable entity for correctness-critical opponent resolution.
Phase 4 now separates participant-owned battle state from figure-owned combat state: per-figure temporary combat buckets live on `BattleFigure`, duplicate bench swaps use authoritative tagged team indices, and Phase 2 HP handling plus Phase 3 pairing remain intact.
Phase 5 now centralizes correctness-critical battle HP mutation on `BattleState`: standard hit damage, healing, periodic HP deltas, self-damage, and chip-driven battle HP changes all resolve through one combat-mutation flow for faint, kill, reset, defeat, and follow-up hooks.
Phase 6 now splits executor responsibilities into smaller runtime helpers: `AbilityExecutor` stays as the public entry facade, `battle.executor.BattleAbilityExecution` handles validation, targeting, scheduling, and self or follow-up stages, `battle.executor.BattleTargeting` owns paired target resolution, and `battle.executor.BattleDamageResolver` owns hit mutation invocation plus post-hit ordering while still delegating authoritative HP outcomes to `BattleState`.
Phase 7 now completes the refactor by parsing `golden_bonus` on load, aligning validation with explicit effect and trait contracts, and removing gameplay-critical generic fallback behavior from validated battle-content paths.
The planned battle-refactor phases are now complete. Intentionally unchanged non-goals now center on debug-only command paths plus broader presentation or session redesign work.
Post-refactor cleanup now separates more of the presentation plumbing without changing battle outcomes: HUD sync builds a typed snapshot before packet serialization, the client overlay consumes that structured snapshot, and battle hotbar reconstruction flows through a dedicated loadout helper instead of living inline in `BattleState`.
Class advantage is now implemented in the live damage pipeline with explicit per-hit bonus tracking so presentation can later show the class bonus separately instead of only as part of final total damage.
Non-player battlers now also use more of that same runtime correctly: `BattleState` updates owner speed and delayed projectiles against the owning entity, and dummy presentation now follows active-figure damage and swaps so arena opponents can run the new first-pass battle AI on top of the normal combat model.

## Player-Facing Behavior
- Battles are real-time, not turn-based.
- The active battle team comes from the Titan Manager team slots, not the vanilla hotbar.
- Active figures spend mana, respect cooldowns, and use virtual figure HP separate from normal item persistence.
- During battle, the player inventory is replaced with battle controls: ability items, tofu when present, an accessory slot, and bench figure swap icons.
- Participant resources such as mana, tofu, battery, accessory runtime, and arena linkage stay on `BattleState`, while temporary combat state such as effects, cooldown buckets, slot progress, and charge/channel state live on the owning `BattleFigure`.
- Team defeat occurs when all figures on that battle team have fainted.

## Source Of Truth
- [`src/main/java/bruhof/teenycraft/battle/AbilityExecutor.java`](../../src/main/java/bruhof/teenycraft/battle/AbilityExecutor.java)
- [`src/main/java/bruhof/teenycraft/battle/executor/BattleAbilityExecution.java`](../../src/main/java/bruhof/teenycraft/battle/executor/BattleAbilityExecution.java)
- [`src/main/java/bruhof/teenycraft/battle/executor/BattleTargeting.java`](../../src/main/java/bruhof/teenycraft/battle/executor/BattleTargeting.java)
- [`src/main/java/bruhof/teenycraft/battle/executor/BattleDamageResolver.java`](../../src/main/java/bruhof/teenycraft/battle/executor/BattleDamageResolver.java)
- [`src/main/java/bruhof/teenycraft/battle/BattleFigure.java`](../../src/main/java/bruhof/teenycraft/battle/BattleFigure.java)
- [`src/main/java/bruhof/teenycraft/battle/FigureClassType.java`](../../src/main/java/bruhof/teenycraft/battle/FigureClassType.java)
- [`src/main/java/bruhof/teenycraft/battle/damage/DamagePipeline.java`](../../src/main/java/bruhof/teenycraft/battle/damage/DamagePipeline.java)
- [`src/main/java/bruhof/teenycraft/capability/IBattleState.java`](../../src/main/java/bruhof/teenycraft/capability/IBattleState.java)
- [`src/main/java/bruhof/teenycraft/capability/BattleState.java`](../../src/main/java/bruhof/teenycraft/capability/BattleState.java)
- [`src/main/java/bruhof/teenycraft/capability/BattleInventoryLoadoutBuilder.java`](../../src/main/java/bruhof/teenycraft/capability/BattleInventoryLoadoutBuilder.java)
- [`src/main/java/bruhof/teenycraft/capability/BattleStateProvider.java`](../../src/main/java/bruhof/teenycraft/capability/BattleStateProvider.java)
- [`src/main/java/bruhof/teenycraft/event/ModEvents.java`](../../src/main/java/bruhof/teenycraft/event/ModEvents.java)
- [`src/main/java/bruhof/teenycraft/battle/presentation/BattleHudSnapshot.java`](../../src/main/java/bruhof/teenycraft/battle/presentation/BattleHudSnapshot.java)
- [`src/main/java/bruhof/teenycraft/battle/presentation/BattleHudSnapshotBuilder.java`](../../src/main/java/bruhof/teenycraft/battle/presentation/BattleHudSnapshotBuilder.java)
- [`src/main/java/bruhof/teenycraft/battle/validation/BattleContentValidation.java`](../../src/main/java/bruhof/teenycraft/battle/validation/BattleContentValidation.java)
- [`src/main/java/bruhof/teenycraft/battle/validation/BattleContentValidator.java`](../../src/main/java/bruhof/teenycraft/battle/validation/BattleContentValidator.java)
- [`src/main/java/bruhof/teenycraft/gametest/BattleGameTests.java`](../../src/main/java/bruhof/teenycraft/gametest/BattleGameTests.java)
- [`src/main/java/bruhof/teenycraft/command/CommandTeeny.java`](../../src/main/java/bruhof/teenycraft/command/CommandTeeny.java)
- [`src/main/java/bruhof/teenycraft/networking/PacketSyncBattleData.java`](../../src/main/java/bruhof/teenycraft/networking/PacketSyncBattleData.java)
- [`src/main/java/bruhof/teenycraft/item/custom/battle/ItemAbility.java`](../../src/main/java/bruhof/teenycraft/item/custom/battle/ItemAbility.java)
- [`src/main/java/bruhof/teenycraft/capability/TitanManager.java`](../../src/main/java/bruhof/teenycraft/capability/TitanManager.java)
- [`src/main/java/bruhof/teenycraft/TeenyBalance.java`](../../src/main/java/bruhof/teenycraft/TeenyBalance.java)

## Phase 1-7 Safety Coverage
Current safety and correctness coverage now includes:

- live GameTests for battle start, swap, faint reset, defeat cleanup, poison tick kill, self-shock kill, charge-up, blue channel, accessory activation, remote mine, and golden bonus paths covering base self plus Phase 7 parsed self/opponent contracts
- mutation-path GameTests for standard-hit kill healing, poison kill healing, and self-shock faint battery hooks
- class-advantage GameTests for last-hit-only multi-hit behavior, minimum `+1` bonus handling, and per-victim matchup evaluation
- delayed-projectile GameTests for mana spend on fire plus resolution-time flight cancellation through `resolveProjectile`
- nearby-distractor GameTests for paired-opponent correctness in faint reset, accessory opponent hooks, and remote mine targeting
- ownership GameTests for figure-local effects/progress cooldown buckets, duplicate bench targeting, and non-leaking charge state across active-figure changes
- reload-time validation for figure ability ids, NPC team figure ids, NPC team golden ability ids, explicit gameplay effect ids, explicit gameplay trait ids, and parsed golden-bonus contracts
- explicit compatibility handling for legacy `trait:instant_cast` golden bonuses as a validated no-op alias rather than a warning-only contract exception
- Phase 2 re-verification of the `ArenaBattleManager.finishBattleForPlayer` defeat cleanup path in runtime code; that exact packet-and-teleport path is not directly GameTested because Forge mock `ServerPlayer` instances do not have a full network channel

## Runtime Model
The battle engine is built around two layers:

- `BattleState`: hot volatile state attached as a capability to a living entity
- `BattleFigure`: hot per-figure snapshot created from a persistent `ItemFigure`

`BattleState` owns:

- the current team of `BattleFigure` objects
- the active figure index
- the authoritative paired-opponent entity id for the active battle
- mana, tofu mana, battery charge, and accessory state
- participant-level `reset_lock`
- pending projectile state
- win and cleanup timers
- the authoritative combat-mutation flow for battle HP deltas, faint bookkeeping, and kill or faint follow-up hooks

`BattleFigure` owns:

- snapshotted base stats from the source figure item
- current virtual HP
- ability cooldowns
- figure-owned active effects and internal cooldowns
- per-slot activate progress
- slot lock and pending charge-up or blue-channel runtime
- dodge and crit shuffle bags
- temporary accessory HP bonus

`reset_lock` remains a narrow participant-level exception because it gates the whole side during round reset rather than belonging to one specific figure.

The original figure `ItemStack` is still kept on the `BattleFigure`, so battle can keep reading persistent data such as ability order, cost tiers, and golden ability status.

## Battle Entry And Participants
Current implemented entry is command-driven through `/teeny battle start`.

Current flow:

1. Read the player's three team slots from the Titan Manager.
2. Resolve an arena id from command input or the current default arena set.
3. Choose a free fixed battle slot in the Teenyverse and paste the arena template into that slot.
4. Teleport the player into the Teenyverse if needed, then place them at the arena's authored player spawn.
5. Build the opposing team either from NPC team JSON or a fallback debug boss.
6. Spawn an `EntityTeenyDummy` at the arena's authored opponent spawn.
7. Initialize both participants' `BattleState` data through one paired bootstrap flow so each side starts with an authoritative opponent id before initial figure activation.
8. Activate each side's opening figure and then replace the player's normal inventory with battle controls.

Important current details:

- `BattleState` capabilities are attached to all living entities, so both players and dummy opponents use the same runtime state type.
- Paired opponent identity is stored directly on each active `BattleState`, and correctness-critical runtime paths use that pairing instead of nearby-entity scans.
- `EntityTeenyDummy` now runs a first-pass battle AI goal, but still executes abilities and swaps through the same battle runtime used by players.
- Arena metadata is separate from arena structure geometry: JSON defines runtime metadata and NBT defines the authored build.
- The current battle start path is still command-driven debug scaffolding, but it no longer depends on a hardcoded iron platform.
- Arena battle sessions can now also run arena pickup logic, temporary walls, and launch state on top of the normal battle loop.
- Entering the Teenyverse causes the Titan Manager capability to save and clear the player's vanilla inventory. Leaving the Teenyverse restores it.

## Battle Controls And Inventory Replacement
During battle, `BattleState.refreshPlayerInventory` clears the player's current inventory and fills it with battle items.
The timing for those refreshes still lives on `BattleState`, but slot population now flows through `BattleInventoryLoadoutBuilder`.

Current slot usage:

- hotbar slots `0` to `2`: battle ability items for the active figure's three ability slots
- slot `4`: tofu item if tofu is currently available
- slot `5`: the equipped Titan Manager accessory in battle form
- slots `6` to `8`: bench figure icons for swapping

Current interaction model:

- left-click with an `ItemAbility` triggers melee attacks through `AttackEntityEvent`
- right-click with an `ItemAbility` triggers ranged, self, and utility abilities
- right-clicking a bench `ItemFigure` during battle swaps to that tagged team slot if valid, so duplicate species are authoritative instead of first-match ambiguous
- some states lock the selected slot, and the server forces the player's held slot to stay aligned

The battle inventory is not the persistent figure inventory. It is a temporary control surface rebuilt from the active battle state, with the loadout-building logic isolated from the state authority that decides when rebuilds happen.

## Core Tick Loop
`BattleState.tick` runs every server tick for living entities that have the battle capability.

Current tick responsibilities:

- handle delayed victory cleanup
- decrement swap cooldown
- regenerate mana
- charge the battery passively
- spawn battery collection thresholds
- tick active accessory runtime and battery drain
- tick each `BattleFigure` ability cooldown array and internal cooldown map
- tick the active figure's charge-up state
- tick participant reset-lock state and each figure's owned effect map
- resolve delayed projectiles

Additional event-driven runtime:

- `ModEvents.onLivingTick` detects charge completion and finishes charge-up casts
- `ModEvents.onLivingTick` also advances blue-channel abilities
- `ModEvents.onPlayerTick` syncs battle state to the client overlay every tick while battling by first building a typed HUD snapshot and then serializing that snapshot through `PacketSyncBattleData`
- `ArenaBattleManager` also ticks active arena sessions each server tick so pickup respawns, collection checks, temporary walls, and launch state stay synchronized with the battle

## Mana, Battery, Tofu, And Accessories
### Mana
- Mana is stored on `BattleState`, not on the figure item.
- Mana regenerates every tick up to `BATTLE_MANA_MAX`.
- Stun, charging, and blue-channeling stop mana regen.
- Curse reduces mana regen.
- Dance increases mana regen.
- Ability execution can now separate actual mana cost from hidden effective mana cost: the player only pays and sees the actual slot cost, while scaling-heavy formulas can read a different effective value for slot-based efficiency tuning.

### Battery
- Battery charge is also stored on `BattleState`.
- It fills passively over time and also gains charge from mana spent on successful actions.
- A battery pickup threshold spawns as a target percentage of the mana bar.
- When current mana crosses that threshold, the battery is collected and charge is awarded.
- Battery gain from successful ability use follows actual mana spent, not hidden effective mana scaling.

### Accessories
- The equipped accessory comes from the Titan Manager accessory slot.
- Accessories activate only when battery charge meets the activation minimum.
- Once active, the accessory drains battery each tick and runs its own executor hooks.
- Accessory state can modify battle behavior and may refresh the player's battle inventory.

### Tofu
- Tofu is stored as temporary battle-only mana state.
- When present, it appears as a separate battle item.
- Using tofu triggers a random self or opponent effect based on the stored tofu power.
- Tofu spawn chance from normal ability use also follows actual mana spent, while tofu power created by ability effects still follows the ability's gameplay scaling inputs.

## Ability Execution Flow
`AbilityExecutor` is now a thin public facade over the Phase 6 executor helpers.

Current top-level paths:

- `executeAttack` for melee abilities triggered by left-click
- `executeAction` for non-melee abilities triggered by right-click
- `finishCharge` for delayed charge-up casts
- `tickBlueChannel` for blue-channel abilities
- `resolveProjectile` for delayed raycast projectiles
- `executeTofu` for the temporary tofu item

Common pre-checks before an action executes:

- reset-lock check
- slot-lock check
- stun check
- charging or blue-channel lock check
- waffle slot-block check
- mana cost check
- target resolution for ranged abilities

Important runtime behaviors:

- `BattleAbilityExecution` keeps the current top-level cast ordering but now reads as orchestration: validate input, resolve target or schedule, run the cast body, then trigger the relevant follow-ups
- `BattleTargeting` owns the pairing-aware cone and opponent lookups used by action, charge, blue-channel, projectile, tofu, and remote-mine paths
- `BattleDamageResolver` owns standard hit resolution, mutation invocation, pet and reflect follow-ups, opponent-effect handoff, and the tiny vanilla hurt feedback, while `BattleState` still remains the single authoritative HP/faint/kill correctness path
- battle-target resolution for the covered runtime paths is pairing-aware: melee validation, ranged cone checks, remote mine detonation, tofu opponent effects, blue-channel ticks, accessory opponent hooks, first-appearance chip hooks, faint reset visuals, and HUD sync all resolve through the authoritative paired opponent
- raycast delay creates a `PendingProjectile`; mana and self effects happen on fire, and damage or opponent effects happen on projectile resolution
- remote mine casts can detonate an existing mine instead of placing or firing normally
- some traits can cancel or redirect execution through `TraitRegistry.triggerExecutionHooks`
- self effects and opponent effects are applied through `EffectApplierRegistry`
- `AbilityLoader` now parses `golden_bonus` into structured self, opponent, and trait contracts on reload, and the executor, damage, and trait runtime consumes those parsed records instead of reparsing raw strings
- validated gameplay content now uses explicit effect-input ids from `EffectApplierRegistry` and explicit trait ids from `TraitRegistry`, so bad battle content fails validation instead of generic-applying in core battle paths

## Damage And Mitigation Pipeline
Outgoing damage is built by `DamagePipeline.calculateOutput`, then incoming mitigation is resolved by `DamagePipeline.calculateMitigation`.

Current outgoing order:

1. Read the current ability and mana cost.
2. Skip normal damage if the ability is pure utility.
3. Compute base damage from Power, mana cost, `BASE_DAMAGE_PERMANA`, and damage tier multiplier.
4. Apply trait-based outgoing modifiers such as `activate`, `charge_up`, and `surprise`.
5. Add flat damage bonuses or penalties from active battle effects.
6. Build a `DamageResult`, then split it into per-hit packets when traits such as `multi_hit` are present.
7. Resolve class-advantage bonus per hit against the specific victim before mitigation.
8. Carry base damage and class bonus damage separately through mitigation so later UI can display the class portion independently.

Current incoming order:

1. Roll crit per hit through the attacker's crit shuffle bag.
2. Apply crit multiplier using effective Luck.
3. Apply defense percent modifiers.
4. Apply reflect-style damage reduction if the victim currently has reflect.
5. Consume shield if present.
6. Roll dodge mitigation unless the hit is undodgeable.
7. Apply flight evasion for non-group hits.

Important current rules:

- dodge is mitigation, not full avoidance by default
- crit and dodge both use shuffle bags rather than flat percent rolls
- shield is consumed when checked
- group damage breaks flight on the victim before mitigation
- class advantage follows the current cycle `Cute > Dark Arts > Super > Tech > Martial Arts > Beast > Cute`
- class advantage adds `20%` bonus damage with a minimum of `+1`
- class advantage is applied to the last split of a multi-hit attack, not to every split
- group damage evaluates class advantage separately for each struck figure
- figures with class `none` do not participate in advantage matchups
- poison-style abilities can suppress the instant damage portion and rely on follow-up effects
- direct hit damage, remote mine detonation, reflect retaliation, and blue-channel hit ticks can gain class advantage
- poison, pet follow-up damage, accessory damage, chip damage, and other non-direct fixed damage currently do not gain class advantage
- correctness-critical battle HP changes now route through the `BattleState` combat-mutation helpers rather than ad hoc `BattleFigure.modifyHp` calls
- standard hit damage still applies its HP delta before pet and reflect follow-up branches, then finalizes faint, kill, reset, and defeat through the same `BattleState` mutation flow so the old combat ordering stays intact
- heal, group-heal, health-radio, poison, self-shock, and chip heal hooks all use that same mutation flow for battle HP changes
- delayed periodic effects that persist a caster now keep the original source figure index alongside the participant UUID, so later kill or faint hooks resolve against the original figure instead of the source side's current active figure
- after virtual damage is applied, the target entity still receives a tiny vanilla hurt event for hit feedback

## Swapping And Figure Locks
Swapping is handled by `BattleState.swapFigure`.

A swap is blocked when:

- the player is stunned
- the player is rooted
- the destination figure has fainted
- the destination figure is disabled
- swap cooldown is active

Current swap behavior:

- swapping changes the active figure index
- swapping cancels flight
- swapping no longer drags the old active figure's effects, activate progress, or charge state onto the new active figure
- swapping applies a cooldown using `SWAP_COOLDOWN`
- the player's battle inventory is immediately rebuilt

Disable effects can also force a swap:

- disable is tracked per figure slot as `disable_0`, `disable_1`, and `disable_2`
- at most two disables are kept at once
- if the active figure becomes disabled, battle attempts to force-swap to the next valid alive figure

## Fainting, Round Reset, And Match End
Virtual HP mutation outcomes are finalized through the `BattleState` combat-mutation helpers, and active-figure defeat transitions still route through `BattleState.checkFaint`.

When the active figure faints and another team member is still alive:

- the next alive figure becomes active
- the fainted figure's active effects are cleared
- a participant-level `reset_lock` effect is applied
- both sides receive reset visuals
- the player's battle inventory is rebuilt

When no figures remain alive:

- dummy opponents mark their paired player battle state as a win
- players mark their own state as a defeat
- a short victory timer delays cleanup

After the victory timer ends:

- battle exit routes through the arena session manager
- both participant states are cleaned up
- the client battle overlay is turned off with `PacketSyncBattleData.off()`
- the player is returned to their pre-battle position rather than a hardcoded overworld fallback
- the active arena slot is cleared so the next battle can reuse it cleanly

Phase 2 note:

- the live defeat cleanup path has been re-verified in runtime code, not only the lighter battle-state harness
- the exact `finishBattleForPlayer` packet-and-teleport exit remains awkward to GameTest directly because mock `ServerPlayer` instances lack a real Netty channel
- the broader explicit end-state refactor from the roadmap is still deferred

## Client Sync
The client overlay does not inspect server battle logic directly. It receives a synced snapshot every player tick while battling.

Current opponent-facing sync now resolves against the authoritative paired opponent stored on `BattleState`, not the nearest battle-capable entity in range.

Current synced data includes:

- active and enemy figure ids, names, model types, and slot indices
- HP, mana, battery, cooldowns, and slot progress
- current ability ids, tiers, and golden flags
- active effect strings
- bench figure ids and HP summaries
- remote mine state flags, including mines that stay on benched figures

This sync is display-oriented. Runtime authority remains server-side in `BattleState`, `AbilityExecutor`, and the related battle registries.
The current synced HUD snapshot does not yet expose the separated class-bonus damage numbers; the runtime tracks them now so later UI work can surface them without redefining combat math.

## Design Notes
- Player and opponent battle participants should stay as symmetric as possible by sharing the same capability and runtime model.
- Battle runtime state should stay hot and volatile, while figure items remain the persistent long-term storage.
- Traits and effects should hook into the existing registries before new one-off battle branches are added.
- Numeric tuning belongs in `TeenyBalance.java`.
- Docs should distinguish current command-driven battle entry from future world or NPC encounter flows.

## Balance Hooks
- mana generation and cap
- ability mana cost by slot and cost tier
- cooldown timings
- damage multipliers
- class advantage multiplier
- crit and dodge rules
- flight, shield, reflect, and defense interactions
- swap cooldown and reset-lock timings
- battery generation and accessory drain
- arena pickup collection radius
- shared arena speed levels

## Open Questions
- how battle entry should evolve beyond the current command and debug flow
- how NPC or future PvP battles should choose and control opponents without relying on the current dummy-centric setup
- whether the saved-battle-state NBT should remain minimal or grow into real reconnect support
- whether more explicit battle phases are needed beyond the current state-plus-executor model
- whether periodic damage and other delayed effects should eventually store original source-figure identity, instead of the current paired-participant plus current-active-figure fallback used when only the caster UUID is available
- whether more indirect damage sources should opt into class advantage once their presentation and source-ownership rules are clearer
- whether the debug-only proximity lookups in [`src/main/java/bruhof/teenycraft/command/CommandTeeny.java`](../../src/main/java/bruhof/teenycraft/command/CommandTeeny.java) should be folded into the same pairing helpers later; Phase 4 still leaves those dev-command paths unchanged because they are not live battle-runtime authority

## Planned Additions
- richer accessory and chip integration
- fuller world encounter and NPC battle entry paths
- clearer AI parity documentation once the opponent control path is finalized
- more explicit extension points for future mechanics once the current battle loop stabilizes
