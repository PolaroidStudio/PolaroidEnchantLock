package me.juancayc.polaroidenchantlock.domain;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lets one refusal message through per player every {@code gap} milliseconds. A player hammering
 * a blocked result slot is told once why it does nothing, not once per click.
 *
 * <p>The clock is passed in rather than read, so the rule is testable without waiting.
 */
public final class MessageThrottle {

    private final Map<UUID, Long> last = new ConcurrentHashMap<>();

    /**
     * @return true when the message may be sent now, which also starts the next wait
     */
    public boolean tryAcquire(UUID player, long nowMillis, long gapMillis) {
        boolean[] allowed = {false};
        last.compute(player, (id, previous) -> {
            if (previous == null || nowMillis - previous >= gapMillis) {
                allowed[0] = true;
                return nowMillis;
            }
            return previous;
        });
        return allowed[0];
    }

    /** Drops what is remembered about a player who left. */
    public void forget(UUID player) {
        last.remove(player);
    }
}
