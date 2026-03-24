package bruhof.teenycraft.battle.trait;

import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.battle.BattleFigure;
import bruhof.teenycraft.battle.damage.DamagePipeline;
import bruhof.teenycraft.capability.IBattleState;
import bruhof.teenycraft.item.custom.ItemFigure;
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

        // BLUE CHANNEL
        register(new ITrait.IExecutionTrait() {
            @Override public String getId() { return "blue"; }
            @Override public boolean onExecute(IBattleState state, LivingEntity attacker, BattleFigure figure, int slotIndex, AbilityLoader.AbilityData data, LivingEntity target, List<Float> params, boolean isGolden) {
                float param = params.isEmpty() ? 1.0f : params.get(0);
                
                java.util.ArrayList<String> tiers = ItemFigure.getAbilityTiers(figure.getOriginalStack());
                String tierLetter = (slotIndex < tiers.size()) ? tiers.get(slotIndex) : "a";
                int manaCost = TeenyBalance.getManaCost(slotIndex + 1, tierLetter);
                
                float totalMana = state.getCurrentMana();
                int numberOfHits = (int)(totalMana * TeenyBalance.BLUE_TICKS_PER_MANA) + TeenyBalance.BLUE_TICKS_FLAT;
                int interval = (int)(TeenyBalance.BLUE_BASE_INTERVAL * param);
                if (interval <= 0) interval = 1;
                
                // Pre-calculate Total Damage
                bruhof.teenycraft.battle.damage.DamagePipeline.DamageResult calc = 
                    bruhof.teenycraft.battle.damage.DamagePipeline.calculateOutput(state, figure, data, manaCost, isGolden);
                int totalDamage = Math.round(calc.baseDamagePerHit * TeenyBalance.BLUE_DAMAGE_MULT);

                // Pre-calculate Total Healing (if applicable)
                float totalHealFloat = 0;
                if (data.effectsOnSelf != null) {
                     for (AbilityLoader.EffectData e : data.effectsOnSelf) {
                         if ("heal".equals(e.id) || "eagle".equals(e.id) || "group_heal".equals(e.id)) { 
                              float magParam = (!e.params.isEmpty()) ? e.params.get(0) : 1.0f;
                              totalHealFloat += bruhof.teenycraft.battle.effect.EffectCalculator.calculateHealMagnitude(figure, manaCost, magParam);
                         }
                     }
                }
                int totalHeal = Math.round(totalHealFloat * TeenyBalance.BLUE_DAMAGE_MULT);
                
                // --- STRETCHING LOGIC ---
                // If we have more hits than we have damage (e.g. 10 hits for 2 damage), 
                // we stretch the hits to avoid "dead time" (0 damage hits).
                int maxOutput = Math.max(totalDamage, totalHeal);
                if (maxOutput > 0 && numberOfHits > maxOutput) {
                    int oldDuration = numberOfHits * interval;
                    numberOfHits = maxOutput; // Cap hits at available damage/heal
                    interval = oldDuration / numberOfHits; // Stretch interval to maintain exact same total duration
                }
                
                int totalTicks = numberOfHits * interval;

                if (attacker instanceof ServerPlayer sp) {
                    sp.sendSystemMessage(Component.literal("§b§lCHANNELING..."));
                }
                
                state.startBlueChannel(totalTicks, interval, data, slotIndex, isGolden, totalDamage, totalHeal, totalMana);
                return false; // Block instant execution
            }
        });

        // CHARGE UP
        register(new ITrait.IExecutionTrait() {
            @Override public String getId() { return "charge_up"; }
            @Override public boolean onExecute(IBattleState state, LivingEntity attacker, BattleFigure figure, int slotIndex, AbilityLoader.AbilityData data, LivingEntity target, List<Float> params, boolean isGolden) {
                float param = params.isEmpty() ? 1.0f : params.get(0);
                int ticks = (int) (TeenyBalance.BASE_CHARGE_DELAY * param);
                
                // Check for instant cast chance (Golden Bonus or regular trait)
                boolean hasInstantChance = false;
                if (isGolden && data.goldenBonus != null && data.goldenBonus.contains("trait:instant_cast_chance")) {
                    hasInstantChance = true;
                } else if (data.traits != null) {
                    for (AbilityLoader.TraitData t : data.traits) {
                        if ("instant_cast_chance".equals(t.id)) {
                            hasInstantChance = true;
                            break;
                        }
                    }
                }

                if (hasInstantChance) {
                    if (attacker.getRandom().nextFloat() < TeenyBalance.INSTANT_CAST_CHANCE_PERCENTAGE) {
                        if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§6§lINSTANT CAST!"));
                        return true; // Skip charge and proceed to execution
                    }
                }

                if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§e§lCHARGING..."));
                
                java.util.ArrayList<String> tiers = ItemFigure.getAbilityTiers(figure.getOriginalStack());
                String tierLetter = (slotIndex < tiers.size()) ? tiers.get(slotIndex) : "a";
                int manaCost = TeenyBalance.getManaCost(slotIndex + 1, tierLetter);
                
                state.consumeMana(manaCost); 
                state.startCharge(ticks, data, slotIndex, isGolden, (target != null) ? target.getUUID() : null);
                return false; 
            }
        });

        // INSTANT CAST CHANCE (Marker trait)
        register(new ITrait() {
            @Override public String getId() { return "instant_cast_chance"; }
        });

        // SURPRISE (Marker trait)
        register(new ITrait() {
            @Override public String getId() { return "surprise"; }
        });

        // UNDODGEABLE
        register(new ITrait.IPipelineTrait() {
            @Override public String getId() { return "undodgeable"; }
            @Override public void modifyOutput(DamagePipeline.DamageResult result, List<Float> params) {
                result.undodgeable = true;
            }
        });

        // ACTIVATE
        register(new ITrait.IExecutionTrait() {
            @Override public String getId() { return "activate"; }
            @Override public boolean onExecute(IBattleState state, LivingEntity attacker, BattleFigure figure, int slotIndex, AbilityLoader.AbilityData data, LivingEntity target, List<Float> params, boolean isGolden) {
                float required = params.isEmpty() ? 2.0f : params.get(0);
                int current = state.getSlotProgress(slotIndex) + 1;
                
                if (current < (int)required) {
                    state.setSlotProgress(slotIndex, current);
                    if (attacker instanceof ServerPlayer sp) {
                        sp.sendSystemMessage(Component.literal("§e§lCHARGING... §f(" + current + "/" + (int)required + ")"));
                    }
                    return false;
                } else {
                    state.setSlotProgress(slotIndex, 0);
                    return true;
                }
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
        if (data.traits != null) {
            for (AbilityLoader.TraitData t : data.traits) {
                // If it's golden, we check if the golden bonus overrides this specific trait's parameters
                List<Float> finalParams = t.params;
                if (isGolden && data.goldenBonus != null) {
                    for (String bonus : data.goldenBonus) {
                        String[] parts = bonus.split(":");
                        if (parts.length >= 3 && "trait".equalsIgnoreCase(parts[0]) && t.id.equals(parts[1])) {
                            java.util.List<Float> goldenParams = new java.util.ArrayList<>();
                            for (String p : parts[2].split(",")) {
                                try { goldenParams.add(Float.parseFloat(p)); } catch (Exception ignored) {}
                            }
                            finalParams = goldenParams; // Override with golden params
                        }
                    }
                }
                
                ITrait impl = get(t.id);
                if (impl instanceof ITrait.IExecutionTrait e) {
                    if (!e.onExecute(state, attacker, figure, slotIndex, data, target, finalParams, isGolden)) return false;
                }
            }
        }
        
        // Also check if there are golden traits that aren't in the base traits list at all
        if (isGolden && data.goldenBonus != null) {
             for (String bonus : data.goldenBonus) {
                 String[] parts = bonus.split(":");
                 if (parts.length >= 2 && "trait".equalsIgnoreCase(parts[0])) {
                     String traitId = parts[1];
                     
                     // Check if we already executed it in the base traits loop
                     boolean alreadyExecuted = false;
                     if (data.traits != null) {
                         for (AbilityLoader.TraitData t : data.traits) {
                             if (t.id.equals(traitId)) { alreadyExecuted = true; break; }
                         }
                     }
                     
                     if (!alreadyExecuted) {
                         java.util.List<Float> goldenParams = new java.util.ArrayList<>();
                         if (parts.length >= 3) {
                             for (String p : parts[2].split(",")) {
                                 try { goldenParams.add(Float.parseFloat(p)); } catch (Exception ignored) {}
                             }
                         }
                         ITrait impl = get(traitId);
                         if (impl instanceof ITrait.IExecutionTrait e) {
                             if (!e.onExecute(state, attacker, figure, slotIndex, data, target, goldenParams, isGolden)) return false;
                         }
                     }
                 }
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
