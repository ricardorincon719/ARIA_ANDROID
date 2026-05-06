# ARIA Android

ARIA Android es un prototipo de asistente personal para Android construido con Kotlin y Jetpack Compose.

## Funciones actuales

- Guarda el nombre del usuario en memoria local.
- Muestra historial de conversacion durante la sesion.
- Responde comandos basicos: `hola`, `hora`, `fecha`, `presentate` y `ayuda`.
- Permite guardar recordatorios con `recordar comprar pan`.
- Permite listar y borrar recordatorios con `recordatorios` y `olvidar 1`.

## Requisitos

- Android Studio o Android SDK instalado.
- JDK compatible con Gradle.
- SDK Android 36.1 para compilar esta version.

## Compilar

En esta maquina el SDK local se configura con un archivo `local.properties` ignorado por Git:

```properties
sdk.dir=/home/samsung-ubuntu/Android/Sdk
```

Para ejecutar pruebas unitarias:

```bash
./gradlew testDebugUnitTest
```

Para generar un APK debug:

```bash
./gradlew assembleDebug
```

## Estructura

- `app/src/main/java/com/ricardo/aria/MainActivity.kt`: pantalla principal en Compose.
- `app/src/main/java/com/ricardo/aria/AriaCommandProcessor.kt`: motor de comandos de ARIA.
- `app/src/main/java/com/ricardo/aria/AriaMemoryStore.kt`: persistencia local con `SharedPreferences`.
- `app/src/test/java/com/ricardo/aria/AriaCommandProcessorTest.kt`: pruebas unitarias del motor de comandos.
- `app/src/main/java/com/ricardo/aria/ui/theme`: tema visual de Compose.
- `gradle/libs.versions.toml`: versiones de plugins y dependencias.

## Proximos pasos sugeridos

- Mejorar la interfaz con historial de conversacion.
- Integrar voz cuando la base este estable.
- Agregar comandos para conectar ARIA con servicios locales.
