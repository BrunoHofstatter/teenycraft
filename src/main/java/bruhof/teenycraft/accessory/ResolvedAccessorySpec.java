package bruhof.teenycraft.accessory;

public record ResolvedAccessorySpec(
        AccessorySpec baseSpec,
        int tier,
        int intervalTicks,
        int effectDurationTicks,
        int effectMagnitude,
        int damage,
        float maxHpBonusPct,
        float batteryDrainPerTick,
        int kryptoPowerUp,
        int kryptoHeal,
        float kryptoTofuPower
) {
    public String id() {
        return baseSpec.getId();
    }

    public AccessorySpec.Type type() {
        return baseSpec.getType();
    }

    public AccessorySpec.Target target() {
        return baseSpec.getTarget();
    }

    public String effectId() {
        return baseSpec.getEffectId();
    }

    public int hitCount() {
        return baseSpec.getHitCount();
    }

    public boolean groupDamage() {
        return baseSpec.isGroupDamage();
    }
}
