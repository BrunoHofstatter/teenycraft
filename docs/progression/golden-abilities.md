# Golden Ability Acquisition

## Purpose
Document the implemented player-facing loop for advancing a figure's per-ability golden progress by feeding figures to Silkie.

## Current Status
The functional system is implemented. The Silkie Station block opens a server-authoritative screen for choosing a target, previewing one to five sacrifices, and confirming the irreversible feeding. Silkie animations, staged eating and burping, particles, and final art are deferred polish.

The block is available in the Teeny Craft creative tab. It intentionally has no recipe or survival acquisition path yet.

## Runtime Source Of Truth
- [`src/main/java/bruhof/teenycraft/golden/GoldenSacrificeCalculator.java`](../../src/main/java/bruhof/teenycraft/golden/GoldenSacrificeCalculator.java) for affinity and combo scoring
- [`src/main/java/bruhof/teenycraft/item/custom/ItemFigure.java`](../../src/main/java/bruhof/teenycraft/item/custom/ItemFigure.java) for persistent per-ability points and legacy migration
- [`src/main/java/bruhof/teenycraft/screen/SilkieStationMenu.java`](../../src/main/java/bruhof/teenycraft/screen/SilkieStationMenu.java) for validation, selection, previews, and the feed transaction
- [`src/main/java/bruhof/teenycraft/screen/SilkieStationScreen.java`](../../src/main/java/bruhof/teenycraft/screen/SilkieStationScreen.java) for the current client interface
- [`src/main/java/bruhof/teenycraft/TeenyBalance.java`](../../src/main/java/bruhof/teenycraft/TeenyBalance.java) for every scoring value and ability requirement
- [`src/main/java/bruhof/teenycraft/util/FigureGroupLoader.java`](../../src/main/java/bruhof/teenycraft/util/FigureGroupLoader.java) for group membership

## Player Flow
- Place and open a Silkie Station outside battle.
- Drag a target figure from the active team, favorites, or vanilla player inventory into the target area. The station retains a non-owning reference; it does not temporarily remove the target from its source.
- The station automatically progresses the first incomplete ability in authored order. Ability 2 remains locked until ability 1 is golden, and ability 3 remains locked until abilities 1 and 2 are golden.
- Search or sort normal Titan Manager storage and click one to five donor figures to add them. Click an offering to remove it.
- Review every base point, combo bonus, applied point, and discarded overflow value.
- Press `Feed Silkie`, then press the changed confirmation button to perform the feeding.
- On success, every donor is consumed atomically, the target stays selected, and progression automatically advances to the next authored ability if the current one completed. Search and sort state remain in place.

Closing the screen or changing the target never consumes donors. Changing the target clears the offering because all affinities may change.

## Target And Donor Rules
- A target may be any figure in the active team or vanilla player inventory, or any Titan Manager storage figure marked as a favorite.
- A fully golden figure cannot be selected as a target. There is no minimum target level.
- Donors come only from normal Titan Manager figure storage.
- Active figures and favorites are protected from sacrifice.
- Leveled, upgraded, partially golden, fully golden, and chip-equipped donors are allowed, but the screen warns that invested figures are selected. Investment adds no feed value.
- Feeding has no coin cost and is disabled during battle.

## Base Affinity
Each donor receives exactly one highest-priority classification.

| Priority | Affinity | Base points |
|---:|---|---:|
| 1 | Exact target figure id | 5 |
| 2 | Shares any group with the target | 3 |
| 3 | Same class as the target | 2 |
| 4 | Unrelated | 1 |

An exact donor does not also count as group or class, and a group donor does not also count as class. Independent combo categories may nevertheless all award bonuses in a mixed feeding when their own classified donors meet the thresholds.

## Exact-Duplicate Combo

| Exact donors | Bonus |
|---:|---:|
| 0-1 | 0 |
| 2 | 2 |
| 3 | 5 |
| 4 | 7 |
| 5 | 10 |

Exact-only feedings therefore award `5`, `12`, `20`, `27`, or `35` total points for one through five donors.

## Class Combo

| Qualifying class donors | Bonus |
|---:|---:|
| 0-2 | 0 |
| 3 | 1 |
| 4 | 2 |
| 5 | 3 |

Only donors whose final affinity is `CLASS` contribute. At most two copies of one figure id count toward the qualifying donor count. A third or later copy still awards its individual class base points but does not advance the combo. This is intentionally less strict than requiring every donor id to be distinct.

## Group Combo
Only donors whose final affinity is `GROUP` contribute. Repeated ids still award individual base points, but only distinct donor ids advance a group combination. The target counts when checking group completion.

| Distinct group donor ids | Standard bonus |
|---:|---:|
| 0-2 | 0 |
| 3 | 3 |
| 4 | 3 |
| 5 | 5 |

Completing a target group instead awards one point per required distinct donor, including two-person groups. Singleton groups receive no completion bonus. When several target groups qualify, only the highest-value group bonus is awarded; ties prefer more distinct participating donors and then a deterministic group id order.

## Ability Requirements And Persistence

| Original authored ability position | Required points |
|---:|---:|
| 1 | 20 |
| 2 | 24 |
| 3 | 28 |

The requirement follows the original figure JSON order, not the current reordered battle position. Progress is stored as integer points per ability. Existing normalized `GoldenProgress` values migrate lazily by rounding `old progress * requirement`; existing command and NPC helpers remain compatible through `ItemFigure`.

Points beyond the current ability's requirement are discarded, never banked or transferred. The preview and success message expose that waste before and after confirmation.

## Browser And Transparency
- Ten candidates are visible in a `5 x 2` page.
- `Recommended` orders by exact, group, class, and unrelated affinity, then by low level.
- `Class` puts the target class first and orders entries by low level within class.
- `Level` orders from low to high.
- Typing in search filters by name, id, class, or group and orders results by low level.
- The screen exposes current and required progress, remaining points, each donor's base points and relationship, category combo counts and bonuses, best group progress, total gain, applied gain, and overflow.

The server recalculates all preview values and revalidates the target and every donor immediately before consumption. Either progress and all donor removals succeed together, or nothing is consumed.

## Economy Risk And Future Feed Multiplier
Affinity scoring alone cannot guarantee that related donors are always the most coin-efficient choice. An unlimited, very cheap unrelated or same-class figure may outperform a costly duplicate despite the duplicate's larger point award.

The eventual economy should combine the current scoring with one or more of:
- a named feed-value tier or multiplier per donor
- shop stock limits, rotations, or escalating repeat-purchase prices
- minimum prices informed by sacrifice usefulness
- acquisition restrictions that prevent unlimited cheap-figure conversion

The feed multiplier is deliberately not part of the first implementation. Its content field, tier values, rounding, and whether it affects base or combo points remain undecided. Scoring is centralized so the multiplier can be added without rebuilding the UI or transaction flow.

## Planned Additions
- final Silkie Station model and texture
- eating, burping, particles, sound, and staged confirmation presentation
- a survival acquisition path or recipe
- feed-value tiers after the shop economy is designed
