# NPC Challengers

## Purpose
Document how authored NPC battle opponents are expected to work in the world and which supporting pieces already exist in the repo.

## Current Status
- Partially implemented.
- NPC challenger entities, dialogue flow, and progression gating are not implemented yet.
- The repo does already support JSON-loaded NPC team definitions.
- Debug battle flow can start a fight against a loaded NPC team and represent that opponent through the current dummy battle entity.
- NPC team JSON can now also include a small optional AI profile for battle behavior tuning.

## Player-Facing Behavior
- The intended long-term flow is to find or unlock challengers in the world, interact with them, and trigger authored battles.
- Repeat clears may later branch into alternate dialogue, trades, or other utility behavior.
- The current player-facing implementation is still debug-oriented rather than a real NPC encounter loop.

## Source Of Truth
- [`src/main/java/bruhof/teenycraft/util/NPCTeamLoader.java`](../../src/main/java/bruhof/teenycraft/util/NPCTeamLoader.java)
- [`src/main/resources/data/teenycraft/npc_teams/`](../../src/main/resources/data/teenycraft/npc_teams)
- [`src/main/java/bruhof/teenycraft/command/CommandTeeny.java`](../../src/main/java/bruhof/teenycraft/command/CommandTeeny.java)
- [`src/main/java/bruhof/teenycraft/entity/custom/EntityTeenyDummy.java`](../../src/main/java/bruhof/teenycraft/entity/custom/EntityTeenyDummy.java)
- [`src/main/java/bruhof/teenycraft/entity/ModEntities.java`](../../src/main/java/bruhof/teenycraft/entity/ModEntities.java)
- [`npc-ai-controls.md`](npc-ai-controls.md)

## Implemented Behavior
- NPC teams can be authored as JSON under `data/teenycraft/npc_teams`.
- `NPCTeamLoader` loads those teams into runtime data.
- `/teeny battle start <npcId>` can build an opponent team from that loaded NPC team data.
- NPC figure entries can now include `chip_id` and `chip_rank`.
- Team JSON can optionally include an `ai` object for battle behavior tuning.
- For the exact AI JSON contract and authoring guide, use [npc-ai-controls.md](npc-ai-controls.md).
- The current battle opponent is still represented by `EntityTeenyDummy`, but that dummy now runs a first-pass battle AI instead of only acting as an idle target.

## Design Notes
- World challengers should own authored teams, encounter identity, and progression requirements.
- Encounter flow can branch on first-clear versus repeat-clear state.
- NPC battle content should remain data-driven where possible for teams and requirements, with code handling interaction flow and encounter runtime.
- Challenger world placement should stay aligned with room/world docs instead of becoming a separate ad hoc spawning system.

## Planned Additions
- Dedicated challenger NPC entities or encounter blocks.
- Interaction flow for dialogue, requirements, battle start, and post-clear behavior.
- Progression-gated challengers tied to quests, advancements, or other unlock state.
- World placement rules for challenger set pieces or Teenyverse room encounters.
- Support for non-battle repeat interactions such as trade, hints, or quest handoff.

## Open Questions
- Whether challengers should primarily live in the Overworld, the Teenyverse, or both.
- Whether repeat clears should remain battle replays, convert to shops/dialogue, or offer optional rematches.
- How much challenger identity should be driven by data versus custom scripted encounter logic.
