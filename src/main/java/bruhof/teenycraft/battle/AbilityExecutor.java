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
    public static void executeAttack(ServerPlayer player, BattleFigure figure, int slotIndex, Entity target) {
        if (!(target instanceof LivingEntity livingTarget)) return;
        
        AbilityLoader.AbilityData data = getAbilityData(figure, slotIndex);
        if (data == null || !"melee".equalsIgnoreCase(data.hitType)) return;

        // Get State
        player.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(state -> {
            // Cost check before executing
            java.util.ArrayList<String> tiers = ItemFigure.getAbilityTiers(figure.getOriginalStack());
            String tierLetter = (slotIndex < tiers.size()) ? tiers.get(slotIndex) : "a";
            int manaCost = TeenyBalance.getManaCost(slotIndex + 1, tierLetter);

            if (state.hasEffect("stun") || state.isCharging()) {
                player.sendSystemMessage(Component.literal("§cCannot attack right now!"));
                return;
            }
            
            if (state.hasEffect("waffle")) {
                int blockedSlot = state.getEffectMagnitude("waffle");
                if (blockedSlot == slotIndex) {
                     player.sendSystemMessage(Component.literal("§eSlot " + (slotIndex + 1) + " is Waffled!"));
                     return;
                }
            }

            if (state.getCurrentMana() < manaCost) { // Use State Mana
                player.sendSystemMessage(Component.literal("§cNot enough Mana! Need " + manaCost));
                return;
            }

            executeCommon(state, player, figure, slotIndex, data, livingTarget, true);
        });
    }

    /**
     * Triggered by Right-Click (Use Item). Only works for RANGED/SELF abilities.
     */
    public static void executeAction(ServerPlayer player, BattleFigure figure, int slotIndex) {
        AbilityLoader.AbilityData data = getAbilityData(figure, slotIndex);
        if (data == null) return;
        
        if ("melee".equalsIgnoreCase(data.hitType)) return;

        player.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(state -> {
            // Cost Check
            java.util.ArrayList<String> tiers = ItemFigure.getAbilityTiers(figure.getOriginalStack());
            String tierLetter = (slotIndex < tiers.size()) ? tiers.get(slotIndex) : "a";
            int manaCost = TeenyBalance.getManaCost(slotIndex + 1, tierLetter);

            if (state.hasEffect("stun") || state.isCharging()) {
                player.sendSystemMessage(Component.literal("§cCannot attack right now!"));
                return;
            }
            
            if (state.hasEffect("waffle")) {
                int blockedSlot = state.getEffectMagnitude("waffle");
                if (blockedSlot == slotIndex) {
                     player.sendSystemMessage(Component.literal("§eSlot " + (slotIndex + 1) + " is Waffled!"));
                     return;
                }
            }

            if (state.getCurrentMana() < manaCost) { // Use State Mana
                player.sendSystemMessage(Component.literal("§cNot enough Mana! Need " + manaCost));
                return;
            }

            LivingEntity target = null;
            boolean isRanged = "raycasting".equalsIgnoreCase(data.hitType) || "ranged".equalsIgnoreCase(data.hitType);

            if (isRanged) {
                 target = getConeTarget(player, data);
                 state.consumeMana(manaCost); // Use State Mana
                 
                 if (target == null) {
                     player.sendSystemMessage(Component.literal("§cNo target found! (-" + manaCost + " Mana)"));
                     return;
                 }
            } else {
                 target = player; 
                 state.consumeMana(manaCost);
            }

            executeCommon(state, player, figure, slotIndex, data, target, false);
        });
    }
    
    public static void executeDebugCast(IBattleState state, ServerPlayer player, BattleFigure figure, AbilityLoader.AbilityData data, int manaCost, boolean isGolden, boolean isSelf) {
        LivingEntity target;
        if (isSelf) {
            target = player;
        } else {
            // Find Opponent Entity
            java.util.UUID oppId = state.getOpponentEntityUUID();
            if (oppId != null && player.level() instanceof ServerLevel sl) {
                Entity e = sl.getEntity(oppId);
                if (e instanceof LivingEntity le) {
                    target = le;
                } else {
                    player.sendSystemMessage(Component.literal("§cOpponent entity not found!"));
                    return;
                }
            } else {
                player.sendSystemMessage(Component.literal("§cNo opponent registered!"));
                return;
            }
        }
        
        // Call executeCommon with manual data
        executeCommon(state, player, figure, data, manaCost, target, false, isGolden);
    }

    public static void executeTofu(ServerPlayer player, IBattleState state) {
        float virtualMana = state.getCurrentTofuMana();
        if (virtualMana <= 0) return;

        // Reset Tofu State first to prevent double-use
        state.spawnTofu(0);
        state.refreshPlayerInventory(player);

        String[] selfEffects = {"power_up", "heal", "bar_fill", "cleanse", "dance"};
        String[] oppEffects = {"freeze", "stun", "waffle"};
        
        boolean isSelf = Math.random() < (5.0 / 8.0); // Ratio of self to opp effects
        String effectId;
        
        if (isSelf) {
            effectId = selfEffects[(int) (Math.random() * selfEffects.length)];
        } else {
            effectId = oppEffects[(int) (Math.random() * oppEffects.length)];
        }

        // Determine Multiplier
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

        player.sendSystemMessage(Component.literal("§6§lTOFU USED! §fIt was: §e" + effectId.toUpperCase()));

        if (isSelf) {
            EffectApplierRegistry.get(effectId).apply(state, player, active, null, (int) virtualMana, java.util.Collections.singletonList(mult), player);
        } else {
            // Find Target
            java.util.UUID oppId = state.getOpponentEntityUUID();
            if (oppId != null && player.level() instanceof ServerLevel sl) {
                Entity e = sl.getEntity(oppId);
                if (e instanceof LivingEntity le) {
                    EffectApplierRegistry.get(effectId).apply(state, player, active, null, (int) virtualMana, java.util.Collections.singletonList(mult), le);
                }
            }
        }
    }

    // ==========================================
    // SHARED LOGIC
    // ==========================================

    private static void executeCommon(IBattleState state, ServerPlayer player, BattleFigure figure, int slotIndex, AbilityLoader.AbilityData data, LivingEntity target, boolean consumeMana) {
        // Calculate Standard Context
        java.util.ArrayList<String> tiers = ItemFigure.getAbilityTiers(figure.getOriginalStack());
        String tierLetter = (slotIndex < tiers.size()) ? tiers.get(slotIndex) : "a";
        int manaCost = TeenyBalance.getManaCost(slotIndex + 1, tierLetter);
        boolean isGolden = ItemFigure.isAbilityGolden(figure.getOriginalStack(), data.id);
        
        // Check for Charge Up trait
        AbilityLoader.TraitData chargeTrait = null;
        if (data.traits != null) {
            for (AbilityLoader.TraitData t : data.traits) {
                if ("charge_up".equals(t.id)) {
                    chargeTrait = t;
                    break;
                }
            }
        }

        if (chargeTrait != null) {
            float param = chargeTrait.params.isEmpty() ? 1.0f : chargeTrait.params.get(0);
            int ticks = (int) (TeenyBalance.BASE_CHARGE_DELAY * param);
            
            player.sendSystemMessage(Component.literal("§e§lCHARGING..."));
            state.consumeMana(manaCost);
            state.startCharge(ticks, data, slotIndex, isGolden, (target != null) ? target.getUUID() : null);
            return;
        }

        executeCommon(state, player, figure, data, manaCost, target, consumeMana, isGolden);
    }

    public static void finishCharge(IBattleState state, ServerPlayer player) {
        AbilityLoader.AbilityData data = state.getPendingAbility();
        int slot = state.getPendingSlot();
        boolean isGolden = state.isPendingGolden();
        UUID targetUUID = state.getPendingTargetUUID();
        
        state.cancelCharge(); // Clear before execution 

        BattleFigure figure = state.getActiveFigure();
        if (figure == null || data == null) return;

        // RETARGETING LOGIC
        LivingEntity target = null;
        if (TeenyBalance.CHARGE_LOCK_TARGET_ON_START && targetUUID != null) {
            if (player.level() instanceof ServerLevel sl) {
                Entity e = sl.getEntity(targetUUID);
                if (e instanceof LivingEntity le) target = le;
            }
        } else {
            boolean isRanged = "raycasting".equalsIgnoreCase(data.hitType) || "ranged".equalsIgnoreCase(data.hitType);
            if (isRanged) {
                target = getConeTarget(player, data);
            } else {
                target = player;
            }
        }

        if (target == null && !"none".equalsIgnoreCase(data.hitType)) {
            player.sendSystemMessage(Component.literal("§cCharge failed: Target lost!"));
            return;
        }

        java.util.ArrayList<String> tiers = ItemFigure.getAbilityTiers(figure.getOriginalStack());
        String tierLetter = (slot < tiers.size()) ? tiers.get(slot) : "a";
        int manaCost = TeenyBalance.getManaCost(slot + 1, tierLetter);

        executeCommon(state, player, figure, data, manaCost, target, false, isGolden);
    }

    private static void executeCommon(IBattleState state, ServerPlayer player, BattleFigure figure, AbilityLoader.AbilityData data, int manaCost, LivingEntity target, boolean consumeMana, boolean isGolden) {
        DamageResult result = DamagePipeline.calculateOutput(state, figure, data, manaCost);
        player.sendSystemMessage(Component.literal("§7[Debug] Raw Damage: " + result.baseDamagePerHit));

        // Spawn Particles (if defined)
        if (data.particleId != null && !data.particleId.isEmpty()) {
            ResourceLocation particleLoc = new ResourceLocation(data.particleId);
            if (ForgeRegistries.PARTICLE_TYPES.containsKey(particleLoc)) {
                ParticleType<?> particleType = ForgeRegistries.PARTICLE_TYPES.getValue(particleLoc);
                if (particleType instanceof ParticleOptions particleOptions) {
                    player.serverLevel().sendParticles(particleOptions, 
                        player.getX(), player.getEyeY(), player.getZ(), 
                        data.particleCount, 0.5, 0.5, 0.5, 0.1);
                }
            }
        }

        if (!"none".equalsIgnoreCase(data.hitType)) {
            state.triggerOnAttack(figure);
            
            int totalDamageDealt = 0;

            // Split TOTAL damage into hits (Multi-Hit logic)
            int[] hitSplits = bruhof.teenycraft.battle.damage.DistributionHelper.split(result.baseDamagePerHit, result.hitCount);
            
            for (int damagePart : hitSplits) {
                 DamageResult singleHit = new DamageResult(damagePart, 1, result.isGroupDamage, result.canCrit);
                 
                 if (result.isGroupDamage && target instanceof ServerPlayer targetP) {
                     // Group Damage Logic: Split this specific hit among enemies
                     totalDamageDealt += targetP.getCapability(BattleStateProvider.BATTLE_STATE).map(targetState -> {
                         List<BattleFigure> enemies = targetState.getTeam();
                         List<BattleFigure> alive = new java.util.ArrayList<>();
                         for (BattleFigure f : enemies) {
                             if (f.getCurrentHp() > 0) alive.add(f);
                         }
                         
                         int damageSum = 0;
                         if (!alive.isEmpty()) {
                             int[] groupSplits = bruhof.teenycraft.battle.damage.DistributionHelper.split(damagePart, alive.size());
                             for (int j = 0; j < alive.size(); j++) {
                                 DamageResult enemyHit = new DamageResult(groupSplits[j], 1, false, result.canCrit);
                                 damageSum += applyDamageToFigure(state, player, targetP, alive.get(j), enemyHit, data, manaCost, isGolden, true);
                             }
                         }
                         return damageSum;
                     }).orElse(0);
                 } else {
                     // Single Target Logic
                     totalDamageDealt += applyDamage(state, player, target, singleHit, data, manaCost, isGolden);
                 }
            }

            // Tofu Passive Roll (Once per Attack)
            if (totalDamageDealt > 0 && state.getCurrentTofuMana() == 0 && data.damageTier > 0) {
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
                                chanceMult *= Float.parseFloat(subParts[0]);
                                if (subParts.length > 1) powerMult *= Float.parseFloat(subParts[1]);
                            }
                        }
                    }
                }
                
                float chance = (manaCost * TeenyBalance.TOFU_CHANCE_HIT_PERMANA) * chanceMult;
                if (Math.random() * 100.0 < chance) {
                    state.spawnTofu(TeenyBalance.TOFU_BASE_MANA * powerMult);
                    player.sendSystemMessage(Component.literal("§6§lTOFU SPAWNED!"));
                    state.refreshPlayerInventory(player);
                }
            }
        } else {
            player.sendSystemMessage(Component.literal("§d[Effect] Casting Buffs..."));
            
            for (AbilityLoader.TraitData trait : data.traits) {
                EffectApplierRegistry.get(trait.id).apply(state, player, figure, data, manaCost, trait.params, player);
            }
            
            for (AbilityLoader.EffectData effect : data.effectsOnSelf) {
                EffectApplierRegistry.get(effect.id).apply(state, player, figure, data, manaCost, effect.params, player);
            }
            
            if (isGolden) {
                player.sendSystemMessage(Component.literal("§6[Golden] Bonus Activated!"));
                for (String bonus : data.goldenBonus) {
                    String[] parts = bonus.split(":");
                    if (parts.length >= 3 && "self".equalsIgnoreCase(parts[0])) {
                        java.util.ArrayList<Float> params = new java.util.ArrayList<>();
                        params.add(Float.parseFloat(parts[2]));
                        EffectApplierRegistry.get(parts[1]).apply(state, player, figure, data, manaCost, params, player);
                    }
                }
            }
        }

        if (consumeMana) {
            state.consumeMana(manaCost);
        }
        
        player.resetAttackStrengthTicker();
        player.sendSystemMessage(Component.literal("§aUsed " + data.name + "!"));
    }

    private static AbilityLoader.AbilityData getAbilityData(BattleFigure figure, int slotIndex) {
        ItemStack stack = figure.getOriginalStack();
        java.util.ArrayList<String> order = ItemFigure.getAbilityOrder(stack);
        if (slotIndex >= order.size()) return null;
        return AbilityLoader.getAbility(order.get(slotIndex));
    }

    private static LivingEntity getConeTarget(ServerPlayer player, AbilityLoader.AbilityData data) {
        double maxRange = TeenyBalance.getRangeValue(data.rangeTier);
        double halfAngleRad = Math.toRadians(TeenyBalance.RANGED_CONE_ANGLE / 2.0);
        double startHalfWidth = TeenyBalance.RANGED_START_WIDTH / 2.0;
        
        Vec3 rayOrigin = player.getEyePosition();
        Vec3 rayDir = player.getLookAngle().normalize();
        
        AABB searchBox = player.getBoundingBox().inflate(maxRange);
        List<Entity> entities = player.level().getEntities(player, searchBox, 
            e -> e instanceof LivingEntity && !e.isSpectator() && e != player
        );
        
        LivingEntity closest = null;
        double closestDistSq = Double.MAX_VALUE;

        for (Entity e : entities) {
            AABB targetBox = e.getBoundingBox();
            
            // 1. Find the distance along the ray to the center of the entity's box
            Vec3 boxCenter = targetBox.getCenter();
            Vec3 toCenter = boxCenter.subtract(rayOrigin);
            double distAlongRay = toCenter.dot(rayDir);
            
            // Range check
            if (distAlongRay < 0 || distAlongRay > maxRange) continue;
            
            // 2. Find the closest point on the AABB to the Look Ray
            // This ensures we check the WHOLE hitbox, not just one point
            Vec3 closestPointOnBox = getClosestPointOnAABB(targetBox, rayOrigin, rayDir);
            
            // 3. Calculate distance from the Look Ray to this closest point
            Vec3 toClosest = closestPointOnBox.subtract(rayOrigin);
            double dAlong = toClosest.dot(rayDir);
            Vec3 projection = rayOrigin.add(rayDir.scale(dAlong));
            double distToRaySq = closestPointOnBox.distanceToSqr(projection);
            
            // 4. Calculate the "Target Radius" of our Trapezoid at this distance
            double allowedRadius = startHalfWidth + (distAlongRay * Math.tan(halfAngleRad));
            
            // 5. Check if the closest point is within the allowed radius
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

    /**
     * Helper: Finds the point on an AABB that is closest to a Ray.
     */
    private static Vec3 getClosestPointOnAABB(AABB box, Vec3 origin, Vec3 dir) {
        double x = clamp(origin.x, box.minX, box.maxX);
        double y = clamp(origin.y, box.minY, box.maxY);
        double z = clamp(origin.z, box.minZ, box.maxZ);
        return new Vec3(x, y, z);
    }

    public static void announceDamage(ServerPlayer attacker, Entity targetEntity, BattleFigure victim, DamagePipeline.MitigationResult mit, String source) {
        if (attacker == null || victim == null) return;

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
        attacker.sendSystemMessage(msg);
        
        // Also notify the target player if it's a player
        if (targetEntity instanceof ServerPlayer victimP && victimP != attacker) {
            victimP.sendSystemMessage(msg);
        }
    }

    private static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    private static int applyDamage(IBattleState state, ServerPlayer attacker, LivingEntity target, DamageResult result, AbilityLoader.AbilityData data, int manaCost, boolean isGolden) {
        if (state.isBattling()) {
            BattleFigure victimFigure = null;
            boolean isOpponent = false;

            if (target.getUUID().equals(state.getOpponentEntityUUID())) {
                victimFigure = state.getActiveOpponent();
                isOpponent = true;
            } else if (target.getUUID().equals(attacker.getUUID())) {
                victimFigure = state.getActiveFigure();
            }

            if (victimFigure != null) {
                return applyDamageToFigure(state, attacker, target, victimFigure, result, data, manaCost, isGolden, isOpponent);
            }
        }
        // Fallback for non-battle entities
        target.hurt(attacker.damageSources().playerAttack(attacker), (float) result.baseDamagePerHit);
        attacker.sendSystemMessage(Component.literal("§cDealt " + result.baseDamagePerHit + " dmg!"));
        return result.baseDamagePerHit;
    }

    private static int applyDamageToFigure(IBattleState state, ServerPlayer attacker, LivingEntity targetEntity, BattleFigure victimFigure, DamageResult result, AbilityLoader.AbilityData data, int manaCost, boolean isGolden, boolean isOpponent) {
        // Mitigation Step
        BattleFigure attackerFig = state.getActiveFigure();
        DamagePipeline.MitigationResult mitigation = DamagePipeline.calculateMitigation(state, victimFigure, attackerFig, result, data, isGolden);
        
        // Pure Poison Check: If ability has 'poison' effect, skip instant hit damage
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
                 attacker.sendSystemMessage(Component.literal("§b§lSHIELD PIERCED!"));
                 if (targetEntity instanceof ServerPlayer tp) tp.sendSystemMessage(Component.literal("§c§lSHIELD PIERCED!"));
             } else {
                 attacker.sendSystemMessage(Component.literal("§bBLOCKED!"));
             }
        }
        
        // 1. Apply Final Damage (If any)
        if (instantDmg > 0) {
            victimFigure.modifyHp(-instantDmg);
            
            // Sync Visual Health ONLY for non-players (Dummies)
            if (!(targetEntity instanceof Player)) {
                targetEntity.setHealth(victimFigure.getCurrentHp());
            }
        }

        // 2. Announce Damage (Even if 0)
        String sourceName = (data != null) ? data.name : "Attack";
        announceDamage(attacker, targetEntity, victimFigure, mitigation, sourceName);

        if (instantDmg > 0 && victimFigure.getCurrentHp() <= 0) {
            if (isOpponent) {
                if (victimFigure == state.getActiveOpponent()) {
                    targetEntity.kill(); 
                    state.triggerVictory(attacker);
                } else {
                    attacker.sendSystemMessage(Component.literal("§c" + victimFigure.getNickname() + " Fainted!"));
                }
            } else {
                attacker.sendSystemMessage(Component.literal("§c§lYou Fainted!"));
            }
        }
        
        // 2. Apply Effects (If not dodged)
        if (!mitigation.isDodged) {
            if (data != null) {
                BattleFigure attackingFigure = state.getActiveFigure();
                
                // Standard Effects
                for (AbilityLoader.EffectData effect : data.effectsOnOpponent) {
                    EffectApplierRegistry.get(effect.id).apply(state, attacker, attackingFigure, data, manaCost, effect.params, targetEntity);
                }
                
                // Golden Bonus Effects
                if (isGolden) {
                    for (String bonus : data.goldenBonus) {
                        String[] parts = bonus.split(":");
                        if (parts.length >= 3 && "opponent".equalsIgnoreCase(parts[0])) {
                            String effectId = parts[1];
                            String paramStr = parts[2];
                            java.util.List<Float> params = new java.util.ArrayList<>();
                            
                            for (String p : paramStr.split(",")) {
                                try {
                                    params.add(Float.parseFloat(p));
                                } catch (NumberFormatException e) {
                                    params.add(1.0f);
                                }
                            }
                            
                            EffectApplierRegistry.get(effectId).apply(state, attacker, attackingFigure, data, manaCost, params, targetEntity);
                        }
                    }
                }
            }

            // VISUAL FEEDBACK (Only if target is the active entity AND hit was NOT dodged AND damage dealt)
            if (targetEntity != null && victimFigure == state.getActiveOpponent() && instantDmg > 0) {
                 targetEntity.hurt(attacker.damageSources().playerAttack(attacker), 0.01f);
                 if (targetEntity instanceof Player) {
                    targetEntity.setHealth(targetEntity.getMaxHealth()); 
                 }
            }
        }

        return instantDmg;
    }
}
