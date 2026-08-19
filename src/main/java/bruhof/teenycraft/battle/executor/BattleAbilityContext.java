package bruhof.teenycraft.battle.executor;

import bruhof.teenycraft.battle.BattleAbilitySlot;
import bruhof.teenycraft.battle.BattleFigure;
import bruhof.teenycraft.capability.IBattleState;
import bruhof.teenycraft.util.AbilityLoader;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public record BattleAbilityContext(IBattleState state, LivingEntity attacker, BattleFigure figure, int slotIndex,
                                   AbilityLoader.AbilityData data, int actualManaCost, int effectiveManaCost,
                                   boolean isGolden) {
    @Nullable
    public static BattleAbilityContext create(IBattleState state, LivingEntity attacker, BattleFigure figure, int slotIndex) {
        BattleAbilitySlot slot = BattleAbilitySlot.resolve(figure, slotIndex);
        if (slot == null) {
            return null;
        }
        return createResolved(
                state,
                attacker,
                figure,
                slotIndex,
                slot.data(),
                slot.actualManaCost(),
                slot.effectiveManaCost(),
                slot.golden()
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
        BattleAbilitySlot slot = BattleAbilitySlot.resolve(figure, slotIndex);
        return slot != null ? slot.data() : null;
    }

    public static int resolveActualManaCost(BattleFigure figure, int slotIndex) {
        BattleAbilitySlot slot = BattleAbilitySlot.resolve(figure, slotIndex);
        return slot != null ? slot.actualManaCost() : 0;
    }

    public static int resolveEffectiveManaCost(BattleFigure figure, int slotIndex) {
        BattleAbilitySlot slot = BattleAbilitySlot.resolve(figure, slotIndex);
        return slot != null ? slot.effectiveManaCost() : 0;
    }
}
