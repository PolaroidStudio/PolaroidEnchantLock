package me.juancayc.polaroidenchantlock;

import me.juancayc.polaroidenchantlock.commands.EnchantLockCommand;
import me.juancayc.polaroidenchantlock.config.Settings;
import me.juancayc.polaroidenchantlock.item.ItemResolver;
import me.juancayc.polaroidenchantlock.item.MythicMobsHook;
import me.juancayc.polaroidenchantlock.item.NexoHook;
import me.juancayc.polaroidenchantlock.listeners.PolaroidHooksListener;
import me.juancayc.polaroidenchantlock.listeners.VanillaLockListener;
import me.juancayc.polaroidenchantlock.lock.LockService;
import me.juancayc.polaroidenchantlock.menu.MenuConfig;
import me.juancayc.polaroidenchantlock.menu.MenuItemMarker;
import me.juancayc.polaroidenchantlock.menu.MenuListener;
import me.juancayc.polaroidenchantlock.menu.MenuManager;
import me.juancayc.polaroidenchantlock.messaging.MessageService;
import me.juancayc.polaroidenchantlock.messaging.MiniMessageProvider;
import me.juancayc.polaroidenchantlock.storage.SetStore;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class PolaroidEnchantLockPlugin extends JavaPlugin {

    private volatile Settings settings;
    private MessageService messages;
    private MenuConfig menuConfig;
    private MenuManager menus;
    private LockService locks;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = Settings.read(getConfig());

        messages = new MessageService(this, MiniMessageProvider.get());
        messages.reload(settings.language());

        // Both hooks are registered whether or not their plugin is installed: each one checks
        // isEnabled() before every call, so an item plugin added later is picked up by a restart
        // with no change here, and one that is absent is never touched.
        ItemResolver items = new ItemResolver(getLogger());
        items.registerHook(new NexoHook());
        items.registerHook(new MythicMobsHook());

        locks = new LockService(new SetStore(new File(getDataFolder(), "sets.yml"), getLogger()),
                items, messages, this::settings, getLogger());
        int sets = locks.reload();

        MenuItemMarker marker = new MenuItemMarker(this);
        menuConfig = new MenuConfig(this, messages, marker);
        menuConfig.reload();
        menus = new MenuManager(this, locks, items, messages, menuConfig, marker, this::settings);

        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new VanillaLockListener(locks, this::settings), this);
        pluginManager.registerEvents(new PolaroidHooksListener(locks, messages, this::settings, getLogger()), this);
        pluginManager.registerEvents(new MenuListener(menus), this);

        new EnchantLockCommand(this, menus, messages).register();

        getLogger().info(sets + " locked set(s) loaded.");
        if (!items.anyEnabled()) {
            getLogger().warning("Neither Nexo nor MythicMobs is installed: there is nothing this plugin can lock.");
        }
    }

    @Override
    public void onDisable() {
        // A disabled plugin has no listener left to cancel clicks, so no menu may stay on screen.
        // Null-safe because onDisable runs even when onEnable failed part-way.
        if (menus != null) {
            menus.closeAll();
        }
    }

    /** The current settings. Always read through here: a reload replaces the instance. */
    public Settings settings() {
        return settings;
    }

    /**
     * Re-reads config.yml, the language file, menus.yml and sets.yml.
     *
     * @return how many locked sets are loaded afterwards
     */
    public int reloadEverything() {
        reloadConfig();
        settings = Settings.read(getConfig());
        messages.reload(settings.language());
        menuConfig.reload();
        return locks.reload();
    }
}
