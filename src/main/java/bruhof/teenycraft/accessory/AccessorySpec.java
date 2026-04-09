package bruhof.teenycraft.accessory;

public class AccessorySpec {
    public enum Type {
        PERIODIC_EFFECT,
        PERIODIC_DAMAGE,
        TITANS_COIN
    }

    public enum Target {
        SELF,
        OPPONENT
    }

    private final String id;
    private final Type type;
    private final Target target;
    private final int intervalTicks;
    private final String effectId;
    private final int effectDurationTicks;
    private final int effectMagnitude;
    private final int damage;
    private final int hitCount;
    private final boolean groupDamage;
    private final float maxHpBonusPct;

    private AccessorySpec(String id, Type type, Target target, int intervalTicks, String effectId, int effectDurationTicks,
                          int effectMagnitude, int damage, int hitCount, boolean groupDamage, float maxHpBonusPct) {
        this.id = id;
        this.type = type;
        this.target = target;
        this.intervalTicks = intervalTicks;
        this.effectId = effectId;
        this.effectDurationTicks = effectDurationTicks;
        this.effectMagnitude = effectMagnitude;
        this.damage = damage;
        this.hitCount = hitCount;
        this.groupDamage = groupDamage;
        this.maxHpBonusPct = maxHpBonusPct;
    }

    public static AccessorySpec periodicEffect(String id, Target target, String effectId, int intervalTicks, int effectDurationTicks, int effectMagnitude) {
        return new AccessorySpec(id, Type.PERIODIC_EFFECT, target, intervalTicks, effectId, effectDurationTicks, effectMagnitude, 0, 0, false, 0.0f);
    }

    public static AccessorySpec periodicDamage(String id, int intervalTicks, int damage, int hitCount, boolean groupDamage) {
        return new AccessorySpec(id, Type.PERIODIC_DAMAGE, Target.OPPONENT, intervalTicks, null, 0, 0, damage, hitCount, groupDamage, 0.0f);
    }

    public static AccessorySpec titansCoin(String id, float maxHpBonusPct) {
        return new AccessorySpec(id, Type.TITANS_COIN, Target.SELF, 0, null, 0, 0, 0, 0, false, maxHpBonusPct);
    }

    public String getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    public Target getTarget() {
        return target;
    }

    public int getIntervalTicks() {
        return intervalTicks;
    }

    public String getEffectId() {
        return effectId;
    }

    public int getEffectDurationTicks() {
        return effectDurationTicks;
    }

    public int getEffectMagnitude() {
        return effectMagnitude;
    }

    public int getDamage() {
        return damage;
    }

    public int getHitCount() {
        return hitCount;
    }

    public boolean isGroupDamage() {
        return groupDamage;
    }

    public float getMaxHpBonusPct() {
        return maxHpBonusPct;
    }
}
