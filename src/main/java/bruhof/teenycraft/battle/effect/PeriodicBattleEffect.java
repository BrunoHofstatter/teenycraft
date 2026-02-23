package bruhof.teenycraft.battle.effect;

import bruhof.teenycraft.battle.BattleFigure;
import bruhof.teenycraft.capability.IBattleState;

/**
 * A template for effects that trigger periodically (every X ticks).
 * Expects the interval to be stored in the 'magnitude' field of the EffectInstance.
 */
public abstract class PeriodicBattleEffect extends BattleEffect {

    public PeriodicBattleEffect(String id, EffectCategory category) {
        super(id, EffectType.DURATION, category);
    }

    @Override
    public void onTick(IBattleState state, BattleFigure target, int remainingDuration) {
        EffectInstance inst = state.getEffectInstance(getId());
        if (inst == null || inst.magnitude <= 0) return;

        // Trigger on initial application if Duration is a multiple of Interval, 
        // and then every Interval ticks thereafter.
        if (remainingDuration > 0 && remainingDuration % inst.magnitude == 0) {
            onPeriodicTick(state, target, remainingDuration, inst);
        }
    }

    /**
     * Logic executed every time the interval passes.
     */
    protected abstract void onPeriodicTick(IBattleState state, BattleFigure target, int remainingDuration, EffectInstance instance);

    /**
     * Helper to calculate a perfectly balanced magnitude for the current tick, 
     * consuming from the 'power' field of the instance.
     */
    protected int getSmartSplitValue(EffectInstance inst, int remainingDuration) {
        int interval = inst.magnitude;
        // Calculate remaining ticks including this one
        int ticksLeft = (remainingDuration + interval - 1) / interval;
        if (ticksLeft <= 0) return 0;
        
        int val = Math.round(inst.power / ticksLeft);
        inst.power -= val;
        return val;
    }
}
