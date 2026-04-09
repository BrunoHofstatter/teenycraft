package bruhof.teenycraft.battle;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;

import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.item.custom.ItemFigure;
import bruhof.teenycraft.util.AbilityLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Collections;

import bruhof.teenycraft.battle.damage.DamagePipeline;
import bruhof.teenycraft.battle.damage.DamagePipeline.DamageResult;
import bruhof.teenycraft.battle.effect.EffectApplierRegistry;
import bruhof.teenycraft.capability.BattleStateProvider;
import bruhof.teenycraft.networking.ModMessages;
import bruhof.teenycraft.networking.PacketSyncBattleData;
import bruhof.teenycraft.world.dimension.ModDimensions;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import java.util.Set;

import bruhof.teenycraft.capability.IBattleState;

public class AbilityExecutor {

    /**
     * Triggered by Left-Click (Attack). Only works for MELEE abilities.
     */
    public static void executeAttack(LivingEntity attacker, BattleFigure figure, int slotIndex, Entity target) {
        if (!(target instanceof LivingEntity livingTarget)) return;
        
        AbilityLoader.AbilityData data = getAbilityData(figure, slotIndex);
        if (data == null || !"melee".equalsIgnoreCase(data.hitType)) return;

        attacker.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(state -> {
            if (state.hasEffect("reset_lock")) {
                if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§eWaiting for reset..."));
                return;
            }

            int locked = state.getLockedSlot();
            if (locked != -1 && locked != slotIndex) {
                if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§cAction locked to slot " + (locked + 1)));
                return;
            }

            java.util.ArrayList<String> tiers = ItemFigure.getAbilityTiers(figure.getOriginalStack());
            String tierLetter = (slotIndex < tiers.size()) ? tiers.get(slotIndex) : "a";
            int manaCost = TeenyBalance.getManaCost(slotIndex + 1, tierLetter);

            if (state.hasEffect("stun") || state.isCharging() || state.isBlueChanneling()) {
                if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§cCannot attack right now!"));
                return;
            }
            
            if (state.hasEffect("waffle")) {
                int blockedSlot = state.getEffectMagnitude("waffle");
                if (blockedSlot == slotIndex) {
                     if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§eSlot " + (slotIndex + 1) + " is Waffled!"));
                     return;
                }
            }

            if (state.getCurrentMana() < manaCost) {
                if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§cNot enough Mana! Need " + manaCost));
                return;
            }

            executeCommon(state, attacker, figure, slotIndex, data, livingTarget, true, true);
        });
    }

    /**
     * Triggered by Right-Click (Use Item). Only works for RANGED/SELF abilities.
     */
    public static void executeAction(LivingEntity attacker, BattleFigure figure, int slotIndex) {
        AbilityLoader.AbilityData data = getAbilityData(figure, slotIndex);
        if (data == null) return;
        if ("melee".equalsIgnoreCase(data.hitType)) return;

        attacker.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(state -> {
            if (state.hasEffect("reset_lock")) {
                if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§eWaiting for reset..."));
                return;
            }

            int locked = state.getLockedSlot();
            if (locked != -1 && locked != slotIndex) {
                if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§cAction locked to slot " + (locked + 1)));
                return;
            }

            java.util.ArrayList<String> tiers = ItemFigure.getAbilityTiers(figure.getOriginalStack());
            String tierLetter = (slotIndex < tiers.size()) ? tiers.get(slotIndex) : "a";
            int manaCost = TeenyBalance.getManaCost(slotIndex + 1, tierLetter);

            if (state.hasEffect("stun") || state.isCharging() || state.isBlueChanneling()) {
                if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§cCannot attack right now!"));
                return;
            }
            
            if (state.hasEffect("waffle")) {
                int blockedSlot = state.getEffectMagnitude("waffle");
                if (blockedSlot == slotIndex) {
                     if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§eSlot " + (slotIndex + 1) + " is Waffled!"));
                     return;
                }
            }

            if (state.getCurrentMana() < manaCost) {
                if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§cNot enough Mana! Need " + manaCost));
                return;
            }

            // --- REMOTE MINE STAGE 2 (DETONATE) ---
            if (data.effectsOnOpponent != null) {
                boolean isRemoteMine = data.effectsOnOpponent.stream().anyMatch(e -> "remote_mine".equals(e.id));
                if (isRemoteMine) {
                    LivingEntity target = findNearestOpponent(attacker);
                    if (target != null) {
                        IBattleState ts = target.getCapability(BattleStateProvider.BATTLE_STATE).orElse(null);
                        if (ts != null && ts.hasEffect("remote_mine_" + slotIndex)) {
                            bruhof.teenycraft.battle.effect.EffectInstance mine = ts.getEffectInstance("remote_mine_" + slotIndex);
                            if (attacker.getUUID().equals(mine.casterUUID)) {
                                state.consumeMana(manaCost);
                                if (detonateMine(attacker, state, target, ts, slotIndex, mine) > 0) {
                                    awardBatteryFromManaSpent(state, manaCost);
                                }
                                return;
                            }
                        }
                    }
                }
            }

            LivingEntity target = null;
            boolean isRanged = "raycasting".equalsIgnoreCase(data.hitType) || "ranged".equalsIgnoreCase(data.hitType);

            if (isRanged) {
                 target = getConeTarget(attacker, data);
                 
                 // --- REMOTE MINE RANGED DETONATE ---
                 if (data.effectsOnOpponent != null) {
                    boolean isRemoteMine = data.effectsOnOpponent.stream().anyMatch(e -> "remote_mine".equals(e.id));
                    if (isRemoteMine && target != null) {
                        IBattleState ts = target.getCapability(BattleStateProvider.BATTLE_STATE).orElse(null);
                        if (ts != null && ts.hasEffect("remote_mine_" + slotIndex)) {
                            bruhof.teenycraft.battle.effect.EffectInstance mine = ts.getEffectInstance("remote_mine_" + slotIndex);
                            if (attacker.getUUID().equals(mine.casterUUID)) {
                                state.consumeMana(manaCost);
                                if (detonateMine(attacker, state, target, ts, slotIndex, mine) > 0) {
                                    awardBatteryFromManaSpent(state, manaCost);
                                }
                                return;
                            }
                        }
                    }
                 }

                 boolean hasCharge = data.traits != null && data.traits.stream().anyMatch(t -> "charge_up".equals(t.id));

                 if (target == null && !hasCharge) {
                     state.consumeMana(manaCost);
                     if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§cNo target found!"));
                     return;
                 }
                 
                 boolean isGolden = ItemFigure.isAbilityGolden(figure.getOriginalStack(), data.id);
                 if (data.raycastDelayTier > 0) {
                     if (target == null) {
                         state.consumeMana(manaCost);
                         if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§cNo target found!"));
                         return;
                     }
                     state.consumeMana(manaCost);
                     double distance = attacker.getEyePosition().distanceTo(target.getEyePosition());
                     int delayTicks = (int) (distance * TeenyBalance.getRaycastDelay(data.raycastDelayTier));
                     
                     // Ensure minimum 1 tick if it's supposed to have a delay, even if point-blank
                     if (delayTicks < 1) delayTicks = 1;

                     state.addProjectile(new IBattleState.PendingProjectile(data, slotIndex, isGolden, manaCost, target.getUUID(), attacker.getEyePosition(), delayTicks, figure));
                     
                     if (attacker instanceof ServerPlayer sp) {
                         sp.sendSystemMessage(Component.literal("§eProjectile fired! (" + (delayTicks/20.0f) + "s)"));
                     }
                     executeSelfEffectsOnly(state, attacker, figure, data, manaCost, isGolden);
                     return;
                 }
            } else {
                 target = attacker; 
            }

            executeCommon(state, attacker, figure, slotIndex, data, target, true, true);
        });
    }

    private static void executeSelfEffectsOnly(IBattleState attackerState, LivingEntity attacker, BattleFigure figure, AbilityLoader.AbilityData data, int manaCost, boolean isGolden) {
        if ("none".equalsIgnoreCase(data.hitType)) {
            if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§d[Effect] Casting Buffs..."));
            for (AbilityLoader.TraitData trait : data.traits) {
                EffectApplierRegistry.get(trait.id).apply(attackerState, attacker, figure, data, manaCost, trait.params, attacker);
            }
        }

        for (AbilityLoader.EffectData effect : data.effectsOnSelf) {
            EffectApplierRegistry.get(effect.id).apply(attackerState, attacker, figure, data, manaCost, effect.params, attacker);
        }

        if (isGolden) {
            if ("none".equalsIgnoreCase(data.hitType) && attacker instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal("§6[Golden] Bonus Activated!"));
            }
            for (String bonus : data.goldenBonus) {
                String[] parts = bonus.split(":");
                if (parts.length >= 3 && "self".equalsIgnoreCase(parts[0])) {
                    java.util.ArrayList<Float> params = new java.util.ArrayList<>();
                    params.add(Float.parseFloat(parts[2]));
                    EffectApplierRegistry.get(parts[1]).apply(attackerState, attacker, figure, data, manaCost, params, attacker);
                }
            }
        }
        
        if (attacker instanceof Player p) p.resetAttackStrengthTicker();
        if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§aUsed " + data.name + "!"));
    }
    
    public static void executeDebugCast(IBattleState casterState, LivingEntity caster, BattleFigure figure, AbilityLoader.AbilityData data, int manaCost, boolean isGolden, LivingEntity enemy) {
        LivingEntity finalTarget;
        if ("none".equalsIgnoreCase(data.hitType)) {
            finalTarget = caster;
        } else {
            finalTarget = enemy;
        }

        if (finalTarget == null) {
            if (caster instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§cCast failed: No target determined!"));
            return;
        }
        
        executeCommon(casterState, caster, figure, data, manaCost, finalTarget, false, false, isGolden);
    }

    public static void executeTofu(LivingEntity caster, IBattleState state) {
        if (state.getLockedSlot() != -1) {
            if (caster instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§cCannot use Tofu right now!"));
            return;
        }
        float virtualMana = state.getCurrentTofuMana();
        if (virtualMana <= 0) return;

        state.spawnTofu(0);
        if (caster instanceof ServerPlayer sp) state.refreshPlayerInventory(sp);

        String[] selfEffects = {"power_up", "heal", "bar_fill", "cleanse", "dance"};
        String[] oppEffects = {"freeze", "stun", "waffle"};
        
        boolean isSelf = Math.random() < (5.0 / 8.0);
        String effectId = isSelf ? selfEffects[(int) (Math.random() * selfEffects.length)] : oppEffects[(int) (Math.random() * oppEffects.length)];

        float mult = switch (effectId) {
            case "heal" -> TeenyBalance.TOFU_HEAL_MULT;
            case "power_up" -> TeenyBalance.TOFU_POWER_UP_MULT;
            case "bar_fill" -> TeenyBalance.TOFU_BAR_FILL_MULT;
            case "dance" -> TeenyBalance.TOFU_DANCE_MULT;
            case "cleanse" -> TeenyBalance.TOFU_CLEANSE_MULT;
            case "stun" -> TeenyBalance.TOFU_STUN_MULT;
            case "freeze" -> TeenyBalance.TOFU_FREEZE_MULT;
            case "waffle" -> TeenyBalance.TOFU_WAFFLE_MULT;
            default -> 1.0f;
        };

        BattleFigure active = state.getActiveFigure();
        if (active == null) return;

        if (caster instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§6§lTOFU USED! §fIt was: §e" + effectId.toUpperCase()));

        if (isSelf) {
            EffectApplierRegistry.get(effectId).apply(state, caster, active, null, (int) virtualMana, java.util.Collections.singletonList(mult), caster);
        } else {
            LivingEntity target = caster.level().getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(15), e -> e != caster && e.getCapability(BattleStateProvider.BATTLE_STATE).isPresent())
                .stream().findFirst().orElse(null);
            if (target != null) {
                EffectApplierRegistry.get(effectId).apply(state, caster, active, null, (int) virtualMana, java.util.Collections.singletonList(mult), target);
            }
        }
    }

    private static void executeCommon(IBattleState state, LivingEntity attacker, BattleFigure figure, int slotIndex, AbilityLoader.AbilityData data, LivingEntity target, boolean consumeManaNow, boolean awardBatteryOnSuccess) {
        java.util.ArrayList<String> tiers = ItemFigure.getAbilityTiers(figure.getOriginalStack());
        String tierLetter = (slotIndex < tiers.size()) ? tiers.get(slotIndex) : "a";
        int manaCost = TeenyBalance.getManaCost(slotIndex + 1, tierLetter);
        boolean isGolden = ItemFigure.isAbilityGolden(figure.getOriginalStack(), data.id);
        
        // Flight recast block
        if (state.hasEffect("flight") && data.effectsOnSelf != null && data.effectsOnSelf.stream().anyMatch(e -> "flight".equals(e.id))) {
            if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§cYou are already flying!"));
            return;
        }

        // ==========================================
        // FLIGHT CANCELLATION LOGIC
        // If you want to change it so ANY ability (including buffs) cancels flight, 
        // just remove the `!"none".equalsIgnoreCase(data.hitType)` check here!
        // ==========================================
        if (!"none".equalsIgnoreCase(data.hitType)) {
            if (state.hasEffect("flight")) {
                state.removeEffect("flight");
                if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§eFlight cancelled by attacking!"));
            }
        }

        if (!bruhof.teenycraft.battle.trait.TraitRegistry.triggerExecutionHooks(state, attacker, figure, slotIndex, data, target, isGolden)) {
            return;
        }

        executeCommon(state, attacker, figure, data, manaCost, target, consumeManaNow, awardBatteryOnSuccess, isGolden);
    }

    public static void finishCharge(IBattleState state, LivingEntity attacker) {
        AbilityLoader.AbilityData data = state.getPendingAbility();
        int slot = state.getPendingSlot();
        boolean isGolden = state.isPendingGolden();
        UUID targetUUID = state.getPendingTargetUUID();
        
        state.cancelCharge(); 

        BattleFigure figure = state.getActiveFigure();
        if (figure == null || data == null) return;

        LivingEntity target = null;
        if (TeenyBalance.CHARGE_LOCK_TARGET_ON_START && targetUUID != null) {
            if (attacker.level() instanceof ServerLevel sl) {
                Entity e = sl.getEntity(targetUUID);
                if (e instanceof LivingEntity le) target = le;
            }
        } else {
            boolean isRanged = "raycasting".equalsIgnoreCase(data.hitType) || "ranged".equalsIgnoreCase(data.hitType);
            if (isRanged) {
                target = getConeTarget(attacker, data);
            } else {
                target = attacker;
            }
        }

        if (target == null && !"none".equalsIgnoreCase(data.hitType)) {
            if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§cCharge failed: Target lost!"));
            return;
        }

        java.util.ArrayList<String> tiers = ItemFigure.getAbilityTiers(figure.getOriginalStack());
        String tierLetter = (slot < tiers.size()) ? tiers.get(slot) : "a";
        int manaCost = TeenyBalance.getManaCost(slot + 1, tierLetter);

        executeCommon(state, attacker, figure, data, manaCost, target, false, true, isGolden);
    }

    public static void tickBlueChannel(IBattleState state, LivingEntity attacker) {
        int ticks = state.getBlueChannelTicks();
        if (ticks <= 0 || state.getCurrentMana() <= 0 || state.hasEffect("stun") || state.hasEffect("reset_lock")) {
            state.cancelBlueChannel();
            if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§b§lCHANNEL ENDED."));
            return;
        }

        int interval = state.getBlueChannelInterval();
        float totalMana = state.getBlueChannelTotalMana();
        int originalTotalTicks = state.getBlueChannelTotalTicks();
        
        float tickRate = totalMana / originalTotalTicks;
        state.consumeMana(tickRate);

        int ticksActive = originalTotalTicks - ticks;

        if (ticksActive % interval == 0) {
            AbilityLoader.AbilityData data = state.getBlueChannelAbility();
            int slot = state.getBlueChannelSlot();
            boolean isGolden = state.isBlueChannelGolden();
            float totalDamage = state.getBlueChannelTotalDamage();
            float totalHeal = state.getBlueChannelTotalHeal();
            BattleFigure figure = state.getActiveFigure();

            if (figure != null && data != null) {
                int numberOfIntervals = ((originalTotalTicks - 1) / interval) + 1;
                if (numberOfIntervals <= 0) numberOfIntervals = 1;

                int currentIntervalIndex = ticksActive / interval;
                
                int totalCalculatedDamage = Math.round(totalDamage * TeenyBalance.BLUE_DAMAGE_MULT);
                int[] damageSplits = bruhof.teenycraft.battle.damage.DistributionHelper.split(totalCalculatedDamage, numberOfIntervals);
                int damagePerInterval = (currentIntervalIndex < damageSplits.length) ? damageSplits[currentIntervalIndex] : 0;
                
                int totalCalculatedHeal = Math.round(totalHeal * TeenyBalance.BLUE_DAMAGE_MULT);
                int[] healSplits = bruhof.teenycraft.battle.damage.DistributionHelper.split(totalCalculatedHeal, numberOfIntervals);
                int healPerInterval = (currentIntervalIndex < healSplits.length) ? healSplits[currentIntervalIndex] : 0;

                LivingEntity target = null;
                boolean isRanged = "raycasting".equalsIgnoreCase(data.hitType) || "ranged".equalsIgnoreCase(data.hitType);
                if (isRanged) {
                    target = getConeTarget(attacker, data);
                } else if ("melee".equalsIgnoreCase(data.hitType)) {
                    target = findNearestOpponent(attacker); 
                } else {
                    target = attacker;
                }

                if (target != null) {
                    boolean awardedBatteryThisInterval = false;
                    if (damagePerInterval > 0 && target != attacker) {
                        IBattleState targetState = target.getCapability(BattleStateProvider.BATTLE_STATE).orElse(null);
                        BattleFigure targetFigure = (targetState != null) ? targetState.getActiveFigure() : null;
                        DamagePipeline.DamageResult result = new DamagePipeline.DamageResult(damagePerInterval, 1, false, true);
                        int damageDealt = applyDamageToFigure(state, attacker, target, targetState, targetFigure, result, data, 0, isGolden, false, false);
                        if (damageDealt > 0) {
                            awardBatteryFromManaSpent(state, tickRate * interval);
                            awardedBatteryThisInterval = true;
                        }
                    }
                    if (healPerInterval > 0) {
                        state.applyEffect("heal", 0, healPerInterval);
                        if (!awardedBatteryThisInterval) {
                            awardBatteryFromManaSpent(state, tickRate * interval);
                        }
                    }
                }
            }
        }

        state.decrementBlueChannelTicks();
        
        if (state.getBlueChannelTicks() <= 0 || state.getCurrentMana() <= 0) {
            state.cancelBlueChannel();
            if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§b§lCHANNEL ENDED."));
        }
    }

    private static void executeCommon(IBattleState attackerState, LivingEntity attacker, BattleFigure figure, AbilityLoader.AbilityData data, int manaCost, LivingEntity target, boolean consumeManaNow, boolean awardBatteryOnSuccess, boolean isGolden) {
        executeCommon(attackerState, attacker, figure, data, manaCost, target, consumeManaNow, awardBatteryOnSuccess, isGolden, false);
    }

    private static void executeCommon(IBattleState attackerState, LivingEntity attacker, BattleFigure figure, AbilityLoader.AbilityData data, int manaCost, LivingEntity target, boolean consumeManaNow, boolean awardBatteryOnSuccess, boolean isGolden, boolean skipSelfEffects) {
        DamageResult result = DamagePipeline.calculateOutput(attackerState, figure, data, manaCost, isGolden);
        
        if (data.effectsOnOpponent != null) {
            for (AbilityLoader.EffectData e : data.effectsOnOpponent) {
                if ("remote_mine".equals(e.id)) {
                    result.baseDamagePerHit = 0;
                    break;
                }
            }
        }

        if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§7[Debug] Raw Damage: " + result.baseDamagePerHit));

        if (data.particleId != null && !data.particleId.isEmpty()) {
            ResourceLocation particleLoc = new ResourceLocation(data.particleId);
            if (ForgeRegistries.PARTICLE_TYPES.containsKey(particleLoc)) {
                ParticleType<?> particleType = ForgeRegistries.PARTICLE_TYPES.getValue(particleLoc);
                if (particleType instanceof ParticleOptions particleOptions && attacker.level() instanceof ServerLevel sl) {
                    sl.sendParticles(particleOptions, 
                        attacker.getX(), attacker.getEyeY(), attacker.getZ(), 
                        data.particleCount, 0.5, 0.5, 0.5, 0.1);
                }
            }
        }

        if (!"none".equalsIgnoreCase(data.hitType)) {
            // Strip flight on attack
            if (attackerState.hasEffect("flight")) {
                attackerState.removeEffect("flight");
                if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§eFlight cancelled by attacking!"));
            }

            attackerState.triggerOnAttack(figure);
            
            int totalDamageDealt = 0;
            int[] hitSplits = bruhof.teenycraft.battle.damage.DistributionHelper.split(result.baseDamagePerHit, result.hitCount);
            
            for (int damagePart : hitSplits) {
                 DamageResult singleHit = new DamageResult(damagePart, 1, result.isGroupDamage, result.canCrit);
                 singleHit.undodgeable = result.undodgeable;
                 
                 if (result.isGroupDamage) {
                     totalDamageDealt += target.getCapability(BattleStateProvider.BATTLE_STATE).map(targetState -> {
                         List<BattleFigure> team = targetState.getTeam();
                         List<BattleFigure> alive = new java.util.ArrayList<>();
                         for (BattleFigure f : team) {
                             if (f.getCurrentHp() > 0) alive.add(f);
                         }
                         
                         int damageSum = 0;
                         if (!alive.isEmpty()) {
                             int[] groupSplits = bruhof.teenycraft.battle.damage.DistributionHelper.split(damagePart, alive.size());
                             for (int j = 0; j < alive.size(); j++) {
                                 DamageResult enemyHit = new DamageResult(groupSplits[j], 1, false, result.canCrit);
                                 enemyHit.undodgeable = singleHit.undodgeable;
                                 damageSum += applyDamageToFigure(attackerState, attacker, target, targetState, alive.get(j), enemyHit, data, manaCost, isGolden, false, false);
                             }
                         }
                         return damageSum;
                     }).orElse(0);
                 } else {
                     totalDamageDealt += applyDamage(attackerState, attacker, target, singleHit, data, manaCost, isGolden, false);
                 }
            }

            if (totalDamageDealt > 0) {
                if (awardBatteryOnSuccess) {
                    awardBatteryFromManaSpent(attackerState, manaCost);
                }

                rollTofu(attackerState, attacker, figure, data, manaCost, target, isGolden);
                bruhof.teenycraft.battle.trait.TraitRegistry.triggerHitHooks(attackerState, attacker, figure, data, manaCost, target, totalDamageDealt, isGolden);
            }
        } else {
            if (!skipSelfEffects) {
                if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§d[Effect] Casting Buffs..."));
                for (AbilityLoader.TraitData trait : data.traits) {
                    EffectApplierRegistry.get(trait.id).apply(attackerState, attacker, figure, data, manaCost, trait.params, attacker);
                }

                if (awardBatteryOnSuccess) {
                    awardBatteryFromManaSpent(attackerState, manaCost);
                }
            }
        }

        if (!skipSelfEffects) {
            for (AbilityLoader.EffectData effect : data.effectsOnSelf) {
                EffectApplierRegistry.get(effect.id).apply(attackerState, attacker, figure, data, manaCost, effect.params, attacker);
            }

            if (isGolden) {
                if ("none".equalsIgnoreCase(data.hitType) && attacker instanceof ServerPlayer sp) {
                    sp.sendSystemMessage(Component.literal("§6[Golden] Bonus Activated!"));
                }
                for (String bonus : data.goldenBonus) {
                    String[] parts = bonus.split(":");
                    if (parts.length >= 3 && "self".equalsIgnoreCase(parts[0])) {
                        java.util.ArrayList<Float> params = new java.util.ArrayList<>();
                        params.add(Float.parseFloat(parts[2]));
                        EffectApplierRegistry.get(parts[1]).apply(attackerState, attacker, figure, data, manaCost, params, attacker);
                    }
                }
            }
        }
        
        if (consumeManaNow && !skipSelfEffects) {
            attackerState.consumeMana(manaCost);
        }
        
        if (attacker instanceof Player p) p.resetAttackStrengthTicker();
        if (attacker instanceof ServerPlayer sp && !skipSelfEffects) sp.sendSystemMessage(Component.literal("§aUsed " + data.name + "!"));
    }

    public static void resolveProjectile(IBattleState state, LivingEntity caster, IBattleState.PendingProjectile proj) {
        if (proj.attackerFigure == null || proj.data == null || proj.targetUUID == null) return;
        
        Entity targetEntity = ((ServerLevel)caster.level()).getEntity(proj.targetUUID);
        if (!(targetEntity instanceof LivingEntity target)) return;

        // Perform line of sight check from castPosition to target.getEyePosition()
        net.minecraft.world.phys.HitResult hit = caster.level().clip(new net.minecraft.world.level.ClipContext(
            proj.castPosition, 
            target.getEyePosition(), 
            net.minecraft.world.level.ClipContext.Block.COLLIDER, 
            net.minecraft.world.level.ClipContext.Fluid.NONE, 
            caster
        ));

        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            if (caster instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal("§c" + proj.data.name + " hit a wall and missed!"));
            }
            return; // Blocked by wall
        }

        // Hit! We now execute ONLY the damage and opponent effects. 
        // Self-effects and mana were consumed when the projectile was fired.
        executeCommon(state, caster, proj.attackerFigure, proj.data, proj.manaCost, target, false, true, proj.isGolden, true);
    }

    private static LivingEntity findNearestOpponent(LivingEntity attacker) {
        return attacker.level().getEntitiesOfClass(LivingEntity.class, attacker.getBoundingBox().inflate(20), 
            e -> e != attacker && e.getCapability(BattleStateProvider.BATTLE_STATE).isPresent())
            .stream().findFirst().orElse(null);
    }

    private static AbilityLoader.AbilityData getAbilityData(BattleFigure figure, int slotIndex) {
        ItemStack stack = figure.getOriginalStack();
        java.util.ArrayList<String> order = ItemFigure.getAbilityOrder(stack);
        if (slotIndex >= order.size()) return null;
        return AbilityLoader.getAbility(order.get(slotIndex));
    }

    private static LivingEntity getConeTarget(LivingEntity attacker, AbilityLoader.AbilityData data) {
        double maxRange = TeenyBalance.getRangeValue(data.rangeTier);
        double halfAngleRad = Math.toRadians(TeenyBalance.RANGED_CONE_ANGLE / 2.0);
        double startHalfWidth = TeenyBalance.RANGED_START_WIDTH / 2.0;
        
        Vec3 rayOrigin = attacker.getEyePosition();
        Vec3 rayDir = attacker.getLookAngle().normalize();
        
        AABB searchBox = attacker.getBoundingBox().inflate(maxRange);
        List<Entity> entities = attacker.level().getEntities(attacker, searchBox, 
            e -> e instanceof LivingEntity && !e.isSpectator() && e != attacker
        );
        
        LivingEntity closest = null;
        double closestDistSq = Double.MAX_VALUE;

        for (Entity e : entities) {
            AABB targetBox = e.getBoundingBox();
            Vec3 boxCenter = targetBox.getCenter();
            Vec3 toCenter = boxCenter.subtract(rayOrigin);
            double distAlongRay = toCenter.dot(rayDir);
            
            if (distAlongRay < 0 || distAlongRay > maxRange) continue;
            
            Vec3 closestPointOnBox = getClosestPointOnAABB(targetBox, rayOrigin, rayDir);
            Vec3 toClosest = closestPointOnBox.subtract(rayOrigin);
            double dAlong = toClosest.dot(rayDir);
            Vec3 projection = rayOrigin.add(rayDir.scale(dAlong));
            double distToRaySq = closestPointOnBox.distanceToSqr(projection);
            
            double allowedRadius = startHalfWidth + (distAlongRay * Math.tan(halfAngleRad));
            
            if (distToRaySq <= (allowedRadius * allowedRadius)) {
                double totalDistSq = rayOrigin.distanceToSqr(closestPointOnBox);
                if (totalDistSq < closestDistSq) {
                    closestDistSq = totalDistSq;
                    closest = (LivingEntity) e;
                }
            }
        }
        return closest;
    }

    private static Vec3 getClosestPointOnAABB(AABB box, Vec3 origin, Vec3 dir) {
        double x = clamp(origin.x, box.minX, box.maxX);
        double y = clamp(origin.y, box.minY, box.maxY);
        double z = clamp(origin.z, box.minZ, box.maxZ);
        return new Vec3(x, y, z);
    }

    public static void announceDamage(LivingEntity attacker, Entity targetEntity, BattleFigure victim, DamagePipeline.MitigationResult mit, String source) {
        if (victim == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append("§7[").append(source).append("] ");
        sb.append("§cHit ").append(victim.getNickname()).append(": ");
        sb.append("§f").append(mit.finalDamage).append("§c Damage");

        if (mit.isDodged && mit.dodgeReduction > 0) {
            sb.append(" §e(-").append(mit.dodgeReduction).append(" Dodged!)");
        }
        if (mit.isCritical && mit.critBonus > 0) {
            sb.append(" §c§l(+").append(mit.critBonus).append(" Crit!)");
        }
        
        sb.append(" §7(").append(victim.getCurrentHp()).append(" HP left)");

        Component msg = Component.literal(sb.toString());
        if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(msg);
        
        if (targetEntity instanceof ServerPlayer victimP && victimP != attacker) {
            victimP.sendSystemMessage(msg);
        }
    }

    private static void awardBatteryFromManaSpent(IBattleState state, float manaSpent) {
        if (manaSpent <= 0) return;
        state.addBatteryCharge(manaSpent * TeenyBalance.ABILITY_BATTERY_CHARGE_MULT);
    }

    private static int detonateMine(LivingEntity attacker, IBattleState attackerState, LivingEntity target, IBattleState victimState, int slot, bruhof.teenycraft.battle.effect.EffectInstance mine) {
        float maxSnapshot = mine.power;
        int stages = mine.magnitude;
        
        float initial = maxSnapshot * TeenyBalance.REMOTE_MINE_START_PCT;
        float pool = maxSnapshot - initial;
        float weight = pool / TeenyBalance.REMOTE_MINE_STAGES;
        
        int detDmg = Math.round(initial + (stages * weight));
        
        DamageResult result = new DamageResult(detDmg, 1, false, true);
        if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§b§lDETONATING! §fBoom for " + detDmg + " damage."));
        
        int damageDealt = applyDamageToFigure(attackerState, attacker, target, victimState, victimState.getActiveFigure(), result, null, 0, false, false, false);
        victimState.removeEffect("remote_mine_" + slot);
        return damageDealt;
    }

    private static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    private static int applyDamage(IBattleState attackerState, LivingEntity attacker, LivingEntity target, DamageResult result, AbilityLoader.AbilityData data, int manaCost, boolean isGolden, boolean isPetFire) {
        IBattleState victimState = target.getCapability(BattleStateProvider.BATTLE_STATE).orElse(null);
        
        if (victimState != null && victimState.isBattling()) {
            return applyDamageToFigure(attackerState, attacker, target, victimState, victimState.getActiveFigure(), result, data, manaCost, isGolden, false, isPetFire);
        }
        
        target.hurt(attacker.damageSources().mobAttack(attacker), (float) result.baseDamagePerHit);
        if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§cDealt " + result.baseDamagePerHit + " dmg!"));
        return result.baseDamagePerHit;
    }

    public static int applyDamageToFigure(IBattleState attackerState, LivingEntity attacker, LivingEntity targetEntity, IBattleState victimState, BattleFigure victimFigure, DamageResult result, AbilityLoader.AbilityData data, int manaCost, boolean isGolden, boolean isReflected, boolean isPetFire) {
        BattleFigure attackerFig = attackerState.getActiveFigure();
        if (victimFigure == null) return 0;
        
        // Strip flight if it's group damage
        if (result.isGroupDamage && victimState != null && victimState.hasEffect("flight")) {
            victimState.removeEffect("flight");
            if (targetEntity instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§bFlight broken by Group Damage!"));
        }

        DamagePipeline.MitigationResult mitigation = DamagePipeline.calculateMitigation(victimState, victimFigure, attackerState, attackerFig, result, data, isGolden);
        
        int instantDmg = mitigation.finalDamage;
        if (data != null && data.effectsOnOpponent != null) {
            for (AbilityLoader.EffectData e : data.effectsOnOpponent) {
                if ("poison".equals(e.id)) {
                    instantDmg = 0;
                    break;
                }
            }
        }

        if (mitigation.isBlocked) {
             if (mitigation.finalDamage > 0) {
                 if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§b§lSHIELD PIERCED!"));
                 if (targetEntity instanceof ServerPlayer tp) tp.sendSystemMessage(Component.literal("§c§lSHIELD PIERCED!"));
             } else {
                 if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§bBLOCKED!"));
             }
        }
        
        if (instantDmg > 0) {
            victimFigure.modifyHp(-instantDmg);

            // --- PETS LOGIC ---
            if (!isPetFire) {
                // Check Pet Slot 1
                if (attackerState != null && attackerState.hasEffect("pet_slot_1")) {
                    int icd = attackerState.getInternalCooldown("pet_fire_1");
                    if (icd <= 0) {
                        int petMag = attackerState.getEffectMagnitude("pet_slot_1");
                        DamageResult petResult = new DamageResult(petMag, 1, false, false);
                        if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§b§lPET 1! §fFired for " + petMag));
                        applyDamageToFigure(attackerState, attacker, targetEntity, victimState, victimFigure, petResult, null, 0, false, false, true);
                        attackerState.setInternalCooldown("pet_fire_1", TeenyBalance.PET_FIRE_COOLDOWN);
                    }
                }
                // Check Pet Slot 2
                if (attackerState != null && attackerState.hasEffect("pet_slot_2")) {
                    int icd = attackerState.getInternalCooldown("pet_fire_2");
                    if (icd <= 0) {
                        int petMag = attackerState.getEffectMagnitude("pet_slot_2");
                        DamageResult petResult = new DamageResult(petMag, 1, false, false);
                        if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§b§lPET 2! §fFired for " + petMag));
                        applyDamageToFigure(attackerState, attacker, targetEntity, victimState, victimFigure, petResult, null, 0, false, false, true);
                        attackerState.setInternalCooldown("pet_fire_2", TeenyBalance.PET_FIRE_COOLDOWN);
                    }
                }
            }

            // --- REFLECTION LOGIC (Cuteness & Reflect) ---
            if (!isReflected) {
                // 1. Cuteness (Standard)
                int cutenessPercent = victimFigure.getEffectiveStat(StatType.REFLECT_PERCENT, victimState);
                if (cutenessPercent > 0) {
                    int reflectedBase = Math.round(instantDmg * (cutenessPercent / 100.0f));
                    if (reflectedBase > 0) {
                        DamageResult reflectResult = new DamageResult(reflectedBase);
                        reflectResult.canCrit = true;
                        if (targetEntity instanceof ServerPlayer tp) tp.sendSystemMessage(Component.literal("§d§lREFLECTED! §fDealt " + reflectedBase + " back!"));
                        applyDamageToFigure(victimState, targetEntity, attacker, attackerState, attackerFig, reflectResult, null, 0, false, true, false);
                    }
                }

                // 2. Reflect Effect (Special Reduction-based)
                if (victimState.hasEffect("reflect")) {
                    bruhof.teenycraft.battle.effect.EffectInstance inst = victimState.getEffectInstance("reflect");
                    if (inst != null && inst.power > 0) {
                        // Reflect % is based on damage BEFORE reflect reduction
                        // mitigation.finalDamage is already reduced if reflect is active.
                        // We need to 'undo' the reflect reduction to get the base for calculation.
                        float reductionMult = inst.magnitude / 100.0f;
                        float preReflectDamage = (reductionMult > 0) ? (instantDmg / reductionMult) : instantDmg;
                        
                        int reflectedBase = Math.round(preReflectDamage * (inst.power / 100.0f));
                        if (reflectedBase > 0) {
                            DamageResult reflectResult = new DamageResult(reflectedBase);
                            reflectResult.canCrit = true;
                            if (targetEntity instanceof ServerPlayer tp) tp.sendSystemMessage(Component.literal("§b§lREFLECTED! §fDealt " + reflectedBase + " back!"));
                            applyDamageToFigure(victimState, targetEntity, attacker, attackerState, attackerFig, reflectResult, null, 0, false, true, false);
                            victimState.removeEffect("reflect");
                        }
                    }
                }
            }
        }

        if (instantDmg > 0 || mitigation.isDodged || mitigation.isBlocked) {
            announceDamage(attacker, targetEntity, victimFigure, mitigation, (data != null) ? data.name : "Attack");
        }

        // Faint Check
        victimState.checkFaint(targetEntity);
        
        if (!mitigation.isDodged && !isReflected) {
            if (data != null) {
                BattleFigure attackingFigure = attackerState.getActiveFigure();
                for (AbilityLoader.EffectData effect : data.effectsOnOpponent) {
                    EffectApplierRegistry.get(effect.id).apply(attackerState, attacker, attackingFigure, data, manaCost, effect.params, targetEntity);
                }
                
                if (isGolden) {
                    for (String bonus : data.goldenBonus) {
                        String[] parts = bonus.split(":");
                        if (parts.length >= 3 && "opponent".equalsIgnoreCase(parts[0])) {
                            String effectId = parts[1];
                            String paramStr = parts[2];
                            java.util.List<Float> params = new java.util.ArrayList<>();
                            for (String p : paramStr.split(",")) {
                                try { params.add(Float.parseFloat(p)); } catch (Exception e) {}
                            }
                            EffectApplierRegistry.get(effectId).apply(attackerState, attacker, attackingFigure, data, manaCost, params, targetEntity);
                        }
                    }
                }
            }

            if (targetEntity != null && instantDmg > 0) {
                 targetEntity.hurt(attacker.damageSources().mobAttack(attacker), 0.01f);
                 if (targetEntity instanceof Player) {
                    targetEntity.setHealth(targetEntity.getMaxHealth()); 
                 }
            }
        }

        return instantDmg;
    }

    private static void rollTofu(IBattleState state, LivingEntity attacker, BattleFigure figure, AbilityLoader.AbilityData data, int manaCost, LivingEntity target, boolean isGolden) {
        if (state.getCurrentTofuMana() > 0 || data.damageTier <= 0) return;

        float chanceMult = 1.0f;
        float powerMult = 1.0f;

        if (data.traits != null) {
            for (AbilityLoader.TraitData t : data.traits) {
                if ("tofu_chance".equals(t.id)) {
                    chanceMult *= t.params.isEmpty() ? 1.0f : t.params.get(0);
                    if (t.params.size() > 1) powerMult *= t.params.get(1);
                }
            }
        }

        if (isGolden && data.goldenBonus != null) {
            for (String bonus : data.goldenBonus) {
                if (bonus.contains("tofu_chance")) {
                    String[] parts = bonus.split(":");
                    if (parts.length >= 3) {
                        String[] subParts = parts[2].split(",");
                        try {
                            chanceMult *= Float.parseFloat(subParts[0]);
                            if (subParts.length > 1) powerMult *= Float.parseFloat(subParts[1]);
                        } catch (Exception ex) {}
                    }
                }
            }
        }

        float chance = (manaCost * TeenyBalance.TOFU_CHANCE_HIT_PERMANA) * chanceMult;
        if (Math.random() * 100.0 < chance) {
            state.spawnTofu(TeenyBalance.TOFU_BASE_MANA * powerMult);
            if (attacker instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal("§6§lTOFU SPAWNED!"));
                state.refreshPlayerInventory(sp);
            }
        }
    }
}
