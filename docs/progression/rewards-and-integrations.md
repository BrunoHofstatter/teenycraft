# Rewards And Integrations

## Purpose
Document battle reward directions, crate-style reward ideas, and optional mod integration plans without mixing them into the core shop loop doc.

## Current Status
- Mostly planned.
- The repo does not currently implement reward crates, optional cross-mod reward tables, or PvP duel device flow.
- Existing accessory items and battle rewards are not yet organized into a full post-battle reward system.

## Player-Facing Behavior
- Battles are expected to feed broader progression through rewards, not just through figure XP.
- Rewards may eventually include authored crates, economy items, or optional integration loot when supported.
- The current playable implementation does not yet expose this as a complete player loop.

## Source Of Truth
- [`docs/progression/economy-and-shops.md`](economy-and-shops.md)
- [`docs/systems/player-vault.md`](../systems/player-vault.md)
- [`src/main/java/bruhof/teenycraft/item/ModItems.java`](../../src/main/java/bruhof/teenycraft/item/ModItems.java)

## Design Notes
- Reward abstraction is useful because it decouples encounter difficulty from exact item drops.
- Optional integrations should stay additive and gated on mod presence rather than becoming required dependencies.
- PvP-specific reward or duel systems should stay separate from the baseline PvE progression loop unless a future design intentionally merges them.

## Planned Additions
- Tiered reward crates or equivalent reward bundles.
- Post-battle reward rules tied to encounter type or difficulty.
- Optional mod-specific reward pools when supported mods are present.
- A clearer split between economy rewards, collection rewards, and encounter-clear rewards.
- PvP-specific tools or reward structures if multiplayer battle support expands.

## Open Questions
- Whether crates are the long-term reward format or just an intermediate idea.
- How much reward logic should be data-driven versus code-authored.
- Whether PvP belongs in the same reward layer as NPC/world progression or in a separate system.
