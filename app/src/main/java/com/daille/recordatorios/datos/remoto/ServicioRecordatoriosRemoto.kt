package com.daille.recordatorios.datos.remoto

import com.daille.recordatorios.BuildConfig
import com.daille.recordatorios.datos.local.EntidadRecordatorio
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

/** Representa el contrato JSON del servicio remoto y no se entrega directamente a Compose. */
data class DtoRecordatorioRemoto(
    val id: Long? = null,
    val title: String,
    val completed: Boolean = false,
    val userId: Int = 1
)

/** Declara las operaciones HTTP utilizadas por el repositorio durante la sincronización. */
interface ServicioRecordatoriosRemoto {
    /** Consulta recordatorios remotos cuando la aplicación necesita actualizar datos locales. */
    @GET("todos")
    suspend fun consultarRecordatorios(): Response<List<DtoRecordatorioRemoto>>

    /** Envía un recordatorio local creado sin conexión o aún no sincronizado. */
    @POST("todos")
    suspend fun crearRecordatorio(@Body recordatorio: DtoRecordatorioRemoto): Response<DtoRecordatorioRemoto>

    /** Actualiza un recordatorio remoto usando su identificador asignado por el servicio. */
    @PUT("todos/{identificador}")
    suspend fun actualizarRecordatorio(
        @Path("identificador") identificador: String,
        @Body recordatorio: DtoRecordatorioRemoto
    ): Response<DtoRecordatorioRemoto>

    /** Solicita la eliminación de un recordatorio que ya fue sincronizado previamente. */
    @DELETE("todos/{identificador}")
    suspend fun eliminarRecordatorio(@Path("identificador") identificador: String): Response<Unit>
}

/** Construye Retrofit y OkHttp en la capa de datos, fuera de las pantallas Compose. */
object FabricaServicioRemoto {
    /** Crea un servicio Retrofit con URL configurable y tiempos de espera controlados. */
    fun crear(): ServicioRecordatoriosRemoto {
        val registroHttp = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val cliente = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(registroHttp)
            .build()
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.URL_BASE_SERVICIO)
            .client(cliente)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ServicioRecordatoriosRemoto::class.java)
    }
}

/** Convierte una entidad local al contrato mínimo utilizado por el servicio remoto simulado. */
fun EntidadRecordatorio.aDtoRemoto(): DtoRecordatorioRemoto = DtoRecordatorioRemoto(
    id = identificadorRemoto?.toLongOrNull(),
    title = titulo,
    completed = estado.codigoPersistente == "COMPLETADO"
)
