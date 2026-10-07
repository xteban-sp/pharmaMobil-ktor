package pe.edu.upeu.pharmamobil.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import pe.edu.upeu.pharmamobil.data.remote.ConfiguracionApi
import pe.edu.upeu.pharmamobil.data.remote.EscenarioPrueba
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import pe.edu.upeu.pharmamobil.platform.CompartidorAndroid

actual val platformModule: Module = module {
    // Motor OkHttp (requiere el permiso INTERNET en el AndroidManifest).
    single<HttpClientEngine> { OkHttp.create() }
    // 10.0.2.2 = localhost de la PC vista desde el emulador.
    // En celular fisico reemplazar por la IP local de la PC.
    single {
        ConfiguracionApi(
            urlBase = "http://10.0.2.2:8080/api/v1/",
            // Bitacoras S7/S8: cambiar a TIEMPO_AGOTADO o JSON_ESTRICTO
            // para provocar cada escenario. Dejar en NINGUNO para el uso normal.
            escenario = EscenarioPrueba.NINGUNO
        )
    }
    // Sesion 9: capacidad nativa de compartir. androidContext() es el Context de
    // la aplicacion que MainApplication le entrega a Koin al arrancar.
    single<Compartidor> { CompartidorAndroid(androidContext()) }
}
