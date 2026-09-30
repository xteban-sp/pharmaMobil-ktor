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
    val connectTimeoutMs: Long = 10_000
)
