# Player Vault

## Purpose
Document how the mod temporarily removes the player's normal inventory/state for Teeny battle flow, what is currently preserved, and what a fuller vault system may need later.

## Current Status
- Partially implemented.
- The current code saves the player's vanilla inventory contents into the Titan Manager capability when they enter the Teenyverse and restores those contents when they leave.
- The current code does not yet save or restore the wider player state described in older design notes, such as potion effects, experience, game mode, or full attribute baselines.
- Battle loadout replacement is implemented separately by clearing the inventory and repopulating it with Teeny battle items during battle flow.

## Player-Facing Behavior
- Entering Teenyverse battle flow currently strips the normal carried inventory and swaps the player onto a battle-specific loadout.
- Leaving the Teenyverse restores the previously saved vanilla inventory contents.
- This system is intended to let Teeny battles run with controlled items and stats without permanently deleting the player's normal gear.

## Source Of Truth
- [`src/main/java/bruhof/teenycraft/capability/ITitanManager.java`](../../src/main/java/bruhof/teenycraft/capability/ITitanManager.java)
- [`src/main/java/bruhof/teenycraft/capability/TitanManager.java`](../../src/main/java/bruhof/teenycraft/capability/TitanManager.java)
- [`src/main/java/bruhof/teenycraft/capability/BattleState.java`](../../src/main/java/bruhof/teenycraft/capability/BattleState.java)
- [`src/main/java/bruhof/teenycraft/event/ModEvents.java`](../../src/main/java/bruhof/teenycraft/event/ModEvents.java)
- [`src/main/java/bruhof/teenycraft/command/CommandTeeny.java`](../../src/main/java/bruhof/teenycraft/command/CommandTeeny.java)

## Implemented Behavior
- `TitanManager` stores a saved `ListTag` of vanilla inventory data under capability NBT.
- Entering the Teenyverse through the current dimension-change hook calls `saveVanillaInventory(player)`.
- Leaving the Teenyverse calls `restoreVanillaInventory(player)`.
- The saved vanilla inventory payload is persisted as part of the Titan Manager capability rather than a dedicated world-level save object.
- `BattleState.refreshPlayerInventory` clears the player's inventory and fills battle slots with:
  - active figure abilities
  - bench figure swap icons
  - tofu when available
  - the equipped battle accessory when present
- The current implementation does not perform a full-player snapshot before battle.

## Design Notes
- This system exists to separate normal Minecraft player state from controlled Teeny battle state.
- Current implementation is inventory-focused and intentionally lightweight.
- The inventory vault currently lives inside Titan Manager because that capability already persists on the player and is synchronized by existing Teeny systems.
- If the system later expands beyond inventory, it may need to move into a more explicit battle-entry snapshot model instead of remaining a simple Titan Manager field.

## Planned Additions
- Save and restore a broader player snapshot when battle flow demands it.
- Explicit handling for potion effects, experience, game mode, and non-Teeny attribute modifiers if those become battle-relevant.
- Clear policy for modded equipment slots such as Curios-style inventories if those are introduced or supported.
- Crash-safe recovery rules if a player disconnects or a server stops while a battle snapshot is active.
- Better separation between "dimension transition inventory vault" and "battle loadout injection" if the runtime grows more complex.

## Open Questions
- Whether the final design should keep the vault in Titan Manager capability data or move to a dedicated battle snapshot store.
- Whether full attribute restoration is necessary once battle models and accessories are more mature.
- How modded equipment slots should be handled if the mod later integrates with extra inventory APIs.
- Whether the system should restore state on all battle exits, or only on explicit Teenyverse transitions.
