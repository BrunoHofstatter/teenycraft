package bruhof.teenycraft.battle.executor;

import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.battle.BattleFigure;
import bruhof.teenycraft.capability.IBattleState;
import bruhof.teenycraft.item.custom.ItemFigure;
import bruhof.teenycraft.util.AbilityLoader;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public record BattleAbilityContext(IBattleState state, LivingEntity attacker, BattleFigure figure, int slotIndex,
                                   AbilityLoader.AbilityData data, int actualManaCost, int effectiveManaCost,
                                   boolean isGolden) {
    @Nullable
    public static BattleAbilityContext create(IBattleState state, LivingEntity attacker, BattleFigure figure, int slotIndex) {
        AbilityLoader.AbilityData data = getAbilityData(figure, slotIndex);
        if (data == null) {
            return null;
        }
        return create(state, attacker, figure, slotIndex, data);
    }

    public static BattleAbilityContext create(IBattleState state, LivingEntity attacker, BattleFigure figure, int slotIndex,
                                              AbilityLoader.AbilityData data) {
        return createResolved(
                state,
                attacker,
                figure,
                slotIndex,
                data,
                resolveActualManaCost(figure, slotIndex),
                resolveEffectiveManaCost(figure, slotIndex),
                ItemFigure.isAbilityGolden(figure.getOriginalStack(), data.id)
        );
    }

    public static BattleAbilityContext createResolved(IBattleState state, LivingEntity attacker, BattleFigure figure, int slotIndex,
                                                      AbilityLoader.AbilityData data, int manaCost, boolean isGolden) {
        return createResolved(state, attacker, figure, slotIndex, data, manaCost, manaCost, isGolden);
    }

    public static BattleAbilityContext createResolved(IBattleState state, LivingEntity attacker, BattleFigure figure, int slotIndex,
                                                      AbilityLoader.AbilityData data, int actualManaCost, int effectiveManaCost,
                                                      boolean isGolden) {
        return new BattleAbilityContext(state, attacker, figure, slotIndex, data, actualManaCost, effectiveManaCost, isGolden);
    }

    public boolean isMelee() {
        return "melee".equalsIgnoreCase(data.hitType);
    }

    public boolean isRanged() {
        return "raycasting".equalsIgnoreCase(data.hitType) || "ranged".equalsIgnoreCase(data.hitType);
    }

    public boolean isSelfTargeted() {
        return "none".equalsIgnoreCase(data.hitType);
    }

    public boolean hasTrait(String traitId) {
        return data.traits != null && data.traits.stream().anyMatch(trait -> traitId.equals(trait.id));
    }

    public boolean hasOpponentEffect(String effectId) {
        return data.effectsOnOpponent != null && data.effectsOnOpponent.stream().anyMatch(effect -> effectId.equals(effect.id));
    }

    @Nullable
    public static AbilityLoader.AbilityData getAbilityData(BattleFigure figure, int slotIndex) {
        ItemStack stack = figure.getOriginalStack();
        ArrayList<String> order = ItemFigure.getAbilityOrder(stack);
        if (slotIndex >= order.size()) {
            return null;
        }
        return AbilityLoader.getAbility(order.get(slotIndex));
    }

    public static int resolveActualManaCost(BattleFigure figure, int slotIndex) {
        ArrayList<String> tiers = ItemFigure.getAbilityTiers(figure.getOriginalStack());
        String tierLetter = (slotIndex < tiers.size()) ? tiers.get(slotIndex) : "a";
        return TeenyBalance.getManaCost(slotIndex + 1, tierLetter);
    }

    public static int resolveEffectiveManaCost(BattleFigure figure, int slotIndex) {
        ArrayList<String> tiers = ItemFigure.getAbilityTiers(figure.getOriginalStack());
        String tierLetter = (slotIndex < tiers.size()) ? tiers.get(slotIndex) : "a";
        return TeenyBalance.getEffectiveManaCost(slotIndex + 1, tierLetter);
    }
}
