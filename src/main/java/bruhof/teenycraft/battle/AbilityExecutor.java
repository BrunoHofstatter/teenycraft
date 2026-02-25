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

            if (state.hasEffect("stun") || state.isCharging()) {
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

            executeCommon(state, attacker, figure, slotIndex, data, livingTarget, true);
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

            if (state.hasEffect("stun") || state.isCharging()) {
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

            LivingEntity target = null;
            boolean isRanged = "raycasting".equalsIgnoreCase(data.hitType) || "ranged".equalsIgnoreCase(data.hitType);

            if (isRanged) {
                 target = getConeTarget(attacker, data);
                 state.consumeMana(manaCost);
                 
                 if (target == null) {
                     if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§cNo target found! (-" + manaCost + " Mana)"));
                     return;
                 }
            } else {
                 target = attacker; 
                 state.consumeMana(manaCost);
            }

            executeCommon(state, attacker, figure, slotIndex, data, target, false);
        });
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
        
        executeCommon(casterState, caster, figure, data, manaCost, finalTarget, false, isGolden);
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

    private static void executeCommon(IBattleState state, LivingEntity attacker, BattleFigure figure, int slotIndex, AbilityLoader.AbilityData data, LivingEntity target, boolean consumeMana) {
        java.util.ArrayList<String> tiers = ItemFigure.getAbilityTiers(figure.getOriginalStack());
        String tierLetter = (slotIndex < tiers.size()) ? tiers.get(slotIndex) : "a";
        int manaCost = TeenyBalance.getManaCost(slotIndex + 1, tierLetter);
        boolean isGolden = ItemFigure.isAbilityGolden(figure.getOriginalStack(), data.id);
        
        if (!bruhof.teenycraft.battle.trait.TraitRegistry.triggerExecutionHooks(state, attacker, figure, slotIndex, data, target, isGolden)) {
            return;
        }

        executeCommon(state, attacker, figure, data, manaCost, target, consumeMana, isGolden);
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

        executeCommon(state, attacker, figure, data, manaCost, target, false, isGolden);
    }

    private static void executeCommon(IBattleState attackerState, LivingEntity attacker, BattleFigure figure, AbilityLoader.AbilityData data, int manaCost, LivingEntity target, boolean consumeMana, boolean isGolden) {
        DamageResult result = DamagePipeline.calculateOutput(attackerState, figure, data, manaCost);
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
                                 damageSum += applyDamageToFigure(attackerState, attacker, target, targetState, alive.get(j), enemyHit, data, manaCost, isGolden, false);
                             }
                         }
                         return damageSum;
                     }).orElse(0);
                 } else {
                     totalDamageDealt += applyDamage(attackerState, attacker, target, singleHit, data, manaCost, isGolden);
                 }
            }

            if (totalDamageDealt > 0) {
                rollTofu(attackerState, attacker, figure, data, manaCost, target, isGolden);
                bruhof.teenycraft.battle.trait.TraitRegistry.triggerHitHooks(attackerState, attacker, figure, data, manaCost, target, totalDamageDealt, isGolden);
            }
        } else {
            if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§d[Effect] Casting Buffs..."));
            
            for (AbilityLoader.TraitData trait : data.traits) {
                EffectApplierRegistry.get(trait.id).apply(attackerState, attacker, figure, data, manaCost, trait.params, attacker);
            }
            
            for (AbilityLoader.EffectData effect : data.effectsOnSelf) {
                EffectApplierRegistry.get(effect.id).apply(attackerState, attacker, figure, data, manaCost, effect.params, attacker);
            }
            
            if (isGolden) {
                if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§6[Golden] Bonus Activated!"));
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

        if (consumeMana) {
            attackerState.consumeMana(manaCost);
        }
        
        if (attacker instanceof Player p) p.resetAttackStrengthTicker();
        if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§aUsed " + data.name + "!"));
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

    private static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    private static int applyDamage(IBattleState attackerState, LivingEntity attacker, LivingEntity target, DamageResult result, AbilityLoader.AbilityData data, int manaCost, boolean isGolden) {
        IBattleState victimState = target.getCapability(BattleStateProvider.BATTLE_STATE).orElse(null);
        
        if (victimState != null && victimState.isBattling()) {
            return applyDamageToFigure(attackerState, attacker, target, victimState, victimState.getActiveFigure(), result, data, manaCost, isGolden, false);
        }
        
        target.hurt(attacker.damageSources().mobAttack(attacker), (float) result.baseDamagePerHit);
        if (attacker instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§cDealt " + result.baseDamagePerHit + " dmg!"));
        return result.baseDamagePerHit;
    }

    private static int applyDamageToFigure(IBattleState attackerState, LivingEntity attacker, LivingEntity targetEntity, IBattleState victimState, BattleFigure victimFigure, DamageResult result, AbilityLoader.AbilityData data, int manaCost, boolean isGolden, boolean isReflected) {
        BattleFigure attackerFig = attackerState.getActiveFigure();
        if (victimFigure == null) return 0;
        
        DamagePipeline.MitigationResult mitigation = DamagePipeline.calculateMitigation(victimState, victimFigure, attackerState, attackerFig, result, data, isGolden);
        
        int instantDmg = mitigation.finalDamage;
        if (data != null) {
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
                        applyDamageToFigure(victimState, targetEntity, attacker, attackerState, attackerFig, reflectResult, null, 0, false, true);
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
                            applyDamageToFigure(victimState, targetEntity, attacker, attackerState, attackerFig, reflectResult, null, 0, false, true);
                            victimState.removeEffect("reflect");
                        }
                    }
                }
            }
        }

        announceDamage(attacker, targetEntity, victimFigure, mitigation, (data != null) ? data.name : "Attack");

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
