package me.juancayc.polaroidenchantlock.domain;

import me.juancayc.polaroidenchantlock.domain.SetNameRules.Result;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SetNameRulesTest {

    private static Result check(String raw) {
        return check(raw, 24);
    }

    private static Result check(String raw, int max) {
        Set<String> taken = Set.of("dragon");
        return SetNameRules.check(SetNameRules.normalize(raw), max, name -> taken.contains(name.toLowerCase()));
    }

    @Test
    void acceptsLettersDigitsSpacesUnderscoresAndHyphens() {
        assertEquals(Result.OK, check("Phoenix"));
        assertEquals(Result.OK, check("Tier 2 - void_set"));
    }

    @Test
    void normalizeTrimsAndCollapsesWhitespace() {
        assertEquals("Void Set", SetNameRules.normalize("  Void \t  Set "));
        assertEquals("", SetNameRules.normalize(null));
    }

    @Test
    void anEmptyOrBlankNameIsEmpty() {
        assertEquals(Result.EMPTY, check(""));
        assertEquals(Result.EMPTY, check("    "));
        assertEquals(Result.EMPTY, check(null));
    }

    @Test
    void theLengthCapCountsTheNormalizedName() {
        assertEquals(Result.OK, check("abcde", 5));
        assertEquals(Result.TOO_LONG, check("abcdef", 5));
        assertEquals(Result.OK, check("  abcde  ", 5));
    }

    @Test
    void theConfiguredCapCannotExceedTheHardCap() {
        String tooLong = "a".repeat(SetNameRules.HARD_MAX_LENGTH + 1);
        assertEquals(Result.TOO_LONG, check(tooLong, 500));
        assertEquals(Result.OK, check("a".repeat(SetNameRules.HARD_MAX_LENGTH), 500));
        assertEquals(1, SetNameRules.clampMaxLength(0));
    }

    @Test
    void rejectsMiniMessageYamlAndPathCharacters() {
        assertEquals(Result.BAD_CHARACTERS, check("<red>Dragon"));
        assertEquals(Result.BAD_CHARACTERS, check("<click:run_command:/op me>x", 32));
        assertEquals(Result.BAD_CHARACTERS, check("a.b"));
        assertEquals(Result.BAD_CHARACTERS, check("a:b"));
        assertEquals(Result.BAD_CHARACTERS, check("§cDragon"));
        assertEquals(Result.BAD_CHARACTERS, check("Dragón"));
    }

    @Test
    void aTakenNameIsADuplicateWhateverItsCase() {
        assertEquals(Result.DUPLICATE, check("Dragon"));
        assertEquals(Result.DUPLICATE, check("  DRAGON "));
    }
}
