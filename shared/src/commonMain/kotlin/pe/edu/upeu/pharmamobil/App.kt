package pe.edu.upeu.pharmamobil

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.compose.KoinContext
import org.koin.compose.viewmodel.koinViewModel

import pe.edu.upeu.pharmamobil.navigation.PilaDeNavegacion
import pe.edu.upeu.pharmamobil.navigation.Screen
import pe.edu.upeu.pharmamobil.platform.AlPulsarAtras
import pe.edu.upeu.pharmamobil.presentation.acerca.AcercaDeScreen
import pe.edu.upeu.pharmamobil.presentation.cliente.ClienteScreen
import pe.edu.upeu.pharmamobil.presentation.components.EstadoVacio
import pe.edu.upeu.pharmamobil.presentation.detalle.DetalleProductoScreen
import pe.edu.upeu.pharmamobil.presentation.inicio.InicioScreen
import pe.edu.upeu.pharmamobil.presentation.producto.FormularioProductoScreen
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoScreen
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoViewModel
import pe.edu.upeu.pharmamobil.theme.PharmaMobilTheme

/** Modulos del menu lateral. */
private data class Destino(
    val screen: Screen,
    val titulo: String,
    val icono: ImageVector
)

private val DESTINOS = listOf(
    Destino(Screen.Inicio, "Inicio", Icons.Default.Home),
    Destino(Screen.Productos, "Productos", Icons.Default.Medication),
    Destino(Screen.Clientes, "Clientes", Icons.Default.Person),
    Destino(Screen.Pedidos, "Pedidos", Icons.Default.ShoppingCart),
    Destino(Screen.AcercaDe, "Acerca de", Icons.Default.Info)
)

private const val DURACION_TRANSICION_MS = 220

/**
 * @param pantallaInicial pantalla con la que abre la app. Por defecto Inicio;
 * se puede cambiar al arrancar: `-pantalla productos` en iOS y el extra
 * «pantalla» del Intent en Android (lo usa el workflow de GitHub Actions para
 * capturar evidencias en el simulador y en el emulador).
 */
@Composable
fun App(pantallaInicial: Screen = Screen.Inicio) = KoinContext {

    // Pila de pantallas: la flecha de la barra y el boton Atras quitan la de arriba.
    val pila = rememberSaveable(saver = PilaDeNavegacion.Saver) {
        PilaDeNavegacion.desde(pantallaInicial)
    }

    var darkTheme by rememberSaveable {
        mutableStateOf(false)
    }

    val drawerState = rememberDrawerState(
        initialValue = DrawerValue.Closed
    )

    val scope = rememberCoroutineScope()

    // El ViewModel de productos se pide solo dentro de su modulo: asi la
    // portada no dispara la carga del inventario.
    val productoViewModel: ProductoViewModel? =
        if (pila.actual.esDelModuloProductos()) koinViewModel() else null

    fun volver() {
        val saliendoDe = pila.actual
        if (pila.volver() && saliendoDe is Screen.FormularioProducto) {
            // Se sale sin guardar: el formulario y sus avisos no deben quedar para despues.
            productoViewModel?.cancelarEdicion()
        }
    }

    // Boton Atras del sistema: cierra el menu o vuelve a la pantalla anterior.
    AlPulsarAtras(habilitado = drawerState.isOpen || pila.puedeVolver) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            volver()
        }
    }

    // Avanzar desliza hacia la izquierda; volver, hacia la derecha.
    var profundidadAnterior by remember { mutableIntStateOf(pila.profundidad) }
    val avanza = pila.profundidad >= profundidadAnterior
    SideEffect { profundidadAnterior = pila.profundidad }

    PharmaMobilTheme(
        darkTheme = darkTheme
    ) {

        ModalNavigationDrawer(

            drawerState = drawerState,

            // Dentro de un modulo el borde izquierdo queda para el gesto de volver.
            gesturesEnabled = drawerState.isOpen || !pila.puedeVolver,

            drawerContent = {

                ModalDrawerSheet {

                    DrawerHeader()

                    HorizontalDivider()

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    DESTINOS.forEach { destino ->

                        NavigationDrawerItem(
                            label = {
                                Text(destino.titulo)
                            },
                            selected = pila.moduloActual() == destino.screen,
                            onClick = {

                                pila.irAModulo(destino.screen)

                                scope.launch {
                                    drawerState.close()
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = destino.icono,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier.padding(
                                NavigationDrawerItemDefaults.ItemPadding
                            )
                        )
                    }

                    Spacer(
                        modifier = Modifier.weight(1f)
                    )

                    HorizontalDivider()

                    ModoOscuro(
                        activo = darkTheme,
                        onCambiar = { darkTheme = it }
                    )
                }
            }
        ) {

            Scaffold(

                topBar = {

                    TopAppBar(

                        title = {
                            Text(
                                text = tituloDe(pila.actual),
                                fontWeight = FontWeight.SemiBold
                            )
                        },

                        navigationIcon = {

                            if (pila.puedeVolver) {

                                IconButton(onClick = ::volver) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Volver"
                                    )
                                }

                            } else {

                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            drawerState.open()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Abrir menú"
                                    )
                                }
                            }
                        },

                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

            ) { paddingValues ->

                AnimatedContent(
                    targetState = pila.actual,
                    transitionSpec = {
                        val sentido = if (avanza) 1 else -1
                        (slideInHorizontally(tween(DURACION_TRANSICION_MS)) { sentido * it / 5 } +
                            fadeIn(tween(DURACION_TRANSICION_MS))) togetherWith
                            (slideOutHorizontally(tween(DURACION_TRANSICION_MS)) { -sentido * it / 5 } +
                                fadeOut(tween(DURACION_TRANSICION_MS / 2)))
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) { pantalla ->

                    Box(modifier = Modifier.fillMaxSize()) {

                        when (pantalla) {

                            Screen.Inicio ->
                                InicioScreen(
                                    onNavegar = { destino ->
                                        pila.ir(destino)
                                    }
                                )

                            Screen.Productos -> {
                                val viewModel: ProductoViewModel = koinViewModel()
                                ProductoScreen(
                                    viewModel = viewModel,
                                    onNuevo = {
                                        viewModel.prepararNuevo()
                                        pila.ir(Screen.FormularioProducto())
                                    },
                                    onVerDetalle = { id ->
                                        pila.ir(Screen.DetalleProducto(id))
                                    }
                                )
                            }

                            is Screen.DetalleProducto -> {
                                val viewModel: ProductoViewModel = koinViewModel()
                                DetalleProductoScreen(
                                    productoId = pantalla.productoId,
                                    onEditar = {
                                        viewModel.editar(pantalla.productoId)
                                        pila.ir(Screen.FormularioProducto(pantalla.productoId))
                                    },
                                    onEliminar = {
                                        // El inventario muestra el progreso y el resultado.
                                        pila.volverA(Screen.Productos)
                                        viewModel.eliminar(pantalla.productoId)
                                    }
                                )
                            }

                            is Screen.FormularioProducto ->
                                FormularioProductoScreen(
                                    viewModel = koinViewModel(),
                                    productoId = pantalla.productoId,
                                    activa = pantalla == pila.actual,
                                    onGuardado = {
                                        // Tras guardar se regresa al inventario, ya actualizado.
                                        pila.volverA(Screen.Productos)
                                    },
                                    onCerrar = ::volver
                                )

                            Screen.Clientes ->
                                ClienteScreen(
                                    viewModel = koinViewModel()
                                )

                            Screen.Pedidos ->
                                EstadoVacio(
                                    icono = Icons.Default.ShoppingCart,
                                    titulo = "Pedidos en construcción",
                                    descripcion = "Este módulo llega en una próxima sesión del curso.",
                                    modifier = Modifier.align(Alignment.Center)
                                )

                            Screen.AcercaDe ->
                                AcercaDeScreen()
                        }
                    }
                }
            }
        }
    }
}


/** Las tres pantallas que comparten ProductoViewModel. */
private fun Screen.esDelModuloProductos(): Boolean =
    this is Screen.Productos || this is Screen.DetalleProducto || this is Screen.FormularioProducto

/** Modulo al que pertenece la pantalla visible: es el que se marca en el menu lateral. */
private fun PilaDeNavegacion.moduloActual(): Screen = when (val pantalla = actual) {
    is Screen.DetalleProducto, is Screen.FormularioProducto -> Screen.Productos
    else -> pantalla
}


@Composable
private fun DrawerHeader() {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {

            Icon(
                imageVector = Icons.Default.LocalPharmacy,
                contentDescription = null,
                modifier = Modifier
                    .padding(10.dp)
                    .size(28.dp)
            )
        }

        Column {

            Text(
                text = "PharmaMobil",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "Gestión farmacéutica",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
private fun ModoOscuro(
    activo: Boolean,
    onCambiar: (Boolean) -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 28.dp,
                vertical = 16.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Icon(
            imageVector = Icons.Default.DarkMode,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "Modo oscuro",
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = activo,
            onCheckedChange = onCambiar
        )
    }
}


private fun tituloDe(
    screen: Screen
): String = when (screen) {
    Screen.Inicio -> "PharmaMobil"
    Screen.Productos -> "Productos"
    Screen.Clientes -> "Clientes"
    Screen.Pedidos -> "Pedidos"
    Screen.AcercaDe -> "Acerca de"
    is Screen.DetalleProducto -> "Detalle del producto"
    is Screen.FormularioProducto ->
        if (screen.productoId == null) "Nuevo producto" else "Editar producto"
}
