# Accessories

## Purpose
Document how the Titan Manager accessory slot feeds battle and where accessory behavior is implemented.

## Current Status
Accessories are implemented as real items that equip into the Titan Manager accessory slot and activate during battle through the shared battery system.

## Player-Facing Behavior
- One accessory can be equipped in the Titan Manager accessory slot.
- During battle, the equipped accessory is copied into hotbar slot `5` as an activation button.
- Accessories do not auto-activate anymore.
- The player can activate the equipped accessory once battery charge reaches at least `50`.
- Once active, the accessory stays active until battery drains to `0`, the equipped accessory is removed, or battle ends.
- Active accessories drain the existing battle battery bar continuously instead of using a separate resource.

## Current Implemented Content
- Titan's Coin
- Mother Box
- Bat Signal
- Red Lantern Battery
- Green Lantern Battery
- Violet Lantern Battery
- Raven's Spellbook
- Cyborg's Waffle Shooter
- Lil' Penguin
- Kryptonite
- Justice League Coin
- Birdarang
- Superman's Underpants
- Krypto the Superdog

## Source Of Truth
- [`src/main/java/bruhof/teenycraft/accessory/AccessorySpec.java`](../../src/main/java/bruhof/teenycraft/accessory/AccessorySpec.java)
- [`src/main/java/bruhof/teenycraft/accessory/AccessoryRegistry.java`](../../src/main/java/bruhof/teenycraft/accessory/AccessoryRegistry.java)
- [`src/main/java/bruhof/teenycraft/accessory/AccessoryExecutor.java`](../../src/main/java/bruhof/teenycraft/accessory/AccessoryExecutor.java)
- [`src/main/java/bruhof/teenycraft/capability/BattleState.java`](../../src/main/java/bruhof/teenycraft/capability/BattleState.java)
- [`src/main/java/bruhof/teenycraft/capability/TitanManager.java`](../../src/main/java/bruhof/teenycraft/capability/TitanManager.java)
- [`src/main/java/bruhof/teenycraft/item/custom/ItemAccessory.java`](../../src/main/java/bruhof/teenycraft/item/custom/ItemAccessory.java)
- [`src/main/java/bruhof/teenycraft/TeenyBalance.java`](../../src/main/java/bruhof/teenycraft/TeenyBalance.java)

## Design Notes
- Accessories are code-defined for now because the difficult part is runtime hook behavior, not content loading.
- Accessories reuse existing effect ids like `heal`, `bar_fill`, `power_up`, `curse`, and `freeze` directly where possible.
- Accessories bypass the ability scaling layer and apply fixed behavior through accessory-specific runtime code.
- Numeric tuning remains centralized in `TeenyBalance.java`.

## Planned Additions
- more accessory content beyond the current starter set
- reactive accessory hooks for on-hit, on-damaged, and retaliation behavior
- optional HUD feedback for active accessory state
