package me.juancayc.polaroidenchantlock.domain;

import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * What a set may be called. The name is typed by an admin into a dialog, becomes a key in
 * {@code sets.yml} and is shown in menus and chat, so it is held to a small alphabet: letters,
 * digits, spaces, underscores and hyphens. That keeps it a valid YAML key (no dots, no colons)
 * and leaves nothing MiniMessage could read as a tag — the name is escaped before rendering all
 * the same, so the two protections do not depend on each other.
 */
public final class SetNameRules {

    /** The longest name any configuration may allow; {@code sets.name-max-length} is capped to it. */
    public static final int HARD_MAX_LENGTH = 32;

    private static final Pattern ALLOWED = Pattern.compile("^[A-Za-z0-9 _-]+$");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    public enum Result {
        OK,
        EMPTY,
        TOO_LONG,
        BAD_CHARACTERS,
        DUPLICATE
    }

    private SetNameRules() {
    }

    /** Trims the name and collapses every run of whitespace to one space. {@code null} becomes empty. */
    public static String normalize(String raw) {
        return raw == null ? "" : SPACES.matcher(raw.trim()).replaceAll(" ");
    }

    /**
     * Judges a name that has already been through {@link #normalize}.
     *
     * @param maxLength the configured cap; values outside {@code 1..HARD_MAX_LENGTH} are clamped
     * @param taken     whether a set with this name already exists (ignoring case)
     */
    public static Result check(String name, int maxLength, Predicate<String> taken) {
        if (name == null || name.isEmpty()) {
            return Result.EMPTY;
        }
        if (name.length() > clampMaxLength(maxLength)) {
            return Result.TOO_LONG;
        }
        if (!ALLOWED.matcher(name).matches()) {
            return Result.BAD_CHARACTERS;
        }
        if (taken.test(name)) {
            return Result.DUPLICATE;
        }
        return Result.OK;
    }

    public static int clampMaxLength(int configured) {
        return Math.max(1, Math.min(HARD_MAX_LENGTH, configured));
    }
}
