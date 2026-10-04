package me.juancayc.polaroidenchantlock.listeners;

import me.juancayc.polaroidenchantlock.config.Settings;
import me.juancayc.polaroidenchantlock.domain.LockRules;
import me.juancayc.polaroidenchantlock.lock.LockService;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.Repairable;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Keeps locked pieces out of every vanilla way of changing enchantments: the enchanting table,
 * the anvil, the grindstone, the smithing table and the crafting-grid repair (which, like the
 * grindstone, returns the item without its enchantments).
 *
 * <p><b>Two halves.</b> The {@code Prepare*} events remove the result, so the player sees an
 * empty result slot. They say nothing: they fire on every change of the inputs and every letter
 * typed into the anvil's name field. The message is sent from the click on the result slot —
 * the moment the player actually tries — and is rate-limited per player on top of that.
 *
 * <p>Whether the last preview was refused is remembered per player, because once the result has
 * been removed the click alone cannot tell "refused" from "nothing to craft".
 *
 * <p><b>Priority.</b> The {@code Prepare*} handlers run at MONITOR on purpose, against the usual
 * rule that MONITOR only observes. These events cannot be cancelled; the only way to refuse is
 * to have the last word on the result, and enchantment plugins write their own result at HIGH
 * and HIGHEST (PolaroidEnchant does exactly that). The click handler re-checks a result that is
 * somehow still there, so a plugin that writes even later is still stopped at the click.
 */
public final class VanillaLockListener implements Listener {

    private static final int ANVIL_RESULT = 2;
    private static final int GRINDSTONE_RESULT = 2;
    private static final int SMITHING_BASE = 1;
    private static final int SMITHING_RESULT = 3;
    private static final int CRAFTING_RESULT = 0;

    private final LockService locks;
    private final Supplier<Settings> settings;
    /** Players whose current preview was refused. Cleared by the next preview and on close. */
    private final Set<UUID> refused = ConcurrentHashMap.newKeySet();

    public VanillaLockListener(LockService locks, Supplier<Settings> settings) {
        this.locks = locks;
        this.settings = settings;
    }

    // ── Enchanting table ─────────────────────────────────────────────────────

    /**
     * No offers are shown for a locked piece. This is the one preview that does speak: with no
     * offers there is nothing left for the player to click, so there is no later moment to
     * explain. It fires when the item or the lapis changes, not continuously, and the message
     * is rate-limited like every other.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPrepareEnchant(PrepareItemEnchantEvent event) {
        if (locks.isLocked(event.getItem())) {
            event.setCancelled(true);
            locks.refuse(event.getEnchanter(), "blocked.enchanting_table");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        if (locks.isLocked(event.getItem())) {
            event.setCancelled(true);
            locks.refuse(event.getEnchanter(), "blocked.enchanting_table");
        }
    }

    // ── Previews ─────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        Inventory anvil = event.getInventory();
        boolean block = anvilBlocked(anvil.getItem(0), anvil.getItem(1), event.getResult());
        if (block) {
            event.setResult(null);
        }
        remember(event, block);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPrepareGrindstone(PrepareGrindstoneEvent event) {
        Inventory grindstone = event.getInventory();
        boolean block = present(event.getResult()) && LockRules.strippingBlocked(
                locks.isLocked(grindstone.getItem(0)), locks.isLocked(grindstone.getItem(1)));
        if (block) {
            event.setResult(null);
        }
        remember(event, block);
    }

    /** Only the piece being worked on matters: the template and the material are just consumed. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        boolean block = present(event.getResult()) && locks.isLocked(event.getInventory().getItem(SMITHING_BASE));
        if (block) {
            event.setResult(null);
        }
        remember(event, block);
    }

    /** Two damaged items of a kind in a crafting grid become one repaired item with no enchantments. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        boolean block = false;
        if (event.isRepair()) {
            for (ItemStack ingredient : event.getInventory().getMatrix()) {
                if (locks.isLocked(ingredient)) {
                    block = true;
                    break;
                }
            }
        }
        if (block) {
            event.getInventory().setResult(null);
        }
        remember(event, block);
    }

    private void remember(InventoryEvent event, boolean block) {
        for (HumanEntity viewer : event.getViewers()) {
            if (block) {
                refused.add(viewer.getUniqueId());
            } else {
                refused.remove(viewer.getUniqueId());
            }
        }
    }

    // ── The attempt ──────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onResultClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        InventoryType type = top.getType();
        if (event.getRawSlot() != resultSlot(type) || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        boolean refusedPreview = refused.contains(player.getUniqueId()) && involvesLocked(type, top);
        if (!refusedPreview && !stillBlocked(type, top)) {
            return;
        }
        event.setCancelled(true);
        locks.refuse(player, messageKey(type));
    }

    /**
     * The same question the preview asked, asked again of what is in the window right now. It
     * only ever finds something when another plugin put a result back after the preview.
     */
    private boolean stillBlocked(InventoryType type, Inventory top) {
        return switch (type) {
            case ANVIL -> anvilBlocked(top.getItem(0), top.getItem(1), top.getItem(ANVIL_RESULT));
            case GRINDSTONE -> present(top.getItem(GRINDSTONE_RESULT)) && LockRules.strippingBlocked(
                    locks.isLocked(top.getItem(0)), locks.isLocked(top.getItem(1)));
            case SMITHING -> present(top.getItem(SMITHING_RESULT)) && locks.isLocked(top.getItem(SMITHING_BASE));
            default -> false;
        };
    }

    /**
     * Whether a locked piece is among the inputs right now. Guards the remembered verdict
     * against going stale: if the piece was taken out and no new preview was computed, the
     * player must not be told an empty table is locked.
     */
    private boolean involvesLocked(InventoryType type, Inventory top) {
        return switch (type) {
            case ANVIL, GRINDSTONE -> locks.isLocked(top.getItem(0)) || locks.isLocked(top.getItem(1));
            case SMITHING -> locks.isLocked(top.getItem(SMITHING_BASE));
            case WORKBENCH, CRAFTING -> {
                // Slot 0 is the result; the grid follows it.
                for (int slot = 1; slot < top.getSize(); slot++) {
                    if (locks.isLocked(top.getItem(slot))) {
                        yield true;
                    }
                }
                yield false;
            }
            default -> false;
        };
    }

    private static int resultSlot(InventoryType type) {
        return switch (type) {
            case ANVIL -> ANVIL_RESULT;
            case GRINDSTONE -> GRINDSTONE_RESULT;
            case SMITHING -> SMITHING_RESULT;
            case WORKBENCH, CRAFTING -> CRAFTING_RESULT;
            default -> -1;
        };
    }

    private static String messageKey(InventoryType type) {
        return switch (type) {
            case ANVIL -> "blocked.anvil";
            case GRINDSTONE -> "blocked.grindstone";
            case SMITHING -> "blocked.smithing_table";
            default -> "blocked.crafting";
        };
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        refused.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        refused.remove(event.getPlayer().getUniqueId());
        locks.forget(event.getPlayer().getUniqueId());
    }

    // ── Reading the anvil ────────────────────────────────────────────────────

    private boolean anvilBlocked(@Nullable ItemStack base, @Nullable ItemStack addition, @Nullable ItemStack result) {
        if (!present(result)) {
            return false;
        }
        boolean baseLocked = locks.isLocked(base);
        boolean additionLocked = locks.isLocked(addition);
        if (!baseLocked && !additionLocked) {
            return false;
        }
        LockRules.AnvilAttempt attempt = new LockRules.AnvilAttempt(baseLocked, additionLocked,
                enchantments(base), enchantments(result), baseLocked && onlyRepairedOrRenamed(base, result));
        return LockRules.anvilBlocked(attempt, settings.get().allowRepairAndRename());
    }

    private static boolean present(@Nullable ItemStack item) {
        return item != null && item.getType() != Material.AIR;
    }

    /** Enchantment key to level; a book's are the ones it stores. */
    private static Map<String, Integer> enchantments(@Nullable ItemStack item) {
        Map<String, Integer> levels = new HashMap<>();
        if (!present(item)) {
            return levels;
        }
        Map<Enchantment, Integer> held = item.getItemMeta() instanceof EnchantmentStorageMeta book
                ? book.getStoredEnchants() : item.getEnchantments();
        for (Map.Entry<Enchantment, Integer> entry : held.entrySet()) {
            levels.put(entry.getKey().getKey().asString(), entry.getValue());
        }
        return levels;
    }

    /**
     * Whether the result is the base item with nothing changed but its durability, its name and
     * its prior-work cost — the three things a repair or a rename touch. Anything else that
     * differs (lore, persistent data, components) means something wrote to the item, which is
     * how a plugin that stores its enchantments outside the vanilla list shows up here.
     */
    private static boolean onlyRepairedOrRenamed(ItemStack base, ItemStack result) {
        return base.getType() == result.getType() && neutral(base).isSimilar(neutral(result));
    }

    private static ItemStack neutral(ItemStack item) {
        ItemStack copy = item.clone();
        copy.editMeta(meta -> {
            meta.displayName(null);
            if (meta instanceof Damageable damageable) {
                damageable.setDamage(0);
            }
            if (meta instanceof Repairable repairable) {
                repairable.setRepairCost(0);
            }
        });
        return copy;
    }
}
