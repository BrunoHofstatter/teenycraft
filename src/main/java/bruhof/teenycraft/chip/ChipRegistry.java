package bruhof.teenycraft.chip;

import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.item.custom.ItemChip;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ChipRegistry {
    private static final Map<String, ChipSpec> REGISTRY = new HashMap<>();

    static {
        register(ChipSpec.builder("tough_guy", 3)
                .addFlatStat(ChipStatType.POWER, TeenyBalance.CHIP_TOUGH_GUY_POWER_BY_RANK)
                .addFlatStat(ChipStatType.MAX_HP, TeenyBalance.CHIP_TOUGH_GUY_HP_BY_RANK)
                .fusionCosts(TeenyBalance.CHIP_TOUGH_GUY_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("smokescreen", 3)
                .addFlatStat(ChipStatType.DODGE, TeenyBalance.CHIP_SMOKESCREEN_DODGE_BY_RANK)
                .fusionCosts(TeenyBalance.CHIP_SMOKESCREEN_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("tough_smokescreen", 3)
                .inherit("tough_guy", TeenyBalance.CHIP_SPECIAL_TOUGH_SMOKESCREEN_TOUGH_GUY_SCALE)
                .inherit("smokescreen", TeenyBalance.CHIP_SPECIAL_TOUGH_SMOKESCREEN_SMOKESCREEN_SCALE)
                .fusionCosts(TeenyBalance.CHIP_TOUGH_SMOKESCREEN_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("lucky_hearts", 3)
                .onCritHit(ChipSpec.Action.healSelfMaxHpPct(TeenyBalance.CHIP_LUCKY_HEARTS_HEAL_PCT_BY_RANK))
                .fusionCosts(TeenyBalance.CHIP_LUCKY_HEARTS_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("insta_cast_chance", 3)
                .extraInstantCastChance(TeenyBalance.CHIP_INSTA_CAST_CHANCE_BY_RANK)
                .fusionCosts(TeenyBalance.CHIP_INSTA_CAST_CHANCE_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("dance_entry", 3)
                .onFirstAppearance(ChipSpec.Action.applyEffectSelf("dance", TeenyBalance.CHIP_DANCE_DURATION_BY_RANK, TeenyBalance.CHIP_ONE_MAGNITUDE_BY_RANK))
                .fusionCosts(TeenyBalance.CHIP_DANCE_ENTRY_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("mana_boost", 3)
                .onFirstAppearance(ChipSpec.Action.addManaSelf(TeenyBalance.CHIP_MANA_BOOST_MANA_BY_RANK))
                .fusionCosts(TeenyBalance.CHIP_MANA_BOOST_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("death_energy", 3)
                .onFaint(ChipSpec.Action.addBatterySelf(TeenyBalance.CHIP_DEATH_ENERGY_BATTERY_BY_RANK))
                .fusionCosts(TeenyBalance.CHIP_DEATH_ENERGY_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("self_explosion", 3)
                .onFaint(ChipSpec.Action.dealDamageOpponent(TeenyBalance.CHIP_SELF_EXPLOSION_DAMAGE_BY_RANK, true))
                .fusionCosts(TeenyBalance.CHIP_SELF_EXPLOSION_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("necromancer", 3)
                .onKill(ChipSpec.Action.summonPetSelf(TeenyBalance.CHIP_NECROMANCER_DURATION_BY_RANK, TeenyBalance.CHIP_NECROMANCER_DAMAGE_BY_RANK))
                .fusionCosts(TeenyBalance.CHIP_NECROMANCER_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("vampire", 3)
                .onKill(ChipSpec.Action.healSelfMaxHpPct(TeenyBalance.CHIP_VAMPIRE_HEAL_PCT_BY_RANK))
                .fusionCosts(TeenyBalance.CHIP_VAMPIRE_FUSION_COST_BY_RANK)
                .build());
    }

    public static void init() {
        // Intentional no-op. Called during mod bootstrap to make registry initialization explicit.
    }

    private static void register(ChipSpec spec) {
        REGISTRY.put(spec.getId(), spec);
    }

    public static ChipSpec get(String id) {
        return REGISTRY.get(id);
    }

    public static ChipSpec get(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemChip itemChip)) {
            return null;
        }
        return get(itemChip.getChipId());
    }

    public static int getRank(ItemStack stack) {
        ChipSpec spec = get(stack);
        if (spec == null) {
            return 0;
        }
        return spec.clampRank(ItemChip.getChipRank(stack));
    }

    public static Collection<ChipSpec> getAll() {
        return REGISTRY.values();
    }
}
