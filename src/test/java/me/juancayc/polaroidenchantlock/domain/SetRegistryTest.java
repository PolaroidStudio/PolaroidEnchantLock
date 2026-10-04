package me.juancayc.polaroidenchantlock.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SetRegistryTest {

    private static LockedSet set(String name, String... pieces) {
        return new LockedSet(name, List.of(pieces));
    }

    @Test
    void anEmptyRegistryLocksNothing() {
        SetRegistry registry = new SetRegistry();
        assertTrue(registry.isEmpty());
        assertFalse(registry.isLocked("nexo:ruby"));
        assertFalse(registry.isLocked(null));
    }

    @Test
    void aPieceOfAnySetIsLocked() {
        SetRegistry registry = new SetRegistry();
        registry.add(set("Dragon", "nexo:dragon_helmet", "mythicmobs:DragonChestplate"));
        assertTrue(registry.isLocked("nexo:dragon_helmet"));
        assertTrue(registry.isLocked("mythicmobs:DragonChestplate"));
        assertFalse(registry.isLocked("nexo:other"));
        // MythicMobs ids are case-sensitive, so the lookup is too.
        assertFalse(registry.isLocked("mythicmobs:dragonchestplate"));
    }

    @Test
    void namesAreUniqueIgnoringCase() {
        SetRegistry registry = new SetRegistry();
        assertTrue(registry.add(set("Dragon", "nexo:a")));
        assertFalse(registry.add(set("dragon", "nexo:b")));
        assertTrue(registry.contains("DRAGON"));
        assertFalse(registry.isLocked("nexo:b"));
        assertEquals(1, registry.sets().size());
    }

    @Test
    void aPieceSharedByTwoSetsStaysLockedUntilBothAreGone() {
        SetRegistry registry = new SetRegistry();
        registry.add(set("One", "nexo:shared", "nexo:only_one"));
        registry.add(set("Two", "nexo:shared"));

        assertTrue(registry.remove("One"));
        assertTrue(registry.isLocked("nexo:shared"));
        assertFalse(registry.isLocked("nexo:only_one"));

        assertTrue(registry.remove("two"));
        assertFalse(registry.isLocked("nexo:shared"));
        assertTrue(registry.isEmpty());
    }

    @Test
    void removingAnUnknownSetChangesNothing() {
        SetRegistry registry = new SetRegistry();
        registry.add(set("Dragon", "nexo:a"));
        assertFalse(registry.remove("Phoenix"));
        assertTrue(registry.isLocked("nexo:a"));
    }

    @Test
    void updateReplacesThePiecesAndKeepsNameAndPosition() {
        SetRegistry registry = new SetRegistry();
        registry.add(set("Dragon", "nexo:a"));
        registry.add(set("Phoenix", "nexo:p"));

        assertTrue(registry.update("dragon", List.of("nexo:b", "nexo:c")));

        assertFalse(registry.isLocked("nexo:a"));
        assertTrue(registry.isLocked("nexo:b"));
        assertEquals(List.of("Dragon", "Phoenix"), registry.sets().stream().map(LockedSet::name).toList());
        assertEquals(List.of("nexo:b", "nexo:c"), registry.find("Dragon").orElseThrow().pieces());
        assertFalse(registry.update("Missing", List.of("nexo:x")));
    }

    @Test
    void replaceAllDropsWhatWasThereAndALaterDuplicateName() {
        SetRegistry registry = new SetRegistry();
        registry.add(set("Old", "nexo:old"));

        registry.replaceAll(List.of(set("Dragon", "nexo:a"), set("DRAGON", "nexo:b")));

        assertFalse(registry.isLocked("nexo:old"));
        assertTrue(registry.isLocked("nexo:a"));
        assertFalse(registry.isLocked("nexo:b"));
        assertEquals(1, registry.sets().size());
    }

    @Test
    void aSetHoldsEachPieceOnceInTheOrderAdded() {
        LockedSet set = set("Dragon", "nexo:b", "nexo:a", "nexo:b");
        assertEquals(List.of("nexo:b", "nexo:a"), set.pieces());
        assertEquals(2, set.size());
    }
}
