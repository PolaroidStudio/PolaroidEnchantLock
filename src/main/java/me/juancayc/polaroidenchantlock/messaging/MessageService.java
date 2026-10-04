package me.juancayc.polaroidenchantlock.messaging;

import me.juancayc.polaroidenchantlock.domain.Placeholders;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads {@code lang/messages_<language>.yml} (with English fallback via {@code setDefaults}) and
 * builds/sends localized components.
 *
 * <p>Every key is either a plain MiniMessage string or a {@code {text, hover, click}} section.
 * The Polaroid prefix lives in its own top-level {@code prefix:} key and is composed onto a
 * message IN CODE by {@link #sendPrefixed} — no message value may contain a literal
 * {@code <prefix>} tag (see POLAROID-STYLE.md section 3). Dialog text and list rows are built
 * with {@link #build} and never carry the prefix.
 *
 * <p><b>Placeholders are {@code %token%} and are substituted before deserializing</b>, in a single
 * pass ({@link Placeholders}). A substituted value therefore becomes MiniMessage source: anything
 * a person typed — in this plugin, a set name — must be passed through {@link #escape} first, so
 * a name can never colour or click-enable somebody else's chat.
 */
public final class MessageService {

    /** The languages bundled in the jar. English first: it is the fallback for any other. */
    public static final List<String> SHIPPED_LANGUAGES = List.of("en");

    private final JavaPlugin plugin;
    private final MiniMessage mm;
    private volatile YamlConfiguration messages;

    public MessageService(JavaPlugin plugin, MiniMessage mm) {
        this.plugin = plugin;
        this.mm = mm;
    }

    /** Loads (or reloads) the active language file. Cheap; safe to call from /enchantlock reload. */
    public void reload(String language) {
        for (String shipped : SHIPPED_LANGUAGES) {
            saveResourceIfMissing("lang/messages_" + shipped + ".yml");
        }
        // A language the jar does not ship is one the owner wrote: lang/messages_<language>.yml
        // in the data folder. When it is not there either, English is used.
        File file = new File(plugin.getDataFolder(), "lang/messages_" + language + ".yml");
        if (!file.exists()) file = new File(plugin.getDataFolder(), "lang/messages_en.yml");

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        try (InputStream in = plugin.getResource("lang/messages_en.yml")) {
            if (in != null) {
                cfg.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8)));
            }
        } catch (IOException ignored) {
            // Falls back to whatever the file on disk already has.
        }
        this.messages = cfg;
    }

    private void saveResourceIfMissing(String path) {
        if (plugin.getResource(path) != null && !new File(plugin.getDataFolder(), path).exists()) {
            plugin.saveResource(path, false);
        }
    }

    /** The Polaroid prefix component, built fresh from the current lang file each call. */
    public Component prefix() {
        return mm.deserialize(messages.getString("prefix", ""));
    }

    /** Escapes MiniMessage tags in an untrusted value so it can only ever render as text. */
    public String escape(String untrusted) {
        return untrusted == null ? "" : mm.escapeTags(untrusted);
    }

    /** Deserializes server-authored MiniMessage. */
    public Component render(String miniMessage) {
        return mm.deserialize(miniMessage == null ? "" : miniMessage);
    }

    /** Builds a component from a message key, applying {@code %token%}/value replacement pairs. */
    public Component build(String key, String... replacements) {
        Map<String, String> values = toMap(replacements);
        ConfigurationSection sec = messages.getConfigurationSection(key);

        String text;
        String hover = null;
        String click = null;

        if (sec != null) {
            text = sec.getString("text", "");
            hover = sec.getString("hover", null);
            click = sec.getString("click", null);
        } else {
            text = messages.getString(key, "<red>Missing message key: " + key);
        }

        Component component = render(Placeholders.apply(text, values));
        if (hover != null && !hover.isBlank()) {
            component = component.hoverEvent(HoverEvent.showText(render(Placeholders.apply(hover, values))));
        }
        if (click != null && !click.isBlank()) {
            component = ClickActionParser.apply(component, Placeholders.apply(click, values), "");
        }
        return component;
    }

    /** Standalone result message: prefixed. Players get hover/click, console gets plain text. */
    public void sendPrefixed(CommandSender sender, String key, String... replacements) {
        deliver(sender, prefix().append(build(key, replacements)));
    }

    /** An already-built component, with the same player/console split as every other send. */
    public void deliver(CommandSender sender, Component component) {
        if (sender instanceof Player player) {
            player.sendMessage(component);
        } else {
            sender.sendMessage(PlainTextComponentSerializer.plainText().serialize(component));
        }
    }

    /** {@code "%id%", "7"} pairs to a {@code {id=7}} map. An odd trailing element is ignored. */
    public static Map<String, String> toMap(String... replacements) {
        if (replacements == null || replacements.length < 2) {
            return Map.of();
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            String token = replacements[i];
            if (token.length() > 2 && token.startsWith("%") && token.endsWith("%")) {
                token = token.substring(1, token.length() - 1);
            }
            values.put(token, replacements[i + 1] == null ? "" : replacements[i + 1]);
        }
        return values;
    }
}
