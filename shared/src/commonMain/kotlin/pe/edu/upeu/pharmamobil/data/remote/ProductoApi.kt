package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import pe.edu.upeu.pharmamobil.data.remote.dto.PaginaDto
import pe.edu.upeu.pharmamobil.data.remote.dto.ProductoDto

/**
 * Servicio remoto de productos. Solo habla HTTP y devuelve DTO:
 * nunca expone el modelo de dominio ni sabe de la UI.
 */
class ProductoApi(private val client: HttpClient) {

    /** GET {urlBase}productos?pagina=0&tamanio=20 */
    suspend fun obtenerProductos(pagina: Int = 0, tamanio: Int = 20): PaginaDto<ProductoDto> =
        client.get("productos") {
            parameter("pagina", pagina)
            parameter("tamanio", tamanio)
        }.body()
}
