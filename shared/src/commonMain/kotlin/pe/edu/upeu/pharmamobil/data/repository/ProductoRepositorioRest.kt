package pe.edu.upeu.pharmamobil.data.repository

import pe.edu.upeu.pharmamobil.data.mapper.toDomain
import pe.edu.upeu.pharmamobil.data.mapper.toRequest
import pe.edu.upeu.pharmamobil.data.remote.ProductoApi
import pe.edu.upeu.pharmamobil.data.remote.ejecutarLlamada
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

/**
 * Implementacion REST del contrato ProductoRepository sobre PharmaSoft.
 *
 * Toda llamada pasa por ejecutarLlamada: si algo falla, lo que sale de aqui
 * es siempre un ErrorApiException (dominio), nunca una excepcion de Ktor.
 *
 * @param categoriaPorDefecto categoria que se envia al crear un producto: el
 * formulario de la app aun no la pide y el backend la exige.
 */
class ProductoRepositorioRest(
    private val api: ProductoApi,
    private val categoriaPorDefecto: Long
) : ProductoRepository {

    override suspend fun listar(): List<Producto> = ejecutarLlamada {
        api.listar().contenido
            // estado=false es un producto dado de baja (el DELETE de PharmaSoft es logico).
            .filter { it.estado }
            // Un registro que rompe las reglas del dominio (p. ej. precio 0)
            // se descarta en vez de tumbar toda la lista.
            .mapNotNull { dto -> runCatching { dto.toDomain() }.getOrNull() }
    }.getOrThrow()

    override suspend fun obtener(id: Long): Producto = ejecutarLlamada {
        api.obtener(id).toDomain()
    }.getOrThrow()

    override suspend fun registrar(producto: Producto): Producto = ejecutarLlamada {
        api.crear(producto.toRequest(categoriaPorDefecto)).toDomain()
    }.getOrThrow()

    override suspend fun actualizar(producto: Producto): Producto = ejecutarLlamada {
        api.actualizar(producto.id, producto.toRequest(categoriaPorDefecto)).toDomain()
    }.getOrThrow()

    override suspend fun eliminar(id: Long) = ejecutarLlamada {
        api.eliminar(id)
    }.getOrThrow()
}
