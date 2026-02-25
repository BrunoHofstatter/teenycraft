package bruhof.teenycraft.util;

import bruhof.teenycraft.item.custom.ItemFigure;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class NPCFigureBuilder {

    public static class NPCFigureData {
        public String figureId;
        public int level = 1;
        public String upgrades = "";
        public List<Integer> abilityOrder = new ArrayList<>(); // Indices 0, 1, 2
        public List<String> goldenAbilities = new ArrayList<>(); // Ability IDs
    }

    public static ItemStack build(NPCFigureData data) {
        ItemStack stack = FigureLoader.getFigureStack(data.figureId);
        if (stack.isEmpty()) return ItemStack.EMPTY;

        // 1. Set Level
        ItemFigure.setLevel(stack, data.level);

        // 2. Apply Upgrades (HPLD string)
        ItemFigure.applyUpgrades(stack, data.upgrades);

        // 3. Configure Ability Order
        if (data.abilityOrder != null && !data.abilityOrder.isEmpty()) {
            List<String> currentAbilities = ItemFigure.getAbilityOrder(stack); // Gets the pool
            List<String> newOrder = new ArrayList<>();
            for (int index : data.abilityOrder) {
                if (index >= 0 && index < currentAbilities.size()) {
                    newOrder.add(currentAbilities.get(index));
                }
            }
            // Fill remaining
            for (String id : currentAbilities) {
                if (!newOrder.contains(id)) newOrder.add(id);
            }
            ItemFigure.setAbilityOrder(stack, newOrder);
        }

        // 4. Configure Golden Status
        if (data.goldenAbilities != null) {
            for (String abilityId : data.goldenAbilities) {
                ItemFigure.setGoldenProgress(stack, abilityId, 1.0f);
            }
        }

        return stack;
    }
}
