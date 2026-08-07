# Effects

## Purpose
Document the current battle effect system: how ability JSON resolves into effect application, how active effects are stored and ticked, what `duration`, `magnitude`, and `power` mean in practice, and which concrete combat statuses are implemented now.

## Current Status
The mod has an implemented centralized effect system built around `EffectApplierRegistry`, `EffectRegistry`, `BattleState.applyEffect`, and `EffectInstance`. Effects already drive buffs, debuffs, control states, periodic damage or healing, resource changes, mines, pets, flight, and reset logic.
Phase 1 of the battle refactor now validates effect references during reload so broken ability data fails early instead of falling through at runtime.
Phase 7 now makes `EffectApplierRegistry` the explicit gameplay-content effect-input contract: validated ability data and golden self/opponent bonuses resolve through registered applier ids, while generic fallback behavior is separated from core validated runtime paths.
The current ownership model makes persistent effects participant-owned by default. Disable is the explicit figure-scoped exception because it locks a specific team slot.

## Player-Facing Behavior
- Effects can buff, debuff, damage over time, heal over time, lock actions, alter mana flow, summon pets, place mines, or modify dodge, reflect, and flight behavior.
- Effects live in battle runtime state, not on the figure item.
- Most effects are shown to the client overlay as either a remaining duration in seconds or a magnitude value for infinite stack effects.
- Manual swaps preserve participant effects, so buffs, debuffs, control, periodic effects, pets, and mines continue against the newly active figure.
- A faint round reset clears the fainting side's participant effects and figure-scoped disables before applying `reset_lock`.
- Effects are not persistent figure progression data and are cleared when the battle ends.

## Source Of Truth
- [`src/main/java/bruhof/teenycraft/battle/effect/EffectApplierRegistry.java`](../../src/main/java/bruhof/teenycraft/battle/effect/EffectApplierRegistry.java)
- [`src/main/java/bruhof/teenycraft/battle/effect/EffectRegistry.java`](../../src/main/java/bruhof/teenycraft/battle/effect/EffectRegistry.java)
- [`src/main/java/bruhof/teenycraft/battle/effect/BattleEffect.java`](../../src/main/java/bruhof/teenycraft/battle/effect/BattleEffect.java)
- [`src/main/java/bruhof/teenycraft/battle/effect/PeriodicBattleEffect.java`](../../src/main/java/bruhof/teenycraft/battle/effect/PeriodicBattleEffect.java)
- [`src/main/java/bruhof/teenycraft/battle/effect/EffectInstance.java`](../../src/main/java/bruhof/teenycraft/battle/effect/EffectInstance.java)
- [`src/main/java/bruhof/teenycraft/battle/effect/EffectCalculator.java`](../../src/main/java/bruhof/teenycraft/battle/effect/EffectCalculator.java)
- [`src/main/java/bruhof/teenycraft/battle/validation/BattleContentValidation.java`](../../src/main/java/bruhof/teenycraft/battle/validation/BattleContentValidation.java)
- [`src/main/java/bruhof/teenycraft/capability/BattleState.java`](../../src/main/java/bruhof/teenycraft/capability/BattleState.java)
- [`src/main/resources/data/teenycraft/abilities`](../../src/main/resources/data/teenycraft/abilities)

## Runtime Pipeline
The current effect pipeline is:

1. Ability JSON names an effect id in `effects_on_self` or `effects_on_opponent`.
2. `AbilityExecutor` routes that id through `EffectApplierRegistry`.
3. The applier computes concrete numbers from the caster, mana cost, parameters, and target.
4. The target `BattleState` receives `applyEffect(...)`.
5. `EffectRegistry` supplies lifecycle hooks such as `onApply`, `onTick`, `onAttack`, and `onRemove`.

`AbilityLoader` now parses golden bonuses on reload. Golden effect bonuses such as `self:power_up:0.3` or `opponent:stun:1.2` reuse this same applier path through their parsed scope/id/param records instead of reparsing raw strings at cast time.

## Effect Storage Model
Persistent participant effects are stored in `BattleState.participantEffects`, keyed by effect id. Figure-scoped effects are stored on the targeted `BattleFigure`.

Ownership is explicit on `BattleEffect` through `EffectScope`:

- `PARTICIPANT` is the default for registered effects and for internal unregistered battle locks
- `FIGURE` is reserved for effects that truly belong to one team figure; the current examples are `disable_0`, `disable_1`, and `disable_2`
- instant effects are executed against their resolved target but are not stored
- source attribution is independent of target ownership, so a participant-owned periodic effect can still remember the original caster figure

Participant effects tick once per participant and use the current active figure as their target for lifecycle hooks. Figure-scoped disables keep ticking on their targeted slots while those figures are benched.

## EffectInstance Fields
Each active effect is stored as an `EffectInstance`.

Current fields:

- `duration`: remaining ticks
- `initialDuration`: original duration at creation
- `magnitude`: the primary integer payload
- `tickCounter`: custom elapsed-tick counter used by some effects
- `power`: secondary float payload used by effects that need more than one number
- `casterUUID`: attacker context for follow-up logic such as periodic damage ownership

These fields do not all mean the same thing for every effect.

Common current meanings:

- `magnitude` as stack strength for `power_up` and `power_down`
- `magnitude` as percent for defense, luck, freeze mana burn, or cuteness
- `magnitude` as interval for periodic effects such as poison, shock, and radios
- `magnitude` as charges for `dodge_smoke`
- `magnitude` as blocked slot index for `waffle`
- `magnitude` as mine stage count for `remote_mine_*`
- `power` as periodic payload for poison, radio effects, reflect, flight, and remote mine

## Effect Types And Categories
`BattleEffect` defines three runtime types:

- `INSTANT`
- `DURATION`
- `INFINITE`

It also defines four categories:

- `BUFF`
- `DEBUFF`
- `CONTROL`
- `SPECIAL`

Categories are not cosmetic. They are used by cleanse, kiss, and dispel logic:

- `cleanse` removes `DEBUFF` and `CONTROL`
- `kiss` removes `BUFF` when applied and blocks many future positive effects
- `dispel` removes `BUFF`

## Apply, Refresh, And Remove Rules
`BattleState.applyEffect` uses the following rules:

- `onApply` runs first and may cancel the effect entirely
- the effect's declared scope chooses the participant map or the explicitly targeted figure map
- if the effect already exists and `canStackMagnitude()` is true, new magnitude is added
- otherwise, reapplying the effect overwrites `magnitude`
- `power` keeps the larger of the old and new values
- `casterUUID` is replaced by the newest application
- duration `-1` means infinite
- otherwise, reapplication only extends the duration if the new duration is longer

Tick and removal rules:

- positive durations count down every tick
- infinite effects do not tick down automatically
- when duration reaches `0`, `removeEffect` runs and then `onRemove` executes
- `triggerOnAttack` removes any active effect whose `onAttack` hook returns `true`

Current examples of on-attack consumption:

- `power_up`
- `power_down`

## Periodic Effect Model
`PeriodicBattleEffect` is the shared base for repeating effects.

Current behavior:

- periodic effects are still `DURATION` effects
- the effect's `magnitude` field is treated as the interval in ticks
- `onPeriodicTick` fires when `remainingDuration % interval == 0`
- `getSmartSplitValue` can evenly split a stored total value across the remaining pulses by consuming from `power`
- delayed effects that carry a caster now also persist the original source figure identity needed for later combat-source hooks
- chip-applied runtime effects now preserve that same source-figure attribution when they are authored through chip hook actions instead of normal ability JSON
- periodic participant effects act on the active figure at each pulse, so swapping changes the current poison, shock, or radio target without duplicating or resetting its timer

Implemented periodic effects:

- `health_radio`
- `power_radio`
- `poison`
- `shock`

Related special case:

- `remote_mine_*` is not a `PeriodicBattleEffect`, but it does use `tickCounter` to grow through mine stages over time

## Application Patterns And Common Gates
Most effect math is centralized in `EffectCalculator`, and most runtime application is centralized in `EffectApplierRegistry`.

Common current gating rules:

- most positive buffs are blocked if the target has `kiss`
- most hostile debuffs and control effects are blocked if the target has `cleanse_immunity`
- `reflect` is a notable exception and does not use the same buff-block gate as most positive effects

Common current composite patterns:

- `cleanse` removes debuffs and control, then applies `cleanse_immunity`
- `kiss` removes existing buffs, then blocks many future positive effects
- `freeze` immediately burns mana, then applies `freeze_movement`
- `reflect` applies a reflect state and also applies self `stun` and `freeze_movement`
- `disable` targets the current active figure slot and converts into `disable_0`, `disable_1`, or `disable_2`
- `remote_mine` converts into `remote_mine_0`, `remote_mine_1`, or `remote_mine_2`; the mine belongs to the target participant and detonates against that participant's current active figure
- `pets` chooses `pet_slot_1` or `pet_slot_2` rather than storing a generic `pets` effect id

## Implemented Effect Glossary
### Direct HP And Resource Effects
- `heal`: instant HP restore on the target.
- `group_heal`: instant heal split across alive team members.
- `bar_fill`: instant mana restore.
- `bar_deplete`: instant mana drain.
- `self_shock`: instant self-damage based on max HP and mana cost.
- `self_damage`: recoil sent through the normal mitigation pipeline.
- `tofu_spawn`: spawns a temporary tofu resource in battle state.

### Buff And Support Effects
- `power_up`: infinite stackable flat damage bonus, consumed on attack.
- `defense_up`: duration-based incoming damage reduction percent.
- `luck_up`: duration-based Luck bonus that improves crits and effect scaling.
- `speed_up`: duration-based movement-speed bonus used by dodge-reactive chip effects.
- `dance`: duration-based mana regen multiplier.
- `cuteness`: duration-based reflect percentage.
- `shield`: negates the next hit unless the hit is undodgeable and shield-piercing logic still leaves damage.
- `dodge_smoke`: duration-based dodge support with limited charges.
- `health_radio`: periodic healing over time.
- `power_radio`: periodic power-up generation over time.
- `pet_slot_1` and `pet_slot_2`: internal buff slots for summoned pet attacks. Each active pet fires follow-up damage when its owning figure lands a damaging attack, subject to the shared pet-fire cooldown.
- `flight`: duration-based evasion state with vertical launch, drag, apex handling, and gravity restoration on removal.

### Debuff And Control Effects
- `power_down`: infinite stackable flat damage penalty, consumed on attack.
- `defense_down`: duration-based increased incoming damage percent.
- `root`: participant-owned duration-based swap lock.
- `speed_down`: duration-based movement-speed penalty debuff, currently used by `Second Chance` rescue swaps.
- `disable_0`, `disable_1`, `disable_2`: figure-scoped duration-based slot locks. Cleanse and faint reset can remove disables across the affected team.
- `stun`: duration-based action lock.
- direct `stun` applications can now have their duration reduced by chip-authored direct-stun resistance, but `shock` still applies its periodic mini-stuns at full authored duration because it bypasses the direct `stun` applier path.
- `freeze`: instant mana burn plus application of `freeze_movement`.
- `freeze_movement`: duration-based movement freeze helper effect.
- `poison`: periodic damage-over-time effect.
- `shock`: periodic mini-stun control effect.
- `curse`: duration-based reduced mana regeneration.
- `waffle`: duration-based random blocked ability slot.
- `kiss`: duration-based positive-effect blocker that also strips buffs on apply.
- `remote_mine_0`, `remote_mine_1`, `remote_mine_2`: infinite participant-owned staged mines tied to an ability slot and caster.

### Special And Internal Effects
- `cleanse`: instant purge of debuffs and control plus application of `cleanse_immunity`.
- `cleanse_immunity`: duration-based immunity to many debuffs, control effects, and dispels.
- `dispel`: instant removal of buffs.
- `reflect`: special defensive state that reduces incoming damage, reflects damage back, and locks the target through extra self-control effects.
- `reset_lock`: short-lived special effect used during faint and round reset flow.

## Applier-Only Helper Ids
Not every effect id used in ability data becomes a persistent `activeEffects` entry with the same name.

Current helper ids include:

- `self:heal`
- `self:power_up`
- `eagle`
- `self:eagle`
- `waffle_chance`
- `pets`
- `disable`
- `remote_mine`

These are routing helpers in `EffectApplierRegistry`. They often translate into one or more real stored effect ids.
They are part of the validated gameplay-content input contract even when they are not one-to-one `EffectRegistry` entries.
By contrast, `EffectRegistry` ids such as stored internal statuses are runtime state ids, not generic ability-data inputs unless an applier explicitly exposes them.
`EffectApplierRegistry.getOrFallback(...)` still exists for non-validated/debug contexts, but validated gameplay execution now uses strict registered lookups.

## Client Display
`BattleState.getEffectList` currently exposes:

- infinite effects as `effect_id (Mag: value)`
- other effects as `effect_id (Xs)`

The client overlay therefore shows duration cleanly, but not every effect's hidden `power` payload or custom meaning of `magnitude`.

## Balance Hooks
- effect duration formulas
- effect magnitude formulas
- power-based and luck-based scaling
- periodic interval formulas
- shield, reflect, dodge-smoke, and freeze math
- pet and remote-mine tuning
- cleanse, kiss, and control interaction timing
- flight movement behavior
- reset-lock timing

## Design Notes
- New effect logic should prefer the existing registry model.
- Shared effect concepts should become reusable registry entries rather than custom branches per ability.
- Effect docs need to distinguish applier ids from stored effect ids.
- Lockout-style effects must stay explicit about what they block: movement, casting, swapping, buffs, or incoming damage handling.
- New persistent effects should remain participant-scoped unless their mechanic explicitly targets one team slot like Disable.
- Target ownership and caster attribution are separate concerns and should not be coupled when adding delayed effects.

## Open Questions
- whether the current `magnitude` and `power` overloading should be formalized into stronger typed payloads
- whether instant helper effects such as `cleanse` should keep their current registry shape or be split into clearer runtime-only helpers
- whether effect categories need stronger validation beyond the current enum

## Planned Additions
- add JSON examples for common effect parameter patterns
- document the exact UI wording and icon treatment once battle presentation is more stable
