package pe.edu.upeu.pharmamobil.presentation.producto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upeu.pharmamobil.presentation.components.EstadoError
import pe.edu.upeu.pharmamobil.presentation.components.EstadoVacio
import pe.edu.upeu.pharmamobil.presentation.components.IndicadorCarga
import pe.edu.upeu.pharmamobil.presentation.components.MensajeError
import pe.edu.upeu.pharmamobil.presentation.components.MensajeExito
import pe.edu.upeu.pharmamobil.presentation.components.ValidatedTextField
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoUiState.Fase
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoUiState.Operacion

@Composable
fun ProductoScreen(
    viewModel: ProductoViewModel,
    modifier: Modifier = Modifier
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Producto pendiente de confirmar su eliminacion (estado solo de la interfaz).
    var porEliminar by remember { mutableStateOf<ProductoUi?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        FormularioProductoCard(
            formulario = uiState.formulario,
            operacion = uiState.operacion,
            onNombreChange = viewModel::onNombreChange,
            onPrecioChange = viewModel::onPrecioChange,
            onStockChange = viewModel::onStockChange,
            onGuardar = viewModel::guardar,
            onCancelarEdicion = viewModel::cancelarEdicion
        )

        // La operacion en curso se anuncia sin tapar el listado.
        when (val operacion = uiState.operacion) {

            Operacion.Inactiva -> Unit

            is Operacion.EnCurso ->
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            is Operacion.Fallida ->
                MensajeError(
                    texto = operacion.mensaje,
                    onCerrar = viewModel::descartarError
                )
        }

        uiState.mensajeExito?.let {
            MensajeExito(it)
        }

        EncabezadoInventario(uiState.fase)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {

            // when exhaustivo sobre la sealed interface: si se agrega una fase
            // y no se trata aqui, el proyecto no compila.
            when (val fase = uiState.fase) {

                Fase.Cargando ->
                    IndicadorCarga(
                        texto = "Cargando inventario…",
                        modifier = Modifier.align(Alignment.Center)
                    )

                Fase.SinProductos ->
                    EstadoVacio(
                        icono = Icons.Default.Inventory2,
                        titulo = "Todavía no hay productos",
                        descripcion = "Registra el primero con el formulario de arriba.",
                        modifier = Modifier.align(Alignment.Center)
                    )

                is Fase.ConProductos ->
                    ListaProductos(
                        productos = fase.productos,
                        operacion = uiState.operacion,
                        editandoId = uiState.formulario.editandoId,
                        onEditar = viewModel::editar,
                        onEliminar = { porEliminar = it }
                    )

                is Fase.Error ->
                    EstadoError(
                        titulo = "No pudimos cargar el inventario",
                        mensaje = fase.mensaje,
                        onReintentar = viewModel::cargarProductos,
                        modifier = Modifier.align(Alignment.Center)
                    )
            }
        }
    }

    porEliminar?.let { producto ->
        AlertDialog(
            onDismissRequest = { porEliminar = null },
            title = { Text("Eliminar producto") },
            text = { Text("¿Dar de baja \"${producto.nombre}\" del inventario?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        porEliminar = null
                        viewModel.eliminar(producto.id)
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { porEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}


@Composable
private fun FormularioProductoCard(
    formulario: FormularioProducto,
    operacion: Operacion,
    onNombreChange: (String) -> Unit,
    onPrecioChange: (String) -> Unit,
    onStockChange: (String) -> Unit,
    onGuardar: () -> Unit,
    onCancelarEdicion: () -> Unit
) {

    val operando = operacion is Operacion.EnCurso
    val guardando = operacion is Operacion.EnCurso && operacion.tipo != Operacion.Tipo.Eliminar

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = if (formulario.enEdicion) "Editar producto" else "Registrar producto",
                style = MaterialTheme.typography.titleMedium
            )

            ValidatedTextField(
                value = formulario.nombre,
                onValueChange = onNombreChange,
                label = "Nombre",
                error = formulario.nombreError,
                leadingIcon = Icons.Default.Medication,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                ValidatedTextField(
                    value = formulario.precio,
                    onValueChange = onPrecioChange,
                    label = "Precio",
                    error = formulario.precioError,
                    ayuda = "En soles",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )

                ValidatedTextField(
                    value = formulario.stock,
                    onValueChange = onStockChange,
                    label = "Stock",
                    error = formulario.stockError,
                    ayuda = "Unidades",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                if (formulario.enEdicion) {
                    OutlinedButton(
                        onClick = onCancelarEdicion,
                        enabled = !operando,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }
                }

                // Solo se deshabilita el boton: la lista sigue visible mientras se guarda.
                Button(
                    onClick = onGuardar,
                    enabled = !operando,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        when {
                            formulario.enEdicion && guardando -> "Guardando…"
                            formulario.enEdicion -> "Guardar cambios"
                            guardando -> "Registrando…"
                            else -> "Registrar"
                        }
                    )
                }
            }
        }
    }
}


@Composable
private fun EncabezadoInventario(
    fase: Fase
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = "Inventario",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )

        if (fase is Fase.ConProductos) {

            val cantidad = fase.productos.size

            Text(
                text = if (cantidad == 1) "1 producto" else "$cantidad productos",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
private fun ListaProductos(
    productos: List<ProductoUi>,
    operacion: Operacion,
    editandoId: Long?,
    onEditar: (Long) -> Unit,
    onEliminar: (ProductoUi) -> Unit
) {

    val enCurso = operacion as? Operacion.EnCurso

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = productos,
            key = { it.id }
        ) { producto ->
            ProductoItem(
                producto = producto,
                // Solo la fila afectada muestra progreso; el resto queda deshabilitado.
                enProceso = enCurso?.productoId == producto.id,
                seleccionado = editandoId == producto.id,
                accionesHabilitadas = enCurso == null,
                onEditar = { onEditar(producto.id) },
                onEliminar = { onEliminar(producto) }
            )
        }
    }
}


@Composable
private fun ProductoItem(
    producto: ProductoUi,
    enProceso: Boolean,
    seleccionado: Boolean,
    accionesHabilitadas: Boolean,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Surface(
                shape = CircleShape,
                color = if (seleccionado) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondaryContainer,
                contentColor = if (seleccionado) MaterialTheme.colorScheme.onPrimary
                               else MaterialTheme.colorScheme.onSecondaryContainer
            ) {

                Icon(
                    imageVector = Icons.Default.Medication,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = producto.nombre,
                    style = MaterialTheme.typography.titleSmall
                )

                Text(
                    text = "${producto.precio}  ·  ${producto.stock}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (producto.requiereReposicion) {

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ) {

                    Text(
                        text = "Reponer",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 4.dp
                        )
                    )
                }
            }

            if (enProceso) {

                Box(
                    modifier = Modifier.size(96.dp, 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                }

            } else {

                Row {

                    IconButton(
                        onClick = onEditar,
                        enabled = accionesHabilitadas
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar ${producto.nombre}"
                        )
                    }

                    IconButton(
                        onClick = onEliminar,
                        enabled = accionesHabilitadas
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar ${producto.nombre}"
                        )
                    }
                }
            }
        }
    }
}
