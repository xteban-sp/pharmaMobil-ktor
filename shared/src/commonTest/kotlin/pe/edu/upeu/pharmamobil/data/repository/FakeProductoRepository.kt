package pe.edu.upeu.pharmamobil.data.repository

import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

/**
 * Doble del inventario para las pruebas: sin delay y capaz de fallar a
 * voluntad. Sin el, probar el camino de error del caso de uso o del ViewModel
 * era imposible, porque el repositorio en memoria nunca falla.
 */
class FakeProductoRepository(
    private val productos: MutableList<Producto> = mutableListOf()
) : ProductoRepository {

    var fallaAlRegistrar: Throwable? = null
    var fallaAlListar: Throwable? = null
    var fallaAlObtener: Throwable? = null
    var fallaAlActualizar: Throwable? = null
    var fallaAlEliminar: Throwable? = null

    private var siguienteId = 1L

    override suspend fun registrar(producto: Producto): Producto {

        fallaAlRegistrar?.let { throw it }

        val guardado = producto.copy(id = siguienteId++)
        productos.add(guardado)
        return guardado
    }

    override suspend fun listar(): List<Producto> {

        fallaAlListar?.let { throw it }

        return productos.toList()
    }

    override suspend fun obtener(id: Long): Producto {

        fallaAlObtener?.let { throw it }

        return productos.firstOrNull { it.id == id }
            ?: throw ErrorApiException(ErrorApi.NoEncontrado)
    }

    override suspend fun actualizar(producto: Producto): Producto {

        fallaAlActualizar?.let { throw it }

        val indice = productos.indexOfFirst { it.id == producto.id }
        if (indice < 0) throw ErrorApiException(ErrorApi.NoEncontrado)
        productos[indice] = producto
        return producto
    }

    override suspend fun eliminar(id: Long) {

        fallaAlEliminar?.let { throw it }

        if (!productos.removeAll { it.id == id }) throw ErrorApiException(ErrorApi.NoEncontrado)
    }
}
