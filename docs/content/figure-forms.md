# Figure Forms

## Purpose
Document the data-driven, battle-only form system and the first Gorilla transformation.

## Current Status
Battle forms are implemented as runtime presentation and loadout overrides on `BattleFigure`. They do not replace the collectible figure, persist to its item, or change its stats. The first shipped form is `gorilla`, currently entered by regular `beast_boy`; `martian_beast_boy` and `80s_beast_boy` can use the same shared form once their normal loadouts gain `gorilla_transform`. Cat Beast Boy intentionally has no transformation.

The Gorilla skin files are temporary Killer Croc duplicates until final Gorilla art is available.

## Source Of Truth
- [`src/main/java/bruhof/teenycraft/util/FigureFormLoader.java`](../../src/main/java/bruhof/teenycraft/util/FigureFormLoader.java)
- [`src/main/java/bruhof/teenycraft/battle/BattleFigure.java`](../../src/main/java/bruhof/teenycraft/battle/BattleFigure.java)
- [`src/main/java/bruhof/teenycraft/battle/BattleAbilitySlot.java`](../../src/main/java/bruhof/teenycraft/battle/BattleAbilitySlot.java)
- [`src/main/java/bruhof/teenycraft/battle/AbilityCostResolver.java`](../../src/main/java/bruhof/teenycraft/battle/AbilityCostResolver.java)
- [`src/main/resources/data/teenycraft/figure_forms`](../../src/main/resources/data/teenycraft/figure_forms)
- [`src/main/resources/data/teenycraft/abilities/self_effect/gorilla_transform.json`](../../src/main/resources/data/teenycraft/abilities/self_effect/gorilla_transform.json)
- [`src/main/resources/data/teenycraft/abilities/self_effect/transform_back.json`](../../src/main/resources/data/teenycraft/abilities/self_effect/transform_back.json)

## Form JSON Contract
Form definitions load from `data/teenycraft/figure_forms/*.json`.

```json
{
  "id": "gorilla",
  "skin": "gorilla",
  "enter_ability": "gorilla_transform",
  "exit_ability": "transform_back",
  "ability_counterparts": {
    "gorilla_transform": "transform_back",
    "tofu": "mighty_punch",
    "whale_drop": "trash_can_toss"
  },
  "ability_cost_tiers": {
    "transform_back": "c",
    "mighty_punch": "e",
    "trash_can_toss": "e"
  }
}
```

Fields:

- `id`: unique form id and, for shipped data, the JSON filename.
- `skin`: texture id used for the world model and battle HUD.
- `enter_ability`: base-form ability that activates this form.
- `exit_ability`: effective form ability that returns to base.
- `model_type`: optional `default` or `slim` presentation override. When omitted, the original figure model type remains active.
- `ability_counterparts`: source normal ability id to effective form ability id.
- `ability_cost_tiers`: effective form ability id to tier `a` through `e`.

The counterpart map follows the figure's current reordered normal slots. Reordering therefore moves each source ability, inherited golden state, and transformed counterpart together. Form costs resolve by effective ability id, not by a fixed slot.

## Runtime Ownership And Lifetime
`BattleFigure.activeFormId` owns the active form. The original figure id and `ItemStack` stay unchanged.

- The form survives swapping to the bench, fainting, and revival in the same battle.
- Cooldowns, activate progress, slot locks, and pending slot state remain attached to the same battle slots rather than resetting during a form change.
- Form state disappears with the temporary `BattleFigure` at battle end.
- Unknown or invalid form data fails safely back toward the base loadout/presentation; reload validation rejects invalid shipped data.

`BattleAbilitySlot` is the authoritative per-slot resolver. It keeps the source normal ability separate from the effective ability and supplies the effective data, cost tier, actual/effective mana costs, icon variant, and inherited golden state to execution, damage, inventory, and HUD paths.

## Transform Effect
`transform` is a parameterless instantaneous self effect. It is not stored as a timed `EffectInstance`.

The executing ability id determines the transition:

- a form's `enter_ability` activates that form;
- the active form's `exit_ability` clears the form and returns to base.

This keeps `gorilla_transform` and `transform_back` free of redundant form parameters while allowing future forms to remain data-driven through their enter/exit ability ids. A successful player transition immediately rebuilds the temporary battle inventory.

## Gorilla Content
The current shared Gorilla mappings cover every normal ability that the regular and planned Beast Boy variants may carry:

- `gorilla_transform` -> `transform_back`
- `tofu` and `cat_scratch` -> `mighty_punch`
- `whale_drop`, `mind_control`, and `dance` -> `trash_can_toss`

Gorilla costs are `c`, `e`, and `e` for `transform_back`, `mighty_punch`, and `trash_can_toss` respectively. `trash_can_toss` is temporary until Barrel Toss is added.

Both transform directions have the `reduced_mana_cost` golden bonus. `AbilityCostResolver` applies the centralized `TeenyBalance.GOLDEN_TRANSFORM_MANA_REFUND_PERCENT` value of `0.70`, so a golden transition pays approximately 30% of its normal cost, rounded to whole mana with a minimum cost of one. The cost reduction happens before spending, but is balanced as the equivalent of refunding 70%.

## Presentation
Network snapshots keep original gameplay identity and form presentation separate:

- `activeFigureId`: original collectible figure id;
- `activeSkinId`: current effective skin id.

The local player model and opponent dummy use the effective skin. The HUD directly renders `textures/item/<activeSkinId>.png` for a transformed portrait while class and collectible identity continue to come from the original figure.

## Validation
Reload validation checks form ids and filenames, enter/exit ability references, parameterless transform effects, counterpart ability references, complete cost-tier coverage, legal tiers and model types, unique enter abilities, and complete mappings for transforming figure loadouts.

## AI Limitation
Opponent AI transformation selection is intentionally not implemented in this feature, and no AI code was changed. The shared runtime can represent and execute effective form slots, but an NPC team that depends on choosing transformation may not behave correctly until the planned broader AI overhaul.
