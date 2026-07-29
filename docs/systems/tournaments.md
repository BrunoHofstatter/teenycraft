# Tournaments

## Purpose
Document the final intended design for Teeny Craft tournaments: multi-match runs with a fixed six-figure roster, tournament-persistent HP, limited accessories, physical tournament rooms, authored opponents, choice paths, final rewards, and aggregated battle results. This is a system-design document, not an implementation plan. As the feature is implemented, its status sections should be updated while the intended player-facing rules remain canonical.

## Current Status
- Planned.
- The repository does not currently implement tournament run state, tournament rooms, tournament choice nodes, tournament setup or status screens, or tournament completion rewards.
- Several required foundations already exist: the real-time battle engine, `BattleFigure` snapshots, Titan Manager figure and accessory storage, accessory mastery and runtime, JSON NPC teams with fixed figure levels and AI, Teenyverse arena sessions, and authored arena structures.
- Mid-battle disconnect and crash recovery, the planned give-up item, tournament wagers, and normal-battle result screens are explicitly deferred. They are recorded here so they are not forgotten.

## Player-Facing Behavior
A tournament is an authored sequence of matches and choice nodes inside a physical Teenyverse tournament room.

Core rules:

- The player always selects exactly `6` figures for the tournament roster.
- Each tournament defines its own accessory-slot count. It may allow any authored number, including `0`.
- Tournament accessories are selected from the Titan Manager `Accessories` storage page.
- Duplicate accessory ids are not allowed in one tournament loadout.
- Before each match, the player selects between `1` and `3` living roster figures, their team order, the opening figure, the eligible group combo for the first two slots, and optionally one available tournament accessory.
- Figure HP and fainted state persist between tournament matches.
- Mana, battery, tofu, effects, cooldowns, arena pickups, temporary HP, and other battle-only state reset between matches unless a tournament choice explicitly grants a next-battle starting benefit.
- A chosen accessory normally becomes spent when its match begins and cannot be used again unless restored or preserved by a tournament choice.
- Losing any match immediately ends the tournament.
- The final match is an authored boss battle with short pre-battle dialogue.
- Completing the tournament grants an authored figure and an authored amount of Teeny Coins.
- Completion rewards are hidden before completion so the figure reward cannot spoil story content. The coin reward is hidden as well.
- Every successful completion, including repeat completions, grants the same figure and coin reward.

## Core Tournament Identity
Tournaments preserve the main identity of the source-game mode while extending it for Teeny Craft:

- a six-figure endurance roster
- a tournament-specific limited accessory loadout
- an authored order and number of matches
- authored NPC opponent names, portraits, teams, levels, AI, and boss placement
- persistent roster damage and fainting across matches
- choice nodes between some matches
- a final boss interaction and reward

The intended tournament content direction is to preserve the exact match count, match order, opponent identity, portrait direction, and final boss position of a referenced tournament where appropriate. Teeny Craft may replace or expand the original automatic buff steps with its two-path category system.

Tournament difficulty is authored through the NPC team data and tournament sequence. Opponent teams use fixed figure levels and authored builds; they do not automatically scale to the player's level.

## Tournament Definition
Each tournament definition is expected to identify at least:

- tournament id and display name
- story or progression requirement
- tournament room structure and theme
- accessory-slot count
- ordered match and choice nodes
- NPC identity and NPC team id for every match
- arena used by every match
- whether a match hides its enemy team preview
- choice categories and hidden choice-node strength
- final boss dialogue
- completion figure reward
- completion Teeny Coin reward
- per-match figure XP reward

Tournament content that is intended to vary by tournament should be data-driven. Numeric balance constants, category base values, random-factor ranges, probabilities, clamps, and shared multipliers belong in `TeenyBalance.java`.

## Tournament Entry And Setup
The entry flow uses a tournament setup screen backed by the Titan Manager rather than the normal three active team slots.

Setup rules:

1. Select exactly six figures from the Titan Manager figure collection.
2. Select the exact number of accessories required by the tournament from the Titan Manager `Accessories` storage page.
3. Reject duplicate accessory ids.
4. Confirm the tournament loadout.
5. Snapshot the selected figure and accessory data into the tournament run.
6. Enter the tournament's physical Teenyverse room.

A tournament with `0` accessory slots skips accessory selection entirely. Its status UI states that accessories are disabled, its match setup does not offer an accessory selection, Accessory category nodes cannot appear, and Gambit cannot roll accessory-loss or accessory-reward outcomes.

The six figures and selected accessories are tournament snapshots. Changes made to the original items after tournament entry must not alter the active run. The exact long-term locking or instance-identity mechanism is an implementation concern, but the player-facing tournament state must remain stable for the run.

The Titan Manager's normal three active team slots and equipped accessory slot do not control tournament matches. They remain the normal collection and general battle setup, while the tournament has its own temporary roster and match loadouts.

## Physical Tournament Room
The tournament flow should be represented by a physical, authored Teenyverse room rather than only a sequence of menus.

The room is a themed corridor or course containing:

- the current NPC challenger blocking progress
- visible future corridor sections and later NPCs
- physical two-path choice forks
- authored decoration around each match and choice section
- a final boss area
- a completion and exit area

The player should be able to see past the current NPC. Future choices and later challengers can be visible farther down the room, creating anticipation and making progress spatially understandable.

Progress is controlled by invisible server-authoritative barriers, not by NPC collision alone:

- an invisible barrier plane blocks passage behind an undefeated NPC
- after victory, the NPC moves to the side and the barrier is removed
- at a choice fork, both category paths are visible
- selecting one path opens that route and permanently closes the other route for the run
- both paths can rejoin before the next authored node

Tournament rooms should eventually be instanced per run or player, using separated Teenyverse slots in the same general spirit as current arena slots. This prevents one player's NPC state, barriers, or choice paths from affecting another player.

Tournament areas must eventually deny normal block breaking, placement, and unintended route bypass. Exact room-protection and anti-bypass rules can be developed with the broader Teenyverse room system.

## NPC And Match Progression
Every match is represented by an authored NPC with at least:

- id
- displayed name
- portrait or face
- NPC team reference
- arena reference
- optional dialogue
- boss status

The final boss always has a short pre-battle conversation before team selection and battle. Ordinary opponents may also use short dialogue where authored.

After the player interacts with the current NPC:

1. Show the opponent introduction and any dialogue.
2. Show the opponent's team preview unless that match hides it.
3. Let the player choose the tournament match loadout.
4. Start the normal arena battle through the shared battle runtime.
5. On victory, write the match result back into tournament state and return the player to the tournament room.
6. Move the defeated NPC aside and open the next corridor section.
7. On defeat, end the tournament immediately.

The room is the persistent presentation layer, while the actual fight can continue using authored arena slots and the normal paired battle engine.

## Opponent Preview
By default, pre-match setup shows:

- opponent name and portrait
- all opposing figures
- opposing figure levels
- opposing figure classes

Specific authored matches may hide the enemy team as a special challenge. When hidden:

- opposing figures are not shown
- their levels are not shown
- their classes are not shown

The match may still show the NPC's name and portrait unless a separate story rule says otherwise.

## Match Team Selection
Before every match, the player selects between one and three living figures from the six-figure tournament roster.

Match setup includes:

- selected figures
- team order
- opening figure
- the group-combo choice available to the first two ordered slots
- one available accessory or no accessory
- current persistent HP and fainted status
- opponent preview when enabled
- pending next-battle benefits or penalties

The player is allowed to enter with only one or two figures. This matters when several roster figures have fainted and allows desperate late-tournament victories instead of treating the run as lost while living figures remain.

Fainted figures cannot be selected until revived.

## Tournament HP And Fainting
Tournament HP is stored in a six-slot tournament health ledger. It is not written as long-term HP onto the persistent figure item.

Match flow for HP:

1. Build normal `BattleFigure` snapshots for the selected match team.
2. Inject each selected figure's starting HP from the tournament ledger.
3. Run the battle through the normal authoritative HP, faint, swap, and defeat flow.
4. On victory, write every selected figure's remaining normalized HP back into its tournament roster slot.
5. Keep unselected figures' tournament HP unchanged.
6. Keep fainted figures at `0` until a tournament Revive effect returns them.

Tournament healing cannot revive a fainted figure unless the result is explicitly a Revive result.

Temporary maximum HP and overheal do not persist as extra HP between matches. At match end, stored tournament HP is capped to the figure's normal tournament max HP. Temporary HP can absorb damage during the match but cannot be carried forward.

Effects belong to the battle participant side and are not cleared by swapping figures during that battle. Unless explicitly granted as a pending next-battle benefit or penalty, effects reset when the match ends.

## State That Persists Between Matches
The tournament retains:

- current node
- six selected figure snapshots
- each figure's current HP and fainted state
- selected accessory slots
- accessory availability, spent, preserved, and lost state
- pending next-battle benefits or penalties
- completed choice categories and rolled outcomes
- completed match reports
- accumulated tournament summary data

The tournament does not normally retain:

- mana
- battery
- tofu
- ability cooldowns
- active battle effects
- arena pickup state
- accessory activation state
- temporary maximum HP
- temporary overheal
- swap cooldown
- opponent state from a completed match

There are no run-long stacking tournament buffs in the current design. Choice results are either immediate tournament-state changes, such as healing, revival, accessory changes, or coins, or explicit starting benefits and penalties for the next match.

## Tournament Accessories
The tournament accessory-slot count is authored per tournament and may be `0`.

Accessory rules:

- Accessories are selected from the Titan Manager `Accessories` storage page during tournament setup.
- Duplicate accessory ids are forbidden.
- A player may choose one available tournament accessory for a match or enter without one.
- The chosen accessory uses the player's normal unlocked accessory mastery tier.
- The chosen accessory behaves normally during that match and may activate as often as the battle battery allows.
- The accessory becomes spent when the match successfully begins, whether or not the player activates it.
- A preserved accessory consumes its preservation on match entry instead of becoming spent, so it remains available for one additional match.

Accessory slot states are:

- `available`: may be selected for a match
- `preserved`: available, and its next match use consumes preservation instead of spending it
- `spent`: already used and unavailable unless restored or replaced
- `lost`: removed by a Gambit penalty and unavailable unless its slot is replaced

Preservation only protects against normal spending from one match use. It does not protect the accessory from Gambit's permanent accessory-loss penalty.

## Choice Nodes
A choice node presents two physical paths. Each path displays only its category identity, not its exact result or hidden strength.

Choice-node rules:

- The player can inspect both categories before committing.
- The exact result is rolled only after the player enters, presses, or otherwise commits to one path.
- The unchosen path closes permanently for that run.
- A choice node has one hidden authored strength multiplier shared by both offered categories.
- Hidden strength grows with story and tournament difficulty rather than being exposed as a random stronger or weaker path.
- Gambit visibly communicates its fixed `50% Reward / 50% Penalty` rule.
- Other categories show their identity but not the exact rolled result.
- Invalid results are removed or rerolled according to the category's applicability rules.
- The rolled result is shown through a clear physical animation, UI reveal, or both before progression continues.

The final category set is:

- Recovery
- Revive
- Preparation
- Accessory
- Wild
- Gambit

## Shared Balance Model
Every scalable non-coin result has an effect-specific base value in `TeenyBalance.java`.

The normal resolution model is:

```text
resolved value = effect base value * hidden choice-node strength
```

Wild adds its own random factor:

```text
wild resolved value = effect base value * hidden choice-node strength * wild random factor
```

Gambit positive rewards use category values improved by a Gambit reward multiplier. Gambit penalties use their own base values and the hidden node strength. Discrete results such as restoring one accessory do not become fractional or duplicate automatically; their resolver defines how strength applies, if at all.

Coin results tied to the tournament completion reward are a deliberate exception. Their amount already scales with tournament difficulty through the authored completion coin reward, so they do not multiply by choice-node strength.

Numeric balance includes:

- Recovery total-healing base
- Heal All total multiplier
- Revive base HP percentage
- Revive fallback weighted coin list
- Preparation resource and effect bases
- Wild random-factor ranges
- Wild top-level outcome weights
- Wild coin-reward percentage or percentage range
- Gambit positive and negative chance
- Gambit reward multiplier
- Gambit penalty values
- Gambit coin-reward percentage
- minimum HP clamp for tournament-node damage
- bench XP multiplier

All of these belong in `TeenyBalance.java`.

## Recovery Category
Recovery restores existing living-figure HP and never revives fainted figures.

First resolve one total targeted-healing budget:

```text
targeted healing total = Recovery base total * hidden node strength
```

Recovery can roll:

- Heal All
- one healing charge
- two healing charges
- three healing charges

Charge rules:

- one charge receives the entire targeted-healing total
- two charges each receive half of the total
- three charges each receive one third of the total
- every charge can target any living figure
- multiple charges may target the same figure
- excess healing beyond missing HP is wasted

All charge-count variants have the same maximum total healing. More charges only provide greater targeting flexibility.

Heal All intentionally has a greater theoretical total because it is less precise and can waste more value:

```text
Heal All total = targeted healing total * Heal All total multiplier
```

The Heal All total is divided across all six tournament roster slots. Healing assigned to full-HP or fainted figures is wasted. This creates a real distinction between a broad but potentially inefficient heal and the smaller, fully targeted healing budget.

## Revive Category
Revive returns one selected fainted tournament figure.

Rules:

- The player chooses which fainted figure returns.
- Exactly one figure is revived.
- Revived HP is a percentage of that figure's normal tournament max HP.
- The base revive percentage is defined in `TeenyBalance.java`.
- Hidden choice-node strength multiplies the revive percentage.
- Revived HP cannot exceed `100%` of normal tournament max HP.

If no tournament figure is fainted, Revive converts into a small unpredictable Teeny Coin consolation roll. The player is told before choosing that the path will award a random `10-500` coins instead of a revive.

The fallback selects uniformly from an authored integer list containing values such as:

```text
10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 200, 300, 400, 500
```

Values may appear multiple times in the actual `TeenyBalance` list. Repeating low values such as `10` or `20` makes them more likely without requiring a separate weighted-random system. This fallback does not use the tournament completion reward and does not need precise expected-value balancing.

## Preparation Category
Preparation grants a standard-strength benefit at the beginning of the next battle.

Preparation can roll:

- starting Mana
- starting Battery
- Power Up
- Defense Up
- Shield
- Dance
- Tofu

Preparation does not include:

- Speed
- Luck Up
- direct healing
- revival
- coins
- accessory manipulation

Preparation uses the normal base-value-times-node-strength model without Wild's additional random magnitude. Participant-side effects remain active when the player swaps figures during that battle, following the normal battle effect model.

Mana, Battery, and Tofu are applied as starting participant resources. Power Up, Defense Up, Shield, and Dance are applied as starting participant-side effects for the next match.

## Accessory Category
Accessory choices are only valid in tournaments with at least one accessory slot.

The category can roll one of three normal results:

### Restore
- Select one spent accessory.
- Return it to `available` state.
- It can be selected normally in a future match.

### Preserve
- Select one available accessory.
- Mark it as `preserved`.
- Its next match use consumes preservation instead of spending the accessory.
- It remains available after that match and is spent normally after a later unpreserved use.
- Preservation does not protect against Gambit's accessory-loss penalty.

### Replacement
- Select any tournament accessory slot, including an available, preserved, spent, or lost slot.
- Select a replacement accessory from the Titan Manager `Accessories` storage page.
- Reject a replacement whose accessory id duplicates another current tournament accessory.
- The replacement enters the tournament as available and unpreserved.
- Replacing a spent or lost slot effectively provides a new usable accessory.

If a rolled Accessory result is invalid for the current state, it is removed or rerolled. For example, Restore is invalid when no accessory is spent.

## Wild Category
Wild is a guaranteed positive gamble. The uncertainty is the result and its magnitude, not whether the player is harmed.

Wild's top-level eligible outcomes have equal weight:

- Healing
- Power Up
- Mana
- Battery
- Coins

If Healing is selected, it then rolls equally between:

- one healing charge
- two healing charges
- three healing charges

This nested roll means all healing variants together have the same top-level probability as Power Up, Mana, Battery, or Coins. Each healing charge-count variant has one third of the Healing outcome's probability.

Wild never includes Heal All.

Wild Healing is eligible only when the roster has enough total missing living-figure HP to receive the entire resolved targeted-healing budget. It should not produce a nominally positive result whose value must be wasted. The resulting charges follow the Recovery targeting rules and share one total healing budget regardless of charge count.

Power Up, Mana, and Battery become next-battle starting benefits. Their magnitude uses the normal hidden node strength plus a Wild random factor.

Wild Coins are based on a percentage or percentage range of that tournament's hidden completion coin reward:

```text
Wild coins = tournament completion coins * Wild coin percentage/random factor
```

Wild Coins do not multiply by choice-node strength because the completion coin reward already scales with tournament difficulty.

Wild does not include:

- Revive
- Accessory outcomes
- Defense Up
- Shield
- Dance
- Tofu
- harmful outcomes
- another Wild roll

## Gambit Category
Gambit always has a visible and fixed:

```text
50% Reward / 50% Penalty
```

The probability does not change by tournament, story stage, or node strength.

### Gambit Reward Pool
The reward side draws from enhanced versions of eligible positive systems:

- Recovery, only when its full resolved healing value can be used
- Revive, only when at least one figure is fainted
- starting Mana
- starting Battery
- Power Up
- Dance
- Accessory choice, only when at least one Accessory action is valid
- Teeny Coins

Gambit does not reward:

- Defense Up
- Shield
- Tofu
- Wild outcomes

Gambit Recovery, Revive, Mana, Battery, Power Up, and Dance use stronger values than their normal category equivalents through the Gambit reward multiplier.

If Gambit rolls an Accessory reward, it does not randomly choose a single Accessory action. The player chooses between every currently valid option among:

- Restore
- Preserve
- Replacement

This deliberate choice makes a successful Gambit Accessory result stronger and more flexible than the normal Accessory category.

Gambit Coins are based on a defined percentage of the tournament's hidden completion coin reward:

```text
Gambit coins = tournament completion coins * Gambit coin percentage
```

They do not multiply by choice-node strength. The Gambit percentage should be meaningfully stronger than the normal Wild coin expectation because it comes from the successful half of a `50/50` gamble.

Invalid rewards are removed before the reward roll. Healing only appears when its resolved total can be used, Revive only appears with a fainted figure, and Accessory only appears when at least one of Restore, Preserve, or Replacement is valid.

### Gambit Penalty Pool
The penalty side can roll:

- damage one random living tournament figure
- damage every living tournament figure
- start the next match with Curse
- start the next match with Power Down
- start the next match with Defense Down
- permanently lose one random available accessory

Tournament-node damage cannot faint a figure. Every affected living figure is clamped to at least `1 HP`. There is no direct figure-kill or forced-faint outcome.

Next-battle debuffs apply to the participant side and remain through figure swaps during that battle, following the normal effect model.

Accessory-loss rules:

- It only targets an available or preserved tournament accessory.
- The targeted accessory becomes `lost`.
- Preservation does not prevent permanent Gambit loss.
- The outcome is invalid in a zero-accessory tournament.
- The outcome is invalid when no available or preserved accessory remains.

Invalid penalties are removed before rolling.

## Rewards
Every tournament has one authored completion reward containing:

- one figure
- an amount of Teeny Coins

The completion reward is intentionally hidden before tournament completion. The figure can contain story spoilers, and the coin reward does not need to be advertised separately.

On boss victory:

1. Finish the final battle and return to the completion area.
2. Reveal the completion reward.
3. Grant the authored figure.
4. Grant the authored amount of Teeny Coins.
5. Show the final tournament summary.
6. Unlock the tournament exit.

Every successful completion grants the exact same reward. Repeat clears still grant the same figure and the same amount of coins; tournaments can therefore become repeatable, authored figure-acquisition sources.

Wild and Gambit coin rewards do not reduce or replace the final completion reward. They are additional rewards earned during the run.

Losing or abandoning a tournament does not grant the final completion reward. XP already earned from completed matches remains per-match progression.

## Match XP
Figure XP is awarded per completed match, not only at tournament completion.

Each tournament match or shared battle definition provides a base figure XP value.

For the figures selected into that match:

- the opening figure is marked as having participated
- swapping to another selected figure marks that figure as having participated
- every figure that becomes active at least once receives full match XP
- selected figures that never become active receive a percentage of the base XP
- tournament-roster figures not selected for that match receive no XP

The reduced percentage is controlled by a shared `BENCH_XP_MULTIPLIER` in `TeenyBalance.java`. This should be implemented as a general battle feature rather than tournament-only XP logic so normal NPC battles can use the same rule.

No minimum active-time requirement is part of the current design. Becoming active at least once counts as participation.

## General Battle Reports
Tournament summary tracking should build on a general battle-report model rather than an isolated tournament-only combat statistics system.

A reusable per-battle report should be able to record:

- result
- opponent
- battle duration
- damage dealt
- damage taken
- actual healing
- friendly figure faints
- enemy figures defeated
- tofu used
- accessory activations or relevant accessory use
- XP awarded per selected figure

A tournament aggregates its completed match reports and adds tournament-specific state:

- categories chosen
- exact rolled choice outcomes
- immediate and next-battle rewards or penalties
- final HP and fainted state of all six figures
- accessories spent
- accessories preserved
- accessories restored
- accessories replaced
- accessories lost
- accessories left unused
- tournament completion reward

The same general report data may later power result screens for normal battles. Normal-battle report presentation can be implemented separately; it does not need to block the first tournament version.

## Tournament UI
The system needs several related interfaces.

### Tournament Setup Screen
Shows:

- six figure roster slots
- Titan Manager figure collection selection
- tournament-defined accessory slots
- Titan Manager Accessories page selection
- duplicate-accessory validation
- tournament confirmation

The hidden final figure and coin reward are not shown.

### Pre-Match Screen
Shows:

- current opponent name and portrait
- opponent figures, levels, and classes unless hidden
- persistent HP and fainted state of all six tournament figures
- selection of one to three living figures
- selected team order
- opening figure
- eligible first-two-slot combo choice
- available accessory selection or no accessory
- pending next-battle benefits and penalties

### Tournament HUD
The corridor HUD should show:

- all six figure portraits
- current HP bars
- fainted state
- accessory slots
- available, preserved, spent, and lost accessory states
- pending next-battle benefits and penalties
- current tournament progress or node

The HUD does not need to show coins earned during the run.

During battle, the existing battle HUD remains primary. A smaller tournament indicator can be added later if the full six-figure state is useful during combat.

### Choice Reveal
The physical path shows only the category. After commitment, an animation and/or focused result UI shows:

- the rolled result
- resolved magnitude
- required target selections
- resulting HP, accessory, resource, effect, or coin changes

### Final Summary
The final tournament summary should include:

- total completion time
- damage dealt
- damage taken
- actual healing
- friendly figure faints
- enemy figures defeated
- tofu used
- final HP and fainted state of all six figures
- accessories spent and unused
- accessories preserved, restored, replaced, or lost
- every chosen category
- every rolled outcome
- XP awarded across matches
- completion figure reward
- completion coin reward

## Failure, Abandonment, And Recovery
Current final rules:

- Losing any tournament match immediately ends the run.
- No final completion reward is granted on failure.
- Per-match XP from earlier completed matches remains awarded.

A give-up or abandon item is planned for the final hotbar slot. Its exact UI, confirmation, and cleanup behavior should be designed with the broader battle abandonment flow.

Mid-battle disconnect and crash recovery are deferred to a broader battle-session recovery system rather than solved only for tournaments. During early testing, a disconnect or crash during a tournament battle may reset or invalidate the test tournament. The final system must revisit reconnect checkpoints and cleanup before relying on tournaments in normal survival progression.

## Future Tournament Wagers
Tournament wagers are an approved future extension but are not part of the initial system.

Before a match, the player may eventually accept an optional challenge such as:

- win without using an accessory
- win without a friendly figure fainting
- include or finish with a specified class
- meet a battle-specific condition

Wager rewards may include:

- figures
- chips
- Teeny Coins
- mystery boxes after that reward type is designed

Wagers should use the general battle-report and condition-tracking layer where possible. Their exact challenge pool, reward valuation, and UI require a separate design phase.

## Design Notes
- Tournament runtime should extend the existing battle, arena, Titan Manager, NPC team, accessory, currency, and figure systems instead of creating parallel combat rules.
- The tournament owns the six-figure roster and accessory pool; the existing Titan Manager active slots do not secretly control tournament matches.
- Tournament HP is volatile run state, while original figure items remain persistent collection state.
- NPC teams and levels remain fixed authored data.
- Choice content should remain data-driven, with centralized numeric balance.
- Physical room progression and UI should complement each other: the room provides presence and anticipation, while screens handle detailed selection and exact information.
- Invisible barriers allow future NPCs and choices to remain visible without allowing sequence breaks.
- The final reward stays hidden to protect story reveals.
- Repeat clears intentionally give the same rewards.
- There are no tournament-wide stacking buffs in the current design.

## Source Of Truth
Until implementation exists, this document is the canonical tournament design.

Related existing runtime and documentation:

- [`battle-engine.md`](battle-engine.md)
- [`titan-manager.md`](titan-manager.md)
- [`../content/figures.md`](../content/figures.md)
- [`../content/accessories.md`](../content/accessories.md)
- [`../world/arenas.md`](../world/arenas.md)
- [`../world/npcs.md`](../world/npcs.md)
- [`../world/teenyverse.md`](../world/teenyverse.md)
- [`../world/rooms.md`](../world/rooms.md)
- [`../progression/leveling.md`](../progression/leveling.md)
- [`../progression/rewards-and-integrations.md`](../progression/rewards-and-integrations.md)
- [`../../src/main/java/bruhof/teenycraft/capability/BattleState.java`](../../src/main/java/bruhof/teenycraft/capability/BattleState.java)
- [`../../src/main/java/bruhof/teenycraft/capability/TitanManager.java`](../../src/main/java/bruhof/teenycraft/capability/TitanManager.java)
- [`../../src/main/java/bruhof/teenycraft/world/arena/ArenaBattleManager.java`](../../src/main/java/bruhof/teenycraft/world/arena/ArenaBattleManager.java)
- [`../../src/main/java/bruhof/teenycraft/util/NPCTeamLoader.java`](../../src/main/java/bruhof/teenycraft/util/NPCTeamLoader.java)
- [`../../src/main/java/bruhof/teenycraft/TeenyBalance.java`](../../src/main/java/bruhof/teenycraft/TeenyBalance.java)

## Open Balance Values
The system design is fixed, but numerical tuning remains open:

- Recovery base total
- Heal All multiplier
- Revive base HP percentage
- repeated entries in the Revive fallback coin list
- Preparation bases
- Wild magnitude range
- Wild coin percentage range
- Gambit reward multiplier
- Gambit coin percentage
- Gambit penalty magnitudes
- bench XP multiplier

These values should be tuned after a minimal playable tournament can measure real HP attrition, accessory value, match length, and run success rate.

## Planned Additions
- implemented tournament run persistence
- tournament setup, pre-match, HUD, choice, and summary screens
- physical instanced tournament rooms
- tournament NPC and invisible-barrier progression
- tournament JSON loading and validation
- tournament HP and accessory state
- category resolution and animations
- completion rewards
- shared match XP and battle reports
- broader reconnect and abandonment support
- optional tournament wagers
