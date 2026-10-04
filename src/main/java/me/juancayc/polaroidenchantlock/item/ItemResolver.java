package me.juancayc.polaroidenchantlock.item;

import me.juancayc.polaroidenchantlock.domain.ItemReference;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Turns an item into its reference ({@code nexo:ruby_helmet}) and a reference back into an item
 * to show in a menu.
 *
 * <p>Every call into an item plugin is guarded twice: {@link ItemHook#isEnabled()} first, so a
 * hook's class is never touched while its plugin is absent, and a catch around the call itself,
 * because these run inside inventory events on every anvil preview — a hook that throws (an item
 * plugin still loading, or a newer version that moved a method) must cost one warning, not the
 * event.
 */
public final class ItemResolver {

    private final List<ItemHook> hooks = new ArrayList<>();
    private final Set<String> reported = ConcurrentHashMap.newKeySet();
    private final Logger logger;

    public ItemResolver(Logger logger) {
        this.logger = logger;
    }

    public void registerHook(ItemHook hook) {
        hooks.add(hook);
    }

    /** Whether at least one item plugin is present, i.e. whether anything can be locked at all. */
    public boolean anyEnabled() {
        for (ItemHook hook : hooks) {
            if (hook.isEnabled()) {
                return true;
            }
        }
        return false;
    }

    /** The reference of a custom item, or null for a vanilla item or one no enabled hook claims. */
    public @Nullable String reference(@Nullable ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }
        for (ItemHook hook : hooks) {
            if (!hook.isEnabled()) {
                continue;
            }
            try {
                String id = hook.getId(item);
                if (id != null && !id.isBlank()) {
                    return ItemReference.of(hook.getPrefix(), id).asString();
                }
            } catch (Throwable failure) {
                report(hook, failure);
            }
        }
        return null;
    }

    /** A fresh item for a stored reference, or null when its plugin is absent or the id is gone. */
    public @Nullable ItemStack item(String reference) {
        Optional<ItemReference> parsed = ItemReference.parse(reference);
        if (parsed.isEmpty()) {
            return null;
        }
        for (ItemHook hook : hooks) {
            if (!hook.getPrefix().equals(parsed.get().prefix()) || !hook.isEnabled()) {
                continue;
            }
            try {
                ItemStack item = hook.getItem(parsed.get().id());
                return item == null || item.getType() == Material.AIR ? null : item;
            } catch (Throwable failure) {
                report(hook, failure);
            }
        }
        return null;
    }

    /** One warning per hook, not one per click. */
    private void report(ItemHook hook, Throwable failure) {
        if (reported.add(hook.getPrefix())) {
            logger.log(Level.WARNING, "The " + hook.getPrefix() + " item hook failed; its items are treated as "
                    + "not locked until this is fixed. Further failures of this hook are not logged.", failure);
        }
    }
}
