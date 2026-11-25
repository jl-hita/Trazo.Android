# Capturas para la landing (Angular)

Capturas reales de la app (Pixel 10 Pro, 1280×2856, tema oscuro) en español (`es/`) e inglés (`en/`), con los mismos
datos de demostración en ambos idiomas. Los ejercicios y sus imágenes son los del catálogo real; el usuario, el plan y
la semana son ficticios (servidor local `tools/mock-api`), así que no hay datos de ninguna cuenta real.

| Archivo | Pantalla |
|---|---|
| `01-login` | Inicio de sesión |
| `02-plan` | Semana actual con sus sesiones |
| `03-session` | Prescripción de una sesión (series, reps, RIR, descanso, carga objetivo, imagen) |
| `04-workout` | Registro de series con temporizador de descanso |
| `05-plan-overview` | Resumen del plan (fases del macrociclo, sin sesiones futuras) |
| `06-catalog` | Catálogo con búsqueda |
| `07-catalog-filters` | Filtros del catálogo |
| `08-exercise-detail` | Ficha del ejercicio con imágenes |
| `09-account` | Cuenta, suscripción y seguridad |
| `10-wizard-goal` | Asistente de creación del plan: objetivo, nivel, días y reparto |
| `11-wizard-flexible` | Asistente con «Sin días fijos» (ciclo de sesiones) |
| `12-wizard-details` | Asistente: descargas, duración, material y molestias |
| `13-plan-next` | Semana con la tarjeta «Siguiente sesión» y la recuperación |
| `14-plan-sequence` | Plan sin fechas (ciclos y sesiones) |
| `15-block` | El bloque completo |
| `16-session` | Sesión con la carga objetivo |
| `17-workout` | Registro con «última vez» y temporizador |
| `18-adaptation` | Qué ha ajustado el motor tras la sesión |
| `19-progress` | Pestaña Progreso |

Sugerencias para Angular: servir los PNG desde `assets/screenshots/{lang}/` y usar `<img loading="lazy" width="1280" height="2856">`
con `srcset` generado al construir (los originales pesan ~100–200 KB). Para un marco de dispositivo, recortar la barra de
estado del emulador (los ~110 px superiores) o usar un mockup CSS.

## Otros idiomas

Para los demás idiomas la landing usa nueve de estas pantallas (10, 11, 12, 14, 15, 16, 17, 18 y 19) y las toma un
script, con el emulador arrancado y la compilación de depuración instalada (pasos 2 de abajo):

    python tools/mock-api/capture.py de --webp ../Trazo/admin/Trazo.Admin.Web/public/landing/screenshots

Los datos de demostración de cada idioma están en `tools/mock-api/lang/{código}.json`. Los originales quedan en
`store/google-play/screenshots/{código}/` (sin versionar) y las copias WebP, en la landing. No dejes otro `server.py`
abierto en el mismo puerto: en Windows ambos escuchan y contesta el antiguo.

## Regenerarlas

1. `python tools/mock-api/server.py --lang es` (o `en`; `--noplan` para el asistente de creación; `--mode sequence` para el plan sin fechas).
2. `./gradlew assembleDebug -PapiBaseUrl=http://10.0.2.2:8099/`, instalar en el emulador y entrar con cualquier correo/contraseña.
3. Cambiar el idioma de la app: `adb shell cmd locale set-app-locales com.trazo.android --locales en`.

En las compilaciones normales (y en release) las pantallas con la rutina bloquean las capturas (`FLAG_SECURE`).
