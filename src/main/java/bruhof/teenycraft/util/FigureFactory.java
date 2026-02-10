package bruhof.teenycraft.util;

import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.item.custom.ItemFigure;
import net.minecraft.world.item.ItemStack;

public class FigureFactory {

    /**
     * Creates a fresh figure item using the Scaling System.
     *
     * @param item The ItemFigure instance
     * @param id The ID (e.g., "robin")
     * @param hpScale How many "Upgrade Points" is the base HP worth?
     * @param powerScale How many "Upgrade Points" is the base Power worth?
     * @param dodgeScale ...
     * @param luckScale ...
     * @return A generic ItemFigure Stack with specific NBT data.
     */
    public static ItemStack create(ItemStack stack, String id, float hpScale, float powerScale, float dodgeScale, float luckScale) {

        // 1. Calculate the Raw Stats using the Master Balance File
        int finalHp = (int) (hpScale * TeenyBalance.UPGRADE_GAIN_HP);
        int finalPower = (int) (powerScale * TeenyBalance.UPGRADE_GAIN_POWER);
        int finalDodge = (int) (dodgeScale * TeenyBalance.UPGRADE_GAIN_DODGE);
        int finalLuck = (int) (luckScale * TeenyBalance.UPGRADE_GAIN_LUCK);

        // 2. Inject into the Item NBT
        ItemFigure.initializeFigure(stack, id, finalHp, finalPower, finalDodge, finalLuck);

        return stack;
    }
}