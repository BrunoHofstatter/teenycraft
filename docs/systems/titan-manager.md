# Titan Manager

## Purpose
Describe the player-owned figure storage, active team rules, and the menu/search/sort behavior.

## Current Status
The Titan Manager is an implemented player capability with a large internal inventory, menu UI, server-side sorting/searching, and active team slot logic.

## Player-Facing Behavior
- The Titan Manager is separate from the vanilla inventory.
- The active battle team comes from the dedicated team slots.
- A dedicated accessory slot exists for team-wide battle modifiers.
- Main storage is box-based and exposed through a virtualized UI.

## Source Of Truth
- [`src/main/java/bruhof/teenycraft/capability/ITitanManager.java`](../../src/main/java/bruhof/teenycraft/capability/ITitanManager.java)
- [`src/main/java/bruhof/teenycraft/capability/TitanManager.java`](../../src/main/java/bruhof/teenycraft/capability/TitanManager.java)
- [`src/main/java/bruhof/teenycraft/capability/TitanManagerProvider.java`](../../src/main/java/bruhof/teenycraft/capability/TitanManagerProvider.java)
- [`src/main/java/bruhof/teenycraft/screen/TitanManagerMenu.java`](../../src/main/java/bruhof/teenycraft/screen/TitanManagerMenu.java)
- [`src/main/java/bruhof/teenycraft/screen/TitanManagerScreen.java`](../../src/main/java/bruhof/teenycraft/screen/TitanManagerScreen.java)
- [`src/main/java/bruhof/teenycraft/networking/PacketSyncTitanData.java`](../../src/main/java/bruhof/teenycraft/networking/PacketSyncTitanData.java)
- [`src/main/java/bruhof/teenycraft/networking/PacketSearchTitanManager.java`](../../src/main/java/bruhof/teenycraft/networking/PacketSearchTitanManager.java)
- [`src/main/java/bruhof/teenycraft/networking/PacketSortTitanManager.java`](../../src/main/java/bruhof/teenycraft/networking/PacketSortTitanManager.java)

## Design Notes
- Team slot rules are gameplay-critical and should not drift from battle assumptions.
- UI virtualization is required because the full storage size is too large for a vanilla-style menu presentation.
- Sorting and searching should stay server-authoritative to avoid client divergence.

## Current Slot Model
- Team slots
- Accessory slot
- Main storage slots
- Deposit/input behavior

## Open Questions
- Final accessory behavior scope
- Whether chips should live inside figures, in dedicated manager slots, or in a separate progression UI

## Planned Additions
- more explicit accessory/chip integration rules
- stronger collection management tools
- better status and filtering support if the figure roster grows significantly
