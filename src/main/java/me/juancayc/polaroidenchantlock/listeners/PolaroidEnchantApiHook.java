package me.juancayc.polaroidenchantlock.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Cancels PolaroidEnchant's own {@code EnchantApplyEvent} and {@code EnchantRemoveEvent} when
 * the item they are about is locked. Those events fire right before PolaroidEnchant applies or
 * removes an enchantment — from a real anvil, its virtual anvil, {@code /penchant} or another
 * plugin calling its API — so this is the one place that covers all of them, the API path
 * included, which nothing outside that plugin could see before.
 *
 * <p>The events are bound by name through PolaroidEnchant's own class loader, and read by
 * reflection. That is deliberate: PolaroidEnchant is a private repository, so compiling against
 * its API would make this plugin's build depend on a token. Nothing here is linked against it.
 *
 * <p>A PolaroidEnchant older than its developer API has no such events; {@link #bind} then
 * returns {@code false} and the outside guards in {@link PolaroidHooksListener} remain the only
 * protection, exactly as before. With a newer one both run, and the outside guards usually stop
 * the attempt first.
 */
public final class PolaroidEnchantApiHook implements Listener {

    private static final String PLUGIN = "PolaroidEnchant";
    private static final List<String> EVENTS = List.of(
            "dev.juancorso.polaroidenchant.api.event.EnchantApplyEvent",
            "dev.juancorso.polaroidenchant.api.event.EnchantRemoveEvent");

    private final Plugin owner;
    private final Predicate<ItemStack> locked;
    private final BiConsumer<Player, String> refuse;
    private final BooleanSupplier enabled;
    private final Logger logger;
    private boolean bound;

    /**
     * @param owner   this plugin, which the event handlers are registered for
     * @param locked  whether an item is locked
     * @param refuse  tells a player an attempt was blocked, given the message key
     * @param enabled whether the PolaroidEnchant hook is switched on in config.yml
     */
    public PolaroidEnchantApiHook(Plugin owner, Predicate<ItemStack> locked,
                                  BiConsumer<Player, String> refuse, BooleanSupplier enabled, Logger logger) {
        this.owner = owner;
        this.locked = locked;
        this.refuse = refuse;
        this.enabled = enabled;
        this.logger = logger;
    }

    /**
     * Registers the two events if PolaroidEnchant is enabled and has them. Safe to call more
     * than once: it binds at most once.
     *
     * @return whether the events are bound after this call
     */
    public synchronized boolean bind() {
        if (bound) {
            return true;
        }
        PluginManager pluginManager = owner.getServer().getPluginManager();
        Plugin target = pluginManager.getPlugin(PLUGIN);
        if (target == null || !target.isEnabled()) {
            return false;
        }
        ClassLoader loader = target.getClass().getClassLoader();
        try {
            for (String name : EVENTS) {
                Class<? extends Event> type = loader.loadClass(name).asSubclass(Event.class);
                // LOWEST and ignoreCancelled: refuse before any other listener spends work on it.
                pluginManager.registerEvent(type, this, EventPriority.LOWEST, executor(type), owner, true);
            }
        } catch (ReflectiveOperationException | ClassCastException | LinkageError e) {
            // An older PolaroidEnchant, or one whose events changed shape. Not an error for us.
            logger.info("PolaroidEnchant has no compatible developer API (" + e + "); "
                    + "its menus and commands are still guarded from the outside.");
            return false;
        }
        bound = true;
        logger.info("Hooked into PolaroidEnchant's developer API events.");
        return true;
    }

    /** PolaroidEnchant is not a dependency, so it may well enable after this plugin does. */
    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (PLUGIN.equals(event.getPlugin().getName())) {
            bind();
        }
    }

    /**
     * Builds the handler for one event type. Fails here, at bind time, when the type lacks
     * {@code getItem()} or {@code getPlayer()} or is not cancellable, rather than on the first
     * event.
     */
    EventExecutor executor(Class<? extends Event> type) throws NoSuchMethodException {
        if (!Cancellable.class.isAssignableFrom(type)) {
            throw new NoSuchMethodException(type.getName() + " is not cancellable");
        }
        Method item = type.getMethod("getItem");
        Method player = type.getMethod("getPlayer");
        return (listener, event) -> {
            if (!type.isInstance(event) || !enabled.getAsBoolean()) {
                return;
            }
            try {
                if (!(item.invoke(event) instanceof ItemStack stack) || !locked.test(stack)) {
                    return;
                }
                ((Cancellable) event).setCancelled(true);
                // Null when another plugin went through the API: there is nobody to tell.
                if (player.invoke(event) instanceof Player who) {
                    refuse.accept(who, "blocked.menu");
                }
            } catch (ReflectiveOperationException e) {
                logger.warning("Could not read PolaroidEnchant's " + type.getSimpleName() + ": " + e);
            }
        };
    }
}
