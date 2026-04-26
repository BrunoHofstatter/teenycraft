package bruhof.teenycraft.battle.effect;

public class EffectInstance {
    public static final int NO_CASTER_FIGURE_INDEX = -1;

    public int duration;
    public final int initialDuration;
    public int magnitude;
    public int tickCounter; // Tracks elapsed ticks for infinite effects
    public float power; // Casting power/mana for scaling
    public java.util.UUID casterUUID; // Tracks attacker for crit/luck calculations
    public int casterFigureIndex; // Tracks the original attacker figure on that participant side
    
    public EffectInstance(int d, int m) {
        this(d, m, 0, null);
    }

    public EffectInstance(int d, int m, float p) {
        this(d, m, p, null);
    }

    public EffectInstance(int d, int m, float p, java.util.UUID caster) {
        this(d, m, p, caster, NO_CASTER_FIGURE_INDEX);
    }

    public EffectInstance(int d, int m, float p, java.util.UUID caster, int casterFigureIndex) {
        this.duration = d;
        this.initialDuration = d;
        this.magnitude = m;
        this.power = p;
        this.casterUUID = caster;
        this.casterFigureIndex = casterFigureIndex;
    }
}
