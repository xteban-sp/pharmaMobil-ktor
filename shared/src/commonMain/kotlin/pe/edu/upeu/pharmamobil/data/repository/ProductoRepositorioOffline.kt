package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import pe.edu.upeu.pharmamobil.data.local.ProductoLocal
import pe.edu.upeu.pharmamobil.data.remote.ProductoRemoto
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

/**
 * Sesion 11: repositorio offline-first. La fuente de verdad es la base local
 * ([ProductoLocal]); [ProductoRemoto] (PharmaSoft) solo sirve para sincronizar.
 *
 * - Leer: siempre de la base local, con o sin red.
 * - Crear: primero en local (pendiente) y despues se intenta subir.
 * - Editar, dar de baja y reactivar necesitan al servidor (es quien valida
 *   nombres duplicados y permisos); el resultado se guarda en local y el Flow emite.
 */
class ProductoRepositorioOffline(
    private val local: ProductoLocal,
    private val remoto: ProductoRemoto
) : ProductoRepository {

    override fun observar(): Flow<List<Producto>> = local.observar()

    override suspend fun registrar(producto: Producto): Producto {
        // 1. Primero en local: desde aqui el producto ya aparece en la lista (pendiente).
        val guardado = local.guardarPendiente(producto)
        return try {
            // 2. Con red se sube de inmediato y cambia su id local por el real.
            val creado = remoto.registrar(guardado)
            local.confirmarSubida(guardado.id, creado)
            // 3. Se aprovecha la conexion para subir otros pendientes y bajar la lista.
            intentar { sincronizar() }
            creado
        } catch (cancelacion: CancellationException) {
            throw cancelacion
        } catch (e: ErrorApiException) {
            if (e.error.esFaltaDeRed()) {
                // Sin red no pasa nada: queda pendiente y se sube al sincronizar.
                guardado
            } else {
                // El servidor lo rechazo (nombre duplicado, datos invalidos...): no se guarda,
                // igual que antes de la sesion 11, y el formulario muestra el error.
                local.borrar(guardado.id)
                throw e
            }
        }
    }

    /** Primero subir, despues bajar. Al reves, la bajada borraria lo creado sin red. */
    override suspend fun sincronizar() {
        val rechazo = subirPendientes()
        local.reemplazarSincronizados(remoto.listar())
        // Un pendiente que el servidor no acepta se queda en el dispositivo (se puede editar
        // o eliminar) y se informa; no bloquea a los demas ni a la bajada.
        rechazo?.let { throw it }
    }

    override suspend fun obtener(id: Long): Producto {
        // Un pendiente solo existe en el dispositivo.
        if (id < 0) return local.obtener(id) ?: throw ErrorApiException(ErrorApi.NoEncontrado)
        return try {
            remoto.obtener(id).also { local.guardarSincronizado(it) }
        } catch (e: ErrorApiException) {
            // Sin red se muestra la copia local; cualquier otro error se informa.
            if (e.error.esFaltaDeRed()) local.obtener(id) ?: throw e else throw e
        }
    }

    override suspend fun actualizar(producto: Producto): Producto {
        if (producto.id < 0) return local.actualizarPendiente(producto)
        val guardado = conservandoLaListaAlDia { remoto.actualizar(producto) }
        local.guardarSincronizado(guardado)
        return guardado
    }

    override suspend fun eliminar(id: Long) {
        if (id < 0) return local.borrar(id)
        conservandoLaListaAlDia { remoto.eliminar(id) }
        local.marcarDeBaja(id)
    }

    /** Devuelve el primer rechazo del servidor (si hubo). Un corte de red se propaga. */
    private suspend fun subirPendientes(): ErrorApiException? {
        var rechazo: ErrorApiException? = null
        local.pendientes().forEach { pendiente ->
            try {
                val creado = remoto.registrar(pendiente)
                // Se confirma uno por uno: si la red se corta a la mitad, lo ya subido
                // no se vuelve a enviar (no hay duplicados) y el resto sigue pendiente.
                local.confirmarSubida(pendiente.id, creado)
            } catch (e: ErrorApiException) {
                if (e.error.esFaltaDeRed()) throw e
                if (rechazo == null) rechazo = e
            }
        }
        return rechazo
    }

    /**
     * Un 404 o un 409 al editar significa que la copia local quedo vieja (otro usuario
     * cambio el producto): se sincroniza en segundo plano y se informa el error igual.
     */
    private suspend fun <T> conservandoLaListaAlDia(bloque: suspend () -> T): T =
        try {
            bloque()
        } catch (e: ErrorApiException) {
            if (e.error == ErrorApi.NoEncontrado || e.error is ErrorApi.Conflicto) intentar { sincronizar() }
            throw e
        }

    /** Un fallo de la sincronizacion no debe tumbar la operacion local que ya se hizo. */
    private suspend fun intentar(bloque: suspend () -> Unit) {
        try {
            bloque()
        } catch (cancelacion: CancellationException) {
            throw cancelacion
        } catch (sinRed: ErrorApiException) {
            // Queda pendiente; se reintentara al sincronizar.
        }
    }
}

/** Sin red, servidor apagado o sin respuesta: el producto se queda en el dispositivo. */
internal fun ErrorApi.esFaltaDeRed(): Boolean =
    this == ErrorApi.SinConexion || this == ErrorApi.TiempoAgotado
