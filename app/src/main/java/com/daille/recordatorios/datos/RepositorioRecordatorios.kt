package com.daille.recordatorios.datos

import android.content.Context
import com.daille.recordatorios.datos.local.BaseDatosRecordatorios
import com.daille.recordatorios.datos.local.DaoRecordatorios
import com.daille.recordatorios.datos.local.EntidadRecordatorio
import com.daille.recordatorios.datos.local.aDominio
import com.daille.recordatorios.datos.local.aEntidad
import com.daille.recordatorios.datos.preferencias.RepositorioPreferencias
import com.daille.recordatorios.datos.preferencias.almacenPreferencias
import com.daille.recordatorios.datos.red.ObservadorConectividad
import com.daille.recordatorios.datos.remoto.DtoRecordatorioRemoto
import com.daille.recordatorios.datos.remoto.FabricaServicioRemoto
import com.daille.recordatorios.datos.remoto.ServicioRecordatoriosRemoto
import com.daille.recordatorios.datos.remoto.aDtoRemoto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

/** Describe el resultado de una sincronización sin exponer excepciones técnicas a las pantallas. */
sealed interface ResultadoSincronizacion {
    /** Informa que las operaciones locales pendientes fueron procesadas correctamente. */
    data object Exito : ResultadoSincronizacion

    /** Informa un fallo temporal que puede volver a intentarse sin perder la información local. */
    data object ErrorReintentable : ResultadoSincronizacion

    /** Informa un fallo permanente, como una solicitud inválida, que requiere revisar los datos. */
    data object ErrorPermanente : ResultadoSincronizacion
}

/** Define la fuente única de datos que utilizan los ViewModel, sin exponer Room ni Retrofit a Compose. */
interface RepositorioRecordatorios {
    /** Emite el listado local disponible para que la interfaz pueda funcionar sin conexión. */
    fun observarRecordatorios(): Flow<List<Recordatorio>>

    /** Emite un recordatorio identificado para actualizar la pantalla de detalle automáticamente. */
    fun observarRecordatorio(identificador: String): Flow<Recordatorio?>

    /** Crea un registro local y lo deja preparado para una futura sincronización. */
    suspend fun crearRecordatorio(recordatorio: Recordatorio)

    /** Modifica el estado local y marca la operación como pendiente. */
    suspend fun actualizarEstado(identificador: String, estado: EstadoRecordatorio)

    /** Oculta el registro de la interfaz y conserva una eliminación pendiente si ya existía en remoto. */
    suspend fun eliminarRecordatorio(identificador: String)

    /** Procesa operaciones locales pendientes cuando la persona solicita sincronizar. */
    suspend fun sincronizar(): ResultadoSincronizacion
}

/** Coordina Room y Retrofit bajo una estrategia local-first, manteniendo Room como fuente de verdad. */
class RepositorioRecordatoriosLocalFirst(
    private val dao: DaoRecordatorios,
    private val servicioRemoto: ServicioRecordatoriosRemoto
) : RepositorioRecordatorios {
    /** Evita que dos eventos de red ejecuten simultáneamente la misma sincronización dentro del proceso. */
    private val mutexSincronizacion = Mutex()

    /** Convierte las entidades observables de Room en modelos de dominio para el resto de la aplicación. */
    override fun observarRecordatorios(): Flow<List<Recordatorio>> =
        dao.observarTodos().map { entidades -> entidades.map { entidad -> entidad.aDominio() } }

    /** Convierte el resultado de detalle de Room en un modelo de dominio o en ausencia de registro. */
    override fun observarRecordatorio(identificador: String): Flow<Recordatorio?> =
        dao.observarPorIdentificador(identificador).map { entidad -> entidad?.aDominio() }

    /** Guarda primero en Room y permite que la pantalla muestre el cambio aun sin red. */
    override suspend fun crearRecordatorio(recordatorio: Recordatorio) {
        dao.guardar(recordatorio.copy(estadoSincronizacion = EstadoSincronizacion.PENDIENTE).aEntidad())
    }

    /** Actualiza el estado solicitado y registra que el servicio remoto todavía debe recibir el cambio. */
    override suspend fun actualizarEstado(identificador: String, estado: EstadoRecordatorio) {
        dao.actualizarEstado(
            identificador = identificador,
            estado = estado,
            actualizadoEn = System.currentTimeMillis(),
            estadoSincronizacion = EstadoSincronizacion.PENDIENTE
        )
    }

    /** Aplica una eliminación lógica local para conservar la operación hasta que pueda sincronizarse. */
    override suspend fun eliminarRecordatorio(identificador: String) {
        val entidad = dao.obtenerPorIdentificador(identificador) ?: return
        if (entidad.identificadorRemoto == null) {
            // Un registro que nunca llegó al servicio puede retirarse definitivamente sin realizar una llamada HTTP.
            dao.eliminarDefinitivamente(identificador)
        } else {
            dao.marcarEliminado(
                identificador = identificador,
                actualizadoEn = System.currentTimeMillis(),
                estadoSincronizacion = EstadoSincronizacion.PENDIENTE_ELIMINACION
            )
        }
    }

    /** Procesa una operación a la vez, conserva los cambios locales y clasifica las fallas de red o HTTP. */
    override suspend fun sincronizar(): ResultadoSincronizacion = mutexSincronizacion.withLock {
        val pendientes = dao.obtenerPendientesDeSincronizacion()
        for (entidad in pendientes) {
            val resultado = sincronizarEntidad(entidad)
            if (resultado != ResultadoSincronizacion.Exito) return@withLock resultado
        }
        actualizarCambiosRemotosExistentes()
    }

    /** Envía una creación, actualización o eliminación según el estado pendiente de la entidad local. */
    private suspend fun sincronizarEntidad(entidad: EntidadRecordatorio): ResultadoSincronizacion = try {
        when (entidad.estadoSincronizacion) {
            EstadoSincronizacion.PENDIENTE, EstadoSincronizacion.ERROR -> sincronizarGuardado(entidad)
            EstadoSincronizacion.PENDIENTE_ELIMINACION -> sincronizarEliminacion(entidad)
            EstadoSincronizacion.SINCRONIZADO -> ResultadoSincronizacion.Exito
        }
    } catch (_: SocketTimeoutException) {
        dao.actualizarEstadoSincronizacion(entidad.identificador, EstadoSincronizacion.ERROR)
        ResultadoSincronizacion.ErrorReintentable
    } catch (_: IOException) {
        dao.actualizarEstadoSincronizacion(entidad.identificador, EstadoSincronizacion.ERROR)
        ResultadoSincronizacion.ErrorReintentable
    }

    /** Elige POST o PUT sin duplicar un recurso remoto ya identificado. */
    private suspend fun sincronizarGuardado(entidad: EntidadRecordatorio): ResultadoSincronizacion {
        val respuesta = if (entidad.identificadorRemoto == null) {
            servicioRemoto.crearRecordatorio(entidad.aDtoRemoto())
        } else {
            servicioRemoto.actualizarRecordatorio(entidad.identificadorRemoto, entidad.aDtoRemoto())
        }
        return procesarRespuestaGuardado(entidad, respuesta)
    }

    /** Confirma una creación o actualización exitosa, o conserva el cambio local cuando la respuesta falla. */
    private suspend fun procesarRespuestaGuardado(
        entidad: EntidadRecordatorio,
        respuesta: Response<DtoRecordatorioRemoto>
    ): ResultadoSincronizacion = when {
        respuesta.isSuccessful -> {
            val identificadorRemoto = respuesta.body()?.id?.toString() ?: entidad.identificadorRemoto
            dao.confirmarSincronizacion(entidad.identificador, identificadorRemoto)
            ResultadoSincronizacion.Exito
        }

        respuesta.code() in 500..599 -> {
            dao.actualizarEstadoSincronizacion(entidad.identificador, EstadoSincronizacion.ERROR)
            ResultadoSincronizacion.ErrorReintentable
        }

        else -> {
            dao.actualizarEstadoSincronizacion(entidad.identificador, EstadoSincronizacion.ERROR)
            ResultadoSincronizacion.ErrorPermanente
        }
    }

    /** Confirma una eliminación remota antes de retirar definitivamente la fila local. */
    private suspend fun sincronizarEliminacion(entidad: EntidadRecordatorio): ResultadoSincronizacion {
        val identificadorRemoto = entidad.identificadorRemoto ?: run {
            dao.eliminarDefinitivamente(entidad.identificador)
            return ResultadoSincronizacion.Exito
        }
        val respuesta = servicioRemoto.eliminarRecordatorio(identificadorRemoto)
        return when {
            respuesta.isSuccessful || respuesta.code() == 404 -> {
                dao.eliminarDefinitivamente(entidad.identificador)
                ResultadoSincronizacion.Exito
            }

            respuesta.code() in 500..599 -> {
                dao.actualizarEstadoSincronizacion(entidad.identificador, EstadoSincronizacion.ERROR)
                ResultadoSincronizacion.ErrorReintentable
            }

            else -> {
                dao.actualizarEstadoSincronizacion(entidad.identificador, EstadoSincronizacion.ERROR)
                ResultadoSincronizacion.ErrorPermanente
            }
        }
    }

    /** Integra solamente cambios remotos ya asociados a un identificador local para evitar importar duplicados. */
    private suspend fun actualizarCambiosRemotosExistentes(): ResultadoSincronizacion = try {
        val respuesta = servicioRemoto.consultarRecordatorios()
        when {
            respuesta.isSuccessful -> {
                respuesta.body().orEmpty().forEach { dto -> actualizarEntidadExistenteDesdeDto(dto) }
                ResultadoSincronizacion.Exito
            }

            respuesta.code() in 500..599 -> ResultadoSincronizacion.ErrorReintentable
            else -> ResultadoSincronizacion.ErrorPermanente
        }
    } catch (_: SocketTimeoutException) {
        ResultadoSincronizacion.ErrorReintentable
    } catch (_: IOException) {
        ResultadoSincronizacion.ErrorReintentable
    }

    /** Mapea el DTO antes de guardarlo en Room y conserva los campos locales que el servicio simulado no conoce. */
    private suspend fun actualizarEntidadExistenteDesdeDto(dto: DtoRecordatorioRemoto) {
        val identificadorRemoto = dto.id?.toString() ?: return
        val entidadLocal = dao.obtenerPorIdentificadorRemoto(identificadorRemoto) ?: return
        if (entidadLocal.estadoSincronizacion != EstadoSincronizacion.SINCRONIZADO) return
        dao.guardar(
            entidadLocal.copy(
                titulo = dto.title,
                estado = if (dto.completed) EstadoRecordatorio.Completado else EstadoRecordatorio.Pendiente,
                actualizadoEn = System.currentTimeMillis()
            )
        )
    }
}

/** Crea las dependencias persistentes una vez para que todos los ViewModel compartan la misma fuente de datos. */
object ContenedorDependencias {
    /** Mantiene la base local inicializada desde la actividad antes de crear cualquier ViewModel. */
    private lateinit var baseDatos: BaseDatosRecordatorios

    /** Expone las preferencias persistentes sin permitir recrear DataStore desde una pantalla. */
    lateinit var repositorioPreferencias: RepositorioPreferencias
        private set

    /** Expone el repositorio local-first que usa Room y el cliente Retrofit. */
    lateinit var repositorioRecordatorios: RepositorioRecordatorios
        private set

    /** Expone la conectividad observada para que los ViewModel puedan solicitar sincronización al recuperarse la red. */
    lateinit var observadorConectividad: ObservadorConectividad
        private set

    /** Inicializa las instancias únicas usando el contexto de aplicación para evitar fugas de Activity. */
    fun inicializar(contexto: Context) {
        if (::baseDatos.isInitialized) return
        baseDatos = BaseDatosRecordatorios.crear(contexto.applicationContext)
        repositorioPreferencias = RepositorioPreferencias(contexto.applicationContext.almacenPreferencias)
        observadorConectividad = ObservadorConectividad(contexto.applicationContext)
        repositorioRecordatorios = RepositorioRecordatoriosLocalFirst(
            dao = baseDatos.daoRecordatorios(),
            servicioRemoto = FabricaServicioRemoto.crear()
        )
    }
}
