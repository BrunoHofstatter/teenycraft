# Rooms

## Purpose
Define the planned room model inside the Teenyverse and keep room types, access patterns, and shared-space rules explicit while the feature is still largely unimplemented.

## Current Status
- This is currently a planning-first document.
- The codebase does not yet define a full room catalog, room structures, room unlock objects, or room-local protection rules.
- The current repo only establishes the Teenyverse as a void dimension and uses a simple origin platform for debug battle flow.

## Player-Facing Behavior
- The intended player experience is to move between purpose-built rooms instead of exploring natural terrain.
- Rooms are expected to serve specific gameplay roles such as combat, shopping, quest progression, travel, and utility.
- The current playable implementation does not expose this room network yet.

## Source Of Truth
- [`docs/world/teenyverse.md`](teenyverse.md)
- [`src/main/java/bruhof/teenycraft/world/dimension/ModDimensions.java`](../../src/main/java/bruhof/teenycraft/world/dimension/ModDimensions.java)
- [`src/main/resources/data/teenycraft/dimension/teenyverse.json`](../../src/main/resources/data/teenycraft/dimension/teenyverse.json)
- [`src/main/java/bruhof/teenycraft/command/CommandTeeny.java`](../../src/main/java/bruhof/teenycraft/command/CommandTeeny.java)

## Design Notes
- The current world plan assumes a single shared Teenyverse dimension rather than one dimension per room.
- Rooms should be spaced far enough apart that players do not naturally see, reach, or interfere with unrelated rooms.
- Each room should be enclosed and themed to match its function and entry point.
- Shared occupancy is acceptable, but progression logic, loot, and other sensitive interactions may need per-player state.
- If room travel is coordinate-based, coordinate ownership and spacing rules should stay documented rather than being left implicit.

## Room Categories
- battle arenas
- shops and markets
- parks and quest hubs
- dungeons and boss rooms
- progression gates
- utility rooms such as travel stations or card rewriting

## Planned Layout Direction
- Overworld entry should come from small "Teeny Pod" structures that route to specific Teenyverse rooms.
- Pods may share a common shell while signaling room type through color or other visual variants.
- Early planning assumes a fixed coordinate grid, with example destinations like a hub near the origin and later rooms spaced along large intervals.
- This grid is still conceptual and should not be treated as final runtime coordinates.

## Planned Additions
- one file per mature room type once details stabilize
- explicit room coordinate rules
- unlock requirements per room or room family
- NPC placement rules
- loot and reset behavior
- shared-room protections and anti-grief policy
- instanced or per-player chest/interaction rules

## Open Questions
- Final room catalog and naming scheme.
- Exact coordinate spacing and whether vertical layering is needed.
- Whether every room needs an overworld pod, or whether some rooms are only reachable from other Teenyverse hubs.
- Which room interactions must be shared and which must be per-player.
