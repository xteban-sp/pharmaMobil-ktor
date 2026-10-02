package pe.edu.upeu.pharmamobil.domain.model

data class Producto(
    val id:Long,
    val nombre:String,
    val precio:Double,
    val stock: Int,
    // Sesion 8: categoria que le asigna el backend. Se conserva al editar;
    // null en un producto nuevo (el repositorio usa la categoria por defecto).
    val categoriaId: Long? = null
) {


    init {
        require(nombre.isNotBlank()) {
            "El nombre del producto no puede estar vacio"
        }
        require(precio > 0 && precio.isFinite()) {
            "El precio debe ser un numero mayor que cero"
        }
        require(stock >= 0) {
            "El stock no puede ser negativo"
        }
    }

    /**
     * Un producto necesita reposicion cuando su stock cae por debajo del
     * minimo que la botica mantiene en gondola.
     */
    val requiereReposicion: Boolean
        get() = stock < STOCK_MINIMO

    companion object {
        const val STOCK_MINIMO = 10
    }
}
