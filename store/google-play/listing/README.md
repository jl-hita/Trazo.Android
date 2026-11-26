# Fichas de Google Play por idioma

Textos preparados de la ficha de la tienda para **todos los idiomas de la app** (29 idiomas; el flamenco usa la ficha `nl-NL`). **Aún no están subidos a Play Console.**

Cada carpeta lleva el código de idioma que usa Play Console y tres ficheros de texto plano (el mismo esquema que `fastlane supply`, por si algún día se automatiza la subida):

| Fichero | Límite de Play |
|---|---|
| `title.txt` | 30 caracteres |
| `short_description.txt` | 80 caracteres |
| `full_description.txt` | 4000 caracteres |
| `release_notes.txt` | 500 caracteres |

Para copiar y pegar a mano hay además dos ficheros conjuntos: `fichas-todos-los-idiomas.md` (todos los campos de cada idioma) y `release_notes_play.txt` (las novedades en el formato con etiquetas de idioma de Play Console, sin bosnio, que Play no ofrece). Son copias de los textos por carpeta: si cambia uno, hay que actualizarlos también.

Todos cumplen los límites (el inglés tiene un título de exactamente 30). Para volver a comprobarlo basta contar caracteres de cada fichero.

## Idiomas y códigos de Play

| App | Carpeta | App | Carpeta | App | Carpeta |
|---|---|---|---|---|---|
| English | `en-US` | Español | `es-ES` | Català | `ca` |
| Galego | `gl-ES` | Euskara | `eu-ES` | Português (Brasil) | `pt-BR` |
| Français | `fr-FR` | Italiano | `it-IT` | Deutsch | `de-DE` |
| Nederlands / Vlaams | `nl-NL` | Dansk | `da-DK` | Norsk bokmål | `no-NO` |
| Svenska | `sv-SE` | Suomi | `fi-FI` | Polski | `pl-PL` |
| Čeština | `cs-CZ` | Slovenčina | `sk` | Slovenščina | `sl` |
| Hrvatski | `hr` | Srpski (latinica) | `sr` | Bosanski | `bs` |
| Македонски | `mk-MK` | Русский | `ru-RU` | Ελληνικά | `el-GR` |
| Türkçe | `tr-TR` | العربية | `ar` | 中文（简体） | `zh-CN` |
| 日本語 | `ja-JP` | 한국어 | `ko-KR` | | |

## Cómo subirlas

1. Play Console → la app → **Crecimiento → Presencia en la tienda → Ficha principal de Play Store** → **Gestionar traducciones** (o **Fichas de tienda personalizadas** según la versión de la consola) → añadir idioma.
2. Para cada idioma, pegar `title.txt`, `short_description.txt` y `full_description.txt`.
3. Subir también las **capturas de pantalla de ese idioma** (con la app en ese idioma; mínimo 2), el gráfico de funciones y el icono. Un texto traducido con capturas en otro idioma convierte mal.
4. Hacerlo cuando la versión de la app con los idiomas ya esté publicada.

## Avisos

- **Sin revisión nativa.** Las traducciones son automáticas; conviene que las revise una persona nativa antes de publicarlas, sobre todo alemán, francés, polaco, turco, chino, japonés, coreano, árabe, ruso y los idiomas de la antigua Yugoslavia.
- **Serbio:** la carpeta `sr` está en alfabeto latino, igual que la app (`values-b+sr+Latn`). Play Console no distingue escritura: si se quiere ofrecer también el alfabeto cirílico habría que traducirlo aparte.
- **Contenido veraz:** el texto habla solo de lo que la app hace hoy (plan manual gratis, plan automático y adaptación en Premium con prueba gratuita, registro sin conexión, progreso, superseries, cambio de ejercicios, calendario flexible). Si cambia el modelo (precios, prueba, funciones), hay que revisar los 29 textos.
- **Aviso médico:** todos terminan con la nota de que las orientaciones no sustituyen a un médico ni a un entrenador.
