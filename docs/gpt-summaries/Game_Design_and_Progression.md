# Game Design and Progression

## Core Concept
Teeny Craft is a real-time collectible figure battler inspired by *Teeny Titans*.
The core loop is: Collect Figures ➔ Manage Team (Titan Manager) ➔ Battle in the Teenyverse ➔ Gain XP/Level Up ➔ Upgrade Stats and Master Abilities (Golden).

## Current Implementation Status

**Implemented:**
- **The Titan Manager (Player PC):** A storage system separated from the vanilla Minecraft inventory. It holds the player's active team (up to 3 figures), 1 equipped team accessory, and has separate, server-sorted tabs for storing surplus Figures, Chips, and Accessories.
- **The Player Vault:** When entering a battle, the player's normal vanilla inventory is temporarily saved and replaced with a "Battle Loadout" (ability items, tofu consumable, bench figures, and accessory). After the battle, the normal inventory is restored.
- **Economy:** Players have a persistent "Teeny Coin" balance attached to them (currently adjustable via commands).
- **Persistent Figure Stats:** Figures store their Level, XP, and core stats (HP, Power, Dodge, Luck). Upgrading stats is done manually via a Figure Screen UI using pending upgrade points.

**Planned (Subject to Change):**
- **Quests & Progression Gating:** Using Minecraft Advancements or a custom quest system to gate access to certain NPC challengers, new rooms in the Teenyverse, and travel items.
- **Shops & Vending:** Toy Shop NPCs for buying Mystery Boxes (loot crates for new figures) and Collector NPCs for selling duplicates.
- **Post-Battle Rewards:** Wiring up actual XP rewards into the PvE loop, plus potential reward crates or tiered loot pools based on encounter difficulty.
- **The Silkie Block:** An incubator where a player sacrifices one figure to grant "Golden" (mastery) status to another figure's ability.
