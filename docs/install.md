# Install

## Requirements

| | |
|---|---|
| Server | Paper **1.21.7** or newer, including 26.x |
| Java | 21 |

Paper 1.21.7 is the floor because the set-naming dialog uses Paper's Dialog API, which first
shipped in that version.

Optional plugins, each detected at startup:

| Plugin | Adds |
|---|---|
| Nexo | Its items can be locked, stored as `nexo:<id>` |
| MythicMobs | Its items can be locked, stored as `mythicmobs:<id>` |
| PolaroidEnchant | Locked pieces are also refused in its virtual anvil and by `/penchant` |
| PolaroidEnchanter | A guard exists but does not protect locked pieces yet (see [integrations](integrations.md#polaroidenchanter)) |

The plugin enables with none of them installed, but it has nothing to lock until Nexo or
MythicMobs is present.

## Steps

1. Stop the server.
2. Put `PolaroidEnchantLock-<version>.jar` in `plugins/`.
3. Start the server. On the first start `plugins/PolaroidEnchantLock/` is created with
   `config.yml`, `menus.yml` and `lang/messages_en.yml`. `sets.yml` appears when the first set
   is saved.
4. Give `polaroidenchantlock.admin` to whoever manages the sets (see [commands](commands.md)).
5. Run `/enchantlock`, click **Create set**, click the pieces in your inventory, **Save**, and
   type the set's name.

No database and no downloads: the jar bundles nothing and needs no internet.

## Upgrading

Replace the jar and restart. `sets.yml` is kept as it is.

## Building from source

```
./gradlew build
```

Runs the unit tests and writes `build/libs/PolaroidEnchantLock-<version>.jar`. A JDK 21 must be
installed. Nexo and MythicMobs are compiled against but never bundled.
