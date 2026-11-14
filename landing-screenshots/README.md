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

Sugerencias para Angular: servir los PNG desde `assets/screenshots/{lang}/` y usar `<img loading="lazy" width="1280" height="2856">`
con `srcset` generado al construir (los originales pesan ~100–200 KB). Para un marco de dispositivo, recortar la barra de
estado del emulador (los ~110 px superiores) o usar un mockup CSS.

## Regenerarlas

1. `python tools/mock-api/server.py --lang es` (o `en`).
2. `./gradlew assembleDebug -PapiBaseUrl=http://10.0.2.2:8099/ -PsecureScreens=false`, instalar en el emulador y entrar con cualquier correo/contraseña.
3. Cambiar el idioma de la app: `adb shell cmd locale set-app-locales com.trazo.android --locales en`.

En las compilaciones normales (y en release) las pantallas con la rutina bloquean las capturas (`FLAG_SECURE`).
