package pe.edu.upeu.pharmamobil.domain.repository

import pe.edu.upeu.pharmamobil.domain.model.Producto

/**
 * Contrato del inventario. Vive en el dominio; la implementacion con Ktor
 * (ProductoRepositorioRest) y la de memoria viven en la capa de datos.
 *
 * Si la operacion falla contra el servidor, la implementacion lanza
 * ErrorApiException: los casos de uso la convierten en Result.failure.
 */
interface ProductoRepository {

    /** Entrega todos los productos, incluidos los dados de baja (activo = false). */
    suspend fun listar(): List<Producto>

    /** Trae un producto por su id; falla con NoEncontrado si ya no existe. */
    suspend fun obtener(id: Long): Producto

    /** Incorpora el producto al inventario y devuelve el producto ya identificado. */
    suspend fun registrar(producto: Producto): Producto

    /** Reemplaza los datos del producto con ese id y devuelve la version guardada. */
    suspend fun actualizar(producto: Producto): Producto

    /** Da de baja el producto con ese id (se reactiva actualizandolo con activo = true). */
    suspend fun eliminar(id: Long)
}
