package me.juancayc.polaroidenchantlock.domain;

import me.juancayc.polaroidenchantlock.domain.LockRules.AnvilAttempt;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LockRulesTest {

    private static final Map<String, Integer> SHARP_3 = Map.of("minecraft:sharpness", 3);
    private static final Map<String, Integer> SHARP_4 = Map.of("minecraft:sharpness", 4);
    private static final Map<String, Integer> SHARP_3_UNBREAKING = Map.of("minecraft:sharpness", 3, "minecraft:unbreaking", 1);

    private static AnvilAttempt attempt(boolean base, boolean addition, Map<String, Integer> before,
                                        Map<String, Integer> after, boolean restUnchanged) {
        return new AnvilAttempt(base, addition, before, after, restUnchanged);
    }

    @Test
    void anAnvilWithNoLockedPieceIsNeverBlocked() {
        assertFalse(LockRules.anvilBlocked(attempt(false, false, SHARP_3, SHARP_4, false), true));
        assertFalse(LockRules.anvilBlocked(attempt(false, false, SHARP_3, SHARP_4, false), false));
    }

    @Test
    void aRepairOrRenameOfALockedPieceFollowsTheSetting() {
        AnvilAttempt repair = attempt(true, false, SHARP_3, SHARP_3, true);
        assertFalse(LockRules.anvilBlocked(repair, true));
        assertTrue(LockRules.anvilBlocked(repair, false));
    }

    @Test
    void aRepairWithAnotherLockedPieceIsStillARepair() {
        assertFalse(LockRules.anvilBlocked(attempt(true, true, SHARP_3, SHARP_3, true), true));
    }

    @Test
    void anyEnchantmentChangeOnALockedPieceIsBlockedEvenWhenRepairsAreAllowed() {
        assertTrue(LockRules.anvilBlocked(attempt(true, false, SHARP_3, SHARP_4, true), true));
        assertTrue(LockRules.anvilBlocked(attempt(true, false, SHARP_3, SHARP_3_UNBREAKING, true), true));
        assertTrue(LockRules.anvilBlocked(attempt(true, false, SHARP_3, Map.of(), true), true));
        assertTrue(LockRules.anvilBlocked(attempt(true, false, Map.of(), SHARP_3, true), true));
    }

    @Test
    void sameVanillaEnchantmentsButOtherDataRewrittenIsBlocked() {
        // A plugin that keeps its enchantments in persistent data: the vanilla list is equal,
        // the item is not.
        assertTrue(LockRules.anvilBlocked(attempt(true, false, SHARP_3, SHARP_3, false), true));
    }

    @Test
    void aLockedPieceUsedUpOnAnotherItemIsAlwaysBlocked() {
        assertTrue(LockRules.anvilBlocked(attempt(false, true, SHARP_3, SHARP_3, true), true));
        assertTrue(LockRules.anvilBlocked(attempt(false, true, Map.of(), SHARP_3, false), true));
    }

    @Test
    void enchantmentComparisonIgnoresOrder() {
        Map<String, Integer> oneWay = new java.util.LinkedHashMap<>();
        oneWay.put("minecraft:sharpness", 3);
        oneWay.put("minecraft:unbreaking", 1);
        Map<String, Integer> otherWay = new java.util.LinkedHashMap<>();
        otherWay.put("minecraft:unbreaking", 1);
        otherWay.put("minecraft:sharpness", 3);
        assertTrue(LockRules.enchantmentsUnchanged(oneWay, otherWay));
        assertFalse(LockRules.enchantmentsUnchanged(SHARP_3, SHARP_4));
    }

    @Test
    void oneLockedInputIsEnoughToBlockStripping() {
        assertTrue(LockRules.strippingBlocked(true, false));
        assertTrue(LockRules.strippingBlocked(false, true));
        assertFalse(LockRules.strippingBlocked(false, false));
    }
}
