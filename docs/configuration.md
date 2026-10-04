# Configuration

Everything lives in `plugins/PolaroidEnchantLock/`.

| File | Holds |
|---|---|
| `config.yml` | Language, the anvil repair option, the message cooldown, name limits, the two hooks |
| `lang/messages_en.yml` | Every chat message and the dialog's text, in MiniMessage |
| `menus.yml` | Titles, buttons, lore and the click sound of the menus |
| `sets.yml` | The locked sets |

## config.yml

| Key | Default | Meaning |
|---|---|---|
| `language` | `en` | Uses `lang/messages_<language>.yml`. Only English ships; keys missing from another file fall back to English |
| `anvil.allow-repair-and-rename` | `true` | `true`: a locked piece can be repaired and renamed on an anvil as long as the result carries exactly the same enchantments. `false`: the anvil does nothing with a locked piece |
| `messages.cooldown-milliseconds` | `1500` | Least time between two "this item is locked" messages to the same player. `0` sends it on every blocked attempt |
| `sets.name-max-length` | `24` | Longest set name, in characters. Values above 32 are treated as 32 |
| `sets.name-dialog-lifetime-seconds` | `300` | How long the naming dialog's buttons keep working. Values below 30 are treated as 30 |
| `hooks.polaroidenchant` | `true` | Guard PolaroidEnchant's virtual anvil and `/penchant` |
| `hooks.polaroidenchanter` | `true` | Guard PolaroidEnchanter's menus. Does not protect yet; see [integrations](integrations.md#polaroidenchanter) |

## menus.yml

Titles, button materials, names and lore are yours to change, in MiniMessage. Where each button
sits is fixed in code. One click sound is shared by every menu (`click-sound`); leave its name
empty for silence.

## sets.yml

```yaml
sets:
  Dragon armor:
    - nexo:dragon_helmet
    - nexo:dragon_chestplate
    - mythicmobs:DragonBlade
```

The file is written by the menus and can also be edited by hand. Run `/enchantlock reload`
afterwards. A set with an invalid or repeated name, or a reference that is not `nexo:<id>` or
`mythicmobs:<id>`, is skipped with a console warning and the rest still loads.

## Translating

Copy `lang/messages_en.yml` to `lang/messages_<code>.yml`, translate it, and set `language` to
that code.

## What `/enchantlock reload` reloads

`config.yml`, the language file, `menus.yml` and `sets.yml`. Nothing needs a restart.
