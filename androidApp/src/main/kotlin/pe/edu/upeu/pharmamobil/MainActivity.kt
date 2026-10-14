package pe.edu.upeu.pharmamobil

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.koin.mp.KoinPlatform
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobil.navigation.pantallaPorNombre

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Extra opcional «pantalla» (por ejemplo, "acerca"): lo usa el workflow
        // de GitHub Actions para abrir la app directo en una pantalla.
        val pantallaInicial = pantallaPorNombre(intent?.getStringExtra("pantalla"))

        // Sesion 10: extra «acciones=todas» para provocar un 403 a proposito (la
        // interfaz muestra acciones que el rol no tiene). Solo en depuracion.
        val esDepuracion = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val mostrarAccionesSinPermiso = esDepuracion && intent?.getStringExtra("acciones") == "todas"

        // Sesion 10 (autonoma, escenario 3): extra «token=alterado» cambia UN caracter de
        // la firma del token guardado, como si alguien lo hubiera manipulado. Solo en depuracion.
        if (esDepuracion && intent?.getStringExtra("token") == "alterado") {
            alterarTokenGuardado(KoinPlatform.getKoin().get<AlmacenSeguro>())
        }

        setContent {
            App(pantallaInicial = pantallaInicial, mostrarAccionesSinPermiso = mostrarAccionesSinPermiso)
        }
    }
}

private fun alterarTokenGuardado(almacen: AlmacenSeguro) {
    val token = almacen.leerToken() ?: return
    val partes = token.split(".")
    if (partes.size != 3 || partes[2].length < 6) return
    val firma = partes[2]
    val cambiado = if (firma[5] == 'A') 'B' else 'A'
    almacen.guardarToken(partes[0] + "." + partes[1] + "." + firma.substring(0, 5) + cambiado + firma.substring(6))
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
