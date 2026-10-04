package me.juancayc.polaroidenchantlock.listeners;

import me.juancayc.polaroidenchantlock.config.Settings;
import me.juancayc.polaroidenchantlock.domain.ForeignCommands;
import me.juancayc.polaroidenchantlock.lock.LockService;
import me.juancayc.polaroidenchantlock.messaging.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Protects locked pieces inside the two enchantment plugins of the Polaroid line. Neither is a
 * dependency: their menus are recognised by the class name of the inventory holder and their
 * command by its label, so this class links against nothing of theirs and does nothing at all
 * on a server that has neither.
 *
 * <p>Neither plugin exposes an API or fires an event of its own, so the guard works from the
 * outside: while one of their menus is open, any click or drag that involves a locked piece —
 * the item clicked, the item on the cursor, or the hotbar or off-hand item a key would swap in —
 * is cancelled at LOWEST, before their listener sees it.
 *
 * <ul>
 *   <li><b>PolaroidEnchant</b> — the virtual anvil for Bedrock players. Its listener skips
 *       cancelled events, so a cancelled click never puts the piece in. {@code /penchant enchant}
 *       and {@code /penchant remove} write straight to the item in a player's hand; they are
 *       cancelled when that item is locked, from a player or from the console.</li>
 *   <li><b>PolaroidEnchanter</b> — the Enchanter, Disenchanter, Library and Anvil menus. The
 *       click is cancelled the same way, <b>but that plugin's listener acts on cancelled events
 *       too</b>, so on its current code the piece is stored anyway. Nothing on this side can
 *       stop that short of hiding the player's real item from the other listener, which this
 *       plugin does not do. The guard starts working the moment that listener honours
 *       cancellation; see the README.</li>
 * </ul>
 *
 * <p>Because the second case exists, the guard checks its own work: at MONITOR it looks at
 * whether the piece is still where it was. Only then is the player told it is locked. If the
 * piece moved despite the cancellation, the player is told nothing false and the console is
 * warned once, naming the menu that ignored the cancellation.
 */
public final class PolaroidHooksListener implements Listener {

    /** PolaroidEnchant's virtual anvil holder. */
    private static final String VIRTUAL_ANVIL = "dev.juancorso.polaroidenchant.floodgate.VirtualAnvilGUI";
    /** PolaroidEnchanter's menus all live here and implement its sealed {@code PolaroidMenu}. */
    private static final String ENCHANTER_MENUS = "studio.polaroid.enchanter.menu.";

    /** What a guarded click looked like when it was cancelled, to compare against afterwards. */
    private record Guarded(@Nullable ItemStack clicked, @Nullable ItemStack cursor) {
    }

    private final LockService locks;
    private final MessageService messages;
    private final Supplier<Settings> settings;
    private final Logger logger;
    private final Map<InventoryClickEvent, Guarded> pending = new ConcurrentHashMap<>();
    private final Set<String> warned = ConcurrentHashMap.newKeySet();

    public PolaroidHooksListener(LockService locks, MessageService messages, Supplier<Settings> settings,
                                 Logger logger) {
        this.locks = locks;
        this.messages = messages;
        this.settings = settings;
        this.logger = logger;
    }

    // ── Menus ────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!guarded(event.getView()) || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (locks.isLocked(event.getCurrentItem()) || locks.isLocked(event.getCursor())
                || locks.isLocked(swappedIn(event, player))) {
            event.setCancelled(true);
            pending.put(event, new Guarded(copy(event.getCurrentItem()), copy(event.getCursor())));
        }
    }

    /** After every other listener: did the cancellation hold? */
    @EventHandler(priority = EventPriority.MONITOR)
    public void afterClick(InventoryClickEvent event) {
        Guarded before = pending.remove(event);
        if (before == null || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        boolean held = Objects.equals(before.clicked(), copy(event.getCurrentItem()))
                && Objects.equals(before.cursor(), copy(event.getCursor()));
        if (held) {
            locks.refuse(player, "blocked.menu");
            return;
        }
        InventoryHolder holder = event.getView().getTopInventory().getHolder(false);
        String type = holder == null ? "unknown" : holder.getClass().getName();
        if (warned.add(type)) {
            logger.warning("A locked item was moved by " + type + " even though the click was cancelled. "
                    + "That menu acts on cancelled inventory events, so PolaroidEnchantLock cannot protect "
                    + "locked items inside it until it honours cancellation. This is logged once per menu type.");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!guarded(event.getView()) || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (locks.isLocked(event.getOldCursor())) {
            event.setCancelled(true);
            locks.refuse(player, "blocked.menu");
        }
    }

    /** The item a number key or the off-hand key would move, which is not the item clicked. */
    private static @Nullable ItemStack swappedIn(InventoryClickEvent event, Player player) {
        if (event.getClick() == ClickType.NUMBER_KEY && event.getHotbarButton() >= 0) {
            return player.getInventory().getItem(event.getHotbarButton());
        }
        if (event.getClick() == ClickType.SWAP_OFFHAND) {
            return player.getInventory().getItemInOffHand();
        }
        return null;
    }

    /** An empty slot reads as null or as air depending on the path; both become null here. */
    private static @Nullable ItemStack copy(@Nullable ItemStack item) {
        return item == null || item.getType().isAir() ? null : item.clone();
    }

    private boolean guarded(InventoryView view) {
        InventoryHolder holder = view.getTopInventory().getHolder(false);
        if (holder == null) {
            return false;
        }
        String type = holder.getClass().getName();
        Settings current = settings.get();
        return (current.hookPolaroidEnchant() && type.equals(VIRTUAL_ANVIL))
                || (current.hookPolaroidEnchanter() && type.startsWith(ENCHANTER_MENUS));
    }

    // ── /penchant enchant | remove ───────────────────────────────────────────

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (refusesCommand(event.getPlayer(), event.getMessage())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        if (refusesCommand(event.getSender(), event.getCommand())) {
            event.setCancelled(true);
        }
    }

    private boolean refusesCommand(CommandSender sender, String commandLine) {
        if (!settings.get().hookPolaroidEnchant()) {
            return false;
        }
        String targetName = ForeignCommands.penchantTarget(commandLine);
        if (targetName == null) {
            return false;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        // On Folia a player in another region may not be read from this thread. On Paper every
        // player is owned by the one main thread, so this never skips anybody there.
        if (target == null || !Bukkit.isOwnedByCurrentRegion(target)
                || !locks.isLocked(target.getInventory().getItemInMainHand())) {
            return false;
        }
        // Not rate-limited: this answers a command somebody typed, once per command.
        messages.sendPrefixed(sender, "blocked.command", "player", messages.escape(target.getName()));
        return true;
    }
}
