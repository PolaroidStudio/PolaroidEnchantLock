package me.juancayc.polaroidenchantlock.storage;

import me.juancayc.polaroidenchantlock.domain.ItemReference;
import me.juancayc.polaroidenchantlock.domain.LockedSet;
import me.juancayc.polaroidenchantlock.domain.SetNameRules;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

/**
 * {@code sets.yml}: every locked set as {@code name -> list of item references}.
 *
 * <pre>
 * sets:
 *   Dragon:
 *     - nexo:dragon_helmet
 *     - mythicmobs:DragonChestplate
 * </pre>
 *
 * <p>The file is small and written only when an admin saves or deletes a set, so it is written
 * whole, to a temporary file that then replaces the real one: a crash mid-write leaves the old
 * file, never half of the new one.
 *
 * <p>Reading is tolerant. The file may be edited by hand, so a bad name, a bad reference or a
 * repeated name is skipped with a warning naming it, and the rest of the file still loads.
 */
public final class SetStore {

    private static final String ROOT = "sets";
    private static final List<String> HEADER = List.of(
            "PolaroidEnchantLock — locked sets.",
            "",
            "Written by the in-game menu (/enchantlock). It can be edited by hand too: each set is a",
            "name and the list of item ids it locks, as nexo:<id> or mythicmobs:<id>.",
            "Run /enchantlock reload after editing.");

    private final File file;
    private final Logger logger;

    public SetStore(File file, Logger logger) {
        this.file = file;
        this.logger = logger;
    }

    /** Every valid set in the file, in file order. A missing file is simply no sets. */
    public List<LockedSet> load() {
        if (!file.exists()) {
            return List.of();
        }
        return read(YamlConfiguration.loadConfiguration(file), logger);
    }

    /** Replaces the file with exactly these sets. */
    public void save(Collection<LockedSet> sets) throws IOException {
        Path target = file.toPath();
        Files.createDirectories(target.toAbsolutePath().getParent());
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(temporary, write(sets).saveToString(), StandardCharsets.UTF_8);
        try {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Parses an already-loaded YAML document. Split from {@link #load} so it runs without a disk. */
    public static List<LockedSet> read(YamlConfiguration yaml, Logger logger) {
        ConfigurationSection section = yaml.getConfigurationSection(ROOT);
        if (section == null) {
            return List.of();
        }
        List<LockedSet> sets = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String key : section.getKeys(false)) {
            String name = SetNameRules.normalize(key);
            SetNameRules.Result verdict = SetNameRules.check(name, SetNameRules.HARD_MAX_LENGTH,
                    candidate -> seen.contains(candidate.toLowerCase(Locale.ROOT)));
            if (verdict != SetNameRules.Result.OK) {
                logger.warning("sets.yml: set '" + key + "' skipped (" + verdict + ").");
                continue;
            }
            List<String> pieces = new ArrayList<>();
            for (String raw : section.getStringList(key)) {
                Optional<ItemReference> reference = ItemReference.parse(raw);
                if (reference.isEmpty()) {
                    logger.warning("sets.yml: '" + raw + "' in set '" + key
                            + "' is not nexo:<id> or mythicmobs:<id>, skipped.");
                    continue;
                }
                pieces.add(reference.get().asString());
            }
            seen.add(name.toLowerCase(Locale.ROOT));
            sets.add(new LockedSet(name, pieces));
        }
        return sets;
    }

    /** The YAML document for these sets, header included. */
    public static YamlConfiguration write(Collection<LockedSet> sets) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.options().setHeader(HEADER);
        // An empty section is written explicitly so the file still shows where sets go.
        ConfigurationSection section = yaml.createSection(ROOT);
        for (LockedSet set : sets) {
            section.set(set.name(), new ArrayList<>(set.pieces()));
        }
        return yaml;
    }
}
