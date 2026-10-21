package pe.edu.upeu.pharmamobil.domain.usecase

import kotlinx.coroutines.flow.Flow
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

/**
 * Sesion 11: reemplaza a ListarProductosUseCase. La pantalla ya no pide la lista:
 * se suscribe una vez y recibe cada cambio de la base local.
 */
class ObservarProductosUseCase(
    private val productoRepository: ProductoRepository
) {
    operator fun invoke(): Flow<List<Producto>> = productoRepository.observar()
}

/** Actividad autonoma 11: hora de la ultima sincronizacion correcta (null = nunca). */
class ObservarUltimaSincronizacionUseCase(
    private val productoRepository: ProductoRepository
) {
    operator fun invoke(): Flow<Long?> = productoRepository.ultimaSincronizacion()
}

/** Sube lo pendiente y trae la lista del servidor. Sin red devuelve failure (SinConexion). */
class SincronizarProductosUseCase(
    private val productoRepository: ProductoRepository
) {
    suspend operator fun invoke(): Result<Unit> = resultadoDe {
        productoRepository.sincronizar()
    }
}
