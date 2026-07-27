package bruhof.teenycraft.accessory;

import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.capability.AccessoryMasteryProvider;
import net.minecraft.world.entity.player.Player;

public final class AccessoryTierResolver {
    private AccessoryTierResolver() {
    }

    public static int getTier(Player player, String accessoryId) {
        if (player == null) {
            return AccessoryProgression.MIN_TIER;
        }
        return player.getCapability(AccessoryMasteryProvider.ACCESSORY_MASTERY)
                .map(mastery -> mastery.getTier(accessoryId))
                .orElse(AccessoryProgression.MIN_TIER);
    }

    public static ResolvedAccessorySpec resolveForPlayer(Player player, AccessorySpec spec) {
        return resolve(spec, getTier(player, spec.getId()));
    }

    public static ResolvedAccessorySpec resolve(AccessorySpec spec, int requestedTier) {
        int tier = AccessoryProgression.clampTier(requestedTier);
        int interval = spec.getIntervalTicks();
        int duration = spec.getEffectDurationTicks();
        int magnitude = spec.getEffectMagnitude();
        int damage = spec.getDamage();
        float maxHpBonus = spec.getMaxHpBonusPct();
        float drain = TeenyBalance.ACCESSORY_DRAIN_PER_TICK;
        int kryptoPowerUp = TeenyBalance.ACCESSORY_KRYPTO_POWER_UP;
        int kryptoHeal = TeenyBalance.ACCESSORY_KRYPTO_HEAL;
        float kryptoTofuPower = TeenyBalance.ACCESSORY_KRYPTO_TOFU_POWER;

        if (tier >= 2) {
            switch (spec.getId()) {
                case "titans_coin" -> maxHpBonus = scale(maxHpBonus, TeenyBalance.ACCESSORY_TIER_2_STANDARD_STRENGTH_MULT);
                case "mother_box", "bat_signal", "birdarang" ->
                        damage = scale(damage, TeenyBalance.ACCESSORY_TIER_2_STANDARD_STRENGTH_MULT);
                case "red_lantern_battery", "green_lantern_battery", "violet_lantern_battery" ->
                        magnitude = scale(magnitude, TeenyBalance.ACCESSORY_TIER_2_STANDARD_STRENGTH_MULT);
                case "ravens_spellbook" -> magnitude = curseReductionMagnitude(
                        TeenyBalance.ACCESSORY_RAVENS_SPELLBOOK_TIER_2_STRENGTH_MULT);
                case "cyborgs_waffle_shooter" ->
                        duration = scale(duration, TeenyBalance.ACCESSORY_WAFFLE_SHOOTER_TIER_2_DURATION_MULT);
                case "lil_penguin" ->
                        magnitude = scale(magnitude, TeenyBalance.ACCESSORY_LIL_PENGUIN_TIER_2_FREEZE_MULT);
                case "kryptonite" ->
                        magnitude = scale(magnitude, TeenyBalance.ACCESSORY_KRYPTONITE_TIER_2_STRENGTH_MULT);
                case "krypto_the_superdog" -> {
                    kryptoPowerUp = scale(kryptoPowerUp, TeenyBalance.ACCESSORY_TIER_2_STANDARD_STRENGTH_MULT);
                    kryptoHeal = scale(kryptoHeal, TeenyBalance.ACCESSORY_TIER_2_STANDARD_STRENGTH_MULT);
                    kryptoTofuPower = scale(kryptoTofuPower, TeenyBalance.ACCESSORY_TIER_2_STANDARD_STRENGTH_MULT);
                }
            }
        }

        if (tier >= 3) {
            drain = scale(drain, TeenyBalance.ACCESSORY_TIER_3_BATTERY_DRAIN_MULT);
        }

        if (tier >= 4) {
            switch (spec.getId()) {
                case "titans_coin" -> maxHpBonus = scale(spec.getMaxHpBonusPct(), TeenyBalance.ACCESSORY_TIER_4_STANDARD_STRENGTH_MULT);
                case "mother_box", "bat_signal", "red_lantern_battery", "green_lantern_battery",
                     "violet_lantern_battery", "lil_penguin", "krypto_the_superdog" ->
                        interval = scaleInterval(spec.getIntervalTicks());
                case "ravens_spellbook" -> magnitude = curseReductionMagnitude(
                        TeenyBalance.ACCESSORY_RAVENS_SPELLBOOK_TIER_4_STRENGTH_MULT);
                case "cyborgs_waffle_shooter" ->
                        duration = scale(spec.getEffectDurationTicks(), TeenyBalance.ACCESSORY_WAFFLE_SHOOTER_TIER_4_DURATION_MULT);
                case "kryptonite" ->
                        magnitude = scale(spec.getEffectMagnitude(), TeenyBalance.ACCESSORY_KRYPTONITE_TIER_4_STRENGTH_MULT);
                case "birdarang" ->
                        damage = scale(spec.getDamage(), TeenyBalance.ACCESSORY_TIER_4_STANDARD_STRENGTH_MULT);
            }
        }

        if ("ravens_spellbook".equals(spec.getId()) && tier == 1) {
            magnitude = curseReductionMagnitude(1.0f);
        }

        if (tier >= 5 && "bat_signal".equals(spec.getId())) {
            damage = scale(damage, TeenyBalance.ACCESSORY_BAT_SIGNAL_TIER_5_DAMAGE_MULT);
        }

        return new ResolvedAccessorySpec(spec, tier, interval, duration, magnitude, damage, maxHpBonus, drain,
                kryptoPowerUp, kryptoHeal, kryptoTofuPower);
    }

    private static int scale(int base, float multiplier) {
        return Math.round(base * multiplier);
    }

    private static float scale(float base, float multiplier) {
        return base * multiplier;
    }

    private static int scaleInterval(int base) {
        if (base <= 0) {
            return base;
        }
        return Math.max(1, Math.round(base * TeenyBalance.ACCESSORY_TIER_4_INTERVAL_MULT));
    }

    private static int curseReductionMagnitude(float strengthMultiplier) {
        float baseReduction = 1.0f - TeenyBalance.CURSE_EFFICIENCY;
        return Math.round(baseReduction * strengthMultiplier * 100.0f);
    }
}
