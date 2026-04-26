# World and The Teenyverse

## The Teenyverse Dimension
The Teenyverse is a dedicated dimension designed specifically for controlled gameplay spaces, acting as a shared network of curated rooms rather than open survival terrain.

**Implemented:**
- **Dimension Framework:** The `teenyverse` dimension is registered as a void-style world (no natural terrain generation).
- **Debug Battle Entry:** Battles can currently be initiated via commands. The system grabs an arena template, pastes it into a fixed "battle slot" grid in the void, teleports the player there, and cleans up the structure once the battle concludes.

**Planned:**
- **Teeny Pods (Overworld Gateways):** Small, futuristic 3x3x4 capsules scattered in the Overworld that act as teleporters to the Teenyverse (gated by quest progress).
- **Rooms Network:** A fixed grid of distinct locations within the void dimension (Shops, Parks, Quest Hubs, Dungeons, Travel Stations).
- **Fast Travel:** Using "Location Cards", "TP Pads" for player bases, and "Cartridge Rewriters" to recreate lost cards.

## Arenas and Pickups
Arenas are authored spaces for combat. They are not flat duel pads; they provide verticality, line-of-sight blockers, and tactical routing.

**Currently In-Progress / Implemented:**
- **Arena Design Rules:** Fully enclosed (no sky/void visibility), minimum 12 blocks of height to support `flight` abilities, and deliberate cover to complicate ranged abilities.
- **Pickup System:** Pickups spawn at fixed, authored spots with cooldowns triggered upon collection. They are divided into 4 tiers of complexity:
  - **Tier 1:** `Heal` (Flat HP), `Mana` (Flat Mana), `Amp` (Flat Damage Boost).
  - **Tier 2:** `Speed` (Grants a timed speed buff level).
  - **Tier 3:** `Launch` (A jump pad that sends the player on a fixed, predictable trajectory regardless of where they are looking).
  - **Tier 4:** `Wall` (Spawns a temporary 4x4 line-of-sight blocking wall at an authored, marked location).

## NPC Challengers

**Implemented:**
- **Data-Driven Teams:** The repo supports JSON loading for NPC Team rosters (including their specific levels, chips, and golden abilities).
- **Placeholder Target:** Battles currently use an `EntityTeenyDummy` as the opponent container for testing mechanics.

**Planned:**
- **World Encounters:** True NPC entities placed in specific Teenyverse rooms or the Overworld with dialogue, quest prerequisites, and rematch logic.