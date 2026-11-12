# Configuración de TRAZO Android

La aplicación Android está en este repositorio. La API y el panel de administración Angular están en [jl-hita/Trazo](https://github.com/jl-hita/Trazo).

Esta guía cubre las propiedades de compilación disponibles actualmente, Google Sign-In, entornos de desarrollo y generación de APK/AAB. No contiene secretos reales.

## 1. Requisitos

- Android Studio compatible con AGP 9.2.1.
- JDK 17.
- Android SDK 36.
- Gradle Wrapper del proyecto (preferido) o Gradle compatible con el wrapper.
- Una API TRAZO accesible desde el dispositivo/emulador.

Compilación de comprobación:

```bash
./gradlew --no-daemon :app:assembleDebug
```

En Windows:

```powershell
.\gradlew.bat --no-daemon :app:assembleDebug
```

## 2. Propiedades de configuración de la API

El módulo `app` admite estas propiedades Gradle:

| Propiedad | Valor por defecto | Descripción |
|---|---|---|
| `apiScheme` | `http` | Esquema, normalmente `https` fuera del emulador local |
| `apiHost` | `10.0.2.2` | Hostname o IP del servidor |
| `apiPort` | `5001` | Puerto HTTP(S) |
| `apiBasePath` | `/` | Prefijo de ruta, si el servidor está configurado para ello |
| `apiBaseUrl` | — | URL completa; tiene prioridad sobre las propiedades anteriores |
| `googleWebClientId` | cadena vacía | OAuth Client ID de tipo Web usado para solicitar ID tokens de Google |

La URL completa debe acabar en `/`. El endpoint release no tiene un valor real de producción por defecto: si no se configura, queda deliberadamente en `https://api.example.invalid/` para evitar publicar una aplicación apuntando por accidente al servidor de ejemplo.

### Opción recomendada: archivo local de propiedades Gradle

Puedes guardar los valores de desarrollo en `~/.gradle/gradle.properties` (Windows: `%USERPROFILE%\.gradle\gradle.properties`). Ese archivo es local a tu equipo y no se versiona con el repositorio.

Ejemplo para TRAZO una vez reservado y configurado `trazo.duckdns.org`:

```properties
apiBaseUrl=https://trazo.duckdns.org/
googleWebClientId=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com
```

Para desarrollo local con emulador puedes dejar sin definir `apiBaseUrl` y usar el endpoint debug por defecto `http://10.0.2.2:5001/`.

Si prefieres configurar las partes por separado:

```properties
apiScheme=https
apiHost=trazo.duckdns.org
apiPort=443
apiBasePath=/
googleWebClientId=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com
```

No añadas contraseñas de usuarios, claves JWT, secretos OAuth Web ni credenciales de servidor a este archivo. El Client ID OAuth público no es un secreto.

### Opción por línea de comandos

Las propiedades también se pueden pasar en cada compilación:

```bash
./gradlew --no-daemon :app:assembleDebug \
  -PapiBaseUrl=https://trazo.duckdns.org/ \
  -PgoogleWebClientId=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com
```

O de forma separada:

```bash
./gradlew --no-daemon :app:assembleDebug \
  -PapiScheme=https \
  -PapiHost=trazo.duckdns.org \
  -PapiPort=443 \
  -PgoogleWebClientId=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com
```

Si el servidor usa un puerto HTTPS no estándar, cambia `apiPort` o incluye el puerto en `apiBaseUrl`, por ejemplo `https://trazo.example.net:8443/`.

## 3. Emulador, dispositivo físico y producción

### Emulador Android

El valor debug predeterminado es:

```text
http://10.0.2.2:5001/
```

`10.0.2.2` es la dirección especial que permite al emulador Android acceder al host donde se ejecuta el emulador. La API local debe escuchar en un puerto accesible desde el host/emulador; el perfil Development del backend usa HTTP 5001.

### Dispositivo físico en la red local

Usa la IP LAN del servidor de desarrollo, no `10.0.2.2`:

```bash
./gradlew --no-daemon :app:assembleDebug \
  -PapiBaseUrl=http://192.168.1.20:5001/
```

Sustituye la IP por la dirección LAN real. HTTP sin cifrar está permitido solo en builds debug. No expongas el puerto 5001 a Internet.

### Producción / pruebas externas

Una vez que DuckDNS, Nginx y TLS estén configurados:

```bash
./gradlew --no-daemon :app:assembleRelease \
  -PapiBaseUrl=https://trazo.duckdns.org/ \
  -PgoogleWebClientId=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com
```

Las builds release requieren HTTPS. Con el diseño de proxy inverso previsto, Android conecta al puerto público 443 y Nginx reenvía internamente a `127.0.0.1:5001`. No pongas `:5001` en la URL pública ni abras ese puerto en el router.

Un prefijo como `/trazo-api/` solo funcionará si el backend ASP.NET Core y Nginx se configuran expresamente para PathBase y proxy de prefijo. No lo uses con el hostname dedicado previsto.

## 4. Google Sign-In

Se necesitan dos clientes OAuth en Google Cloud Console:

1. **Web application OAuth Client ID**: se utiliza como audiencia para obtener el ID token que valida la API. Configura el ID en `googleWebClientId` durante la compilación Android y en el backend como `Authentication__Google__ClientId`.
2. **Android OAuth Client ID**: package name `com.trazo.android` y SHA-1 de los certificados que firman los APK instalados.

Registra las huellas SHA-1 necesarias:
- Debug: certificado debug del equipo de desarrollo.
- Distribución Google Play: certificado de Play App Signing mostrado en Play Console.
- Si distribuyes APK firmado manualmente, añade también la huella de ese certificado.

Para consultar la huella del keystore debug:

```bash
keytool -list -v -alias androiddebugkey \
  -keystore ~/.android/debug.keystore \
  -storepass android -keypass android
```

En Windows, el keystore suele estar en `%USERPROFILE%\.android\debug.keystore`.

No incluyas un Google client secret en el APK ni en el repositorio. La app solicita un ID token para el cliente Web y lo envía a la API; el backend valida firma, emisor, audiencia y correo verificado.

## 5. Deep links de correo

La app registra actualmente estos enlaces:

- `trazo://account/account/verify-email`
- `trazo://account/account/reset-password`

El backend envía enlaces basados en `Email__ActionBaseUrl=trazo://account` y añade las rutas de verificación/recuperación. Comprueba el enlace recibido por correo en un dispositivo real; la configuración SMTP y la URL base se mantienen en el backend, no en Gradle.

## 6. Construcción de artefactos

Debug APK:

```bash
./gradlew --no-daemon :app:assembleDebug
```

Release APK:

```bash
./gradlew --no-daemon :app:assembleRelease \
  -PapiBaseUrl=https://trazo.duckdns.org/ \
  -PgoogleWebClientId=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com
```

Bundle para Google Play:

```bash
./gradlew --no-daemon :app:bundleRelease \
  -PapiBaseUrl=https://trazo.duckdns.org/ \
  -PgoogleWebClientId=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com
```

La firma de release debe configurarse de forma segura en el entorno de compilación/CI. No guardes contraseñas ni archivos keystore privados en Git. El proyecto actual no define credenciales de firma release dentro de `app/build.gradle.kts`; configura la firma antes de distribuir una build.

## 7. Diagnóstico rápido

| Síntoma | Comprobación |
|---|---|
| No conecta desde el emulador | API activa en host y escuchando en HTTP 5001; URL debug `10.0.2.2:5001` |
| No conecta desde un móvil | Usa IP LAN o hostname público, no `10.0.2.2` |
| Error de certificado | Usa un certificado TLS válido para el hostname; no desactives la validación TLS |
| Google Sign-In falla | Comprueba Web Client ID, package `com.trazo.android` y SHA-1 del certificado instalado |
| Login correcto pero refresh falla | Comprueba persistencia de PostgreSQL y que el servidor conserva el mismo key material de autenticación |
| Release apunta a URL incorrecta | Pasa `-PapiBaseUrl` explícitamente al compilar release |

## 8. Valores que deben coordinarse con el backend

| Android | Backend |
|---|---|
| `apiBaseUrl=https://trazo.duckdns.org/` | Nginx host `trazo.duckdns.org` -> `127.0.0.1:5001` |
| `googleWebClientId` | `Authentication__Google__ClientId` (mismo Web Client ID) |
| Deep links `trazo://account` | `Email__ActionBaseUrl=trazo://account` |
| No aplica | `Authentication__Jwt__SigningKey`, SMTP password y DB password permanecen solo en el servidor |

El hostname `trazo.duckdns.org` es el destino propuesto, pendiente de reserva DNS, certificado TLS y configuración del proxy. Esta guía no significa que el servicio público esté ya disponible.
