package me.juancayc.polaroidenchantlock.menu;

import me.juancayc.polaroidenchantlock.domain.Pager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Every menu this plugin opens. {@link MenuListener} recognises them by this type, never by
 * title, and cancels every click before anything here runs.
 *
 * <p>All three are pure menus: every slot is plugin chrome and the player owns nothing in them.
 * Even the editor, which shows the admin's own items, shows marked copies — the real items never
 * leave the inventory.
 *
 * <p>The layout is shared: five rows of content, then one row of chrome.
 */
public abstract class Menu implements InventoryHolder {

    static final int SIZE = 54;
    static final int PAGE_SIZE = 45;

    static final int BACK = 45;
    static final int PREVIOUS = 47;
    static final int LEFT = 48;
    static final int CENTER = 49;
    static final int RIGHT = 50;
    static final int NEXT = 51;
    static final int CLOSE = 53;

    protected final MenuManager menus;
    protected final Player viewer;
    protected int page;

    private final Inventory inventory;
    private final Map<Integer, Runnable> actions = new HashMap<>();

    protected Menu(MenuManager menus, Player viewer, Component title) {
        this.menus = menus;
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(this, SIZE, title);
    }

    @Override
    public final Inventory getInventory() {
        return inventory;
    }

    /** Draws the whole menu again from the current state. Player thread only. */
    public final void render() {
        inventory.clear();
        actions.clear();
        ItemStack filler = menus.config().item("fill", null);
        for (int slot = PAGE_SIZE; slot < SIZE; slot++) {
            inventory.setItem(slot, filler);
        }
        button(CLOSE, menus.config().item("buttons.close", null), viewer::closeInventory);
        draw();
    }

    /** The menu's own content and buttons; the filler row and Close are already in place. */
    protected abstract void draw();

    /** Puts an item in a slot. With no action it is information: a click on it does nothing. */
    protected final void button(int slot, ItemStack item, @Nullable Runnable action) {
        inventory.setItem(slot, item);
        if (action != null) {
            actions.put(slot, action);
        }
    }

    /** Clamps the page and shows Previous / Next only where there is a page to go to. */
    protected final void paging(int entries) {
        page = Pager.clamp(page, entries, PAGE_SIZE);
        if (page > 0) {
            button(PREVIOUS, menus.config().item("buttons.previous", null), () -> turn(-1));
        }
        if (page < Pager.pageCount(entries, PAGE_SIZE) - 1) {
            button(NEXT, menus.config().item("buttons.next", null), () -> turn(1));
        }
    }

    private void turn(int by) {
        page += by;
        render();
    }

    /**
     * A click on one of the menu's own slots, already cancelled and already deferred to the tick
     * after the event.
     *
     * @return whether the slot had an action, so the listener knows whether to play the sound
     */
    final boolean click(int rawSlot) {
        Runnable action = actions.get(rawSlot);
        if (action == null) {
            return false;
        }
        action.run();
        return true;
    }

    /**
     * A click on an item in the player's own inventory while this menu is open. The click was
     * cancelled, so the item did not move; {@code copy} is a clone taken during the event.
     *
     * @return whether the menu did something with it
     */
    boolean ownInventoryClick(ItemStack copy) {
        return false;
    }
}
