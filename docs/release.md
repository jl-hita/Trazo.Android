# Publicar TRAZO en la prueba cerrada de Google Play

Guía paso a paso, en español. **Nada de esto se ha hecho todavía**: el repositorio solo deja la app lista para generar el AAB. No se sube nada a Play ni al servidor desde el código.

## 1. Firma de release (una sola vez)

1. Crea un keystore **fuera del repositorio** (y haz copia de seguridad: si lo pierdes tendrás que pedir a Google un restablecimiento de la clave de subida):

   ```bash
   keytool -genkeypair -v -keystore trazo-upload.jks -alias trazo -keyalg RSA -keysize 2048 -validity 10000
   ```

   Usa una clave **distinta** de la de cualquier otra app.
2. Indica a Gradle dónde está, en `local.properties` (no se sube: está en `.gitignore`), en `~/.gradle/gradle.properties` o con variables de entorno:

   | local.properties / propiedad Gradle | Variable de entorno |
   |---|---|
   | `trazoSigningKeystorePath` | `TRAZO_SIGNING_KEYSTORE_PATH` |
   | `trazoSigningKeystorePassword` | `TRAZO_SIGNING_KEYSTORE_PASSWORD` |
   | `trazoSigningKeyAlias` | `TRAZO_SIGNING_KEY_ALIAS` |
   | `trazoSigningKeyPassword` | `TRAZO_SIGNING_KEY_PASSWORD` |

   Si falta alguna, `bundleRelease` genera igualmente un AAB **sin firmar** (útil para comprobar que compila, pero Play no lo acepta).
3. En Play Console conviene activar **Play App Signing** (es lo predeterminado): tu keystore es solo la clave de *subida*.

## 2. Versión

`versionName` está en `app/build.gradle.kts` (`appVersionName`). El `versionCode` se calcula: `mayor*1.000.000 + menor*10.000 + parche*100 + buildNumber`. Para subir otra compilación de la misma versión, aumenta `buildNumber` (0–99):

```bash
./gradlew bundleRelease -PbuildNumber=1
```

Play rechaza un `versionCode` ya usado, así que cada subida necesita uno mayor.

## 3. Generar el AAB

El host de la API se fija al compilar (la compilación release exige HTTPS). Si ya lo tienes en `~/.gradle/gradle.properties` (`apiScheme`, `apiHost`, `apiPort`, `googleWebClientId`) basta con:

```bash
./gradlew bundleRelease -PapiScheme=https -PapiHost=trazostrength.duckdns.org -PapiPort=443
```

Resultado: `app/build/outputs/bundle/release/app-release.aab`. R8 (minificación y reducción de recursos) está activo; las reglas están en `app/proguard-rules.pro`. Prueba siempre una compilación release firmada en un dispositivo antes de subirla.

Ajustes opcionales: `-PbillingProductId=<id>` (producto de suscripción, por defecto `trazo_premium`, inactivo mientras el backend no exija pago).

## 4. Crear la app en Play Console

1. **Crear app**: nombre «TRAZO», idioma predeterminado español (España), tipo *App*, *Gratuita*. El paquete es `com.trazo.android`.
2. Completa **Panel → Configura tu app**: política de privacidad, acceso a la app, anuncios (ninguno), clasificación de contenido, público objetivo, seguridad de los datos, categoría (Salud y forma física) y datos de contacto.

### Política de privacidad
Debe estar en una URL pública. Indica al menos: datos tratados (abajo), finalidad (generar y adaptar el plan de entrenamiento y gestionar la cuenta), que se guardan en el servidor de TRAZO, que no se venden ni se comparten con terceros salvo Google (inicio de sesión y, en su día, pagos), cómo pedir el borrado de la cuenta y un contacto.

### Formulario de seguridad de los datos (qué recoge la app)
| Dato | Recogido | Para qué | Compartido |
|---|---|---|---|
| Correo y nombre (cuenta, o de Google) | Sí | Gestión de la cuenta, inicio de sesión | No |
| ID de usuario | Sí | Cuenta y suscripción | No |
| Salud y forma física: objetivo, nivel, lesiones/limitaciones declaradas, series, pesos, repeticiones, esfuerzo, dolor, notas | Sí | Funcionalidad de la app (planes y adaptación) | No |
| Información de compra (token de Google Play) | Solo cuando se active el cobro | Verificar la suscripción | No |
| Ubicación, contactos, fotos, audio, anuncios | No | — | — |

Marca: datos cifrados en tránsito (HTTPS), y que el usuario puede solicitar el borrado (ver apartado de cuenta del backend). Revisa las respuestas contra lo que el backend guarde realmente. La app no usa SDK de analítica ni de publicidad.

### Clasificación de contenido
Cuestionario IARC: sin violencia, sin contenido sexual, sin juegos de azar, sin contenido generado por usuarios, sin compras de objetos virtuales. Previsiblemente «PEGI 3 / Todos». Al ser una app de salud y forma física, responde con cuidado a las preguntas sobre consejos médicos: el plan es orientativo y no sustituye a un profesional (la app y la API incluyen ese aviso).

### Idiomas
La app viene en español (idioma por defecto) e inglés (`values-en`); cualquier otro idioma del dispositivo ve el español. En Play Console añade ambos idiomas a la ficha (Presencia en la tienda → Fichas de tienda personalizadas) y traduce descripción, capturas y política de privacidad. Android 13+ permite elegir el idioma de la app en Ajustes (`locales_config.xml`). El contenido que envía el servidor (planes, avisos, instrucciones de ejercicios) hoy solo está en español; los nombres de ejercicio se muestran en inglés cuando existe `nameEn`.

### Ficha de la tienda
Gráficos disponibles en `store/google-play/` (icono 512 y gráfico de funciones 1024×500). Las **capturas de pantalla** (es/en) están en `landing-screenshots/` (sirven también para la ficha de Play; mínimo 2 por idioma). Para regenerarlas usa un build debug con `-PsecureScreens=false`, porque en release las pantallas con la rutina bloquean las capturas (`FLAG_SECURE`).

## 5. Prueba cerrada

1. **Pruebas → Pruebas cerradas → Crear pista** (o usa «Alfa»).
2. **Testers**: crea una lista de correos (cuentas de Google) o un Grupo de Google y asígnala a la pista. Añade un correo de comentarios para los testers.
3. **Crear versión** → sube `app-release.aab` → nombre y notas de la versión → *Guardar* → *Revisar versión* → *Iniciar lanzamiento*. La primera revisión de Google puede tardar días.
4. Copia el **enlace de participación** de la pista y envíalo a los testers: deben aceptar la invitación con la cuenta de Google que está en la lista y luego instalar desde Play.
5. Para probar el inicio de sesión con Google, el SHA-1 de la **firma de la app de Play** (Play Console → Integridad de la app) debe estar registrado en el cliente OAuth de Android de Google Cloud (además del SHA-1 de tu clave de subida y de debug).

### Requisito de testers para cuentas personales nuevas
Google exige a las **cuentas de desarrollador personales creadas recientemente** haber hecho una prueba cerrada con un **mínimo de testers (hoy, 12) durante un mínimo de días seguidos (hoy, 14)** antes de poder solicitar el acceso a producción. Las cifras y la obligación cambian y dependen del tipo y la antigüedad de la cuenta: **comprueba en tu Play Console, en Panel → «Obtén acceso a producción», qué se te aplica**. Si te aplica, empieza cuanto antes a reclutar testers: los plazos no se pueden acortar.

## 6. Suscripciones (cuando se decida cobrar)

Hoy TRAZO es gratis: la pantalla de planes **no aparece** mientras `GET /api/v1/subscription` devuelva `enforced: false`. La librería de facturación y el permiso `com.android.vending.BILLING` ya están, de modo que la subida a prueba cerrada ya los incluye y no habrá que cambiar el manifiesto después. Para activarlo:

1. En Play Console → Monetizar → Suscripciones: crea el producto (ID igual a `BILLING_PRODUCT_ID`) con un plan base mensual/anual y una oferta de prueba gratuita de 14 días. Necesita un perfil de pagos.
2. Configura en el panel de administración de TRAZO (Ajustes → Google Play Billing) la cuenta de servicio y las notificaciones (ver `docs/subscriptions.md` del backend) y, después, activa «Exigir suscripción».
3. Recompila con `-PbillingProductId=<id real>` si el ID no es `trazo_premium`.

Las compras solo se pueden probar con una versión subida a Play (al menos a la pista interna/cerrada) y cuentas de prueba de licencia.

## 7. Lista de comprobación antes de subir

- [ ] `./gradlew testDebugUnitTest` verde.
- [ ] AAB firmado con la clave de subida y `versionCode` nuevo.
- [ ] Probado en un dispositivo real con la build release (login con Google, crear plan, registrar una sesión, modo avión + enviar después).
- [ ] Política de privacidad publicada y enlazada.
- [ ] Seguridad de los datos y clasificación de contenido enviadas.
- [ ] Lista de testers asignada a la pista cerrada.

## 8. Prueba manual (sin login de Google automatizable)

El inicio de sesión con Google necesita una cuenta real y la pantalla del selector de cuentas, así que no se puede automatizar. Procedimiento:

1. Instala la build debug (apunta a producción si `apiHost` está configurado) en un dispositivo/emulador con una cuenta de Google.
2. *Continuar con Google* → debe entrar y mostrar «Tu semana» (o «Crea tu plan» si no hay plan).
3. Crea el plan; abre una sesión; *Empezar entrenamiento*; rellena reps y marca series (arranca el descanso); *Terminar sesión* → comprueba el informe de adaptación.
4. Repite con el modo avión activado al terminar: debe avisar de que se enviará después; al recuperar la conexión, entra en *Cuenta → Enviar ahora* o al abrir *Plan*.
5. *Cuenta → Cerrar sesión* y comprueba que al volver a entrar no queda nada del plan anterior en caché.
