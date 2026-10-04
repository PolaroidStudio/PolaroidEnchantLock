package me.juancayc.polaroidenchantlock.item;

import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * MythicMobs items. Both lookups are methods of {@code io.lumine.mythic.core.items.ItemExecutor}
 * — what {@code MythicBukkit.inst().getItemManager()} returns — checked against the
 * Mythic-Dist 5.10.0 jar: {@code getItemStack(String)} and {@code getMythicTypeFromItem(ItemStack)}.
 */
public final class MythicMobsHook implements ItemHook {

    @Override
    public String getPrefix() {
        return "mythicmobs";
    }

    @Override
    public boolean isEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("MythicMobs");
    }

    @Override
    public @Nullable ItemStack getItem(String id) {
        return MythicBukkit.inst().getItemManager().getItemStack(id);
    }

    @Override
    public @Nullable String getId(ItemStack item) {
        return MythicBukkit.inst().getItemManager().getMythicTypeFromItem(item);
    }
}
