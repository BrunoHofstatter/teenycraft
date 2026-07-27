package bruhof.teenycraft.accessory;

import bruhof.teenycraft.TeenyBalance;

import java.text.DecimalFormat;

public final class AccessoryPresentation {
    private static final DecimalFormat DECIMAL = new DecimalFormat("0.#");

    private AccessoryPresentation() {
    }

    public static String description(String accessoryId) {
        return switch (accessoryId) {
            case "titans_coin" -> "Raises the maximum HP of every figure on your team while active.";
            case "mother_box" -> "Repeatedly damages the opposing active figure.";
            case "bat_signal" -> "Calls in a delayed barrage that damages the whole opposing team.";
            case "red_lantern_battery" -> "Repeatedly grants Power Up to your active figure.";
            case "green_lantern_battery" -> "Repeatedly restores mana to your active figure.";
            case "violet_lantern_battery" -> "Repeatedly heals your active figure.";
            case "ravens_spellbook" -> "Repeatedly curses the opposing active figure, slowing its mana regeneration.";
            case "cyborgs_waffle_shooter" -> "Repeatedly covers the opposing active figure in disabling waffles.";
            case "lil_penguin" -> "Periodically freezes the opposing active figure and burns its mana.";
            case "kryptonite" -> "Repeatedly lowers the opposing active figure's defense.";
            case "justice_league_coin" -> "Repeatedly raises the luck of your active figure.";
            case "birdarang" -> "Retaliates against enemies that damage your active figure.";
            case "supermans_underpants" -> "Uses battery power to reduce incoming damage and prevent fatal hits.";
            case "krypto_the_superdog" -> "Calls Krypto for a random heal, Power Up, or tofu boost.";
            default -> "A battle accessory powered by the shared Battle Battery.";
        };
    }

    public static String role(String accessoryId) {
        return switch (accessoryId) {
            case "mother_box", "bat_signal", "birdarang" -> "DAMAGE";
            case "ravens_spellbook", "cyborgs_waffle_shooter", "lil_penguin", "kryptonite" -> "CONTROL";
            case "titans_coin", "red_lantern_battery", "green_lantern_battery",
                 "violet_lantern_battery", "justice_league_coin" -> "SUPPORT";
            case "supermans_underpants" -> "DEFENSE";
            case "krypto_the_superdog" -> "WILDCARD";
            default -> "ACCESSORY";
        };
    }

    public static String tierUpgrade(String accessoryId, int tier) {
        AccessorySpec spec = AccessoryRegistry.get(accessoryId);
        if (spec == null) {
            return "Unknown accessory.";
        }
        if (tier <= 1) {
            return "Base accessory effect unlocked.";
        }
        if (tier == 3) {
            ResolvedAccessorySpec base = AccessoryTierResolver.resolve(spec, 1);
            ResolvedAccessorySpec resolved = AccessoryTierResolver.resolve(spec, tier);
            float reduction = base.batteryDrainPerTick() <= 0.0f
                    ? 0.0f
                    : 1.0f - resolved.batteryDrainPerTick() / base.batteryDrainPerTick();
            return "Battery drains " + percent(reduction) + " slower while this accessory is active.";
        }
        if (tier == 5) {
            return switch (accessoryId) {
                case "titans_coin" -> "Activation heals each living team figure by "
                        + percent(TeenyBalance.ACCESSORY_TITANS_COIN_TIER_5_HEAL_PCT)
                        + " of its original maximum HP.";
                case "mother_box" -> "Every " + TeenyBalance.ACCESSORY_MOTHER_BOX_TIER_5_CRITICAL_HIT_INTERVAL
                        + "th damage pulse is a critical hit that deals "
                        + DECIMAL.format(TeenyBalance.ACCESSORY_MOTHER_BOX_TIER_5_CRITICAL_DAMAGE_MULT) + "x damage.";
                case "bat_signal" -> "Deals "
                        + percent(TeenyBalance.ACCESSORY_BAT_SIGNAL_TIER_5_DAMAGE_MULT - 1.0f)
                        + " more damage and cannot be dodged or blocked by shields.";
                case "red_lantern_battery" -> "Retains "
                        + percent(TeenyBalance.ACCESSORY_RED_LANTERN_TIER_5_POWER_UP_RETENTION)
                        + " of its Power Up after that Power Up is consumed.";
                case "green_lantern_battery" -> "Activation immediately restores "
                        + TeenyBalance.ACCESSORY_GREEN_LANTERN_TIER_5_ACTIVATION_MANA + " mana.";
                case "violet_lantern_battery" -> "Its healing can exceed normal maximum HP by "
                        + percent(TeenyBalance.ACCESSORY_VIOLET_LANTERN_TIER_5_OVERHEAL_PCT) + ".";
                case "ravens_spellbook" -> "Cursed enemies also have their movement speed reduced by "
                        + percent(1.0f - TeenyBalance.ACCESSORY_RAVENS_SPELLBOOK_TIER_5_SPEED_MULT) + ".";
                case "cyborgs_waffle_shooter" -> "Every "
                        + TeenyBalance.ACCESSORY_WAFFLE_SHOOTER_TIER_5_SECONDARY_INTERVAL
                        + "th successful waffle blocks a second ability for half duration.";
                case "kryptonite" -> "Also reduces enemy damage, movement speed, and mana regeneration by "
                        + percent(1.0f - TeenyBalance.ACCESSORY_KRYPTONITE_TIER_5_PENALTY_MULT) + ".";
                case "birdarang" -> "While inactive, retaliates for "
                        + TeenyBalance.ACCESSORY_BIRDARANG_TIER_5_PASSIVE_DAMAGE + " damage when the active figure is hit.";
                case "supermans_underpants" -> "No longer drains battery passively; battery is spent only when blocking damage.";
                case "krypto_the_superdog" -> "Each pulse grants "
                        + TeenyBalance.ACCESSORY_KRYPTO_TIER_5_EFFECT_COUNT + " different Krypto effects.";
                default -> "Unique mastery upgrade is not implemented yet.";
            };
        }

        ResolvedAccessorySpec resolved = AccessoryTierResolver.resolve(spec, tier);
        return switch (accessoryId) {
            case "titans_coin" -> "Team maximum HP bonus becomes " + percent(resolved.maxHpBonusPct()) + ".";
            case "mother_box" -> tier == 2
                    ? "Each pulse deals " + resolved.damage() + " damage."
                    : intervalText(resolved.intervalTicks());
            case "bat_signal" -> tier == 2
                    ? "Each barrage hit deals " + resolved.damage() + " damage."
                    : "The barrage begins after " + seconds(resolved.intervalTicks()) + " seconds.";
            case "red_lantern_battery" -> tier == 2
                    ? "Each pulse grants " + resolved.effectMagnitude() + " Power Up."
                    : intervalText(resolved.intervalTicks());
            case "green_lantern_battery" -> tier == 2
                    ? "Each pulse restores " + resolved.effectMagnitude() + " mana."
                    : intervalText(resolved.intervalTicks());
            case "violet_lantern_battery" -> tier == 2
                    ? "Each pulse restores " + resolved.effectMagnitude() + " HP."
                    : intervalText(resolved.intervalTicks());
            case "ravens_spellbook" -> "Curse reduces mana regeneration by " + resolved.effectMagnitude() + "%.";
            case "cyborgs_waffle_shooter" -> "Waffles last " + seconds(resolved.effectDurationTicks()) + " seconds.";
            case "lil_penguin" -> tier == 2
                    ? "Freeze burns " + resolved.effectMagnitude() + "% of current mana."
                    : intervalText(resolved.intervalTicks());
            case "kryptonite" -> "Defense Down reduces defense by " + resolved.effectMagnitude() + "%.";
            case "birdarang" -> "Each retaliation deals " + resolved.damage() + " damage.";
            case "krypto_the_superdog" -> tier == 2
                    ? "Krypto grants " + resolved.kryptoHeal() + " healing, " + resolved.kryptoPowerUp()
                    + " Power Up, or a " + percent(resolved.kryptoTofuPower() / 100.0f) + " tofu boost."
                    : intervalText(resolved.intervalTicks());
            case "justice_league_coin", "supermans_underpants" ->
                    "No Tier " + tier + " runtime upgrade is configured yet.";
            default -> "Tier " + tier + " upgrade.";
        };
    }

    public static String milestone(AccessoryMilestoneDefinition definition) {
        if (definition == null) {
            return "This milestone has not been designed yet.";
        }
        return definition.description();
    }

    private static String intervalText(int ticks) {
        return "Triggers every " + seconds(ticks) + " seconds.";
    }

    private static String percent(float fraction) {
        return DECIMAL.format(fraction * 100.0f) + "%";
    }

    private static String seconds(int ticks) {
        return DECIMAL.format(ticks / 20.0f);
    }
}
