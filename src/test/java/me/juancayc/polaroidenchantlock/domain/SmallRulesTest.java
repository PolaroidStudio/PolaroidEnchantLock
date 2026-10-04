package me.juancayc.polaroidenchantlock.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The throttle, the pager and the foreign-command reader: three small rules, one file. */
class SmallRulesTest {

    // ── MessageThrottle ──────────────────────────────────────────────────────

    @Test
    void theFirstMessageGoesThroughAndTheNextWaitsForTheGap() {
        MessageThrottle throttle = new MessageThrottle();
        UUID player = UUID.randomUUID();
        assertTrue(throttle.tryAcquire(player, 1_000, 1_500));
        assertFalse(throttle.tryAcquire(player, 1_001, 1_500));
        assertFalse(throttle.tryAcquire(player, 2_499, 1_500));
        assertTrue(throttle.tryAcquire(player, 2_500, 1_500));
    }

    @Test
    void aRefusedAttemptDoesNotRestartTheWait() {
        MessageThrottle throttle = new MessageThrottle();
        UUID player = UUID.randomUUID();
        throttle.tryAcquire(player, 0, 1_000);
        throttle.tryAcquire(player, 900, 1_000);
        // Counted from the message that was sent at 0, not from the click at 900.
        assertTrue(throttle.tryAcquire(player, 1_000, 1_000));
    }

    @Test
    void playersAreThrottledSeparatelyAndCanBeForgotten() {
        MessageThrottle throttle = new MessageThrottle();
        UUID one = UUID.randomUUID();
        UUID two = UUID.randomUUID();
        assertTrue(throttle.tryAcquire(one, 0, 1_000));
        assertTrue(throttle.tryAcquire(two, 0, 1_000));
        throttle.forget(one);
        assertTrue(throttle.tryAcquire(one, 1, 1_000));
        assertFalse(throttle.tryAcquire(two, 1, 1_000));
    }

    @Test
    void aZeroGapNeverThrottles() {
        MessageThrottle throttle = new MessageThrottle();
        UUID player = UUID.randomUUID();
        assertTrue(throttle.tryAcquire(player, 5, 0));
        assertTrue(throttle.tryAcquire(player, 5, 0));
    }

    // ── Pager ────────────────────────────────────────────────────────────────

    @Test
    void thereIsAlwaysAtLeastOnePage() {
        assertEquals(1, Pager.pageCount(0, 45));
        assertEquals(1, Pager.pageCount(45, 45));
        assertEquals(2, Pager.pageCount(46, 45));
    }

    @Test
    void aStalePageIsClampedInsteadOfThrowing() {
        List<Integer> entries = List.of(1, 2, 3, 4, 5);
        assertEquals(List.of(1, 2), Pager.slice(entries, 0, 2));
        assertEquals(List.of(5), Pager.slice(entries, 2, 2));
        assertEquals(List.of(5), Pager.slice(entries, 99, 2));
        assertEquals(List.of(1, 2), Pager.slice(entries, -3, 2));
        assertEquals(List.of(), Pager.slice(List.of(), 4, 2));
    }

    // ── ForeignCommands ──────────────────────────────────────────────────────

    @Test
    void findsTheTargetOfPenchantEnchantAndRemove() {
        assertEquals("Steve", ForeignCommands.penchantTarget("/penchant enchant Steve v_sharpness 7"));
        assertEquals("Steve", ForeignCommands.penchantTarget("penchant remove Steve c_autosmelt"));
        assertEquals("Steve", ForeignCommands.penchantTarget("/pe ENCHANT Steve v_sharpness 7"));
        assertEquals("Steve", ForeignCommands.penchantTarget("/PolaroidEnchant:penchant enchant   Steve x 1"));
    }

    @Test
    void ignoresEveryOtherCommandAndSubcommand() {
        assertNull(ForeignCommands.penchantTarget(null));
        assertNull(ForeignCommands.penchantTarget("/penchant"));
        assertNull(ForeignCommands.penchantTarget("/penchant enchant"));
        assertNull(ForeignCommands.penchantTarget("/penchant book Steve v_sharpness 7"));
        assertNull(ForeignCommands.penchantTarget("/penchant reload now please"));
        assertNull(ForeignCommands.penchantTarget("/enchant Steve sharpness 5"));
        assertNull(ForeignCommands.penchantTarget("/pex enchant Steve x"));
    }
}
