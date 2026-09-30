package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.CancellationException
import pe.edu.upeu.pharmamobil.data.mapper.toDomain
import pe.edu.upeu.pharmamobil.data.remote.ErrorDeRed
import pe.edu.upeu.pharmamobil.data.remote.ProductoApi
import pe.edu.upeu.pharmamobil.data.remote.mensajeLegible
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

/**
 * Implementacion conectada del contrato ProductoRepository (sesion 7).
 * Reemplaza a ProductoRepositorioEnMemoria en Koin: el caso de uso y el
 * ViewModel no se enteran del cambio. El contrato no cambia: si algo falla
 * se lanza la excepcion y ListarProductosUseCase la envuelve en Result.
 */
class ProductoRepositoryImpl(
    private val api: ProductoApi
) : ProductoRepository {

    override suspend fun listar(): List<Producto> = traducirErrores {
        api.obtenerProductos().contenido
            // estado=false es un producto dado de baja (borrado logico en PharmaSoft).
            .filter { it.estado }
            // Un registro que rompe las reglas del dominio (p. ej. precio 0)
            // se descarta en vez de tumbar toda la lista.
            .mapNotNull { dto -> runCatching { dto.toDomain() }.getOrNull() }
    }

    override suspend fun registrar(producto: Producto): Producto =
        throw UnsupportedOperationException("El registro remoto (POST) se implementa en la sesion 8.")

    private suspend fun <T> traducirErrores(bloque: suspend () -> T): T =
        try {
            bloque()
        } catch (e: CancellationException) {
            throw e   // nunca tragarse la cancelacion de corrutinas
        } catch (e: Throwable) {
            throw ErrorDeRed(e.mensajeLegible(), e)
        }
}
