# Arenas

## Purpose
Document the intended battle arena spaces used by Teeny battles and keep arena-specific layout rules separate from the broader Teenyverse room overview.

## Current Status
- First-pass arena runtime is implemented for command-driven battles.
- Arena metadata now loads from JSON under [`src/main/resources/data/teenycraft/arenas`](../../src/main/resources/data/teenycraft/arenas).
- Arena structures now load from template NBT files under [`src/main/resources/data/teenycraft/structures/arenas`](../../src/main/resources/data/teenycraft/structures/arenas).
- Current runtime can select an arena, paste it into a fixed Teenyverse battle slot, spawn the opponent in that arena, and clean the slot back up after battle.
- Pickup objects, wall runtime, and other arena interactables are still planned rather than implemented.

## Player-Facing Behavior
- Battles are intended to happen in authored spaces rather than open survival terrain.
- Arenas should shape movement, line-of-sight, and pickup routing instead of acting as empty flat rooms.
- Current debug flow can already start battles in authored arena templates, but the arena catalog is still small and pickup gameplay is not active yet.

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
- `Launch`: applies a fixed authored launch path. It should send the player to a predictable location every time rather than using free aim.
- `Wall`: spawns a temporary `4x4` wall at an authored arena location.

### Speed Levels
- Speed pickups should use predefined speed levels rather than fully arbitrary values.
- Current direction is to define at least `3` shared speed levels and let each pickup instance choose which level it uses.

### Launch Rules
- Launch should not depend on the player's look direction.
- Launch should use fixed authored direction and authored destination intent.
- A player who takes the same launch pickup under the same circumstances should reliably end up in the same destination zone.
- Launch is expected to have a very short cooldown or effectively no cooldown relative to other pickups.

### Wall Rules
- Wall should not spawn dynamically based on the current line between players.
- Wall should not be fully free-aimed in the first implementation.
- Each wall pickup should be linked to a fixed authored wall location in the arena.
- Wall positions should be marked on the floor with a distinct material or color so players can immediately read where a wall can appear.
- Wall is intended as the most complex pickup tier and should only appear in arenas designed to make meaningful use of temporary line-of-sight or route blocking.

## Pickup Spawn Rules
- Pickups spawn at specific authored spots.
- Each spot has its own cooldown.
- That cooldown starts when the pickup is collected.
- Pickups can either spawn immediately when a battle starts or wait for an initial delay.
- First spawn timing can differ per pickup or per spawn setup.
- A pickup type can be tied to one spot or to multiple eligible spots.
- If a pickup type has multiple possible spots, it should only exist in one of those spots at a time.
- Multi-spot pickups can either rotate through spots in a fixed order or choose the next spot randomly.
- Each pickup setup should explicitly define:
  - assigned pickup type
  - eligible spot or spot group
  - cooldown
  - first-spawn behavior
  - any per-instance modifiers such as heal amount, mana amount, amp amount, speed level, or duration

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
- Pickup entities or arena interactables such as health, mana, or mobility boosts.
- Full pickup runtime, wall runtime, and battle-local arena interactables.
- Arena-specific docs once individual layouts become mature enough to deserve their own files.

## Open Questions
- Exact arena size ranges and final vertical sizing rules.
- Exact arena archetypes for the first `5` builds.
- Exact cover density targets across the arena set.
- Whether long-term arena placement should stay slot-based, become room-based, or use a hybrid.
