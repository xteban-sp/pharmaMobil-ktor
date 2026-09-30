package pe.edu.upeu.pharmamobil.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.core.module.Module
import org.koin.dsl.module
import pe.edu.upeu.pharmamobil.data.remote.ConfiguracionApi

actual val platformModule: Module = module {
    // Motor OkHttp (requiere el permiso INTERNET en el AndroidManifest).
    single<HttpClientEngine> { OkHttp.create() }
    // 10.0.2.2 = localhost de la PC vista desde el emulador.
    // En celular fisico reemplazar por la IP local de la PC.
    single { ConfiguracionApi(urlBase = "http://10.0.2.2:8080/api/v1/") }
}
