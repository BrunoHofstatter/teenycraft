# Teenyverse

## Purpose
Document the Teenyverse dimension, how players are expected to access it, and which parts of the room/travel design are implemented versus still planned.

## Current Status
- Implemented:
  - The repo registers a `teenyverse` dimension and dimension type.
  - The dimension currently uses a void-like flat generator with no terrain features.
  - Current debug battle flow can paste authored arena templates into fixed battle slots in the Teenyverse.
  - Arena templates can now be single-template or multipart.
  - Arena pickup runtime can now run inside those battle slots during active battles.
  - Titan Manager inventory save/restore already reacts to entering and leaving the Teenyverse.
- Not implemented yet:
  - Overworld gateway pods
  - room unlock progression
  - location cards
  - TP pads
  - cartridge rewriter flow
  - room protection rules
  - instanced loot chests
  - a documented room grid beyond the current battle-slot layout

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
- Arena metadata and structure templates are data-driven and can be loaded from JSON and NBT resources.
- Current debug battle flow chooses a fixed Teenyverse battle slot, clears that slot, pastes the arena template or multipart template set there, and teleports the player to the arena's authored spawn point.
- The current slot layout is a simple fixed grid of `4` battle slots spaced `256` blocks apart at `y=64`.
- While an arena battle is active, the slot can also host arena pickup visuals, temporary authored walls, and other per-session arena runtime state.
- On dimension change, entering the Teenyverse triggers Titan Manager inventory save logic and leaving it triggers restore logic.
- If a battle starts while the player is already in the Teenyverse, the current code can still save and restore vanilla inventory for that local battle flow.
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
