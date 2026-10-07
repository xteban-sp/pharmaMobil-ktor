package pe.edu.upeu.pharmamobil.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.dsl.module
import pe.edu.upeu.pharmamobil.data.remote.ConfiguracionApi
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import pe.edu.upeu.pharmamobil.platform.CompartidorIos
import platform.Foundation.NSBundle

actual val platformModule: Module = module {
    // Motor Darwin (NSURLSession).
    single<HttpClientEngine> { Darwin.create() }
    single { ConfiguracionApi(urlBase = urlDePharmaSoft()) }
    // Sesion 9: capacidad nativa de compartir (hoja de compartir de UIKit).
    single<Compartidor> { CompartidorIos() }
}

private const val URL_DEL_SIMULADOR = "http://localhost:8080/api/v1/"

/**
 * URL base de PharmaSoft. El simulador comparte la red de la Mac, asi que
 * localhost llega al backend. Un iPhone fisico necesita la IP de la PC: se
 * escribe al compilar en la clave PharmaSoftUrl del Info.plist.
 */
private fun urlDePharmaSoft(): String {
    val configurada = (NSBundle.mainBundle.objectForInfoDictionaryKey("PharmaSoftUrl") as? String)
        ?.trim()
        .orEmpty()
    if (configurada.isEmpty()) return URL_DEL_SIMULADOR
    return if (configurada.endsWith("/")) configurada else "$configurada/"
}
