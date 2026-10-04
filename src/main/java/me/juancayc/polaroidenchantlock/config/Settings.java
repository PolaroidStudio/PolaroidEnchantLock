package me.juancayc.polaroidenchantlock.config;

import me.juancayc.polaroidenchantlock.domain.SetNameRules;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * {@code config.yml} read once into an immutable value. A reload builds a new one; nothing holds
 * on to an old instance, because every reader asks the plugin for the current settings.
 *
 * @param language               the lang file to use, {@code lang/messages_<language>.yml}
 * @param allowRepairAndRename   whether an anvil may still repair or rename a locked piece
 * @param messageCooldownMillis  the least time between two refusal messages to one player
 * @param nameMaxLength          the longest set name, already clamped to the hard cap
 * @param dialogLifetimeSeconds  how long the naming dialog's buttons stay valid
 * @param hookPolaroidEnchant    guard PolaroidEnchant's virtual anvil and /penchant
 * @param hookPolaroidEnchanter  guard PolaroidEnchanter's menus
 */
public record Settings(String language, boolean allowRepairAndRename, long messageCooldownMillis,
                       int nameMaxLength, long dialogLifetimeSeconds,
                       boolean hookPolaroidEnchant, boolean hookPolaroidEnchanter) {

    public static Settings read(FileConfiguration config) {
        return new Settings(
                config.getString("language", "en"),
                config.getBoolean("anvil.allow-repair-and-rename", true),
                Math.max(0L, config.getLong("messages.cooldown-milliseconds", 1500L)),
                SetNameRules.clampMaxLength(config.getInt("sets.name-max-length", 24)),
                Math.max(30L, config.getLong("sets.name-dialog-lifetime-seconds", 300L)),
                config.getBoolean("hooks.polaroidenchant", true),
                config.getBoolean("hooks.polaroidenchanter", true));
    }
}
