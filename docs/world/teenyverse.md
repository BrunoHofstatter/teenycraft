# Teenyverse

## Purpose
Document the Teenyverse dimension, how players are expected to access it, and which parts of the room/travel design are implemented versus still planned.

## Current Status
- Implemented:
  - The repo registers a `teenyverse` dimension and dimension type.
  - The dimension currently uses a void-like flat generator with no terrain features.
  - Current debug battle flow can teleport the player into the Teenyverse and create a simple iron platform at the origin.
  - Titan Manager inventory save/restore already reacts to entering and leaving the Teenyverse.
- Not implemented yet:
  - Overworld gateway pods
  - room unlock progression
  - location cards
  - TP pads
  - cartridge rewriter flow
  - room protection rules
  - instanced loot chests
  - a documented fixed room grid beyond the origin debug platform

## Player-Facing Behavior
- Right now, normal player access is not a finished gameplay feature.
- In the current codebase, the Teenyverse mainly exists as a registered battle-capable dimension used by debug/dev flow.
- The intended long-term direction is a shared room-based dimension for battles, shops, quest hubs, dungeons, and other progression spaces.

## Source Of Truth
- [`src/main/java/bruhof/teenycraft/world/dimension/ModDimensions.java`](../../src/main/java/bruhof/teenycraft/world/dimension/ModDimensions.java)
- [`src/main/resources/data/teenycraft/dimension/teenyverse.json`](../../src/main/resources/data/teenycraft/dimension/teenyverse.json)
- [`src/main/resources/data/teenycraft/dimension_type/teenyverse.json`](../../src/main/resources/data/teenycraft/dimension_type/teenyverse.json)
- [`src/main/java/bruhof/teenycraft/event/ModEvents.java`](../../src/main/java/bruhof/teenycraft/event/ModEvents.java)
- [`src/main/java/bruhof/teenycraft/command/CommandTeeny.java`](../../src/main/java/bruhof/teenycraft/command/CommandTeeny.java)

## Design Notes
- The Teenyverse should behave like a curated room dimension, not like normal survival terrain.
- The current dimension JSON already matches that direction by using a void-style setup instead of overworld generation.
- Current code does not define real room placement, room access objects, or room protection logic yet.
- Inventory handoff matters when moving into or out of the Teenyverse because battle participation is tied to the Titan Manager system rather than normal overworld inventory flow.

## Implemented Behavior
- `teenyverse` is registered under the mod namespace and has its own dimension type.
- The dimension generator is effectively void-only, with `minecraft:the_void` biome and no configured terrain features.
- `/teeny battle start` can teleport a player into the Teenyverse if they are not already there.
- The current debug command creates a small iron platform around `0, 63, 0` before teleporting the player near `0.5, 64, 0.5`.
- On dimension change, entering the Teenyverse triggers Titan Manager inventory save logic and leaving it triggers restore logic.
- No general-purpose player travel flow into the Teenyverse is currently exposed through world objects or progression systems.

## Planned Additions
- Overworld "Teeny Pod" structures that act as room entrances.
- A single shared Teenyverse dimension with rooms spaced far apart on a fixed coordinate grid.
- Room categories such as shops, battle arenas, parks, quest hubs, and boss dungeons.
- Progression-gated access checks tied to quests or advancements.
- Location cards that store target dimension and coordinates.
- A cartridge rewriter utility that recreates unlocked travel cards.
- Player-home fast travel through linked TP pads.
- Shared rooms with personal loot or per-player interaction state where needed.
- Protection rules that prevent normal survival griefing inside curated rooms.

## Open Questions
- Whether the final room network should be entirely fixed-coordinate, structure-generated, or hybrid.
- Whether all room access should come from overworld pods versus some hub-first travel flow.
- How much of the progression lock should use advancements directly versus a custom quest/state system.
- Whether personal loot should be solved with instanced containers, room-local player state, or another persistence approach.
