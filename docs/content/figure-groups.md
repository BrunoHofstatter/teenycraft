# Figure Groups And Combos

## Purpose
Document group membership, combo selection, combo effects, and the two Titan Manager slots that participate in a combo.

## Current Status
Figure groups are data-driven gameplay content. Each group owns its membership and one or more combo-effect ids. Figure JSON and figure item NBT no longer store group membership.

The shipped data contains 45 group files synchronized from the `Groups` tab of the `Figures Data` sheet. Eleven currently use authored stat effects. The remaining groups use the registered `none` placeholder so membership can ship before their final combo designs are chosen. The sheet also contains planned memberships for figures that are not yet registered in the current 65-figure library; those rows are content design, not shipped runtime data.

## Source Of Truth
- [`src/main/resources/data/teenycraft/figure_groups`](../../src/main/resources/data/teenycraft/figure_groups)
- [`src/main/java/bruhof/teenycraft/util/FigureGroupLoader.java`](../../src/main/java/bruhof/teenycraft/util/FigureGroupLoader.java)
- [`src/main/java/bruhof/teenycraft/group/FigureGroupDefinition.java`](../../src/main/java/bruhof/teenycraft/group/FigureGroupDefinition.java)
- [`src/main/java/bruhof/teenycraft/group/FigureGroupResolver.java`](../../src/main/java/bruhof/teenycraft/group/FigureGroupResolver.java)
- [`src/main/java/bruhof/teenycraft/group/GroupComboEffectRegistry.java`](../../src/main/java/bruhof/teenycraft/group/GroupComboEffectRegistry.java)
- [`src/main/java/bruhof/teenycraft/group/GroupComboExecutor.java`](../../src/main/java/bruhof/teenycraft/group/GroupComboExecutor.java)
- [`src/main/java/bruhof/teenycraft/TeenyBalance.java`](../../src/main/java/bruhof/teenycraft/TeenyBalance.java)

The `Figures Data` sheet is the working content-design source for group rosters before they are synchronized into the repository. Runtime JSON remains authoritative for what is actually shipped.

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
- Group identity is the unique `id`; `name` is presentation-only and does not need to be unique.
- Membership appears only here. A `groups` field in figure JSON is rejected as legacy data.
- Multiple effects are allowed and their stat bonuses combine.
- `none` is a real no-op registry entry used for groups whose combo is not authored yet.

## Content Membership Conventions
Group membership should describe the specific figure version, not automatically inherit every affiliation of its base character. Alternate variants can keep major identities such as Titans when appropriate, while relationship, story, or costume-specific groups should only be added when they fit that version.

New groups should have a clear theme rather than exist only to increase a figure's group count. When several figures have few useful memberships, a coherent thematic group can give them another pairing option; planned examples in `Figures Data` include `Nature Calls`, `Mind Games`, `Monkey Business`, `Dress-Up Day`, and `Dem Legs`.

### Same Same But Different
Alternate forms or identities of the same underlying character can use dedicated groups that all display the name `Same Same But Different`. Each family still has its own unique id, such as `same_same_raven` or `same_same_cyborg`, so only members of the same family actually share a combo.

The repeated display name is intentional. Players can see the same visible group name on unrelated character families without those groups matching. A figure should normally belong to only one `Same Same But Different` family so its own group list does not contain indistinguishable duplicate entries. Existing identity-specific groups that become redundant with this convention, such as the planned Superman/Clark Kent pairing replacing `SuperKent`, should not be kept in parallel.

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
- Synchronize the expanded `Figures Data` group rosters as the remaining figures are registered.
- Replace the existing `Green Lanterns` content with the broader planned `Lantern Corps` roster when its new figures are added.
- Add the planned `Same Same But Different` families and other new thematic groups alongside their figure content.
- Replace `none` on the remaining groups as their combo designs are decided.
- Register non-stat combo behaviors when their battle timing and presentation contracts are defined.
