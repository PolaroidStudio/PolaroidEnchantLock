package me.juancayc.polaroidenchantlock.domain;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * The identity of a custom item as an item plugin knows it: {@code nexo:ruby_helmet},
 * {@code mythicmobs:DragonChestplate}.
 *
 * <p>A locked piece is stored and compared as this string, never as an {@code ItemStack}: the
 * lock then survives a rename, a lore change, a restyle or any enchantment the piece already
 * carries. The prefix is lower-cased; the id is kept exactly as the item plugin spells it, since
 * MythicMobs ids are case-sensitive.
 */
public record ItemReference(String prefix, String id) {

    /** The item plugins a piece can come from. An item none of them claims is never lockable. */
    public static final Set<String> PREFIXES = Set.of("nexo", "mythicmobs");

    /** The reference built from a hook's own prefix and the id it reported for an item. */
    public static ItemReference of(String prefix, String id) {
        return new ItemReference(prefix.toLowerCase(Locale.ROOT), id);
    }

    /**
     * Reads a stored reference. Both separators are accepted — {@code nexo:id} and {@code nexo-id}
     * — because the rest of the Polaroid line writes item references either way; the id keeps any
     * further separator it contains.
     *
     * @return empty when the text has no known prefix, no id, or whitespace inside the id
     */
    public static Optional<ItemReference> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String text = raw.trim();
        for (String prefix : PREFIXES) {
            if (text.length() <= prefix.length() + 1 || !text.regionMatches(true, 0, prefix, 0, prefix.length())) {
                continue;
            }
            char separator = text.charAt(prefix.length());
            if (separator != ':' && separator != '-') {
                continue;
            }
            String id = text.substring(prefix.length() + 1);
            if (id.isBlank() || id.chars().anyMatch(Character::isWhitespace)) {
                return Optional.empty();
            }
            return Optional.of(new ItemReference(prefix, id));
        }
        return Optional.empty();
    }

    /** The canonical stored form, always with a colon: {@code nexo:ruby_helmet}. */
    public String asString() {
        return prefix + ":" + id;
    }

    @Override
    public String toString() {
        return asString();
    }
}
