package me.juancayc.polaroidenchantlock.lock;

import me.juancayc.polaroidenchantlock.config.Settings;
import me.juancayc.polaroidenchantlock.domain.LockedSet;
import me.juancayc.polaroidenchantlock.domain.MessageThrottle;
import me.juancayc.polaroidenchantlock.domain.SetRegistry;
import me.juancayc.polaroidenchantlock.item.ItemResolver;
import me.juancayc.polaroidenchantlock.messaging.MessageService;
import me.juancayc.polaroidenchantlock.storage.SetStore;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The locked sets as the rest of the plugin uses them: the listeners ask whether an item is
 * locked and tell the player when it is; the menus create, change and delete sets.
 *
 * <p>Every change is written to {@code sets.yml} first and only then put into the registry, so
 * what is enforced in memory is never ahead of what survives a restart.
 */
public final class LockService {

    private final SetRegistry registry = new SetRegistry();
    private final MessageThrottle throttle = new MessageThrottle();
    private final SetStore store;
    private final ItemResolver items;
    private final MessageService messages;
    private final Supplier<Settings> settings;
    private final Logger logger;

    public LockService(SetStore store, ItemResolver items, MessageService messages,
                       Supplier<Settings> settings, Logger logger) {
        this.store = store;
        this.items = items;
        this.messages = messages;
        this.settings = settings;
        this.logger = logger;
    }

    // ── Reading ──────────────────────────────────────────────────────────────

    /**
     * Whether this item is a piece of any locked set. Vanilla items, air and items whose plugin
     * is not installed are never locked.
     */
    public boolean isLocked(@Nullable ItemStack item) {
        // Nothing locked at all: do not even ask the item plugins, on every anvil preview.
        if (item == null || registry.isEmpty()) {
            return false;
        }
        return registry.isLocked(items.reference(item));
    }

    public List<LockedSet> sets() {
        return registry.sets();
    }

    public @Nullable LockedSet find(String name) {
        return registry.find(name).orElse(null);
    }

    public boolean exists(String name) {
        return registry.contains(name);
    }

    // ── Telling the player ───────────────────────────────────────────────────

    /**
     * Sends the refusal message for a blocked attempt, at most once per cooldown per player.
     * Only ever called from a real attempt — a click, an enchant — never from a preview.
     */
    public void refuse(Player player, String messageKey) {
        if (throttle.tryAcquire(player.getUniqueId(), System.currentTimeMillis(),
                settings.get().messageCooldownMillis())) {
            messages.sendPrefixed(player, messageKey);
        }
    }

    public void forget(UUID player) {
        throttle.forget(player);
    }

    // ── Changing ─────────────────────────────────────────────────────────────

    /** Replaces the registry with what {@code sets.yml} holds. Returns how many sets were loaded. */
    public synchronized int reload() {
        List<LockedSet> loaded = store.load();
        registry.replaceAll(loaded);
        return registry.sets().size();
    }

    /** Creates a set. False when the name is taken or the file could not be written. */
    public synchronized boolean create(LockedSet set) {
        if (registry.contains(set.name())) {
            return false;
        }
        List<LockedSet> next = new ArrayList<>(registry.sets());
        next.add(set);
        return persist(next) && registry.add(set);
    }

    /** Replaces a set's pieces. False when it no longer exists or the file could not be written. */
    public synchronized boolean update(String name, List<String> pieces) {
        LockedSet current = find(name);
        if (current == null) {
            return false;
        }
        List<LockedSet> next = new ArrayList<>(registry.sets());
        next.replaceAll(set -> set == current ? new LockedSet(current.name(), pieces) : set);
        return persist(next) && registry.update(name, pieces);
    }

    /** Deletes a set. False when it no longer exists or the file could not be written. */
    public synchronized boolean delete(String name) {
        LockedSet current = find(name);
        if (current == null) {
            return false;
        }
        List<LockedSet> next = new ArrayList<>(registry.sets());
        next.remove(current);
        return persist(next) && registry.remove(name);
    }

    private boolean persist(List<LockedSet> sets) {
        try {
            store.save(sets);
            return true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not write sets.yml; the change was not applied.", e);
            return false;
        }
    }
}
