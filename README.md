<div align="center">

**English** · [Español](README.es.md)

# 🔒 PolaroidEnchantLock

**Custom sets that stay exactly as they were made.**

Lock Nexo and MythicMobs item sets on PaperMC so their enchantments cannot be changed.
Sets are registered from an in-game menu, and a player who tries to change a locked piece is
told why it did not work.

[![PaperMC](https://img.shields.io/badge/PaperMC-1.21.7%2B%20%7C%2026.x-0288D1?style=for-the-badge&logo=minecraft&logoColor=white)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-21-E76F00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Release](https://img.shields.io/badge/release-v1.0.0--b1-2EA043?style=for-the-badge)](https://github.com/PolaroidStudio/PolaroidEnchantLock/releases)
<br>
[![Tests](https://img.shields.io/badge/tests-52%20unit-2EA043?style=for-the-badge)](docs/testing.md)
[![Live server](https://img.shields.io/badge/live%20server-not%20verified%20yet-B22222?style=for-the-badge)](docs/testing.md#not-verified-on-a-live-server)
[![Items](https://img.shields.io/badge/items-Nexo%20%7C%20MythicMobs-8A63D2?style=for-the-badge)](docs/install.md)
[![License](https://img.shields.io/badge/license-proprietary-B22222?style=for-the-badge)](LICENSE)

[**Download**](https://github.com/PolaroidStudio/PolaroidEnchantLock/releases) ·
[**Install**](docs/install.md) ·
[**Commands**](docs/commands.md) ·
[**Configuration**](docs/configuration.md) ·
[**Integrations**](docs/integrations.md)

</div>

---

> [!IMPORTANT]
> **Status:** the plugin builds and its 52 unit tests pass, but it has **not been run on a live
> server yet**. What that leaves unverified is listed in
> [testing.md](docs/testing.md#not-verified-on-a-live-server).

## ✨ What it does

<table>
<tr>
<td width="50%" valign="top">

### 🔒 Locked by item ID
A locked piece is recognised by its Nexo or MythicMobs ID, not by its name or lore, so the
lock survives a restyle of the item. Plain vanilla items are never locked.

</td>
<td width="50%" valign="top">

### 🚫 Every vanilla path blocked
Enchanting table, anvil (books and combining), grindstone, smithing table and repairing two
pieces in a crafting grid. A locked piece cannot be used up as the second item either.

</td>
</tr>
<tr>
<td width="50%" valign="top">

### 📋 Sets managed in game
`/enchantlock` lists every locked set. Creating one opens an editor where clicking a piece in
your inventory adds a copy of it; your own items never leave your inventory. A dialog then asks
for the set's name.

</td>
<td width="50%" valign="top">

### 💬 The player is told
A blocked attempt sends a configurable message when the player clicks the blocked result, not
while they are still arranging items, and at most once per cooldown.

</td>
</tr>
<tr>
<td width="50%" valign="top">

### 🔧 Repairs stay optional
Anvil repairs and renames that leave the enchantments untouched are allowed by default and can
be switched off with one option.

</td>
<td width="50%" valign="top">

### 🔌 Other Polaroid plugins
PolaroidEnchant's anvil, virtual anvil and `/penchant` are covered. PolaroidEnchanter's menus
are **not covered yet**; see [integrations](docs/integrations.md) for why and the workaround.

</td>
</tr>
</table>

## 📦 Requirements

| | |
|---|---|
| Server | Paper **1.21.7** or newer, including 26.x (the naming dialog uses Paper's Dialog API) |
| Java | 21 |
| Optional | [Nexo](https://nexomc.com), [MythicMobs](https://mythiccraft.io) — at least one is needed for the plugin to have anything to lock |

Drop the jar in `plugins/` and start the server. Full steps in [install.md](docs/install.md).

## ⌨️ Commands

| Command | Does |
|---|---|
| `/enchantlock` (alias `/elock`) | Open the locked-set menu: create, view, edit and delete sets |
| `/enchantlock reload` | Reload `config.yml`, the language file, `menus.yml` and `sets.yml` |

Permissions are in [commands.md](docs/commands.md).

## ⚠️ Known limits

- PolaroidEnchanter's Enchanter, Disenchanter and Anvil menus are not protected with its
  current version. See [integrations.md](docs/integrations.md#polaroidenchanter).
- `/enchant`, `/item`, `/give` and third-party enchantment plugins do not go through the
  vanilla enchanting events, so they are not covered.
- The crafter block is not covered, only a player's crafting grid and the crafting table.
- An item whose plugin is uninstalled no longer reports an ID and is treated as not locked
  until the plugin is back.

## 🧪 How it was checked

- **52 unit tests** on the set registry, item references, name rules, the repair-or-enchant
  decision, `sets.yml` and the shipped language and menu files.
- **No live-server run yet.** The menus, the naming dialog, the event listeners and the Nexo,
  MythicMobs and Polaroid hooks have never been exercised in game. The checklist for that run
  is in [testing.md](docs/testing.md).

## 📚 Documentation

| Doc | Content |
|---|---|
| [install.md](docs/install.md) | Requirements, installation, files created |
| [configuration.md](docs/configuration.md) | Every config key, the menus and language files, what reload covers |
| [commands.md](docs/commands.md) | Commands and permissions |
| [integrations.md](docs/integrations.md) | Nexo, MythicMobs, PolaroidEnchant and PolaroidEnchanter: what is covered and what is not |
| [testing.md](docs/testing.md) | Unit tests and the live-server checklist |

---

<div align="center">

Made by **PolaroidStudio** · Proprietary, all rights reserved

</div>
