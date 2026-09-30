This is a Kotlin Multiplatform project targeting Android, iOS.

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
    folder is the appropriate location.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :shared:testAndroidHostTest`
- iOS tests: `./gradlew :shared:iosSimulatorArm64Test`

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
---

## Sesión 7 · Cliente Ktor y consumo GET (rama `feature/ktor-client`)

El inventario de Productos ya no viene del repositorio en memoria: se obtiene del backend **PharmaSoft** con Ktor Client.

### Conexión

| Dato | Valor |
|---|---|
| Backend | [dreyna/pharmaSoft](https://github.com/dreyna/pharmaSoft) (Spring Boot + Oracle, puerto 8080) |
| URL base Android (emulador) | `http://10.0.2.2:8080/api/v1/` |
| URL base iOS (simulador) | `http://localhost:8080/api/v1/` |
| Endpoint consumido | `GET productos?pagina=0&tamanio=20` |
| Prueba de vida | `GET http://localhost:8080/api/health` → 200 |
| Swagger | `http://localhost:8080/swagger-ui.html` |

La URL base la aporta `platformModule` de cada plataforma (`ConfiguracionApi`). En un celular físico se reemplaza por la IP local de la PC.

### Dependencias y permisos

- Ktor `3.6.0`: `core`, `content-negotiation`, `serialization-kotlinx-json` y `logging` en `commonMain`; `okhttp` en `androidMain` y `darwin` en `iosMain`.
- Plugin `kotlin("plugin.serialization")` con **la misma versión de Kotlin del proyecto** (`version.ref = "kotlin"`).
- OkHttp fijado en `5.4.0`: la versión 5.5.0 que trae Ktor 3.6.0 exige `compileSdk 37` y el proyecto compila con 36.
- Android: permiso `INTERNET` y `network_security_config.xml`, que permite HTTP en claro **solo** hacia `10.0.2.2`, `localhost` y `127.0.0.1`.
- iOS: `NSAllowsLocalNetworking` en `Info.plist`. ATS sigue exigiendo HTTPS para todo lo demás.

### Diccionario de DTO

El backend responde con un envoltorio paginado (`PaginaResponseDTO`). Los productos vienen dentro de `contenido`.

| Campo JSON (`ProductoResponseDTO`) | Tipo DTO | Obligatorio | Dominio (`Producto`) |
|---|---|---|---|
| `id` | `Long` | Sí | `id` |
| `nombre` | `String` | Sí | `nombre` (sin espacios extremos) |
| `precio` (BigDecimal) | `Double` | Sí | `precio` |
| `stock` | `Int` | No (0) | `stock` |
| `estado` | `Boolean` | No (true) | Filtro: los `false` (baja lógica) no se muestran |
| `categoriaId`, `categoriaNombre`, `fechaCreacion`, `fechaModificacion` | — | — | Ignorados (`ignoreUnknownKeys`) |

### Flujo

`ProductoScreen → ProductoViewModel → ListarProductosUseCase → ProductoRepositoryImpl → ProductoApi → HttpClient (OkHttp/Darwin)`

- `data/remote/HttpClientFactory.kt`: ContentNegotiation, Logging (`HEADERS`, prefijo `KtorHttp`), HttpTimeout (15 s / 10 s), DefaultRequest y `expectSuccess`.
- `data/remote/ErroresRed.kt`: traduce las excepciones de Ktor (4xx, 5xx, timeout, sin red, JSON inválido) a un mensaje para el usuario. La UI no conoce Ktor.
- `registrar()` remoto (POST) queda para la sesión 8.

### Pruebas (`ProductoRepositoryImplTest`, con `MockEngine`)

1. Éxito 200: deserializa la página y mapea al dominio.
2. Error 404: devuelve `Result.failure` con un mensaje legible.
3. Sin conexión: la app no se cae.
4. Timeout: devuelve un mensaje de tiempo agotado.
5. JSON con campos nuevos: se ignoran.

Se ejecutan con `./gradlew :shared:testAndroidHostTest`.

### Evidencias

En Logcat, filtra por `KtorHttp` para ver `REQUEST: http://10.0.2.2:8080/api/v1/productos...` y `RESPONSE: 200`.
