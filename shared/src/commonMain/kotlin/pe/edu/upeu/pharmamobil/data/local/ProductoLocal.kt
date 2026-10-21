package pe.edu.upeu.pharmamobil.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.db.Producto as FilaProducto

/**
 * Sesion 11: capa local del inventario. Concentra las consultas de Producto.sq y
 * traduce entre las filas generadas por SQLDelight y el modelo de dominio.
 *
 * No conoce Ktor ni los DTO del backend (punto de control 3): lo que viene del
 * servidor llega aqui ya convertido en Producto.
 *
 * Las escrituras van fuera del hilo principal (SQLite hace E/S de disco).
 */
class ProductoLocal(base: PharmaDb) {

    private val consultas = base.productoQueries

    /** La pantalla se suscribe aqui: emite de nuevo cada vez que la tabla cambia. */
    fun observar(): Flow<List<Producto>> =
        consultas.listarTodos()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { filas -> filas.map { it.aDominio() } }

    suspend fun pendientes(): List<Producto> = enDisco {
        consultas.listarPendientes().executeAsList().map { it.aDominio() }
    }

    suspend fun obtener(id: Long): Producto? = enDisco {
        consultas.obtenerPorId(id).executeAsOneOrNull()?.aDominio()
    }

    /**
     * Guarda lo que el usuario creo sin conexion. El id es local y NEGATIVO: el backend
     * nunca usa esos numeros, asi que no puede chocar con un producto real.
     */
    suspend fun guardarPendiente(producto: Producto): Producto = enDisco {
        consultas.transactionWithResult {
            val menor = consultas.siguienteIdLocal().executeAsOne()
            val local = producto.copy(id = minOf(menor, 0L) - 1L, pendiente = true)
            insertar(local)
            local
        }
    }

    /** Cambia un producto que todavia no llego al servidor: sigue pendiente y con su id local. */
    suspend fun actualizarPendiente(producto: Producto): Producto = enDisco {
        val local = producto.copy(pendiente = true)
        insertar(local)
        local
    }

    /**
     * Recibe la lista del servidor. Borra SOLO lo sincronizado (pendiente = 0): lo que el
     * usuario creo sin red se conserva. Todo va en una transaccion: el Flow emite una vez
     * y la lista no parpadea mientras se reconstruye.
     */
    suspend fun reemplazarSincronizados(productos: List<Producto>) = enDisco {
        consultas.transaction {
            consultas.borrarSincronizados()
            productos.forEach { insertar(it.copy(pendiente = false)) }
        }
    }

    /** El servidor acepto un pendiente: la fila local (id negativo) se cambia por la real. */
    suspend fun confirmarSubida(idLocal: Long, delServidor: Producto) = enDisco {
        consultas.transaction {
            consultas.borrarPorId(idLocal)
            insertar(delServidor.copy(pendiente = false))
        }
    }

    /** Guarda la version que devolvio el servidor tras editar o reactivar. */
    suspend fun guardarSincronizado(producto: Producto) = enDisco {
        insertar(producto.copy(pendiente = false))
    }

    /** PharmaSoft no borra: desactiva. Se refleja igual en la copia local. */
    suspend fun marcarDeBaja(id: Long) { enDisco { consultas.marcarDeBaja(id) } }

    /** Un pendiente que el usuario elimina antes de sincronizar nunca llega al servidor. */
    suspend fun borrar(id: Long) { enDisco { consultas.borrarPorId(id) } }

    private fun insertar(p: Producto) {
        consultas.insertar(
            p.id, p.nombre, p.precio, p.stock.toLong(), p.categoriaId,
            if (p.activo) 1L else 0L, if (p.pendiente) 1L else 0L
        )
    }

    private suspend fun <T> enDisco(bloque: () -> T): T = withContext(Dispatchers.IO) { bloque() }
}

private fun FilaProducto.aDominio() = Producto(
    id = id,
    nombre = nombre,
    precio = precio,
    stock = stock.toInt(),
    categoriaId = categoria_id,
    activo = activo == 1L,
    pendiente = pendiente == 1L
)
