package bruhof.teenycraft.battle.effect;

import bruhof.teenycraft.battle.BattleFigure;
import bruhof.teenycraft.capability.IBattleState;
import bruhof.teenycraft.util.AbilityLoader;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

@FunctionalInterface
public interface EffectApplier {
    /**
     * Applies an effect logic.
     * 
     * @param attackerState The state of the caster/attacker.
     * @param attackerEntity The entity of the attacker.
     * @param attackerFigure The active figure of the attacker.
     * @param data The ability data being executed.
     * @param manaCost The cost of the ability.
     * @param params The specific parameters for this effect instance.
     * @param target The entity the effect is being applied TO (can be self or opponent).
     */
    void apply(IBattleState attackerState, LivingEntity attackerEntity, BattleFigure attackerFigure, AbilityLoader.AbilityData data, int manaCost, List<Float> params, LivingEntity target);
}
