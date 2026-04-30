# Ability JSON Reference

## Purpose
Technical authoring reference for ability JSON under `src/main/resources/data/teenycraft/abilities`.
This page documents the live loader contract, effect ids, trait ids, parameter usage, and golden bonus runtime behavior.

## Source Of Truth
- [`src/main/java/bruhof/teenycraft/util/AbilityLoader.java`](../../src/main/java/bruhof/teenycraft/util/AbilityLoader.java)
- [`src/main/java/bruhof/teenycraft/battle/executor/BattleAbilityContext.java`](../../src/main/java/bruhof/teenycraft/battle/executor/BattleAbilityContext.java)
- [`src/main/java/bruhof/teenycraft/battle/executor/BattleAbilityExecution.java`](../../src/main/java/bruhof/teenycraft/battle/executor/BattleAbilityExecution.java)
- [`src/main/java/bruhof/teenycraft/battle/executor/BattleDamageResolver.java`](../../src/main/java/bruhof/teenycraft/battle/executor/BattleDamageResolver.java)
- [`src/main/java/bruhof/teenycraft/battle/effect/EffectApplierRegistry.java`](../../src/main/java/bruhof/teenycraft/battle/effect/EffectApplierRegistry.java)
- [`src/main/java/bruhof/teenycraft/battle/effect/EffectCalculator.java`](../../src/main/java/bruhof/teenycraft/battle/effect/EffectCalculator.java)
- [`src/main/java/bruhof/teenycraft/battle/effect/EffectRegistry.java`](../../src/main/java/bruhof/teenycraft/battle/effect/EffectRegistry.java)
- [`src/main/java/bruhof/teenycraft/battle/trait/TraitRegistry.java`](../../src/main/java/bruhof/teenycraft/battle/trait/TraitRegistry.java)
- [`src/main/java/bruhof/teenycraft/battle/damage/DamagePipeline.java`](../../src/main/java/bruhof/teenycraft/battle/damage/DamagePipeline.java)
- [`src/main/java/bruhof/teenycraft/capability/BattleState.java`](../../src/main/java/bruhof/teenycraft/capability/BattleState.java)
- [`src/main/java/bruhof/teenycraft/TeenyBalance.java`](../../src/main/java/bruhof/teenycraft/TeenyBalance.java)

## Minimal Shape
```json
{
  "id": "example_ability",
  "name": "Example Ability",
  "description": "Optional tooltip text",
  "golden_description": "Optional golden tooltip text",
  "texture_index": 999,
  "particle": "minecraft:happy_villager",
  "particle_count": 5,
  "hit_type": "none",
  "raycast_delay_tier": 0,
  "range_tier": 4,
  "damage_tier": 7,
  "effects_on_opponent": [
    { "id": "stun", "params": [1.0] }
  ],
  "effects_on_self": [
    { "id": "power_up", "params": [1.0] }
  ],
  "traits": [
    { "id": "charge_up", "params": [1.2] }
  ],
  "golden_bonus": [
    "self:power_up:0.3",
    "trait:instant_cast_chance"
  ]
}
```

## Top-Level Fields
| Field                 | Required | Type   | Runtime notes                                                                                                  |
|-----------------------|----------|--------|----------------------------------------------------------------------------------------------------------------|
| `id`                  | yes      | string | Unique ability id. Reload validation checks duplicates.                                                        |
| `name`                | no       | string | Defaults to `id` if omitted. UI-facing only.                                                                   |
| `description`         | no       | string | Figure screen tooltip only. No battle logic.                                                                   |
| `golden_description`  | no       | string | Figure screen tooltip only. No battle logic.                                                                   |
| `hit_type`            | yes      | string | Supported live values: `melee`, `ranged`, `raycasting`, `none`.                                                |
| `damage_tier`         | yes      | int    | Used by `DamagePipeline`. `0` means no direct hit damage, but effect logic can still use the ability normally. |
| `raycast_delay_tier`  | no       | int    | Defaults to `0`. Only matters when `hit_type` is `ranged` or `raycasting`.                                     |
| `range_tier`          | no       | int    | Defaults to `4`. Used for ranged targeting distance.                                                           |
| `texture_index`       | no       | int    | Defaults to `0`. Used by ability icon model overrides.                                                         |
| `particle`            | no       | string | Loader-supported. Particle ids are intentionally not listed on this page.                                      |
| `particle_count`      | no       | int    | Defaults to `10`.                                                                                              |
| `effects_on_opponent` | no       | array  | Array of effect objects.                                                                                       |
| `effects_on_self`     | no       | array  | Array of effect objects.                                                                                       |
| `traits`              | no       | array  | Array of trait objects.                                                                                        |
| `golden_bonus`        | no       | array  | Array of strings, parsed on reload.                                                                            |

## `hit_type`
| Value        | Runtime meaning                                                 |
|--------------|-----------------------------------------------------------------|
| `melee`      | Uses the melee attack path and requires a direct target entity. |
| `ranged`     | Treated as ranged by `BattleAbilityContext`.                    |
| `raycasting` | Also treated as ranged by `BattleAbilityContext`.               |
| `none`       | Self-targeted cast. No opponent target is required.             |

Current note:

- `ranged` and `raycasting` are both considered ranged by runtime. The code does not currently give them different targeting behavior by name alone.

## Numeric Tier Fields
### `damage_tier`
`TeenyBalance.getDamageMultiplier(...)` currently uses:

| Tier | Mult   |
|------|--------|
| `0`  | `0.00` |
| `1`  | `0.82` |
| `2`  | `0.85` |
| `3`  | `0.88` |
| `4`  | `0.91` |
| `5`  | `0.94` |
| `6`  | `0.97` |
| `7`  | `1.00` |
| `8`  | `1.03` |
| `9`  | `1.06` |
| `10` | `1.09` |
| `11` | `1.12` |
| `12` | `1.15` |
| `13` | `1.18` |

### `raycast_delay_tier`
If `raycast_delay_tier > 0`, runtime schedules a delayed projectile.
Current delay factor table:

| Tier | Value  |
|------|--------|
| `0`  | `0.0`  |
| `1`  | `0.25` |
| `2`  | `0.5`  |
| `3`  | `0.75` |
| `4`  | `1.0`  |
| `5`  | `1.25` |
| `6`  | `1.5`  |
| `7`  | `2.0`  |
| `8`  | `2.5`  |
| `9`  | `3.0`  |
| `10` | `3.5`  |
| `11` | `4.0`  |

Current formula:

- `delayTicks = floor(distanceToTarget * tierValue)`

### `range_tier`
`TeenyBalance.getRangeValue(...)` currently uses:

| Tier | Range |
|------|-------|
| `1`  | `5`   |
| `2`  | `7`   |
| `3`  | `9`   |
| `4`  | `11`  |
| `5`  | `13`  |
| `6`  | `15`  |
| `7`  | `17`  |
| `8`  | `19`  |

Current fallback:

- missing or invalid `range_tier` resolves to `11`

## Effect Object Schema
Each `effects_on_self` or `effects_on_opponent` entry is:

```json
{ "id": "effect_id", "params": [1.0, 1.0] }
```

Rules:

- `id` is required.
- `params` is optional.
- params are loaded as floats.
- reload validation checks that the effect id exists in `EffectApplierRegistry`.
- reload validation does not currently enforce param count per effect.

## Trait Object Schema
Each `traits` entry is:

```json
{ "id": "trait_id", "params": [1.0] }
```

Rules:

- `id` is required.
- `params` is optional.
- params are loaded as floats.
- reload validation checks that the trait id exists in `TraitRegistry`.
- reload validation does not currently enforce param count per trait.

## Supported Effect Ids
These ids are currently accepted by reload validation because they are registered in `EffectApplierRegistry`.

### Common ability-data ids
| Effect id      | Params used now                                     | Meaning                                                                                               |
|----------------|-----------------------------------------------------|-------------------------------------------------------------------------------------------------------|
| `heal`         | `1`: `[heal_mult]`                                  | Heal amount multiplier.                                                                               |
| `group_heal`   | `1`: `[heal_mult]`                                  | Total group heal multiplier.                                                                          |
| `bar_fill`     | `1`: `[mana_mult]`                                  | Mana restore multiplier.                                                                              |
| `bar_deplete`  | `1`: `[deplete_mult]`                               | Mana drain multiplier.                                                                                |
| `power_up`     | `1`: `[power_mult]`                                 | Flat power-up magnitude multiplier.                                                                   |
| `power_down`   | `1`: `[power_mult]`                                 | Flat power-down magnitude multiplier.                                                                 |
| `defense_up`   | `1`: `[shared_mult]`                                | Same param scales both duration and defense percent. Extra params are currently ignored.              |
| `defense_down` | `1`: `[shared_mult]`                                | Same param scales both duration and defense percent. Extra params are currently ignored.              |
| `luck_up`      | `2`: `[magnitude_mult, duration_mult]`              | First param scales luck percent, second scales duration.                                              |
| `stun`         | `1`: `[duration_mult]`                              | Stun duration multiplier.                                                                             |
| `root`         | `1`: `[duration_mult]`                              | Root duration multiplier.                                                                             |
| `disable`      | `1`: `[duration_mult]`                              | Disables the target's current active figure slot.                                                     |
| `freeze`       | `2`: `[burn_mult, duration_mult]`                   | First param scales mana burn percent, second scales freeze duration.                                  |
| `poison`       | `1`: `[amount_and_interval_mult]`                   | First param scales tick count and interval. Extra params are currently ignored.                       |
| `shock`        | `2`: `[amount_and_interval_mult, stun_length_mult]` | First param scales count and interval, second scales each shock mini-stun length.                     |
| `dance`        | `1`: `[duration_mult]`                              | Dance duration multiplier.                                                                            |
| `curse`        | `1`: `[duration_mult]`                              | Curse duration multiplier.                                                                            |
| `cleanse`      | `1`: `[immunity_duration_mult]`                     | Cleanses debuffs/control, then applies `cleanse_immunity` with this duration scaling.                 |
| `kiss`         | `1`: `[duration_mult]`                              | Kiss duration multiplier.                                                                             |
| `shield`       | `1`: `[duration_mult]`                              | Shield duration multiplier.                                                                           |
| `dodge_smoke`  | `1`: `[duration_mult]`                              | Duration multiplier. Smoke charges come from balance, not params. Extra params are currently ignored. |
| `cuteness`     | `2`: `[duration_mult, reflect_mult]`                | First param scales duration, second scales reflect percent.                                           |
| `reflect`      | `2`: `[duration_mult, reflect_mult]`                | First param scales duration, second scales both damage reduction and reflected damage.                |
| `pets`         | `2`: `[duration_mult, pet_damage_mult]`             | First param scales pet lifetime, second scales pet shot damage.                                       |
| `remote_mine`  | `1`: `[damage_mult]`                                | Scales the mine's stored max damage snapshot.                                                         |
| `waffle`       | `2`: `[proc_chance, duration_mult]`                 | First param is chance from `0.0` to `1.0`, second scales duration if it procs.                        |
| `health_radio` | `2`: `[interval_mult, heal_mult]`                   | First param scales interval, second scales the total heal payload.                                    |
| `power_radio`  | `2`: `[interval_mult, power_mult]`                  | First param scales interval, second scales the total power-up payload.                                |
| `self_shock`   | `1`: `[damage_mult]`                                | Self-damage multiplier.                                                                               |
| `self_damage`  | `1`: `[recoil_mult]`                                | Recoil as a fraction of this ability's raw damage.                                                    |
| `tofu_spawn`   | `1`: `[power_mult]`                                 | Tofu power multiplier.                                                                                |
| `flight`       | `1`: `[duration_mult]`                              | Flight duration multiplier.                                                                           |

### Helper ids that are valid but not current main stored effect ids
| Effect id       | Params used now                     | Meaning                                                                                            |
|-----------------|-------------------------------------|----------------------------------------------------------------------------------------------------|
| `waffle_chance` | `2`: `[proc_chance, duration_mult]` | Alias of `waffle`.                                                                                 |
| `self:heal`     | `1`: `[heal_mult]`                  | Alias of `heal`.                                                                                   |
| `self:power_up` | `1`: `[power_mult]`                 | Alias of `power_up`.                                                                               |
| `eagle`         | `1`: `[shared_mult]`                | 50% chance to heal, 50% chance to power up.                                                        |
| `self:eagle`    | `1`: `[shared_mult]`                | Alias of `eagle`.                                                                                  |
| `dispel`        | `0`                                 | Removes buffs from the target. Registered and valid, but not used by current shipped ability JSON. |

### Important effect authoring notes
- Some current JSON files still pass extra params that runtime ignores. Current examples: `defense_up`, `dodge_smoke`, and `poison`.
- Helper ids such as `disable` and `remote_mine` do not store exactly that id in runtime state. They convert into figure-specific runtime ids like `disable_0` or `remote_mine_2`.
- Opponent effects are applied after hit resolution. If the hit is dodged, opponent effects do not apply.

## Supported Trait Ids
These ids are currently accepted by reload validation because they are registered in `TraitRegistry`.

| Trait id              | Params used now                       | Meaning                                                                             |
|-----------------------|---------------------------------------|-------------------------------------------------------------------------------------|
| `multi_hit`           | `1`: `[hit_count]`                    | Sets outgoing hit count.                                                            |
| `group_damage`        | `0`                                   | Marks the hit as group damage.                                                      |
| `blue`                | `1`: `[interval_mult]`                | Converts the cast into blue channeling.                                             |
| `charge_up`           | `1`: `[charge_time_mult]`             | Converts the cast into a charge-up cast.                                            |
| `instant_cast_chance` | `0`                                   | Marker checked by `charge_up`. Gives instant-cast chance on charge-up abilities.    |
| `instant_cast`        | `0`                                   | Validated compatibility alias only. Current runtime behavior is a no-op.            |
| `surprise`            | `0`                                   | Marker checked in `DamagePipeline` for random damage variance.                      |
| `tofu_chance`         | `2`: `[chance_mult, tofu_power_mult]` | Used by `rollTofu(...)`, not by the normal trait registry hooks.                    |
| `undodgeable`         | `0`                                   | Makes the hit skip dodge.                                                           |
| `activate`            | `1`: `[required_casts]`               | Cast counter before the ability actually fires. Extra params are currently ignored. |

### Important trait authoring notes
- `multi_hit`, `group_damage`, and `undodgeable` are pipeline-level traits.
- `blue`, `charge_up`, and `activate` are execution-time traits.
- `surprise` and `instant_cast_chance` are marker traits.
- `tofu_chance` is valid and live, but it is handled manually in `BattleAbilityExecution.rollTofu(...)`.
- Current validation only checks the trait id, not the param count.

## `golden_bonus` String Format
Each entry is a string:

```text
scope:target_id:param1,param2,...
```

Supported scopes:

| Scope      | Target id type | Params required |
|------------|----------------|-----------------|
| `self`     | effect id      | yes             |
| `opponent` | effect id      | yes             |
| `trait`    | trait id       | no              |

Examples:

- `self:power_up:0.3`
- `opponent:stun:1.3`
- `trait:blue:0.7`
- `trait:instant_cast_chance`

Reload validation currently checks:

- the string format
- that the scope exists
- that self/opponent target ids exist in `EffectApplierRegistry`
- that trait target ids exist in `TraitRegistry`

Reload validation currently does not check:

- per-effect param count
- per-trait param count
- whether the target id makes sense for the ability's `hit_type`

## Golden Runtime Behavior
### `self` and `opponent` scope
Self and opponent golden bonuses are not param overrides.
They are extra effect applications that run after the base `effects_on_self` or `effects_on_opponent` list.

Current order:

1. Base self/opponent effects apply first.
2. Golden self/opponent bonuses apply second, one by one.

What happens when the golden bonus targets the same effect id as the base effect depends on normal `BattleState.applyEffect(...)` reapply rules:

- stackable effects add magnitude
- non-stackable effects overwrite magnitude
- `power` keeps the larger value
- duration only increases if the new duration is longer

Current practical examples:

- `self:power_up:0.3` adds more `power_up` because `power_up` stacks magnitude.
- `opponent:power_down:...` would also add because `power_down` stacks magnitude.
- `self:dance:1.3` reapplies `dance`, so it refreshes or extends the same effect instead of creating a second independent stack.
- `self:flight:1.3` reapplies `flight`; it does not create two flights.
- `self:health_radio:1.0,0.3` reapplies `health_radio`; because `health_radio` does not stack magnitude, the new application replaces the interval payload fields using normal reapply rules.

### `trait` scope
Trait bonuses do not have one universal merge rule. The behavior depends on the specific trait code path.

Current live rules:

- if the golden trait targets a base execution trait that already exists, execution uses the golden params instead of the base params
- if the golden trait targets an execution trait not present in the base ability, that execution trait is added and executed
- `undodgeable` and `surprise` have special presence checks in `DamagePipeline`
- `tofu_chance` is multiplied into tofu spawn chance and tofu power inside `rollTofu(...)`
- `instant_cast` is still only a validated compatibility alias
- other pipeline traits do not currently have generic golden-bonus support

Current examples:

- `trait:blue:0.7` replaces the base `blue` trait params during execution.
- `trait:instant_cast_chance` adds instant-cast chance to a `charge_up` ability.
- `trait:undodgeable` adds undodgeable behavior without changing base trait arrays.
- `trait:tofu_chance:1.5,1.2` multiplies tofu chance and tofu power; it does not replace any base effect.

Current non-example:

- `trait:multi_hit:...` would validate, but there is no current generic golden pipeline override for `multi_hit`.

## Current Reapply Rules That Explain "Add" vs "Replace"
These are the active rules in `BattleState.applyEffect(...)` for figure effects:

- if the effect already exists and `BattleEffect.canStackMagnitude()` is `true`, `magnitude += newMagnitude`
- otherwise `magnitude = newMagnitude`
- `power = max(oldPower, newPower)`
- `duration = -1` forces infinity
- otherwise duration only changes when the new duration is longer

Current effect ids that stack magnitude:

- `power_up`
- `power_down`

Current effect ids that do not stack magnitude:

- everything else unless a future `BattleEffect` overrides `canStackMagnitude()`

So the current authoring rule is:

- golden bonus "adds" only when it reapplies a stackable effect
- golden bonus "replaces or refreshes" when it reapplies a non-stackable effect

## Current Data Oddities
- Some ability files contain copy-paste `name` values that do not match the ability id. Runtime does not care.
- Some effect entries contain extra params that runtime ignores.
- `activate` currently only reads the first param even though shipped data includes a second one on `mighty_punch`.
- `ranged` and `raycasting` are both treated as ranged by the current runtime.
- Particle fields exist, but this page intentionally does not maintain a particle id list.
