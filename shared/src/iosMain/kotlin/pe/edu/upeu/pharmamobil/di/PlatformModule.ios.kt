package pe.edu.upeu.pharmamobil.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.dsl.module
import pe.edu.upeu.pharmamobil.data.remote.ConfiguracionApi

actual val platformModule: Module = module {
    // Motor Darwin (NSURLSession).
    single<HttpClientEngine> { Darwin.create() }
    // El simulador comparte la red de la Mac: localhost llega al backend.
    single { ConfiguracionApi(urlBase = "http://localhost:8080/api/v1/") }
}
