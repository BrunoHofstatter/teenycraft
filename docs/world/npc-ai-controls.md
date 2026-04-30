# NPC AI Controls

## Purpose
Explain exactly how the `ai` block inside NPC team JSON works, which fields are available, what their values mean in practice, and how to author weak, basic, or strong opponent styles.

## Applies To
- [`src/main/resources/data/teenycraft/npc_teams/`](../../src/main/resources/data/teenycraft/npc_teams)
- [`src/main/java/bruhof/teenycraft/util/NPCTeamLoader.java`](../../src/main/java/bruhof/teenycraft/util/NPCTeamLoader.java)
- [`src/main/java/bruhof/teenycraft/battle/ai/BattleAiProfile.java`](../../src/main/java/bruhof/teenycraft/battle/ai/BattleAiProfile.java)
- [`src/main/java/bruhof/teenycraft/battle/ai/BattleAiGoal.java`](../../src/main/java/bruhof/teenycraft/battle/ai/BattleAiGoal.java)

## Overview
NPC team JSON can include an optional `ai` object.

Example shape:

```json
"ai": {
  "difficulty": 3,
  "aggression": 0.6,
  "swap_bias": 0.4,
  "preferred_range": "mid",
  "mana_discipline": 0.5,
  "risk_tolerance": 0.4,
  "move_speed_mult": 1.0,
  "reaction_ticks": 14,
  "action_commit_ticks": 12,
  "choice_window": 1.6,
  "consider_swap": true,
  "counter_awareness": false,
  "advanced_swap_logic": true,
  "consider_class_disadvantage_swap": true,
  "consider_class_advantage_swap": true,
  "swap_reconsideration_ticks": 48
}
```

Important current rule:
- `difficulty` is optional.
- If `difficulty` is present, it only provides preset defaults.
- Any explicit field in the same `ai` block overrides the preset value.

## Field Guide
### `difficulty`
- Type: integer
- Allowed values: `1` to `5`
- Meaning: optional preset for default timing and reasoning flags

Current preset behavior:
- `1`: slow, loose decisions, weak swapping
- `2`: still slow, starts considering class disadvantage swaps
- `3`: current balanced baseline
- `4`: faster and enables counter awareness
- `5`: fastest default preset

Use this when:
- you want quick setup
- you only want to override a few fields

Skip this when:
- you want total manual control

### `aggression`
- Type: float
- Allowed values: `0.0` to `1.0`
- Meaning: pushes scoring toward offensive actions

Practical feel:
- `0.2`: passive, less eager to pressure
- `0.5`: balanced
- `0.8`: pushes damage choices much harder

### `swap_bias`
- Type: float
- Allowed values: `0.0` to `1.0`
- Meaning: lowers or raises swap willingness on top of the swap logic flags

Practical feel:
- `0.1`: reluctant to swap
- `0.5`: normal
- `0.9`: very eager to take acceptable swap opportunities

This does nothing if `consider_swap` is `false`.

### `preferred_range`
- Type: string
- Allowed values: `auto`, `close`, `mid`, `far`
- Meaning: changes where the AI prefers to stand for ranged pressure

Practical feel:
- `close`: stays tighter
- `mid`: general-purpose default
- `far`: plays safer spacing
- `auto`: lets the AI choose based on action type

### `mana_discipline`
- Type: float
- Allowed values: `0.0` to `1.0`
- Meaning: how cautious the AI is about spending large chunks of mana

Practical feel:
- `0.2`: spends more freely
- `0.5`: balanced
- `0.8`: more conservative with expensive actions

### `risk_tolerance`
- Type: float
- Allowed values: `0.0` to `1.0`
- Meaning: how willing the AI is to keep committing in risky situations

Practical feel:
- `0.2`: safer, more defensive
- `0.5`: balanced
- `0.8`: more willing to commit under pressure

### `move_speed_mult`
- Type: float
- Allowed values: `0.5` to `2.0`
- Meaning: multiplies AI movement speed behavior

Practical feel:
- `0.75`: noticeably slower, more deliberate
- `1.0`: baseline
- `1.2`: fast
- `1.4+`: very aggressive pacing

This changes battle feel a lot.

### `reaction_ticks`
- Type: integer
- Allowed values: `>= 1`
- Meaning: how often the AI reevaluates its plan

Practical feel:
- `24`: slow reaction
- `14`: baseline
- `8`: sharp
- `5`: very reactive

Lower is stronger.

### `action_commit_ticks`
- Type: integer
- Allowed values: `>= 1`
- Meaning: how long the AI tends to stay committed after acting

Practical feel:
- `18`: sluggish, sticks longer
- `12`: baseline
- `8`: snappier
- `5`: very quick to cycle

Lower is stronger and twitchier.

### `choice_window`
- Type: number
- Allowed values: `0.0` to `10.0`
- Meaning: how much lower-scoring options are still allowed into the weighted choice shortlist

Practical feel:
- `2.4`: looser and more random
- `1.6`: baseline
- `1.0`: more focused
- `0.4`: very consistent

Lower is stronger and more deterministic.

### `consider_swap`
- Type: boolean
- Meaning: hard on/off switch for swapping

Behavior:
- `true`: AI may swap if the rest of its logic wants to
- `false`: AI never swaps

Use `false` for teams that should always stay in with their active figure.

### `counter_awareness`
- Type: boolean
- Meaning: enables smarter respect for some enemy answers

Current practical effect:
- helps the AI devalue some debuff/control choices into enemy cleanse situations

Use:
- `false` for lower or simpler AI
- `true` for smarter AI

### `advanced_swap_logic`
- Type: boolean
- Meaning: enables the more proactive swap thresholding style

Practical feel:
- `false`: simpler and more conservative swap logic
- `true`: smarter matchup-oriented swapping

This still does nothing if `consider_swap` is `false`.

### `consider_class_disadvantage_swap`
- Type: boolean
- Meaning: allows swapping away from a bad class matchup

Practical feel:
- `false`: AI ignores class disadvantage as a swap reason
- `true`: AI can leave bad class matchups

### `consider_class_advantage_swap`
- Type: boolean
- Meaning: allows swapping proactively into a better class matchup

Practical feel:
- `false`: no proactive "go to my counter" class swapping
- `true`: AI can swap to gain advantage, not only escape disadvantage

### `swap_reconsideration_ticks`
- Type: integer
- Allowed values: `>= 0`
- Meaning: how often the AI is allowed to seriously rethink swapping

Practical feel:
- `100`: very slow to reconsider swaps
- `48`: baseline
- `24`: quick
- `12`: very active swap thinking

Lower is stronger and more responsive.

This matters only if `consider_swap` is `true`.

## Preset Defaults
If you use only `difficulty`, these are the current default values that get filled in.

### Difficulty 1
- `reaction_ticks: 24`
- `action_commit_ticks: 18`
- `choice_window: 2.4`
- `consider_swap: true`
- `counter_awareness: false`
- `advanced_swap_logic: false`
- `consider_class_disadvantage_swap: false`
- `consider_class_advantage_swap: false`
- `swap_reconsideration_ticks: 100`

### Difficulty 2
- `reaction_ticks: 18`
- `action_commit_ticks: 15`
- `choice_window: 2.0`
- `consider_swap: true`
- `counter_awareness: false`
- `advanced_swap_logic: false`
- `consider_class_disadvantage_swap: true`
- `consider_class_advantage_swap: false`
- `swap_reconsideration_ticks: 72`

### Difficulty 3
- `reaction_ticks: 14`
- `action_commit_ticks: 12`
- `choice_window: 1.6`
- `consider_swap: true`
- `counter_awareness: false`
- `advanced_swap_logic: true`
- `consider_class_disadvantage_swap: true`
- `consider_class_advantage_swap: true`
- `swap_reconsideration_ticks: 48`

### Difficulty 4
- `reaction_ticks: 10`
- `action_commit_ticks: 9`
- `choice_window: 1.2`
- `consider_swap: true`
- `counter_awareness: true`
- `advanced_swap_logic: true`
- `consider_class_disadvantage_swap: true`
- `consider_class_advantage_swap: true`
- `swap_reconsideration_ticks: 30`

### Difficulty 5
- `reaction_ticks: 7`
- `action_commit_ticks: 7`
- `choice_window: 0.8`
- `consider_swap: true`
- `counter_awareness: true`
- `advanced_swap_logic: true`
- `consider_class_disadvantage_swap: true`
- `consider_class_advantage_swap: true`
- `swap_reconsideration_ticks: 18`

## Authoring Patterns
### Simple Weak Opponent

```json
"ai": {
  "difficulty": 1,
  "aggression": 0.35,
  "swap_bias": 0.2,
  "move_speed_mult": 0.9
}
```

### No-Swap Brawler

```json
"ai": {
  "difficulty": 3,
  "consider_swap": false,
  "aggression": 0.8,
  "preferred_range": "close",
  "move_speed_mult": 1.2
}
```

### Defensive Ranged Opponent

```json
"ai": {
  "difficulty": 3,
  "preferred_range": "far",
  "aggression": 0.35,
  "mana_discipline": 0.75,
  "risk_tolerance": 0.2,
  "move_speed_mult": 0.95
}
```

### Fast Smart Swapper

```json
"ai": {
  "difficulty": 4,
  "reaction_ticks": 8,
  "action_commit_ticks": 7,
  "choice_window": 0.9,
  "consider_swap": true,
  "swap_bias": 0.8,
  "consider_class_disadvantage_swap": true,
  "consider_class_advantage_swap": true,
  "swap_reconsideration_ticks": 18
}
```

### Full Manual Control

```json
"ai": {
  "aggression": 0.55,
  "swap_bias": 0.15,
  "preferred_range": "mid",
  "mana_discipline": 0.7,
  "risk_tolerance": 0.3,
  "move_speed_mult": 1.05,
  "reaction_ticks": 11,
  "action_commit_ticks": 10,
  "choice_window": 1.1,
  "consider_swap": true,
  "counter_awareness": true,
  "advanced_swap_logic": true,
  "consider_class_disadvantage_swap": true,
  "consider_class_advantage_swap": false,
  "swap_reconsideration_ticks": 42
}
```

## Practical Advice
- Start with `difficulty: 2` or `difficulty: 3`, then override only the few fields you care about.
- Use `consider_swap: false` instead of trying to fake "no swapping" through other flags.
- Use `choice_window` and `reaction_ticks` carefully together. Very low values on both make the AI feel much sharper.
- `move_speed_mult` changes battle feel more than most other fields.
- `swap_bias` and `swap_reconsideration_ticks` should usually be tuned together.

## Current Limits
- This doc only covers the current implemented control surface.
- Future arena-awareness and more advanced high-difficulty behaviors may add more fields later.
