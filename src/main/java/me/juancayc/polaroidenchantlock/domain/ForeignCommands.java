package me.juancayc.polaroidenchantlock.domain;

import java.util.Locale;
import java.util.Set;

/**
 * Recognises the commands of other plugins that write enchantments straight onto the item a
 * player is holding, from the raw command line alone.
 */
public final class ForeignCommands {

    /** PolaroidEnchant's command and its alias, as its plugin.yml declares them. */
    private static final Set<String> PENCHANT_LABELS = Set.of("penchant", "pe");
    private static final String PENCHANT_NAMESPACE = "polaroidenchant:";
    /** The two subcommands that change the held item: {@code enchant <player> ...}, {@code remove <player> ...}. */
    private static final Set<String> PENCHANT_WRITES = Set.of("enchant", "remove");

    private ForeignCommands() {
    }

    /**
     * The player whose held item {@code /penchant enchant <player> <enchant> <level>} or
     * {@code /penchant remove <player> <enchant>} would change.
     *
     * @param commandLine the command as typed, with or without the leading slash
     * @return the player name exactly as typed, or null when the line is any other command
     */
    public static String penchantTarget(String commandLine) {
        if (commandLine == null) {
            return null;
        }
        String line = commandLine.strip();
        if (line.startsWith("/")) {
            line = line.substring(1);
        }
        String[] parts = line.split("\\s+");
        if (parts.length < 3) {
            return null;
        }
        String label = parts[0].toLowerCase(Locale.ROOT);
        if (label.startsWith(PENCHANT_NAMESPACE)) {
            label = label.substring(PENCHANT_NAMESPACE.length());
        }
        if (!PENCHANT_LABELS.contains(label) || !PENCHANT_WRITES.contains(parts[1].toLowerCase(Locale.ROOT))) {
            return null;
        }
        return parts[2];
    }
}
