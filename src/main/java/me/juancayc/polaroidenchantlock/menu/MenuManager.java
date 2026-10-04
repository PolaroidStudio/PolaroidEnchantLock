package me.juancayc.polaroidenchantlock.menu;

import me.juancayc.polaroidenchantlock.config.Settings;
import me.juancayc.polaroidenchantlock.item.ItemResolver;
import me.juancayc.polaroidenchantlock.lock.LockService;
import me.juancayc.polaroidenchantlock.messaging.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * What the menus share, and the server-side registry of which menu each player has open.
 *
 * <p>The registry is the authority: a click is only acted on when the menu it arrived in is the
 * one registered for that player. It is keyed by UUID, and an entry is dropped on close, on quit,
 * when some other inventory opens, and when the plugin is disabled — several independent paths,
 * because a client can suppress any single one of them.
 */
public final class MenuManager {

    public static final String ADMIN = "polaroidenchantlock.admin";

    private final JavaPlugin plugin;
    private final LockService locks;
    private final ItemResolver items;
    private final MessageService messages;
    private final MenuConfig config;
    private final MenuItemMarker marker;
    private final NameDialog nameDialog;
    private final Map<UUID, Menu> open = new ConcurrentHashMap<>();

    public MenuManager(JavaPlugin plugin, LockService locks, ItemResolver items, MessageService messages,
                       MenuConfig config, MenuItemMarker marker, Supplier<Settings> settings) {
        this.plugin = plugin;
        this.locks = locks;
        this.items = items;
        this.messages = messages;
        this.config = config;
        this.marker = marker;
        this.nameDialog = new NameDialog(this, settings);
    }

    public LockService locks() {
        return locks;
    }

    public ItemResolver items() {
        return items;
    }

    public MessageService messages() {
        return messages;
    }

    public MenuConfig config() {
        return config;
    }

    public MenuItemMarker marker() {
        return marker;
    }

    NameDialog nameDialog() {
        return nameDialog;
    }

    /** Opens the main menu on its first page. Player thread only. */
    public void openMain(Player player) {
        open(player, new MainMenu(this, player, 0));
    }

    /**
     * Opens a menu, replacing whatever menu the player had. Player thread only, and never from
     * inside an inventory event — callers reach this through {@link #run}.
     */
    void open(Player player, Menu menu) {
        if (!player.isOnline() || player.isSleeping()) {
            return;
        }
        menu.render();
        // The new menu is registered BEFORE it is shown: showing it closes the old window, and
        // that close event must find the new menu registered and leave it alone.
        open.put(player.getUniqueId(), menu);
        player.openInventory(menu.getInventory());
        // Another plugin may have cancelled the open; then nothing is on screen to register.
        if (player.getOpenInventory().getTopInventory().getHolder(false) != menu) {
            open.remove(player.getUniqueId(), menu);
        }
    }

    /** Whether this menu is the one the registry holds for the player. */
    boolean isCurrent(Player player, Menu menu) {
        return open.get(player.getUniqueId()) == menu;
    }

    /** Drops the entry only if it still is this menu; a newer menu is left registered. */
    void evict(UUID player, Menu menu) {
        open.remove(player, menu);
    }

    void evict(UUID player) {
        open.remove(player);
    }

    /**
     * Runs a task on the player's own thread, next tick. On Paper that is the main thread; on
     * Folia it is the region that owns the player. Dropped silently if the player is gone first.
     */
    void run(Player player, Runnable task) {
        player.getScheduler().run(plugin, scheduled -> task.run(), null);
    }

    void runLater(Player player, Runnable task, long delayTicks) {
        player.getScheduler().runDelayed(plugin, scheduled -> task.run(), null, Math.max(1, delayTicks));
    }

    /** Closes every open menu. Called when the plugin is disabled. */
    public void closeAll() {
        for (UUID id : open.keySet()) {
            Player player = Bukkit.getPlayer(id);
            if (player == null) {
                continue;
            }
            try {
                if (player.getOpenInventory().getTopInventory().getHolder(false) instanceof Menu) {
                    player.closeInventory();
                    marker.cleanInventory(player);
                }
            } catch (RuntimeException wrongThread) {
                // Folia: the disabling thread does not own this player. The menu's items are
                // marked, so the join sweep removes anything that outlives the plugin.
            }
        }
        open.clear();
    }

    /** The registered menu of a player, or null. */
    @Nullable Menu current(Player player) {
        return open.get(player.getUniqueId());
    }
}
