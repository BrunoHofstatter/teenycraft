package bruhof.teenycraft.screen;

import java.util.List;

public record GroupComboOption(String id, String name, int priority, List<GroupComboEffectOption> effects) {
    public GroupComboOption {
        effects = List.copyOf(effects);
    }
}
