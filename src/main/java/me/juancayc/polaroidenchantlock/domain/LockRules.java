package me.juancayc.polaroidenchantlock.domain;

import java.util.Map;

/**
 * Decides whether an attempt to change an item is refused. No Bukkit type appears here: the
 * listeners describe what is on the table in plain values and this class answers.
 */
public final class LockRules {

    /**
     * What an anvil is about to do.
     *
     * @param baseLocked     the item in the left slot is a locked piece
     * @param additionLocked the item in the right slot is a locked piece
     * @param before         the enchantments (key to level) of the left item
     * @param after          the enchantments (key to level) of the result
     * @param restUnchanged  apart from enchantments, durability, name and prior-work cost, the
     *                       result is the left item — nothing else about it was rewritten. This
     *                       is what catches enchantment plugins that keep their data outside the
     *                       vanilla enchantment list
     */
    public record AnvilAttempt(boolean baseLocked, boolean additionLocked,
                               Map<String, Integer> before, Map<String, Integer> after,
                               boolean restUnchanged) {
    }

    private LockRules() {
    }

    /** Same enchantments at the same levels; order does not matter. */
    public static boolean enchantmentsUnchanged(Map<String, Integer> before, Map<String, Integer> after) {
        return before.equals(after);
    }

    /**
     * Whether the anvil result must be refused.
     *
     * <ul>
     *   <li>No locked piece involved: never refused.</li>
     *   <li>A locked piece on the left: refused unless the result is a pure repair or rename
     *       <em>and</em> those are allowed in the configuration.</li>
     *   <li>A locked piece only on the right: always refused. The anvil would consume it, and
     *       with it move its enchantments onto another item.</li>
     * </ul>
     */
    public static boolean anvilBlocked(AnvilAttempt attempt, boolean allowRepairAndRename) {
        if (!attempt.baseLocked() && !attempt.additionLocked()) {
            return false;
        }
        if (!attempt.baseLocked()) {
            return true;
        }
        boolean repairOrRename = attempt.restUnchanged()
                && enchantmentsUnchanged(attempt.before(), attempt.after());
        return !(repairOrRename && allowRepairAndRename);
    }

    /**
     * Grindstone and crafting-grid repair: both strip enchantments from whatever goes in, so one
     * locked input is enough to refuse.
     */
    public static boolean strippingBlocked(boolean firstLocked, boolean secondLocked) {
        return firstLocked || secondLocked;
    }
}
