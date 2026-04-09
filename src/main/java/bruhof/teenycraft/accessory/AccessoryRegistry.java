package bruhof.teenycraft.accessory;

import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.item.custom.ItemAccessory;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class AccessoryRegistry {
    private static final Map<String, AccessorySpec> REGISTRY = new HashMap<>();

    static {
        register(AccessorySpec.titansCoin("titans_coin", TeenyBalance.ACCESSORY_TITANS_COIN_MAX_HP_BONUS_PCT));
        register(AccessorySpec.periodicDamage("mother_box", TeenyBalance.ACCESSORY_MOTHER_BOX_INTERVAL_TICKS, TeenyBalance.ACCESSORY_MOTHER_BOX_DAMAGE, 1, false));
        register(AccessorySpec.periodicDamage("bat_signal", TeenyBalance.ACCESSORY_BAT_SIGNAL_WAKE_DELAY_TICKS, TeenyBalance.ACCESSORY_BAT_SIGNAL_DAMAGE, TeenyBalance.ACCESSORY_BAT_SIGNAL_HIT_COUNT, true));
        register(AccessorySpec.periodicEffect("red_lantern_battery", AccessorySpec.Target.SELF, "power_up",
                TeenyBalance.ACCESSORY_RED_LANTERN_INTERVAL_TICKS, TeenyBalance.ACCESSORY_RED_LANTERN_INTERVAL_TICKS, TeenyBalance.ACCESSORY_RED_LANTERN_POWER_UP));
        register(AccessorySpec.periodicEffect("green_lantern_battery", AccessorySpec.Target.SELF, "bar_fill",
                TeenyBalance.ACCESSORY_GREEN_LANTERN_INTERVAL_TICKS, 0, TeenyBalance.ACCESSORY_GREEN_LANTERN_MANA));
        register(AccessorySpec.periodicEffect("violet_lantern_battery", AccessorySpec.Target.SELF, "heal",
                TeenyBalance.ACCESSORY_VIOLET_LANTERN_INTERVAL_TICKS, 0, TeenyBalance.ACCESSORY_VIOLET_LANTERN_HEAL));
        register(AccessorySpec.periodicEffect("ravens_spellbook", AccessorySpec.Target.OPPONENT, "curse",
                TeenyBalance.ACCESSORY_RAVENS_SPELLBOOK_INTERVAL_TICKS, TeenyBalance.ACCESSORY_RAVENS_SPELLBOOK_CURSE_DURATION_TICKS, 1));
        register(AccessorySpec.periodicEffect("cyborgs_waffle_shooter", AccessorySpec.Target.OPPONENT, "waffle",
                TeenyBalance.ACCESSORY_CYBORG_WAFFLE_SHOOTER_INTERVAL_TICKS, TeenyBalance.ACCESSORY_CYBORG_WAFFLE_SHOOTER_DURATION_TICKS, 1));
        register(AccessorySpec.periodicEffect("lil_penguin", AccessorySpec.Target.OPPONENT, "freeze",
                TeenyBalance.ACCESSORY_LIL_PENGUIN_INTERVAL_TICKS, TeenyBalance.ACCESSORY_LIL_PENGUIN_FREEZE_DURATION_TICKS, TeenyBalance.ACCESSORY_LIL_PENGUIN_FREEZE_BURN_PCT));
        register(AccessorySpec.periodicEffect("kryptonite", AccessorySpec.Target.OPPONENT, "defense_down",
                TeenyBalance.ACCESSORY_KRYPTONITE_INTERVAL_TICKS, TeenyBalance.ACCESSORY_KRYPTONITE_DEFENSE_DOWN_DURATION_TICKS, TeenyBalance.ACCESSORY_KRYPTONITE_DEFENSE_DOWN_MAGNITUDE));
        register(AccessorySpec.periodicEffect("justice_league_coin", AccessorySpec.Target.SELF, "luck_up",
                TeenyBalance.ACCESSORY_JUSTICE_LEAGUE_COIN_INTERVAL_TICKS, TeenyBalance.ACCESSORY_JUSTICE_LEAGUE_COIN_LUCK_UP_DURATION_TICKS, TeenyBalance.ACCESSORY_JUSTICE_LEAGUE_COIN_LUCK_UP_MAGNITUDE));
    }

    private static void register(AccessorySpec spec) {
        REGISTRY.put(spec.getId(), spec);
    }

    public static AccessorySpec get(String id) {
        return REGISTRY.get(id);
    }

    public static AccessorySpec get(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemAccessory accessory)) {
            return null;
        }
        return get(accessory.getAccessoryId());
    }
}
