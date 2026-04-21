package bruhof.teenycraft.chip;

import bruhof.teenycraft.battle.AbilityExecutor;
import bruhof.teenycraft.battle.BattleFigure;
import bruhof.teenycraft.battle.damage.DamagePipeline;
import bruhof.teenycraft.capability.IBattleState;
import bruhof.teenycraft.item.custom.ItemFigure;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ChipExecutor {

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
        int baseHp = ItemFigure.getHealth(figureStack);
        int basePower = ItemFigure.getPower(figureStack);
        int baseDodge = ItemFigure.getDodge(figureStack);
        int baseLuck = ItemFigure.getLuck(figureStack);

        ItemStack chipStack = ItemFigure.getEquippedChip(figureStack);
        ChipSpec chip = ChipRegistry.get(chipStack);
        if (chip == null) {
            return new ResolvedBattleStats(baseHp, basePower, baseDodge, baseLuck);
        }

        int rank = ChipRegistry.getRank(chipStack);
        return new ResolvedBattleStats(
                applyStatModifiers(baseHp, chip, ChipStatType.MAX_HP, rank, 1),
                applyStatModifiers(basePower, chip, ChipStatType.POWER, rank, 0),
                applyStatModifiers(baseDodge, chip, ChipStatType.DODGE, rank, 0),
                applyStatModifiers(baseLuck, chip, ChipStatType.LUCK, rank, 0)
        );
    }

    public static boolean rollExtraInstantCast(BattleFigure figure, LivingEntity attacker) {
        ItemStack chipStack = figure.getEquippedChip();
        ChipSpec chip = ChipRegistry.get(chipStack);
        if (chip == null) {
            return false;
        }

        int rank = ChipRegistry.getRank(chipStack);
        float chance = chip.getExtraInstantCastChance(rank);
        if (chance <= 0) {
            return false;
        }

        if (attacker.getRandom().nextFloat() < chance) {
            if (attacker instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal("§d§lCHIP INSTANT CAST!"));
            }
            return true;
        }

        return false;
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

    private static List<ChipSpec.Action> getActions(ItemStack chipStack, ChipHookType hookType) {
        ChipSpec chip = ChipRegistry.get(chipStack);
        if (chip == null) {
            return List.of();
        }
        return switch (hookType) {
            case FIRST_APPEARANCE -> chip.getOnFirstAppearanceActions();
            case CRIT_HIT -> chip.getOnCritHitActions();
            case FAINT -> chip.getOnFaintActions();
            case KILL -> chip.getOnKillActions();
        };
    }

    private static void executeHookActions(IBattleState ownerState, LivingEntity ownerEntity, BattleFigure ownerFigure,
                                           IBattleState opponentState, LivingEntity opponentEntity, List<ChipSpec.Action> actions) {
        if (ownerState == null || ownerFigure == null || actions == null || actions.isEmpty()) {
            return;
        }

        ItemStack chipStack = ownerFigure.getEquippedChip();
        int rank = ChipRegistry.getRank(chipStack);

        for (ChipSpec.Action action : actions) {
            switch (action.getType()) {
                case APPLY_EFFECT_SELF -> ownerState.applyEffect(action.getEffectId(), action.getDuration(rank), action.getAmount(rank));
                case ADD_MANA_SELF -> ownerState.addMana(action.getAmount(rank));
                case ADD_BATTERY_SELF -> ownerState.addBatteryCharge(action.getAmount(rank));
                case HEAL_SELF_MAX_HP_PCT -> {
                    int healAmount = Math.max(1, Math.round(ownerFigure.getMaxHp() * action.getPercent(rank)));
                    ownerFigure.modifyHp(healAmount);
                }
                case DEAL_DAMAGE_OPPONENT -> dealFixedChipDamage(ownerState, ownerEntity, opponentState, opponentEntity,
                        action.getAmount(rank), action.isGroupDamage());
                case SUMMON_PET_SELF -> applyFixedPetSummon(ownerState, action.getDuration(rank), action.getAmount(rank));
            }
        }
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

    private static void applyFixedPetSummon(IBattleState targetState, int duration, int magnitude) {
        String slotToUse = "pet_slot_1";
        if (!targetState.hasEffect("pet_slot_1")) {
            slotToUse = "pet_slot_1";
        } else if (!targetState.hasEffect("pet_slot_2")) {
            slotToUse = "pet_slot_2";
        } else {
            var i1 = targetState.getEffectInstance("pet_slot_1");
            var i2 = targetState.getEffectInstance("pet_slot_2");
            if (i1 != null && i2 != null) {
                slotToUse = (i1.duration <= i2.duration) ? "pet_slot_1" : "pet_slot_2";
            }
        }

        targetState.applyEffect(slotToUse, duration, magnitude);
    }

    private static int applyStatModifiers(int baseValue, ChipSpec chip, ChipStatType statType, int rank, int minimumValue) {
        int flatDelta = 0;
        float basePctDelta = 0.0f;

        for (ChipSpec.StatModifier modifier : chip.getStatModifiers()) {
            if (modifier.getStatType() == statType) {
                flatDelta += modifier.getFlat(rank);
                basePctDelta += modifier.getBasePct(rank);
            }
        }

        int finalValue = baseValue + flatDelta + Math.round(baseValue * basePctDelta);
        return Math.max(minimumValue, finalValue);
    }

    private enum ChipHookType {
        FIRST_APPEARANCE,
        CRIT_HIT,
        FAINT,
        KILL
    }
}
