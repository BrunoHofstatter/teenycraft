package bruhof.teenycraft.screen;

import bruhof.teenycraft.golden.GoldenSacrificeCalculator;
import net.minecraft.world.item.ItemStack;

public record SilkieStationFigureView(
        SilkieStationTargetRef ref,
        ItemStack stack,
        GoldenSacrificeCalculator.Affinity affinity,
        int basePoints,
        String reason,
        boolean invested
) {
    public SilkieStationFigureView {
        stack = stack.copy();
        reason = reason == null ? "" : reason;
    }
}
