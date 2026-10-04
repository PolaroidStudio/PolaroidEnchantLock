package me.juancayc.polaroidenchantlock.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The reflection glue only: that a foreign event of the expected shape is bound, and that an
 * event of another shape is rejected when binding rather than when it fires. Whether a locked
 * item really gets its event cancelled needs a real ItemStack, which needs a server.
 */
class PolaroidEnchantApiHookTest {

    /** Shaped like PolaroidEnchant's events: cancellable, with getItem() and getPlayer(). */
    public static class ForeignEvent extends Event implements Cancellable {
        private static final HandlerList HANDLERS = new HandlerList();
        private boolean cancelled;

        public ItemStack getItem() {
            return null;
        }

        public Player getPlayer() {
            return null;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void setCancelled(boolean cancel) {
            cancelled = cancel;
        }

        @Override
        public HandlerList getHandlers() {
            return HANDLERS;
        }
    }

    /** Cancellable, but without the accessors the hook reads. */
    public static class ShapelessEvent extends Event implements Cancellable {
        private static final HandlerList HANDLERS = new HandlerList();

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public void setCancelled(boolean cancel) {
        }

        @Override
        public HandlerList getHandlers() {
            return HANDLERS;
        }
    }

    /** Has the accessors, but cannot be cancelled. */
    public static class UncancellableEvent extends Event {
        private static final HandlerList HANDLERS = new HandlerList();

        public ItemStack getItem() {
            return null;
        }

        public Player getPlayer() {
            return null;
        }

        @Override
        public HandlerList getHandlers() {
            return HANDLERS;
        }
    }

    private final List<ItemStack> asked = new ArrayList<>();
    private final AtomicBoolean refused = new AtomicBoolean();

    private PolaroidEnchantApiHook hook() {
        return new PolaroidEnchantApiHook(null, item -> {
            asked.add(item);
            return true;
        }, (player, key) -> refused.set(true), () -> true, Logger.getLogger("test"));
    }

    @Test
    void anEventWithNoItemIsLeftAloneAndNobodyIsTold() throws Exception {
        PolaroidEnchantApiHook hook = hook();
        ForeignEvent event = new ForeignEvent();

        hook.executor(ForeignEvent.class).execute(hook, event);

        assertFalse(event.isCancelled());
        assertTrue(asked.isEmpty(), "an empty slot is never looked up");
        assertFalse(refused.get());
    }

    @Test
    void anEventWithoutTheAccessorsIsRejectedWhenBinding() {
        assertThrows(NoSuchMethodException.class, () -> hook().executor(ShapelessEvent.class));
    }

    @Test
    void anEventThatCannotBeCancelledIsRejectedWhenBinding() {
        assertThrows(NoSuchMethodException.class, () -> hook().executor(UncancellableEvent.class));
    }

    @Test
    void anEventOfAnotherTypeIsIgnored() throws Exception {
        PolaroidEnchantApiHook hook = hook();

        hook.executor(ForeignEvent.class).execute(hook, new UncancellableEvent());

        assertTrue(asked.isEmpty());
    }
}
