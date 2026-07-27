package bruhof.teenycraft.battle.effect;

import java.util.HashMap;
import java.util.Map;

public class EffectInstance {
    public static final int NO_CASTER_FIGURE_INDEX = -1;

    public int duration;
    public final int initialDuration;
    public int magnitude;
    public int tickCounter; // Tracks elapsed ticks for infinite effects
    public float power; // Casting power/mana for scaling
    public java.util.UUID casterUUID; // Tracks attacker for crit/luck calculations
    public int casterFigureIndex; // Tracks the original attacker figure on that participant side
    public String sourceAccessoryId;
    private final Map<String, Integer> accessoryMagnitudeContributions = new HashMap<>();
    
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
        this(d, m, p, caster, casterFigureIndex, "");
    }

    public EffectInstance(int d, int m, float p, java.util.UUID caster, int casterFigureIndex, String sourceAccessoryId) {
        this.duration = d;
        this.initialDuration = d;
        this.magnitude = m;
        this.power = p;
        this.casterUUID = caster;
        this.casterFigureIndex = casterFigureIndex;
        this.sourceAccessoryId = sourceAccessoryId != null ? sourceAccessoryId : "";
        if (!this.sourceAccessoryId.isEmpty() && m > 0) {
            accessoryMagnitudeContributions.put(this.sourceAccessoryId, m);
        }
    }

    public void addAccessoryMagnitude(String accessoryId, int magnitude) {
        if (accessoryId != null && !accessoryId.isBlank() && magnitude > 0) {
            accessoryMagnitudeContributions.merge(accessoryId, magnitude, Integer::sum);
            sourceAccessoryId = accessoryId;
        }
    }

    public void replaceAccessoryMagnitude(String accessoryId, int magnitude) {
        accessoryMagnitudeContributions.clear();
        sourceAccessoryId = accessoryId != null ? accessoryId : "";
        if (!sourceAccessoryId.isEmpty() && magnitude > 0) {
            accessoryMagnitudeContributions.put(sourceAccessoryId, magnitude);
        }
    }

    public void clearAccessoryMagnitudes() {
        accessoryMagnitudeContributions.clear();
        sourceAccessoryId = "";
    }

    public int getAccessoryMagnitude(String accessoryId) {
        return accessoryMagnitudeContributions.getOrDefault(accessoryId, 0);
    }
}
