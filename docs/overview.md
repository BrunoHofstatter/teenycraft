# Teeny Craft Overview

## Purpose
This file is the canonical high-level summary of the mod. It should stay concise and point readers to the deeper topic docs.

## Mod Summary
Teeny Craft is a Minecraft Forge `1.20.1` mod inspired by Teeny Titans Go Figure.
The player-facing loop is:

1. Collect figures.
2. Manage a 3-figure team in the Titan Manager.
3. Enter battles using a real-time figure combat system.
4. Earn progression through leveling, upgrades, mastery, world unlocks, and economy systems.


## Current Status
### Implemented or substantially present
- Titan Manager capability, team slots, separate figure/chip/accessory storage tabs, and server-driven search/sort/filter flow
- Player-attached Teeny Coin currency with Titan Manager display and debug command support
- First-pass Chip Fuser block/menu/screen for basic duplicate chip rank-up
- Core figure item data and stat handling
- Battle state capability on living entities
- Real-time battle loop with mana, cooldowns, effects, and virtual HP
- Trait/effect registries
- JSON loading for figures, abilities, and NPC teams
- Starter chip runtime with item-backed figure installation and NPC chip support
- Client overlay and debug/dev commands
- Teenyverse dimension registration foundation

### Planned or still evolving
- Broader worldgen/room ecosystem
- Progression loops beyond the current battle foundation
- Shops, accessories, special chip fusion expansion, and content expansion
- More complete quest/travel/world progression structure

## Documentation Rules
- Topic docs must separate `Current status` from `Planned additions`.
- Runtime code remains the final authority for actual current behavior.
- Roadmap notes belong in `docs/roadmap/`, not in canonical system docs unless clearly labeled.
- When a feature changes, update the relevant topic doc in the same task when practical.

## Topic Map
- Systems:
  - [systems/battle-engine.md](systems/battle-engine.md)
  - [systems/battle-ai.md](systems/battle-ai.md)
  - [systems/titan-manager.md](systems/titan-manager.md)
  - [systems/player-vault.md](systems/player-vault.md)
- Content:
  - [content/figures.md](content/figures.md)
  - [content/attributes.md](content/attributes.md)
  - [content/abilities.md](content/abilities.md)
  - [content/traits.md](content/traits.md)
  - [content/effects.md](content/effects.md)
  - [content/accessories.md](content/accessories.md)
  - [content/chips.md](content/chips.md)
- World:
  - [world/teenyverse.md](world/teenyverse.md)
  - [world/rooms.md](world/rooms.md)
  - [world/arenas.md](world/arenas.md)
  - [world/npcs.md](world/npcs.md)
- Progression:
  - [progression/leveling.md](progression/leveling.md)
  - [progression/economy-and-shops.md](progression/economy-and-shops.md)
  - [progression/quests.md](progression/quests.md)
  - [progression/rewards-and-integrations.md](progression/rewards-and-integrations.md)
- Roadmap:
  - [roadmap/README.md](roadmap/README.md)
