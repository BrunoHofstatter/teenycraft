# Figures

## Purpose
Document what a figure is in the current codebase, how figure content is loaded from JSON, what state is stored on the item itself, and how that state is converted into battle runtime data.

## Current Status
Figures are implemented as item-backed collectibles with JSON-authored defaults, persistent NBT state, Titan Manager storage, and battle-time snapshots. Progression data exists on the item, but some progression loops are still partial.

## Player-Facing Behavior
- Figures are collectible `ItemFigure` items with identity, stats, progression state, and an ability loadout.
- The player stores figures in the Titan Manager and fields up to three of them as the active battle team.
- Battles read the team figures from the Titan Manager, then create temporary battle snapshots from those items.
- Golden status is tracked per ability on the figure item and changes ability behavior in battle when active.

## Source Of Truth
- [`src/main/java/bruhof/teenycraft/item/custom/ItemFigure.java`](../../src/main/java/bruhof/teenycraft/item/custom/ItemFigure.java)
- [`src/main/java/bruhof/teenycraft/battle/BattleFigure.java`](../../src/main/java/bruhof/teenycraft/battle/BattleFigure.java)
- [`src/main/java/bruhof/teenycraft/util/FigureLoader.java`](../../src/main/java/bruhof/teenycraft/util/FigureLoader.java)
- [`src/main/java/bruhof/teenycraft/capability/TitanManager.java`](../../src/main/java/bruhof/teenycraft/capability/TitanManager.java)
- [`src/main/java/bruhof/teenycraft/capability/BattleState.java`](../../src/main/java/bruhof/teenycraft/capability/BattleState.java)
- [`src/main/java/bruhof/teenycraft/util/NPCFigureBuilder.java`](../../src/main/java/bruhof/teenycraft/util/NPCFigureBuilder.java)
- [`src/main/java/bruhof/teenycraft/item/ModItems.java`](../../src/main/java/bruhof/teenycraft/item/ModItems.java)
- [`src/main/java/bruhof/teenycraft/TeenyBalance.java`](../../src/main/java/bruhof/teenycraft/TeenyBalance.java)
- [`src/main/resources/data/teenycraft/figures`](../../src/main/resources/data/teenycraft/figures)

## Figure Content Data
Each figure JSON currently provides:

- `id`
- `name`
- `description`
- `class`
- `groups`
- `price`
- `model_type`
- `attributes`
- `abilities`
- `ability_cost_tiers`

`FigureLoader` resolves a figure by JSON `id`, then looks up the matching registered item named `figure_<id>`. In practice, figure content is only valid when the JSON file and `ModItems` registry entry stay in sync.

## Item-Persistent Figure State
When a fresh figure is created, `ItemFigure.initializeFigure` writes the long-term state directly into the item tag.

Implemented persistent fields:

- Identity: figure id, display name, description, class, groups, and price
- Progression: level, XP, `PendingUpgradePoints`, and `LastUpgrade`
- Stats: health, power, dodge, and luck inside a nested `Stats` tag
- Abilities: the figure's ability pool, current ability order, and per-slot cost tiers
- Golden progression: a compound mapping ability id to a `0.0` to `1.0` progress value
- Chip slot: an equipped chip item serialized into the figure item

Important current behavior:

- Fresh figures start at level 1 with 0 XP.
- The default nickname is written, but runtime battle code still reads the base figure name rather than a separate editable nickname field.
- `resetToFactory` rebuilds the figure from the JSON default for that figure id.
- Right-clicking a held figure outside battle now opens the dedicated figure screen.
- The figure screen exposes stat inspection, pending level-up choices, chip installation, and ability reorder.
- The figure screen now shows class beside the figure name and uses hover tooltips for longer ability descriptions.
- Installing a new chip from the figure screen destroys the previously installed chip.

## Base Stats From JSON
Figure JSON does not store final battle stats directly. It stores stat scales under `attributes`.

`FigureLoader` converts those scales into the initial item stats through `ItemFigure.create`:

- HP uses `hp_scale * TeenyBalance.UPGRADE_GAIN_HP`
- Power uses `power_scale * TeenyBalance.UPGRADE_GAIN_POWER`
- Dodge uses `dodge_scale * TeenyBalance.UPGRADE_GAIN_DODGE`
- Luck uses `luck_scale * TeenyBalance.UPGRADE_GAIN_LUCK`

That means the same balance constants used for upgrades also define the size of one authored stat step in figure JSON.

## Ability Loadout On The Figure
Each figure item stores:

- `Abilities`: the full authored ability list
- `AbilityOrder`: the current ordered loadout used by battle and tooltips
- `AbilityTiers`: the mana cost letter for each slot

Current rules:

- Fresh figures start with `AbilityOrder` copied from the authored `abilities` list.
- Reordering is persistent and battle uses the reordered list.
- Slot order changes do not delete abilities. Missing entries are appended back to the end.
- Cost tiers come from figure JSON and are read when calculating mana cost and preview damage.
- The figure screen only allows reorder once the figure reaches level `7`.
- Reorder is charged as a single `250` Teeny Coin confirmation, not per slot swap.
- The figure screen shows ability icons in the reorder list and keeps longer move descriptions in hover tooltips instead of inline row text.

## Golden Ability State
Golden is currently tracked per ability, not as a whole-figure rarity flag.

- `GoldenProgress` stores a float per ability id.
- An ability is considered golden when its progress is `>= 1.0`.
- Battle checks golden status directly from the original figure item.
- Golden bonuses come from ability JSON `golden_bonus` entries, not from figure JSON.
- NPC team definitions can spawn figures with selected golden abilities already enabled.

## Figure To Battle Snapshot
`BattleFigure` is the temporary runtime wrapper created when a battle starts.

Data copied or snapshotted from the original figure item:

- figure id
- display name currently used as nickname
- max HP
- power
- dodge
- luck

Runtime-only battle state stored on `BattleFigure`:

- current HP
- ability cooldowns
- dodge and crit shuffle bags
- temporary accessory HP bonus

The original `ItemStack` is still kept on the `BattleFigure`, so battle systems can keep reading persistent figure data such as ability order, cost tiers, and golden ability status.

## Titan Manager Integration
- The Titan Manager stores full figure items, not abstract figure records.
- Team slot validation only accepts `ItemFigure` items.
- The three team slots reject duplicate figure ids.
- Starting a battle reads the current team stacks from the Titan Manager and wraps each one in a `BattleFigure`.

## Design Notes
- Persistent figure state belongs on the item side.
- Battle should continue using snapshots for volatile data instead of mutating the long-term item every tick.
- Figure content should remain data-driven through JSON plus centralized balance values.
- Figure docs should distinguish collectible player figures from NPC presets, because NPC teams can inject level, upgrades, order, and golden state without a player progression loop.

## Open Questions
- whether nickname editing should become a real player-facing feature rather than stored-only data
- whether groups and class should gain gameplay meaning beyond metadata and filtering
- how the normal player loop should eventually unlock or advance golden progress

## Planned Additions
- add a dedicated figure JSON schema reference if the content library grows
- document collectible acquisition once the drop or shop loop is more stable
- expand the progression cross-links once normal battle XP rewards and golden earning flows are implemented
