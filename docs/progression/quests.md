# Quests

## Purpose
Document the intended quest and progression gating layer for challenger access, room unlocks, and story flow.

## Current Status
- Planned.
- The repo does not currently contain a finished quest system or authored advancement tree for Teeny progression.
- Existing world docs already plan for progression-gated access to rooms and travel, but runtime quest content is not present yet.

## Player-Facing Behavior
- The intended progression flow is to unlock battles, rooms, and travel options through structured progression rather than free immediate access to everything.
- Quest-like guidance may come from advancement progress, explicit mission items, or both.
- Current playable flow does not yet expose this as a full system.

## Source Of Truth
- [`docs/world/teenyverse.md`](../world/teenyverse.md)
- [`docs/world/rooms.md`](../world/rooms.md)
- [`docs/world/npcs.md`](../world/npcs.md)

## Design Notes
- Advancements are a strong candidate for the underlying progression backbone because they are visible, data-driven, and easy for modpack tooling to inspect.
- Quest delivery does not need to be limited to a single UI model; the important part is the unlock and completion state.
- Progression rules should stay compatible with challenger encounters, room access, and world travel systems.
- Guidance items such as notes, leads, or locator-style tools can supplement the main progression backbone without replacing it.

## Planned Additions
- Advancement or quest chains that gate challengers, rooms, and travel.
- First-clear rewards and repeat-clear state tracking.
- Optional guidance items that point the player toward encounters or objectives.
- Hooks that let external quest mods react to Teeny progression if desired.

## Open Questions
- Whether quests should remain mostly advancement-driven or gain a separate custom state layer.
- How much explicit quest UI the mod should own versus leaving progression visible through world interactions and advancements.
- Whether story progression should be mostly linear, region-based, or partially open.
