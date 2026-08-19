package bruhof.teenycraft.battle.presentation;

import java.util.List;

public record BattleHudParticipantSnapshot(
        int entityId,
        String name,
        String activeFigureId,
        String activeSkinId,
        String activeFigureModelType,
        int activeFigureIndex,
        int currentHp,
        int maxHp,
        float currentMana,
        float currentTofuMana,
        String tofuPreviewEffectId,
        boolean tofuPreviewSelfTarget,
        float batteryCharge,
        float batterySpawnPct,
        boolean accessoryActive,
        String equippedAccessoryId,
        int basePower,
        int powerUp,
        int powerDown,
        int[] cooldowns,
        int[] slotProgress,
        boolean[] hasActiveMine,
        int waffleBlockedSlot,
        int waffleTicksRemaining,
        int chargeSlot,
        int chargeTicksRemaining,
        int chargeTotalTicks,
        int blueChannelSlot,
        int blueChannelTicksRemaining,
        int blueChannelTotalTicks,
        List<String> abilityIds,
        List<String> abilityTiers,
        List<Boolean> abilityGolden,
        List<BattleHudEffectSnapshot> effects,
        List<String> benchInfo,
        List<Integer> benchIndices,
        List<String> benchFigureIds
) {
    private static final int ABILITY_SLOT_COUNT = 3;

    public BattleHudParticipantSnapshot {
        name = name != null ? name : "";
        activeFigureId = activeFigureId != null ? activeFigureId : "none";
        activeSkinId = activeSkinId != null ? activeSkinId : activeFigureId;
        activeFigureModelType = activeFigureModelType != null ? activeFigureModelType : "default";
        tofuPreviewEffectId = tofuPreviewEffectId != null ? tofuPreviewEffectId : "";
        equippedAccessoryId = equippedAccessoryId != null ? equippedAccessoryId : "";
        cooldowns = copyIntArray(cooldowns);
        slotProgress = copyIntArray(slotProgress);
        hasActiveMine = copyBooleanArray(hasActiveMine);
        abilityIds = safeList(abilityIds);
        abilityTiers = safeList(abilityTiers);
        abilityGolden = safeList(abilityGolden);
        effects = safeList(effects);
        benchInfo = safeList(benchInfo);
        benchIndices = safeList(benchIndices);
        benchFigureIds = safeList(benchFigureIds);
    }

    public static BattleHudParticipantSnapshot empty() {
        return new BattleHudParticipantSnapshot(
                0,
                "",
                "none",
                "none",
                "default",
                0,
                0,
                0,
                0.0f,
                0.0f,
                "",
                true,
                0.0f,
                -1.0f,
                false,
                "",
                0,
                0,
                0,
                new int[ABILITY_SLOT_COUNT],
                new int[ABILITY_SLOT_COUNT],
                new boolean[ABILITY_SLOT_COUNT],
                -1,
                0,
                -1,
                0,
                0,
                -1,
                0,
                0,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    public boolean hasActiveMine(int slot) {
        return slot >= 0 && slot < hasActiveMine.length && hasActiveMine[slot];
    }

    public boolean hasEffect(String effectPrefix) {
        for (BattleHudEffectSnapshot effect : effects) {
            if (effect.id().toLowerCase().startsWith(effectPrefix.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public boolean isVisibleOpponent() {
        return !name.isEmpty() && !"None".equals(name);
    }

    public boolean hasAccessory() {
        return !equippedAccessoryId.isEmpty();
    }

    private static int[] copyIntArray(int[] values) {
        int[] copy = new int[ABILITY_SLOT_COUNT];
        if (values != null) {
            System.arraycopy(values, 0, copy, 0, Math.min(values.length, ABILITY_SLOT_COUNT));
        }
        return copy;
    }

    private static boolean[] copyBooleanArray(boolean[] values) {
        boolean[] copy = new boolean[ABILITY_SLOT_COUNT];
        if (values != null) {
            System.arraycopy(values, 0, copy, 0, Math.min(values.length, ABILITY_SLOT_COUNT));
        }
        return copy;
    }

    private static <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
