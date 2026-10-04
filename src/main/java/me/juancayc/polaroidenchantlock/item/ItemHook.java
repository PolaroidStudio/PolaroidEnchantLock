package me.juancayc.polaroidenchantlock.item;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Contract for resolving the items of one item plugin, in both directions. */
public interface ItemHook {

    /** The reference prefix, e.g. "nexo". References are written as prefix + ":" + id. */
    String getPrefix();

    /** True when the backing plugin is present and enabled. */
    boolean isEnabled();

    /** Resolves the id (everything after the separator) to an ItemStack, or null if unknown. */
    @Nullable ItemStack getItem(String id);

    /** Reverse lookup: this hook's id for the given item, or null when the item is not its own. */
    @Nullable String getId(ItemStack item);
}
