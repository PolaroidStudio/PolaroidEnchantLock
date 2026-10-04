# Integrations

## Item plugins

| Plugin | Stored as | How a piece is recognised |
|---|---|---|
| Nexo | `nexo:<id>` | `NexoItems.idFromItem(item)` |
| MythicMobs | `mythicmobs:<id>` | `MythicBukkit.inst().getItemManager().getMythicTypeFromItem(item)` |

Both are optional. Checked against Nexo 1.8.0 and Mythic-Dist 5.10.0; newer versions have not
been checked.

## Other Polaroid enchantment plugins

Neither plugin is a dependency. Their menus are recognised by the class of the inventory they
open and their command by its name, so PolaroidEnchantLock runs the same with or without them.
Neither exposes an API or an event another plugin can cancel, so both are guarded from the
outside. Each hook can be switched off under `hooks:` in `config.yml`.

### PolaroidEnchant

| Path | Covered |
|---|---|
| Its enchanted books applied on a real anvil | **Yes.** This is the normal anvil. Its custom enchantments are stored outside the vanilla enchantment list, so the anvil rule compares the whole item, not only that list |
| Its virtual anvil for Bedrock players | **Yes.** A click that involves a locked piece is cancelled while that menu is open, and the menu skips cancelled clicks |
| `/penchant enchant` and `/penchant remove` | **Yes**, from a player or the console: refused while the target holds a locked piece in the main hand |

### PolaroidEnchanter

| Path | Covered |
|---|---|
| A real anvil it does not take over | **Yes.** This is the normal anvil |
| Its Enchanter, Disenchanter and Anvil menus | **No, not with the current PolaroidEnchanter** |

PolaroidEnchantLock cancels every click that would move a locked piece into those menus, but
PolaroidEnchanter's menu listener acts on a click even after another plugin cancelled it, so
the piece is stored and can be enchanted or disenchanted there. When that happens the player is
not told the piece is locked, and the console warns once that the cancellation was ignored.

Two ways to close the gap:

- **In PolaroidEnchanter** (the real fix): have its menu listener return when the click event
  is already cancelled. From then on this hook works with no change here.
- **Today, by configuration**: in PolaroidEnchanter's `enchantables.yml`, list the locked
  pieces in a `custom` group with `slots: 0` (as `nexo-<id>` / `mythicmobs-<id>`). An item
  with no slots is refused by its item slot. This does not stop a locked piece being chosen as
  the *second* item in its Anvil menu, where it would be consumed.

## What another enchanting plugin needs to expose

The outside guards above depend on class and command names, which can change. The robust
integration is a cancellable Bukkit event fired **before** the plugin stores or modifies an
item, on the player's thread:

- the player,
- the item about to be stored, enchanted, disenchanted or consumed,
- the action (`STORE`, `ENCHANT`, `DISENCHANT`, `ANVIL_INPUT`),
- `Cancellable`, with the plugin doing nothing when it comes back cancelled.
