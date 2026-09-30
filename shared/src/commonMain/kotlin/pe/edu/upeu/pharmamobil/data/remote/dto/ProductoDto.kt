package pe.edu.upeu.pharmamobil.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Contrato de PharmaSoft: ProductoResponseDTO (GET /api/v1/productos).
 * Solo se modelan los campos que la app usa; el resto (categoriaId,
 * categoriaNombre, fechas) lo descarta ignoreUnknownKeys.
 * BigDecimal llega como numero JSON, por eso precio es Double.
 */
@Serializable
data class ProductoDto(
    val id: Long,
    val nombre: String,
    val precio: Double,
    val stock: Int = 0,
    val estado: Boolean = true
)
