package bruhof.teenycraft.battle.trait;

import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.battle.BattleFigure;
import bruhof.teenycraft.battle.damage.DamagePipeline;
import bruhof.teenycraft.battle.executor.BattleAbilityContext;
import bruhof.teenycraft.chip.ChipExecutor;
import bruhof.teenycraft.capability.IBattleState;
import bruhof.teenycraft.util.AbilityLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TraitRegistry {
    private static final Map<String, ITrait> REGISTRY = new HashMap<>();

    public static void register(ITrait trait) {
        REGISTRY.put(trait.getId(), trait);
    }

    public static ITrait get(String id) {
        return REGISTRY.get(id);
    }

    public static Set<String> getRegisteredIds() {
        return new LinkedHashSet<>(REGISTRY.keySet());
    }

    public static Set<String> getSupportedAbilityTraitIds() {
        return getRegisteredIds();
    }

    public static ITrait getValidated(String id) {
        ITrait trait = REGISTRY.get(id);
        if (trait == null) {
            throw new IllegalArgumentException("Unknown validated battle trait id '" + id + "'");
        }
        return trait;
    }

    public static void init() {
        register(new ITrait.IPipelineTrait() {
            @Override public String getId() { return "multi_hit"; }
            @Override public void modifyOutput(DamagePipeline.DamageResult result, List<Float> params) {
                if (!params.isEmpty()) result.hitCount = Math.max(1, params.get(0).intValue());
            }
        });

        register(new ITrait.IPipelineTrait() {
            @Override public String getId() { return "group_damage"; }
            @Override public void modifyOutput(DamagePipeline.DamageResult result, List<Float> params) {
                result.isGroupDamage = true;
            }
        });

        register(new ITrait.IExecutionTrait() {
            @Override public String getId() { return "blue"; }
            @Override public boolean onExecute(IBattleState state, LivingEntity attacker, BattleFigure figure, int slotIndex,
                                               AbilityLoader.AbilityData data, LivingEntity target, List<Float> params, boolean isGolden) {
                float param = params.isEmpty() ? 1.0f : params.get(0);
                int effectiveManaCost = BattleAbilityContext.resolveEffectiveManaCost(figure, slotIndex);

                float totalMana = state.getCurrentMana();
                int numberOfHits = (int) (totalMana * TeenyBalance.BLUE_TICKS_PER_MANA) + TeenyBalance.BLUE_TICKS_FLAT;
                int interval = (int) (TeenyBalance.BLUE_BASE_INTERVAL * param);
                if (interval <= 0) interval = 1;

                DamagePipeline.DamageResult calc = DamagePipeline.calculateOutput(state, figure, data, effectiveManaCost, isGolden);
                int totalDamage = Math.round(calc.baseDamagePerHit * TeenyBalance.BLUE_DAMAGE_MULT);

                float totalHealFloat = 0;
                if (data.effectsOnSelf != null) {
                    for (AbilityLoader.EffectData effect : data.effectsOnSelf) {
                        if ("heal".equals(effect.id) || "eagle".equals(effect.id) || "group_heal".equals(effect.id)) {
                            float magParam = (!effect.params.isEmpty()) ? effect.params.get(0) : 1.0f;
                            totalHealFloat += bruhof.teenycraft.battle.effect.EffectCalculator.calculateHealMagnitude(figure, effectiveManaCost, magParam);
                        }
                    }
                }
                int totalHeal = Math.round(totalHealFloat * TeenyBalance.BLUE_DAMAGE_MULT);

                int maxOutput = Math.max(totalDamage, totalHeal);
                if (maxOutput > 0 && numberOfHits > maxOutput) {
                    int oldDuration = numberOfHits * interval;
                    numberOfHits = maxOutput;
                    interval = oldDuration / numberOfHits;
                }

                int totalTicks = numberOfHits * interval;

                if (attacker instanceof ServerPlayer sp) {
                    sp.sendSystemMessage(Component.literal("§b§lCHANNELING..."));
                }

                state.startBlueChannel(totalTicks, interval, data, slotIndex, isGolden, totalDamage, totalHeal, totalMana);
                return false;
            }
        });

        register(new ITrait.IExecutionTrait() {
            @Override public String getId() { return "charge_up"; }
            @Override public boolean onExecute(IBattleState state, LivingEntity attacker, BattleFigure figure, int slotIndex,
                                               AbilityLoader.AbilityData data, LivingEntity target, List<Float> params, boolean isGolden) {
                float param = params.isEmpty() ? 1.0f : params.get(0);
                int ticks = (int) (TeenyBalance.BASE_CHARGE_DELAY * param);

                boolean hasInstantChance = isGolden
                        && data.hasGoldenBonus(AbilityLoader.GoldenBonusScope.TRAIT, "instant_cast_chance");
                if (!hasInstantChance && data.traits != null) {
                    for (AbilityLoader.TraitData trait : data.traits) {
                        if ("instant_cast_chance".equals(trait.id)) {
                            hasInstantChance = true;
                            break;
                        }
                    }
                }

                if (hasInstantChance && attacker.getRandom().nextFloat() < TeenyBalance.INSTANT_CAST_CHANCE_PERCENTAGE) {
                    if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§6§lINSTANT CAST!"));
                    return true;
                }

                if (ChipExecutor.rollExtraInstantCast(figure, attacker)) {
                    return true;
                }

                if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§e§lCHARGING..."));

                state.consumeMana(BattleAbilityContext.resolveActualManaCost(figure, slotIndex));
                state.startCharge(ticks, data, slotIndex, isGolden, (target != null) ? target.getUUID() : null);
                return false;
            }
        });

        register(new ITrait() {
            @Override public String getId() { return "instant_cast_chance"; }
        });

        // Legacy data id kept explicit as a compatibility alias so validated
        // content is no longer warning-only while gameplay remains unchanged.
        register(new ITrait() {
            @Override public String getId() { return "instant_cast"; }
        });

        register(new ITrait() {
            @Override public String getId() { return "surprise"; }
        });

        // This trait is handled in AbilityExecutor.rollTofu rather than through
        // the standard execution hooks, but it is still part of the live data contract.
        register(new ITrait() {
            @Override public String getId() { return "tofu_chance"; }
        });

        register(new ITrait.IPipelineTrait() {
            @Override public String getId() { return "undodgeable"; }
            @Override public void modifyOutput(DamagePipeline.DamageResult result, List<Float> params) {
                result.undodgeable = true;
            }
        });

        register(new ITrait.IExecutionTrait() {
            @Override public String getId() { return "activate"; }
            @Override public boolean onExecute(IBattleState state, LivingEntity attacker, BattleFigure figure, int slotIndex,
                                               AbilityLoader.AbilityData data, LivingEntity target, List<Float> params, boolean isGolden) {
                float required = params.isEmpty() ? 2.0f : params.get(0);
                int current = state.getSlotProgress(slotIndex) + 1;

                if (current < (int) required) {
                    state.setSlotProgress(slotIndex, current);
                    if (attacker instanceof ServerPlayer sp) {
                        sp.sendSystemMessage(Component.literal("§e§lCHARGING... §f(" + current + "/" + (int) required + ")"));
                    }
                    return false;
                }

                state.setSlotProgress(slotIndex, 0);
                return true;
            }
        });
    }

    public static void triggerPipelineHooks(List<AbilityLoader.TraitData> traits, DamagePipeline.DamageResult result) {
        if (traits == null) return;
        for (AbilityLoader.TraitData trait : traits) {
            ITrait impl = getValidated(trait.id);
            if (impl instanceof ITrait.IPipelineTrait pipelineTrait) {
                pipelineTrait.modifyOutput(result, trait.params);
            }
        }
    }

    public static boolean triggerExecutionHooks(IBattleState state, LivingEntity attacker, BattleFigure figure, int slotIndex,
                                                AbilityLoader.AbilityData data, LivingEntity target, boolean isGolden) {
        if (data.traits != null) {
            for (AbilityLoader.TraitData trait : data.traits) {
                List<Float> finalParams = trait.params;
                if (isGolden) {
                    AbilityLoader.GoldenBonusData goldenBonus =
                            data.findGoldenBonus(AbilityLoader.GoldenBonusScope.TRAIT, trait.id);
                    if (goldenBonus != null) {
                        finalParams = goldenBonus.params();
                    }
                }

                ITrait impl = getValidated(trait.id);
                if (impl instanceof ITrait.IExecutionTrait executionTrait) {
                    if (!executionTrait.onExecute(state, attacker, figure, slotIndex, data, target, finalParams, isGolden)) {
                        return false;
                    }
                }
            }
        }

        if (isGolden) {
            for (AbilityLoader.GoldenBonusData goldenBonus : data.getGoldenBonuses(AbilityLoader.GoldenBonusScope.TRAIT)) {
                String traitId = goldenBonus.targetId();

                boolean alreadyExecuted = false;
                if (data.traits != null) {
                    for (AbilityLoader.TraitData trait : data.traits) {
                        if (trait.id.equals(traitId)) {
                            alreadyExecuted = true;
                            break;
                        }
                    }
                }

                if (!alreadyExecuted) {
                    ITrait impl = getValidated(traitId);
                    if (impl instanceof ITrait.IExecutionTrait executionTrait) {
                        if (!executionTrait.onExecute(state, attacker, figure, slotIndex, data, target, goldenBonus.params(), isGolden)) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    public static void triggerMitigationHooks(List<AbilityLoader.TraitData> traits, DamagePipeline.MitigationResult result, boolean isGolden) {
        if (traits == null) return;
        for (AbilityLoader.TraitData trait : traits) {
            ITrait impl = getValidated(trait.id);
            if (impl instanceof ITrait.IMitigationTrait mitigationTrait) {
                mitigationTrait.modifyMitigation(result, trait.params, isGolden);
            }
        }
    }

    public static void triggerHitHooks(IBattleState state, LivingEntity attacker, BattleFigure figure, AbilityLoader.AbilityData data,
                                       int manaCost, LivingEntity target, int damageDealt, boolean isGolden) {
        if (data.traits == null) return;
        for (AbilityLoader.TraitData trait : data.traits) {
            ITrait impl = getValidated(trait.id);
            if (impl instanceof ITrait.IHitTriggerTrait hitTriggerTrait) {
                hitTriggerTrait.onHit(state, attacker, figure, data, manaCost, trait.params, target, damageDealt, isGolden);
            }
        }
    }
}
