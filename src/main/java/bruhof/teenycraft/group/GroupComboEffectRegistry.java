package bruhof.teenycraft.group;

import bruhof.teenycraft.TeenyBalance;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class GroupComboEffectRegistry {
    private static final Map<String, GroupComboEffectSpec> REGISTRY = new LinkedHashMap<>();

    static {
        register(new GroupComboEffectSpec(
                "none",
                "Unassigned",
                "No combo effect has been assigned yet.",
                "none",
                GroupComboStatBonus.NONE
        ));
        register(new GroupComboEffectSpec(
                "stat_health",
                "Health Boost",
                "Both combo figures gain +" + TeenyBalance.GROUP_COMBO_HEALTH_BONUS + " max HP.",
                "health",
                new GroupComboStatBonus(TeenyBalance.GROUP_COMBO_HEALTH_BONUS, 0, 0, 0)
        ));
        register(new GroupComboEffectSpec(
                "stat_power",
                "Power Boost",
                "Both combo figures gain +" + TeenyBalance.GROUP_COMBO_POWER_BONUS + " Power.",
                "power",
                new GroupComboStatBonus(0, TeenyBalance.GROUP_COMBO_POWER_BONUS, 0, 0)
        ));
        register(new GroupComboEffectSpec(
                "stat_dodge",
                "Dodge Boost",
                "Both combo figures gain +" + TeenyBalance.GROUP_COMBO_DODGE_BONUS + " Dodge.",
                "dodge",
                new GroupComboStatBonus(0, 0, TeenyBalance.GROUP_COMBO_DODGE_BONUS, 0)
        ));
        register(new GroupComboEffectSpec(
                "stat_luck",
                "Luck Boost",
                "Both combo figures gain +" + TeenyBalance.GROUP_COMBO_LUCK_BONUS + " Luck.",
                "luck",
                new GroupComboStatBonus(0, 0, 0, TeenyBalance.GROUP_COMBO_LUCK_BONUS)
        ));
    }

    private GroupComboEffectRegistry() {
    }

    public static void init() {
        // Explicit bootstrap hook.
    }

    private static void register(GroupComboEffectSpec spec) {
        if (REGISTRY.putIfAbsent(spec.id(), spec) != null) {
            throw new IllegalStateException("Duplicate group combo effect id: " + spec.id());
        }
    }

    public static GroupComboEffectSpec get(String id) {
        return REGISTRY.get(id);
    }

    public static Set<String> getSupportedIds() {
        return Set.copyOf(REGISTRY.keySet());
    }

    public static Collection<GroupComboEffectSpec> getAll() {
        return java.util.List.copyOf(REGISTRY.values());
    }
}
