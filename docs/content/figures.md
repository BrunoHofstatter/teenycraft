# Figures

## Purpose
Document what a figure is in the current codebase, how figure content is loaded from JSON, what state is stored on the item itself, and how that state is converted into battle runtime data.

## Current Status
Figures are implemented as item-backed collectibles with JSON-authored defaults, persistent NBT state, Titan Manager storage, and battle-time snapshots. Figure class is no longer metadata-only: battle damage now uses the authored class field for class-advantage bonus damage. Battle-only forms can temporarily override a snapshot's effective abilities, costs, and skin without mutating the collectible. Progression data exists on the item, but some progression loops are still partial.

## Player-Facing Behavior
- Figures are collectible `ItemFigure` items with identity, stats, progression state, and an ability loadout.
- Each figure also has a class, and battle uses that class for the live advantage cycle `Cute > Dark Arts > Super > Tech > Martial Arts > Beast > Cute`.
- The player stores figures in the Titan Manager and fields up to three of them as the active battle team.
- Battles read the team figures from the Titan Manager, then create temporary battle snapshots from those items.
- Golden status is tracked per ability on the figure item and changes ability behavior in battle when active.

## Source Of Truth
- [`src/main/java/bruhof/teenycraft/item/custom/ItemFigure.java`](../../src/main/java/bruhof/teenycraft/item/custom/ItemFigure.java)
- [`src/main/java/bruhof/teenycraft/battle/BattleFigure.java`](../../src/main/java/bruhof/teenycraft/battle/BattleFigure.java)
- [`src/main/java/bruhof/teenycraft/battle/FigureClassType.java`](../../src/main/java/bruhof/teenycraft/battle/FigureClassType.java)
- [`src/main/java/bruhof/teenycraft/util/FigureLoader.java`](../../src/main/java/bruhof/teenycraft/util/FigureLoader.java)
- [`src/main/java/bruhof/teenycraft/util/FigureFormLoader.java`](../../src/main/java/bruhof/teenycraft/util/FigureFormLoader.java)
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
- `price`
- `model_type`
- `attributes`
- `abilities`
- `ability_cost_tiers`

The current shipped figure library contains 65 registered figures synchronized from the `Figures Data` source sheet. Rows without authored stats are intentionally not shipped yet. New rows also require all three slot costs before they are added; existing figures may retain their current slot costs when the sheet leaves those cells blank.

Current authored battle classes in shipped content are:

- `Cute`
- `Dark Arts`
- `Super`
- `Tech`
- `Martial Arts`
- `Beast`
- `none`

`FigureLoader` resolves a figure by JSON `id`, then looks up the matching registered item named `figure_<id>`. In practice, figure content is only valid when the JSON file and `ModItems` registry entry stay in sync.

## Figure Authoring Checklist
When adding a new figure id like `argyle_trigon`, update all of these together:

- Register the item in [`src/main/java/bruhof/teenycraft/item/ModItems.java`](../../src/main/java/bruhof/teenycraft/item/ModItems.java) as `figure_<id>`.
- Add the figure content JSON under [`src/main/resources/data/teenycraft/figures`](../../src/main/resources/data/teenycraft/figures) with matching `id`, `class`, `abilities`, `ability_cost_tiers`, and `model_type`.
- Add the held-item model JSON at `assets/teenycraft/models/item/figure_<id>.json`, usually pointing at `teenycraft:item/<id>`.
- Add the item texture at `assets/teenycraft/textures/item/<id>.png`.
- Add the battle skin texture at `assets/teenycraft/textures/entity/figure/<id>.png`.
- Add `item.teenycraft.figure_<id>` to [`src/main/resources/assets/teenycraft/lang/en_us.json`](../../src/main/resources/assets/teenycraft/lang/en_us.json).


## Item-Persistent Figure State
When a fresh figure is created, `ItemFigure.initializeFigure` writes the long-term state directly into the item tag.

Implemented persistent fields:

- Identity: figure id, display name, description, class, and price
- Progression: level, XP, `PendingUpgradePoints`, and `LastUpgrade`
- Stats: health, power, dodge, and luck inside a nested `Stats` tag
- Abilities: the figure's ability pool, current ability order, and per-slot cost tiers
- Golden progression: a compound mapping ability id to integer points; legacy normalized progress migrates when read
- Chip slot: an equipped chip item serialized into the figure item

Important current behavior:

- Fresh figures start at level 1 with 0 XP.
- The default nickname is written, but runtime battle code still reads the base figure name rather than a separate editable nickname field.
- `resetToFactory` rebuilds the figure from the JSON default for that figure id.
- Right-clicking a held figure outside battle now opens the dedicated figure screen.
- Right-clicking a figure in any Titan Manager team, storage, or player-inventory slot opens the same figure screen without first moving the item. Figures opened this way retain a stable reference to their real backing slot.
- The figure screen shows a `Back` button when entered through the Titan Manager and restores the manager's previous collection view.
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

Figure JSON may optionally select color variants for equipped ability icons:

```json
"ability_icon_variants": {
  "construct_beam": "green"
}
```

The keys must be ability ids in that figure's `abilities` list. Values are lowercase variant ids matching `ability_<ability_id>__variant_<variant>.png`. An omitted entry uses the normal `ability_<ability_id>.png`. This is presentation metadata only: it does not create a new gameplay ability, change golden progress, or add another `texture_index`. Runtime resolves the mapping from the figure id and copies the selected variant onto temporary ability items used in battle and UI previews.

## Battle-Only Forms
Forms are runtime state on `BattleFigure`, not persistent figure-item data. An active form can replace each current reordered source ability with a data-authored effective counterpart, give that counterpart a form-specific cost tier, and use a different presentation skin. Golden state remains owned by and inherited from the source normal ability.

The original collectible identity, class, stats, and `ItemStack` remain unchanged. Form state survives bench swaps, fainting, and revival, then disappears when the battle snapshot is discarded. See [figure-forms.md](figure-forms.md) for the schema, Gorilla content, transform effect, validation, and current AI limitation.

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
- figure class
- max HP
- power
- dodge
- luck

Current class behavior:

- class is read from the original figure item when the `BattleFigure` is created
- `none` means the figure does not gain or suffer matchup advantage in the current cycle
- matchup advantage is resolved against the specific victim figure at hit time, not once for the whole cast

Runtime-only battle state stored on `BattleFigure`:

- current HP
- ability cooldowns
- dodge and crit shuffle bags
- temporary accessory HP bonus
- active battle form id

The original `ItemStack` is still kept on the `BattleFigure`, so battle systems can keep reading persistent figure data such as ability order, cost tiers, and golden ability status.

## Titan Manager Integration
- The Titan Manager stores full figure items, not abstract figure records.
- Team slot validation only accepts `ItemFigure` items.
- The three team slots reject duplicate figure ids.
- Starting a battle reads the current team stacks from the Titan Manager and wraps each one in a `BattleFigure`.
- Group membership is resolved from separate group JSON rather than the figure item. See [figure-groups.md](figure-groups.md).
- The Silkie Station advances one ability by atomically consuming one to five figures from normal Titan Manager storage. See [golden-abilities.md](../progression/golden-abilities.md).

## Design Notes
- Persistent figure state belongs on the item side.
- Battle should continue using snapshots for volatile data instead of mutating the long-term item every tick.
- Figure content should remain data-driven through JSON plus centralized balance values.
- Figure docs should distinguish collectible player figures from NPC presets, because NPC teams can inject level, upgrades, order, and golden state without a player progression loop.

## Open Questions
- whether nickname editing should become a real player-facing feature rather than stored-only data
- whether golden acquisition should gain a progression unlock after its initial creative-only block access

## Planned Additions
- add a dedicated figure JSON schema reference if the content library grows
- document collectible acquisition once the drop or shop loop is more stable
- add the Silkie Station's survival acquisition path when the economy and world progression are ready
