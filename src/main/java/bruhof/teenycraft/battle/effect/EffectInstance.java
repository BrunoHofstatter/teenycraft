package bruhof.teenycraft.battle.effect;

public class EffectInstance {
    public int duration;
    public int magnitude;
    public float power; // Casting power/mana for scaling
    public java.util.UUID casterUUID; // Tracks attacker for crit/luck calculations
    
    public EffectInstance(int d, int m) {
        this(d, m, 0, null);
    }

    public EffectInstance(int d, int m, float p) {
        this(d, m, p, null);
    }

    public EffectInstance(int d, int m, float p, java.util.UUID caster) {
        this.duration = d;
        this.magnitude = m;
        this.power = p;
        this.casterUUID = caster;
    }
}
