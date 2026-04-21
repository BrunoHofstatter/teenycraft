# Economy And Shops

## Purpose
Collect the design and implementation notes for currency sinks, figure acquisition, and vendor behavior.

## Current Status
Economy and shop behavior are still mostly planned, but the base player currency layer now exists.

## Player-Facing Behavior
- Players now have a persistent Teeny Coin balance stored on the player, not as inventory items.
- The current Titan Manager screen shows that balance in the top-right corner.
- `/teeny coins <amount>` adds or subtracts from the player's balance for debug and setup.
- The current Chip Fuser screen also reads that player-stored balance and shows a fusion cost placeholder.
- Shops provide controlled access to figures, mystery boxes, and other progression items.
- Duplicate figures may feed both economy and progression systems.

## Source Of Truth
- [`src/main/java/bruhof/teenycraft/capability/ITeenyCoins.java`](../../src/main/java/bruhof/teenycraft/capability/ITeenyCoins.java)
- [`src/main/java/bruhof/teenycraft/capability/TeenyCoins.java`](../../src/main/java/bruhof/teenycraft/capability/TeenyCoins.java)
- [`src/main/java/bruhof/teenycraft/capability/TeenyCoinsProvider.java`](../../src/main/java/bruhof/teenycraft/capability/TeenyCoinsProvider.java)
- [`src/main/java/bruhof/teenycraft/networking/PacketSyncTeenyCoins.java`](../../src/main/java/bruhof/teenycraft/networking/PacketSyncTeenyCoins.java)
- [`src/main/java/bruhof/teenycraft/screen/TitanManagerScreen.java`](../../src/main/java/bruhof/teenycraft/screen/TitanManagerScreen.java)
- [`src/main/java/bruhof/teenycraft/command/CommandTeeny.java`](../../src/main/java/bruhof/teenycraft/command/CommandTeeny.java)
- Related acquisition content currently overlaps with figure items, loaders, and future NPC/world systems.

## Design Notes
- The economy should support collection and progression without making random acquisition feel mandatory or punishing.
- Each shop type should have a distinct role.
- Selling duplicates should not undermine other progression sinks such as sacrifice, crafting, or mastery.

## Open Questions
- current versus planned shop implementation scope
- pricing model and refresh logic
- player-to-player transfer flow if coins should ever be tradeable directly

## Planned Additions
- vendor catalog
- mystery box rules
- duplicate sell values
- acquisition balance references
