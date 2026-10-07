package pe.edu.upeu.pharmamobil.navigation

sealed class Screen {

    data object Inicio : Screen()

    data object Productos : Screen()

    data object Clientes : Screen()

    data object Pedidos : Screen()

    /** Detalle de un producto: se llega tocando una fila del inventario. */
    data class DetalleProducto(val productoId: Long) : Screen()

    /** Formulario de producto. Con [productoId] edita; sin el, registra uno nuevo. */
    data class FormularioProducto(val productoId: Long? = null) : Screen()
}
