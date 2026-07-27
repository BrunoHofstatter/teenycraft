package bruhof.teenycraft.client;

import bruhof.teenycraft.battle.presentation.BattleUiEventPayload;
import net.minecraft.Util;

import java.util.ArrayList;
import java.util.List;

public final class BattleUiFeedbackManager {
    private static final int MAX_EVENTS = 48;
    private static final List<ActiveEvent> EVENTS = new ArrayList<>();

    private BattleUiFeedbackManager() {
    }

    public static void push(BattleUiEventPayload payload) {
        if (payload == null) {
            return;
        }

        pruneExpired();
        EVENTS.add(new ActiveEvent(payload, Util.getMillis()));
        if (EVENTS.size() > MAX_EVENTS) {
            EVENTS.remove(0);
        }
    }

    public static void clear() {
        EVENTS.clear();
    }

    public static List<ActiveEvent> getVisibleEvents() {
        pruneExpired();
        return List.copyOf(EVENTS);
    }

    private static void pruneExpired() {
        long now = Util.getMillis();
        EVENTS.removeIf(event -> event.ageMs(now) >= event.lifeMs());
    }

    public record ActiveEvent(BattleUiEventPayload payload, long createdAtMs) {
        public long ageMs(long now) {
            return Math.max(0L, now - createdAtMs);
        }

        public long lifeMs() {
            return switch (payload.type()) {
                case DAMAGE, DAMAGE_GHOST, HEAL -> 1550L;
                case ABILITY, TOFU_GAINED, TOFU_RESULT, PICKUP -> 1700L;
                case CRIT, CLASS_BONUS, MANA, BATTERY -> 1350L;
            };
        }
    }
}
