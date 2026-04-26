# Arenas

## Purpose
Document the intended battle arena spaces used by Teeny battles and keep arena-specific layout rules separate from the broader Teenyverse room overview.

## Current Status
- First-pass arena runtime is implemented for command-driven battles.
- Arena metadata now loads from JSON under [`src/main/resources/data/teenycraft/arenas`](../../src/main/resources/data/teenycraft/arenas).
- Arena structures now load from template NBT files under [`src/main/resources/data/teenycraft/structures/arenas`](../../src/main/resources/data/teenycraft/structures/arenas).
- Current runtime can select an arena, paste it into a fixed Teenyverse battle slot, spawn the opponent in that arena, and clean the slot back up after battle.
- Arena pickup runtime is now implemented for `Heal`, `Mana`, `Amp`, `Speed`, `Launch`, and `Wall`.
- Temporary wall runtime is now implemented from authored block coordinates and a per-variant block id.
- Pickup visuals currently use non-collectible dropped item entities as placeholder visuals until custom pickup presentation is added.
- Arena templates can now be authored as either a single `template` id or a multipart `template` array.
- Multipart string templates infer their placement from ids ending in `_xNzM`, such as `arena_white_mint_x1z0`.

## Player-Facing Behavior
- Battles are intended to happen in authored spaces rather than open survival terrain.
- Arenas should shape movement, line-of-sight, and pickup routing instead of acting as empty flat rooms.
- Current debug flow can already start battles in authored arena templates with live pickup gameplay, but the arena catalog is still small and mostly dev-authored.

## Source Of Truth
- [`docs/world/teenyverse.md`](teenyverse.md)
- [`docs/world/rooms.md`](rooms.md)
- [`src/main/java/bruhof/teenycraft/command/CommandTeeny.java`](../../src/main/java/bruhof/teenycraft/command/CommandTeeny.java)
- [`src/main/java/bruhof/teenycraft/world/arena/ArenaBattleManager.java`](../../src/main/java/bruhof/teenycraft/world/arena/ArenaBattleManager.java)
- [`src/main/java/bruhof/teenycraft/world/arena/ArenaLoader.java`](../../src/main/java/bruhof/teenycraft/world/arena/ArenaLoader.java)
- [`src/main/resources/data/teenycraft/dimension/teenyverse.json`](../../src/main/resources/data/teenycraft/dimension/teenyverse.json)

## Design Notes
- Arenas should include walls, pillars, or other blockers so ranged pressure and targeting are not trivial.
- Arena shape should support real-time movement and swapping rather than feeling like a flat duel pad.
- Arena-specific pickups are a core part of arena identity, but they should stay readable and limited rather than becoming random clutter.
- Visual presentation should reduce unnecessary vanilla noise if that improves readability during battle.

## Current Design Direction
- Arenas are not meant to be strongly themed around specific rooms, NPCs, or world regions.
- Arena identity should come more from battle style, difficulty, and traversal pressure than from heavy lore theming.
- The environment should support the fight, not become the main attraction over player abilities and active combat decisions.
- The initial target is a set of `5` arena specimens.
- Those first `5` arenas should collectively cover the long-term arena feature set well enough that future battle AI does not need a redesign just because new arenas are added later.
- Each arena has three main parts:
  - structure
  - decoration/theme
  - pickups

## Authoring Coordinates
- Arena JSON coordinates are local to the authored structure origin, not world coordinates.
- Current runtime places arenas with `Rotation.NONE` and `Mirror.NONE`.
- Local axes follow normal world axes:
  - `+X` = east
  - `+Y` = up
  - `+Z` = south
- `0,0,0` is whatever block position was used as the structure origin when the NBT was saved.
- Spawn positions, pickup spots, and launch destinations should usually use centered decimal coordinates like `10.5, 1.0, 6.5`.
- Wall `blocks` use integer local block coordinates because they refer to exact placed blocks.

## Global Arena Rules
- Arenas must be fully enclosed. No sky should be visible from the playable space.
- Arenas must not expose the void as a fail state or traversal hazard.
- Most of the arena should support at least `12` blocks of height so flight remains a valid mechanic.
- It should not be too hard to reach the opponent.
- Arenas should not be so large that traversal becomes the main challenge.
- Arena complexity should not always mean denser clutter. Some advanced arenas can still be relatively open.
- Decoration should stay restrained and should avoid excessive props or non-full-block noise that hurts readability.

## Pickup Design
### Pickup Pool
The current pickup set is:

- `Heal`
- `Mana`
- `Amp`
- `Speed`
- `Launch`
- `Wall`

### Pickup Tiers
- `Tier 1`: `Heal`, `Mana`, `Amp`
- `Tier 2`: `Speed`
- `Tier 3`: `Launch`
- `Tier 4`: `Wall`

The intended rollout is to add pickups to arenas in roughly that order, from simpler arena mechanics to more complex ones.

### Pickup Type Count Per Arena
- Pickup count refers to how many pickup `types` an arena uses, not the total number of pickup spawn points or total spawned pickups.
- Most arenas should use `1` to `3` pickup types.
- `4` pickup types is allowed, but it should be uncommon.

### Pickup Behavior
- `Heal`: grants a flat HP value. The value is defined per pickup instance.
- `Mana`: grants a flat mana value. The value is defined per pickup instance.
- `Amp`: grants a flat damage-amp value. The value is defined per pickup instance.
- `Speed`: grants a timed speed buff. Each pickup chooses a speed level and duration.
- `Launch`: applies one ballistic launch impulse from an authored pickup spot toward an authored destination using normal movement physics.
- `Wall`: spawns a temporary wall using exact authored block coordinates and an authored block id.
- Current runtime uses flat authored values for `Heal` and `Amp`.
- Likely future direction is to keep flat base values in arena JSON and scale them by battle or challenge difficulty once arena rank/difficulty assignment exists.

### Implemented Pickup Schema
- Arena JSON can now define a `pickups` array.
- Arena JSON `template` can now be either:
  - a single structure id string
  - an array of structure ids for multipart arenas
- Multipart string templates currently infer their grid placement from ids ending in `_xNzM` such as `arena_white_mint_x1z0`.
- Each pickup spawner defines:
  - `id`
  - `spots`
  - `spot_mode`
  - `variants`
  - `variant_mode`
  - `first_spawn_delay_ticks`
  - `cooldown_ticks`
- Each spawner only has one active pickup at a time.
- Pickup collection is handled by arena runtime proximity checks rather than vanilla inventory pickup.
- The current collection radius is controlled by `TeenyBalance.ARENA_PICKUP_COLLECTION_RADIUS`.
- Pickup visuals are currently dropped `ItemEntity` placeholders, but collection is custom runtime logic and never inserts an item into inventory.
- `Amp` reuses the existing battle `power_up` runtime so it stacks with other sources exactly the same way.
- `Speed` is participant-level and uses shared `MULTIPLY_BASE` speed multipliers from `TeenyBalance`, so it stays separate from dodge-derived speed scaling.
- `Launch` variants define `destination` and `arc_height`.
- `duration_ticks` is now optional legacy data for launch variants and is no longer used for the actual motion.
- Launch now uses a one-time ballistic impulse solved from the authored destination and arc height, then lets normal movement physics carry the player.
- Launch currently locks battle actions and player movement input during flight so the trajectory stays readable and reliable.
- `Wall` variants define `duration_ticks`, `block`, and explicit authored `blocks` coordinates.

### Pickup Authoring Shape
- A pickup spawner can rotate through multiple `spots`, multiple `variants`, or both.
- `spot_mode` controls how the next spawn position is chosen:
  - `cycle`
  - `random`
- `variant_mode` controls how the next pickup variant is chosen:
  - `cycle`
  - `random`
- `first_spawn_delay_ticks` controls the initial delay before the first spawn. `0` means it appears immediately when battle starts.
- `cooldown_ticks` controls how long the spawner waits after collection before spawning again.
- `variants` are full pickup definitions, not just scalar modifiers. That is what allows one spawner to cycle between `mana`, `wall`, `launch`, and other pickup types at the same spot.

### Pickup Parameter Notes
- `heal.amount`: flat authored heal value
- `mana.amount`: flat authored mana value
- `amp.amount`: flat authored `power_up` value
- `speed.level`: index into the shared arena speed levels in `TeenyBalance`
- `speed.duration_ticks`: how long the participant-level speed buff lasts
- `launch.destination`: local destination position
- `launch.arc_height`: desired authored apex height above the launch start and landing line
- `wall.block`: block id used for the temporary wall
- `wall.blocks`: exact local block coordinates filled while the wall is active

### Speed Levels
- Speed pickups should use predefined speed levels rather than fully arbitrary values.
- Current runtime supports authored speed levels mapped to shared values in `TeenyBalance`.

### Launch Rules
- Launch should not depend on the player's look direction.
- Launch should use fixed authored direction and authored destination intent.
- A player who takes the same launch pickup under the same circumstances should reliably end up in the same destination zone.
- Launch intentionally uses normal movement physics instead of per-tick teleports so the motion reads as a natural launch rather than a scripted reposition.

### Wall Rules
- Wall should not spawn dynamically based on the current line between players.
- Wall should not be fully free-aimed in the first implementation.
- Each wall pickup should be linked to fixed authored wall block coordinates in the arena.
- Wall positions should be marked on the floor with a distinct material or color so players can immediately read where a wall can appear.
- Wall is intended as the most complex pickup tier and should only appear in arenas designed to make meaningful use of temporary line-of-sight or route blocking.

## Pickup Spawn Rules
- Pickups spawn at specific authored spots.
- Each pickup spawner has its own cooldown.
- That cooldown starts when the active pickup from that spawner is collected.
- Pickups can either spawn immediately when a battle starts or wait for an initial delay.
- First spawn timing can differ per pickup or per spawn setup.
- A pickup type can be tied to one spot or to multiple eligible spots.
- If a pickup type has multiple possible spots, it should only exist in one of those spots at a time.
- Multi-spot pickups can either rotate through spots in a fixed order or choose the next spot randomly.
- Each pickup setup should explicitly define:
  - eligible spot or spot group
  - pickup variant set
  - cooldown
  - first-spawn delay
  - spot selection mode
  - variant selection mode

## Arena Coverage Checklist
The first `5` arenas do not need fixed archetype names yet, but together they should cover the following gameplay cases:

- at least one mostly open arena
- at least one arena with meaningful cover or sightline breaks
- at least one arena where vertical movement matters
- at least one arena with dangerous or highly contestable pickup positions
- at least one arena with meaningful route choice
- at least one arena where `Launch` is central to movement
- at least one arena where `Wall` can meaningfully change combat flow
- at least one arena that only uses `Tier 1` pickups
- at least one arena that introduces `Speed`
- at least one arena that introduces `Launch`
- at least one arena that introduces `Wall`
- at least one arena that stays simple and highly readable
- at least one arena that is more complex without relying on clutter
- coverage for both relatively open complexity and denser segmented complexity

## Per-Arena Build Checklist
- clear main combat space
- enclosed ceiling or roof
- no exposed void
- at least `12` blocks of useful height across most of the arena
- opponent re-engagement stays reasonably fast
- arena does not become traversal-heavy enough to overshadow combat
- cover is deliberate and readable
- pickup type count stays within the intended `1` to `3` range unless there is a strong reason to use `4`
- each pickup type has a clear tactical purpose in that arena
- pickup placements are contestable rather than free rewards in completely safe corners
- if `Launch` is present, its destination and tactical use are intentional
- if `Wall` is present, its fixed placement is readable and meaningful
- decoration does not reduce combat readability

## Planned Additions
- Arena variants with distinct geometry and traversal pressure.
- Custom pickup visuals or entity presentation beyond the current placeholder dropped-item form.
- Arena rank or difficulty assignment that can scale authored pickup base values for later-game battles.
- Arena-specific docs once individual layouts become mature enough to deserve their own files.
- Difficulty-aware pickup scaling so flat authored `Heal` and `Amp` values can stay readable in JSON while still scaling for later-game arena battles.

## Open Questions
- Exact arena size ranges and final vertical sizing rules.
- Exact arena archetypes for the first `5` builds.
- Exact cover density targets across the arena set.
- Whether long-term arena placement should stay slot-based, become room-based, or use a hybrid.
