# AGENTS.md

## Project Overview
Teeny Craft is a Minecraft Forge mod inspired by Teeny Titans Go Figure.
The core loop is: collect figures, manage a 3-figure team in the Titan Manager, battle using a real-time figure combat system, and progress through the Teenyverse.
This repo already contains a working battle foundation, Titan Manager inventory system, JSON-driven figure and ability data, and battle-state capabilities.

## Technical Stack
- Minecraft `1.20.1`
- Forge `47.4.16`
- Java `17`
- ForgeGradle `6.x`
- Official Mojang mappings for `1.20.1`
- Sponge Mixin (`teenycraft.mixins.json`)
- Forge capabilities for player/entity attached state
- Custom packet sync under `src/main/java/bruhof/teenycraft/networking`
- JSON/data-pack driven content under `src/main/resources/data/teenycraft`

## Read Order
### Required First Read
- [docs/overview.md](docs/overview.md)

### Read As Needed
- [docs/systems/battle-engine.md](docs/systems/battle-engine.md) for battle logic, damage, effects, traits, or combat flow changes
- [docs/systems/titan-manager.md](docs/systems/titan-manager.md) for team slots, storage, box logic, search, sort, or manager UI work
- [docs/content/figures.md](docs/content/figures.md) for figure data, progression state, and item-backed figure behavior
- [docs/content/abilities.md](docs/content/abilities.md) for ability schema, execution, and content additions
- [docs/content/effects.md](docs/content/effects.md) for combat effects, periodic logic, and status rules
- [docs/content/accessories.md](docs/content/accessories.md) for battle accessory design and slot behavior
- [docs/content/chips.md](docs/content/chips.md) for chip planning or future implementation
- [docs/world/teenyverse.md](docs/world/teenyverse.md) and [docs/world/rooms.md](docs/world/rooms.md) for world, dimension, room, and travel work
- [docs/progression/leveling.md](docs/progression/leveling.md) and [docs/progression/economy-and-shops.md](docs/progression/economy-and-shops.md) for progression and economy tasks
- [docs/roadmap/README.md](docs/roadmap/README.md) only for exploratory or future-facing work

## Non-Negotiable Rules
- All numeric gameplay balance values belong in [`src/main/java/bruhof/teenycraft/TeenyBalance.java`](src/main/java/bruhof/teenycraft/TeenyBalance.java). Do not hardcode balance numbers in feature logic.
- Figure, ability, NPC team, and other content that is intended to be data-driven should live in JSON/data loaders, not as scattered hardcoded values.
- Player battle participation comes from the Titan Manager team slots, not the vanilla hotbar.
- When code behavior changes, update the related docs in the same task when feasible.
- Keep implemented behavior, intended design, and speculative roadmap notes separated. Do not present roadmap ideas as shipped behavior.

## Source Of Truth
- Runtime mod entry and registration:
  - [`src/main/java/bruhof/teenycraft/TeenyCraft.java`](src/main/java/bruhof/teenycraft/TeenyCraft.java)
- Battle runtime and formulas:
  - [`src/main/java/bruhof/teenycraft/battle`](src/main/java/bruhof/teenycraft/battle)
  - [`src/main/java/bruhof/teenycraft/TeenyBalance.java`](src/main/java/bruhof/teenycraft/TeenyBalance.java)
- Player/team storage and battle state:
  - [`src/main/java/bruhof/teenycraft/capability`](src/main/java/bruhof/teenycraft/capability)
- Menus and client UI:
  - [`src/main/java/bruhof/teenycraft/screen`](src/main/java/bruhof/teenycraft/screen)
  - [`src/main/java/bruhof/teenycraft/client`](src/main/java/bruhof/teenycraft/client)
- Data-driven content:
  - [`src/main/resources/data/teenycraft`](src/main/resources/data/teenycraft)
- Project documentation:
  - [`docs/`](docs)
- Legacy summary:
  - [`GEMINI.md`](GEMINI.md)

## Working Rules For Agents
- Start from `docs/overview.md`, then open the specific topic doc for the system you are changing.
- Treat `Current status` sections in docs as design summaries, but verify behavior in code before making assumptions.
- If a doc and the code disagree, keep the code stable unless explicitly changing behavior, and update the doc to describe the mismatch or the new intended state.
- Prefer extending existing systems over creating parallel ones. This mod already has clear centers for balance, battle execution, effects, traits, capabilities, and content loading.
- Keep new docs short, explicit, and status-driven. Each topic doc should say what is implemented, what is planned, and where the runtime truth lives.

## Documentation Map
- Canonical overview:
  - [docs/overview.md](docs/overview.md)
- System docs:
  - [docs/systems/battle-engine.md](docs/systems/battle-engine.md)
  - [docs/systems/titan-manager.md](docs/systems/titan-manager.md)
- Content docs:
  - [docs/content/figures.md](docs/content/figures.md)
  - [docs/content/abilities.md](docs/content/abilities.md)
  - [docs/content/effects.md](docs/content/effects.md)
  - [docs/content/accessories.md](docs/content/accessories.md)
  - [docs/content/chips.md](docs/content/chips.md)
- World docs:
  - [docs/world/teenyverse.md](docs/world/teenyverse.md)
  - [docs/world/rooms.md](docs/world/rooms.md)
- Progression docs:
  - [docs/progression/leveling.md](docs/progression/leveling.md)
  - [docs/progression/economy-and-shops.md](docs/progression/economy-and-shops.md)
- Roadmap notes:
  - [docs/roadmap/README.md](docs/roadmap/README.md)
- Reusable template:
  - [docs/templates/topic-template.md](docs/templates/topic-template.md)

## Verification
- For code changes, prefer at least `./gradlew.bat build` from the repo root when the task affects compilation.
- For battle or UI changes, verify both code paths and the related topic docs.
- For data/content changes, verify the relevant JSON loader paths and the runtime items/models they map to.

## Known Traps
- `gradle.properties` uses `mod_id=teeny_craft`, while runtime/resource usage in code and data is centered on `teenycraft`. Be careful when touching metadata versus gameplay/resource namespaces.
- `GEMINI.md` is useful background, but new canonical design notes should go under `docs/`.
- Not every feature described in high-level summaries is fully implemented yet. Check code before treating a design note as shipped behavior.
