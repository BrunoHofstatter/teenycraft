# Figure Transformation Implementation Plan

## Status
Implemented as a historical plan. The canonical shipped contract now lives in [figure forms](../content/figure-forms.md), [figures](../content/figures.md), [abilities](../content/abilities.md), and the [battle engine](../systems/battle-engine.md).

Final implementation decisions that differ from early sections below:

- Only regular `beast_boy` currently has `gorilla_transform`. `martian_beast_boy` and `80s_beast_boy` will join later through the same shared form; Cat Beast Boy intentionally does not transform.
- `transform` is parameterless. Form data resolves transitions through `enter_ability` and `exit_ability`, so repeating `form=gorilla` or `form=base` in each effect is unnecessary.
- Gorilla costs are `c`, `e`, and `e` for return, Mighty Punch, and Trash Can Toss.
- Golden transformation does not become free. Both directions use a centralized 70% cost reduction/refund equivalent and therefore pay approximately 30%, rounded to whole mana with a minimum of one.
- AI transformation support was deliberately deferred and no AI code was changed. An affected development NPC team may not use the mechanic correctly yet.
- The current Gorilla entity and item textures are temporary Killer Croc duplicates pending final art.

## Goal
Add a general, data-driven battle form system that allows an ability to transform the active `BattleFigure` into another form without replacing the underlying collectible figure.

The first implementation is the Beast Boy Gorilla transformation used by the three Beast Boy figures that transform into the same Gorilla in the original game.

The transformation changes only:

- the three effective battle abilities;
- the effective mana/cost tier of those transformed abilities;
- the figure skin shown in the world;
- the active figure presentation/skin shown in the battle HUD.

Everything else remains owned by the original figure and must stay unchanged, including figure identity, current/max HP, stats, class, level, chip/group identity, persistent progression, and other figure data.

## Core Design Rules

### Battle-only form state
Transformation is runtime battle state owned by `BattleFigure`, not persistent `ItemFigure` data.

A `BattleFigure` should gain an active form identifier, conceptually:

```java
String activeFormId; // null/base when untransformed
```

For the first feature:

- base Beast Boy: no active form;
- transformed Beast Boy: `gorilla`.

The original `ItemStack` and figure ID remain unchanged while transformed.

Do not represent Gorilla as a normal collectible figure and do not replace the underlying figure ID with a Gorilla ID.

### Transformation lifetime
The Gorilla form has no timer.

Once transformed, the `BattleFigure` remains Gorilla until the Gorilla's return-transform ability successfully executes.

The form must survive:

- swapping the transformed figure to the bench;
- swapping it back in;
- fainting;
- revival during the same battle.

Fainting does not clear the form. Therefore a Gorilla that is revived returns as Gorilla.

The form naturally disappears when the battle runtime ends because it exists only on `BattleFigure` and is never persisted back to the collectible figure.

### Shared Gorilla form
All three relevant Beast Boy figures use the same Gorilla form:

- same Gorilla skin;
- same Gorilla abilities;
- same Gorilla ability costs;
- same transformation behavior.

Do not duplicate Gorilla form data in each figure JSON.

## Data-Driven Form Definition
Introduce a dedicated data type and loader for battle forms, with data under a location such as:

```text
data/teenycraft/figure_forms/
```

The first definition should be `gorilla.json`.

The exact schema may be adjusted during implementation to fit existing loader conventions, but the Gorilla definition should own at least:

- form id;
- presentation/skin id;
- normal-ability -> transformed-ability counterpart mapping;
- transformed ability -> cost tier mapping;
- optional model-type override only if the rendering architecture benefits from supporting it generically.

Conceptual example:

```json
{
  "id": "gorilla",
  "skin": "gorilla",
  "ability_counterparts": {
    "gorilla_transform": "transform_back",
    "normal_ability_a": "mighty_punch",
    "normal_ability_b": "trashcan_toss"
  },
  "ability_cost_tiers": {
    "transform_back": "...",
    "mighty_punch": "...",
    "trashcan_toss": "..."
  }
}
```

Use the actual normal ability IDs and actual cost tiers when implementing the content.

`trashcan_toss` is intentionally temporary for the first implementation. The final Gorilla ability is `barrel_toss`, but Barrel Toss belongs to the next batch of figure/ability content and is not implemented yet. Once `barrel_toss` exists, replace the temporary Gorilla counterpart with it.

### Why the counterpart map belongs on the form
The three transforming figures share the same Gorilla form, so transformation-specific data should not be repeated in their figure JSONs.

The counterpart map also preserves the existing ability-reordering system. The transformed order is derived from the figure's current normal ability order rather than being a fixed three-slot Gorilla order.

For each current normal ability slot:

```text
current normal ability id
    -> lookup in gorilla ability_counterparts
    -> effective Gorilla ability id in the same current slot
```

Example only:

```text
Current normal order:
whale_drop
gorilla_transform
tofu

Counterpart resolution:
whale_drop        -> trashcan_toss
gorilla_transform -> transform_back
tofu              -> mighty_punch

Effective Gorilla order:
trashcan_toss
transform_back
mighty_punch
```

This means reordering a normal ability also reorders its Gorilla counterpart.

## Figure JSON Changes
Do not add Gorilla/form configuration fields to each affected figure JSON unless implementation discovers a concrete requirement that cannot be represented cleanly by the shared form definition.

The affected figure JSONs should only need their correct normal ability loadouts. In particular, figures that currently use `mighty_punch` as a workaround for the missing transformation mechanic should be restored to their intended `gorilla_transform` normal ability where appropriate.

The transformation is granted by the ability itself, not by a `gorilla: true` or similar figure flag.

## Ability Trigger Through the Effect System
Prefer extending the existing effect/application infrastructure rather than adding Beast Boy-specific handling to `AbilityExecutor`.

The transformation abilities should be self-targeting abilities that invoke a generic instantaneous transform action/effect with parameters.

Conceptually:

```json
{
  "id": "gorilla_transform",
  "type": "self_effect",
  "effect": "transform",
  "params": {
    "form": "gorilla"
  }
}
```

The Gorilla return ability should use the same generic mechanic:

```json
{
  "id": "transform_back",
  "type": "self_effect",
  "effect": "transform",
  "params": {
    "form": "base"
  }
}
```

The exact JSON keys must follow the actual current ability/effect schema rather than blindly copying these conceptual examples.

### Instant action, not persistent participant effect
Do not model Gorilla as an ordinary persistent `EffectInstance` stored on participant-level `BattleState`.

The transform effect/application should act as an instantaneous command that changes `BattleFigure.activeFormId`.

This is important because normal persistent effects are participant-owned and can follow the active side across swaps, while transformation belongs to one specific figure.

Conceptual execution:

```text
ability successfully resolves
    -> generic transform effect/action receives form parameter
    -> resolve active BattleFigure
    -> set/clear activeFormId
    -> rebuild effective battle loadout when necessary
    -> sync presentation
```

Transformation should occur only when the ability successfully resolves. Starting/clicking an ability must not transform the figure before normal validation, cost, cast, and execution rules succeed.

## Effective Battle Loadout
The main foundation change is to stop assuming that the persistent `ItemFigure` ability loadout is always the currently usable battle loadout.

`BattleFigure` (or a narrowly scoped resolver owned by the battle layer) should become the source of effective battle slot data.

For an untransformed figure, effective data resolves to the existing normal `ItemFigure` data.

For a transformed figure, effective data resolves through the active form definition.

The implementation should expose clear APIs for concepts such as:

- effective ability ID for a battle slot;
- source/original normal ability ID for a battle slot;
- effective cost tier for a battle slot;
- effective skin/presentation ID;
- effective model type if form model overrides are supported.

Avoid scattering `if (form == gorilla)` checks throughout battle, HUD, and rendering code.

## Source Ability vs Effective Ability
Transformation introduces an important distinction:

```text
source ability = the original normal figure ability occupying that current reordered slot
effective ability = the ability currently executed/displayed in that slot
```

For example while transformed:

```text
source ability:    tofu
effective ability: mighty_punch
```

The source ability remains important for persistent progression and slot correspondence. The effective ability controls current battle behavior, icon/name, damage/execution data, and transformed mana cost.

## Golden Ability Behavior
Gorilla abilities do not have independent persistent golden progression.

Each transformed ability inherits the golden status of its normal counterpart.

For each current battle slot:

```text
source normal ability -> persistent golden lookup
effective Gorilla ability -> uses that inherited golden status
```

Example:

```text
tofu is golden
    -> transform
    -> corresponding mighty_punch is presented/executed as golden
```

If abilities are reordered, the source ability and its transformed counterpart move together, so golden status follows the pair automatically.

Do not write Gorilla ability golden progress back to the figure as independent progression.

Any golden-bonus behavior that depends on the executing ability must be reviewed carefully during implementation so inherited golden state works with the effective transformed ability while persistent ownership remains on the source ability.

## Ability Costs
Gorilla abilities use the Gorilla form's own authored ability cost tiers rather than the normal source abilities' costs.

Because transformed order can change with normal ability reordering, costs should resolve by transformed ability ID/counterpart rather than by a fixed form slot index.

## Cooldowns and Other Slot Runtime State
Existing battle cooldown/progress state should remain tied to the corresponding battle slot/pair through transformation rather than being reset merely because the effective ability ID changed.

At minimum review and preserve the intended continuity of:

- ability cooldowns;
- activate progress;
- slot-local Disable behavior;
- slot locks;
- charge/channel state and pending casts.

The transformation implementation must not accidentally provide cooldown resets or progression resets by changing ability IDs.

If an in-progress charge/channel state can legally coexist with transformation, define deterministic behavior. Prefer using the existing successful-execution lifecycle so the transformation itself does not leave stale pending state.

## Battle Inventory / Hotbar
The battle inventory currently rebuilds ability items from the original figure stack. Refactor that path so ability slots are populated from the active `BattleFigure`'s effective battle loadout.

After a successful transformation or transformation back, refresh the player's battle inventory so the three ability items immediately represent the new effective abilities, costs, icons, damage values, and inherited golden status.

The refactor must preserve existing normal-figure behavior when no form is active.

## Player and NPC Execution
The form mechanic must live in the shared battle runtime and work for both player-controlled and AI/NPC-controlled battlers.

Do not implement Gorilla transformation only as a player inventory or client presentation trick.

NPC AI should see/execute the effective transformed loadout through the same battle APIs used by the player path. Review AI ability selection for any direct reads from the original `ItemFigure` loadout and route them through effective battle slot data where necessary.

## Skin and Presentation Changes
Transformation must change the visible figure skin both in-world and in the battle HUD.

### Keep gameplay identity separate from presentation identity
Do not change the active gameplay figure ID from the original Beast Boy figure to `gorilla`.

Presentation sync should distinguish concepts such as:

```text
activeFigureId = original collectible/gameplay identity
activeSkinId = effective form presentation texture
```

For an untransformed figure, `activeSkinId` can resolve to the normal figure skin/ID.

For Gorilla, it resolves to the shared Gorilla skin.

### Local player world skin
Update the client player skin override path so the active battle figure's effective skin/presentation ID is used instead of deriving the texture exclusively from the original figure ID.

When Beast Boy transforms, the actual player model visible in the Minecraft world must immediately display the Gorilla skin.

When transforming back, it must return to that Beast Boy figure's normal skin.

### Opponent dummy skin
Apply the same effective-skin concept to `DummyRenderer` so AI/NPC Beast Boy transformations are visible correctly.

### HUD active figure presentation
Any battle HUD figure portrait/card/skin rendering must use the effective presentation skin while keeping the original figure ID for gameplay identity and any logic that needs the collectible figure.

### Model type
Gorilla currently only requires a skin swap unless the actual asset requires a different existing player model type. Preserve the current figure model type by default.

The form schema/runtime may support an optional model override if doing so is inexpensive and keeps presentation resolution clean, but do not invent a new entity/model system for this feature.

## Validation and Failure Behavior
Extend battle-content validation for the new data-driven form system.

Validate at reload/startup as appropriate that:

- every form ID referenced by a transform action exists;
- every normal ability key in a form counterpart map exists;
- every transformed ability ID exists;
- every transformed ability has a valid cost tier;
- form skin/presentation references follow the project's resource expectations where practical;
- affected transforming figures have all required counterpart mappings for every ability they can carry in their normal reordered loadout;
- transform-back configuration cannot leave the figure in an invalid/unknown form.

Prefer loud validation errors for invalid shipped content rather than silent fallback to unrelated abilities.

Runtime code should still fail safely if malformed/reloaded data is encountered during an active battle.

## Initial Gorilla Content
The first implementation should restore the original transformation concept for the relevant Beast Boy figures.

The shared Gorilla effective abilities are:

1. return transformation ability (`transform_back` or the final chosen internal ID);
2. `mighty_punch`;
3. `barrel_toss` eventually.

For this implementation, use `trashcan_toss` as the temporary counterpart in place of `barrel_toss` because Barrel Toss will be implemented with the next figure/ability batch.

The Gorilla form should use the shared Gorilla skin asset.

Before editing content, inspect the actual three Beast Boy figure JSONs and original intended ability relationships so the counterpart map uses the correct source ability IDs. Do not infer those IDs from examples in this plan.

## Suggested Implementation Phases

### Phase 1 - Form data foundation
- Define the battle-form data model.
- Add the form loader/reload integration following existing JSON loader patterns.
- Add `gorilla.json` with the shared presentation, counterpart map, and transformed costs.
- Add validation for form structure and referenced ability IDs.
- Keep normal battle behavior unchanged at this stage.

### Phase 2 - BattleFigure effective-loadout abstraction
- Add battle-only active form state to `BattleFigure`.
- Add effective/source ability resolution APIs.
- Add effective cost and presentation resolution.
- Preserve normal behavior as the fallback when no form is active.
- Refactor correctness-critical direct reads of original figure ability data where transformed behavior requires it.

### Phase 3 - Generic transform effect/action
- Add a generic data-driven instantaneous transform application through the existing effect/application infrastructure.
- Support at least `form=gorilla` and returning to base.
- Ensure transformation happens only after successful ability execution.
- Ensure form state is figure-owned and survives swaps, fainting, and revival.
- Ensure it is discarded naturally at battle end.

### Phase 4 - Battle loadout and golden integration
- Refactor battle hotbar reconstruction to consume effective slot data.
- Preserve current reordered normal ability order through the counterpart map.
- Resolve transformed costs from the form.
- Make transformed abilities inherit golden state from their source normal abilities.
- Review golden bonus execution so effective ability behavior and source progression ownership are both correct.
- Verify cooldown, activate progress, Disable, locks, and charge/channel state do not reset or leak incorrectly across form changes.

### Phase 5 - Presentation sync and rendering
- Extend battle HUD/client sync with effective skin/presentation identity separate from gameplay figure ID.
- Update local player world skin rendering.
- Update opponent dummy rendering.
- Update active HUD figure presentation.
- Verify swapping a transformed figure out/in shows the correct skin each time.
- Verify transforming back restores the correct original Beast Boy variant skin rather than a generic Beast Boy skin.

### Phase 6 - Beast Boy content integration
- Restore `gorilla_transform` to the relevant Beast Boy normal figure loadouts where it was previously replaced as a workaround.
- Add the return-transform ability content.
- Wire Gorilla to `mighty_punch` and temporarily `trashcan_toss`.
- Use the correct Gorilla costs.
- Add/verify the Gorilla skin resource.
- Confirm all three transforming Beast Boy figures use the one shared form definition.

### Phase 7 - AI, validation, regression coverage
- Verify NPC AI uses effective transformed abilities and can execute transformation paths correctly.
- Add focused GameTests/unit tests where appropriate for form resolution and battle behavior.
- Run existing battle regression tests.
- Run the normal build/verification workflow.

Important cases to cover include:

- transform normal -> Gorilla;
- transform Gorilla -> base;
- reordered normal abilities produce correspondingly reordered Gorilla abilities;
- Gorilla costs follow effective abilities after reorder;
- golden state follows the normal counterpart;
- cooldown/progress state survives transformation correctly;
- swap out/in while Gorilla;
- faint while Gorilla;
- revive while Gorilla;
- battle cleanup does not persist Gorilla state to the collectible;
- player skin changes in-world;
- NPC dummy skin changes;
- HUD skin and ability slots change;
- different Beast Boy variants return to their own correct normal skin;
- malformed/missing form mappings are caught by validation;
- non-transforming figures behave exactly as before.

### Phase 8 - Documentation update
After the feature is fully implemented and verified, inspect the documentation under `/docs` and update every document that needs to describe the new implemented mechanic, schemas, runtime ownership, execution behavior, validation, or presentation behavior.

Do not rely on this plan to predetermine which existing docs need edits. The implementing agent should inspect the final implementation and the current `/docs` state at that time, then update all necessary documentation so it matches the shipped behavior.

Once implementation is complete, this roadmap document should remain clearly marked as a historical implementation plan or be updated/moved according to the repository's documentation conventions; canonical system/content docs should describe the actual implemented behavior.

## Non-Goals
This feature does not require:

- making Gorilla a collectible figure;
- persisting forms outside the current battle;
- changing figure stats while transformed;
- timed transformations;
- automatic transformation reversal on swap/faint/revive;
- separate Gorilla golden progression;
- a Beast Boy-specific execution branch in the core ability executor;
- implementing `barrel_toss` early solely for this feature;
- creating a new custom Gorilla entity/model system when a normal skin/presentation swap is sufficient.

## Final Intended Runtime Flow

```text
Normal Beast Boy BattleFigure
    -> player/AI successfully executes gorilla_transform
    -> self transform application: form=gorilla
    -> BattleFigure.activeFormId = gorilla
    -> current reordered source abilities resolve through gorilla counterpart map
    -> transformed abilities use Gorilla costs
    -> golden state remains inherited from each source ability
    -> hotbar/effective AI loadout refreshes
    -> HUD sync reports Gorilla presentation
    -> player/dummy world renderer uses Gorilla skin

Gorilla BattleFigure
    -> can swap, faint, and revive without losing form
    -> successfully executes transform_back
    -> transform application returns to base
    -> BattleFigure.activeFormId cleared
    -> original reordered abilities/costs/presentation resolve again
    -> original Beast Boy variant skin returns
```

## Implementation Principle
Keep the feature generic at the battle-form/effect level and specific only in data.

The code should understand concepts such as `BattleFigure` form, effective ability, source ability, effective cost, and effective presentation. It should not need to understand what a Gorilla or Beast Boy is.
