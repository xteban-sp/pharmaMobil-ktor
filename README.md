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

## Conectividad REST (Sesión 7 · rama `feature/ktor-client`)

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

En el emulador, cada escenario se provoca cambiando `escenario` en `PlatformModule.android.kt`:

| Escenario | Valor | Lo que ve el usuario |
|---|---|---|
| Tiempo agotado | `TIEMPO_AGOTADO` | "El servidor tardó demasiado en responder." |
| Campo desconocido | `JSON_ESTRICTO` | "La respuesta del servidor no tiene el formato esperado." |
| Sin conexión | modo avión | "No se pudo conectar con el servidor. Revisa tu conexión." |

### Evidencias

En Logcat, filtra por `KtorHttp` para ver `REQUEST: http://10.0.2.2:8080/api/v1/productos...` y `RESPONSE: 200`.

---

## CRUD REST completo (Sesión 8 · rama `feature/crud-productos-istana`)

La pantalla de Productos ya hace las cuatro operaciones contra PharmaSoft: listar, registrar, editar y eliminar.

### Endpoints consumidos

| Operación en la app | Método y ruta | Éxito | Errores que se manejan |
|---|---|---|---|
| Cargar inventario | `GET /api/v1/productos?pagina=0&tamanio=20` | 200 (página) | sin conexión, timeout, 5xx |
| Abrir un producto para editar | `GET /api/v1/productos/{id}` | 200 | 404 |
| Registrar | `POST /api/v1/productos` | 201 | 400, 409 |
| Guardar cambios | `PUT /api/v1/productos/{id}` | 200 | 400, 404, 409 |
| Eliminar | `DELETE /api/v1/productos/{id}` | 204 sin cuerpo | 404, 409 |

El `DELETE` de PharmaSoft es una baja lógica (`estado = false`): el producto sale del inventario activo, pero su nombre sigue ocupado y registrar otro igual devuelve 409.

### Productos dados de baja

El listado del backend incluye los productos con `estado = false`. La app los separa del inventario activo:

- El filtro **Activos / De baja** del encabezado alterna entre las dos listas.
- En "De baja", el botón **Reactivar** trae el producto (`GET /productos/{id}`) y lo actualiza con `estado = true` (`PUT /productos/{id}`). Conserva su nombre, precio, stock y categoría.
- Si se intenta registrar un producto con el nombre de uno dado de baja, el 409 se muestra como "Ya existe un producto dado de baja con ese nombre. Reactívalo desde "De baja"."

`ReactivarProductoUseCase` reutiliza las operaciones `obtener` y `actualizar` del repositorio: el contrato sigue teniendo cinco operaciones.

### Capas

- `data/remote/dto`: `ProductoRequestDto`, `ProductoResponseDto`, `PaginaResponseDto<T>` y `ErrorResponseDto`.
- `data/remote/ProductoApi.kt`: `listar`, `obtener`, `crear`, `actualizar` y `eliminar` (este último no llama a `body()`).
- `data/remote/EjecutarLlamada.kt`: **único punto** donde las excepciones de Ktor se traducen a `ErrorApi`.
- `domain/error/ErrorApi.kt`: `Validacion`, `NoEncontrado`, `Conflicto`, `NoAutorizado`, `Servidor`, `SinConexion`, `TiempoAgotado` y `RespuestaInesperada`.
- `data/repository/ProductoRepositorioRest.kt`: implementa las cinco operaciones de `ProductoRepository`. El repositorio en memoria se conserva para pruebas.
- `domain/usecase`: `Listar`, `Obtener`, `Registrar`, `Actualizar`, `Eliminar` y `ReactivarProductoUseCase`; todos devuelven `Result`.
- `presentation`: ninguna clase importa `io.ktor`. `mensajeDe(ErrorApi)` convierte el error en el texto que ve el usuario.

### Estados de la interfaz

`ProductoUiState` separa dos cosas:

- `fase` (`Cargando`, `SinProductos`, `ConProductos`, `Error`): el estado de la pantalla completa.
- `operacion` (`Inactiva`, `EnCurso(tipo, productoId)`, `Fallida(mensaje)`): el estado de la acción del usuario.

Al guardar o eliminar, la lista sigue visible: solo se deshabilitan los botones y la fila afectada muestra progreso. Tras cada cambio se vuelve a pedir el listado, sin pasar por `Cargando`.

### Manejo de errores

Las excepciones de Ktor se traducen en un único punto (`ejecutarLlamada`) a un `ErrorApi` del dominio. La presentación no conoce códigos HTTP: recibe el `ErrorApi` y decide dónde mostrarlo.

| Respuesta | `ErrorApi` | Qué ve el usuario | Dónde |
|---|---|---|---|
| 400 con `validationErrors` | `Validacion(porCampo)` | El mensaje del servidor | Debajo del campo (nombre, precio o stock) |
| 404 | `NoEncontrado` | "El producto ya no existe en el servidor." | Aviso sobre la lista, que se refresca |
| 409 | `Conflicto(mensaje)` | El mensaje del servidor (nombre duplicado, producto ya inactivo) | Aviso sobre la lista, que se refresca |
| 401 / 403 | `NoAutorizado` | "No tienes permiso para realizar esta operación." | Aviso |
| 5xx | `Servidor` | "El servidor tuvo un problema. Intenta de nuevo en un momento." | Aviso |
| Sin red o servidor apagado | `SinConexion` | "No se pudo conectar con el servidor. Revisa tu conexión." | Aviso (o pantalla de error con Reintentar si falla la carga inicial) |
| Timeout | `TiempoAgotado` | "El servidor tardó demasiado en responder." | Igual que el anterior |
| JSON inesperado | `RespuestaInesperada` | "La respuesta del servidor no tiene el formato esperado." | Igual que el anterior |

Reglas que sigue la pantalla:

- Un error de una **operación** (crear, actualizar, eliminar, reactivar) va a `operacion = Fallida(mensaje)`: la lista sigue visible. Solo el fallo de la **carga inicial** cambia `fase` a `Error`.
- La validación local (nombre vacío, precio ≤ 0, stock negativo) se resuelve en el caso de uso y no llega a hacer la petición.
- En 404 y 409 la lista que tenía la app estaba desactualizada, así que se vuelve a pedir el listado.
- `CancellationException` nunca se convierte en `ErrorApi`: `ejecutarLlamada` la relanza y no se muestra ningún mensaje.

Comportamientos de PharmaSoft verificados contra el backend (bitácora de la actividad autónoma 08):

- Repetir un `DELETE` sobre el mismo producto no devuelve 404 sino **409** ("El producto X ya se encuentra inactivo"), porque la baja es lógica y el registro sigue existiendo.
- Un precio `0` o negativo lo detiene la validación local. Para ver el 400 del servidor bajo el campo precio hay que enviar un valor positivo menor que `0.01` (p. ej. `0.001`).
- El `ViewModel` está ligado a la `Activity`, no a la pantalla: salir de Productos a Inicio **no cancela** una operación en curso; termina y la lista queda actualizada al volver. La cancelación solo ocurre cuando se destruye la `Activity`.

Escenarios que no se pueden provocar desde la interfaz: se activan cambiando una línea (`escenario = EscenarioPrueba.…`) en `shared/src/androidMain/.../di/PlatformModule.android.kt`.

| `EscenarioPrueba` | Efecto |
|---|---|
| `NINGUNO` | Uso normal |
| `TIEMPO_AGOTADO` | Timeout de 1 ms: toda petición termina en `TiempoAgotado` |
| `JSON_ESTRICTO` | `ignoreUnknownKeys = false`: la respuesta termina en `RespuestaInesperada` |
| `RESPUESTA_LENTA` | Crear, actualizar y eliminar tardan 8 s: da tiempo a salir de la pantalla |

### Categoría

El backend exige `categoriaId` y el formulario aún no lo pide. Al crear se envía `CATEGORIA_POR_DEFECTO` (id 1, en `di/AppModule.kt`); al editar se conserva la categoría que ya tenía el producto.

### Pruebas

`./gradlew :shared:testAndroidHostTest`

- `EjecutarLlamadaTest`: traducción de 400, 401/403, 404, 409, 500 y cancelación.
- `ProductoRepositorioRestTest`: los cinco verbos con `MockEngine` (método, URL, cuerpo enviado y 204 sin cuerpo).
- `CrudProductoUseCasesTest`: obtener, actualizar y eliminar.
- `ProductoViewModelTest`: transiciones de `fase` (`Cargando` → `ConProductos` / `SinProductos` / `Error`) y de `operacion` (en curso, fallida, validación bajo el campo sin pasar a `Error`, 404 y 409 con refresco, cancelación sin mensaje).

---

## Capacidades nativas (Sesión 9 · rama `feature/expect-actual-istana`)

Dos capacidades que dependen del sistema operativo, resueltas con los dos mecanismos que ofrece Kotlin Multiplatform. La interfaz Compose y el dominio siguen en `commonMain`.

| Capacidad | Mecanismo | Código común | Android | iOS |
|---|---|---|---|---|
| Formato de moneda | `expect` / `actual` | `platform/Formato.kt` | `platform/Formato.android.kt` (`java.text.NumberFormat`, es-PE) | `platform/Formato.ios.kt` (`NSNumberFormatter`, es_PE) |
| Compartir | Interfaz + inyección | `domain/platform/Compartidor.kt` | `platform/CompartidorAndroid.kt` (`Intent.ACTION_SEND`) | `platform/CompartidorIos.kt` (`UIActivityViewController`) |

Las rutas son relativas a `shared/src/<sourceSet>/kotlin/pe/edu/upeu/pharmamobil/`.

### Por qué un mecanismo distinto para cada una

- **`formatearSoles` es un `expect`**: es una función pura, sin estado ni dependencias. El compilador exige el `actual` en cada plataforma; si falta uno, el proyecto no compila.
- **`Compartidor` es una interfaz**: necesita algo que `commonMain` no conoce (un `Context` en Android, un controlador de vista en iOS). Recibir dependencias por constructor y sustituirla en una prueba es sencillo con una interfaz y complicado con un `expect`.
- Las dos implementaciones de `Compartidor` se registran dentro del `expect val platformModule`, así que el compilador sigue exigiendo que cada plataforma aporte la suya.

### Dónde se usa

- `presentation/producto/ProductoUi.kt`: `Producto.aUi()` llama a `formatearSoles`. El dominio conserva el precio como número y el composable solo pinta el texto.
- `domain/usecase/TextoParaCompartir.kt`: `Producto.comoTextoParaCompartir()` arma el texto en código común (`Naproxeno 550mg — S/ 7.80 · Stock: 6`).
- `presentation/detalle/`: al tocar una fila del inventario se abre el detalle del producto (`GET /productos/{id}`) con el botón **Compartir**. `DetalleProductoViewModel` recibe el `Compartidor` por constructor.
- Ninguna clase de `presentation` ni de `domain` importa `android.*`, `java.*` ni `platform.UIKit`.

### Registro en Koin

```kotlin
// androidMain/di/PlatformModule.android.kt
single<Compartidor> { CompartidorAndroid(androidContext()) }

// iosMain/di/PlatformModule.ios.kt
single<Compartidor> { CompartidorIos() }
```

En Android el `Context` es el de la aplicación (lo entrega `MainApplication` con `androidContext(...)`), por eso el selector se abre con `FLAG_ACTIVITY_NEW_TASK`.

### Qué cambia entre plataformas

El mismo `Double` no se ve igual: el símbolo, el espacio que lo separa del monto y los separadores los decide cada sistema. Por eso las pruebas de `FormatoTest` no comparan con un texto fijo: verifican el símbolo, los dos decimales y las cifras.

### Interoperabilidad Kotlin-Swift

| En Swift (`iosApp/iosApp/`) | Por qué se llama así |
|---|---|
| `import Shared` | El módulo `shared` se compila como framework con `baseName = "Shared"` |
| `KoinIosKt.doInitKoinIos()` | Las funciones de nivel superior de `KoinIos.kt` quedan en la clase `KoinIosKt`; Swift reserva los nombres que empiezan con `init`, así que Kotlin antepone `do` |
| `MainViewControllerKt.MainViewController()` | Mismo criterio: función de nivel superior de `MainViewController.kt` |

### Punto de control 1: el error que exige los `actual`

El primer commit de la rama declara solo el `expect`. En ese estado el proyecto no compila:

```
git log --oneline --grep="declara el expect"      # ubica el commit
git checkout <hash>
.\gradlew :shared:compileAndroidMain
# e: .../platform/Formato.kt:11:1 Expected formatearSoles has no actual declaration in module <commonMain> for JVM
git checkout feature/expect-actual-istana
```

### Pruebas

`./gradlew :shared:testAndroidHostTest`

- `FormatoTest`: símbolo, dos decimales, redondeo y cifras del monto formateado.
- `TextoParaCompartirTest`: el texto incluye nombre, precio formateado y stock.
- `DetalleProductoViewModelTest`: carga del detalle, errores y que `compartir()` entrega al `Compartidor` el texto armado en común. Usa `CompartidorFalso`.
- `AppModuleTest`: Koin resuelve `DetalleProductoViewModel`; el `Compartidor` y el motor HTTP se sustituyen por dobles, de modo que la prueba ya no sale a la red.
