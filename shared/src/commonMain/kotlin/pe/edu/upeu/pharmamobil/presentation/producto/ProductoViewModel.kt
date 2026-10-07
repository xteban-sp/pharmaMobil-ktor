package pe.edu.upeu.pharmamobil.presentation.producto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.usecase.ActualizarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.EliminarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ListarProductosUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ObtenerProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ProductoInvalidoException
import pe.edu.upeu.pharmamobil.domain.usecase.ReactivarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.pharmamobil.presentation.error.mensajeDe
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoUiState.Fase
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoUiState.Operacion
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoUiState.Operacion.Tipo

/**
 * Cada operacion sigue el mismo patron: marcar en curso, ejecutar el caso de
 * uso (que devuelve Result y nunca lanza) y resolver con exito o con error.
 * No importa nada de io.ktor: solo conoce ErrorApi.
 */
class ProductoViewModel(
    private val listarProductos: ListarProductosUseCase,
    private val obtenerProducto: ObtenerProductoUseCase,
    private val registrarProducto: RegistrarProductoUseCase,
    private val actualizarProducto: ActualizarProductoUseCase,
    private val eliminarProducto: EliminarProductoUseCase,
    private val reactivarProducto: ReactivarProductoUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductoUiState())
    val uiState: StateFlow<ProductoUiState> = _uiState.asStateFlow()

    /** Version del servidor del producto en edicion: conserva lo que el formulario no muestra. */
    private var productoEnEdicion: Producto? = null

    init {
        cargarProductos()
    }

    /** Carga inicial y boton Reintentar: aqui si se muestra la fase Cargando. */
    fun cargarProductos() {
        viewModelScope.launch {
            _uiState.update { it.copy(fase = Fase.Cargando) }
            listarProductos().fold(
                onSuccess = ::mostrarInventario,
                onFailure = { fallo ->
                    _uiState.update { it.copy(fase = Fase.Error(mensajeDe(fallo))) }
                }
            )
        }
    }

    fun onNombreChange(nombre: String) {
        _uiState.update {
            it.copy(
                formulario = it.formulario.copy(nombre = nombre, nombreError = null),
                mensajeExito = null
            )
        }
    }

    fun onPrecioChange(precio: String) {
        _uiState.update {
            it.copy(
                formulario = it.formulario.copy(precio = precio, precioError = null),
                mensajeExito = null
            )
        }
    }

    fun onStockChange(stock: String) {
        _uiState.update {
            it.copy(
                formulario = it.formulario.copy(stock = stock, stockError = null),
                mensajeExito = null
            )
        }
    }

    /**
     * Deja el formulario en blanco para registrar un producto. Se invoca al
     * abrir la pantalla del formulario, no desde ella: asi una rotacion no
     * borra lo que el usuario ya escribio.
     */
    fun prepararNuevo() {
        productoEnEdicion = null
        _uiState.update {
            it.copy(
                formulario = FormularioProducto(),
                operacion = Operacion.Inactiva,
                mensajeExito = null
            )
        }
    }

    /** Crea un producto nuevo o guarda los cambios del que esta en edicion. */
    fun guardar() {

        if (_uiState.value.operando) return

        val formulario = _uiState.value.formulario
        val original = productoEnEdicion
        val tipo = if (original != null) Tipo.Actualizar else Tipo.Crear

        viewModelScope.launch {

            _uiState.update {
                it.copy(operacion = Operacion.EnCurso(tipo, original?.id), mensajeExito = null)
            }

            val resultado = if (original != null) {
                actualizarProducto(original, formulario.nombre, formulario.precio, formulario.stock)
            } else {
                registrarProducto(formulario.nombre, formulario.precio, formulario.stock)
            }

            resultado.fold(
                onSuccess = { producto ->
                    productoEnEdicion = null
                    refrescarInventario()
                    _uiState.update {
                        it.copy(
                            operacion = Operacion.Inactiva,
                            formulario = FormularioProducto(),
                            mensajeExito = if (tipo == Tipo.Crear) {
                                "Producto \"${producto.nombre}\" registrado correctamente"
                            } else {
                                "Producto \"${producto.nombre}\" actualizado correctamente"
                            }
                        )
                    }
                },
                onFailure = { fallo -> manejarFallo(fallo) }
            )
        }
    }

    /** Trae del servidor la version vigente del producto y la carga en el formulario. */
    fun editar(id: Long) {

        if (_uiState.value.operando) return

        viewModelScope.launch {

            _uiState.update {
                it.copy(operacion = Operacion.EnCurso(Tipo.Actualizar, id), mensajeExito = null)
            }

            obtenerProducto(id).fold(
                onSuccess = { producto ->
                    productoEnEdicion = producto
                    _uiState.update {
                        it.copy(
                            operacion = Operacion.Inactiva,
                            formulario = FormularioProducto(
                                nombre = producto.nombre,
                                precio = producto.precio.toString(),
                                stock = producto.stock.toString(),
                                editandoId = producto.id
                            )
                        )
                    }
                },
                onFailure = { fallo -> manejarFallo(fallo) }
            )
        }
    }

    fun cancelarEdicion() {
        productoEnEdicion = null
        _uiState.update {
            it.copy(formulario = FormularioProducto(), operacion = Operacion.Inactiva)
        }
    }

    fun eliminar(id: Long) {

        if (_uiState.value.operando) return

        viewModelScope.launch {

            _uiState.update {
                it.copy(operacion = Operacion.EnCurso(Tipo.Eliminar, id), mensajeExito = null)
            }

            eliminarProducto(id).fold(
                onSuccess = {
                    val estabaEnEdicion = productoEnEdicion?.id == id
                    if (estabaEnEdicion) productoEnEdicion = null
                    refrescarInventario()
                    _uiState.update {
                        it.copy(
                            operacion = Operacion.Inactiva,
                            formulario = if (estabaEnEdicion) FormularioProducto() else it.formulario,
                            mensajeExito = "Producto eliminado"
                        )
                    }
                },
                onFailure = { fallo -> manejarFallo(fallo) }
            )
        }
    }

    /** Vuelve a activar un producto dado de baja (PUT con estado = true). */
    fun reactivar(id: Long) {

        if (_uiState.value.operando) return

        viewModelScope.launch {

            _uiState.update {
                it.copy(operacion = Operacion.EnCurso(Tipo.Reactivar, id), mensajeExito = null)
            }

            reactivarProducto(id).fold(
                onSuccess = { producto ->
                    refrescarInventario()
                    _uiState.update {
                        it.copy(
                            operacion = Operacion.Inactiva,
                            mensajeExito = "Producto \"${producto.nombre}\" reactivado",
                            // Si ya no queda ninguno de baja, se regresa al inventario activo.
                            viendoBajas = it.viendoBajas && it.dadosDeBaja.isNotEmpty()
                        )
                    }
                },
                onFailure = { fallo -> manejarFallo(fallo) }
            )
        }
    }

    /** Alterna entre el inventario activo y la lista de dados de baja. */
    fun verDadosDeBaja(ver: Boolean) {
        _uiState.update { it.copy(viendoBajas = ver, mensajeExito = null) }
    }

    /** Cierra el aviso de una operacion fallida. */
    fun descartarError() {
        _uiState.update {
            if (it.operacion is Operacion.Fallida) it.copy(operacion = Operacion.Inactiva) else it
        }
    }

    /**
     * Recarga tras una mutacion SIN pasar por Fase.Cargando: lo que se ve en
     * pantalla siempre es lo que existe en el servidor, y la lista no parpadea.
     * Si la recarga falla, se conserva la lista que ya estaba.
     */
    private suspend fun refrescarInventario() {
        listarProductos().onSuccess(::mostrarInventario)
    }

    /** Separa lo que llega del servidor: activos a la fase, dados de baja a su propia lista. */
    private fun mostrarInventario(productos: List<Producto>) {

        val (activos, bajas) = productos.partition { it.activo }

        _uiState.update {
            it.copy(
                fase = if (activos.isEmpty()) Fase.SinProductos
                       else Fase.ConProductos(activos.map { p -> p.aUi() }),
                dadosDeBaja = bajas.map { p -> p.aUi() }
            )
        }
    }

    private suspend fun manejarFallo(fallo: Throwable) {

        // Validacion local (antes de ir al servidor).
        if (fallo is ProductoInvalidoException) {
            _uiState.update {
                it.copy(
                    operacion = Operacion.Inactiva,
                    formulario = it.formulario.copy(
                        nombreError = fallo.errores.nombre,
                        precioError = fallo.errores.precio,
                        stockError = fallo.errores.stock
                    )
                )
            }
            return
        }

        val error = (fallo as? ErrorApiException)?.error

        // Solo crear y actualizar envian el nombre del formulario al servidor.
        val tipo = (_uiState.value.operacion as? Operacion.EnCurso)?.tipo
        val enviaElFormulario = tipo == Tipo.Crear || tipo == Tipo.Actualizar

        when {
            // 400 del servidor: las claves de validationErrors son los campos del formulario.
            error is ErrorApi.Validacion && error.porCampo.keys.any { it in CAMPOS_DEL_FORMULARIO } ->
                _uiState.update {
                    it.copy(
                        operacion = Operacion.Inactiva,
                        formulario = it.formulario.copy(
                            nombreError = error.porCampo["nombre"],
                            precioError = error.porCampo["precio"],
                            stockError = error.porCampo["stock"]
                        )
                    )
                }

            // 404: el producto ya no existe. Se refresca la lista y se abandona la edicion.
            error is ErrorApi.NoEncontrado -> {
                productoEnEdicion = null
                refrescarInventario()
                _uiState.update {
                    it.copy(
                        operacion = Operacion.Fallida(mensajeDe(fallo)),
                        formulario = if (it.formulario.enEdicion) FormularioProducto() else it.formulario
                    )
                }
            }

            // 409: una regla de negocio impide la operacion. Suele indicar que la lista
            // quedo desactualizada (p. ej. eliminar un producto que otro usuario ya dio
            // de baja), asi que se refresca antes de avisar.
            error is ErrorApi.Conflicto -> {
                refrescarInventario()
                _uiState.update {
                    it.copy(
                        operacion = Operacion.Fallida(
                            // Si el nombre pertenece a un producto dado de baja, se indica la salida.
                            if (enviaElFormulario && nombreCoincideConUnaBaja()) {
                                "Ya existe un producto dado de baja con ese nombre. " +
                                    "Reactívalo desde \"De baja\"."
                            } else {
                                mensajeDe(fallo)
                            }
                        )
                    )
                }
            }

            else -> _uiState.update {
                it.copy(operacion = Operacion.Fallida(mensajeDe(fallo)))
            }
        }
    }

    private fun nombreCoincideConUnaBaja(): Boolean {
        val estado = _uiState.value
        val nombre = estado.formulario.nombre.trim()
        return nombre.isNotEmpty() && estado.dadosDeBaja.any { it.nombre.equals(nombre, ignoreCase = true) }
    }

    private companion object {
        val CAMPOS_DEL_FORMULARIO = setOf("nombre", "precio", "stock")
    }
}
