# Arenas

## Purpose
Document the intended battle arena spaces used by Teeny battles and keep arena-specific layout rules separate from the broader Teenyverse room overview.

## Current Status
- Mostly planned.
- The current code does not define finished arena structures, arena pickup objects, or arena-specific world logic.
- Current debug battle flow can create a simple iron platform in the Teenyverse, but that is a temporary test surface rather than a real arena system.

## Player-Facing Behavior
- Battles are intended to happen in authored spaces rather than open survival terrain.
- Arenas should shape movement, line-of-sight, and pickup routing instead of acting as empty flat rooms.
- The current playable flow does not yet expose a mature arena catalog.

## Source Of Truth
- [`docs/world/teenyverse.md`](teenyverse.md)
- [`docs/world/rooms.md`](rooms.md)
- [`src/main/java/bruhof/teenycraft/command/CommandTeeny.java`](../../src/main/java/bruhof/teenycraft/command/CommandTeeny.java)
- [`src/main/resources/data/teenycraft/dimension/teenyverse.json`](../../src/main/resources/data/teenycraft/dimension/teenyverse.json)

## Design Notes
- Arenas should include walls, pillars, or other blockers so ranged pressure and targeting are not trivial.
- Arena shape should support real-time movement and swapping rather than feeling like a flat duel pad.
- Arena-specific pickups are a valid design direction if they reinforce movement and timing instead of becoming random clutter.
- Visual presentation should reduce unnecessary vanilla noise if that improves readability during battle.

## Planned Additions
- Authored arena structures inside the Teenyverse room network.
- Arena variants with distinct geometry and traversal pressure.
- Pickup entities or arena interactables such as health, mana, or mobility boosts.
- Clear arena reset rules between battles.
- Arena-specific docs once individual layouts become mature enough to deserve their own files.

## Open Questions
- Whether arenas should be fixed reusable rooms, pasted structures, or generated from a controlled pool.
- Whether pickups are core to every arena or optional modifiers for selected battles.
- How much arena theming should overlap with room progression, NPC identity, or region identity.
