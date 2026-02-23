package bruhof.teenycraft.battle.effect;

import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.battle.BattleFigure;
import bruhof.teenycraft.battle.AbilityExecutor;
import bruhof.teenycraft.capability.BattleStateProvider;
import bruhof.teenycraft.capability.IBattleState;
import bruhof.teenycraft.util.AbilityLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EffectApplierRegistry {
    private static final Map<String, EffectApplier> REGISTRY = new HashMap<>();

    public static void register(String id, EffectApplier applier) {
        REGISTRY.put(id, applier);
    }

    public static EffectApplier get(String id) {
        return REGISTRY.getOrDefault(id, (state, player, figure, data, manaCost, params, target) -> {
            // Default Fallback Logic
            float paramMult = (!params.isEmpty()) ? params.get(0) : 1.0f;
            int magnitude = (int) paramMult;
            int duration = (params.size() > 1) ? params.get(1).intValue() : -1;
            
            if (target instanceof ServerPlayer targetP) {
                targetP.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(targetState -> {
                    targetState.applyEffect(id, duration, magnitude);
                    if (targetP == player) {
                        player.sendSystemMessage(Component.literal("§dApplied " + id + " (Mag: " + magnitude + ")"));
                    }
                });
            }
        });
    }

    public static void init() {
        // HEAL
        EffectApplier healApplier = (state, player, figure, data, manaCost, params, target) -> {
            float paramMult = (!params.isEmpty()) ? params.get(0) : 1.0f;
            int magnitude = EffectCalculator.calculateHealMagnitude(figure, manaCost, paramMult);
            
            if (target instanceof ServerPlayer targetP) {
                targetP.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(targetState -> {
                    if (targetState.hasEffect("kiss")) {
                        targetP.sendSystemMessage(Component.literal("§d§lKISS! §cPositive effects blocked!"));
                    } else {
                        targetState.applyEffect("heal", 0, magnitude);
                        targetP.sendSystemMessage(Component.literal("§aHealed " + magnitude + " HP!"));
                        if (targetP != player) player.sendSystemMessage(Component.literal("§aHealed target for " + magnitude + " HP!"));
                    }
                });
            } else {
                 // Fallback for non-players (Dummies)
                 target.heal(magnitude);
                 player.sendSystemMessage(Component.literal("§aHealed target for " + magnitude + " HP!"));
            }
        };
        register("heal", healApplier);
        register("self:heal", healApplier);

        // GROUP HEAL
        register("group_heal", (state, player, figure, data, manaCost, params, target) -> {
            float paramMult = (!params.isEmpty()) ? params.get(0) : 1.0f;
            int magnitude = EffectCalculator.calculateHealMagnitude(figure, manaCost, paramMult);
            
            // Note: Group heal usually targets the caster's team, regardless of 'target' param
            if (state.hasEffect("kiss")) {
                player.sendSystemMessage(Component.literal("§d§lKISS! §cPositive effects blocked!"));
            } else {
                state.applyEffect("group_heal", 0, magnitude);
                player.sendSystemMessage(Component.literal("§aGroup Heal: " + magnitude + " (Split among team)"));
            }
        });

        // MANA RESTORE (BAR FILL)
        register("bar_fill", (state, player, figure, data, manaCost, params, target) -> {
            float paramMult = (!params.isEmpty()) ? params.get(0) : 1.0f;
            int magnitude = EffectCalculator.calculateManaMagnitude(figure, data, manaCost, paramMult);
            
            if (target instanceof ServerPlayer targetP) {
                targetP.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(targetState -> {
                    if (targetState.hasEffect("kiss")) {
                        targetP.sendSystemMessage(Component.literal("§d§lKISS! §cPositive effects blocked!"));
                    } else {
                        targetState.addMana(magnitude);
                        targetP.sendSystemMessage(Component.literal("§bRestored " + magnitude + " Mana!"));
                        if (targetP != player) player.sendSystemMessage(Component.literal("§bRestored " + magnitude + " Mana to target!"));
                    }
                });
            }
        });

        // MANA DEPLETE (BAR DEPLETE)
        register("bar_deplete", (state, player, figure, data, manaCost, params, target) -> {
            float paramMult = (!params.isEmpty()) ? params.get(0) : 1.0f;
            int magnitude = EffectCalculator.calculateBarDepleteMagnitude(figure, manaCost, paramMult);
            
            if (target instanceof ServerPlayer targetP) {
                targetP.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(targetState -> {
                    if (targetState.hasEffect("cleanse_immunity")) {
                        targetP.sendSystemMessage(Component.literal("§b§lIMMUNE! §7Mana reduction blocked."));
                    } else {
                        targetState.consumeMana(magnitude);
                        targetP.sendSystemMessage(Component.literal("§c§lMANA DEPLETED! §7Lost " + magnitude + " Mana"));
                        if (targetP != player) player.sendSystemMessage(Component.literal("§cDepleted " + magnitude + " of target's mana!"));
                    }
                });
            }
        });

        // POWER UP
        register("power_up", (state, player, figure, data, manaCost, params, target) -> {
            float paramMult = (!params.isEmpty()) ? params.get(0) : 1.0f;
            int magnitude = EffectCalculator.calculatePowerUpMagnitude(figure, manaCost, paramMult);
            
            if (target instanceof ServerPlayer targetP) {
                targetP.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(targetState -> {
                    if (targetState.hasEffect("kiss")) {
                        targetP.sendSystemMessage(Component.literal("§d§lKISS! §cPositive effects blocked!"));
                    } else {
                        int oldMag = targetState.getEffectMagnitude("power_up");
                        targetState.applyEffect("power_up", -1, magnitude);
                        int newMag = targetState.getEffectMagnitude("power_up");
                        
                        targetP.sendSystemMessage(Component.literal("§6§lPOWER UP! §eStrength increased by " + magnitude + " (Total: " + newMag + ")"));
                        if (targetP != player) player.sendSystemMessage(Component.literal("§6Gave target +" + magnitude + " Power! (Total: " + newMag + ")"));
                    }
                });
            }
        });

        // POWER DOWN
        register("power_down", (state, player, figure, data, manaCost, params, target) -> {
            float paramMult = (!params.isEmpty()) ? params.get(0) : 1.0f;
            int magnitude = EffectCalculator.calculatePowerDownMagnitude(figure, manaCost, paramMult);
            
            if (target instanceof ServerPlayer targetP) {
                targetP.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(targetState -> {
                    if (targetState.hasEffect("cleanse_immunity")) {
                        targetP.sendSystemMessage(Component.literal("§b§lIMMUNE! §7Strength reduction blocked."));
                    } else {
                        int oldMag = targetState.getEffectMagnitude("power_down");
                        targetState.applyEffect("power_down", -1, magnitude);
                        int newMag = targetState.getEffectMagnitude("power_down");
                        
                        targetP.sendSystemMessage(Component.literal("§c§lPOWER DOWN! §7Strength reduced by " + magnitude + " (Total: " + newMag + ")"));
                        if (targetP != player) player.sendSystemMessage(Component.literal("§cLowered target's power by " + magnitude + " (Total: " + newMag + ")"));
                    }
                });
            }
        });

        // DEFENSE UP
        register("defense_up", (state, player, figure, data, manaCost, params, target) -> {
            float paramMult = (!params.isEmpty()) ? params.get(0) : 1.0f;
            int duration = EffectCalculator.calculateDefenseUpDuration(figure, manaCost, paramMult);
            int magnitude = EffectCalculator.calculateDefenseMagnitude(manaCost, paramMult);
            
            if (target instanceof ServerPlayer targetP) {
                targetP.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(targetState -> {
                    if (targetState.hasEffect("kiss")) {
                        targetP.sendSystemMessage(Component.literal("§d§lKISS! §cPositive effects blocked!"));
                    } else {
                        targetState.applyEffect("defense_up", duration, magnitude, manaCost);
                        targetP.sendSystemMessage(Component.literal("§9§lDEFENSE UP! §fIncoming damage -" + magnitude + "% (" + (duration/20.0) + "s)"));
                        if (targetP != player) player.sendSystemMessage(Component.literal("§9Buffed target's defense by " + magnitude + "%!"));
                    }
                });
            }
        });

        // DEFENSE DOWN
        register("defense_down", (state, player, figure, data, manaCost, params, target) -> {
            float paramMult = (!params.isEmpty()) ? params.get(0) : 1.0f;
            int duration = EffectCalculator.calculateDefenseDownDuration(figure, manaCost, paramMult);
            int magnitude = EffectCalculator.calculateDefenseMagnitude(manaCost, paramMult);
            
            if (target instanceof ServerPlayer targetP) {
                targetP.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(targetState -> {
                    if (targetState.hasEffect("cleanse_immunity")) {
                        targetP.sendSystemMessage(Component.literal("§b§lIMMUNE! §7Defense reduction blocked."));
                    } else {
                        targetState.applyEffect("defense_down", duration, magnitude, manaCost);
                        targetP.sendSystemMessage(Component.literal("§c§lDEFENSE DOWN! §7Incoming damage +" + magnitude + "% (" + (duration/20.0) + "s)"));
                        if (targetP != player) player.sendSystemMessage(Component.literal("§cLowered target's defense by " + magnitude + "%!"));
                    }
                });
            }
        });

        // STUN
        register("stun", (state, player, figure, data, manaCost, params, target) -> {
            float paramMult = (!params.isEmpty()) ? params.get(0) : 1.0f;
            int duration = EffectCalculator.calculateStunDuration(figure, manaCost, paramMult);
            
            if (target instanceof ServerPlayer targetP) {
                targetP.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(targetState -> {
                    if (targetState.hasEffect("cleanse_immunity")) {
                        targetP.sendSystemMessage(Component.literal("§b§lIMMUNE! §7Stun blocked."));
                    } else {
                        targetState.applyEffect("stun", duration, 1);
                        targetP.sendSystemMessage(Component.literal("§eYou are STUNNED for " + (duration/20.0) + "s!"));
                        if (targetP != player) player.sendSystemMessage(Component.literal("§eStunned target for " + (duration/20.0) + "s!"));
                    }
                });
            }
        });

        // ROOT
        register("root", (state, player, figure, data, manaCost, params, target) -> {
            float paramMult = (!params.isEmpty()) ? params.get(0) : 1.0f;
            int duration = EffectCalculator.calculateRootDuration(figure, manaCost, paramMult);
            
            if (target instanceof ServerPlayer targetP) {
                targetP.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(targetState -> {
                    if (targetState.hasEffect("cleanse_immunity")) {
                        targetP.sendSystemMessage(Component.literal("§b§lIMMUNE! §7Root blocked."));
                    } else {
                        targetState.applyEffect("root", duration, 1);
                        targetP.sendSystemMessage(Component.literal("§c§lROOTED! §7You cannot swap figures (" + (duration/20.0) + "s)"));
                        if (targetP != player) player.sendSystemMessage(Component.literal("§cRooted target for " + (duration/20.0) + "s!"));
                    }
                });
            }
        });

        // DISABLE
        register("disable", (state, player, figure, data, manaCost, params, target) -> {
            float paramMult = (!params.isEmpty()) ? params.get(0) : 1.0f;
            int duration = EffectCalculator.calculateDisableDuration(figure, manaCost, paramMult);
            
            if (target instanceof ServerPlayer targetP) {
                targetP.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(targetState -> {
                    int activeIndex = targetState.getActiveFigureIndex();
                    targetState.disableFigure(activeIndex, duration, targetP);
                    if (targetP != player) player.sendSystemMessage(Component.literal("§cDisabled target's active figure!"));
                });
            }
        });

        // POISON
        register("poison", (state, player, figure, data, manaCost, params, target) -> {
            float paramMult = (!params.isEmpty()) ? params.get(0) : 1.0f;
            int amount = EffectCalculator.calculatePoisonAmount(manaCost, paramMult);
            int interval = EffectCalculator.calculatePoisonInterval(figure, manaCost, paramMult);
            int dmgTier = (data != null) ? data.damageTier : 0;
            
            IBattleState attackerState = player.getCapability(BattleStateProvider.BATTLE_STATE).orElse(null);
            float totalDmg = EffectCalculator.calculateRawBaseDamage(figure, attackerState, manaCost, dmgTier);
            float damagePerTick = totalDmg / amount;
            int totalDuration = amount * interval;
            
            if (target instanceof ServerPlayer targetP) {
                targetP.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(targetState -> {
                    if (targetState.hasEffect("cleanse_immunity")) {
                        targetP.sendSystemMessage(Component.literal("§b§lIMMUNE! §7Poison blocked."));
                    } else {
                        targetState.applyEffect("poison", totalDuration, interval, damagePerTick, player.getUUID());
                        targetP.sendSystemMessage(Component.literal("§2§lPOISONED! §7Taking periodic damage for " + (totalDuration/20.0) + "s"));
                        if (targetP != player) player.sendSystemMessage(Component.literal("§2Poisoned target for " + (totalDuration/20.0) + "s!"));
                    }
                });
            }
        });

        // SHOCK
        register("shock", (state, player, figure, data, manaCost, params, target) -> {
            float paramMult = (!params.isEmpty()) ? params.get(0) : 1.0f;
            int amount = EffectCalculator.calculateShockAmount(manaCost, paramMult);
            int interval = EffectCalculator.calculateShockInterval(figure, manaCost, paramMult);
            float lenMult = (params.size() > 1) ? params.get(1) : 1.0f;
            int shockLength = EffectCalculator.calculateShockLength(figure, manaCost, lenMult);
            int totalDuration = amount * interval;
            
            if (target instanceof ServerPlayer targetP) {
                targetP.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(targetState -> {
                    if (targetState.hasEffect("cleanse_immunity")) {
                        targetP.sendSystemMessage(Component.literal("§b§lIMMUNE! §7Shock blocked."));
                    } else {
                        targetState.applyEffect("shock", totalDuration, interval, (float) shockLength);
                        targetP.sendSystemMessage(Component.literal("§e§lSHOCK! §7Periodic stuns for " + (totalDuration/20.0) + "s"));
                        if (targetP != player) player.sendSystemMessage(Component.literal("§eShocked target for " + (totalDuration/20.0) + "s!"));
                    }
                });
            }
        });

        // DANCE
        register("dance", (state, player, figure, data, manaCost, params, target) -> {
            float paramMult = (!params.isEmpty()) ? params.get(0) : 1.0f;
            int duration = EffectCalculator.calculateDanceDuration(figure, manaCost, paramMult);
            
            if (target instanceof ServerPlayer targetP) {
                targetP.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(targetState -> {
                    if (targetState.hasEffect("kiss")) {
                        targetP.sendSystemMessage(Component.literal("§d§lKISS! §cPositive effects blocked!"));
                    } else {
                        targetState.applyEffect("dance", duration, 1);
                        targetP.sendSystemMessage(Component.literal("§dDance! Mana Regen x2 for " + (duration/20.0) + "s"));
                    }
                });
            }
        });

        // CURSE
        register("curse", (state, player, figure, data, manaCost, params, target) -> {
            float paramMult = (!params.isEmpty()) ? params.get(0) : 1.0f;
            int duration = EffectCalculator.calculateCurseDuration(figure, manaCost, paramMult);
            
            if (target instanceof ServerPlayer targetP) {
                targetP.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(targetState -> {
                    if (targetState.hasEffect("cleanse_immunity")) {
                        targetP.sendSystemMessage(Component.literal("§b§lIMMUNE! §7Curse blocked."));
                    } else {
                        targetState.applyEffect("curse", duration, 1);
                        targetP.sendSystemMessage(Component.literal("§5You are CURSED! Mana Regen reduced. (" + (duration/20.0) + "s)"));
                        if (targetP != player) player.sendSystemMessage(Component.literal("§5Cursed target for " + (duration/20.0) + "s!"));
                    }
                });
            }
        });

        // CLEANSE
        register("cleanse", (state, player, figure, data, manaCost, params, target) -> {
            float paramMult = (!params.isEmpty()) ? params.get(0) : 1.0f;
            int duration = EffectCalculator.calculateCleanseDuration(figure, manaCost, paramMult);
            
            if (target instanceof ServerPlayer targetP) {
                targetP.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(targetState -> {
                    targetState.applyEffect("cleanse", duration, 1);
                });
            }
        });

        // KISS
        register("kiss", (state, player, figure, data, manaCost, params, target) -> {
            float paramMult = (!params.isEmpty()) ? params.get(0) : 1.0f;
            int duration = EffectCalculator.calculateKissDuration(figure, manaCost, paramMult);
            
            if (target instanceof ServerPlayer targetP) {
                targetP.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(targetState -> {
                    if (targetState.hasEffect("cleanse_immunity")) {
                        targetP.sendSystemMessage(Component.literal("§b§lIMMUNE! §7Kiss blocked."));
                    } else {
                        targetState.applyEffect("kiss", duration, 1);
                        targetP.sendSystemMessage(Component.literal("§d§lKISS! §cPositive effects blocked (" + (duration/20.0) + "s)"));
                        if (targetP != player) player.sendSystemMessage(Component.literal("§dKissed target for " + (duration/20.0) + "s!"));
                    }
                });
            }
        });

        // SHIELD
        register("shield", (state, player, figure, data, manaCost, params, target) -> {
            float paramMult = (!params.isEmpty()) ? params.get(0) : 1.0f;
            int duration = EffectCalculator.calculateShieldDuration(figure, manaCost, paramMult);
            
            if (target instanceof ServerPlayer targetP) {
                targetP.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(targetState -> {
                    if (targetState.hasEffect("kiss")) {
                        targetP.sendSystemMessage(Component.literal("§d§lKISS! §cPositive effects blocked!"));
                    } else {
                        targetState.applyEffect("shield", duration, 1);
                        targetP.sendSystemMessage(Component.literal("§9§lSHIELD! §fNext hit negated (" + (duration/20.0) + "s)"));
                        if (targetP != player) player.sendSystemMessage(Component.literal("§9Gave target a shield for " + (duration/20.0) + "s!"));
                    }
                });
            }
        });

        // DODGE SMOKE
        register("dodge_smoke", (state, player, figure, data, manaCost, params, target) -> {
            float paramMult = (!params.isEmpty()) ? params.get(0) : 1.0f;
            int duration = EffectCalculator.calculateDodgeSmokeDuration(figure, manaCost, paramMult);
            int magnitude = TeenyBalance.DODGE_SMOKE_USES;
            
            if (target instanceof ServerPlayer targetP) {
                targetP.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(targetState -> {
                    if (targetState.hasEffect("kiss")) {
                        targetP.sendSystemMessage(Component.literal("§d§lKISS! §cPositive effects blocked!"));
                    } else {
                        targetState.applyEffect("dodge_smoke", duration, magnitude, manaCost);
                        targetP.sendSystemMessage(Component.literal("§7§lSMOKE! §fDodge chance up! (Charges: " + magnitude + ")"));
                        if (targetP != player) player.sendSystemMessage(Component.literal("§7Gave target dodge smoke!"));
                    }
                });
            }
        });

        // FREEZE
        register("freeze", (state, player, figure, data, manaCost, params, target) -> {
            float paramMult = (!params.isEmpty()) ? params.get(0) : 1.0f;
            int magnitude = EffectCalculator.calculateFreezeMagnitude(figure, manaCost, paramMult);
            float durMult = (params.size() > 1) ? params.get(1) : 1.0f;
            int duration = EffectCalculator.calculateFreezeDuration(figure, manaCost, durMult);
            
            if (target instanceof ServerPlayer targetP) {
                targetP.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(targetState -> {
                    if (targetState.hasEffect("cleanse_immunity")) {
                        targetP.sendSystemMessage(Component.literal("§b§lIMMUNE! §7Freeze blocked."));
                    } else {
                        targetState.applyEffect("freeze", duration, magnitude);
                        targetP.sendSystemMessage(Component.literal("§b§lFROZEN! §3Mana Burn: " + magnitude + "% (" + (duration/20.0) + "s)"));
                        if (targetP != player) player.sendSystemMessage(Component.literal("§bFrozen target! Burned " + magnitude + "% Mana."));
                    }
                });
            }
        });

        // WAFFLE
        EffectApplier waffleApplier = (state, player, figure, data, manaCost, params, target) -> {
            float paramMult = (!params.isEmpty()) ? params.get(0) : 1.0f;
            if (Math.random() < paramMult) {
                float durMult = (params.size() > 1) ? params.get(1) : 1.0f;
                int duration = EffectCalculator.calculateWaffleDuration(figure, manaCost, durMult);
                int blockedSlot = (int) (Math.random() * 3);
                
                if (target instanceof ServerPlayer targetP) {
                    targetP.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(targetState -> {
                        if (targetState.hasEffect("cleanse_immunity")) {
                             targetP.sendSystemMessage(Component.literal("§b§lIMMUNE! §7Waffle blocked."));
                        } else {
                            targetState.removeEffect("waffle");
                            targetState.applyEffect("waffle", duration, blockedSlot);
                            targetP.sendSystemMessage(Component.literal("§6§lWAFFLED! §eSlot " + (blockedSlot+1) + " Blocked (" + (duration/20.0) + "s)"));
                            if (targetP != player) player.sendSystemMessage(Component.literal("§6Waffled target's Slot " + (blockedSlot+1) + "!"));
                        }
                    });
                }
            }
        };
        register("waffle", waffleApplier);
        register("waffle_chance", waffleApplier);

        // DISPEL
        register("dispel", (state, player, figure, data, manaCost, params, target) -> {
            if (target instanceof ServerPlayer targetP) {
                targetP.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(targetState -> {
                    if (targetState.hasEffect("cleanse_immunity")) {
                        targetP.sendSystemMessage(Component.literal("§b§lIMMUNE! §7Dispel failed."));
                    } else {
                        targetState.applyEffect("dispel", 0, 1);
                        targetP.sendSystemMessage(Component.literal("§c§lDISPELLED! §7Your buffs were removed."));
                        if (targetP != player) player.sendSystemMessage(Component.literal("§bDispelled opponent's buffs!"));
                    }
                });
            }
        });

        // SELF SHOCK
        register("self_shock", (state, player, figure, data, manaCost, params, target) -> {
            float paramMult = (!params.isEmpty()) ? params.get(0) : 1.0f;
            int magnitude = EffectCalculator.calculateSelfShockDamage(figure, manaCost, paramMult);
            
            // This specifically targets the caster
            player.hurt(player.damageSources().magic(), 0.01f);
            player.setHealth(player.getMaxHealth());
            player.sendSystemMessage(Component.literal("§cSelf Shock! Took " + magnitude + " damage!"));
            state.applyEffect("self_shock", 0, magnitude);
        });

        // TOFU SPAWN
        register("tofu_spawn", (state, player, figure, data, manaCost, params, target) -> {
            float paramMult = (!params.isEmpty()) ? params.get(0) : 1.0f;
            int magnitude = (int) (TeenyBalance.TOFU_BASE_MANA * (1.0f + (manaCost * TeenyBalance.TOFU_ABILITY_PERMANA)) * paramMult);
            
            player.sendSystemMessage(Component.literal("§6§lTOFU SPAWNED! (Power: " + magnitude + ")"));
            state.spawnTofu(magnitude);
            state.refreshPlayerInventory(player);
        });

        // HEALTH RADIO
        register("health_radio", (state, player, figure, data, manaCost, params, target) -> {
            float intervalParam = (!params.isEmpty()) ? params.get(0) : 1.0f;
            float magParam = (params.size() > 1) ? params.get(1) : 1.0f;
            
            int totalMag = EffectCalculator.calculateHealMagnitude(figure, manaCost, magParam);
            int amount = EffectCalculator.calculateRadioAmount(manaCost, 1.0f);
            int interval = EffectCalculator.calculateRadioInterval(figure, manaCost, intervalParam);
            
            if (target instanceof ServerPlayer targetP) {
                targetP.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(targetState -> {
                    if (targetState.hasEffect("kiss")) {
                        targetP.sendSystemMessage(Component.literal("§d§lKISS! §cPositive effects blocked!"));
                    } else {
                        targetState.applyEffect("health_radio", amount * interval, interval, totalMag, player.getUUID());
                        targetP.sendSystemMessage(Component.literal("§a§lHEALTH RADIO! §7Healing over time..."));
                        if (targetP != player) player.sendSystemMessage(Component.literal("§aApplied Health Radio to target!"));
                    }
                });
            }
        });

        // POWER RADIO
        register("power_radio", (state, player, figure, data, manaCost, params, target) -> {
            float intervalParam = (!params.isEmpty()) ? params.get(0) : 1.0f;
            float magParam = (params.size() > 1) ? params.get(1) : 1.0f;
            
            int totalMag = EffectCalculator.calculatePowerUpMagnitude(figure, manaCost, magParam);
            int amount = EffectCalculator.calculateRadioAmount(manaCost, 1.0f);
            int interval = EffectCalculator.calculateRadioInterval(figure, manaCost, intervalParam);
            
            if (target instanceof ServerPlayer targetP) {
                targetP.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(targetState -> {
                    if (targetState.hasEffect("kiss")) {
                        targetP.sendSystemMessage(Component.literal("§d§lKISS! §cPositive effects blocked!"));
                    } else {
                        targetState.applyEffect("power_radio", amount * interval, interval, totalMag, player.getUUID());
                        targetP.sendSystemMessage(Component.literal("§6§lPOWER RADIO! §7Increasing power over time..."));
                        if (targetP != player) player.sendSystemMessage(Component.literal("§6Applied Power Radio to target!"));
                    }
                });
            }
        });
    }
}
