package bruhof.teenycraft.battle.trait;

import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.battle.BattleFigure;
import bruhof.teenycraft.battle.damage.DamagePipeline;
import bruhof.teenycraft.capability.IBattleState;
import bruhof.teenycraft.util.AbilityLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TraitRegistry {
    private static final Map<String, ITrait> REGISTRY = new HashMap<>();

    public static void register(ITrait trait) {
        REGISTRY.put(trait.getId(), trait);
    }

    public static ITrait get(String id) {
        return REGISTRY.get(id);
    }

    public static void init() {
        // MULTI HIT
        register(new ITrait.IPipelineTrait() {
            @Override public String getId() { return "multi_hit"; }
            @Override public void modifyOutput(DamagePipeline.DamageResult result, List<Float> params) {
                if (!params.isEmpty()) result.hitCount = Math.max(1, params.get(0).intValue());
            }
        });

        // GROUP DAMAGE
        register(new ITrait.IPipelineTrait() {
            @Override public String getId() { return "group_damage"; }
            @Override public void modifyOutput(DamagePipeline.DamageResult result, List<Float> params) {
                result.isGroupDamage = true;
            }
        });

        // CHARGE UP
        register(new ITrait.IExecutionTrait() {
            @Override public String getId() { return "charge_up"; }
            @Override public boolean onExecute(IBattleState state, LivingEntity attacker, BattleFigure figure, int slotIndex, AbilityLoader.AbilityData data, LivingEntity target, List<Float> params, boolean isGolden) {
                float param = params.isEmpty() ? 1.0f : params.get(0);
                int ticks = (int) (TeenyBalance.BASE_CHARGE_DELAY * param);
                
                if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§e§lCHARGING..."));
                state.consumeMana(TeenyBalance.getManaCost(slotIndex + 1, "a")); 
                state.startCharge(ticks, data, slotIndex, isGolden, (target != null) ? target.getUUID() : null);
                return false; 
            }
        });

        // UNDODGEABLE
        register(new ITrait.IPipelineTrait() {
            @Override public String getId() { return "undodgeable"; }
            @Override public void modifyOutput(DamagePipeline.DamageResult result, List<Float> params) {
                result.undodgeable = true;
            }
        });
    }

    // TRIGGER HELPERS

    public static void triggerPipelineHooks(List<AbilityLoader.TraitData> traits, DamagePipeline.DamageResult result) {
        if (traits == null) return;
        for (AbilityLoader.TraitData t : traits) {
            ITrait impl = get(t.id);
            if (impl instanceof ITrait.IPipelineTrait p) p.modifyOutput(result, t.params);
        }
    }

    public static boolean triggerExecutionHooks(IBattleState state, LivingEntity attacker, BattleFigure figure, int slotIndex, AbilityLoader.AbilityData data, LivingEntity target, boolean isGolden) {
        if (data.traits == null) return true;
        for (AbilityLoader.TraitData t : data.traits) {
            ITrait impl = get(t.id);
            if (impl instanceof ITrait.IExecutionTrait e) {
                if (!e.onExecute(state, attacker, figure, slotIndex, data, target, t.params, isGolden)) return false;
            }
        }
        return true;
    }

    public static void triggerMitigationHooks(List<AbilityLoader.TraitData> traits, DamagePipeline.MitigationResult result, boolean isGolden) {
        if (traits == null) return;
        for (AbilityLoader.TraitData t : traits) {
            ITrait impl = get(t.id);
            if (impl instanceof ITrait.IMitigationTrait m) m.modifyMitigation(result, t.params, isGolden);
        }
    }

    public static void triggerHitHooks(IBattleState state, LivingEntity attacker, BattleFigure figure, AbilityLoader.AbilityData data, int manaCost, LivingEntity target, int damageDealt, boolean isGolden) {
        if (data.traits == null) return;
        for (AbilityLoader.TraitData t : data.traits) {
            ITrait impl = get(t.id);
            if (impl instanceof ITrait.IHitTriggerTrait h) {
                h.onHit(state, attacker, figure, data, manaCost, t.params, target, damageDealt, isGolden);
            }
        }
    }
}
