# Gemini Technical Brain - Teeny Craft Battle System

## 1. Core Architecture: Decoupled Battle States
The system is built on a **Participant-Agnostic** model. Battle logic does not depend on `ServerPlayer`; instead, it interacts with the `IBattleState` capability.

*   **Capability Attachment:** `IBattleState` is attached to all `LivingEntity` instances via `ModEvents.onAttachCapabilities`.
*   **Registration:** Manually registered in `ModEvents` (not `@AutoRegisterCapability`) to avoid conflicts.
*   **The Global Loop:** `ModEvents.onLivingTick` handles the 20Hz update for all active battle participants. It processes Mana regeneration, Effect ticking, and Charge-up completion.

---

## 2. Ability Execution Pipeline (`AbilityExecutor`)
Abilities are executed through three primary entry points:
*   **Melee (`executeAttack`):** Triggered by Left-Click. Performs a localized AOE check in front of the caster.
*   **Ranged/Self (`executeAction`):** Triggered by Right-Click. Uses a Cone-based target search (angle and range defined in `TeenyBalance`).
*   **None (Buffs/Utility):** Instant application to self.

### Symmetrical Targeting Logic
The engine automatically identifies targets based on context:
*   **Attacks:** Target the "Enemy" (the nearest entity with a `BattleState`).
*   **Buffs:** Target the "Self" (the caster).
*   **Debug Casting:** The `/teeny cast` command uses `self` or `opponent` literals to force the caster role, while the engine determines the logical target based on `hitType`.

---

## 3. Damage Calculation (`DamagePipeline`)
Damage is processed in a two-stage decoupled flow:

### Stage 1: Output Calculation (Attacker Context)
`calculateOutput` uses the attacker's **HOT Stats** (Active Power, Mana, Tier Multipliers) and processed **Traits** (like `multi_hit` or `group_damage`) to create a `DamageResult` package.

### Stage 2: Mitigation Calculation (Victim Context)
`calculateMitigation` receives the package and applies:
1.  **Effective Defense:** Multiplicative reduction based on the victim's level-scaled Defense stat.
2.  **Shields:** Absolute negation of one damage instance (consumes the shield).
3.  **Dodge:** "Shuffle Bag" logic. If triggered, damage is reduced by the victim's flat Dodge stat.
4.  **Critical Hits:** Per-hit roll using the attacker's Luck-based Shuffle Bag.

---

## 4. Modular Logic Registries
To avoid monolithic classes, logic is moved into registries:

### Effect Applier Registry (`EffectApplierRegistry`)
Decouples "The Ability" from "The Result". Effects (Heal, Poison, Stun) are applied via standardized `EffectApplier` implementations. 
*   **Periodic Effects:** `PeriodicBattleEffect` abstract class handles DOT (Poison) and smart-split Radios (Heal/Power).
*   **State Control:** Centralized handling for Stun (lock cast/move), Root (lock swap), and Waffle (lock slot).

### Trait Registry (`TraitRegistry`)
Traits hook into specific lifecycle events:
*   **`IExecutionTrait`:** Can cancel or modify the initial cast (e.g., `charge_up`).
*   **`IPipelineTrait`:** Modifies the outgoing `DamageResult` (e.g., `multi_hit`, `group_damage`).
*   **`IHitTriggerTrait`:** Fires after a successful hit (e.g., `tofu_chance`, Recoil).

---

## 5. Round Reset & Death Swapping
When a figure faints (Virtual HP hit 0):
1.  **CheckFaint:** Scans the team for the next alive figure.
2.  **Auto-Swap:** Instantly updates `activeFigureIndex`.
3.  **Full Cleanse:** Wipes the `activeEffects` map for a fresh start.
4.  **Lockdown:** Both participants receive a `reset_lock` effect (3 seconds), blocking all ability usage.
5.  **Symmetrical Visuals:** Both participants Glow, play a blast sound, and emit 60 `LARGE_SMOKE` particles.
6.  **Persistence:** The vanilla entity (Player or Dummy) stays alive during swaps. The Dummy only dies when the entire team is defeated.

---

## 6. Technical Standards & Balance
*   **`TeenyBalance.java`:** The single source of truth. NO hardcoded numbers in logic classes.
*   **`StatType`:** Unified enum for calculating effective stats (Level + Upgrades + Buffs).
*   **JSON Sync:** Abilities, Figures, and NPC Teams are loaded via `SimplePreparableReloadListener` and synced to clients.
*   **Tiny Damage:** Uses `0.01f` vanilla damage to trigger red flashes/knockback without interfering with Virtual HP logic.
