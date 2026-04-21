# Leveling And Figure Progression

## Purpose
Document the figure growth loop and related upgrade/mastery systems.

## Current Status
Figure items now have an implemented player-facing level-up choice flow through the figure screen. XP rewards from normal battles are still not wired into the live PvE loop yet, but the item-side progression path, pending stat choices, and debug command support are in place.

## Player-Facing Behavior
- Figures have a persistent level and XP value on the item.
- Figures can bank XP across multiple level-ups and spend pending upgrade points later in the figure screen.
- Figures can be leveled, upgraded, reordered, and marked golden through current helper code such as commands, the figure screen, and NPC team builders.
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
- `addXp` reads XP thresholds from `TeenyBalance.FIGURE_XP_REQUIRED_BY_LEVEL`.
- `addXp` can process multiple level-ups in one call.
- Reaching max level blocks additional XP gain.
- Leveling up grants `PendingUpgradePoints` instead of automatically changing stats.
- The figure screen opens by right-clicking a held figure outside battle.
- Pending upgrades are spent manually in the figure screen on `Health`, `Power`, `Dodge`, or `Luck`.
- The previously chosen stat is blocked for the next pending upgrade choice by `LastUpgrade`.
- Ability reorder is now part of the figure screen and costs `250` Teeny Coins per confirmed reorder.
- Ability reorder requires figure level `7`.
- Ability descriptions are now shown in figure-screen hover tooltips instead of inline ability rows.
- `applyUpgrades` directly increases HP, Power, Dodge, or Luck using the `H`, `P`, `D`, and `L` code letters.
- `LastUpgrade` is stored on the item when upgrades are applied.
- NPC team data can spawn figures with predefined level, upgrades, ability order, and golden abilities.

## Not Fully Wired Yet
- I did not find battle victory code that writes XP back into player figures.
- Golden progress can be stored and read, but the normal loop for earning that progress is not yet documented in code as a complete system.
- Ability descriptions and golden descriptions are now supported in ability JSON, but the current content library may still use screen fallbacks until those fields are authored per move.

## Open Questions
- what the normal post-battle XP reward flow should be
- how level-up stat choices should be presented to the player
- what the intended distinction is between "golden", "mastery", and any future duplicate-figure progression loop

## Planned Additions
- tune or expand the XP curve array if the level cap changes
- wire battle reward XP into the normal player loop
- document the final golden earning loop once it exists outside commands and NPC presets
