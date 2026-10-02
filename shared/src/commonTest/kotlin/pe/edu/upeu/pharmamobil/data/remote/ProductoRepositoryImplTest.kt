package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import pe.edu.upeu.pharmamobil.data.repository.ProductoRepositoryImpl
import pe.edu.upeu.pharmamobil.domain.usecase.ListarProductosUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Bitacora de pruebas de conexion (actividad autonoma, sesion 7) sin depender
 * de que PharmaSoft este encendido: MockEngine reemplaza al motor real.
 */
class ProductoRepositoryImplTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private val config = ConfiguracionApi(urlBase = "http://10.0.2.2:8080/api/v1/")

    private fun casoDeUsoCon(engine: MockEngine) =
        ListarProductosUseCase(ProductoRepositoryImpl(ProductoApi(crearHttpClient(engine, config))))

    // Forma real de PaginaResponseDTO<ProductoResponseDTO> de PharmaSoft.
    private val paginaValida = """
        {"contenido":[
            {"id":1,"nombre":"Paracetamol 500mg","precio":3.50,"stock":120,"estado":true,
             "categoriaId":1,"categoriaNombre":"Analgesicos",
             "fechaCreacion":"2026-09-30T10:00:00","fechaModificacion":null},
            {"id":2,"nombre":"Ibuprofeno 400mg","precio":5.20,"stock":4,"estado":false,
             "categoriaId":1,"categoriaNombre":"Analgesicos"}
         ],
         "pagina":0,"tamanio":20,"totalElementos":2,"totalPaginas":1,"ultima":true}
    """.trimIndent()

    @Test
    fun caso1_exito200_deserializaLaPaginaYMapeaAlDominio() = runTest {
        var urlPedida = ""
        val engine = MockEngine { request ->
            urlPedida = request.url.toString()
            respond(paginaValida, HttpStatusCode.OK, jsonHeaders)
        }

        val productos = casoDeUsoCon(engine)().getOrThrow()

        // El inactivo (estado=false) no se muestra.
        assertEquals(1, productos.size)
        with(productos.first()) {
            assertEquals(1L, id)
            assertEquals("Paracetamol 500mg", nombre)
            assertEquals(3.50, precio)
            assertEquals(120, stock)
        }
        assertEquals("http://10.0.2.2:8080/api/v1/productos?pagina=0&tamanio=20", urlPedida)
    }

    @Test
    fun caso2_error404_devuelveFailureConMensajeLegible() = runTest {
        val engine = MockEngine { respond("""{"mensaje":"No encontrado"}""", HttpStatusCode.NotFound, jsonHeaders) }

        val fallo = casoDeUsoCon(engine)().exceptionOrNull()

        assertIs<ErrorDeRed>(fallo)
        assertTrue(fallo.message!!.contains("404"))
    }

    @Test
    fun caso3_sinConexion_devuelveFailureSinCaerse() = runTest {
        val engine = MockEngine { throw IOException("Failed to connect to /10.0.2.2:8080") }

        val fallo = casoDeUsoCon(engine)().exceptionOrNull()

        assertIs<ErrorDeRed>(fallo)
        assertTrue(fallo.message!!.startsWith("No se pudo conectar"))
    }

    @Test
    fun caso4_timeout_devuelveFailureConMensajeDeTiempo() = runTest {
        val engine = MockEngine { throw HttpRequestTimeoutException("productos", 15_000) }

        val fallo = casoDeUsoCon(engine)().exceptionOrNull()

        assertIs<ErrorDeRed>(fallo)
        assertTrue(fallo.message!!.contains("tardo demasiado"))
    }

    @Test
    fun caso5_jsonConCampoNuevo_seIgnoraGraciasAIgnoreUnknownKeys() = runTest {
        val conCampoNuevo = """
            {"contenido":[{"id":7,"nombre":"Loratadina","precio":2.0,"stock":30,
              "estado":true,"codigoBarras":"775000123","laboratorio":{"id":3}}],
             "pagina":0,"tamanio":20,"totalElementos":1,"totalPaginas":1,"ultima":true,
             "enlaces":{"siguiente":null}}
        """.trimIndent()
        val engine = MockEngine { respond(conCampoNuevo, HttpStatusCode.OK, jsonHeaders) }

        val productos = casoDeUsoCon(engine)().getOrThrow()

        assertEquals(listOf("Loratadina"), productos.map { it.nombre })
    }

    @Test
    fun caso5b_sinIgnoreUnknownKeys_elCampoNuevoProvocaErrorDeFormato() = runTest {
        val estricto = config.copy(escenario = EscenarioPrueba.JSON_ESTRICTO)
        val engine = MockEngine { respond(paginaValida, HttpStatusCode.OK, jsonHeaders) }
        val casoDeUso = ListarProductosUseCase(ProductoRepositoryImpl(ProductoApi(crearHttpClient(engine, estricto))))

        val fallo = casoDeUso().exceptionOrNull()

        assertIs<ErrorDeRed>(fallo)
        assertEquals("La respuesta del servidor no tiene el formato esperado.", fallo.message)
    }

    @Test
    fun registroQueRompeReglasDelDominio_seDescartaSinTumbarLaLista() = runTest {
        val json = """{"contenido":[
            {"id":1,"nombre":"Muestra gratis","precio":0,"stock":5,"estado":true},
            {"id":2,"nombre":"Valido","precio":5,"stock":5,"estado":true}],
            "pagina":0,"tamanio":20,"totalElementos":2,"totalPaginas":1,"ultima":true}"""
        val engine = MockEngine { respond(json, HttpStatusCode.OK, jsonHeaders) }

        val productos = casoDeUsoCon(engine)().getOrThrow()

        assertEquals(listOf("Valido"), productos.map { it.nombre })
    }
}
