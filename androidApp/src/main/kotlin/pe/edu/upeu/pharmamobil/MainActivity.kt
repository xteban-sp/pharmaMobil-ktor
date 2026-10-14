package pe.edu.upeu.pharmamobil

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
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

        setContent {
            App(pantallaInicial = pantallaInicial, mostrarAccionesSinPermiso = mostrarAccionesSinPermiso)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
