# Leveling And Figure Progression

## Purpose
Document the figure growth loop and related upgrade/mastery systems.

## Current Status
Figure items already store level, XP, upgrades, and golden ability state. However, the full player-facing progression loop is only partially implemented. The underlying data model exists, but battle rewards, level-up choice flow, and golden earning are not fully wired into a normal gameplay path yet.

## Player-Facing Behavior
- Figures have a persistent level and XP value on the item.
- Figures can be leveled, upgraded, reordered, and marked golden through current helper code such as commands and NPC team builders.
- Upgrades permanently increase the figure's stored stats.
- Golden state is tracked per ability and changes battle behavior when active.

## Source Of Truth
- [`src/main/java/bruhof/teenycraft/item/custom/ItemFigure.java`](../../src/main/java/bruhof/teenycraft/item/custom/ItemFigure.java)
- [`src/main/java/bruhof/teenycraft/battle/BattleFigure.java`](../../src/main/java/bruhof/teenycraft/battle/BattleFigure.java)
- [`src/main/java/bruhof/teenycraft/TeenyBalance.java`](../../src/main/java/bruhof/teenycraft/TeenyBalance.java)
- [`src/main/java/bruhof/teenycraft/command/CommandTeeny.java`](../../src/main/java/bruhof/teenycraft/command/CommandTeeny.java)
- [`src/main/java/bruhof/teenycraft/util/NPCFigureBuilder.java`](../../src/main/java/bruhof/teenycraft/util/NPCFigureBuilder.java)
- [`src/main/resources/data/teenycraft/npc_teams`](../../src/main/resources/data/teenycraft/npc_teams)

## Design Notes
- Keep progression state on the figure item, not in transient battle state.
- The current code separates level gain from stat gains. Leveling up does not automatically grant stats.
- Upgrade math should continue to flow through `TeenyBalance` rather than hardcoded feature logic.
- Golden progression is implemented as per-ability state on the figure item, not as a whole-figure toggle.

## Implemented Now
- Level is clamped between 1 and `MAX_LEVEL`.
- `setLevel` resets the figure's XP to 0.
- `addXp` uses a simple `currentLevel * 100` threshold.
- `addXp` only processes one level-up per call.
- Reaching max level blocks additional XP gain.
- Leveling up does not automatically add stat points.
- `applyUpgrades` directly increases HP, Power, Dodge, or Luck using the `H`, `P`, `D`, and `L` code letters.
- `LastUpgrade` is stored on the item when upgrades are applied.
- NPC team data can spawn figures with predefined level, upgrades, ability order, and golden abilities.

## Not Fully Wired Yet
- I did not find battle victory code that writes XP back into player figures.
- The level-up GUI and player choice flow mentioned in code comments are not part of the normal implemented runtime yet.
- Golden progress can be stored and read, but the normal loop for earning that progress is not yet documented in code as a complete system.

## Open Questions
- what the normal post-battle XP reward flow should be
- how level-up stat choices should be presented to the player
- what the intended distinction is between "golden", "mastery", and any future duplicate-figure progression loop

## Planned Additions
- replace the placeholder XP curve if progression moves beyond `level * 100`
- document the final upgrade-choice UI once implemented
- document the final golden earning loop once it exists outside commands and NPC presets
