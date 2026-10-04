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
[![Nexo](https://img.shields.io/badge/Nexo-supported-289BD0?style=for-the-badge)](https://nexomc.com)
[![MythicMobs](https://img.shields.io/badge/MythicMobs-supported-8A63D2?style=for-the-badge)](https://mythiccraft.io)
[![License](https://img.shields.io/badge/license-proprietary-B22222?style=for-the-badge)](#license)

</div>

---

> [!IMPORTANT]
> <a name="status"></a>**Status:** this repository holds the design only. No code has been
> written yet, so nothing below is implemented or tested.

## What it will do

- **Lock by item ID.** A locked piece is recognised by its Nexo or MythicMobs ID, not by its
  name or lore, so the lock survives a restyle of the item.
- **Block every way to change enchantments.** Enchanting table, anvil (books and combining),
  grindstone and smithing table.
- **Tell the player.** A blocked attempt sends a configurable message instead of failing
  silently.
- **Keep repairs optional.** Anvil repairs that leave enchantments untouched can be allowed or
  blocked in `config.yml`.

## Managing sets

Sets are managed from a menu, with no file editing:

1. The main menu lists every locked set and how many pieces it holds.
2. **Create set** opens an editor. Clicking a piece in your inventory adds a copy of it to the
   set; your own items never leave your inventory.
3. **Save** opens a dialog asking for the set's name, then returns to the main menu with the
   new set listed.

Items that do not come from Nexo or MythicMobs are refused in the editor, because locking a
plain vanilla item would lock every item of that material on the server.

## Requirements

| | |
|---|---|
| Server | PaperMC 1.21.7 or newer (the naming dialog uses Paper's Dialog API) |
| Java | 21 |
| Optional | [Nexo](https://nexomc.com), [MythicMobs](https://mythiccraft.io) — at least one is needed for the plugin to have anything to lock |

## Known limits

- `/enchant` and third-party enchantment plugins do not go through the vanilla enchanting
  events, so they are not covered by the listeners above.

## License

Proprietary — © PolaroidStudio. All rights reserved. The source is public for reference; it is
not licensed for redistribution or reuse.
