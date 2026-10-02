package pe.edu.upeu.pharmamobil.presentation.producto

/**
 * Estado unico e inmutable de la pantalla de productos.
 *
 * Separa dos cosas que no deben confundirse:
 *  - [fase]: el estado de la pantalla completa (la consulta del inventario).
 *  - [operacion]: el estado de la accion del usuario (crear, actualizar, eliminar).
 * Asi, al guardar o eliminar, el listado sigue visible y no vuelve a Cargando.
 *
 * La fase describe el inventario ACTIVO; los dados de baja van aparte en
 * [dadosDeBaja] para poder reactivarlos.
 */
data class ProductoUiState(
    val fase: Fase = Fase.Cargando,
    val formulario: FormularioProducto = FormularioProducto(),
    val operacion: Operacion = Operacion.Inactiva,
    val mensajeExito: String? = null,
    /** Productos dados de baja (eliminados): se pueden reactivar. */
    val dadosDeBaja: List<ProductoUi> = emptyList(),
    /** true cuando el usuario mira la lista de dados de baja en vez del inventario activo. */
    val viendoBajas: Boolean = false
) {

    /** true mientras hay una operacion contra el servidor: los botones se deshabilitan. */
    val operando: Boolean
        get() = operacion is Operacion.EnCurso

    /** Fases excluyentes del inventario: solo una puede estar activa. */
    sealed interface Fase {

        data object Cargando : Fase

        data object SinProductos : Fase

        data class ConProductos(val productos: List<ProductoUi>) : Fase

        data class Error(val mensaje: String) : Fase
    }

    /** Estado de la accion en curso; convive con cualquier fase. */
    sealed interface Operacion {

        data object Inactiva : Operacion

        /** [productoId] identifica la fila afectada (null al crear). */
        data class EnCurso(val tipo: Tipo, val productoId: Long? = null) : Operacion

        /** La operacion fallo: se avisa sin perder la lista. */
        data class Fallida(val mensaje: String) : Operacion

        enum class Tipo { Crear, Actualizar, Eliminar, Reactivar }
    }
}

data class FormularioProducto(
    val nombre: String = "",
    val precio: String = "",
    val stock: String = "",
    val nombreError: String? = null,
    val precioError: String? = null,
    val stockError: String? = null,
    /** Id del producto que se esta editando; null cuando el formulario crea uno nuevo. */
    val editandoId: Long? = null
) {

    val enEdicion: Boolean
        get() = editandoId != null
}
