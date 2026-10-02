package pe.edu.upeu.pharmamobil.data.repository

import pe.edu.upeu.pharmamobil.data.mapper.toDomain
import pe.edu.upeu.pharmamobil.data.remote.ProductoApi
import pe.edu.upeu.pharmamobil.data.remote.ejecutarLlamada
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

/**
 * Implementacion conectada del contrato ProductoRepository.
 * Toda llamada pasa por ejecutarLlamada: si algo falla, lo que sale de aqui
 * es siempre un ErrorApiException (dominio), nunca una excepcion de Ktor.
 */
class ProductoRepositoryImpl(
    private val api: ProductoApi
) : ProductoRepository {

    override suspend fun listar(): List<Producto> = ejecutarLlamada {
        api.listar().contenido
            // estado=false es un producto dado de baja (borrado logico en PharmaSoft).
            .filter { it.estado }
            // Un registro que rompe las reglas del dominio (p. ej. precio 0)
            // se descarta en vez de tumbar toda la lista.
            .mapNotNull { dto -> runCatching { dto.toDomain() }.getOrNull() }
    }.getOrThrow()

    override suspend fun registrar(producto: Producto): Producto =
        throw UnsupportedOperationException("El registro remoto (POST) se conecta en el paso 4.")
}
