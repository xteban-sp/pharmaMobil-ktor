package pe.edu.upeu.pharmamobil.data.remote

/**
 * URL base del backend PharmaSoft. Cambia segun desde donde se ejecuta la app,
 * por eso la aporta el platformModule de cada plataforma:
 *  - Emulador Android: 10.0.2.2 es el "localhost" de la PC.
 *  - Simulador iOS: comparte la red de la Mac, usa localhost.
 *  - Celular fisico: IP de la PC en la red local (ej. http://192.168.1.50:8080/api/v1/).
 */
data class ConfiguracionApi(
    val urlBase: String,
    val requestTimeoutMs: Long = 15_000,
    val connectTimeoutMs: Long = 10_000,
    val escenario: EscenarioPrueba = EscenarioPrueba.NINGUNO
)

/**
 * Escenarios de la bitacora de pruebas (actividad autonoma S7). Se activan
 * cambiando UNA linea en PlatformModule.android.kt; en uso normal: NINGUNO.
 * (El escenario "sin conexion" se prueba con el modo avion, no necesita flag.)
 */
enum class EscenarioPrueba {
    NINGUNO,
    /** GET a /productos/999999: el backend responde 404. */
    RECURSO_INEXISTENTE,
    /** requestTimeoutMillis = 1 ms: HttpRequestTimeoutException. */
    TIEMPO_AGOTADO,
    /** ignoreUnknownKeys = false: los campos extra de PharmaSoft provocan SerializationException. */
    JSON_ESTRICTO
}
