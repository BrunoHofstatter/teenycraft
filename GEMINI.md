# GEMINI.md - Tenny Craft Context & Core Logic

## 1. Project Overview
* **Project Name:** Tenny Craft
* **Platform:** Minecraft Forge 1.20.1
* **Core Concept:** A real-time collectible figure battler inspired by Teeny Titans.
* **Gameplay Loop:** Players collect figures -> Equip them in a team -> Enter a separate dimension/arena -> Battle in real-time (WASD movement + Ability usage) -> Gain XP/Loot.

---

## 2. The Golden Rule: TennyBalance.java
**CRITICAL:** Do not hardcode numbers, durations, damage values, or probabilities in the logic classes.

I have created a dedicated file named `TennyBalance.java`.

* **Requirement:** Every time you need a value (e.g., "How long does Stun last?", "How much damage does Power add?", "Base Mana Regen rate"), you must reference a public static final variable from `TennyBalance.java`.
* **Goal:** I want to tweak the game's balance by editing one file, hunting down numbers in spread-out classes.

---

## 3. Data Architecture (The "Cold vs. Hot" System)
To ensure performance and data safety, the mod distinguishes between the "Item" and the "Battle Entity".

### A. Cold Storage (The Item)
* **What is it?** The ItemFigure sitting in the player's inventory/Curios slot.
* **Data:** Stores permanent data via NBT (XP, Level, Nickname, Installed Chips).
* **Constraint:** This item does not handle battle logic. It is just a data container.

### B. Hot Data (The Battle Wrapper)
* **What is it?** A temporary Java Object created only when a battle starts.
* **Data:** Stores volatile battle state (Current Mana, Cooldown timers, Active Status Effects, Accessory Battery).
* **Lifecycle:**
    * **Battle Start:** Read NBT from Item -> Create Hot Object.
    * **During Battle:** Logic updates here (20 ticks/second).
    * **Battle End:** Calculate XP/State -> Write back to Item NBT -> Destroy Hot Object.

---

## 4. The Battle Logic (Real-Time Arena)
Battles happen in a custom dimension or arena structure.

### General Mechanics
* **Movement:** Player moves freely using standard WASD.
* **Visuals:** The player's render is replaced (morphed) into the GeckoLib model of their active Figure.
* **Input Throttling:** To prevent spam-clicking, we strictly respect the Vanilla Attack Cooldown (player.getAttackStrengthScale). If the bar isn't full, the ability fails.

### The "Vault" (Inventory Safety)
* **Concept:** Players cannot bring their vanilla items (swords/blocks) into the arena.
* **Flow:**
    * **Save:** Before teleporting, the player's full inventory/state is serialized and saved to a safe external storage (e.g., World Data).
    * **Clear:** Inventory is wiped. Attributes/Potion effects are cleared.
    * **Equip:** Player is given specific "Battle Items" (Ability 1-3, Tofu, Bench Swap).
    * **Restore:** Upon battle end (Win or Loss), the "Battle Items" are removed, and the saved inventory/state is restored from the Vault.

### Ability Types
* **Melee:** Hits an area in front of the player (Hitbox check). Mana consumed on Hit.
* **Ranged (Lock-On):** Uses a Raycast to find a target. If found, it "locks on" and deals damage after a delay. Mana consumed on Cast.

---

## 5. Attributes & Statistics
Figures have 4 base stats. Values and upgrade increments are defined in TennyBalance.java.

* **Health (HP):** Standard health pool. Visualized via a custom GUI bar overlay.
* **Power:** Flat damage addition to abilities.
* **Dodge:** A damage mitigation mechanic.
    * **Logic:** Uses a "Shuffle Bag" (Deck of Cards) system.
    * **Example:** A deck of 10 cards. 1 is "Dodge", 9 are "Hit". You draw one per hit. If "Dodge" is drawn, damage is reduced to 0.
    * **Scaling:** Higher stats improve the odds (e.g., deck size gets smaller).
* **Luck:** Affects Critical Hits and Status Effects.
    * **Crits:** Uses the same "Shuffle Bag" logic as Dodge.
    * **Scaling:** Luck directly increases the Duration or Efficiency of specific abilities (e.g., Stun lasts longer, Heals restore more).

---

## 6. Effects & Traits Registry (Battle Logic Hooks)
The Battle Engine must support specific "Hooks" to handle these effects. All durations, probabilities, and magnitudes are defined in TennyBalance.java.

### A. State Control (The "Can I do this?" Checks)
Before a figure moves, casts, or swaps, the engine checks these flags:
* **Stun:** Hard stop. Logic: canMove = false, canCast = false, manaRegen = 0.
* **Root:** Player cannot swap active figures (Hotbar slots 7-8 locked).
* **Waffle (Silence):** Disables a specific ability slot. Visual: A waffle icon overlays the slot.
* **Freeze:** Stops Mana Regen completely. Reduces movement speed significantly.
* **Reflect:** Player is immobile and cannot cast. Incoming damage is reduced, and a % is reflected back to the attacker.
* **Flight:** Moves the player to a Y-level above the arena floor (on invisible barriers), making them immune to ground melee.
* **Disable Opponent:** Target enemy figure in the "Bench" cannot be swapped in.

### B. Mana Engine Modifiers (The Tick Loop)
Logic applied every tick (20 times/second) regarding Mana.
* **Dance:** Increases Mana Regen rate.
* **Curse:** Decreases Mana Regen rate.
* **Shock:** A strict timer. Every X ticks, it triggers a "Mini-Stun" (0.1s) that interrupts charging/movement.
* **Blue Ability (Channeling):** A state where Mana drains over time instead of instant cost. Effect triggers periodically while draining. Ends if Mana = 0 or Stunned.
* **Battle Bar Fill/Deplete:** Instant addition or subtraction of Mana from self or enemy.

### C. Damage Calculation Pipeline
When a hit registers, the damage value passes through this sequence:
1. **Base:** Attacker Power + Ability Base Damage.
2. **Multipliers (Attacker):**
    * **Power Up/Down:** Temporary flat buff/debuff to the next attack.
    * **Luck Up:** Increases Critical Hit chance.
    * **Shield Pierce:** Ignores the victim's Shield status.
3. **Mitigation (Victim):**
    * **Defense Up/Down:** Multiplies incoming damage (e.g., 0.5x or 1.5x).
    * **Shield:** Negates the next damage instance entirely (sets to 0).
    * **Dodge:** Uses "Shuffle Bag" logic. If Dodge triggers, damage = 0.
    * **Undodgeable:** Skips the Dodge check entirely.
4. **Post-Hit Triggers:**
    * **Cuteness (Thorns):** Return fixed damage to attacker.
    * **Kiss:** If active, prevents the victim from receiving positive buffs/healing.
    * **Cleanse:** Removes all negative effects from self and positive effects from enemy.

### D. Special Mechanics & Summons
* **Tofu (Slot 5):** A consumable generated by abilities or RNG. When eaten, grants a random effect (Heal, Shield, Power Up, etc.).
* **Charge Up:** An attack that requires a "casting time" where the player is vulnerable. Cannot be cancelled by damage, only Stun.
* **Multi-Hit:** A single ability use that triggers multiple damage packets (e.g., 3 hits). Each hit independently rolls for Dodge/Crit.
* **Group Damage:** Deals damage to the active figure AND the figures on the bench.
* **Pets / Radio:** Temporary logic entities that trigger effects (Damage or Heal) on specific intervals or when the player attacks.

---

## 7. Figure Acquisition & Leveling
* **Starters:** On first join, players choose 1 of 6 starter figures via a GUI.
* **Mystery Boxes:** The primary way to get new figures. Acts as a loot crate with rarity logic.
* **Leveling:** Figures gain XP from battles (Win/Loss).
    * **Max Level:** 20.
* **Upgrade UI:** On level up, the figure glows. Player right-clicks to open a GUI offering 3 random stat upgrades (Health, Power, Dodge, or Luck).
    * **Restriction:** Cannot pick the same stat twice in a row.
* **The Silkie Block:** An incubator block.
    * **Input:** 1 Target Figure + 1 Sacrifice Figure.
    * **Result:** Sacrifice is deleted. Target gains "Gold" status on one ability (lowered cost or higher damage).

---

## 8. World Generation & The "Teenyverse"
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

## 9. Economy & Quests
* **Currency:** Teeny Coins.
* **Sources:** Won from battles or selling duplicate figures.
* **Shops:**
    * **Toy Shop NPC:** Sells Mystery Boxes and basic figures.
    * **Collector NPC:** Buys figures for coins.
* **Quests:** Integrated with FTB Quests or Vanilla Advancements. Players find "Challenge Notes" (items) directing them to specific coordinates/NPCs.

---

## 10. Technical Stack (Essentials)
* **JSON Driven:** Figures and Abilities are defined in JSON files (stats, models, action lists) so they can be added without recompiling code.
* **GeckoLib:** Used for all Figure animations and models.
* **Networking:** The Server is the authority. The Client sends "Intent to Cast", the Server validates (Mana/Cooldown/Stun status) and executes.