# Commands and permissions

## Commands

| Command | Does | Permission |
|---|---|---|
| `/enchantlock` (alias `/elock`) | Opens the locked-set menu | `polaroidenchantlock.admin` |
| `/enchantlock reload` | Re-reads `config.yml`, the language file, `menus.yml` and `sets.yml` | `polaroidenchantlock.reload` |

`/enchantlock` is for players only: it opens a menu. `reload` also works from the console.

## Permissions

| Permission | Default | Allows |
|---|---|---|
| `polaroidenchantlock.admin` | op | Open the menu and create, edit or delete sets |
| `polaroidenchantlock.reload` | op | Reload the configuration and the sets |

There is no bypass permission. A locked piece is locked for everyone, operators included.

## The menus

| Menu | What you can do |
|---|---|
| Locked sets | See every set and its piece count, page through them, **Create set** |
| Editor | Click a piece in your own inventory to add a copy to the set; click the copy to take it out; **Save** |
| A set | See its pieces, **Edit items**, **Delete set** (asks for a second, confirming click) |

Saving a new set opens a dialog with one text field for its name. A name may hold letters,
digits, spaces, `_` and `-`, up to `sets.name-max-length` characters, and must not repeat an
existing name.

Items that do not come from Nexo or MythicMobs are refused in the editor, because locking a
plain vanilla item would lock every item of that material on the server.
