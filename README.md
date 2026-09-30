# Club Deportivo — App Android (2ª entrega DAM · Grupo 5 · Com. 2D)

App nativa Android en **Kotlin + XML** con base de datos local **SQLite** (`SQLiteOpenHelper`).
Implementa los tres módulos de la 2ª entrega:

| Módulo | Qué incluye |
|---|---|
| 1 · Ingreso y registro | Pantalla inicial con logo y botón **Ingreso a la App**, login por perfil (Empleado / Socio), Menú Principal, registro de socio con alias único (`SELECT COUNT(*)`) |
| 2 · Cobro y carnet | Búsqueda por N° de socio o por tipo + N° de documento, monto desde tabla de configuración, vencimiento con `LocalDate.now().plusMonths(1)`, comprobante de pago (Intent + `putExtra`), carnet generado con el primer pago |
| 3 · Vencimientos | Listado diario (vencen hoy / vencieron ayer) con `RecyclerView` + `LinearLayoutManager`, `data class Socio`, `SocioAdapter` con `ViewHolder`, solapa de vencidas acumuladas |

---

## Requisitos

- **Android Studio** Koala / Ladybug (2024.1+) o más reciente.
- **JDK 17 a 21** (el que incluye Android Studio en *Settings › Build Tools › Gradle › Gradle JDK*).
- **Android SDK 34** (Android Studio lo ofrece instalar al sincronizar si falta).
- Emulador o dispositivo con **Android 8.0 (API 26)** o superior.
- Conexión a internet la primera vez (para la descarga inicial de dependencias de Gradle).

### Comandos para verificar el entorno

Puedes ejecutar estos comandos en tu terminal (PowerShell / CMD en Windows, o Terminal en Linux/macOS) para asegurar que el entorno está listo antes de trabajar:

#### 1. Verificar versión de Java / JDK (17 a 21)
```bash
java -version
```
> **Esperado:** Debe indicar versión 17 o 21 (por ejemplo OpenJDK o JetBrains Runtime JBR).

#### 2. Verificar Gradle Wrapper y compatibilidad
Ejecuta la prueba de versión de Gradle para confirmar que el wrapper reconoce tu JDK:
```bash
# En Windows (CMD o PowerShell):
.\gradlew.bat --version

# En Linux / macOS:
./gradlew --version
```
> **Esperado:** Muestra información de la versión de Gradle (8.x) y el JVM en uso sin errores.

#### 3. Verificar herramientas de Android (ADB)
```bash
adb version
```
> **Esperado:** Muestra la versión de *Android Debug Bridge*.

#### 4. Verificar emuladores o dispositivos conectados
```bash
adb devices
```
> **Esperado:** Lista de dispositivos o emuladores activos (ej. `emulator-5554 device`).

#### 5. Verificar la compilación del proyecto (Build Check)
Prueba a compilar el paquete Debug del proyecto desde consola:
```bash
# En Windows:
.\gradlew.bat assembleDebug

# En Linux / macOS:
./gradlew assembleDebug
```
> **Esperado:** Mensaje final `BUILD SUCCESSFUL`. Esto confirma que todas las dependencias, plugins y SDKs se descargaron y configuraron correctamente.

## Instalación y ejecución

1. Descomprimir `AppClubDeportivo-main.zip` (se descarga desde GitHub con **Code › Download ZIP**).
2. En Android Studio: **File › Open…** y elegir la carpeta `AppClubDeportivo-main` (la que contiene el archivo `settings.gradle.kts`). Si al descomprimir quedó una carpeta `AppClubDeportivo-main` dentro de otra con el mismo nombre, elegir la de adentro.
3. Esperar el **Gradle Sync** (barra inferior). Si pide instalar SDK/Build-Tools, aceptar.
4. Crear un emulador si no hay uno: **Device Manager › Create Device** (por ejemplo Pixel 7, API 34).
5. Presionar **Run ▶** (`Shift+F10`). La app arranca en la pantalla inicial.

### Generar el APK

**Build › Build App Bundle(s) / APK(s) › Build APK(s)**. El archivo queda en
`app/build/outputs/apk/debug/app-debug.apk` y se puede instalar en un celular arrastrándolo al emulador
o con `adb install app-debug.apk`.

Desde consola (con `ANDROID_HOME` configurado): `./gradlew assembleDebug` (Linux/macOS) o `gradlew.bat assembleDebug` (Windows).

## Usuarios de prueba

La base se crea sola en el primer arranque, con vencimientos **relativos a la fecha del día**,
para que el listado diario siempre tenga datos.

| Perfil | Usuario / alias | Contraseña | Situación |
|---|---|---|---|
| Empleado (ADMIN) | `admin` | `admin123` | Acceso completo |
| Empleado (CONSULTA) | `recepcion` | `recep123` | Solo consulta: no registra ni cobra |
| Socio | `agomez` | `socio123` | Cuota al día, carnet vigente |
| Socio | `mruiz`, `lperez` | `socio123` | La cuota vence hoy |
| Socio | `cdiaz`, `hlopez` | `socio123` | La cuota venció ayer |
| Socio | `jsosa` | `socio123` | Vencida hace 14 días (acumuladas) |
| Socio | `rbenitez` | `socio123` | Vencida hace 40 días (acumuladas) |
| Socio | `nacosta` | `socio123` | Cuota al día pero sin apto físico (pendiente) |
| Socio | `arubio` | `socio123` | Registrado sin primera cuota: todavía no tiene carnet |

## Recorrido sugerido para probar

1. **Ingreso a la App › Empleado** con `admin / admin123` → Menú Principal.
2. **Registro de socio**: completar el formulario. Probar un alias repetido (ej. `agomez`) → aviso y foco en el campo.
3. Al guardar, la app pasa sola a **Cobro de cuota** con el socio cargado → confirmar → comprobante → **Ver carnet**.
4. **Ver vencimientos**: aparecen Díaz, López (vencieron ayer), Pérez y Ruiz (vencen hoy). Tocar uno abre su carnet; **COBRAR** lo lleva al cobro.
5. **Salir** y entrar como **Socio** con `agomez / socio123` → solo ve su carnet.

## Estructura del proyecto en el Repositorio (GitHub)

Estructura completa de archivos y carpetas versiónadas en el repositorio (excluyendo archivos autogenerados o locales filtrados por `.gitignore` como `.gradle/`, `.idea/`, `build/` o `local.properties`):

```text
AppClubDeportivo/
├── .gitignore                      # Reglas de exclusión de Git para archivos temporales y locales
├── README.md                       # Documentación principal del proyecto
├── build.gradle.kts                # Configuración de Gradle a nivel de proyecto (plugins)
├── settings.gradle.kts             # Configuración de módulos e inclusión del subproyecto :app
├── gradle.properties               # Propiedades globales de compilación de Gradle
├── gradlew                         # Script ejecutable de Gradle Wrapper para Linux/macOS
├── gradlew.bat                     # Script ejecutable de Gradle Wrapper para Windows
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar      # Binario del Wrapper de Gradle
│       └── gradle-wrapper.properties # Configuración de la versión de Gradle
└── app/
    ├── build.gradle.kts            # Configuración del módulo :app (SDKs, dependencias, namespace)
    ├── proguard-rules.pro          # Reglas de optimización y ofuscación R8/Proguard
    └── src/
        └── main/
            ├── AndroidManifest.xml # Declaración de Activities, permisos y componente inicial
            ├── java/com/grupo5/clubdeportivo/
            │   ├── data/           # Persistencia SQLite y datos
            │   │   ├── BaseDatosClub.kt  # SQLiteOpenHelper y consultas SQL
            │   │   └── DatosDePrueba.kt  # Carga inicial de datos de prueba
            │   ├── model/          # Clases de modelo y Data Classes
            │   │   ├── Carnet.kt
            │   │   ├── Empleado.kt
            │   │   ├── EstadoSocio.kt
            │   │   ├── ResultadoPago.kt
            │   │   ├── Socio.kt
            │   │   └── SocioDetalle.kt
            │   ├── ui/             # Interfaz de Usuario (Activities y Adapters)
            │   │   ├── CarnetActivity.kt
            │   │   ├── CobroCuotaActivity.kt
            │   │   ├── ComprobanteActivity.kt
            │   │   ├── LoginActivity.kt
            │   │   ├── MainActivity.kt
            │   │   ├── MenuPrincipalActivity.kt
            │   │   ├── RegistroSocioActivity.kt
            │   │   ├── SocioAdapter.kt
            │   │   ├── VencimientosActivity.kt
            │   │   └── Vistas.kt
            │   └── util/           # Funciones de soporte
            │       ├── Formato.kt    # Formateo de fechas y moneda
            │       ├── Seguridad.kt  # Hashing SHA-256 + Salt
            │       └── Sesion.kt     # Gestión de sesión activa en memoria
            └── res/                # Recursos XML, imágenes y tipografía
                ├── color/          # Selectores de color dinámicos
                ├── drawable/       # Backgrounds, iconos vectoriales y logo
                ├── font/           # Tipografía personalizada (Montserrat)
                ├── layout/         # Vistas XML de pantallas e ítems de RecyclerView
                ├── mipmap-anydpi-v26/ # Icono oficial de la app
                └── values/         # Definiciones de colores, cadenas, temas y arreglos
                    ├── arrays.xml
                    ├── colors.xml
                    ├── strings.xml
                    └── themes.xml
```

### Archivos excluidos por Git (`.gitignore`)
Al clonar o descargar el repositorio desde GitHub, **no** se descargarán los siguientes archivos/carpetas por ser temporales, generados o específicos del entorno local:
- `.gradle/` y `.idea/` *(archivos de caché e índices locales del IDE Android Studio)*.
- `local.properties` *(contiene la ruta local del Android SDK de la máquina del desarrollador)*.
- `build/` y `app/build/` *(archivos intermedios de compilación y APKs generados)*.
- `*.apk`, `*.iml` *(ejecutables e índices antiguos)*.

---

## Notas

- **Reiniciar los datos de prueba**: desinstalar la app o *Ajustes › Apps › Club Deportivo › Almacenamiento › Borrar datos*.
- **Ver las tablas**: con la app corriendo, *View › Tool Windows › App Inspection › Database Inspector* (`clubdeportivo.db`).
- La app aplica el control de vencimientos cada vez que se abre (inhabilita socios con cuota vencida).
- Si el Sync falla por versión de Gradle/AGP, aceptar la actualización que sugiere Android Studio (*AGP Upgrade Assistant*).
