package bruhof.teenycraft.battle.effect;

import bruhof.teenycraft.capability.IBattleState;
import bruhof.teenycraft.battle.BattleFigure;

public abstract class BattleEffect {
    // ... (enums and constructor remain same)
    private final String id;
    private final EffectType type;
    private final EffectCategory category;

    public enum EffectType {
        INSTANT, DURATION, INFINITE
    }

    public enum EffectCategory {
        BUFF, DEBUFF, CONTROL, SPECIAL
    }

    public BattleEffect(String id, EffectType type, EffectCategory category) {
        this.id = id;
        this.type = type;
        this.category = category;
    }

    public String getId() { return id; }
    public EffectType getType() { return type; }
    public EffectCategory getCategory() { return category; }
    public boolean isBeneficial() { return category == EffectCategory.BUFF || category == EffectCategory.SPECIAL; }

    // ==========================================
    // LOGIC HOOKS
    // ==========================================

    /**
     * Called when the effect is first applied.
     * Return FALSE to cancel application.
     */
    public boolean onApply(IBattleState state, BattleFigure target, int duration, int magnitude) {
        return true;
    }

    /**
     * Called every tick while active.
     */
    public void onTick(IBattleState state, BattleFigure target, int remainingDuration) {
        // Default: Do nothing
    }
    
    /**
     * Called when the figure attacks.
     * Return TRUE if the effect should be removed (consumed).
     */
    public boolean onAttack(IBattleState state, BattleFigure attacker) {
        return false;
    }

    /**
     * Called when the effect expires or is removed.
     */
    public void onRemove(IBattleState state, BattleFigure target) {
        // Default: Do nothing
    }
}
