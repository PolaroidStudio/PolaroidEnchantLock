package me.juancayc.polaroidenchantlock.storage;

import me.juancayc.polaroidenchantlock.domain.LockedSet;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** sets.yml through Bukkit's own YAML classes, which need no running server. */
class SetStoreTest {

    /** A logger that keeps what it is told instead of printing it. */
    private static final class Captured {
        final List<String> warnings = new ArrayList<>();
        final Logger logger = Logger.getAnonymousLogger();

        Captured() {
            logger.setUseParentHandlers(false);
            logger.addHandler(new Handler() {
                @Override
                public void publish(LogRecord record) {
                    warnings.add(record.getMessage());
                }

                @Override
                public void flush() {
                }

                @Override
                public void close() {
                }
            });
        }
    }

    private static List<LockedSet> read(String yamlText, Captured log) throws InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(yamlText);
        return SetStore.read(yaml, log.logger);
    }

    @Test
    void aMissingFileIsNoSets(@TempDir Path folder) {
        SetStore store = new SetStore(folder.resolve("sets.yml").toFile(), new Captured().logger);
        assertTrue(store.load().isEmpty());
    }

    @Test
    void whatIsSavedIsWhatIsLoaded(@TempDir Path folder) throws IOException {
        Captured log = new Captured();
        // A folder that does not exist yet, as on a first save.
        SetStore store = new SetStore(folder.resolve("data").resolve("sets.yml").toFile(), log.logger);
        List<LockedSet> sets = List.of(
                new LockedSet("Dragon", List.of("nexo:dragon_helmet", "mythicmobs:DragonChestplate")),
                new LockedSet("Tier 2 - void_set", List.of("nexo:void_boots")));

        store.save(sets);

        assertEquals(sets, store.load());
        assertTrue(log.warnings.isEmpty());
    }

    @Test
    void savingReplacesTheFileAndLeavesNoTemporaryFileBehind(@TempDir Path folder) throws IOException {
        SetStore store = new SetStore(folder.resolve("sets.yml").toFile(), new Captured().logger);
        store.save(List.of(new LockedSet("One", List.of("nexo:a")), new LockedSet("Two", List.of("nexo:b"))));
        store.save(List.of(new LockedSet("Two", List.of("nexo:b"))));

        assertEquals(List.of(new LockedSet("Two", List.of("nexo:b"))), store.load());
        try (var files = Files.list(folder)) {
            assertEquals(List.of("sets.yml"), files.map(path -> path.getFileName().toString()).toList());
        }
    }

    @Test
    void savingNoSetsStillWritesAReadableFile(@TempDir Path folder) throws IOException {
        SetStore store = new SetStore(folder.resolve("sets.yml").toFile(), new Captured().logger);
        store.save(List.of());
        assertTrue(store.load().isEmpty());
        assertTrue(Files.readString(folder.resolve("sets.yml")).contains("sets:"));
    }

    @Test
    void handWrittenReferencesAreNormalised() throws InvalidConfigurationException {
        Captured log = new Captured();
        List<LockedSet> sets = read("""
                sets:
                  Dragon:
                    - nexo-dragon_helmet
                    - "  MythicMobs:DragonChestplate "
                    - nexo:dragon_helmet
                """, log);
        assertEquals(List.of(new LockedSet("Dragon", List.of("nexo:dragon_helmet", "mythicmobs:DragonChestplate"))), sets);
        assertTrue(log.warnings.isEmpty());
    }

    @Test
    void aBadReferenceIsSkippedWithAWarningAndTheRestLoads() throws InvalidConfigurationException {
        Captured log = new Captured();
        List<LockedSet> sets = read("""
                sets:
                  Dragon:
                    - DIAMOND_SWORD
                    - nexo:dragon_helmet
                """, log);
        assertEquals(List.of(new LockedSet("Dragon", List.of("nexo:dragon_helmet"))), sets);
        assertEquals(1, log.warnings.size());
        assertTrue(log.warnings.get(0).contains("DIAMOND_SWORD"));
    }

    @Test
    void aBadOrRepeatedNameIsSkippedWithAWarningAndTheRestLoads() throws InvalidConfigurationException {
        Captured log = new Captured();
        List<LockedSet> sets = read("""
                sets:
                  "<red>Evil":
                    - nexo:a
                  Dragon:
                    - nexo:b
                  dragon:
                    - nexo:c
                """, log);
        assertEquals(List.of(new LockedSet("Dragon", List.of("nexo:b"))), sets);
        assertEquals(2, log.warnings.size());
    }

    @Test
    void aFileWithoutTheSetsSectionIsNoSets() throws InvalidConfigurationException {
        assertTrue(read("something-else: true\n", new Captured()).isEmpty());
        assertFalse(SetStore.write(List.of()).saveToString().isBlank());
    }
}
