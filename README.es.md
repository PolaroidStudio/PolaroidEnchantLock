<div align="center">

[English](README.md) · **Español**

# 🔒 PolaroidEnchantLock

**Sets personalizados que se quedan tal como se crearon.**

Bloquea sets de ítems de Nexo y MythicMobs en PaperMC para que sus encantamientos no se puedan
cambiar. Los sets se registran desde un menú dentro del juego, y al jugador que intenta
modificar una pieza bloqueada se le explica por qué no funcionó.

[![PaperMC](https://img.shields.io/badge/PaperMC-1.21.7%2B%20%7C%2026.x-0288D1?style=for-the-badge&logo=minecraft&logoColor=white)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-21-E76F00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Release](https://img.shields.io/badge/release-v1.0.0--b1-2EA043?style=for-the-badge)](https://github.com/PolaroidStudio/PolaroidEnchantLock/releases)
<br>
[![Tests](https://img.shields.io/badge/tests-56%20unitarios-2EA043?style=for-the-badge)](docs/testing.md)
[![Servidor real](https://img.shields.io/badge/servidor%20real-sin%20verificar-B22222?style=for-the-badge)](docs/testing.md#not-verified-on-a-live-server)
[![Ítems](https://img.shields.io/badge/%C3%ADtems-Nexo%20%7C%20MythicMobs-8A63D2?style=for-the-badge)](docs/install.md)
[![Licencia](https://img.shields.io/badge/licencia-propietaria-B22222?style=for-the-badge)](LICENSE)

[**Descargar**](https://github.com/PolaroidStudio/PolaroidEnchantLock/releases) ·
[**Instalación**](docs/install.md) ·
[**Comandos**](docs/commands.md) ·
[**Configuración**](docs/configuration.md) ·
[**Integraciones**](docs/integrations.md)

</div>

---

> [!IMPORTANT]
> **Estado:** el plugin compila y sus 56 tests unitarios pasan, pero **todavía no se ha
> ejecutado en un servidor real**. Lo que queda sin verificar está en
> [testing.md](docs/testing.md#not-verified-on-a-live-server).

La documentación de `docs/` está en inglés.

## ✨ Qué hace

<table>
<tr>
<td width="50%" valign="top">

### 🔒 Bloqueo por ID de ítem
Una pieza bloqueada se reconoce por su ID de Nexo o MythicMobs, no por su nombre ni su lore,
así que el bloqueo sobrevive a un cambio de aspecto del ítem. Los ítems vanilla nunca se
bloquean.

</td>
<td width="50%" valign="top">

### 🚫 Todas las vías vanilla bloqueadas
Mesa de encantamientos, yunque (libros y combinar), piedra de afilar, mesa de herrería y la
reparación de dos piezas en la mesa de crafteo. Una pieza bloqueada tampoco se puede consumir
como segundo ítem.

</td>
</tr>
<tr>
<td width="50%" valign="top">

### 📋 Sets gestionados en el juego
`/enchantlock` lista todos los sets bloqueados. Al crear uno se abre un editor donde hacer clic
en una pieza de tu inventario añade una copia; tus ítems nunca salen de tu inventario. Después
un diálogo pide el nombre del set.

</td>
<td width="50%" valign="top">

### 💬 El jugador recibe un aviso
Un intento bloqueado envía un mensaje configurable cuando el jugador hace clic en el resultado
bloqueado, no mientras coloca los ítems, y como máximo una vez por cada tiempo de espera.

</td>
</tr>
<tr>
<td width="50%" valign="top">

### 🔧 Reparar es opcional
Reparar y renombrar en el yunque sin tocar los encantamientos está permitido por defecto y se
puede desactivar con una opción.

</td>
<td width="50%" valign="top">

### 🔌 Otros plugins de Polaroid
El yunque, el yunque virtual y `/penchant` de PolaroidEnchant están cubiertos. Los menús de
PolaroidEnchanter **todavía no**; en [integraciones](docs/integrations.md) se explica por qué y
cómo evitarlo.

</td>
</tr>
</table>

## 📦 Requisitos

| | |
|---|---|
| Servidor | Paper **1.21.7** o superior, incluido 26.x (el diálogo del nombre usa la Dialog API de Paper) |
| Java | 21 |
| Opcional | [Nexo](https://nexomc.com), [MythicMobs](https://mythiccraft.io) — hace falta al menos uno para que el plugin tenga algo que bloquear |

Coloca el jar en `plugins/` e inicia el servidor. Los pasos completos están en
[install.md](docs/install.md).

## ⌨️ Comandos

| Comando | Qué hace |
|---|---|
| `/enchantlock` (alias `/elock`) | Abre el menú de sets bloqueados: crear, ver, editar y borrar sets |
| `/enchantlock reload` | Recarga `config.yml`, el archivo de idioma, `menus.yml` y `sets.yml` |

Los permisos están en [commands.md](docs/commands.md).

## ⚠️ Límites conocidos

- Los menús Enchanter, Disenchanter y Anvil de PolaroidEnchanter no quedan protegidos con su
  versión actual. Consulta [integrations.md](docs/integrations.md#polaroidenchanter).
- `/enchant`, `/item`, `/give` y los plugins de encantamientos de terceros no pasan por los
  eventos vanilla de encantamiento, así que no están cubiertos.
- El bloque crafter no está cubierto, solo la cuadrícula de crafteo del jugador y la mesa de
  crafteo.
- Un ítem cuyo plugin se desinstala deja de informar su ID y se trata como no bloqueado hasta
  que el plugin vuelve.

## 🧪 Cómo se comprobó

- **56 tests unitarios** sobre el registro de sets, las referencias de ítems, las reglas de
  nombres, la decisión entre reparar y encantar, `sets.yml` y los archivos de idioma y menús
  incluidos.
- **Sin prueba en servidor real todavía.** Los menús, el diálogo del nombre, los listeners y
  los hooks de Nexo, MythicMobs y Polaroid nunca se han ejecutado en el juego. La lista de
  comprobación para esa prueba está en [testing.md](docs/testing.md).

## 📚 Documentación

| Documento | Contenido |
|---|---|
| [install.md](docs/install.md) | Requisitos, instalación, archivos creados |
| [configuration.md](docs/configuration.md) | Cada clave de configuración, los archivos de menús e idioma, qué cubre la recarga |
| [commands.md](docs/commands.md) | Comandos y permisos |
| [integrations.md](docs/integrations.md) | Nexo, MythicMobs, PolaroidEnchant y PolaroidEnchanter: qué está cubierto y qué no |
| [testing.md](docs/testing.md) | Tests unitarios y lista de comprobación en servidor real |

---

<div align="center">

Hecho por **PolaroidStudio** · Propietario, todos los derechos reservados

</div>
