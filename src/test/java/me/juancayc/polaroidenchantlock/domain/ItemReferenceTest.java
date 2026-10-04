package me.juancayc.polaroidenchantlock.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemReferenceTest {

    @Test
    void parsesBothSeparatorsToTheSameCanonicalForm() {
        assertEquals("nexo:ruby_helmet", ItemReference.parse("nexo:ruby_helmet").orElseThrow().asString());
        assertEquals("nexo:ruby_helmet", ItemReference.parse("nexo-ruby_helmet").orElseThrow().asString());
    }

    @Test
    void prefixIsCaseInsensitiveButTheIdKeepsItsCase() {
        ItemReference reference = ItemReference.parse("MythicMobs:DragonChestplate").orElseThrow();
        assertEquals("mythicmobs", reference.prefix());
        assertEquals("DragonChestplate", reference.id());
    }

    @Test
    void theIdKeepsAnyFurtherSeparator() {
        assertEquals("pack:ruby-helmet", ItemReference.parse("nexo:pack:ruby-helmet").orElseThrow().id());
    }

    @Test
    void surroundingWhitespaceIsIgnored() {
        assertEquals("nexo:ruby", ItemReference.parse("  nexo:ruby  ").orElseThrow().asString());
    }

    @Test
    void rejectsWhatIsNotAKnownPluginItem() {
        assertTrue(ItemReference.parse(null).isEmpty());
        assertTrue(ItemReference.parse("").isEmpty());
        assertTrue(ItemReference.parse("DIAMOND_SWORD").isEmpty());
        assertTrue(ItemReference.parse("minecraft:diamond_sword").isEmpty());
        assertTrue(ItemReference.parse("oraxen:ruby").isEmpty());
        // A prefix that merely starts like a known one is not that prefix.
        assertTrue(ItemReference.parse("nexoruby").isEmpty());
    }

    @Test
    void rejectsAMissingOrBrokenId() {
        assertTrue(ItemReference.parse("nexo:").isEmpty());
        assertTrue(ItemReference.parse("nexo").isEmpty());
        assertTrue(ItemReference.parse("nexo:ruby helmet").isEmpty());
    }

    @Test
    void buildsFromAHookPrefixAndId() {
        assertEquals("nexo:Ruby", ItemReference.of("Nexo", "Ruby").asString());
    }
}
