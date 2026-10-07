package pe.edu.upeu.pharmamobil.presentation.detalle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import pe.edu.upeu.pharmamobil.presentation.components.IndicadorCarga

/**
 * Detalle de un producto como panel inferior sobre el inventario. Se abre al
 * tocar una fila y se cierra con la X o tocando fuera del panel.
 *
 * El composable no conoce la implementacion de compartir: solo invoca la
 * accion del ViewModel.
 */
@Composable
fun DetalleProductoScreen(
    productoId: Long,
    onCerrar: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetalleProductoViewModel = koinViewModel()
) {

    val estadoActual by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(productoId) {
        viewModel.cargar(productoId)
    }

    // El ViewModel se reutiliza entre aperturas: hasta que llegue el producto
    // pedido no se muestra el que quedo de la vez anterior.
    val estado = estadoActual.let { actual ->
        if (actual is DetalleProductoUiState.Contenido && actual.producto.id != productoId) {
            DetalleProductoUiState.Cargando
        } else {
            actual
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {

        // Fondo atenuado: tocarlo cierra el detalle.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCerrar
                )
        )

        Surface(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            tonalElevation = 3.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            DetalleProductoContenido(
                estado = estado,
                onCompartir = viewModel::compartir,
                onReintentar = { viewModel.cargar(productoId) },
                onCerrar = onCerrar
            )
        }
    }
}


@Composable
private fun DetalleProductoContenido(
    estado: DetalleProductoUiState,
    onCompartir: () -> Unit,
    onReintentar: () -> Unit,
    onCerrar: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "Detalle del producto",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onCerrar) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar detalle"
                )
            }
        }

        // when exhaustivo sobre la sealed interface, igual que en el inventario.
        when (estado) {

            DetalleProductoUiState.Cargando ->
                IndicadorCarga(
                    texto = "Cargando producto…",
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 24.dp)
                )

            is DetalleProductoUiState.Error ->
                Column(
                    modifier = Modifier.padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Text(
                        text = estado.mensaje,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )

                    FilledTonalButton(onClick = onReintentar) {
                        Text("Reintentar")
                    }
                }

            is DetalleProductoUiState.Contenido ->
                DatosDelProducto(
                    producto = estado.producto,
                    onCompartir = onCompartir
                )
        }
    }
}


@Composable
private fun DatosDelProducto(
    producto: DetalleProductoUi,
    onCompartir: () -> Unit
) {

    Column(
        modifier = Modifier.padding(end = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = producto.nombre,
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = producto.precio,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        HorizontalDivider()

        Dato(etiqueta = "Stock", valor = producto.stock)

        Dato(etiqueta = "Estado", valor = producto.estado)

        if (producto.requiereReposicion) {

            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ) {

                Text(
                    text = "Stock bajo: conviene reponer",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Button(
            onClick = { onCompartir() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Share, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Compartir")
        }
    }
}


@Composable
private fun Dato(
    etiqueta: String,
    valor: String
) {

    Row(
        modifier = Modifier.fillMaxWidth()
    ) {

        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = valor,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
