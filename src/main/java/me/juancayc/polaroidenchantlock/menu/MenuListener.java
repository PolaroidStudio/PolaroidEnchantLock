package me.juancayc.polaroidenchantlock.menu;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The wall around the menus (minecraft-menu-security, pure-menu rules).
 *
 * <p>A menu is recognised by the holder of the TOP inventory, so a click in the player's own
 * inventory while a menu is open is handled here too — that is where shift-click, number keys
 * and the off-hand key would otherwise push a real item into a menu slot. Every such event is
 * cancelled first and looked at second. Nothing opens or closes an inventory inside the event:
 * every action runs on the player's thread the tick after.
 *
 * <p>Marked menu items that escape anyway are deleted on pickup, on drop, on join and shortly
 * after a menu closes.
 */
public final class MenuListener implements Listener {

    /** The client sends shift-clicks twice; they get the longer window. */
    private static final long CLICK_COOLDOWN_MILLIS = 75L;
    private static final long SHIFT_COOLDOWN_MILLIS = 200L;

    private final MenuManager menus;
    private final Map<UUID, Long> lastDispatch = new ConcurrentHashMap<>();

    public MenuListener(MenuManager menus) {
        this.menus = menus;
    }

    // ── Clicks and drags ─────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.LOW)
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder(false) instanceof Menu menu)) {
            return;
        }
        // Cancel first, unconditionally: whatever returns early below, nothing moved.
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        // A window the registry does not know is not acted on, only shut.
        if (!menus.isCurrent(player, menu) || !player.hasPermission(MenuManager.ADMIN)) {
            menus.run(player, player::closeInventory);
            return;
        }

        // Only plain and shift clicks act. A double click collects matching stacks to the
        // cursor; number keys, the off-hand key, drops, creative and unknown clicks have no
        // meaning in a menu. All of them stay cancelled and do nothing.
        ClickType click = event.getClick();
        boolean shift = click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT;
        if (click != ClickType.LEFT && click != ClickType.RIGHT && !shift) {
            return;
        }
        int rawSlot = event.getRawSlot();
        if (rawSlot < 0) {
            return;
        }
        if (onCooldown(player, shift)) {
            return;
        }

        if (rawSlot < top.getSize()) {
            menus.run(player, () -> {
                if (menus.isCurrent(player, menu) && menu.click(rawSlot)) {
                    menus.config().playClick(player);
                }
            });
            return;
        }

        // The player's own inventory. The item stays where it is; the menu gets a copy, taken
        // now, because next tick the slot may hold something else.
        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) {
            return;
        }
        ItemStack copy = current.clone();
        menus.run(player, () -> {
            if (menus.isCurrent(player, menu) && menu.ownInventoryClick(copy)) {
                menus.config().playClick(player);
            }
        });
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder(false) instanceof Menu) {
            event.setCancelled(true);
        }
    }

    private boolean onCooldown(Player player, boolean shift) {
        long now = System.currentTimeMillis();
        Long last = lastDispatch.get(player.getUniqueId());
        if (last != null && now - last < (shift ? SHIFT_COOLDOWN_MILLIS : CLICK_COOLDOWN_MILLIS)) {
            return true;
        }
        lastDispatch.put(player.getUniqueId(), now);
        return false;
    }

    // ── Registry eviction ────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onOpen(InventoryOpenEvent event) {
        if (event.getPlayer().isSleeping() && event.getInventory().getHolder(false) instanceof Menu) {
            event.setCancelled(true);
            return;
        }
        // Some other window took the screen: whatever menu was registered is no longer shown.
        if (!(event.getInventory().getHolder(false) instanceof Menu)) {
            menus.evict(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof Menu menu)) {
            return;
        }
        UUID id = event.getPlayer().getUniqueId();
        // Only this menu's own entry: when one menu replaces another, the new one is already
        // registered by the time the old one's close event arrives.
        menus.evict(id, menu);
        lastDispatch.remove(id);
        if (event.getPlayer() instanceof Player player) {
            menus.runLater(player, () -> menus.marker().cleanInventory(player), 2L);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        menus.evict(id);
        lastDispatch.remove(id);
    }

    // ── Leaked menu items ────────────────────────────────────────────────────

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        menus.evict(player.getUniqueId());
        menus.runLater(player, () -> menus.marker().cleanInventory(player), 10L);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (menus.marker().isMarked(event.getItem().getItemStack())) {
            event.setCancelled(true);
            event.getItem().remove();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (menus.marker().isMarked(event.getItemDrop().getItemStack())) {
            event.getItemDrop().remove();
        }
    }
}
