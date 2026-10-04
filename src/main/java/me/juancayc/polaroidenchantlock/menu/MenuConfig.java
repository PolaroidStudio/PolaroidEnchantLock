package me.juancayc.polaroidenchantlock.menu;

import me.juancayc.polaroidenchantlock.domain.Placeholders;
import me.juancayc.polaroidenchantlock.messaging.MessageService;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@code menus.yml}: every title, button and lore line of the three menus, and the one click
 * sound they share. Slots are fixed in code; how things look is the owner's.
 *
 * <p>This is the single place a menu item is built, so it is also the single place it is marked
 * ({@link MenuItemMarker}): nothing a menu shows can be created without the mark.
 */
public final class MenuConfig {

    private static final String FILE = "menus.yml";

    private final JavaPlugin plugin;
    private final MessageService messages;
    private final MenuItemMarker marker;
    private volatile YamlConfiguration yaml;
    private volatile @Nullable Sound clickSound;

    public MenuConfig(JavaPlugin plugin, MessageService messages, MenuItemMarker marker) {
        this.plugin = plugin;
        this.messages = messages;
        this.marker = marker;
    }

    /** Loads (or reloads) menus.yml; keys missing on disk fall back to the bundled file. */
    public void reload() {
        File file = new File(plugin.getDataFolder(), FILE);
        if (!file.exists()) {
            plugin.saveResource(FILE, false);
        }
        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(file);
        try (InputStream in = plugin.getResource(FILE)) {
            if (in != null) {
                loaded.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8)));
            }
        } catch (IOException ignored) {
            // Falls back to whatever the file on disk already has.
        }
        this.yaml = loaded;
        this.clickSound = readSound(loaded);
    }

    private @Nullable Sound readSound(YamlConfiguration source) {
        String name = source.getString("click-sound.name", "");
        if (name.isBlank()) {
            return null;
        }
        try {
            return Sound.sound(Key.key(name), Sound.Source.MASTER,
                    (float) source.getDouble("click-sound.volume", 0.6),
                    (float) source.getDouble("click-sound.pitch", 1.0));
        } catch (RuntimeException invalidKey) {
            plugin.getLogger().warning("menus.yml: click-sound.name '" + name + "' is not a valid sound key; no sound is played.");
            return null;
        }
    }

    /** The one click sound of the whole plugin. Played only for a click that did something. */
    public void playClick(Player player) {
        Sound sound = clickSound;
        if (sound != null) {
            player.playSound(sound);
        }
    }

    /** A window title. Values are substituted as MiniMessage source, so escape anything typed. */
    public Component title(String path, String... replacements) {
        return messages.render(Placeholders.apply(yaml.getString(path, ""), MessageService.toMap(replacements)));
    }

    /**
     * A button from its {@code material / name / lore / hide-tooltip} section.
     *
     * @param icon when given, a copy of this item is used instead of {@code material}, with its
     *             name and lore replaced — how a set is drawn as its first piece
     */
    public ItemStack item(String path, @Nullable ItemStack icon, String... replacements) {
        Map<String, String> values = MessageService.toMap(replacements);
        ConfigurationSection section = yaml.getConfigurationSection(path);
        ItemStack item = icon != null ? icon.asOne() : new ItemStack(material(section, path));
        ItemMeta meta = item.getItemMeta();
        if (meta == null || section == null) {
            return marker.mark(item);
        }
        if (section.getBoolean("hide-tooltip", false)) {
            meta.setHideTooltip(true);
        }
        String name = section.getString("name");
        if (name != null) {
            meta.displayName(line(name, values));
        }
        meta.lore(lines(section.getStringList("lore"), values));
        item.setItemMeta(meta);
        return marker.mark(item);
    }

    /**
     * A copy of a real item with the section's {@code lore} added under its own. The item keeps
     * its name and look — it is a piece being shown, not a button.
     */
    public ItemStack decorate(ItemStack base, String path, String... replacements) {
        ItemStack item = base.asOne();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return marker.mark(item);
        }
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        lore.addAll(lines(yaml.getStringList(path + ".lore"), MessageService.toMap(replacements)));
        meta.lore(lore);
        item.setItemMeta(meta);
        return marker.mark(item);
    }

    private Material material(@Nullable ConfigurationSection section, String path) {
        String name = section == null ? null : section.getString("material");
        Material material = name == null ? null : Material.matchMaterial(name);
        if (material == null || !material.isItem()) {
            plugin.getLogger().warning("menus.yml: " + path + ".material '" + name + "' is not an item; using PAPER.");
            return Material.PAPER;
        }
        return material;
    }

    private List<Component> lines(List<String> raw, Map<String, String> values) {
        List<Component> out = new ArrayList<>(raw.size());
        for (String entry : raw) {
            out.add(line(entry, values));
        }
        return out;
    }

    /** Item text is italic unless told otherwise; a menu label never should be. */
    private Component line(String raw, Map<String, String> values) {
        return messages.render(Placeholders.apply(raw, values))
                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }
}
