package me.juancayc.polaroidenchantlock.menu;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import me.juancayc.polaroidenchantlock.config.Settings;
import me.juancayc.polaroidenchantlock.domain.LockedSet;
import me.juancayc.polaroidenchantlock.domain.Pager;
import me.juancayc.polaroidenchantlock.domain.SetNameRules;
import me.juancayc.polaroidenchantlock.messaging.MessageService;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The dialog that asks for a new set's name (Paper Dialog API, 1.21.7+): one text field, Save
 * and Cancel.
 *
 * <p>The name comes back from the client, so nothing about it is trusted: it is normalised and
 * checked by {@link SetNameRules} on the server whatever the field's own length limit says, and
 * it is escaped before it is rendered anywhere. A refused name shows the dialog again with the
 * reason and what was typed; Cancel reopens the editor with its pieces intact.
 *
 * <p><b>Callback options are set on purpose.</b> {@code ClickCallback.Options} defaults to a
 * single use and a 12-hour lifetime. One use is right here — each showing builds a fresh dialog
 * with fresh callbacks, so a button can never fire twice — but twelve hours would keep the
 * captured pieces and the player reachable long after the dialog is gone, so the lifetime is the
 * configured {@code sets.name-dialog-lifetime-seconds}. Escape always closes the dialog, so a
 * dialog whose buttons have expired can still be left.
 */
final class NameDialog {

    private static final String INPUT = "name";

    private final MenuManager menus;
    private final Supplier<Settings> settings;

    NameDialog(MenuManager menus, Supplier<Settings> settings) {
        this.menus = menus;
        this.settings = settings;
    }

    /**
     * Shows the dialog. Player thread only.
     *
     * @param pieces   what the editor held; saved under the name on confirm
     * @param initial  what the field starts with — the refused name, on a second showing
     * @param errorKey the lang key of the reason the last name was refused, or null
     */
    void ask(Player player, List<EditorMenu.Piece> pieces, int mainPage, String initial, @Nullable String errorKey) {
        MessageService messages = menus.messages();
        int maxLength = settings.get().nameMaxLength();
        ClickCallback.Options options = ClickCallback.Options.builder()
                .uses(1)
                .lifetime(Duration.ofSeconds(settings.get().dialogLifetimeSeconds()))
                .build();

        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(messages.build("dialog.body",
                "pieces", String.valueOf(pieces.size()), "max", String.valueOf(maxLength))));
        if (errorKey != null) {
            body.add(DialogBody.plainMessage(messages.build(errorKey, "max", String.valueOf(maxLength))));
        }

        DialogInput input = DialogInput.text(INPUT, messages.build("dialog.input_label"))
                .maxLength(maxLength)
                .initial(initial.length() > maxLength ? initial.substring(0, maxLength) : initial)
                .build();

        ActionButton save = ActionButton.builder(messages.build("dialog.save"))
                .action(DialogAction.customClick((response, audience) -> {
                    if (audience instanceof Player clicker) {
                        String typed = response.getText(INPUT);
                        // The callback's thread is the server's business; the player's is ours.
                        menus.run(clicker, () -> confirm(clicker, pieces, mainPage, typed));
                    }
                }, options))
                .build();
        ActionButton cancel = ActionButton.builder(messages.build("dialog.cancel"))
                .action(DialogAction.customClick((response, audience) -> {
                    if (audience instanceof Player clicker) {
                        menus.run(clicker, () -> reopenEditor(clicker, pieces, mainPage));
                    }
                }, options))
                .build();

        Dialog dialog = Dialog.create(factory -> factory.empty()
                .base(DialogBase.builder(messages.build("dialog.title"))
                        .canCloseWithEscape(true)
                        .body(body)
                        .inputs(List.of(input))
                        .build())
                .type(DialogType.confirmation(save, cancel)));
        player.showDialog(dialog);
    }

    private void confirm(Player player, List<EditorMenu.Piece> pieces, int mainPage, @Nullable String typed) {
        // The dialog may have been answered minutes after it was shown.
        if (!player.hasPermission(MenuManager.ADMIN)) {
            return;
        }
        String name = SetNameRules.normalize(typed);
        SetNameRules.Result verdict = SetNameRules.check(name, settings.get().nameMaxLength(), menus.locks()::exists);
        if (verdict != SetNameRules.Result.OK) {
            ask(player, pieces, mainPage, name, errorKey(verdict));
            return;
        }
        if (!menus.locks().create(new LockedSet(name, EditorMenu.references(pieces)))) {
            // The name was free a moment ago, so this is the file refusing to be written.
            menus.messages().sendPrefixed(player, "error.save_failed");
            reopenEditor(player, pieces, mainPage);
            return;
        }
        menus.messages().sendPrefixed(player, "set.created", "name", menus.messages().escape(name),
                "pieces", String.valueOf(pieces.size()));
        // The new set is the last one: open the page it is on.
        int lastPage = Pager.pageCount(menus.locks().sets().size(), Menu.PAGE_SIZE) - 1;
        menus.open(player, new MainMenu(menus, player, lastPage));
    }

    private void reopenEditor(Player player, List<EditorMenu.Piece> pieces, int mainPage) {
        if (player.hasPermission(MenuManager.ADMIN)) {
            menus.open(player, new EditorMenu(menus, player, null, pieces, mainPage));
        }
    }

    private static String errorKey(SetNameRules.Result verdict) {
        return switch (verdict) {
            case EMPTY -> "name.empty";
            case TOO_LONG -> "name.too_long";
            case BAD_CHARACTERS -> "name.bad_characters";
            case DUPLICATE -> "name.duplicate";
            case OK -> throw new IllegalArgumentException("OK is not an error");
        };
    }
}
