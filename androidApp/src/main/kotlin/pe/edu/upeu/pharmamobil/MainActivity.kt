package pe.edu.upeu.pharmamobil

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

        setContent {
            App(pantallaInicial = pantallaInicial)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
