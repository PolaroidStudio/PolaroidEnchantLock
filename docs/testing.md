# Testing

## Unit tests

`./gradlew test` runs 56 tests, none of which needs a server:

| Test | Covers |
|---|---|
| `ItemReferenceTest` | Parsing and formatting `nexo:<id>` / `mythicmobs:<id>` references |
| `SetRegistryTest` | Adding, replacing and removing sets, and the locked-ID lookup |
| `SetNameRulesTest` | Allowed characters, length cap, duplicates |
| `LockRulesTest` | The decision between an allowed repair or rename and a blocked enchantment change |
| `SmallRulesTest` | Message cooldown, paging, foreign command matching |
| `SetStoreTest` | Reading and writing `sets.yml`, including damaged files |
| `ShippedFilesTest` | The language and menu files shipped in the jar |
| `PolaroidEnchantApiHookTest` | Binding PolaroidEnchant's events by reflection: an event of the wrong shape is rejected when binding, an empty one is left alone |

They run in CI on every push, before the jar is built.

## Not verified on a live server

Nothing below has been exercised in game:

- The three menus and their click, drag and close handling.
- The naming dialog, including showing it right after the editor closes.
- Every event listener: enchanting table, anvil, grindstone, smithing table, crafting grid.
- The Nexo and MythicMobs lookups against real items.
- The PolaroidEnchant and PolaroidEnchanter guards.
- Folia. The plugin declares support because every task uses the entity scheduler, but it has
  never run there.

## Live-server checklist

With Nexo or MythicMobs installed and one set created:

**Set management**

1. `/enchantlock` opens; **Create set** opens the editor.
2. Clicking a custom item in your inventory adds a copy; the original stays where it was.
3. Clicking a vanilla item is refused with a message.
4. **Save** opens the dialog; a valid name saves and the set appears in the main menu.
5. An empty, too long, duplicate or badly charactered name is refused.
6. Shift-click, number keys, drag and double-click in each menu move nothing.
7. Closing a menu, relogging and dropping items leaves no menu item in the inventory.
8. **Delete set** needs the confirming click, and the set is gone after `/enchantlock reload`.

**Locks** — for each, the item must be unchanged afterwards and the message must arrive once:

1. Enchanting table: a locked piece gets no offers.
2. Anvil with an enchanted book.
3. Anvil combining two locked pieces with different enchantments.
4. Anvil repair with the material: allowed while `allow-repair-and-rename` is `true`, blocked
   when `false`.
5. Anvil rename: same as repair.
6. Locked piece as the second anvil item, onto an unlocked one.
7. Grindstone, in either slot.
8. Smithing table upgrade and trim.
9. Two damaged locked pieces in the crafting grid.

**Hooks**

1. PolaroidEnchant: book on a real anvil, the virtual anvil, `/penchant enchant` and
   `/penchant remove` on a held locked piece.
2. PolaroidEnchanter: confirm the known gap (the piece is stored) and the single console
   warning; then confirm the `slots: 0` workaround refuses it.
