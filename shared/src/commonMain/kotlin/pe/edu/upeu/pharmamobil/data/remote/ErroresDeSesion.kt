package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.http.HttpStatusCode
import pe.edu.upeu.pharmamobil.domain.model.ErrorSesion

/**
 * UNICO punto donde las respuestas del servidor se traducen a [ErrorSesion]
 * (mismo patron que ejecutarLlamada de la sesion 8).
 *
 * PharmaSoft responde 401 en tres casos y los distingue por el mensaje:
 * credenciales incorrectas en el login, token vencido y token invalido.
 *
 * @param enLogin true si la respuesta es del login: ahi un 401 son credenciales.
 */
fun mapearErrorSesion(estado: HttpStatusCode, mensaje: String?, enLogin: Boolean = false): ErrorSesion =
    when (estado) {
        HttpStatusCode.Unauthorized -> when {
            mensaje?.contains("expirado", ignoreCase = true) == true -> ErrorSesion.SesionExpirada
            enLogin -> ErrorSesion.CredencialesInvalidas
            else -> ErrorSesion.SesionInvalida
        }
        HttpStatusCode.Forbidden -> ErrorSesion.SinPermiso
        else -> ErrorSesion.SinConexion
    }
