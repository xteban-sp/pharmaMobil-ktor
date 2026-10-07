package pe.edu.upeu.pharmamobil.presentation.inicio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.edu.upeu.pharmamobil.navigation.Screen
import pe.edu.upeu.pharmamobil.presentation.components.Etiqueta
import pe.edu.upeu.pharmamobil.presentation.components.IconoEnRecuadro
import pe.edu.upeu.pharmamobil.presentation.components.Tarjeta

/** Cada modulo de la portada lleva a una pantalla de la app. */
private data class Modulo(
    val screen: Screen,
    val icono: ImageVector,
    val titulo: String,
    val descripcion: String,
    val disponible: Boolean = true
)

private val MODULOS = listOf(
    Modulo(
        screen = Screen.Productos,
        icono = Icons.Default.Medication,
        titulo = "Productos",
        descripcion = "Inventario, precios y stock"
    ),
    Modulo(
        screen = Screen.Clientes,
        icono = Icons.Default.Person,
        titulo = "Clientes",
        descripcion = "Datos de contacto para la boleta"
    ),
    Modulo(
        screen = Screen.Pedidos,
        icono = Icons.Default.ShoppingCart,
        titulo = "Pedidos",
        descripcion = "Llega en una próxima sesión del curso",
        disponible = false
    )
)

@Composable
fun InicioScreen(
    onNavegar: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Portada()

        Text(
            text = "Módulos",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp)
        )

        MODULOS.forEach { modulo ->

            AccesoAModulo(
                modulo = modulo,
                onClick = {
                    onNavegar(modulo.screen)
                }
            )
        }
    }
}


@Composable
private fun Portada() {

    val esquema = MaterialTheme.colorScheme

    // En el tema oscuro los tonos "primary" son claros: se usan los contenedores,
    // que son oscuros, para que el texto blanco se siga leyendo.
    val temaOscuro = esquema.background.luminance() < 0.5f
    val degradado = if (temaOscuro) {
        listOf(esquema.primaryContainer, esquema.tertiaryContainer)
    } else {
        listOf(esquema.primary, esquema.tertiary)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(colors = degradado))
    ) {

        // Icono grande de fondo, apenas visible: da caracter sin estorbar el texto.
        Icon(
            imageVector = Icons.Default.LocalPharmacy,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.12f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 28.dp, y = 28.dp)
                .size(168.dp)
        )

        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            IconoEnRecuadro(
                icono = Icons.Default.LocalPharmacy,
                fondo = Color.White.copy(alpha = 0.18f),
                color = Color.White,
                lado = 48.dp
            )

            Text(
                text = "PharmaMobil",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 10.dp)
            )

            Text(
                text = "Gestión farmacéutica para tu cadena de boticas.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}


@Composable
private fun AccesoAModulo(
    modulo: Modulo,
    onClick: () -> Unit
) {

    val esquema = MaterialTheme.colorScheme

    Tarjeta(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            IconoEnRecuadro(
                icono = modulo.icono,
                fondo = if (modulo.disponible) esquema.primaryContainer else esquema.surfaceVariant,
                color = if (modulo.disponible) esquema.onPrimaryContainer else esquema.onSurfaceVariant,
                lado = 48.dp
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Text(
                        text = modulo.titulo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (!modulo.disponible) {
                        Etiqueta(
                            texto = "Pronto",
                            fondo = esquema.tertiaryContainer,
                            color = esquema.onTertiaryContainer
                        )
                    }
                }

                Text(
                    text = modulo.descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = esquema.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = esquema.onSurfaceVariant
            )
        }
    }
}
