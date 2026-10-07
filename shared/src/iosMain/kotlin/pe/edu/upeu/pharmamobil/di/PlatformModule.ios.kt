package pe.edu.upeu.pharmamobil.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.dsl.module
import pe.edu.upeu.pharmamobil.data.remote.ConfiguracionApi
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import pe.edu.upeu.pharmamobil.platform.CompartidorIos

actual val platformModule: Module = module {
    // Motor Darwin (NSURLSession).
    single<HttpClientEngine> { Darwin.create() }
    // El simulador comparte la red de la Mac: localhost llega al backend.
    single { ConfiguracionApi(urlBase = "http://localhost:8080/api/v1/") }
    // Sesion 9: capacidad nativa de compartir (hoja de compartir de UIKit).
    single<Compartidor> { CompartidorIos() }
}
