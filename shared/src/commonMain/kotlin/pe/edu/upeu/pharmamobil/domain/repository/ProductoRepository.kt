package pe.edu.upeu.pharmamobil.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.upeu.pharmamobil.domain.model.Producto

/**
 * Contrato del inventario. Vive en el dominio; la implementacion offline-first
 * (ProductoRepositorioOffline) y la de memoria viven en la capa de datos.
 *
 * Sesion 11: el repositorio ya no entrega una «foto» de la lista pedida a mano.
 * La pantalla se suscribe a [observar] y la red solo mantiene la base local al dia.
 *
 * Si una operacion falla contra el servidor, la implementacion lanza
 * ErrorApiException: los casos de uso la convierten en Result.failure.
 */
interface ProductoRepository {

    /** La pantalla se suscribe una vez y se actualiza sola. Incluye los dados de baja. */
    fun observar(): Flow<List<Producto>>

    /** Sube lo pendiente y luego trae la lista del servidor. En ese orden. */
    suspend fun sincronizar()

    /** Trae un producto por su id; falla con NoEncontrado si ya no existe. */
    suspend fun obtener(id: Long): Producto

    /** Guarda en local; intenta enviar si hay conexion. Sin red queda pendiente. */
    suspend fun registrar(producto: Producto): Producto

    /** Reemplaza los datos del producto con ese id y devuelve la version guardada. */
    suspend fun actualizar(producto: Producto): Producto

    /** Da de baja el producto con ese id (se reactiva actualizandolo con activo = true). */
    suspend fun eliminar(id: Long)
}
