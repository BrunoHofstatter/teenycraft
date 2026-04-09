# Attributes

## Purpose
Document the four core figure stats, how they are initialized and persisted, how battle uses them, and how the shuffle bag system makes dodge and crit behavior semi-deterministic instead of pure random rolling.

## Current Status
The stat system is implemented and actively used by battle. HP, Power, Dodge, and Luck are stored on the figure item, copied into `BattleFigure` at battle start, and then read by damage, crit, dodge, movement, and effect calculations.

## Player-Facing Behavior
- Every figure has four persistent stats: HP, Power, Dodge, and Luck.
- HP increases max health.
- Power increases ability damage and also scales many power-based support effects such as healing.
- Luck affects both critical hits and many effect calculations.
- Dodge affects movement speed in battle and the dodge mitigation system.
- Dodge does not fully avoid damage by itself. A successful dodge reduces incoming damage by the figure's dodge stat.

## Source Of Truth
- [`src/main/java/bruhof/teenycraft/item/custom/ItemFigure.java`](../../src/main/java/bruhof/teenycraft/item/custom/ItemFigure.java)
- [`src/main/java/bruhof/teenycraft/battle/BattleFigure.java`](../../src/main/java/bruhof/teenycraft/battle/BattleFigure.java)
- [`src/main/java/bruhof/teenycraft/battle/damage/DamagePipeline.java`](../../src/main/java/bruhof/teenycraft/battle/damage/DamagePipeline.java)
- [`src/main/java/bruhof/teenycraft/battle/effect/EffectCalculator.java`](../../src/main/java/bruhof/teenycraft/battle/effect/EffectCalculator.java)
- [`src/main/java/bruhof/teenycraft/capability/BattleState.java`](../../src/main/java/bruhof/teenycraft/capability/BattleState.java)
- [`src/main/java/bruhof/teenycraft/util/ShuffleBag.java`](../../src/main/java/bruhof/teenycraft/util/ShuffleBag.java)
- [`src/main/java/bruhof/teenycraft/TeenyBalance.java`](../../src/main/java/bruhof/teenycraft/TeenyBalance.java)

## Persistent Stats
The four core stats are stored on the figure item under the nested `Stats` tag:

- `Health`
- `Power`
- `Dodge`
- `Luck`

These values persist with the figure item and are what upgrades mutate. When battle starts, `BattleFigure` snapshots those values into its runtime state.

## Base Stat Initialization
Fresh figure items are initialized from figure JSON attribute scales:

- `hp_scale`
- `power_scale`
- `dodge_scale`
- `luck_scale`

Those scales are converted into starting stats with the progression step constants in `TeenyBalance`:

- one HP step is `UPGRADE_GAIN_HP`
- one Power step is `UPGRADE_GAIN_POWER`
- one Dodge step is `UPGRADE_GAIN_DODGE`
- one Luck step is `UPGRADE_GAIN_LUCK`

## What Each Stat Does
### HP
- HP sets the figure's base max health in battle.
- `BattleFigure` starts `currentHp` at that max value.
- Accessories can temporarily add extra max HP during battle, but base HP still comes from the figure item stat.

### Power
- Power is the main outgoing damage stat.
- Standard ability damage is based on `power * manaCost * BASE_DAMAGE_PERMANA * damageTierMultiplier`.
- Power also scales many support and effect magnitudes through `EffectCalculator`, including healing and power-based buffs or debuffs.
- Flat bonus or penalty effects are added separately on top of base Power calculations.

### Luck
- Luck affects crit chance through the crit shuffle bag.
- Luck also affects crit damage through the crit multiplier in `DamagePipeline`.
- Luck contributes small bonuses to many effect formulas, usually by slightly increasing duration, magnitude, efficiency, or interval scaling depending on the effect.
- Temporary luck modifiers such as `luck_up` are applied through `BattleFigure.getEffectiveStat`.

### Dodge
- Dodge increases battle movement speed through a transient player speed modifier in `BattleState.updatePlayerSpeed`.
- Dodge affects the dodge shuffle bag and therefore the chance to trigger dodge mitigation.
- On a successful dodge roll, incoming damage is reduced by the current dodge stat instead of being fully ignored.
- Some effects can improve dodge mitigation further by shrinking the virtual bag or multiplying the damage reduction.

## Shuffle Bag System
Luck crits and dodge checks are not rolled as independent pure-random percentages on every hit.

The current runtime model uses a `ShuffleBag`:

- each bag contains exactly one success entry
- the total bag size depends on the stat value
- draws consume entries from the current bag
- when the bag is empty, it is reshuffled into a fresh bag

Current bag size thresholds from `TeenyBalance.getBagSize`:

- stat `0` to `19`: 1 success in a bag of 10
- stat `20` to `34`: 1 success in a bag of 9
- stat `35` to `54`: 1 success in a bag of 8
- stat `55` to `79`: 1 success in a bag of 7
- stat `80+`: 1 success in a bag of 6

This means a figure cannot crit or dodge forever on an unlimited lucky streak, and also cannot go forever without eventually seeing the success card once the current bag is exhausted.

Some effects do not rebuild the physical bag. Instead, battle uses a virtual smaller or larger bag for that check to simulate modified odds.

## Effective Stats During Battle
Not every battle formula reads only the base item stat.

`BattleFigure.getEffectiveStat` currently exposes battle-time derived values for:

- Luck after `luck_up`
- flat damage after `power_up`, `power_down`, and related additive effects
- defense percent after `defense_up` and `defense_down`
- reflect percent after reflect-style effects
- pet flat damage contribution

Power and Dodge mostly remain base snapshot stats in the current engine, with extra additive behavior handled through separate effect magnitudes rather than rewriting the base stat itself.

## Balance Hooks
- `UPGRADE_GAIN_HP`
- `UPGRADE_GAIN_POWER`
- `UPGRADE_GAIN_DODGE`
- `UPGRADE_GAIN_LUCK`
- `BASE_DAMAGE_PERMANA`
- `SPEED_PER_DODGE`
- `LUCK_BALANCE_MULTIPLIER`
- `BASE_LUCK_MULTIPLIER`
- shuffle bag checkpoints and sizes

## Design Notes
- The four persistent stats should stay item-backed so figures remain portable through Titan Manager storage and NPC figure building.
- Battle-only modifications should continue to layer on top of the snapshot through effects rather than rewriting the underlying item stats every tick.
- Dodge should be documented as mitigation, not full evasion, because that is the implemented runtime behavior.
- Luck should be documented as both a crit stat and a secondary effect-scaling stat.

## Open Questions
- whether future progression should expose the shuffle bag math directly in UI or keep it hidden
- whether more effects should modify effective Power or Dodge directly instead of only adding derived bonuses
- whether non-battle systems will ever consume class or groups together with the stat profile

## Planned Additions
- add player-facing stat UI references once inspection or upgrade screens expose these formulas clearly
- document upgrade choice rules once the level-up flow is implemented in a normal gameplay path
