package bruhof.teenycraft.battle.trait;

import bruhof.teenycraft.battle.BattleFigure;
import bruhof.teenycraft.battle.damage.DamagePipeline;
import bruhof.teenycraft.capability.IBattleState;
import bruhof.teenycraft.util.AbilityLoader;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public interface ITrait {
    String getId();

    /**
     * Handles ability execution flow.
     * Return TRUE if standard execution should continue.
     * Return FALSE if execution is blocked or handled (e.g. Charge Up, Activate).
     */
    interface IExecutionTrait extends ITrait {
        boolean onExecute(IBattleState state, LivingEntity attacker, BattleFigure figure, int slotIndex, AbilityLoader.AbilityData data, LivingEntity target, List<Float> params, boolean isGolden);
    }

    /**
     * Modifies the outgoing DamageResult before hits are split.
     */
    interface IPipelineTrait extends ITrait {
        void modifyOutput(DamagePipeline.DamageResult result, List<Float> params);
    }

    /**
     * Modifies mitigation result per hit.
     */
    interface IMitigationTrait extends ITrait {
        void modifyMitigation(DamagePipeline.MitigationResult result, List<Float> params, boolean isGolden);
    }

    /**
     * Triggered on a successful hit (>0 damage dealt).
     */
    interface IHitTriggerTrait extends ITrait {
        void onHit(IBattleState state, LivingEntity attacker, BattleFigure figure, AbilityLoader.AbilityData data, int manaCost, List<Float> params, LivingEntity target, int damageDealt, boolean isGolden);
    }
}
