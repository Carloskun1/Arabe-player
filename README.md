# DCM MUSIC — Fase 1

Reproductor de música Android (Kotlin + Jetpack Compose + Media3). Tu música, tu universo.

## Qué incluye la Fase 1
- Escaneo automático de la música del dispositivo (MP3, FLAC, WAV, AAC, M4A, OGG…).
- Reproducción en segundo plano con notificación multimedia, pantalla de bloqueo, botones Bluetooth y Audio Focus (Media3 + MediaSession).
- Inicio (saludo dinámico, recientes, DCM Selection, artistas, álbumes), Búsqueda con filtros, Biblioteca (canciones con orden, artistas, álbumes).
- Mini reproductor permanente y reproductor completo (progreso, aleatorio, repetir, anterior/siguiente).
- Splash screen, icono adaptable, pantalla Acerca de, tema azul neón.

## Cómo compilar el APK solo con GitHub
1. Crea un repositorio nuevo en github.com (puede ser privado).
2. Sube TODOS los archivos de esta carpeta respetando las carpetas (incluida `.github/workflows/build.yml`).
3. Entra en la pestaña **Actions**. Si no arranca solo, abre «Compilar DCM MUSIC (APK)» y pulsa **Run workflow**.
4. Espera 4–8 minutos. Al terminar (check verde), abre la ejecución y descarga **DCM-MUSIC-APK** en *Artifacts*.
5. Descomprime el ZIP e instala el `.apk` en tu teléfono (permite «instalar apps desconocidas»).

Si sale una X roja, abre el paso que falló, copia el error y pégaselo a Claude para corregirlo.

## Actualizaciones futuras
Edita o sustituye archivos en GitHub y haz *commit*: se compila un APK nuevo automáticamente.
Para subir de versión cambia `versionCode` y `versionName` en `app/build.gradle.kts`.

## Próximas fases
- Fase 2: favoritos, playlists, recientes (Room + DataStore), cola de reproducción.
- Fase 3: ecualizador real, letras LRC, visualizador, temporizador, colores dinámicos, ajustes avanzados.
