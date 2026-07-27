# Accessories

## Purpose
Document how the Titan Manager accessory slot feeds battle and where accessory behavior is implemented.

## Current Status
Accessories are implemented as real items that equip into the Titan Manager accessory slot and activate during battle through the shared battery system.

## Player-Facing Behavior
- One accessory can be equipped in the Titan Manager accessory slot.
- Loose accessory items can be stored in the Titan Manager `Accessories` tab.
- The accessory slot is optional; battles still run normally when no accessory is equipped.
- During battle, the equipped accessory is copied into hotbar slot `5` as an activation button.
- Accessories do not auto-activate anymore.
- The player can activate the equipped accessory once battery charge reaches at least `50`.
- Once active, the accessory stays active until battery drains to `0`, the equipped accessory is removed, or battle ends.
- Active accessories drain the existing battle battery bar continuously instead of using a separate resource.
- The equipped accessory uses the activating player's unlocked mastery tier for the full activation.
- Periodic accessories apply their first pulse immediately, then wait their resolved interval between later pulses. Bat Signal remains a delayed one-shot accessory.

## Current Implemented Content
- Titan's Coin
- Mother Box
- Bat Signal
- Red Lantern Battery
- Green Lantern Battery
- Violet Lantern Battery
- Raven's Spellbook
- Cyborg's Waffle Shooter
- Lil' Penguin
- Kryptonite
- Justice League Coin
- Birdarang
- Superman's Underpants
- Krypto the Superdog

## Source Of Truth
- [`src/main/java/bruhof/teenycraft/accessory/AccessorySpec.java`](../../src/main/java/bruhof/teenycraft/accessory/AccessorySpec.java)
- [`src/main/java/bruhof/teenycraft/accessory/AccessoryRegistry.java`](../../src/main/java/bruhof/teenycraft/accessory/AccessoryRegistry.java)
- [`src/main/java/bruhof/teenycraft/accessory/AccessoryExecutor.java`](../../src/main/java/bruhof/teenycraft/accessory/AccessoryExecutor.java)
- [`src/main/java/bruhof/teenycraft/accessory/AccessoryProgression.java`](../../src/main/java/bruhof/teenycraft/accessory/AccessoryProgression.java)
- [`src/main/java/bruhof/teenycraft/accessory/AccessoryTierResolver.java`](../../src/main/java/bruhof/teenycraft/accessory/AccessoryTierResolver.java)
- [`src/main/java/bruhof/teenycraft/accessory/ResolvedAccessorySpec.java`](../../src/main/java/bruhof/teenycraft/accessory/ResolvedAccessorySpec.java)
- [`src/main/java/bruhof/teenycraft/accessory/AccessoryMasteryService.java`](../../src/main/java/bruhof/teenycraft/accessory/AccessoryMasteryService.java)
- [`src/main/java/bruhof/teenycraft/accessory/AccessoryMilestoneRegistry.java`](../../src/main/java/bruhof/teenycraft/accessory/AccessoryMilestoneRegistry.java)
- [`src/main/java/bruhof/teenycraft/accessory/AccessoryMilestoneService.java`](../../src/main/java/bruhof/teenycraft/accessory/AccessoryMilestoneService.java)
- [`src/main/java/bruhof/teenycraft/accessory/AccessoryBattleProgressTracker.java`](../../src/main/java/bruhof/teenycraft/accessory/AccessoryBattleProgressTracker.java)
- [`src/main/java/bruhof/teenycraft/accessory/AccessoryPresentation.java`](../../src/main/java/bruhof/teenycraft/accessory/AccessoryPresentation.java)
- [`src/main/java/bruhof/teenycraft/capability/BattleState.java`](../../src/main/java/bruhof/teenycraft/capability/BattleState.java)
- [`src/main/java/bruhof/teenycraft/capability/AccessoryMastery.java`](../../src/main/java/bruhof/teenycraft/capability/AccessoryMastery.java)
- [`src/main/java/bruhof/teenycraft/capability/TitanManager.java`](../../src/main/java/bruhof/teenycraft/capability/TitanManager.java)
- [`src/main/java/bruhof/teenycraft/item/custom/ItemAccessory.java`](../../src/main/java/bruhof/teenycraft/item/custom/ItemAccessory.java)
- [`src/main/java/bruhof/teenycraft/screen/AccessoryScreenMenu.java`](../../src/main/java/bruhof/teenycraft/screen/AccessoryScreenMenu.java)
- [`src/main/java/bruhof/teenycraft/screen/AccessoryScreen.java`](../../src/main/java/bruhof/teenycraft/screen/AccessoryScreen.java)
- [`src/main/java/bruhof/teenycraft/TeenyBalance.java`](../../src/main/java/bruhof/teenycraft/TeenyBalance.java)

## Design Notes
- Accessories are code-defined for now because the difficult part is runtime hook behavior, not content loading.
- Accessories reuse existing effect ids like `heal`, `bar_fill`, `power_up`, `curse`, and `freeze` directly where possible.
- Accessories bypass the ability scaling layer and apply fixed behavior through accessory-specific runtime code.
- Numeric tuning remains centralized in `TeenyBalance.java`.

## Planned Additions
- more accessory content beyond the current starter set
- reactive accessory hooks for on-hit, on-damaged, and retaliation behavior
- optional HUD feedback for active accessory state

## Final Planned Design: Accessory Tier Progression System
This section records the final design decision for accessory progression.

The player-bound persistence foundation, client sync, tier-purchase transaction, coin-cost curve, debug controls, shared Tier 2 through Tier 4 runtime scaling, semantic contribution tracking, all currently designed Tier 2 through Tier 5 milestone definitions, and the first accessory progression screen are implemented.

Tier 5 mastery effects are implemented for Titan's Coin, Mother Box, Bat Signal, Red Lantern Battery, Green Lantern Battery, Violet Lantern Battery, Raven's Spellbook, Cyborg's Waffle Shooter, Kryptonite, Birdarang, Superman's Underpants, and Krypto the Superdog. Their Tier 5 purchases are enabled per accessory when a Tier 5 milestone is registered. Green Lantern Battery, Raven's Spellbook, Kryptonite, and Krypto still have blank Tier 5 milestone designs, so they can be debug-tested at Tier 5 but cannot yet be unlocked normally.

Detailed per-accessory milestone planning lives in [accessory-milestones.md](accessory-milestones.md).

### Overview
Accessories use a player-bound tier progression system.

- Accessories are not upgraded through chip-style fusion.
- Accessories do not gain generic battle XP or level through normal battle participation.
- Each accessory is mastered through accessory-specific battle milestones tied to how that accessory is used.
- Completing a milestone makes the next tier available for purchase; it does not upgrade the accessory automatically.
- Tier purchases use a small Teeny Coin cost so each unlock still has a deliberate player-confirmed moment.
- Accessories remain team-wide battle items powered by the shared Battle Battery.
- Progression improves how an accessory performs when equipped and activated without changing its core role or making it harder to use.

### Storage Model
Accessory tier progress is stored on the player, not on the accessory item.

- The item determines which accessory is equipped.
- The player's progression determines which tier that accessory functions at.
- Progress is stored per player, per world or server.

Example:

- Player A has Bat Signal Tier 4 unlocked.
- Player B has Bat Signal Tier 1 unlocked.
- If Player A gives a Bat Signal item to Player B, it functions as Tier 1 for Player B.
- If Player A later obtains another Bat Signal, it still functions as Tier 4 for Player A.

This keeps mastery tied to player familiarity instead of the permanent state of a specific item, which fits Minecraft item loss, trading, storage, and replacement.

### Tier Structure
Each accessory has five tiers:

| Tier | Unlock Method | Upgrade Type |
| --- | --- | --- |
| Tier 1 | Default | Base accessory effect |
| Tier 2 | First milestone | General upgrade |
| Tier 3 | Second milestone | General upgrade |
| Tier 4 | Third milestone | General upgrade |
| Tier 5 | Final milestone | Unique mastery upgrade |

Tier purchase costs are:

| Purchased tier | Teeny Coin cost |
| --- | ---: |
| Tier 2 | `250` |
| Tier 3 | `500` |
| Tier 4 | `1000` |
| Tier 5 | `2000` |

The purchase button becomes available only after the milestone for that tier is complete. Purchasing advances the tier and begins tracking the next milestone.

- Tier 1 is always available once the player has access to the accessory.
- Tiers 2 through 4 improve general performance, such as stronger effects, better battery efficiency, longer duration, faster triggers, or improved reliability.
- Tier 5 is the mastery tier. It should be harder to unlock, accessory-specific, and grant a unique bonus instead of only better numbers.

### Milestone Rules
Each tier above Tier 1 requires exactly one milestone.

- Milestones are accessory-specific and should reflect the accessory's intended role.
- The Tier 2 milestone should usually be a simple activation milestone that teaches the progression loop.
- Later milestones should become more specific and more demanding.

Suggested curve:

| Unlock | Milestone Style |
| --- | --- |
| Tier 2 | Basic usage |
| Tier 3 | Main function usage |
| Tier 4 | Skilled or situational usage |
| Tier 5 | Difficult thematic mastery challenge |

Milestones can use either lifetime progress or one-battle challenges.

- Lifetime milestones fit steady accumulation such as total damage, total healing, total debuffs applied, or total activations.
- One-battle milestones fit higher tiers because they test timing, planning, and correct use.

Example Tier 2 milestone pattern:

- Activate this accessory 3 times.

Example flavored wording:

- Call the Bat Signal 3 times.
- Open the Mother Box 3 times.
- Call Krypto 3 times.
- Unleash the Red Lantern Battery 3 times.

### Progress Requirements
- Progress only counts while the accessory is equipped.
- Progress does not count from passive ownership, unrelated actions, or accessories sitting in storage.
- Milestones should usually reward correct usage rather than requiring battle wins.
- A specific final mastery challenge may require a win if it fits that accessory.
- Progress should be visible to the player.
- A milestone phrased as completing a condition in a number of battles can gain at most one completion per battle.
- Losing counts unless the milestone explicitly requires a win.
- Abandoning a battle never counts a battle-completion milestone.
- Valid progression is intended to come from non-debug, non-test PvE battles. Full enforcement can be added when normal NPC battle entry is distinguishable from command-started debug battles.

### Implemented Milestone Foundation
Accessory runtime events now report semantic contributions instead of writing milestone NBT directly. Supported contribution categories include activation, pulse, damage hits, actual damage, defeated figures, max-HP granted, Power Up granted, actual mana granted, actual healing, effects successfully applied, waffles, mana burned, retaliation hits, blocked damage, fatal hits blocked, and Krypto outcomes.

`BattleState` keeps separate activation and battle snapshots. Each snapshot can track totals, distinct figure targets, and semantic details such as which Krypto outcomes occurred. Milestone definitions can evaluate one of three moments:

- immediately when a contribution occurs
- once when an activation ends
- once when a completed battle ends

Battle-ending definitions return at most one completion for that battle. Natural victories and defeats finalize the battle snapshot, while manual `/teeny battle stop` abandonment discards it. A definition can independently require victory.

Effects applied by accessories retain their accessory source. Stackable effects also retain the magnitude contributed by each accessory, allowing later milestones to attribute only the accessory-owned portion of effects such as Red Lantern Power Up.

The universal Tier 2 requirements and every nonblank Tier 3 through Tier 5 milestone from the live Google Sheet are registered. Blank designs remain unavailable. Completing a milestone marks its tier as purchase-ready and does not purchase it automatically; Tier 5 purchase additionally waits for its mastery effect implementation.

### Anti-Farming Philosophy
The system should avoid heavy global anti-farming rules. Milestones should be designed so they naturally discourage repetitive or boring grind patterns.

Avoid milestone goals that reward stalling or low-value repetition, such as:

- staying active for a long amount of time
- activating the accessory an excessive number of times
- dealing very large lifetime damage totals
- healing huge amounts over many battles without any condition

Prefer meaningful conditions such as:

- during one battle
- during one activation
- while multiple enemy figures are alive
- while the player's active figure is low on HP
- after one of the player's figures has fainted
- before the first friendly figure faints
- against different enemy figures
- with the accessory's real effect contributing directly

Progress should count only real value:

- healing requires actual missing HP restored
- damage requires valid enemy targets
- defensive progress requires damage actually prevented or reduced
- debuff progress requires the debuff to successfully apply to a valid target

Debug battles, test dummies, or otherwise invalid battles can be excluded later if needed.

### Tier Upgrade Design
Tiers 2 through 4 should usually improve general accessory stats without changing the accessory's identity.

| Upgrade Category   | Examples                                                  |
|--------------------|-----------------------------------------------------------|
| Effect strength    | More damage, more healing, stronger buff, stronger debuff |
| Battery efficiency | Lower activation cost or slower battery drain             |
| Duration           | Longer active time                                        |
| Trigger speed      | First effect happens sooner                               |
| Trigger frequency  | Repeated effects happen more often                        |
| Reliability        | Better targeting, less randomness, more consistent effect |

Not every accessory needs every category. Each accessory should upgrade the stats that matter for its role.

### Implemented Numeric Upgrade Rules
These values are final design decisions and are applied by the tier-aware accessory runtime.

Tier scaling is calculated from each accessory's base balance value rather than storing independent final values for every tier. This allows the base accessory to be rebalanced without manually recalculating its upgraded tiers.

| Upgrade category | Planned scaling |
| --- | --- |
| Standard effect strength at Tier 2 | `1.5x` the base value |
| Standard effect strength at Tier 4 | `2.5x` the base value |
| Battery drain at Tier 3 | `0.7x` the base drain rate, or 30% less drain |
| Trigger interval or wait time at Tier 4 | `0.75x` the base time, or 25% less time |

The Tier 4 standard-strength multiplier is 150% more than the base value. Its added strength over the base is therefore three times the Tier 2 addition: Tier 2 adds 50% of the base value, while Tier 4 adds 150%.

All periodic accessories trigger their effect immediately when activated. Their interval controls the time between later triggers, not the delay before the first trigger.

Special effects use separate curves where the standard strength curve would be too strong or would scale the wrong part of the effect:

| Effect | Tier 2 | Tier 4 | Scaled property |
| --- | --- | --- | --- |
| Raven's Spellbook Curse | `1.3x` | `1.8x` | Mana-regeneration reduction; duration is unchanged by these upgrades |
| Cyborg's Waffle Shooter | `1.5x` | `3.0x` | Waffle duration |
| Lil' Penguin Freeze | `1.25x` | Not applicable; Tier 4 improves interval | Mana burned; Freeze duration is unchanged by Tier 2 |
| Kryptonite Defense Down | `1.25x` | `1.5x` | Defense Down magnitude; duration is unchanged by these upgrades |
| Hive Coin Dance | `1.3x` | `1.8x` | Bonus mana regeneration; duration is unchanged by these upgrades |

Accessory-specific application of these rules:

| Accessory | Tier 2 | Tier 3 | Tier 4 |
| --- | --- | --- | --- |
| Titan's Coin | Max-HP bonus `1.5x` | Battery drain `0.7x` | Max-HP bonus `2.5x` |
| Mother Box | Damage `1.5x` | Battery drain `0.7x` | Interval `0.75x` |
| Bat Signal | Damage `1.5x` | Battery drain `0.7x` | Wait time `0.75x` |
| Red Lantern Battery | Power Up `1.5x` | Battery drain `0.7x` | Interval `0.75x` |
| Green Lantern Battery | Mana granted `1.5x` | Battery drain `0.7x` | Interval `0.75x` |
| Violet Lantern Battery | Healing `1.5x` | Battery drain `0.7x` | Interval `0.75x` |
| Raven's Spellbook | Curse strength `1.3x` | Battery drain `0.7x` | Curse strength `1.8x` |
| Cyborg's Waffle Shooter | Waffle duration `1.5x` | Battery drain `0.7x` | Waffle duration `3.0x` |
| Lil' Penguin | Freeze mana burn `1.25x` | Battery drain `0.7x` | Interval `0.75x` |
| Kryptonite | Defense Down magnitude `1.25x` | Battery drain `0.7x` | Defense Down magnitude `1.5x` |
| Birdarang | Damage `1.5x` | Battery drain `0.7x` | Damage `2.5x` |
| Krypto the Superdog | Heal, Power Up, and Tofu effects each `1.5x` | Battery drain `0.7x` | Interval `0.75x` |

Justice League Coin's Justice Crit frequency and damage values remain undecided until the independent Justice Crit effect is designed. Hive Coin's base Dance values also remain undecided until the accessory is added.

Superman's Underpants efficiency means spending less battery to absorb the same incoming damage. Its exact Tier 2 and Tier 4 efficiency multipliers remain undecided.

Until those values are decided, Justice League Coin and Superman's Underpants receive the shared Tier 3 battery-drain upgrade but retain their base Tier 2 and Tier 4 effect strength. Hive Coin is not registered in the runtime yet.

Resolved accessory values are immutable per activation. The static base registry is never modified, and changing mastery data during an active battle does not alter the current activation.

All numeric base values, tier multipliers, caps, and Tier 5 numeric values belong in `TeenyBalance.java`. Runtime accessory logic should calculate upgraded values from those constants rather than hardcoding final tier values.

Tier 5 should work differently:

- it adds a small unique mastery effect
- it grants a special mastered name
- it should feel thematic instead of being just another numeric upgrade

Tier 5 inherits every upgrade from Tiers 2 through 4 and adds its unique mastery behavior. Unlocking Tier 5 never replaces or removes an earlier tier bonus.

The current Tier 5 designs are:

| Accessory | Tier 5 mastery effect | Runtime status |
| --- | --- | --- |
| Titan's Coin | On activation, heal all living friendly figures by 30% of their original max HP. | Implemented |
| Mother Box | Every fifth hit during one activation is a critical hit that deals `4x` damage. | Implemented |
| Bat Signal | Deal 30% more damage; the attack is undodgeable and shield-piercing. | Implemented |
| Red Lantern Battery | When its Power Up is consumed, 50% of the Red Lantern-owned magnitude remains. | Implemented |
| Green Lantern Battery | Grant 100 mana immediately on activation. | Implemented; Tier 5 milestone undecided |
| Violet Lantern Battery | Its own healing can overheal up to 30% of current max HP. Other healing cannot add overheal, and existing overheal remains after deactivation until damaged or battle end. | Implemented |
| Raven's Spellbook | Slow the opponent by multiplying its final movement-speed attribute by `0.8`. | Implemented; Tier 5 milestone undecided |
| Cyborg's Waffle Shooter | Every fifth successful waffle hit fires a second waffle that blocks another ability for half the normal waffle duration. | Implemented |
| Lil' Penguin | While active, make the opponent move as though it were walking on ice. | Planned; movement implementation undecided |
| Kryptonite | Also reduce the target's damage, movement speed, and mana regeneration by 10%. | Implemented; Tier 5 milestone undecided |
| Justice League Coin | Roll Justice Crit twice per hit. The underlying Justice Crit mechanic still needs to be designed and implemented. | Planned; Justice Crit undecided |
| Birdarang | When inactive, retaliate for 2 damage whenever the active figure is hit. | Implemented |
| Superman's Underpants | Remove passive battery drain; battery is spent only when absorbing damage. | Implemented |
| Krypto the Superdog | Grant two different random effects at each trigger instead of one. | Implemented; Tier 5 milestone undecided |
| Hive Coin | While inactive, increase mana regeneration by 20%. | Planned; base accessory not registered |

Rows without a runtime status remain planned.

### Deferred Tier 5 Designs

- **Lil' Penguin:** ice-like movement is intentionally deferred until its movement implementation is chosen.
- **Justice League Coin:** Justice Crit and the coin's broader identity need a major rework before implementation.
- **Hive Coin:** the base accessory must be designed and added before its inactive Tier 5 mana-regeneration passive.

Example structure:

**Cyborg's Waffle Shooter**

- Tier 1: Periodically blocks one enemy ability with a waffle.
- Tier 2: Waffle duration is `1.5x` the base duration.
- Tier 3: Battery drains slower.
- Tier 4: Waffle duration is `3.0x` the base duration.
- Tier 5: Every fifth successful waffle hit fires a second, shorter waffle that blocks another ability.
- Tier 5 name: Overclocked Waffle Shooter.

### Candidate Mastered Names
At Tier 5, each accessory is intended to receive a mastered name as a presentation reward. The live sheet does not currently finalize these names; the entries below are candidates from earlier planning.

| Base Name               | Mastered Name              |
|-------------------------|----------------------------|
| Bat Signal              | Mastered Bat Signal        |
| Krypto the Superdog     | Loyal Krypto               |
| Cyborg's Waffle Shooter | Overclocked Waffle Shooter |
| Red Lantern Battery     | Raging Red Lantern Battery |
| Raven's Spellbook       | Raven's Mastered Spellbook |

The item does not need to become a new item. The displayed name can change based on the player's unlocked tier.

### Accessory UI
The first functional accessory progression screen is implemented.

- Using a held accessory outside battle opens its progression screen.
- Using the battle accessory while battling retains its existing activation behavior.
- Right-clicking an accessory in the Titan Manager opens the same screen without moving the item.
- The screen reads the synced player mastery and Teeny Coin capabilities.
- Tier purchases use the server-authoritative `AccessoryMasteryService` transaction and immediately resync mastery and coins.
- Unlocked tiers and their completed milestone wording remain visible.
- The next tier, current milestone, progress, reward preview, and purchase cost are visible.
- Tiers beyond the current objective remain hidden.
- An unregistered Tier 3 through Tier 5 milestone is shown as unavailable and cannot be purchased through the screen.
- Justice League Coin and Superman's Underpants honestly report that their undecided Tier 2 and Tier 4 runtime improvements are not configured yet.

The screen should show:

- accessory name
- current tier
- mastered name, if Tier 5 is unlocked
- role or category
- current effect stats
- current tier bonuses
- next milestone
- milestone progress
- next tier preview

The screen reveals only the player's current tier and previously completed tiers. Future milestones and future upgrade effects stay hidden until they become the current tier objective.

When the current milestone is complete and the player can afford the cost, the screen enables a tier-purchase action showing the Teeny Coin cost. The upgrade is never purchased automatically.

Example layout:

```text
Cyborg's Waffle Shooter
Tier 3 / 5
Role: Damage / Disruption

Current Effect: Fires waffles while active.
Current Tier Bonus: Battery drains slower.

Next Tier: Tier 4
Milestone: Hit 3 different enemy figures with waffles in one battle.
```

The UI should make progression readable without requiring the player to guess how upgrades work.

### System Identity
Accessory tiers are meant to keep accessories distinct from the mod's other collectible progression systems:

| Collectible Type | Progression Identity |
| --- | --- |
| Figures | Train and level up |
| Chips | Fuse and combine |
| Accessories | Master through specific use |

### Final Definition
Each accessory has 5 player-bound tiers. Tier 1 is the default state. Tiers 2 through 5 become purchasable after completing one accessory-specific milestone each. Progress only counts while the accessory is equipped. Tier progress is stored on the player per world or server, not on the item.

Tiers 2 through 4 provide general upgrades such as stronger effects, better battery efficiency, longer duration, faster triggers, or improved reliability. Tier 5 requires a harder thematic milestone, gives the accessory a mastered name, and unlocks a unique special bonus.

This system uses small Teeny Coin tier-purchase costs, but does not use item fusion or generic accessory XP. Accessory progression is based on meaningful accessory-specific achievements in battle.
