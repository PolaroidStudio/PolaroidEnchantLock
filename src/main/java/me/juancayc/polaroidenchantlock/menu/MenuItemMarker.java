package me.juancayc.polaroidenchantlock.menu;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * The mark every item a menu draws carries, so one that escapes a menu can always be told apart
 * from a real one and deleted. That includes the copies of the admin's own items the editor
 * shows: a copy without the mark would be a free duplicate the moment it leaked.
 */
public final class MenuItemMarker {

    private final NamespacedKey key;

    public MenuItemMarker(Plugin plugin) {
        this.key = new NamespacedKey(plugin, "menu_item");
    }

    public ItemStack mark(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isMarked(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    /** Deletes every marked item the player holds, cursor included, and resyncs the client. */
    public void cleanInventory(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        boolean changed = false;
        for (int slot = 0; slot < contents.length; slot++) {
            if (isMarked(contents[slot])) {
                player.getInventory().setItem(slot, null);
                changed = true;
            }
        }
        if (isMarked(player.getItemOnCursor())) {
            player.setItemOnCursor(null);
            changed = true;
        }
        if (changed) {
            player.updateInventory();
        }
    }
}
