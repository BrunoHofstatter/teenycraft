# Battle Engine

## Purpose
Document the implemented runtime combat model: how battles are entered, how team figures become battle participants, how the live battle loop runs, and where combat behavior should be changed in code.

## Current Status
The repo contains a working real-time battle foundation centered on a battle-state capability attached to living entities, `BattleFigure` runtime snapshots, `AbilityExecutor`, the damage pipeline, effect and trait registries, and client sync for the battle overlay.

## Player-Facing Behavior
- Battles are real-time, not turn-based.
- The active battle team comes from the Titan Manager team slots, not the vanilla hotbar.
- Active figures spend mana, respect cooldowns, and use virtual figure HP separate from normal item persistence.
- During battle, the player inventory is replaced with battle controls: ability items, tofu when present, an accessory slot, and bench figure swap icons.
- Buffs, debuffs, cooldowns, charge state, battery state, and other temporary battle data live in `BattleState`, not on the figure item.
- Team defeat occurs when all figures on that battle team have fainted.

## Source Of Truth
- [`src/main/java/bruhof/teenycraft/battle/AbilityExecutor.java`](../../src/main/java/bruhof/teenycraft/battle/AbilityExecutor.java)
- [`src/main/java/bruhof/teenycraft/battle/BattleFigure.java`](../../src/main/java/bruhof/teenycraft/battle/BattleFigure.java)
- [`src/main/java/bruhof/teenycraft/battle/damage/DamagePipeline.java`](../../src/main/java/bruhof/teenycraft/battle/damage/DamagePipeline.java)
- [`src/main/java/bruhof/teenycraft/capability/IBattleState.java`](../../src/main/java/bruhof/teenycraft/capability/IBattleState.java)
- [`src/main/java/bruhof/teenycraft/capability/BattleState.java`](../../src/main/java/bruhof/teenycraft/capability/BattleState.java)
- [`src/main/java/bruhof/teenycraft/capability/BattleStateProvider.java`](../../src/main/java/bruhof/teenycraft/capability/BattleStateProvider.java)
- [`src/main/java/bruhof/teenycraft/event/ModEvents.java`](../../src/main/java/bruhof/teenycraft/event/ModEvents.java)
- [`src/main/java/bruhof/teenycraft/command/CommandTeeny.java`](../../src/main/java/bruhof/teenycraft/command/CommandTeeny.java)
- [`src/main/java/bruhof/teenycraft/networking/PacketSyncBattleData.java`](../../src/main/java/bruhof/teenycraft/networking/PacketSyncBattleData.java)
- [`src/main/java/bruhof/teenycraft/item/custom/battle/ItemAbility.java`](../../src/main/java/bruhof/teenycraft/item/custom/battle/ItemAbility.java)
- [`src/main/java/bruhof/teenycraft/capability/TitanManager.java`](../../src/main/java/bruhof/teenycraft/capability/TitanManager.java)
- [`src/main/java/bruhof/teenycraft/TeenyBalance.java`](../../src/main/java/bruhof/teenycraft/TeenyBalance.java)

## Runtime Model
The battle engine is built around two layers:

- `BattleState`: hot volatile state attached as a capability to a living entity
- `BattleFigure`: hot per-figure snapshot created from a persistent `ItemFigure`

`BattleState` owns:

- the current team of `BattleFigure` objects
- the active figure index
- mana, tofu mana, battery charge, and accessory state
- active effects and internal cooldowns
- charge-up, blue channel, and projectile state
- win and cleanup timers

`BattleFigure` owns:

- snapshotted base stats from the source figure item
- current virtual HP
- ability cooldowns
- dodge and crit shuffle bags
- temporary accessory HP bonus

The original figure `ItemStack` is still kept on the `BattleFigure`, so battle can keep reading persistent data such as ability order, cost tiers, and golden ability status.

## Battle Entry And Participants
Current implemented entry is command-driven through `/teeny battle start`.

Current flow:

1. Read the player's three team slots from the Titan Manager.
2. Resolve an arena id from command input or the current default arena set.
3. Choose a free fixed battle slot in the Teenyverse and paste the arena template into that slot.
4. Teleport the player into the Teenyverse if needed, then place them at the arena's authored player spawn.
5. Build the player's `BattleState` from those team `ItemStack`s.
6. Build the opposing team either from NPC team JSON or a fallback debug boss.
7. Spawn an `EntityTeenyDummy` at the arena's authored opponent spawn and attach opponent `BattleState` data to it.
8. Replace the player's normal inventory with battle controls.

Important current details:

- `BattleState` capabilities are attached to all living entities, so both players and dummy opponents use the same runtime state type.
- Arena metadata is separate from arena structure geometry: JSON defines runtime metadata and NBT defines the authored build.
- The current battle start path is still command-driven debug scaffolding, but it no longer depends on a hardcoded iron platform.
- Entering the Teenyverse causes the Titan Manager capability to save and clear the player's vanilla inventory. Leaving the Teenyverse restores it.

## Battle Controls And Inventory Replacement
During battle, `BattleState.refreshPlayerInventory` clears the player's current inventory and fills it with battle items.

Current slot usage:

- hotbar slots `0` to `2`: battle ability items for the active figure's three ability slots
- slot `4`: tofu item if tofu is currently available
- slot `5`: the equipped Titan Manager accessory in battle form
- slots `6` to `8`: bench figure icons for swapping

Current interaction model:

- left-click with an `ItemAbility` triggers melee attacks through `AttackEntityEvent`
- right-click with an `ItemAbility` triggers ranged, self, and utility abilities
- right-clicking a bench `ItemFigure` during battle swaps to that figure if valid
- some states lock the selected slot, and the server forces the player's held slot to stay aligned

The battle inventory is not the persistent figure inventory. It is a temporary control surface rebuilt from the active battle state.

## Core Tick Loop
`BattleState.tick` runs every server tick for living entities that have the battle capability.

Current tick responsibilities:

- handle delayed victory cleanup
- decrement swap cooldown
- regenerate mana
- charge the battery passively
- spawn battery collection thresholds
- tick active accessory runtime and battery drain
- tick internal cooldown maps
- tick charge-up state
- tick each `BattleFigure` cooldown array
- tick active effects
- resolve delayed projectiles

Additional event-driven runtime:

- `ModEvents.onLivingTick` detects charge completion and finishes charge-up casts
- `ModEvents.onLivingTick` also advances blue-channel abilities
- `ModEvents.onPlayerTick` syncs battle state to the client overlay every tick while battling

## Mana, Battery, Tofu, And Accessories
### Mana
- Mana is stored on `BattleState`, not on the figure item.
- Mana regenerates every tick up to `BATTLE_MANA_MAX`.
- Stun, charging, and blue-channeling stop mana regen.
- Curse reduces mana regen.
- Dance increases mana regen.

### Battery
- Battery charge is also stored on `BattleState`.
- It fills passively over time and also gains charge from mana spent on successful actions.
- A battery pickup threshold spawns as a target percentage of the mana bar.
- When current mana crosses that threshold, the battery is collected and charge is awarded.

### Accessories
- The equipped accessory comes from the Titan Manager accessory slot.
- Accessories activate only when battery charge meets the activation minimum.
- Once active, the accessory drains battery each tick and runs its own executor hooks.
- Accessory state can modify battle behavior and may refresh the player's battle inventory.

### Tofu
- Tofu is stored as temporary battle-only mana state.
- When present, it appears as a separate battle item.
- Using tofu triggers a random self or opponent effect based on the stored tofu power.

## Ability Execution Flow
`AbilityExecutor` is the main action entry point.

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

- raycast delay creates a `PendingProjectile`; mana and self effects happen on fire, and damage or opponent effects happen on projectile resolution
- remote mine casts can detonate an existing mine instead of placing or firing normally
- some traits can cancel or redirect execution through `TraitRegistry.triggerExecutionHooks`
- self effects and opponent effects are applied through `EffectApplierRegistry`
- golden bonuses are injected by parsing ability `golden_bonus` definitions at execution time

## Damage And Mitigation Pipeline
Outgoing damage is built by `DamagePipeline.calculateOutput`, then incoming mitigation is resolved by `DamagePipeline.calculateMitigation`.

Current outgoing order:

1. Read the current ability and mana cost.
2. Skip normal damage if the ability is pure utility.
3. Compute base damage from Power, mana cost, `BASE_DAMAGE_PERMANA`, and damage tier multiplier.
4. Apply trait-based outgoing modifiers such as `activate`, `charge_up`, and `surprise`.
5. Add flat damage bonuses or penalties from active battle effects.
6. Build a `DamageResult`.

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
- poison-style abilities can suppress the instant damage portion and rely on follow-up effects
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
- swapping applies a cooldown using `SWAP_COOLDOWN`
- the player's battle inventory is immediately rebuilt

Disable effects can also force a swap:

- disable is tracked per figure slot as `disable_0`, `disable_1`, and `disable_2`
- at most two disables are kept at once
- if the active figure becomes disabled, battle attempts to force-swap to the next valid alive figure

## Fainting, Round Reset, And Match End
Virtual HP is checked through `BattleState.checkFaint`.

When the active figure faints and another team member is still alive:

- the next alive figure becomes active
- active effects are cleared
- a `reset_lock` effect is applied
- both sides receive reset visuals
- the player's battle inventory is rebuilt

When no figures remain alive:

- dummy opponents mark the nearby player battle state as a win
- players mark their own state as a defeat
- a short victory timer delays cleanup

After the victory timer ends:

- battle exit routes through the arena session manager
- both participant states are cleaned up
- the client battle overlay is turned off with `PacketSyncBattleData.off()`
- the player is returned to their pre-battle position rather than a hardcoded overworld fallback
- the active arena slot is cleared so the next battle can reuse it cleanly

## Client Sync
The client overlay does not inspect server battle logic directly. It receives a synced snapshot every player tick while battling.

Current synced data includes:

- active and enemy figure ids, names, model types, and slot indices
- HP, mana, battery, cooldowns, and slot progress
- current ability ids, tiers, and golden flags
- active effect strings
- bench figure ids and HP summaries
- remote mine state flags

This sync is display-oriented. Runtime authority remains server-side in `BattleState`, `AbilityExecutor`, and the related battle registries.

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
- crit and dodge rules
- flight, shield, reflect, and defense interactions
- swap cooldown and reset-lock timings
- battery generation and accessory drain

## Open Questions
- how battle entry should evolve beyond the current command and debug flow
- how NPC or future PvP battles should choose and control opponents without relying on the current dummy-centric setup
- whether the saved-battle-state NBT should remain minimal or grow into real reconnect support
- whether more explicit battle phases are needed beyond the current state-plus-executor model

## Planned Additions
- richer accessory and chip integration
- fuller world encounter and NPC battle entry paths
- clearer AI parity documentation once the opponent control path is finalized
- more explicit extension points for future mechanics once the current battle loop stabilizes
