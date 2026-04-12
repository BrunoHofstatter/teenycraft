# Traits

## Purpose
Document the current ability trait system: which trait ids are actually implemented, where they hook into battle execution, which trait-like behaviors live outside the registry, and which data names in ability JSON are not real trait ids.

## Current Status
Traits are partially centralized. The main trait system is implemented in `TraitRegistry`, but not every trait-like mechanic uses that registry. Some behavior is handled directly in `DamagePipeline` or `AbilityExecutor`, and at least one trait id present in ability data does not appear to have a runtime implementation.

## Player-Facing Behavior
- Traits modify how an ability behaves beyond raw damage and effects.
- Some traits change how an ability executes before it fires.
- Some traits change how damage is packaged, such as multi-hit or group damage.
- Some traits are only available through golden bonuses.
- Trait behavior is battle-runtime only and is not stored as separate long-term figure state.

## Source Of Truth
- [`src/main/java/bruhof/teenycraft/battle/trait/TraitRegistry.java`](../../src/main/java/bruhof/teenycraft/battle/trait/TraitRegistry.java)
- [`src/main/java/bruhof/teenycraft/battle/trait/ITrait.java`](../../src/main/java/bruhof/teenycraft/battle/trait/ITrait.java)
- [`src/main/java/bruhof/teenycraft/battle/AbilityExecutor.java`](../../src/main/java/bruhof/teenycraft/battle/AbilityExecutor.java)
- [`src/main/java/bruhof/teenycraft/battle/damage/DamagePipeline.java`](../../src/main/java/bruhof/teenycraft/battle/damage/DamagePipeline.java)
- [`src/main/java/bruhof/teenycraft/util/AbilityLoader.java`](../../src/main/java/bruhof/teenycraft/util/AbilityLoader.java)
- [`src/main/resources/data/teenycraft/abilities`](../../src/main/resources/data/teenycraft/abilities)
- [`src/main/java/bruhof/teenycraft/TeenyBalance.java`](../../src/main/java/bruhof/teenycraft/TeenyBalance.java)

## Trait Hook Model
The code defines four trait hook interfaces:

- `IExecutionTrait`: can block, delay, or redirect ability execution before normal resolution
- `IPipelineTrait`: modifies the outgoing `DamageResult` before hits are split
- `IMitigationTrait`: would modify mitigation per hit
- `IHitTriggerTrait`: would trigger after a successful hit

Current runtime usage:

- execution traits are actively used through `TraitRegistry.triggerExecutionHooks`
- pipeline traits are actively used through `TraitRegistry.triggerPipelineHooks`
- hit hooks are called by `AbilityExecutor`, but no concrete hit-trigger traits are currently registered
- mitigation hooks exist in the interface and registry helper, but I did not find a live call site using `triggerMitigationHooks`

## Where Traits Are Read
Traits are not handled in only one place.

Current split:

- `AbilityExecutor` runs execution hooks before normal ability resolution
- `DamagePipeline` runs pipeline hooks when building outgoing damage
- `DamagePipeline` also contains special-case logic for the `surprise` trait and golden `trait:surprise`
- `AbilityExecutor.rollTofu` handles `tofu_chance` directly without going through `TraitRegistry`

Because of this split, the trait system is partly centralized but not fully consolidated.

## Registered Trait Ids
These trait ids are explicitly registered in `TraitRegistry.init()`:

- `multi_hit`
- `group_damage`
- `blue`
- `charge_up`
- `instant_cast_chance`
- `surprise`
- `undodgeable`
- `activate`

## Implemented Traits
### `multi_hit`
Type: pipeline trait

Current behavior:

- sets `DamageResult.hitCount` from the first trait parameter
- damage is later split across that many hits
- crit and mitigation are then resolved per hit, not once for the whole attack

Current examples:

- `multi_hit`
- `punchies`
- `jiu_jitsu`

### `group_damage`
Type: pipeline trait

Current behavior:

- marks the outgoing hit as group damage
- battle then distributes that hit across the opposing alive team instead of only the current target
- group damage breaks `flight` on affected victims before mitigation

Current examples:

- `missile_barrage`
- `whale_drop`
- `jiu_jitsu`

### `blue`
Type: execution trait

Current behavior:

- prevents the ability from firing instantly
- converts the cast into a blue-channel state stored on `BattleState`
- precomputes total damage, total healing, total mana, hit count, and interval
- spends the current mana pool across a channel over time
- uses `TeenyBalance` blue-channel knobs for hit count, interval, and damage multiplier

Important current detail:

- golden `trait:blue:<param>` overrides the base trait parameter when present

Current example:

- `heat_vision`

### `charge_up`
Type: execution trait

Current behavior:

- prevents immediate execution
- spends mana up front
- stores the pending ability, slot, golden flag, and target in `BattleState`
- finishes later through the charge-completion logic in `ModEvents.onLivingTick`

Important current detail:

- if the ability has `instant_cast_chance`, or a golden bonus adds `trait:instant_cast_chance`, the charge may be skipped
- the charge duration uses the first trait parameter only

Current examples:

- `charge_up`
- `chattering_teeth`
- `laser_eyes`
- `laser_eyes2`
- `curse`

### `instant_cast_chance`
Type: registered marker trait

Current behavior:

- this trait does not execute by itself
- it is checked inside the `charge_up` trait logic
- when present, it gives a chance to skip charging and cast immediately
- the same behavior can be added through a golden bonus using `trait:instant_cast_chance`

Important current detail:

- I did not find any base ability JSON currently declaring `instant_cast_chance` in its normal `traits` array
- it is currently used through golden bonuses on abilities such as `laser_eyes`, `laser_eyes2`, and `curse`

### `surprise`
Type: registered marker trait

Current behavior:

- the marker is checked in `DamagePipeline`
- when present, final raw damage gets a random integer variance around its base value
- golden `trait:surprise` is also supported in the damage pipeline

Important current detail:

- the trait id is `surprise`
- `burp_surprise` is an ability id, not a trait id

Current example:

- `burp_surprise`

### `undodgeable`
Type: pipeline trait

Current behavior:

- marks the outgoing hit as undodgeable
- undodgeable hits skip the victim dodge-mitigation roll
- golden `trait:undodgeable` is also supported directly in `DamagePipeline`

Current examples:

- `freeze`
- `kiss`
- `waffle`
- golden variants of abilities such as `birdarang`, `quick_punch`, `super_sockem`, `punchies`, and `jiu_jitsu`

### `activate`
Type: execution trait

Current behavior:

- uses `BattleState.slotProgress` as a per-slot activation counter
- each use increments progress
- the ability does not execute until the required count is met
- once the requirement is met, progress resets to `0` and normal execution proceeds
- `DamagePipeline` also multiplies the final raw damage for activated abilities by the required-count scaling rule

Important current detail:

- only the first parameter is used as the required cast count
- some JSON entries include extra parameters, but the current code does not read them for `activate`

Current example:

- `mighty_punch`

## Trait-Like Logic Outside TraitRegistry
### `tofu_chance`
This behaves like a trait but is not registered in `TraitRegistry`.

Current behavior:

- `AbilityExecutor.rollTofu` scans the ability's trait list manually for `tofu_chance`
- golden bonuses can also add `trait:tofu_chance:x,y`
- the first value multiplies tofu spawn chance
- the second value multiplies tofu power

Important current detail:

- I did not find any base ability JSON currently declaring `tofu_chance` as a normal trait
- it is currently used through golden bonuses on `mighty_punch` and `burp_surprise`

## Trait Ids Present In Data But Not Confirmed As Implemented
### `instant_cast`
I found `trait:instant_cast` in multiple golden bonus entries, especially on utility abilities such as `freeze`, `kiss`, `root`, `shock`, `poison`, `bar_deplete`, `defense_down`, `multi_hit`, `remote_mine`, and `waffle`.

However:

- `TraitRegistry` does not register `instant_cast`
- I did not find another runtime handler for `trait:instant_cast`

So the current code suggests that `instant_cast` is data-authored but not actually implemented at runtime.

## Names That Are Not Trait Ids
These names can be confused with traits, but they are not trait ids in the current code:

- `burp_surprise`: ability id, not a trait id
- `multi_hit`: both an ability id and a trait id, depending on context
- `charge_up`: both an ability id and a trait id, depending on context

When documenting or authoring JSON, the `traits` array must use the runtime trait ids, not ability ids unless they intentionally match.

## Golden Trait Behavior
Golden ability data can add trait behavior in two ways:

1. override parameters for a base trait already present in the ability
2. add an execution trait that is not present in the base ability at all

Current runtime examples:

- `trait:blue:0.7` overrides the `blue` trait parameter
- `trait:instant_cast_chance` adds charge-skip behavior to some golden abilities
- `trait:undodgeable` adds undodgeable behavior to some golden abilities
- `trait:surprise` would be honored by the damage pipeline if present

Pipeline-side golden handling is not fully uniform:

- `undodgeable` and `surprise` have direct golden handling in `DamagePipeline`
- execution traits are handled through `TraitRegistry.triggerExecutionHooks`
- `tofu_chance` is handled manually in `AbilityExecutor.rollTofu`
- `instant_cast` currently appears data-only and unresolved

## Current Ability Coverage
Traits currently appear on a relatively small subset of abilities.

Base traits in ability JSON currently include:

- `multi_hit`
- `group_damage`
- `blue`
- `charge_up`
- `surprise`
- `undodgeable`
- `activate`

Golden-only trait usage currently includes:

- `instant_cast`
- `instant_cast_chance`
- `undodgeable`
- `blue`
- `tofu_chance`

## Balance Hooks
- `ACTIVATE_DAMAGE_MULT`
- `CHARGE_UP_MULT_PER_SEC`
- `BASE_CHARGE_DELAY`
- `INSTANT_CAST_CHANCE_PERCENTAGE`
- `BLUE_TICKS_PER_MANA`
- `BLUE_TICKS_FLAT`
- `BLUE_DAMAGE_MULT`
- `BLUE_BASE_INTERVAL`
- tofu chance and tofu power constants

## Design Notes
- Trait docs need to separate true registry traits from trait-like logic handled elsewhere.
- New traits should preferably be implemented through `TraitRegistry` instead of adding more direct string checks in `AbilityExecutor` or `DamagePipeline`.
- Marker traits are valid, but they should still be documented where their actual runtime logic lives.
- Data-authored trait ids that have no runtime implementation should be called out explicitly instead of being documented as if they are working.

## Open Questions
- whether `tofu_chance` should be migrated into `TraitRegistry`
- whether `instant_cast` should be implemented or removed from data
- whether mitigation and hit-trigger trait hooks should be used for future traits or removed until needed
- whether traits should get a formal schema reference separate from abilities

## Planned Additions
- add per-trait JSON examples once the trait set is more stable
- revisit this page if `instant_cast` is implemented or if `tofu_chance` moves into the registry
- update the page if any hit-trigger or mitigation traits are added in live code
