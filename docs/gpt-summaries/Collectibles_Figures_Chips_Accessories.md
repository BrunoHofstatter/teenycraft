# Collectibles: Figures, Chips, and Accessories

## Figures
Figures are the core collectible entities. They are item-backed and hold persistent stats.

**Implemented:**
- **4 Core Stats:**
  - **HP:** Increases max health in battle.
  - **Power:** Increases outgoing ability damage and healing scale.
  - **Dodge:** Increases movement speed in battle and reduces incoming damage when a dodge is successful.
  - **Luck:** Increases critical hit chance/damage and slightly improves effect formulas (duration, magnitude).
- **Ability Loadouts:** Figures have an authored list of abilities. Players can permanently reorder the active loadout (costs coins).
- **Golden Status:** Tracks per-ability mastery progress, adding extra traits or modifying effects when fully unlocked.

## Chips
Chips are equippable modifiers. A figure can hold exactly one installed chip at a time (installing a new one destroys the old one).

**Implemented (Current Roster):**
- **Tough Guy:** Increases Power, lowers max HP.
- **Smokescreen:** Increases Dodge.
- **Tough Smokescreen:** Hybrid chip combining Tough Guy and Smokescreen.
- **Lucky Hearts:** Heals the owner when they land a critical hit.
- **Insta Cast Chance:** Adds a chance to instantly cast abilities that normally require charging.
- **Dance:** Applies the "dance" buff (mana regen) the first time the figure enters a battle.
- **Mana Boost:** Grants instant mana the first time the figure enters a battle.
- **Death Energy:** Grants accessory battery charge when the equipped figure faints.
- **Self Explosion:** Deals group damage to the opposing team when the equipped figure faints.
- **Necromancer:** Summons a pet when the equipped figure scores a kill.
- **Vampire:** Heals for a % of max HP when the equipped figure scores a kill.

**Planned:**
- Special curated fusion recipes (combining two different chips into a unique hybrid).
- Broader passive hooks like retaliation (on-hit/on-damaged effects).

## Accessories
Accessories are single, team-wide equippable items that sit in the Titan Manager. They provide battle buffs powered by the "Battle Battery" (which charges passively and from successful attacks).

**Implemented (Current Roster):**
- *Accessories bypass normal ability scaling and apply fixed behaviors while active:*
- **Titan's Coin**
- **Mother Box**
- **Bat Signal**
- **Red Lantern Battery**
- **Green Lantern Battery**
- **Violet Lantern Battery**
- **Raven's Spellbook**
- **Cyborg's Waffle Shooter**
- **Lil' Penguin**
- **Kryptonite**
- **Justice League Coin**
- **Birdarang**
- **Superman's Underpants**
- **Krypto the Superdog**

**Planned:**
- More accessories and reactive triggers (e.g., triggering automatically when damaged).
- HUD feedback for active accessory state.
