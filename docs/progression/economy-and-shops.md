# Economy And Shops

## Purpose
Collect the design and implementation notes for currency sinks, figure acquisition, and vendor behavior.

## Current Status
Economy and shop behavior are part of the game vision, but this doc should be treated as mixed implemented/planned territory until each loop is tied to code.

## Player-Facing Behavior
- Players earn currency through battle performance and collection management.
- Shops provide controlled access to figures, mystery boxes, and other progression items.
- Duplicate figures may feed both economy and progression systems.

## Source Of Truth
- Add runtime file references here as shop and economy systems become concrete.
- Related acquisition content currently overlaps with figure items, loaders, and future NPC/world systems.

## Design Notes
- The economy should support collection and progression without making random acquisition feel mandatory or punishing.
- Each shop type should have a distinct role.
- Selling duplicates should not undermine other progression sinks such as sacrifice, crafting, or mastery.

## Open Questions
- exact currency item/data format
- current versus planned shop implementation scope
- pricing model and refresh logic

## Planned Additions
- vendor catalog
- mystery box rules
- duplicate sell values
- acquisition balance references
