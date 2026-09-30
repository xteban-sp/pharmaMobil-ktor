package pe.edu.upeu.pharmamobil.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Envoltorio paginado de PharmaSoft (PaginaResponseDTO). El listado de
 * productos NO llega como arreglo suelto, sino dentro de "contenido".
 */
@Serializable
data class PaginaDto<T>(
    val contenido: List<T> = emptyList(),
    val pagina: Int = 0,
    val tamanio: Int = 0,
    val totalElementos: Long = 0,
    val totalPaginas: Int = 0,
    val ultima: Boolean = true
)
