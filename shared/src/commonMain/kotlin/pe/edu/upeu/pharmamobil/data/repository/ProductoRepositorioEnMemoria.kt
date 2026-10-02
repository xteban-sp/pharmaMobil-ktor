package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

/**
 * Almacenamiento en memoria de productos. El id lo asigna el repositorio,
 * no la pantalla, para evitar identificadores duplicados.
 *
 * Desde la sesion 8 Koin inyecta ProductoRepositorioRest; esta implementacion
 * se conserva como alternativa sin red y para las pruebas. El delay simula la
 * latencia del backend.
 *
 * Koin lo registra como single, asi que es un objeto compartido y sus metodos
 * son suspend: nada garantiza que dos llamadas no se crucen. El [Mutex]
 * protege la lista y el contador de ids de esa carrera.
 */
class ProductoRepositorioEnMemoria : ProductoRepository {

    private val candado = Mutex()
    private val productos = mutableListOf<Producto>()
    private var siguienteId = 1L

    override suspend fun registrar(producto: Producto): Producto {
        delay(RETARDO_REGISTRO_MS)
        return candado.withLock {
            val guardado = producto.copy(id = siguienteId++)
            productos.add(guardado)
            guardado
        }
    }

    override suspend fun listar(): List<Producto> {
        delay(RETARDO_LISTADO_MS)
        return candado.withLock {
            productos.toList()
        }
    }

    override suspend fun obtener(id: Long): Producto = candado.withLock {
        productos.firstOrNull { it.id == id } ?: throw ErrorApiException(ErrorApi.NoEncontrado)
    }

    override suspend fun actualizar(producto: Producto): Producto {
        delay(RETARDO_REGISTRO_MS)
        return candado.withLock {
            val indice = productos.indexOfFirst { it.id == producto.id }
            if (indice < 0) throw ErrorApiException(ErrorApi.NoEncontrado)
            productos[indice] = producto
            producto
        }
    }

    override suspend fun eliminar(id: Long) {
        delay(RETARDO_REGISTRO_MS)
        candado.withLock {
            if (!productos.removeAll { it.id == id }) throw ErrorApiException(ErrorApi.NoEncontrado)
        }
    }

    private companion object {
        const val RETARDO_REGISTRO_MS = 400L
        const val RETARDO_LISTADO_MS = 600L
    }
}
