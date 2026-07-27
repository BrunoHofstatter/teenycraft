package bruhof.teenycraft.chip;

import bruhof.teenycraft.battle.AbilityExecutor;
import bruhof.teenycraft.battle.BattleFigure;
import bruhof.teenycraft.battle.damage.DamagePipeline;
import bruhof.teenycraft.capability.IBattleState;
import bruhof.teenycraft.item.custom.ItemFigure;
import bruhof.teenycraft.group.GroupComboStatBonus;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class ChipExecutor {
    private static final Map<BattleFigure, Integer> LAST_DAMAGED_REACTION =
            Collections.synchronizedMap(new WeakHashMap<>());

    public static class ResolvedBattleStats {
        public final int maxHp;
        public final int power;
        public final int dodge;
        public final int luck;

        public ResolvedBattleStats(int maxHp, int power, int dodge, int luck) {
            this.maxHp = maxHp;
            this.power = power;
            this.dodge = dodge;
            this.luck = luck;
        }
    }

    public static ResolvedBattleStats resolveBattleStats(ItemStack figureStack) {
        return resolveBattleStats(figureStack, GroupComboStatBonus.NONE);
    }

    public static ResolvedBattleStats resolveBattleStats(ItemStack figureStack, GroupComboStatBonus comboBonus) {
        int baseHp = ItemFigure.getHealth(figureStack);
        int basePower = ItemFigure.getPower(figureStack);
        int baseDodge = ItemFigure.getDodge(figureStack);
        int baseLuck = ItemFigure.getLuck(figureStack);

        ItemStack chipStack = ItemFigure.getEquippedChip(figureStack);
        ChipSpec chip = ChipRegistry.get(chipStack);
        if (chip == null) {
            return new ResolvedBattleStats(
                    Math.max(1, baseHp + comboBonus.health()),
                    Math.max(0, basePower + comboBonus.power()),
                    Math.max(0, baseDodge + comboBonus.dodge()),
                    Math.max(0, baseLuck + comboBonus.luck())
            );
        }

        int rank = ChipRegistry.getRank(chipStack);
        return new ResolvedBattleStats(
                applyStatModifiers(baseHp, comboBonus.health(), chip, ChipStatType.MAX_HP, rank, 1),
                applyStatModifiers(basePower, comboBonus.power(), chip, ChipStatType.POWER, rank, 0),
                applyStatModifiers(baseDodge, comboBonus.dodge(), chip, ChipStatType.DODGE, rank, 0),
                applyStatModifiers(baseLuck, comboBonus.luck(), chip, ChipStatType.LUCK, rank, 0)
        );
    }

    public static boolean rollExtraInstantCast(BattleFigure figure, LivingEntity attacker) {
        ChipSpec chip = getChip(figure);
        if (chip == null) {
            return false;
        }

        int rank = getRank(figure);
        float chance = chip.getExtraInstantCastChance(rank);
        if (chance <= 0) {
            return false;
        }

        if (attacker.getRandom().nextFloat() < chance) {
            if (attacker instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal("Â§dÂ§lCHIP INSTANT CAST!"));
            }
            return true;
        }

        return false;
    }

    public static float getChargeDelayMultiplier(BattleFigure figure) {
        ChipSpec chip = getChip(figure);
        if (chip == null) {
            return 1.0f;
        }

        float bonus = chip.getChargeSpeedBonus(getRank(figure));
        return bonus > 0.0f ? (1.0f / (1.0f + bonus)) : 1.0f;
    }

    public static float getProjectileDelayMultiplier(BattleFigure figure) {
        ChipSpec chip = getChip(figure);
        if (chip == null) {
            return 1.0f;
        }

        float bonus = chip.getProjectileSpeedBonus(getRank(figure));
        return bonus > 0.0f ? (1.0f / (1.0f + bonus)) : 1.0f;
    }

    public static float getPassiveBatteryChargeMultiplier(BattleFigure figure) {
        ChipSpec chip = getChip(figure);
        if (chip == null) {
            return 1.0f;
        }

        float bonus = chip.getPassiveBatteryChargeBonus(getRank(figure));
        return 1.0f + Math.max(0.0f, bonus);
    }

    public static float getTofuChanceMultiplier(BattleFigure figure) {
        ChipSpec chip = getChip(figure);
        if (chip == null) {
            return 1.0f;
        }
        return chip.getTofuChanceMultiplier(getRank(figure));
    }

    public static float getAbilityHealMultiplier(BattleFigure figure) {
        ChipSpec chip = getChip(figure);
        if (chip == null) {
            return 1.0f;
        }
        return chip.getAbilityHealMultiplier(getRank(figure));
    }

    public static int getActiveRegenInterval(BattleFigure figure) {
        ChipSpec chip = getChip(figure);
        if (chip == null) {
            return 0;
        }
        return chip.getActiveRegenInterval(getRank(figure));
    }

    public static int getActiveRegenHeal(BattleFigure figure) {
        ChipSpec chip = getChip(figure);
        if (chip == null) {
            return 0;
        }
        return chip.getActiveRegenHeal(getRank(figure));
    }

    public static float getDirectStunDurationMultiplier(BattleFigure figure) {
        ChipSpec chip = getChip(figure);
        if (chip == null) {
            return 1.0f;
        }
        return chip.getDirectStunDurationMultiplier(getRank(figure));
    }

    public static float getFinisherHpThreshold(BattleFigure figure) {
        ChipSpec chip = getChip(figure);
        if (chip == null) {
            return 0.0f;
        }
        return chip.getFinisherHpThreshold(getRank(figure));
    }

    public static float getFinisherDamageMultiplier(BattleFigure figure) {
        ChipSpec chip = getChip(figure);
        if (chip == null) {
            return 1.0f;
        }
        return chip.getFinisherDamageMultiplier(getRank(figure));
    }

    public static float getTeamMedicBenchHealPct(BattleFigure figure) {
        ChipSpec chip = getChip(figure);
        if (chip == null) {
            return 0.0f;
        }
        return chip.getTeamMedicBenchHealPct(getRank(figure));
    }

    public static int getSecondChanceSurviveHp(BattleFigure figure) {
        ChipSpec chip = getChip(figure);
        if (chip == null) {
            return 0;
        }
        return chip.getSecondChanceSurviveHp(getRank(figure));
    }

    public static int getSecondChanceDebuffDuration(BattleFigure figure) {
        ChipSpec chip = getChip(figure);
        if (chip == null) {
            return 0;
        }
        return chip.getSecondChanceDebuffDuration(getRank(figure));
    }

    public static int getSecondChanceSlowMagnitude(BattleFigure figure) {
        ChipSpec chip = getChip(figure);
        if (chip == null) {
            return 0;
        }
        return chip.getSecondChanceSlowMagnitude(getRank(figure));
    }

    public static boolean tryConsumeSecondChance(BattleFigure figure) {
        return figure != null
                && getSecondChanceSurviveHp(figure) > 0
                && figure.consumeSecondChance();
    }

    public static void applyOutgoingDamageModifiers(BattleFigure attackerFigure, BattleFigure victimFigure,
                                                    DamagePipeline.DamageResult result) {
        if (attackerFigure == null || victimFigure == null || result == null || result.baseDamagePerHit <= 0) {
            return;
        }

        float threshold = getFinisherHpThreshold(attackerFigure);
        if (threshold <= 0.0f || victimFigure.getMaxHp() <= 0) {
            return;
        }

        float victimHpPct = victimFigure.getCurrentHp() / (float) victimFigure.getMaxHp();
        if (victimHpPct <= threshold) {
            result.baseDamagePerHit = Math.max(1, Math.round(result.baseDamagePerHit * getFinisherDamageMultiplier(attackerFigure)));
        }
    }

    public static void onFirstAppearance(IBattleState ownerState, LivingEntity ownerEntity, BattleFigure ownerFigure,
                                         IBattleState opponentState, LivingEntity opponentEntity) {
        executeHookActions(ownerState, ownerEntity, ownerFigure, opponentState, opponentEntity,
                getActions(ownerFigure.getEquippedChip(), ChipHookType.FIRST_APPEARANCE));
    }

    public static void onCritHit(IBattleState ownerState, LivingEntity ownerEntity, BattleFigure ownerFigure,
                                 IBattleState opponentState, LivingEntity opponentEntity) {
        executeHookActions(ownerState, ownerEntity, ownerFigure, opponentState, opponentEntity,
                getActions(ownerFigure.getEquippedChip(), ChipHookType.CRIT_HIT));
    }

    public static void onDodge(IBattleState ownerState, LivingEntity ownerEntity, BattleFigure ownerFigure,
                               IBattleState opponentState, LivingEntity opponentEntity) {
        executeHookActions(ownerState, ownerEntity, ownerFigure, opponentState, opponentEntity,
                getActions(ownerFigure.getEquippedChip(), ChipHookType.DODGE));
    }

    public static void onFaint(IBattleState ownerState, LivingEntity ownerEntity, BattleFigure ownerFigure,
                               IBattleState opponentState, LivingEntity opponentEntity) {
        executeHookActions(ownerState, ownerEntity, ownerFigure, opponentState, opponentEntity,
                getActions(ownerFigure.getEquippedChip(), ChipHookType.FAINT));
    }

    public static void onKill(IBattleState ownerState, LivingEntity ownerEntity, BattleFigure ownerFigure,
                              IBattleState opponentState, LivingEntity opponentEntity) {
        executeHookActions(ownerState, ownerEntity, ownerFigure, opponentState, opponentEntity,
                getActions(ownerFigure.getEquippedChip(), ChipHookType.KILL));
    }

    public static void onDamaged(IBattleState ownerState, LivingEntity ownerEntity, BattleFigure ownerFigure,
                                 IBattleState opponentState, LivingEntity opponentEntity, int reactionId) {
        if (!shouldTriggerDamagedReaction(ownerFigure, reactionId)) {
            return;
        }

        executeHookActions(ownerState, ownerEntity, ownerFigure, opponentState, opponentEntity,
                getActions(ownerFigure.getEquippedChip(), ChipHookType.DAMAGED));
    }

    private static List<ChipSpec.Action> getActions(ItemStack chipStack, ChipHookType hookType) {
        ChipSpec chip = ChipRegistry.get(chipStack);
        if (chip == null) {
            return List.of();
        }
        return switch (hookType) {
            case FIRST_APPEARANCE -> chip.getOnFirstAppearanceActions();
            case CRIT_HIT -> chip.getOnCritHitActions();
            case DODGE -> chip.getOnDodgeActions();
            case FAINT -> chip.getOnFaintActions();
            case KILL -> chip.getOnKillActions();
            case DAMAGED -> chip.getOnDamagedActions();
        };
    }

    private static void executeHookActions(IBattleState ownerState, LivingEntity ownerEntity, BattleFigure ownerFigure,
                                           IBattleState opponentState, LivingEntity opponentEntity, List<ChipSpec.Action> actions) {
        if (ownerState == null || ownerFigure == null || actions == null || actions.isEmpty()) {
            return;
        }

        int rank = getRank(ownerFigure);
        RandomSource random = ownerEntity != null ? ownerEntity.getRandom() : RandomSource.create();
        java.util.UUID sourceEntityId = ownerEntity != null ? ownerEntity.getUUID() : null;
        BattleFigure opponentFigure = opponentState != null ? opponentState.getActiveFigure() : null;

        for (ChipSpec.Action action : actions) {
            switch (action.getType()) {
                case APPLY_EFFECT_SELF -> applyEffectSpec(ownerState, ownerFigure, action.getEffectSpec(), rank, random,
                        sourceEntityId, ownerFigure);
                case APPLY_EFFECT_OPPONENT -> applyEffectSpec(opponentState, opponentFigure, action.getEffectSpec(), rank, random,
                        sourceEntityId, ownerFigure);
                case APPLY_RANDOM_EFFECT_OPPONENT -> applyRandomOpponentEffect(opponentState, opponentFigure, action.getRandomEffectPool(),
                        rank, random, sourceEntityId, ownerFigure);
                case ADD_MANA_SELF -> ownerState.addMana(action.getAmount(rank));
                case ADD_BATTERY_SELF -> ownerState.addBatteryCharge(action.getAmount(rank));
                case HEAL_SELF_FLAT -> applySelfHeal(ownerState, ownerEntity, ownerFigure, Math.max(0, action.getAmount(rank)));
                case HEAL_SELF_MAX_HP_PCT -> {
                    int healAmount = Math.max(1, Math.round(ownerFigure.getMaxHp() * action.getPercent(rank)));
                    applySelfHeal(ownerState, ownerEntity, ownerFigure, healAmount);
                }
                case STEAL_MANA_OPPONENT -> stealOpponentMana(ownerState, opponentState, action.getAmount(rank));
                case DEAL_DAMAGE_OPPONENT -> dealFixedChipDamage(ownerState, ownerEntity, opponentState, opponentEntity,
                        action.getAmount(rank), action.isGroupDamage());
                case SUMMON_PET_SELF -> applyFixedPetSummon(ownerState, ownerFigure, action.getDuration(rank), action.getAmount(rank),
                        sourceEntityId);
            }
        }
    }

    private static void applyRandomOpponentEffect(IBattleState opponentState, BattleFigure opponentFigure,
                                                  List<ChipSpec.EffectSpec> effectPool, int rank, RandomSource random,
                                                  java.util.UUID sourceEntityId, BattleFigure sourceFigure) {
        if (opponentState == null || opponentFigure == null || effectPool == null || effectPool.isEmpty()) {
            return;
        }
        ChipSpec.EffectSpec chosen = effectPool.get(random.nextInt(effectPool.size()));
        applyEffectSpec(opponentState, opponentFigure, chosen, rank, random, sourceEntityId, sourceFigure);
    }

    private static void applyEffectSpec(IBattleState state, BattleFigure targetFigure, ChipSpec.EffectSpec effectSpec, int rank,
                                        RandomSource random, java.util.UUID sourceEntityId, BattleFigure sourceFigure) {
        if (state == null || targetFigure == null || effectSpec == null) {
            return;
        }

        String effectId = effectSpec.getEffectId();
        int duration = effectSpec.getDuration(rank);
        int amount = effectSpec.getAmount(rank);
        float power = effectSpec.getPower(rank);

        if ("waffle".equals(effectId)) {
            state.applyEffect(targetFigure, "waffle", duration, random.nextInt(3), 0, sourceEntityId, sourceFigure);
            return;
        }

        if (power != 0.0f) {
            state.applyEffect(targetFigure, effectId, duration, amount, power, sourceEntityId, sourceFigure);
        } else {
            state.applyEffect(targetFigure, effectId, duration, amount, 0, sourceEntityId, sourceFigure);
        }
    }

    private static void stealOpponentMana(IBattleState ownerState, IBattleState opponentState, int requestedAmount) {
        if (ownerState == null || opponentState == null || requestedAmount <= 0) {
            return;
        }

        int stolen = Math.min(requestedAmount, Math.max(0, (int) Math.floor(opponentState.getCurrentMana())));
        if (stolen <= 0) {
            return;
        }

        opponentState.consumeMana(stolen);
        ownerState.addMana(stolen);
    }

    private static void dealFixedChipDamage(IBattleState attackerState, LivingEntity attackerEntity, IBattleState victimState,
                                            LivingEntity victimEntity, int damage, boolean groupDamage) {
        if (attackerState == null || attackerEntity == null || victimState == null || victimEntity == null || damage <= 0) {
            return;
        }

        if (groupDamage) {
            List<BattleFigure> alive = new ArrayList<>();
            for (BattleFigure figure : victimState.getTeam()) {
                if (figure.getCurrentHp() > 0) {
                    alive.add(figure);
                }
            }
            if (alive.isEmpty()) {
                return;
            }

            int[] splits = bruhof.teenycraft.battle.damage.DistributionHelper.split(damage, alive.size());
            int reactionId = AbilityExecutor.nextAccessoryReactionId();
            for (int i = 0; i < alive.size(); i++) {
                DamagePipeline.DamageResult hit = new DamagePipeline.DamageResult(splits[i], 1, false, false);
                hit.isGroupDamage = true;
                AbilityExecutor.applyDamageToFigure(attackerState, attackerEntity, victimEntity, victimState, alive.get(i), hit,
                        null, 0, false, false, false, reactionId, true);
            }
        } else {
            BattleFigure activeVictim = victimState.getActiveFigure();
            if (activeVictim == null) {
                return;
            }
            DamagePipeline.DamageResult hit = new DamagePipeline.DamageResult(damage, 1, false, false);
            AbilityExecutor.applyDamageToFigure(attackerState, attackerEntity, victimEntity, victimState, activeVictim, hit,
                    null, 0, false, false, false, AbilityExecutor.nextAccessoryReactionId(), true);
        }
    }

    private static void applyFixedPetSummon(IBattleState targetState, BattleFigure targetFigure, int duration, int magnitude,
                                            java.util.UUID sourceEntityId) {
        if (targetState == null || targetFigure == null) {
            return;
        }

        String slotToUse = "pet_slot_1";
        if (!targetState.hasEffect(targetFigure, "pet_slot_1")) {
            slotToUse = "pet_slot_1";
        } else if (!targetState.hasEffect(targetFigure, "pet_slot_2")) {
            slotToUse = "pet_slot_2";
        } else {
            var i1 = targetState.getEffectInstance(targetFigure, "pet_slot_1");
            var i2 = targetState.getEffectInstance(targetFigure, "pet_slot_2");
            if (i1 != null && i2 != null) {
                slotToUse = (i1.duration <= i2.duration) ? "pet_slot_1" : "pet_slot_2";
            }
        }

        targetState.applyEffect(targetFigure, slotToUse, duration, magnitude, 0, sourceEntityId, targetFigure);
    }

    private static void applySelfHeal(IBattleState ownerState, LivingEntity ownerEntity, BattleFigure ownerFigure, int amount) {
        if (ownerState == null || ownerFigure == null || amount <= 0) {
            return;
        }
        ownerState.applyResolvedCombatFigureDelta(ownerFigure, amount, combatSource(ownerState, ownerEntity, ownerFigure));
    }

    private static int applyStatModifiers(int baseValue, int comboDelta, ChipSpec chip, ChipStatType statType, int rank, int minimumValue) {
        int flatDelta = 0;
        float basePctDelta = 0.0f;
        boolean hasExact = false;
        int exactValue = 0;

        for (ChipSpec.StatModifier modifier : chip.getStatModifiers()) {
            if (modifier.getStatType() == statType) {
                if (modifier.hasExactValue()) {
                    hasExact = true;
                    exactValue = modifier.getExactValue(rank);
                } else {
                    flatDelta += modifier.getFlat(rank);
                    basePctDelta += modifier.getBasePct(rank);
                }
            }
        }

        int finalValue = hasExact ? exactValue : baseValue + comboDelta + flatDelta + Math.round(baseValue * basePctDelta);
        return Math.max(minimumValue, finalValue);
    }

    private static ChipSpec getChip(BattleFigure figure) {
        if (figure == null) {
            return null;
        }
        return ChipRegistry.get(figure.getEquippedChip());
    }

    private static int getRank(BattleFigure figure) {
        return figure == null ? 0 : ChipRegistry.getRank(figure.getEquippedChip());
    }

    private static boolean shouldTriggerDamagedReaction(BattleFigure ownerFigure, int reactionId) {
        if (ownerFigure == null) {
            return false;
        }
        Integer lastReaction = LAST_DAMAGED_REACTION.get(ownerFigure);
        if (lastReaction != null && lastReaction == reactionId) {
            return false;
        }
        LAST_DAMAGED_REACTION.put(ownerFigure, reactionId);
        return true;
    }

    private static @org.jetbrains.annotations.Nullable IBattleState.CombatMutationSource combatSource(
            IBattleState state, LivingEntity entity, BattleFigure figure) {
        if (state == null || entity == null || figure == null) {
            return null;
        }
        return new IBattleState.CombatMutationSource(state, entity, figure);
    }

    private enum ChipHookType {
        FIRST_APPEARANCE,
        CRIT_HIT,
        DODGE,
        FAINT,
        KILL,
        DAMAGED
    }
}
