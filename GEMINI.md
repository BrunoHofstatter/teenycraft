# GEMINI.md - Tenny Craft Context & Core Logic

## 0. Current Implementation Status
* **Core Figure System:** NBT structure, Stat logic (`StatType`), Chip slot, Mastery (Golden).
* **Titan Manager:** Independent inventory (976 slots), Box system, Sorting/Search sync.
* **Decoupled Battle Engine:** 
    * `IBattleState` capability attached to all `LivingEntity`.
    * Real-time battle loop with Mana, Cooldowns, and Virtual HP.
    * Generic `AbilityExecutor` supporting Player-vs-NPC, NPC-vs-Player, and future PvP.
* **Modular Mechanics:** Centralized `EffectApplierRegistry` and Interface-driven `TraitRegistry`.
* **Data Loaders:** JSON loading for Figures, Abilities, and NPC Teams.
* **Debug Tools:** Comprehensive commands for casting, figure management, and NPC battles.

## 0. Project Overview
* **Platform:** Minecraft Forge 1.20.1.
* **Core Concept:** Real-time collectible figure battler (Teeny Titans inspiration).
* **Gameplay Loop:** Collect figures -> Manage team in Titan Pad -> Battle in the Teenyverse -> Gain XP/Loot -> Master abilities (Golden status).

---

## 1. The Golden Rule: TeenyBalance.java
**CRITICAL:** Every numeric value (damage mults, durations, regen rates, probabilities) MUST be referenced from `TeenyBalance.java`. 
* **Hardcoding is strictly forbidden.** 
* **Goal:** Tweak the entire game's balance by editing a single class.

---

## 2. The Titan Manager (Team & Storage)
The **Titan Manager** is the central hub for Figure management, acting as the player's "PC" or "Box System".

### A. Architecture
*   **System:** Implemented as a Capability (`ITitanManager`) attached to the Player.
*   **Independence:** It is a completely separate inventory from the vanilla player inventory.
*   **Access:** Players access it via the **Titan Pad** item (`ItemTitanPad`).

### B. Slot Layout (The 976 Slots)
The inventory is divided into three critical zones:

1.  **Team Slots (0-2):**
    *   **Purpose:** The 3 active figures used in battle.
    *   **Battle Logic:** The Battle Engine reads *only* these slots to spawn the player's team. It ignores the hotbar.
    *   **Constraint:** Enforces **Unique Figure IDs**. You cannot have two "Robin" figures in the team at once.
2.  **Accessory Slot (3):**
    *   **Purpose:** Holds the active "Battle Accessory" (e.g., Battery, Badge).
    *   **Battle Logic:** This single item provides the global passive/buff for the entire team during combat.
3.  **Storage Slots (4-975):**
    *   **Purpose:** Long-term storage for the collection.
    *   **Structure:** Logic divides this into 18 "Boxes" of 54 slots each.

### C. The "Virtual" UI Logic
The GUI cannot display 900+ slots at once. It uses a virtualized view:

*   **Box Mode:** The main grid displays 54 slots corresponding to the selected Box (e.g., Box 1 shows slots 4-57).
*   **Search Mode:** The server filters the entire storage and sends a list of *matching slot indices* to the client. The UI then displays these specific slots contiguously, hiding the rest.
*   **Deposit Slot:** A "Smart Input" slot. Placing a figure here automatically pushes it into the first available slot in the main storage (0-975), handling overflow efficiently.
*   **Sorting:** Performed server-side. The client requests a sort (Alphabetical, Level), and the server rearranges the entire storage list and syncs the changes.

---

## 3. Data Architecture (Cold vs. Hot)
### A. Cold Storage (The Item)
* **ItemFigure:** Serializes Figure identity (ID, Class, Base Stats) and Progress (Level, XP, Upgrades, Ability Order, Golden Progress).
* **Logic:** `ItemFigure.java` provides helper methods to calculate "Snapshotted" stats (e.g., `calculateAbilityDamage`) used by the engine.

### B. Hot Data (The Battle State)
* **IBattleState:** A Capability attached to the `LivingEntity` (Player or Dummy).
* **Volatile Data:** Stores Current Mana, Active Effects, Tofu Mana, and Charge-up state.
* **Lifecycle:** 
    * Created/Initialized when battle starts.
    * Ticked 20/sec during combat.
    * Inventory Vault handles player item safety.

---

## 4. The Battle Engine
### A. Participant Symmetry
The system treats Players and NPCs identically. Both have an `IBattleState` and an active `BattleFigure`.
* **Attacks:** Automatically target the "Enemy" (Nearest participant with a state).
* **Buffs/Heals:** Automatically target "Self" (The caster).

### B. Ability Execution (`AbilityExecutor`)
* **Melee:** Area-of-effect check in front of caster. Mana consumed on hit.
* **Ranged:** Cone-based target search (Tier-based range). Mana consumed on cast.
* **None (Buffs):** Instant application to self.
* **Golden Bonus:** Golden abilities trigger extra effects/traits parsed from the `golden_bonus` list.

### C. The Damage Pipeline
1. **Base:** Figure Power * Mana Mult * Ability Base * Golden Mult.
2. **Mitigation:**
    * **Effective Stat:** Stats scale with Level/Upgrades.
    * **Dodge:** Reduces damage by the victim's Dodge stat (formula in `DamagePipeline`).
    * **Shield:** Negates damage instances (unless Shield Pierce is active).
3. **Critical Hits:** Uses "Shuffle Bag" logic for predictable RNG based on Luck.
4. **Group Damage:** Distributes damage among all alive figures in the victim's team.

---

## 5. Effects & Traits System
### A. Modular Effects (`EffectApplier`)
Effects are centralized in `EffectApplierRegistry`. 
* **Periodic Effects:** Abstract `PeriodicBattleEffect` handles DOT/ROT logic (Poison, Radios).
* **Radio Effects:** Smart-split logic (Damage/Heal split between active and bench figures).
* **State Modifiers:** Stun (lock move/cast), Root (lock swap), Waffle (lock slot), Kiss (block buffs).

### B. Trait Registry (`ITrait`)
Abilities possess traits that hook into the execution flow:
* **Execution Hooks:** `multi_hit`, `charge_up`.
* **Pipeline Hooks:** `undodgeable`, `shield_pierce`.
* **Hit Trigger Hooks:** `tofu_chance`, recoil logic.

---

## 6. Death Swapping & Round Reset
When a figure faints (Virtual HP <= 0):
1. **Auto-Swap:** The system instantly selects the next alive figure in the team.
2. **Cleanse:** All active effects (Buffs and Debuffs) are cleared from the state.
3. **Reset Lock:** Both participants receive a `DEATH_SWAP_RESET_TICKS` (3s) lockdown.
4. **Visuals:** Both participants Glow, play a Firework Blast sound, and emit a Large Smoke burst.
5. **Victory:** Triggered only when the entire team (3 figures) has fainted.

---

## 7. NPC & AI Infrastructure
* **NPCTeamLoader:** Loads team compositions from `data/teenycraft/npc_teams/*.json`.
* **EntityTeenyDummy:** A generic combat entity that uses GeckoLib models to morph into the active figure.
* **Persistence:** Dummies stay alive during figure swaps; they only "die" (vanilla kill) when the entire virtual team is defeated.

---

## 8. Figure Acquisition & Leveling
* **Starters:** On first join, players choose 1 of 6 starter figures via a GUI.
* **Mystery Boxes:** The primary way to get new figures. Acts as a loot crate with rarity logic.
* **Leveling:** Figures gain XP from battles (Win/Loss).
    * **Max Level:** 20.
* **Upgrade UI:** On level up, the figure glows. Player right-clicks to open a GUI offering 3 random stat upgrades (Health, Power, Dodge, or Luck).
    * **Restriction:** Cannot pick the same stat twice in a row.
* **The Silkie Block:** An incubator block.
    * **Input:** 1 Target Figure + 1 Sacrifice Figure.
    * **Result:** Sacrifice is deleted. Target gains "Gold" status on one ability (lowered cost or higher damage).
* **Shops:** Buy figures with teeny coins.
    * **Input:** 1 Target Figure + 1 Sacrifice Figure.
    * **Result:** Sacrifice is deleted. Target gains "Gold" status on one ability (lowered cost or higher damage).

---

## 9. World Generation & The "Teenyverse"
### A. Overworld Generation (Teeny Pods)
* **Structure:** Small, futuristic capsules (3x3x4) spawning in Overworld biomes.
* **Function:** Act as gateways/teleporters.
* **Interaction Logic:**
    1. Player Right-Clicks the Pod.
    2. System checks for specific Advancements/Quests (e.g., modid:unlocked_gym_1).
    3. **If Unlocked:** Teleport player to specific coordinates in the Battle Dimension.
    4. **If Locked:** Display denial message.

### B. The Battle Dimension ("The Teenyverse")
* **Type:** A single Void Dimension. No terrain generation.
* **Layout (The Grid):** Rooms (Shops, Arenas, Parks, Dungeons) are distinct structures built at specific coordinates in the same void world, spaced far apart (e.g., Shop at 0,64,0, Gym at 1000,64,0).
* **Protection Rules:**
    * **Events:** Strictly cancel BlockBreakEvent, BlockPlaceEvent, and AttackEntityEvent (PvP) within this dimension ID (unless Creative Mode).
    * **Goal:** A "Shared Instance" feel where players see each other but cannot grief or fight outside of arenas.

### C. Loot & Progression Logic
* **Instanced Chests:**
    * **Block:** BlockLootChest.
    * **Logic:** The TileEntity maintains a Map<UUID, ItemStackHandler>. When opened, it checks the Player's UUID to show their personal loot inventory. Multiple players open the same chest but see different items.
* **Travel System:**
    * **Location Card (Item):** Stores target coordinates (X, Y, Z). Awarded via Quests/Advancements.
    * **Cartridge Rewriter (Block):** A utility station. Reads the player's unlocked Advancements and allows crafting replacement Location Cards (prevents soft-locking if items are lost).
    * **TP Pad (Block):** Player-crafted teleporter for their base. Right-click with a Location Card to link it for fast travel.

### D. NPCs (EntityChallenger)
* **Placement:** NPCs spawn inside the specific Rooms in the Teenyverse.
* **Behavior:**
    * They hold a "Team" of 3 Figures.
    * Interaction triggers the Battle Engine.
    * **AI:** Mimics a player (Mana bar, Cooldowns, Swapping logic).

---

## 10. Economy & Quests
* **Currency:** Teeny Coins.
* **Sources:** Won from battles or selling duplicate figures.
* **Shops:**
    * **Toy Shop NPC:** Sells Mystery Boxes and basic figures.
    * **Collector NPC:** Buys figures for coins.
* **Quests:** Integrated with FTB Quests or Vanilla Advancements. Players find "Challenge Notes" (items) directing them to specific coordinates/NPCs.

---

## 11. Technical Stack
* **GeckoLib:** Animations and morphing renders.
* **Capability System:** Modular data attachment to players/entities.
* **Network Authority:** Server validates all actions (Mana/CD/Stun) and syncs to client via `PacketSyncBattleData`.
* **JSON Driven:** All content (Figures, Abilities, Teams) is loaded via data packs.
