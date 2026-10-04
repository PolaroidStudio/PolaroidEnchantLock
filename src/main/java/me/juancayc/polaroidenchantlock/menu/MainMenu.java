package me.juancayc.polaroidenchantlock.menu;

import me.juancayc.polaroidenchantlock.domain.LockedSet;
import me.juancayc.polaroidenchantlock.domain.Pager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The main menu: one item per locked set, with its name and how many pieces it holds, and the
 * button that starts a new set.
 */
final class MainMenu extends Menu {

    /** Where the "no sets yet" sign sits: the middle of the content area. */
    private static final int EMPTY_SIGN = 22;

    MainMenu(MenuManager menus, Player viewer, int page) {
        super(menus, viewer, menus.config().title("main.title"));
        this.page = page;
    }

    @Override
    protected void draw() {
        List<LockedSet> sets = menus.locks().sets();
        paging(sets.size());

        if (sets.isEmpty()) {
            button(EMPTY_SIGN, menus.config().item("main.empty", null), null);
        }
        int slot = 0;
        for (LockedSet set : Pager.slice(sets, page, PAGE_SIZE)) {
            String name = set.name();
            button(slot++, menus.config().item("main.set", icon(set),
                            "name", menus.messages().escape(name), "pieces", String.valueOf(set.size())),
                    () -> menus.open(viewer, new SetMenu(menus, viewer, name, page)));
        }

        button(CENTER, menus.config().item("main.create", null),
                () -> menus.open(viewer, new EditorMenu(menus, viewer, null, new ArrayList<>(), page)));
    }

    /** A set is drawn as its first piece that still exists; the configured material otherwise. */
    private @Nullable ItemStack icon(LockedSet set) {
        for (String reference : set.pieces()) {
            ItemStack item = menus.items().item(reference);
            if (item != null) {
                return item;
            }
        }
        return null;
    }
}
