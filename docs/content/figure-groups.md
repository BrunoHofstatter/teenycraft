# Figure Groups And Combos

## Purpose
Document group membership, combo selection, combo effects, and the two Titan Manager slots that participate in a combo.

## Current Status
Figure groups are data-driven gameplay content. Each group owns its membership and one or more combo-effect ids. Figure JSON and figure item NBT no longer store group membership.

The shipped data contains 45 group files synchronized from the `Groups` tab of the `Figures Data` sheet. Eleven currently use authored stat effects. The remaining groups use the registered `none` placeholder so membership can ship before their final combo designs are chosen. The sheet's `80s` row is not shipped yet because its five named variants do not have registered figure ids in the current 65-figure library.

## Source Of Truth
- [`src/main/resources/data/teenycraft/figure_groups`](../../src/main/resources/data/teenycraft/figure_groups)
- [`src/main/java/bruhof/teenycraft/util/FigureGroupLoader.java`](../../src/main/java/bruhof/teenycraft/util/FigureGroupLoader.java)
- [`src/main/java/bruhof/teenycraft/group/FigureGroupDefinition.java`](../../src/main/java/bruhof/teenycraft/group/FigureGroupDefinition.java)
- [`src/main/java/bruhof/teenycraft/group/FigureGroupResolver.java`](../../src/main/java/bruhof/teenycraft/group/FigureGroupResolver.java)
- [`src/main/java/bruhof/teenycraft/group/GroupComboEffectRegistry.java`](../../src/main/java/bruhof/teenycraft/group/GroupComboEffectRegistry.java)
- [`src/main/java/bruhof/teenycraft/group/GroupComboExecutor.java`](../../src/main/java/bruhof/teenycraft/group/GroupComboExecutor.java)
- [`src/main/java/bruhof/teenycraft/TeenyBalance.java`](../../src/main/java/bruhof/teenycraft/TeenyBalance.java)

## Group JSON
One file represents one group:

```json
{
  "id": "titans",
  "name": "Titans",
  "priority": 1,
  "figures": ["beast_boy", "cyborg", "raven", "robin", "starfire"],
  "combo_effects": ["stat_health"]
}
```

Rules enforced by reload-time content validation:

- `id` must match the JSON filename.
- `name`, integer `priority`, at least two valid figure ids, and at least one registered combo-effect id are required.
- Membership appears only here. A `groups` field in figure JSON is rejected as legacy data.
- Multiple effects are allowed and their stat bonuses combine.
- `none` is a real no-op registry entry used for groups whose combo is not authored yet.

## Loader Indexes
Each resource reload builds immutable indexes in both directions:

- group id to `FigureGroupDefinition`, including the group's set of figure ids
- figure id to the set of group ids containing that figure

This supports group rosters, figure group displays, shared-group checks, sorting, and tooltips without duplicating membership on figure items.

## Combo Slots And Selection
- Only Titan Manager team slots 1 and 2 form the combo pair.
- When those figures share groups, the server resolves one group for the battle.
- The default order is higher `priority` first, then group id alphabetically. Current shipped groups all use priority `1`.
- The player can open the group-combo overlay and explicitly select any group shared by the two figures, or return to `Automatic`.
- An explicit choice is saved on the Titan Manager capability. If later team changes make it invalid, the server clears it and returns to automatic selection.
- Both combo-slot figures receive the selected effect. Team slot 3 receives no group-combo effect.

## Opening Figure
The combo pair does not force the opening figure. A small `1` marker identifies the first appearance slot and can be clicked to cycle through occupied team slots. This saved slot is applied when the player's battle state is initialized. NPC teams currently open with their first runtime figure.

## Registered Combo Effects
The initial registry contains:

- `none`
- `stat_health`
- `stat_power`
- `stat_dodge`
- `stat_luck`

Their numeric gains come from `TeenyBalance`. Effects are referenced by id from JSON so groups can reuse them, combine several effects, and gain new registry-backed behavior later without embedding implementation details in group files.

For current stat effects, the group bonus is part of the battle snapshot. Chip percentage modifiers still calculate from the figure's base stat, while chip exact-value modifiers remain authoritative over additive combo and chip bonuses.

## UI
The Titan Manager brackets the first two team slots as the combo pair. The `?` control explains the active group and opens the selection overlay. The overlay is fed by the server's eligible-group snapshot, shows each effect's exact description, supports automatic or explicit selection, and uses the dedicated group-combo banner asset.

## Planned Content Work
- Replace `none` on the remaining groups as their combo designs are decided.
- Register non-stat combo behaviors when their battle timing and presentation contracts are defined.
- Add the `80s` group after its named variants have real figure ids and content.
