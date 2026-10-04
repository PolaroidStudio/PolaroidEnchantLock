package me.juancayc.polaroidenchantlock.menu;

import me.juancayc.polaroidenchantlock.domain.LockedSet;
import me.juancayc.polaroidenchantlock.domain.Pager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * One locked set: its pieces, a button to change them and a button to delete the set. Deleting
 * takes two clicks on the same button — the first only arms it.
 */
final class SetMenu extends Menu {

    private final String name;
    /** The main-menu page this was opened from, so Back returns to it. */
    private final int mainPage;
    private boolean deleteArmed;

    SetMenu(MenuManager menus, Player viewer, String name, int mainPage) {
        super(menus, viewer, menus.config().title("set.title", "name", menus.messages().escape(name)));
        this.name = name;
        this.mainPage = mainPage;
    }

    @Override
    protected void draw() {
        button(BACK, menus.config().item("buttons.back", null), this::back);

        LockedSet set = menus.locks().find(name);
        if (set == null) {
            // Deleted by another admin, or by a reload, while this was open: only Back is left.
            return;
        }
        paging(set.size());

        int slot = 0;
        for (String reference : Pager.slice(set.pieces(), page, PAGE_SIZE)) {
            button(slot++, piece(menus, reference, "set.piece"), null);
        }

        String pieces = String.valueOf(set.size());
        button(LEFT, menus.config().item("set.edit", null, "pieces", pieces), () -> edit(set));
        button(RIGHT, menus.config().item(deleteArmed ? "set.delete-confirm" : "set.delete", null, "pieces", pieces),
                this::delete);
    }

    /**
     * A piece as a menu shows it: the real item when its plugin still knows the id, a labelled
     * placeholder when it does not, with the section's lore underneath either way.
     */
    static ItemStack piece(MenuManager menus, String reference, String lorePath) {
        ItemStack real = menus.items().item(reference);
        ItemStack base = real != null ? real : menus.config().item("set.missing-piece", null, "reference", reference);
        return menus.config().decorate(base, lorePath, "reference", reference);
    }

    private void back() {
        menus.open(viewer, new MainMenu(menus, viewer, mainPage));
    }

    private void edit(LockedSet set) {
        List<EditorMenu.Piece> pieces = new ArrayList<>();
        for (String reference : set.pieces()) {
            pieces.add(new EditorMenu.Piece(reference, menus.items().item(reference)));
        }
        menus.open(viewer, new EditorMenu(menus, viewer, set.name(), pieces, mainPage));
    }

    private void delete() {
        if (!deleteArmed) {
            deleteArmed = true;
            render();
            return;
        }
        if (menus.locks().delete(name)) {
            menus.messages().sendPrefixed(viewer, "set.deleted", "name", menus.messages().escape(name));
        } else {
            menus.messages().sendPrefixed(viewer, "error.save_failed");
        }
        back();
    }
}
