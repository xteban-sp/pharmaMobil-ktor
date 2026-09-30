package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

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
                ignoreUnknownKeys = true   // PharmaSoft manda fechas, categoria, etc. que no usamos
                isLenient = true
                encodeDefaults = true
            })
        }

        install(Logging) {
            // HEADERS: suficiente para la evidencia (GET + 200) sin volcar cuerpos.
            level = LogLevel.HEADERS
            // Prefijo fijo para filtrar en Logcat / consola de Xcode: "KtorHttp"
            logger = object : Logger {
                override fun log(message: String) = println("KtorHttp => $message")
            }
            // Pensando en JWT (Producto U2): el token nunca se imprime.
            sanitizeHeader { header -> header == HttpHeaders.Authorization }
        }

        install(HttpTimeout) {
            requestTimeoutMillis = config.requestTimeoutMs
            connectTimeoutMillis = config.connectTimeoutMs
        }

        defaultRequest {
            url(config.urlBase)
            contentType(ContentType.Application.Json)
        }
    }
