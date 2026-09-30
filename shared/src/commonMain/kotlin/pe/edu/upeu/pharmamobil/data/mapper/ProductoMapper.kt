package pe.edu.upeu.pharmamobil.data.mapper

import pe.edu.upeu.pharmamobil.data.remote.dto.ProductoDto
import pe.edu.upeu.pharmamobil.domain.model.Producto

/** Traduce el contrato del backend (DTO) al concepto del negocio (dominio). */
fun ProductoDto.toDomain(): Producto = Producto(
    id = id,
    nombre = nombre.trim(),
    precio = precio,
    stock = stock
)
