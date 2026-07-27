# Accessory Milestones

## Purpose
Track the planned tier milestone design for each accessory in one place.

## Current Status
The milestone event foundation and every nonblank milestone in the Google Sheet's `Accessory Milestones` tab are implemented. This includes Tier 3 and Tier 4 definitions for every implemented accessory except Justice League Coin, plus the eight currently designed Tier 5 challenges.

Blank spreadsheet cells remain unavailable. No milestone was invented for Justice League Coin Tier 3–5, Green Lantern Battery Tier 5, Raven's Spellbook Tier 5, Lil' Penguin Tier 5, Kryptonite Tier 5, or Krypto the Superdog Tier 5. Hive Coin remains unimplemented.

Tier 5 milestone progress is live, but purchasing Tier 5 is intentionally blocked until the separate Tier 5 mastery effects are implemented. Debug tier commands remain available for development.

The Google Sheet remains the design source. `AccessoryMilestoneRegistry` and `TeenyBalance` are the runtime sources for registered conditions and numeric targets.

## Implemented Runtime Table
| Accessory | Tier 3 | Tier 4 | Tier 5 |
| --- | --- | --- | --- |
| Titan's Coin | Grant 200 total max HP | Grant 100 max HP in one battle, 50 times | Leave all 3 figures alive at 1 HP when the Coin ends, 5 times |
| Mother Box | Deal damage 50 times | Deal damage 20 times in one battle, 50 times | Defeat a full-health figure using only Mother Box damage |
| Bat Signal | Hit enemy figures 15 times | Defeat a figure in 50 battles | Defeat all 3 figures with one barrage, 5 times |
| Red Lantern Battery | Gain its Power Up 50 times | Deal 50 accessory-owned bonus damage in one battle, 50 times | Contribute 130 bonus damage to 2 defeated figures in one battle, 5 times |
| Green Lantern Battery | Receive mana 50 times | Go from 5 or less mana to 100 without swapping in 50 battles | Not designed |
| Violet Lantern Battery | Restore missing HP 30 times | Heal one figure by 40 HP in one battle, 50 times | Heal one figure from 1–5 HP to 130 using only the accessory without swapping, 5 times |
| Raven's Spellbook | Apply Curse 10 times | Curse 2 enemy figures in one battle, 50 times | Not designed |
| Cyborg's Waffle Shooter | Apply 50 waffles | Cover all 3 slots of one figure in one battle, 50 times | Cover all 3 slots of all 3 figures in one battle, 5 times |
| Lil' Penguin | Freeze 30 times | Burn 200 mana in one battle, 50 times | Not designed |
| Kryptonite | Apply Defense Down 10 times | Cause 50 Defense Down bonus damage in one battle, 50 times | Not designed |
| Justice League Coin | Not designed | Not designed | Not designed |
| Birdarang | Retaliate 20 times | Retaliate against one figure 5 times in one battle, 50 times | Defeat all 3 figures in one battle, 5 times |
| Superman's Underpants | Block damage 10 times | Block 50 damage in one battle, 50 times | Block 2 fatal hits in one activation, 5 times |
| Krypto the Superdog | Grant a useful effect 50 times | Heal 20 HP and cause 20 Power Up bonus damage in one battle, 50 times | Not designed |

Red Lantern Battery's previously unknown Tier 4 amount is set to 50 attributed bonus damage. Its Tier 5 “entire figure” amount is represented as 130 attributed bonus damage per defeated target. Both values are balance constants and can be tuned without changing milestone logic.

## Scope
- This file covers milestone unlock conditions for accessory Tier 2 through Tier 5.
- Tier 1 is always the default unlocked state and does not need a milestone.
- Tier 2 uses the same simple activation milestone for every accessory.
- Tier 3 proves that the player understands the accessory's main function.
- Tier 4 is a large lifetime contribution milestone and acts as the long-term progression gate.
- Tier 5 is a difficult, specific one-battle mastery challenge.

## Recommended Difficulty Curve
Tier 4 should be the massive milestone, while Tier 5 should be the precise challenge.

This ordering makes Tier 5 late-game because the player cannot attempt it until completing the large Tier 4 requirement. It also makes the final unlock feel like a mastery test instead of ending accessory progression with another long counter.

| Unlock | Milestone Style | Intended Experience |
| --- | --- | --- |
| Tier 2 | Activate the accessory 3 times | Discover and try the accessory |
| Tier 3 | Moderate lifetime use of its real effect | Learn what successful use looks like |
| Tier 4 | Large lifetime contribution from its real effect | Commit to mastering the accessory over many battles |
| Tier 5 | Difficult one-battle challenge, usually requiring a win | Demonstrate deliberate mastery |

The numeric requirements below are first-pass targets. They should be tuned after normal battle length, progression pace, and post-battle tracking are implemented.

Recommended pacing targets:

- Tier 2: approximately 2 to 4 normal battles
- Tier 3: approximately 5 to 10 normal battles
- Tier 4: approximately 30 to 60 normal battles with meaningful accessory contribution
- Tier 5: no fixed battle count; difficult to perform deliberately and unlikely to happen accidentally

The pacing target is more important than keeping the same-looking number across accessories. Each Tier 4 requirement should be calibrated from observed contribution per battle.

## Superseded Planning Table
The following table is retained only as historical context. It predates the live sheet reconciliation and does not describe current runtime requirements.
| Accessory | Tier 2 Milestone | Tier 3 Milestone | Tier 4 Milestone | Tier 5 Milestone |
| --- | --- | --- | --- | --- |
| Titan's Coin | Activate this accessory 3 times. | Take 250 damage while Titan's Coin is active without the damaged figure fainting. | Take 5,000 damage while Titan's Coin is active without the damaged figure fainting. | Win a battle with all 3 friendly figures alive and each at or below 25% of its base max HP. |
| Mother Box | Activate this accessory 3 times. | Deal 250 damage with Mother Box. | Deal 5,000 damage with Mother Box. | In one battle, damage 3 different enemy figures with Mother Box, defeat at least 1 of them with it, and win. |
| Bat Signal | Activate this accessory 3 times. | Hit 15 enemy figures with Bat Signal blasts. | Hit 150 enemy figures with Bat Signal blasts. | Activate Bat Signal while all 3 enemy figures are alive, hit all 3 with its blast, defeat at least 1 with the blast, and win. |
| Red Lantern Battery | Activate this accessory 3 times. | Consume 15 Red Lantern Power Up effects with damaging abilities. | Consume 150 Red Lantern Power Up effects with damaging abilities. | During one activation, consume Red Lantern Power Up with each of the active figure's 3 abilities, defeat an enemy figure, and win. |
| Green Lantern Battery | Activate this accessory 3 times. | Gain 500 mana from Green Lantern Battery. | Gain 5,000 mana from Green Lantern Battery. | Activate below 10 mana, then cast all 3 of the active figure's abilities before the accessory deactivates and win. |
| Violet Lantern Battery | Activate this accessory 3 times. | Restore 250 actual missing HP with Violet Lantern Battery. | Restore 5,000 actual missing HP with Violet Lantern Battery. | In one battle, restore actual missing HP to all 3 friendly figures with Violet Lantern Battery and win with all 3 alive. |
| Raven's Spellbook | Activate this accessory 3 times. | Keep enemies cursed for 2 total minutes with Raven's Spellbook. | Keep enemies cursed for 30 total minutes with Raven's Spellbook. | Win a battle while keeping the opponent continuously cursed from the Spellbook's first curse until the battle ends. |
| Cyborg's Waffle Shooter | Activate this accessory 3 times. | Prevent 15 enemy ability casts with waffles from this accessory. | Prevent 150 enemy ability casts with waffles from this accessory. | In one battle, waffle all 3 enemy ability slots and win before the accessory deactivates. |
| Lil' Penguin | Activate this accessory 3 times. | Remove 500 enemy mana with Lil' Penguin freezes. | Remove 5,000 enemy mana with Lil' Penguin freezes. | In one battle, freeze an enemy at 90 or more mana 3 times and win. |
| Kryptonite | Activate this accessory 3 times. | Deal 500 damage to enemies affected by this accessory's Defense Down. | Deal 10,000 damage to enemies affected by this accessory's Defense Down. | In one battle, defeat all 3 enemy figures while each is affected by this accessory's Defense Down. |
| Justice League Coin | Activate this accessory 3 times. | Land 15 critical hits while affected by this accessory's Luck Up. | Land 150 critical hits while affected by this accessory's Luck Up. | In one battle, land a critical hit with each of the 3 friendly figures while the accessory is active and win. |
| Birdarang | Activate this accessory 3 times. | Retaliate with Birdarang 25 times. | Deal 5,000 retaliation damage with Birdarang. | Win a battle after Birdarang defeats an enemy figure while the active friendly figure is at or below 10% HP. |
| Superman's Underpants | Activate this accessory 3 times. | Absorb 250 damage with Superman's Underpants. | Absorb 5,000 damage with Superman's Underpants. | Absorb a hit that would have defeated the active figure, then win without that figure fainting. |
| Krypto the Superdog | Activate this accessory 3 times. | Receive each of Krypto's 3 possible effects at least 3 times. | Receive 250 total effects from Krypto. | Receive Heal, Power Up, and Tofu from Krypto during one battle, gain real value from all 3, and win. |
| Hive Coin | Activate this accessory 3 times. | Gain 500 bonus mana from Hive Coin's Dance effect. | Gain 5,000 bonus mana from Hive Coin's Dance effect. | Cast all 3 of the active figure's abilities during a single Hive Coin Dance effect and win before that Dance ends. |

## Notes
- Detailed system rules and progression philosophy live in [accessories.md](accessories.md).
- The accessory upgrade paths currently live in the `Teeny Craft - Accessories` Google Sheet.
- `Hive Coin` is present in the upgrade spreadsheet but is not currently implemented or listed in [accessories.md](accessories.md).
- Progress should count real contribution only. Overhealing, damage against defeated figures, mana granted above the cap, and effects that do not successfully apply should not count.
- Tier 5 challenges should only begin tracking after Tier 4 is unlocked.
- Losing counts unless a future milestone explicitly requires a win. Abandoning a battle never awards battle-end progress.
