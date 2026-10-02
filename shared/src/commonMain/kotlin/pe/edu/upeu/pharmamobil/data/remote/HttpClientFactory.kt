package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json

private const val RETARDO_RESPUESTA_LENTA_MS = 8_000L

/**
 * Unico punto donde se configura el cliente HTTP. Es comun a Android e iOS:
 * lo que cambia por plataforma (motor y URL base) llega inyectado por Koin.
 *
 * Crear un HttpClient es costoso (pool de conexiones): Koin lo registra como
 * single y se reutiliza en toda la app.
 */
fun crearHttpClient(engine: HttpClientEngine, config: ConfiguracionApi): HttpClient =
    HttpClient(engine) {
        // 4xx -> ClientRequestException, 5xx -> ServerResponseException
        expectSuccess = true

        install(ContentNegotiation) {
            json(Json {
                // PharmaSoft manda fechas, categoria, etc. que no usamos (escenario 5 lo desactiva)
                ignoreUnknownKeys = config.escenario != EscenarioPrueba.JSON_ESTRICTO
                isLenient = true
                encodeDefaults = true
            })
        }

        install(Logging) {
            // HEADERS: suficiente para la evidencia (GET + 200) sin volcar cuerpos.
            level = LogLevel.HEADERS
            // Prefijo fijo para filtrar en Logcat / consola de Xcode: "KtorHttp"
            logger = object : Logger {
                // Ktor manda bloques de varias lineas: se antepone el prefijo a CADA linea
                // para que el filtro de Logcat las muestre todas.
                override fun log(message: String) =
                    message.lineSequence().forEach { linea -> println("KtorHttp => $linea") }
            }
            // Pensando en JWT (Producto U2): el token nunca se imprime.
            sanitizeHeader { header -> header == HttpHeaders.Authorization }
        }

        install(HttpTimeout) {
            requestTimeoutMillis =
                if (config.escenario == EscenarioPrueba.TIEMPO_AGOTADO) 1 else config.requestTimeoutMs
            connectTimeoutMillis = config.connectTimeoutMs
        }

        // Bitacora S8, escenario de cancelacion: retrasa las operaciones de escritura
        // para poder salir de la pantalla mientras estan en curso.
        if (config.escenario == EscenarioPrueba.RESPUESTA_LENTA) {
            install(createClientPlugin("RespuestaLenta") {
                onRequest { request, _ ->
                    if (request.method != HttpMethod.Get) {
                        println(
                            "KtorHttp => RESPUESTA_LENTA: ${request.method.value} retenido " +
                                "${RETARDO_RESPUESTA_LENTA_MS / 1000} s antes de enviarse"
                        )
                        delay(RETARDO_RESPUESTA_LENTA_MS)
                    }
                }
            })
        }

        defaultRequest {
            url(config.urlBase)
            contentType(ContentType.Application.Json)
        }
    }
