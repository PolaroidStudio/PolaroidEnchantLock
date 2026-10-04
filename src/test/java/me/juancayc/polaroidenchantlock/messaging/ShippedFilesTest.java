package me.juancayc.polaroidenchantlock.messaging;

import me.juancayc.polaroidenchantlock.config.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The files shipped in the jar, checked against what the code asks of them: a key the code
 * reads but the file lacks would otherwise only show up in game, as "Missing message key".
 */
class ShippedFilesTest {

    /** Every lang key some class passes to MessageService. */
    private static final List<String> MESSAGE_KEYS = List.of(
            "prefix",
            "blocked.enchanting_table", "blocked.anvil", "blocked.grindstone", "blocked.smithing_table",
            "blocked.crafting", "blocked.menu", "blocked.command",
            "command.players_only", "command.no_permission", "command.reloaded",
            "editor.not_custom", "editor.already_added", "editor.empty",
            "set.created", "set.updated", "set.deleted",
            "error.save_failed",
            "dialog.title", "dialog.body", "dialog.input_label", "dialog.save", "dialog.cancel",
            "name.empty", "name.too_long", "name.bad_characters", "name.duplicate");

    /** Every menus.yml section drawn as an item with its own material. */
    private static final List<String> MENU_ITEMS = List.of(
            "fill", "buttons.back", "buttons.previous", "buttons.next", "buttons.close",
            "main.set", "main.create", "main.empty",
            "set.missing-piece", "set.edit", "set.delete", "set.delete-confirm",
            "editor.save");

    private static YamlConfiguration resource(String path) throws IOException {
        try (InputStream in = ShippedFilesTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in, path + " is not on the classpath");
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
    }

    @Test
    void theEnglishFileHasEveryKeyTheCodeUses() throws IOException {
        YamlConfiguration lang = resource("lang/messages_en.yml");
        for (String key : MESSAGE_KEYS) {
            assertTrue(lang.isString(key), "lang/messages_en.yml is missing " + key);
        }
    }

    @Test
    void noMessageTypesThePrefixTagAndEveryMessageParses() throws IOException {
        YamlConfiguration lang = resource("lang/messages_en.yml");
        MiniMessage mm = MiniMessage.miniMessage();
        for (String key : lang.getKeys(true)) {
            if (!lang.isString(key)) {
                continue;
            }
            String value = lang.getString(key);
            // POLAROID-STYLE section 3: the prefix is composed in code, never typed in a value.
            assertFalse(value.contains("<prefix>"), key + " contains a literal <prefix>");
            String plain = PlainTextComponentSerializer.plainText().serialize(mm.deserialize(value));
            // A tag MiniMessage did not understand is left in the output as text.
            assertFalse(plain.contains("<") || plain.contains(">"), key + " has an unparsed tag: " + plain);
        }
    }

    @Test
    void thePrefixIsThePolaroidBannerWithThisPluginsName() throws IOException {
        Component prefix = MiniMessage.miniMessage().deserialize(resource("lang/messages_en.yml").getString("prefix"));
        assertEquals("POLAROID ENCHANTLOCK ┇ ", PlainTextComponentSerializer.plainText().serialize(prefix));
    }

    @Test
    void everyMenuItemHasARealMaterialAndEveryTitleExists() throws IOException {
        YamlConfiguration menus = resource("menus.yml");
        for (String path : MENU_ITEMS) {
            ConfigurationSection section = menus.getConfigurationSection(path);
            assertNotNull(section, "menus.yml is missing " + path);
            assertNotNull(Material.matchMaterial(section.getString("material", "")), path + ".material is not a material");
        }
        for (String title : List.of("main.title", "set.title", "editor.title-new", "editor.title-edit")) {
            assertTrue(menus.isString(title), "menus.yml is missing " + title);
        }
        for (String lore : List.of("set.piece.lore", "editor.piece.lore")) {
            assertFalse(menus.getStringList(lore).isEmpty(), "menus.yml is missing " + lore);
        }
        // The filler must hide its tooltip, or an empty box trails the cursor.
        assertTrue(menus.getBoolean("fill.hide-tooltip"));
    }

    @Test
    void theShippedConfigReadsToTheDocumentedDefaults() throws IOException {
        Settings settings = Settings.read(resource("config.yml"));
        assertEquals("en", settings.language());
        assertTrue(settings.allowRepairAndRename());
        assertEquals(1500L, settings.messageCooldownMillis());
        assertEquals(24, settings.nameMaxLength());
        assertEquals(300L, settings.dialogLifetimeSeconds());
        assertTrue(settings.hookPolaroidEnchant());
        assertTrue(settings.hookPolaroidEnchanter());
    }

    @Test
    void outOfRangeSettingsAreClamped() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("sets.name-max-length", 900);
        config.set("sets.name-dialog-lifetime-seconds", 1);
        config.set("messages.cooldown-milliseconds", -5);
        Settings settings = Settings.read(config);
        assertEquals(32, settings.nameMaxLength());
        assertEquals(30L, settings.dialogLifetimeSeconds());
        assertEquals(0L, settings.messageCooldownMillis());
    }
}
