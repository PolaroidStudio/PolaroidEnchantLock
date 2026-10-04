package me.juancayc.polaroidenchantlock.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.juancayc.polaroidenchantlock.PolaroidEnchantLockPlugin;
import me.juancayc.polaroidenchantlock.menu.MenuManager;
import me.juancayc.polaroidenchantlock.messaging.MessageService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * {@code /enchantlock} opens the locked-set menu; {@code /enchantlock reload} re-reads the files.
 *
 * <p>Registered by hand against Paper's public Brigadier API — no shaded command framework, and
 * {@code paper-plugin.yml} carries no {@code commands:} block. A sender without the permission
 * does not see the command at all.
 */
public final class EnchantLockCommand {

    public static final String RELOAD = "polaroidenchantlock.reload";

    private final PolaroidEnchantLockPlugin plugin;
    private final MenuManager menus;
    private final MessageService messages;

    public EnchantLockCommand(PolaroidEnchantLockPlugin plugin, MenuManager menus, MessageService messages) {
        this.plugin = plugin;
        this.menus = menus;
        this.messages = messages;
    }

    public void register() {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(build(), "Manage locked item sets.", List.of("elock")));
    }

    private LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("enchantlock")
                .requires(source -> source.getSender().hasPermission(MenuManager.ADMIN)
                        || source.getSender().hasPermission(RELOAD))
                .executes(context -> openMenu(context.getSource().getSender()))
                .then(Commands.literal("reload")
                        .requires(source -> source.getSender().hasPermission(RELOAD))
                        .executes(context -> reload(context.getSource().getSender())))
                .build();
    }

    private int openMenu(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messages.sendPrefixed(sender, "command.players_only");
            return Command.SINGLE_SUCCESS;
        }
        if (!player.hasPermission(MenuManager.ADMIN)) {
            messages.sendPrefixed(player, "command.no_permission");
            return Command.SINGLE_SUCCESS;
        }
        // The command may run on any thread that owns the sender; the menu opens on the player's.
        player.getScheduler().run(plugin, task -> menus.openMain(player), null);
        return Command.SINGLE_SUCCESS;
    }

    private int reload(CommandSender sender) {
        int sets = plugin.reloadEverything();
        messages.sendPrefixed(sender, "command.reloaded", "sets", String.valueOf(sets));
        return Command.SINGLE_SUCCESS;
    }
}
