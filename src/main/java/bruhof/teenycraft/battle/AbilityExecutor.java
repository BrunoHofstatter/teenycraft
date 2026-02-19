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
import java.util.ArrayList;
import java.util.Collections;

import bruhof.teenycraft.battle.damage.DamagePipeline;
import bruhof.teenycraft.battle.damage.DamagePipeline.DamageResult;
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

            if (state.hasEffect("stun")) {
                player.sendSystemMessage(Component.literal("§cYou are Stunned!"));
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

            if (state.hasEffect("stun")) {
                player.sendSystemMessage(Component.literal("§cYou are Stunned!"));
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
            applyBuffEffect(state, player, active, null, (int) virtualMana, effectId, java.util.Collections.singletonList(mult));
        } else {
            // Find Target
            java.util.UUID oppId = state.getOpponentEntityUUID();
            if (oppId != null && player.level() instanceof ServerLevel sl) {
                Entity e = sl.getEntity(oppId);
                if (e instanceof LivingEntity le) {
                    applyEffectToTarget(player, le, active, effectId, java.util.Collections.singletonList(mult), (int) virtualMana);
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
        
        executeCommon(state, player, figure, data, manaCost, target, consumeMana, isGolden);
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
            
            for (int i = 0; i < result.hitCount; i++) {
                 if (result.isGroupDamage && target instanceof ServerPlayer targetP) {
                     // Group Damage Logic (PvP)
                     targetP.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(targetState -> {
                         List<BattleFigure> enemies = targetState.getTeam();
                         List<BattleFigure> alive = new java.util.ArrayList<>();
                         for (BattleFigure f : enemies) {
                             if (f.getCurrentHp() > 0) alive.add(f);
                         }
                         
                         if (!alive.isEmpty()) {
                             int[] splits = bruhof.teenycraft.battle.damage.DistributionHelper.split(result.baseDamagePerHit, alive.size());
                             DamageResult groupResult = new DamageResult(0); // Clone basic props if needed
                             groupResult.isCritical = result.isCritical;
                             
                             for (int j = 0; j < alive.size(); j++) {
                                 groupResult.baseDamagePerHit = splits[j];
                                 applyDamageToFigure(state, player, targetP, alive.get(j), groupResult, data, manaCost, isGolden, true);
                             }
                         }
                     });
                 } else {
                     // Single Target Logic
                     applyDamage(state, player, target, result, data, manaCost, isGolden);
                 }
            }
        } else {
            player.sendSystemMessage(Component.literal("§d[Effect] Casting Buffs..."));
            
            for (AbilityLoader.TraitData trait : data.traits) {
                applyBuffEffect(state, player, figure, data, manaCost, trait.id, trait.params);
            }
            
            for (AbilityLoader.EffectData effect : data.effectsOnSelf) {
                applyBuffEffect(state, player, figure, data, manaCost, effect.id, effect.params);
            }
            
            if (isGolden) {
                player.sendSystemMessage(Component.literal("§6[Golden] Bonus Activated!"));
                for (String bonus : data.goldenBonus) {
                    String[] parts = bonus.split(":");
                    if (parts.length >= 3 && "self".equalsIgnoreCase(parts[0])) {
                        java.util.ArrayList<Float> params = new java.util.ArrayList<>();
                        params.add(Float.parseFloat(parts[2]));
                        applyBuffEffect(state, player, figure, data, manaCost, parts[1], params);
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

    private static void applyBuffEffect(IBattleState state, ServerPlayer player, BattleFigure figure, AbilityLoader.AbilityData data, int manaCost, String id, List<Float> params) {
        int magnitude = 0;
        int duration = -1;
        float paramMult = (!params.isEmpty()) ? params.get(0) : 1.0f;

        if ("bar_fill".equals(id)) {
             magnitude = bruhof.teenycraft.battle.effect.EffectCalculator.calculateManaMagnitude(figure, data, manaCost, paramMult);
        } else if ("self_shock".equals(id)) {
             magnitude = bruhof.teenycraft.battle.effect.EffectCalculator.calculateSelfShockDamage(figure, manaCost, paramMult);
        } else if ("dance".equals(id)) {
             duration = bruhof.teenycraft.battle.effect.EffectCalculator.calculateDanceDuration(figure, manaCost, paramMult);
             magnitude = 1; // Flag magnitude
        } else if ("curse".equals(id)) {
             duration = bruhof.teenycraft.battle.effect.EffectCalculator.calculateCurseDuration(figure, manaCost, paramMult);
             magnitude = 1;
        } else if ("power_up".equals(id) || "power_down".equals(id)) {
             magnitude = bruhof.teenycraft.battle.effect.EffectCalculator.calculatePowerUpMagnitude(figure, manaCost, paramMult);
        } else if ("self:heal".equals(id) || "heal".equals(id)) {
             magnitude = bruhof.teenycraft.battle.effect.EffectCalculator.calculateHealMagnitude(figure, manaCost, paramMult);
        } else if ("group_heal".equals(id)) {
             magnitude = bruhof.teenycraft.battle.effect.EffectCalculator.calculateHealMagnitude(figure, manaCost, paramMult);
        } else if ("cleanse".equals(id)) {
             duration = bruhof.teenycraft.battle.effect.EffectCalculator.calculateCleanseDuration(figure, manaCost, paramMult);
             magnitude = 1;
        } else if ("tofu_spawn".equals(id)) {
             magnitude = (int) (TeenyBalance.TOFU_BASE_MANA * (1.0f + (manaCost * TeenyBalance.TOFU_ABILITY_PERMANA)) * paramMult);
        } else {
             magnitude = (int) paramMult;
             if (params.size() > 1) duration = params.get(1).intValue();
        }
        
        // MESSAGING & APPLICATION
        if (id.contains("heal") && !"group_heal".equals(id)) {
             player.sendSystemMessage(Component.literal("§aHealed " + magnitude + " HP!"));
             state.applyEffect("heal", 0, magnitude);
        } else if ("group_heal".equals(id)) {
             player.sendSystemMessage(Component.literal("§aGroup Heal: " + magnitude + " (Split among team)"));
             state.applyEffect("group_heal", 0, magnitude);
        } else if ("bar_fill".equals(id)) {
             player.sendSystemMessage(Component.literal("§bRestored " + magnitude + " Mana!"));
             state.applyEffect("bar_fill", 0, magnitude);
        } else if ("self_shock".equals(id)) {
             // Simulate attack for visual/audio feedback (Red flash + Knockback)
             player.hurt(player.damageSources().magic(), 0.01f);
             player.setHealth(player.getMaxHealth()); // Ensure player visual HP stays full
             player.sendSystemMessage(Component.literal("§cSelf Shock! Took " + magnitude + " damage!"));
             state.applyEffect("self_shock", 0, magnitude);
        } else if ("dance".equals(id)) {
             player.sendSystemMessage(Component.literal("§dDance! Mana Regen x2 for " + (duration/20.0) + "s"));
             state.applyEffect(id, duration, magnitude);
        } else if ("curse".equals(id)) {
             player.sendSystemMessage(Component.literal("§5You are CURSED! Mana Regen reduced. (" + (duration/20.0) + "s)"));
             state.applyEffect(id, duration, magnitude);
        } else if ("tofu_spawn".equals(id)) {
             player.sendSystemMessage(Component.literal("§6§lTOFU SPAWNED! (Power: " + magnitude + ")"));
             state.spawnTofu(magnitude);
             state.refreshPlayerInventory(player);
        } else {
             player.sendSystemMessage(Component.literal("§dApplied " + id + " (Mag: " + magnitude + ")"));
             state.applyEffect(id, duration, magnitude);
        }
    }

    private static AbilityLoader.AbilityData getAbilityData(BattleFigure figure, int slotIndex) {
        ItemStack stack = figure.getOriginalStack();
        java.util.ArrayList<String> order = ItemFigure.getAbilityOrder(stack);
        if (slotIndex >= order.size()) return null;
        return AbilityLoader.getAbility(order.get(slotIndex));
    }

    private static LivingEntity getConeTarget(ServerPlayer player, AbilityLoader.AbilityData data) {
        double range = TeenyBalance.getRangeValue(data.rangeTier);
        double coneAngle = TeenyBalance.RANGED_CONE_ANGLE;
        
        Vec3 eyePos = player.getEyePosition();
        AABB searchBox = player.getBoundingBox().inflate(range);
        
        List<Entity> entities = player.level().getEntities(player, searchBox, 
            e -> e instanceof LivingEntity && !e.isSpectator() && e != player && e.distanceToSqr(player) <= range * range
        );
        
        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;
        Vec3 lookVec = player.getLookAngle();

        for (Entity e : entities) {
            Vec3 toEntity = e.position().subtract(player.position()).normalize();
            double dot = lookVec.dot(toEntity);
            double threshold = Math.cos(Math.toRadians(coneAngle / 2.0));
            
            if (dot >= threshold) {
                double dist = player.distanceToSqr(e);
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = (LivingEntity) e;
                }
            }
        }
        return closest;
    }

    private static void applyDamage(IBattleState state, ServerPlayer attacker, LivingEntity target, DamageResult result, AbilityLoader.AbilityData data, int manaCost, boolean isGolden) {
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
                applyDamageToFigure(state, attacker, target, victimFigure, result, data, manaCost, isGolden, isOpponent);
                return;
            }
        }
        // Fallback for non-battle entities (Vanilla Mobs/Dummies without BattleState)
        target.hurt(attacker.damageSources().playerAttack(attacker), (float) result.baseDamagePerHit);
        attacker.sendSystemMessage(Component.literal("§cDealt " + result.baseDamagePerHit + " dmg!"));
    }

    private static void applyDamageToFigure(IBattleState state, ServerPlayer attacker, LivingEntity targetEntity, BattleFigure victimFigure, DamageResult result, AbilityLoader.AbilityData data, int manaCost, boolean isGolden, boolean isOpponent) {
        // Mitigation Step
        DamagePipeline.MitigationResult mitigation = DamagePipeline.calculateMitigation(state, victimFigure, result, data, isGolden);
        
        if (mitigation.isDodged) {
            int reduced = result.baseDamagePerHit - mitigation.finalDamage;
            attacker.sendSystemMessage(Component.literal("§e" + victimFigure.getNickname() + " DODGED! (Reduced by " + reduced + ")"));
            if (targetEntity instanceof ServerPlayer targetPlayer && targetPlayer != attacker) {
                targetPlayer.sendSystemMessage(Component.literal("§a" + victimFigure.getNickname() + " DODGED! (Reduced by " + reduced + ")"));
            }
        }
        
        if (mitigation.isBlocked) {
             attacker.sendSystemMessage(Component.literal("§bBLOCKED!"));
        }
        
        // 1. Apply Final Damage (If any)
        if (mitigation.finalDamage > 0) {
            victimFigure.modifyHp(-mitigation.finalDamage);
            
            // Sync Visual Health ONLY for non-players (Dummies)
            if (!(targetEntity instanceof Player)) {
                targetEntity.setHealth(victimFigure.getCurrentHp());
            }
            
            String critText = result.isCritical ? " §c§lCRITICAL!" : "";
            attacker.sendSystemMessage(Component.literal("§cHit " + victimFigure.getNickname() + ": " + victimFigure.getCurrentHp() + " HP left" + critText));
            
            if (victimFigure.getCurrentHp() <= 0) {
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
        }
        
        // 2. Apply Effects (If not dodged)
        if (!mitigation.isDodged) {
            // Tofu Passive Roll
            if (state.getCurrentTofuMana() == 0) {
                float chanceMult = 1.0f;
                float powerMult = 1.0f;
                
                // Check for tofu_chance trait
                if (data != null) {
                    for (AbilityLoader.TraitData t : data.traits) {
                        if ("tofu_chance".equals(t.id)) {
                            chanceMult *= t.params.isEmpty() ? 1.0f : t.params.get(0);
                            if (t.params.size() > 1) powerMult *= t.params.get(1);
                        }
                    }
                    if (isGolden) {
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
                }
                
                float chance = (manaCost * TeenyBalance.TOFU_CHANCE_HIT_PERMANA) * chanceMult;
                if (Math.random() * 100.0 < chance) {
                    state.spawnTofu(TeenyBalance.TOFU_BASE_MANA * powerMult);
                    attacker.sendSystemMessage(Component.literal("§6§lTOFU SPAWNED!"));
                    state.refreshPlayerInventory(attacker);
                }
            }

            if (data != null) {
                BattleFigure attackingFigure = state.getActiveFigure();
                
                // Standard Effects
                for (AbilityLoader.EffectData effect : data.effectsOnOpponent) {
                    applyEffectToTarget(attacker, targetEntity, attackingFigure, effect.id, effect.params, manaCost);
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
                            
                            applyEffectToTarget(attacker, targetEntity, attackingFigure, effectId, params, manaCost);
                        }
                    }
                }
            }
        }
        
        // VISUAL FEEDBACK (Only if target is the active entity)
        // If hitting bench figures, we don't flash the player red repeatedly.
        if (targetEntity != null && victimFigure == state.getActiveOpponent()) {
             targetEntity.hurt(attacker.damageSources().playerAttack(attacker), 0.01f);
             if (targetEntity instanceof Player) {
                targetEntity.setHealth(targetEntity.getMaxHealth()); 
             }
        }
    }

    private static void applyEffectToTarget(ServerPlayer attacker, LivingEntity targetEntity, BattleFigure attackingFigure, String effectId, List<Float> params, int manaCost) {
        float param = params.isEmpty() ? 1.0f : params.get(0);
        
        if ("stun".equals(effectId)) {
            int duration = bruhof.teenycraft.battle.effect.EffectCalculator.calculateStunDuration(attackingFigure, manaCost, param);
            if (targetEntity instanceof ServerPlayer targetP) {
                targetP.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(targetState -> {
                    targetState.applyEffect("stun", duration, 1);
                    targetP.sendSystemMessage(Component.literal("§eYou are STUNNED for " + (duration/20.0) + "s!"));
                });
                attacker.sendSystemMessage(Component.literal("§eStunned target for " + (duration/20.0) + "s!"));
            }
        } else if ("curse".equals(effectId)) {
            int duration = bruhof.teenycraft.battle.effect.EffectCalculator.calculateCurseDuration(attackingFigure, manaCost, param);
            if (targetEntity instanceof ServerPlayer targetP) {
                targetP.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(targetState -> {
                    targetState.applyEffect("curse", duration, 1);
                    targetP.sendSystemMessage(Component.literal("§5You are CURSED! Mana Regen reduced. (" + (duration/20.0) + "s)"));
                });
                attacker.sendSystemMessage(Component.literal("§5Cursed target for " + (duration/20.0) + "s!"));
            }
        } else if ("waffle".equals(effectId) || "waffle_chance".equals(effectId)) {
            // Param 0: Chance (0.0 - 1.0)
            // Param 1: Duration Multiplier (Optional, default 1.0)
            float chance = param;
            if (Math.random() < chance) {
                float durMult = (params.size() > 1) ? params.get(1) : 1.0f;
                int duration = bruhof.teenycraft.battle.effect.EffectCalculator.calculateWaffleDuration(attackingFigure, manaCost, durMult);
                int blockedSlot = (int) (Math.random() * 3); // 0, 1, or 2
                
                if (targetEntity instanceof ServerPlayer targetP) {
                    targetP.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(targetState -> {
                        targetState.removeEffect("waffle"); // Remove existing to refresh/reroll
                        targetState.applyEffect("waffle", duration, blockedSlot);
                        targetP.sendSystemMessage(Component.literal("§6§lWAFFLED! §eSlot " + (blockedSlot+1) + " Blocked (" + (duration/20.0) + "s)"));
                    });
                    attacker.sendSystemMessage(Component.literal("§6Waffled target's Slot " + (blockedSlot+1) + "!"));
                }
            }
        } else if ("freeze".equals(effectId)) {
            // Param 0: Magnitude (% of mana)
            // Param 1: Duration Multiplier
            int magnitude = bruhof.teenycraft.battle.effect.EffectCalculator.calculateFreezeMagnitude(attackingFigure, manaCost, param);
            float durMult = (params.size() > 1) ? params.get(1) : 1.0f;
            int duration = bruhof.teenycraft.battle.effect.EffectCalculator.calculateFreezeDuration(attackingFigure, manaCost, durMult);
            
            if (targetEntity instanceof ServerPlayer targetP) {
                targetP.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(targetState -> {
                    targetState.applyEffect("freeze", duration, magnitude);
                    targetP.sendSystemMessage(Component.literal("§b§lFROZEN! §3Mana Burn: " + magnitude + "% (" + (duration/20.0) + "s)"));
                });
                attacker.sendSystemMessage(Component.literal("§bFrozen target! Burned " + magnitude + "% Mana."));
            }
        } else if ("kiss".equals(effectId)) {
            int duration = bruhof.teenycraft.battle.effect.EffectCalculator.calculateKissDuration(attackingFigure, manaCost, param);
            if (targetEntity instanceof ServerPlayer targetP) {
                targetP.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(targetState -> {
                    targetState.applyEffect("kiss", duration, 1);
                    targetP.sendSystemMessage(Component.literal("§d§lKISS! §cPositive effects blocked (" + (duration/20.0) + "s)"));
                });
                attacker.sendSystemMessage(Component.literal("§dKissed target for " + (duration/20.0) + "s!"));
            }
        } else if ("cleanse".equals(effectId)) {
            int duration = bruhof.teenycraft.battle.effect.EffectCalculator.calculateCleanseDuration(attackingFigure, manaCost, param);
            if (targetEntity instanceof ServerPlayer targetP) {
                targetP.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(targetState -> {
                    targetState.applyEffect("cleanse", duration, 1);
                });
            }
        }
    }
}
