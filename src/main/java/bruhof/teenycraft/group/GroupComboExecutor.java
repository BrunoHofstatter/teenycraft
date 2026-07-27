package bruhof.teenycraft.group;

public final class GroupComboExecutor {
    private GroupComboExecutor() {
    }

    public static GroupComboStatBonus resolveStatBonus(FigureGroupDefinition group) {
        if (group == null) {
            return GroupComboStatBonus.NONE;
        }

        GroupComboStatBonus result = GroupComboStatBonus.NONE;
        for (String effectId : group.comboEffectIds()) {
            GroupComboEffectSpec effect = GroupComboEffectRegistry.get(effectId);
            if (effect != null) {
                result = result.plus(effect.statBonus());
            }
        }
        return result;
    }
}
