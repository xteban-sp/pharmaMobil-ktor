package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.serialization.ContentConvertException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

/**
 * Falla de red ya traducida a un mensaje para el usuario. Conserva la
 * excepcion de Ktor como causa para depurar.
 */
class ErrorDeRed(mensaje: String, causa: Throwable) : Exception(mensaje, causa)

/**
 * Traduce las excepciones tecnicas de Ktor a un mensaje entendible.
 * Vive en la capa de datos: ni el ViewModel ni la UI conocen Ktor.
 * (Sealed classes y reintentos llegan en la sesion 8.)
 */
fun Throwable.mensajeLegible(): String = when (this) {
    is ClientRequestException -> when (response.status.value) {
        401, 403 -> "No tienes permiso para ver esta informacion."
        404 -> "El recurso solicitado no existe (404)."
        else -> "La solicitud no es valida (${response.status.value})."
    }
    is ServerResponseException -> "El servidor tuvo un problema (${response.status.value}). Intenta mas tarde."
    // Los timeouts heredan de IOException: deben evaluarse antes.
    is HttpRequestTimeoutException, is ConnectTimeoutException, is SocketTimeoutException ->
        "El servidor tardo demasiado en responder."
    is IOException -> "No se pudo conectar con el servidor. Revisa tu conexion o que PharmaSoft este encendido."
    // Ktor envuelve el SerializationException en un JsonConvertException (ContentConvertException)
    is SerializationException, is ContentConvertException -> "La respuesta del servidor no tiene el formato esperado."
    else -> message ?: "Ocurrio un error inesperado."
}
