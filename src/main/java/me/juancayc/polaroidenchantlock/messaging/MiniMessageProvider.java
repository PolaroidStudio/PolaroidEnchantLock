package me.juancayc.polaroidenchantlock.messaging;

import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * The single shared {@link MiniMessage} instance for the whole plugin.
 *
 * <p>{@link MessageService} and the menus both render through this exact instance, so a tag
 * resolver added here is visible everywhere at once. PolaroidEnchantLock has no Nexo glyph
 * integration, so this is plain {@link MiniMessage#miniMessage()} — the hook point still exists
 * for consistency with the other Polaroid-line plugins.
 */
public final class MiniMessageProvider {

    private static final MiniMessage INSTANCE = MiniMessage.miniMessage();

    private MiniMessageProvider() {}

    public static MiniMessage get() {
        return INSTANCE;
    }
}
