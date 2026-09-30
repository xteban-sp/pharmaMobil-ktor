package pe.edu.upeu.pharmamobil

import androidx.compose.ui.window.ComposeUIViewController
import pe.edu.upeu.pharmamobil.navigation.Screen
import platform.Foundation.NSProcessInfo

fun MainViewController() = ComposeUIViewController {
    App(pantallaInicial = pantallaDesdeArgumentos())
}

/**
 * Lee el argumento de arranque `-pantalla <nombre>` (ej. `xcrun simctl launch ... -pantalla productos`).
 * Sin argumento, la app abre en Inicio como siempre.
 */
private fun pantallaDesdeArgumentos(): Screen {
    val argumentos = NSProcessInfo.processInfo.arguments.map { it.toString() }
    val indice = argumentos.indexOf("-pantalla")
    if (indice < 0) return Screen.Inicio
    return when (argumentos.getOrNull(indice + 1)?.lowercase()) {
        "productos" -> Screen.Productos
        "clientes" -> Screen.Clientes
        "pedidos" -> Screen.Pedidos
        else -> Screen.Inicio
    }
}
