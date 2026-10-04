<div align="center">

# PolaroidEnchantLock

**Custom armor sets that stay exactly as they were made.**

Lock Nexo and MythicMobs item sets on PaperMC so they cannot be enchanted, disenchanted or
modified. Sets are registered from an in-game menu, and a player who tries to change a locked
piece is told why it did not work.

[![PaperMC](https://img.shields.io/badge/PaperMC-1.21.7%2B-0288D1?style=for-the-badge&logo=minecraft&logoColor=white)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-21-E76F00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Status](https://img.shields.io/badge/status-in%20development-F2C42F?style=for-the-badge)](#status)
<br>
[![Build](https://github.com/PolaroidStudio/PolaroidEnchantLock/actions/workflows/build.yml/badge.svg)](https://github.com/PolaroidStudio/PolaroidEnchantLock/actions/workflows/build.yml)
<br>
[![Tests](https://img.shields.io/badge/tests-52%20unit-2EA043?style=for-the-badge)](#status)
[![Live server](https://img.shields.io/badge/live%20server-not%20verified%20yet-B22222?style=for-the-badge)](#status)
<br>
[![Nexo](https://img.shields.io/badge/Nexo-supported-289BD0?style=for-the-badge)](https://nexomc.com)
[![MythicMobs](https://img.shields.io/badge/MythicMobs-supported-8A63D2?style=for-the-badge)](https://mythiccraft.io)
[![License](https://img.shields.io/badge/license-proprietary-B22222?style=for-the-badge)](#license)

</div>

---

> [!IMPORTANT]
> <a name="status"></a>**Status:** the plugin is written, builds, and its rules are covered by
> 52 unit tests (`./gradlew build`). **It has not been run on a live server.** The tests cover
> the logic that needs no server — the set registry, item references, name rules, the
> repair-or-enchant decision, `sets.yml` and the shipped language and menu files. Everything
> that needs one is untested: the menus, the naming dialog, the event listeners, the Nexo and
> MythicMobs lookups, and both PolaroidEnchant hooks below. Treat it as unverified until
> someone has tried it in game.

## What it does

- **Lock by item ID.** A locked piece is recognised by its Nexo or MythicMobs ID, not by its
  name or lore, so the lock survives a restyle of the item.
- **Block every way to change enchantments.** Enchanting table, anvil (books and combining),
  grindstone, smithing table, and repairing two pieces in a crafting grid (which, like the
  grindstone, returns the piece without its enchantments).
- **Tell the player.** A blocked attempt sends a configurable message instead of failing
  silently — when the player clicks the blocked result, not while they are still arranging
  items, and at most once every `messages.cooldown-milliseconds`.
- **Keep repairs optional.** Anvil repairs and renames that leave enchantments untouched can be
  allowed or blocked in `config.yml` (`anvil.allow-repair-and-rename`, allowed by default).

A locked piece can also not be used *up* on an anvil or grindstone: placed in the second slot
it would be consumed and its enchantments moved onto another item.

## Managing sets

Sets are managed from a menu, with no file editing:

1. The main menu lists every locked set and how many pieces it holds.
2. **Create set** opens an editor. Clicking a piece in your inventory adds a copy of it to the
   set; your own items never leave your inventory. Clicking the copy takes it out again.
3. **Save** opens a dialog asking for the set's name, then returns to the main menu with the
   new set listed.

Clicking a set opens it: its pieces are listed, **Edit items** reopens the editor on it, and
**Delete set** removes it after a second, confirming click.

Items that do not come from Nexo or MythicMobs are refused in the editor, because locking a
plain vanilla item would lock every item of that material on the server.

A set name may hold letters, digits, spaces, `_` and `-`, up to `sets.name-max-length`
characters (24 by default, 32 at most), and must not repeat an existing name.

Sets are stored in `plugins/PolaroidEnchantLock/sets.yml` as a name and a list of
`nexo:<id>` / `mythicmobs:<id>` references. The file can be edited by hand; run
`/enchantlock reload` afterwards.

## Commands and permissions

| Command | What it does | Permission |
|---|---|---|
| `/enchantlock` (alias `/elock`) | Opens the locked-set menu | `polaroidenchantlock.admin` |
| `/enchantlock reload` | Re-reads `config.yml`, the language file, `menus.yml` and `sets.yml` | `polaroidenchantlock.reload` |

Both permissions default to operators. There is no bypass permission: a locked piece is locked
for everyone.

## Configuration

| File | Holds |
|---|---|
| `config.yml` | Language, the anvil repair option, the message cooldown, name limits, the two hooks |
| `lang/messages_en.yml` | Every chat message and the dialog's text, in MiniMessage |
| `menus.yml` | Titles, buttons, lore and the click sound of the menus |
| `sets.yml` | The locked sets |

## Other Polaroid enchantment plugins

Neither plugin is a dependency. Their menus are recognised by the class of the inventory they
open and their command by its name, so PolaroidEnchantLock runs the same with or without them.
Neither exposes an API or an event another plugin can cancel, so both are guarded from the
outside. Each hook can be switched off under `hooks:` in `config.yml`.

### PolaroidEnchant

| Path | Covered |
|---|---|
| Its enchanted books applied on a real anvil | **Yes.** This is the normal anvil. Its custom enchantments are stored outside the vanilla enchantment list, so the anvil rule compares the whole item, not only that list. |
| Its virtual anvil for Bedrock players | **Yes.** A click that involves a locked piece is cancelled while that menu is open, and the menu skips cancelled clicks. |
| `/penchant enchant` and `/penchant remove` | **Yes**, from a player or the console: refused while the target holds a locked piece in the main hand. |
| The effects of its custom enchantments | Not applicable. They only apply to enchantments the piece already has. |

### PolaroidEnchanter

| Path | Covered |
|---|---|
| A real anvil it does not take over | **Yes.** This is the normal anvil. |
| Its Enchanter, Disenchanter and Anvil menus | **No, not with the current PolaroidEnchanter.** |

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

## Requirements

| | |
|---|---|
| Server | PaperMC 1.21.7 or newer (the naming dialog uses Paper's Dialog API) |
| Java | 21 |
| Optional | [Nexo](https://nexomc.com), [MythicMobs](https://mythiccraft.io) — at least one is needed for the plugin to have anything to lock |

Tasks run on each player's own scheduler and the shared state is thread-safe, so the plugin
declares Folia support. It has not been run on Folia either.

## Building

```
./gradlew build
```

Runs the unit tests and writes `build/libs/PolaroidEnchantLock-<version>.jar`. A JDK 21 must be
installed. Nexo and MythicMobs are compiled against but never bundled.

## Known limits

- `/enchant`, `/item` and `/give`, and third-party enchantment plugins other than the two
  above, do not go through the vanilla enchanting events, so they are not covered.
- The crafter block is not covered: only a player's own crafting grid and the crafting table.
- A piece is locked by its ID. An item whose plugin is uninstalled no longer reports an ID and
  is treated as not locked until the plugin is back.
- PolaroidEnchanter's menus, as described above.

## License

Proprietary — © PolaroidStudio. All rights reserved. The source is public for reference; it is
not licensed for redistribution or reuse.
