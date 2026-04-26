# Combat System and Mechanics

## Core Battle Flow

**Implemented:**
- **Real-Time Combat:** Battles use virtual HP and happen in real-time (no turns). 
- **Mana & Battery:** Mana regenerates continuously (capped) and is spent to use abilities. The Battery charges passively and on hits, unlocking the team's Accessory.
- **Tofu:** Temporary consumable mana states that trigger random effects.
- **Death & Swapping:** When a figure hits 0 HP, it faints. All its active effects are cleansed, a swap lockdown ("reset lock") occurs, and the next figure comes in. Victory is achieved when all 3 enemy figures faint.
- **Shuffle Bag (RNG):** Critical hits and Dodges are not pure RNG. They use a "Shuffle Bag" (e.g., 1 success card mixed into 9 failure cards). This guarantees a success eventually and prevents infinite lucky/unlucky streaks.

**Planned:**
- **Opponent AI:** Currently, enemies are placeholder dummies. True AI will make tactical decisions on spacing, mana usage, cooldown timing, and swapping.

## Traits
Traits modify how an ability behaves or executes.

**Implemented:**
- **multi_hit:** Splits the damage across X hits.
- **group_damage:** Hits the entire opposing alive team, not just the active target.
- **blue:** A channeled ability that spends mana over time while firing multiple times.
- **charge_up:** Delays the cast. Spends mana upfront and locks the figure briefly.
- **instant_cast_chance:** Gives a % chance to completely bypass a `charge_up` delay.
- **surprise:** Adds random variance to the raw damage output.
- **undodgeable:** Completely ignores the victim's Dodge mitigation roll.
- **activate:** Requires the ability to be cast (and stored) X times before it actually fires and does scaled damage.

## Effects Glossary
Effects are applied to the active Figure's `BattleState`. They are cleansed on faint/swap.

**Implemented - HP & Resources:**
- **heal / group_heal:** Restores HP (single target or full team).
- **bar_fill / bar_deplete:** Instantly restores or drains mana.
- **self_shock:** Deals self-damage based on max HP and mana cost.
- **self_damage:** Standard recoil damage mitigated normally.
- **tofu_spawn:** Spawns a temporary tofu consumable.

**Implemented - Buffs:**
- **power_up:** Infinite stackable flat damage bonus (consumed on your next attack).
- **defense_up:** Reduces incoming damage by a %.
- **luck_up:** Temporarily boosts Luck (improving crits/effect scaling).
- **dance:** Multiplies mana regeneration speed.
- **cuteness:** Adds a % chance to reflect damage.
- **shield:** Negates the next instance of incoming damage completely.
- **dodge_smoke:** Grants a set number of enhanced dodge charges.
- **health_radio / power_radio:** Periodic healing or power-ups over time.
- **flight:** Vertical launch state that grants evasion from non-group hits.
- **pet_slot_1 / pet_slot_2:** Internal slots used for summoning auto-attacking pets.

**Implemented - Debuffs & Control:**
- **power_down:** Infinite stackable flat damage penalty (consumed on attack).
- **defense_down:** Increases incoming damage by a %.
- **root:** Locks the opponent from swapping figures.
- **disable (0/1/2):** Locks a specific figure slot entirely (forces a swap if active).
- **stun:** Action lock (cannot move or cast).
- **freeze:** Instantly burns mana and applies a movement freeze.
- **poison:** Standard Damage-over-Time (DoT).
- **shock:** Periodic mini-stun DoT.
- **curse:** Reduces mana regeneration speed.
- **waffle:** Blocks a random ability slot from being used.
- **kiss:** Blocks future positive buffs and strips existing ones.
- **remote_mine (0/1/2):** Places an infinite staged mine tied to an ability slot/caster.

**Implemented - Special:**
- **cleanse:** Instantly purges debuffs/control effects and applies `cleanse_immunity`.
- **dispel:** Instantly purges buffs.
- **reflect:** Reduces incoming damage, reflects damage back, but self-stuns/freezes the user.
- **reset_lock:** A brief, special lockdown applied to both sides during a faint/round reset.