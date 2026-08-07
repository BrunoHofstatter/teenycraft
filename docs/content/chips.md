# Chips

## Purpose
Document the implemented chip customization layer: how chips are stored on figures, which runtime hook types exist, what current starter chips do, and which parts of fusion or UI are still pending.

## Current Status
- Partially implemented.
- Chips now exist as real items that install onto a figure item and are serialized into that figure's NBT.
- Battle snapshots already read installed chips and can apply chip-driven stat changes or runtime hooks.
- The dedicated figure screen is now the player-facing install flow.
- A first-pass Chip Fuser block, menu, and screen now exist for duplicate same-rank fusion.
- Chips now support hybrid composition, so one chip can combine multiple scaled behaviors and multiple actions on the same hook.
- Curated/special fusion recipes are still only partially populated.

## Player-Facing Behavior
- A figure can hold one installed chip at a time.
- Installing a new chip destroys the previously installed chip.
- Installed chips currently persist on the figure item itself.
- Chips are installed by opening the figure screen, placing a chip in the preview slot, and clicking `INSTALL`.
- Loose chip items can be stored in the Titan Manager `Chips` tab.
- Chip effects can modify battle stats, trigger the first time that figure becomes active in a battle, trigger when that figure faints, trigger when that figure defeats an opponent, or react to critical hits.
- Chip effects can also react when that figure dodges.
- Chip effects can also react when that figure takes direct runtime hit damage.
- Self-targeted chip hooks resolve on the figure that actually earned the hook, even if delayed damage later resolves after that participant has swapped to a different active figure.
- Chip values are fixed by rank and come from `TeenyBalance`, not from the normal ability-effect scaling formulas.
- The current Chip Fuser accepts only chips in its two input slots, shows an output preview, and uses a Fuse button to finalize the fusion.
- The current fuser supports both duplicate same-rank upgrades and curated special fusions that combine two different same-rank chips into a hybrid chip of that same rank.
- The figure screen now uses an explicit install confirmation instead of immediate install-on-click, but replacing a chip still destroys the old equipped chip.

## Source Of Truth
- [`src/main/java/bruhof/teenycraft/chip/ChipSpec.java`](../../src/main/java/bruhof/teenycraft/chip/ChipSpec.java)
- [`src/main/java/bruhof/teenycraft/chip/ChipRegistry.java`](../../src/main/java/bruhof/teenycraft/chip/ChipRegistry.java)
- [`src/main/java/bruhof/teenycraft/chip/ChipExecutor.java`](../../src/main/java/bruhof/teenycraft/chip/ChipExecutor.java)
- [`src/main/java/bruhof/teenycraft/chip/ChipFusionRegistry.java`](../../src/main/java/bruhof/teenycraft/chip/ChipFusionRegistry.java)
- [`src/main/java/bruhof/teenycraft/block/custom/ChipFuserBlock.java`](../../src/main/java/bruhof/teenycraft/block/custom/ChipFuserBlock.java)
- [`src/main/java/bruhof/teenycraft/block/entity/ChipFuserBlockEntity.java`](../../src/main/java/bruhof/teenycraft/block/entity/ChipFuserBlockEntity.java)
- [`src/main/java/bruhof/teenycraft/screen/ChipFuserMenu.java`](../../src/main/java/bruhof/teenycraft/screen/ChipFuserMenu.java)
- [`src/main/java/bruhof/teenycraft/screen/ChipFuserScreen.java`](../../src/main/java/bruhof/teenycraft/screen/ChipFuserScreen.java)
- [`src/main/java/bruhof/teenycraft/item/custom/ItemChip.java`](../../src/main/java/bruhof/teenycraft/item/custom/ItemChip.java)
- [`src/main/java/bruhof/teenycraft/item/custom/ItemFigure.java`](../../src/main/java/bruhof/teenycraft/item/custom/ItemFigure.java)
- [`src/main/java/bruhof/teenycraft/battle/BattleFigure.java`](../../src/main/java/bruhof/teenycraft/battle/BattleFigure.java)
- [`src/main/java/bruhof/teenycraft/capability/BattleState.java`](../../src/main/java/bruhof/teenycraft/capability/BattleState.java)
- [`src/main/java/bruhof/teenycraft/battle/AbilityExecutor.java`](../../src/main/java/bruhof/teenycraft/battle/AbilityExecutor.java)
- [`src/main/java/bruhof/teenycraft/battle/trait/TraitRegistry.java`](../../src/main/java/bruhof/teenycraft/battle/trait/TraitRegistry.java)
- [`src/main/java/bruhof/teenycraft/util/NPCFigureBuilder.java`](../../src/main/java/bruhof/teenycraft/util/NPCFigureBuilder.java)
- [`src/main/java/bruhof/teenycraft/util/NPCTeamLoader.java`](../../src/main/java/bruhof/teenycraft/util/NPCTeamLoader.java)
- [`src/main/java/bruhof/teenycraft/TeenyBalance.java`](../../src/main/java/bruhof/teenycraft/TeenyBalance.java)

## Design Notes
- Chips are figure-bound, not participant-bound.
- Titan Manager chip storage is only for loose chip items; installed chips still live inside the figure item NBT.
- Runtime hook points are centralized instead of scattering chip-id checks across battle code.
- Stat modifiers are resolved into the hot `BattleFigure` snapshot at battle start.
- Chip-applied self buffs and summons should stay attached to the credited source figure, not just whichever figure is currently active when the hook finishes resolving.
- Percentage stat modifiers are always calculated from the figure's base item stat, not from other chip multipliers or chip-added flat bonuses.
- Chips reuse battle systems like effect application, pet slots, mana, and battery when those already fit the intended behavior.
- Hybrid chips are built by inheriting and scaling other chip behaviors, so every numeric part of the source chip behavior is reduced together unless explicitly authored differently later.
- Chips can also author lightweight scalar modifiers for battle systems that already exist, such as tofu spawn chance, direct-stun duration reduction, active-only regeneration, low-health finisher damage, healing output for authored healing abilities, and bench-heal splash on self-heal.
- The current fuser supports:
  - exact duplicate fusion: same chip item plus same rank produces the next rank when that chip has a higher rank available
  - curated special fusion: two different chips of the same rank produce a named hybrid chip of that same rank when a recipe exists
- Fusion cost tuning is currently authored per chip and per rank in `TeenyBalance`, with separate arrays for normal duplicate upgrades and recipe-specific special fusions.

## Current Hook Types
- Snapshot stat modifiers
- First appearance in battle
- Critical-hit reactions
- Dodge reactions
- On-damaged reactions
- Extra charge-up instant-cast roll
- On faint
- On kill

## Current Implemented Chips
- `Tough Guy`: increases Power and lowers max HP.
- `Smokescreen`: increases Dodge.
- `Power`: increases Power.
- `Health`: increases max HP.
- `Luck`: increases Luck.
- `Ninja Skills`: increases Dodge and lowers max HP.
- `Loaded Dice`: increases Luck and lowers Power.
- `Tough Stuff`: increases max HP and sets Dodge to `0`.
- `Healthy Duck`: heals the owner when a crit lands. Runtime id and existing item registration still use `lucky_hearts`.
- `Mana Steal Duck`: steals mana from the opponent when a crit lands and grants the same amount to the owner.
- `Lucky Steal Duck`: hybrid chip that combines scaled `Luck` and `Mana Steal Duck` behavior.
- `Healthy Dodge`: heals the owner when the chipped figure dodges.
- `Dodgy Healthy Dodge`: hybrid chip that combines scaled `Smokescreen` and `Healthy Dodge` behavior.
- `Second Chance`: once per battle, when the chipped figure would faint, it instead survives at authored HP, clears its side's participant effects and disables like a normal faint reset, and if another ally can swap in, that next active ally is forced in while the participant receives `speed_down` plus `curse`.
- `Speedy Dodge`: grants a short `speed_up` buff when the chipped figure dodges.
- `Dodgy Speedy Dodge`: hybrid chip that combines scaled `Smokescreen` and `Speedy Dodge` behavior.
- `Healthy Second Chance`: special `Health` hybrid with custom authored behavior, not inherited `Health` stats; it uses the same once-per-battle rescue flow as `Second Chance` but survives at more HP and applies stronger swap-in debuffs.
- `Pointy Health`: hybrid chip that combines scaled `Health` and `Pointy` behavior.
- `Lucky Healthy Duck`: hybrid chip that combines scaled `Luck` and `Healthy Duck` behavior.
- `Insta Cast Chance`: adds an extra charge-up instant-cast roll that stacks separately from the existing trait-based roll.
- `Fast Cast`: reduces `charge_up` delay by a chip-authored speed bonus.
- `Fast Draw`: reduces delayed projectile travel time by a chip-authored speed bonus.
- `Fast DrawCast`: hybrid chip that combines scaled `Fast Draw` and `Fast Cast` behavior.
- `Clean Entry`: applies `cleanse` the first time the figure becomes active in a battle.
- `Beastly Entry`: applies `power_up` the first time the figure becomes active in a battle.
- `Curse Entry`: applies `curse` to the opposing active figure the first time the chipped figure becomes active.
- `Dance`: applies fixed-duration `dance` the first time the figure becomes active in a battle.
- `Mana Boost`: grants fixed mana the first time the figure becomes active in a battle.
- `Momentum`: grants mana when the chipped figure gets a kill.
- `Finishing Momentum`: hybrid chip that combines scaled `Finisher` and `Momentum` behavior.
- `Victory Dance`: applies `dance` when the chipped figure gets a kill.
- `Powered Beastly Entry`: hybrid chip that combines scaled `Power` and `Beastly Entry` behavior.
- `Powered Finisher`: hybrid chip that combines scaled `Power` and `Finisher` behavior.
- `Energetic Battery`: increases passive battery charge per tick while the chipped figure is active.
- `Death Energy`: grants accessory battery charge when the chipped figure faints.
- `Self Explosion`: deals fixed group damage to the opposing team when the chipped figure faints.
- `Last Laugh`: on faint, applies one equal-weight random opposing effect from a curated list of `curse`, `shock`, `poison`, `freeze`, and `waffle`.
- `Necromancer`: summons a fixed-strength pet when the chipped figure gets a kill.
- `Vampire`: heals for a percentage of the owner's max HP when the chipped figure gets a kill.
- `Regenerative Cuteness`: while active, regenerates fixed HP at a fixed interval.
- `Tofu Lover`: increases tofu spawn chance from damaging ability use while the chipped figure is active.
- `Lucky Tofu Lover`: hybrid chip that combines scaled `Luck` and `Tofu Lover` behavior.
- `Hello Nurse`: increases healing output for authored healing abilities used by the chipped figure.
- `Team Medic`: when the chipped figure heals itself, bench allies receive a smaller non-recursive splash heal.
- `Pointy`: when the chipped figure takes direct hit damage, the attacker takes fixed retaliation damage once for that reaction sequence.
- `Stun Resister`: reduces duration from direct `stun` applications only; it does not reduce `shock`'s periodic mini-stuns.
- `Finisher`: increases damage dealt to low-health enemies, evaluated separately for each hit as the target's HP changes.

## NPC Support
- NPC team JSON can now set `chip_id` and `chip_rank` per figure.
- NPC chips use the same installed-chip path as player figures.

## Open Questions
- broader curated special-fusion recipe library beyond the current starter example
- real coin cost tuning now that the system supports per-chip and per-recipe cost arrays
- how many special fusion-only chips should exist versus straightforward rank upgrades
- whether some future chip effects need per-figure active-only cleanup beyond the current starter set

## Planned Additions
- curated special-fusion recipes
- acquisition loop and economy hooks
- richer passive hook coverage such as retaliation or on-damaged chips
