package me.juancayc.polaroidenchantlock.menu;

import me.juancayc.polaroidenchantlock.domain.Pager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The set editor. Clicking an item in the admin's own inventory adds a copy of it to the menu;
 * clicking the copy takes it out again. The real item never moves: the click was cancelled
 * before this class saw it, and what the menu holds is a marked copy plus the item's id.
 *
 * <p>Only the id is ever saved. An item no item plugin claims has no id, so it is refused with a
 * message — locking a plain diamond chestplate would lock every one on the server.
 *
 * <p>Saving a new set asks for its name in a dialog; saving an existing one just writes it.
 */
final class EditorMenu extends Menu {

    /**
     * One piece in the editor.
     *
     * @param reference the canonical item id, which is what gets saved
     * @param icon      the item to draw, or null when its plugin no longer knows the id
     */
    record Piece(String reference, @Nullable ItemStack icon) {
    }

    /** The set being changed, or null when this is a new set that still has no name. */
    private final @Nullable String editing;
    private final List<Piece> pieces;
    private final int mainPage;

    EditorMenu(MenuManager menus, Player viewer, @Nullable String editing, List<Piece> pieces, int mainPage) {
        super(menus, viewer, editing == null
                ? menus.config().title("editor.title-new")
                : menus.config().title("editor.title-edit", "name", menus.messages().escape(editing)));
        this.editing = editing;
        this.pieces = pieces;
        this.mainPage = mainPage;
    }

    @Override
    protected void draw() {
        paging(pieces.size());
        button(BACK, menus.config().item("buttons.back", null), this::back);

        int slot = 0;
        for (Piece piece : Pager.slice(pieces, page, PAGE_SIZE)) {
            ItemStack base = piece.icon() != null ? piece.icon()
                    : menus.config().item("set.missing-piece", null, "reference", piece.reference());
            button(slot++, menus.config().decorate(base, "editor.piece", "reference", piece.reference()),
                    () -> remove(piece));
        }

        button(CENTER, menus.config().item("editor.save", null, "pieces", String.valueOf(pieces.size())), this::save);
    }

    @Override
    boolean ownInventoryClick(ItemStack copy) {
        String reference = menus.items().reference(copy);
        if (reference == null) {
            menus.messages().sendPrefixed(viewer, "editor.not_custom");
            return false;
        }
        for (Piece piece : pieces) {
            if (piece.reference().equals(reference)) {
                menus.messages().sendPrefixed(viewer, "editor.already_added");
                return false;
            }
        }
        pieces.add(new Piece(reference, copy.asOne()));
        // Show the page the new piece landed on.
        page = Pager.pageCount(pieces.size(), PAGE_SIZE) - 1;
        render();
        return true;
    }

    private void remove(Piece piece) {
        pieces.remove(piece);
        render();
    }

    private void back() {
        if (editing == null) {
            menus.open(viewer, new MainMenu(menus, viewer, mainPage));
        } else {
            menus.open(viewer, new SetMenu(menus, viewer, editing, mainPage));
        }
    }

    private void save() {
        if (pieces.isEmpty()) {
            menus.messages().sendPrefixed(viewer, "editor.empty");
            return;
        }
        if (editing == null) {
            // The dialog replaces the menu on screen. The pieces travel with it, so Cancel can
            // bring the editor back exactly as it was.
            viewer.closeInventory();
            menus.nameDialog().ask(viewer, new ArrayList<>(pieces), mainPage, "", null);
            return;
        }
        if (menus.locks().update(editing, references(pieces))) {
            menus.messages().sendPrefixed(viewer, "set.updated", "name", menus.messages().escape(editing),
                    "pieces", String.valueOf(pieces.size()));
        } else {
            menus.messages().sendPrefixed(viewer, "error.save_failed");
        }
        back();
    }

    static List<String> references(List<Piece> pieces) {
        List<String> references = new ArrayList<>(pieces.size());
        for (Piece piece : pieces) {
            references.add(piece.reference());
        }
        return references;
    }
}
