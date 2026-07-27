# NPC Team And AI Test Roster

## Purpose

This file documents the temporary NPC teams used to test figure coverage, upgrade scaling, golden abilities, and opponent AI personalities. These tiers are test fixtures, not final encounter balance or shipped progression design.

The exact runtime values live in the JSON files under [`data/teenycraft/npc_teams`](../../src/main/resources/data/teenycraft/npc_teams). The implemented AI control meanings and preset defaults remain documented in [`npc-ai-controls.md`](../world/npc-ai-controls.md).

## Current Test Setup

- There are 24 tiered teams distributed as 5 / 4 / 3 / 4 / 5 / 3 across test tiers 1-6, plus one standalone passive training dummy.
- The tier-1 set contains one solo team, one duo, and three trios.
- Tier 3 and tier 5 each contain one solo; every other team is a trio.
- All 65 currently loadable figure IDs appear exactly once.
- Ability order stays at the authored default through `[0, 1, 2]`.
- No NPC chips are equipped.
- Team filenames and IDs start with the test tier, followed by recognizable shortened figure names.
- Figure distribution was randomized with a fixed setup pass; it is not intended to imply thematic team composition.

## Passive Training Dummy

[`test_dummy.json`](../../src/main/resources/data/teenycraft/npc_teams/test_dummy.json) uses Killer Croc with seven HP upgrades. Killer Croc has 70 base HP and zero base Dodge, producing a 210 HP target with zero Dodge. Its optional `ai.enabled` field is set to `false`, so it remains in the normal battle runtime without moving, attacking, using abilities, or swapping.

This team is outside the numbered tier counts and exists only for repeatable damage and effect testing.

## Temporary Tier Definitions

| Tier | Teams | Figure level / slots | Upgrades per figure | Golden abilities | AI direction |
|---:|---:|---:|---:|---|---|
| 1 | 5 | 1 / 6 | 0 | None | Preset 1 or deliberately weak variants |
| 2 | 4 | 6 / 10 | 5 | None | Preset 2 with low-complexity personality tests |
| 3 | 3 | 11 / 7 | 10 | Default slot 1 | Preset 3 baseline and defensive variant |
| 4 | 4 | 16 / 12 | 15 | Default slots 1-2 | Preset 4 with counter-aware specialist profiles |
| 5 | 5 | 20 / 13 | 19 | All three | Preset 5 with strong specialist profiles |
| 6 | 3 | 20 / 9 | 30 | All three | Custom elite balanced, offensive, and defensive profiles |

Tier 6 intentionally exceeds normal player progression: figures stay at the implemented level cap of 20 but receive 30 applied upgrade points. The NPC builder accepts this, so it is useful as an explicit boss-strength stress test rather than a normal level.

## Upgrade Allocation

All distributions remain HP/Power-heavy:

- Combat figures spend every point in H/P.
- Dodge-oriented figures reserve a minority of points for D.
- Luck-oriented figures reserve a minority of points for L.
- Support-style figures reserve a minority for both D and L.
- Higher tiers preserve the same direction while increasing the size of the specialist portion.

The allocation label is only a testing heuristic based on the supplied ability list. It is not a permanent role classification.

## AI Personality Matrix

The personality experiments are concentrated on the 20 three-figure teams. Solo and duo profiles provide simpler references. The table records the intended feel and test question; each linked JSON is the exact source for its overrides.

| Team | Figures | Personality | What to observe |
|---|---|---|---|
| [1_trigon.json](../../src/main/resources/data/teenycraft/npc_teams/1_trigon.json) | trigon | Slow solo generalist | Basic single-figure pacing without swap decisions. |
| [1_jinx_bizarro.json](../../src/main/resources/data/teenycraft/npc_teams/1_jinx_bizarro.json) | jinx, bizarro | Unmodified weak preset | Baseline difficulty-1 timing and simple two-figure swaps. |
| [1_robin_beast_manta.json](../../src/main/resources/data/teenycraft/npc_teams/1_robin_beast_manta.json) | robin, beast_boy, black_manta | Hesitant generalists | Whether low urgency remains active but visibly forgiving. |
| [1_starfire_sticky_clark.json](../../src/main/resources/data/teenycraft/npc_teams/1_starfire_sticky_clark.json) | starfire, sticky_joe, clark_kent | Cautious stallers | Slow spacing, conservative mana, and support-action timing. |
| [1_billy_moth_cyborg.json](../../src/main/resources/data/teenycraft/npc_teams/1_billy_moth_cyborg.json) | billy_numerous, killer_moth, cyborg | Reckless no-swap rush | High aggression with deliberately poor team management. |
| [2_bumblebee_beyond_seemore.json](../../src/main/resources/data/teenycraft/npc_teams/2_bumblebee_beyond_seemore.json) | bumblebee, batman_beyond, see_more | Unmodified developing preset | Baseline difficulty-2 behavior. |
| [2_darkseid_hawkgirl_ivy.json](../../src/main/resources/data/teenycraft/npc_teams/2_darkseid_hawkgirl_ivy.json) | darkseid, hawkgirl, poison_ivy | Committed bruisers | Pressure-heavy choices with reluctant swapping. |
| [2_superman_redarrow_redx.json](../../src/main/resources/data/teenycraft/npc_teams/2_superman_redarrow_redx.json) | superman, red_arrow, red_x | Conservative ranged team | Kiting and expensive-move restraint at a low AI tier. |
| [2_hooded_alfred_drlight.json](../../src/main/resources/data/teenycraft/npc_teams/2_hooded_alfred_drlight.json) | the_hooded_hood, alfred, dr_light | Swap-curious learners | Frequent simple swaps without advanced proactive logic. |
| [3_supergirl.json](../../src/main/resources/data/teenycraft/npc_teams/3_supergirl.json) | supergirl | Balanced solo pressure | Mid-tier action scoring without swap noise. |
| [3_mammoth_catboy_flash.json](../../src/main/resources/data/teenycraft/npc_teams/3_mammoth_catboy_flash.json) | mammoth, cat_beast_boy, the_flash | Unmodified balanced preset | Reference team for the default difficulty-3 profile. |
| [3_speedy_batman_booster.json](../../src/main/resources/data/teenycraft/npc_teams/3_speedy_batman_booster.json) | speedy, batman, booster_gold | Defensive planners | Safer decisions, mana saving, and normal advanced swaps. |
| [4_penguin_harley_beetle.json](../../src/main/resources/data/teenycraft/npc_teams/4_penguin_harley_beetle.json) | the_penguin, harley_quinn, blue_beetle | Proactive class swappers | Fast advantage-seeking and escape from bad matchups. |
| [4_lex_jessica_blood.json](../../src/main/resources/data/teenycraft/npc_teams/4_lex_jessica_blood.json) | lex_luthor, jessica_cruz, robotic_brother_blood | Patient controllers | Counter-aware control, spacing, and conservative spending. |
| [4_aquaman_grodd_masmenos.json](../../src/main/resources/data/teenycraft/npc_teams/4_aquaman_grodd_masmenos.json) | aquaman, gorilla_grodd, mas_y_menos | Fast pressure team | Sharper aggressive pacing while preserving smart swaps. |
| [4_washington_stewart_catwoman.json](../../src/main/resources/data/teenycraft/npc_teams/4_washington_stewart_catwoman.json) | george_washington, john_stewart, catwoman | Deliberate optimizer | More deterministic choices at otherwise balanced settings. |
| [5_martian.json](../../src/main/resources/data/teenycraft/npc_teams/5_martian.json) | martian_manhunter | Disciplined solo tactician | Strong single-figure utility and ranged decision-making. |
| [5_argyle_raven_santa.json](../../src/main/resources/data/teenycraft/npc_teams/5_argyle_raven_santa.json) | argyle_trigon, raven, santa_clause | Defensive sustain team | Healing, maintenance, safe spacing, and preservation swaps. |
| [5_kidflash_wonder_blackfire.json](../../src/main/resources/data/teenycraft/npc_teams/5_kidflash_wonder_blackfire.json) | kid_flash, wonder_woman, blackfire | Relentless brawlers | Maximum pressure and risk-taking on the normal top preset. |
| [5_batgirl_croc_riddler.json](../../src/main/resources/data/teenycraft/npc_teams/5_batgirl_croc_riddler.json) | batgirl, killer_croc, the_riddler | Precise balanced team | Consistent high-score actions without extreme personality bias. |
| [5_slade_lightning_gizmo.json](../../src/main/resources/data/teenycraft/npc_teams/5_slade_lightning_gizmo.json) | slade, black_lightning, gizmo | Hyperactive swappers | Stress-test rapid matchup evaluation and swap cadence. |
| [6_nightwing_joker_sinestro.json](../../src/main/resources/data/teenycraft/npc_teams/6_nightwing_joker_sinestro.json) | nightwing, joker, sinestro | Elite balanced optimizer | Best all-round profile and the main tier-6 reference. |
| [6_bane_terra_shazam.json](../../src/main/resources/data/teenycraft/npc_teams/6_bane_terra_shazam.json) | bane, terra, shazam | Elite offensive optimizer | Best-AI reasoning tuned toward fast, risky pressure. |
| [6_silkie_rose_artemis.json](../../src/main/resources/data/teenycraft/npc_teams/6_silkie_rose_artemis.json) | silkie, rose_wilson, artemis | Elite defensive optimizer | Best-AI reasoning tuned toward spacing, restraint, and preservation. |

## Suggested Test Notes

For each battle, record only the most visible results:

- Did the opponent keep acting, or did it idle at an obviously wrong range?
- Did its mana spending match the intended conservative or reckless personality?
- Did support and maintenance actions happen at useful times?
- Did swaps look purposeful, excessive, or absent?
- Did close, mid, far, and auto range profiles feel observably different?
- Did lower `choice_window` profiles feel smarter or merely repetitive?
- Did the elite profiles feel more competent without movement becoming unfair?
- Which exact team and matchup exposed the behavior?

## Status

Implemented as temporary data-driven test content. Results have not yet been playtested, and none of these team compositions, tier values, or personality settings should be treated as final balance.
