# Titan Manager

## Purpose
Describe the player-owned figure storage, active team rules, and the menu/search/sort behavior.

## Current Status
The Titan Manager is an implemented player capability with separate figure, chip, and accessory storage pools, a wide tabbed menu UI, server-driven search/sort/filter state, favorites support, and active team slot logic.

## Player-Facing Behavior
- The Titan Manager is separate from the vanilla inventory.
- The active battle team comes from the dedicated team slots.
- A dedicated equipped accessory slot exists for team-wide battle modifiers.
- Main storage is split into separate `Figures`, `Chips`, and `Accessories` tabs instead of a single mixed vault.
- The current screen shows the player's Teeny Coin balance in the top-left area.
- The storage grid is still virtualized, so the menu can expose a large vault without rendering every backing slot at once.
- Search, tab, sort, favorites-only, class filter, and page state are server-authoritative.
- Middle-clicking a visible storage item toggles its favorite state in the current temporary UI.

## Source Of Truth
- [`src/main/java/bruhof/teenycraft/capability/ITitanManager.java`](../../src/main/java/bruhof/teenycraft/capability/ITitanManager.java)
- [`src/main/java/bruhof/teenycraft/capability/TitanManager.java`](../../src/main/java/bruhof/teenycraft/capability/TitanManager.java)
- [`src/main/java/bruhof/teenycraft/capability/TitanManagerProvider.java`](../../src/main/java/bruhof/teenycraft/capability/TitanManagerProvider.java)
- [`src/main/java/bruhof/teenycraft/screen/TitanManagerMenu.java`](../../src/main/java/bruhof/teenycraft/screen/TitanManagerMenu.java)
- [`src/main/java/bruhof/teenycraft/screen/TitanManagerScreen.java`](../../src/main/java/bruhof/teenycraft/screen/TitanManagerScreen.java)
- [`src/main/java/bruhof/teenycraft/screen/TitanManagerViewState.java`](../../src/main/java/bruhof/teenycraft/screen/TitanManagerViewState.java)
- [`src/main/java/bruhof/teenycraft/networking/PacketTitanManagerAction.java`](../../src/main/java/bruhof/teenycraft/networking/PacketTitanManagerAction.java)
- [`src/main/java/bruhof/teenycraft/networking/PacketSyncTitanManagerView.java`](../../src/main/java/bruhof/teenycraft/networking/PacketSyncTitanManagerView.java)
- [`src/main/java/bruhof/teenycraft/networking/PacketSyncTitanData.java`](../../src/main/java/bruhof/teenycraft/networking/PacketSyncTitanData.java)

## Design Notes
- Team slot rules are gameplay-critical and should not drift from battle assumptions.
- UI virtualization is required because the full storage size is too large for a vanilla-style menu presentation.
- Sorting, filtering, search, and page projection stay server-authoritative to avoid client divergence.
- Storage categories should stay explicit in capability code instead of falling back to one mixed handler with item-type checks scattered through the menu.

## Current Slot Model
- Team slots
- Equipped accessory slot
- Figure storage slots
- Chip storage slots
- Accessory storage slots
- Virtualized visible-page projection over the active storage tab

## Open Questions
- Whether the final art/layout should keep the current temporary control density or move some controls into icon-only buttons once a texture exists
- Whether figures need more management filters beyond class and favorites as the roster grows

## Planned Additions
- final textured UI art and polish
- more collection-management filters if the roster grows significantly
- clearer item detail/preview affordances in the manager UI
