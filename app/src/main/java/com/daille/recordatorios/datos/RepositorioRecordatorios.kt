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
import com.daille.recordatorios.datos.autenticacion.RepositorioAutenticacion
import com.daille.recordatorios.datos.autenticacion.RepositorioAutenticacionFirebase
import com.daille.recordatorios.datos.remoto.FuenteRecordatoriosFirebase
import com.daille.recordatorios.datos.remoto.ProveedorIdentidad
import com.daille.recordatorios.datos.remoto.ProveedorIdentidadFirebase
import com.daille.recordatorios.datos.remoto.aEntidadLocal
import com.daille.recordatorios.datos.remoto.aModeloFirebase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Describe el resultado de una sincronización sin exponer excepciones técnicas a las pantallas. */
sealed interface ResultadoSincronizacion {
    /** Informa que las operaciones locales pendientes fueron procesadas correctamente. */
    data object Exito : ResultadoSincronizacion

    /** Informa un fallo temporal que puede volver a intentarse sin perder la información local. */
    data object ErrorReintentable : ResultadoSincronizacion

    /** Informa un fallo permanente, como una solicitud inválida, que requiere revisar los datos. */
    data object ErrorPermanente : ResultadoSincronizacion

    /** Informa que no existe una sesión Firebase válida para determinar la ruta privada de Firestore. */
    data object ErrorSinSesion : ResultadoSincronizacion
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

/** Coordina Room y Firebase bajo una estrategia local-first, manteniendo Room como fuente de verdad. */
class RepositorioRecordatoriosLocalFirst(
    private val dao: DaoRecordatorios,
    private val fuenteFirebase: FuenteRecordatoriosFirebase,
    private val proveedorIdentidad: ProveedorIdentidad
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
        // El uid es obligatorio para que cada escritura se dirija solo a los datos de su cuenta.
        val identificadorUsuario = proveedorIdentidad.obtenerIdentificadorUsuarioActual()
            ?: return@withLock ResultadoSincronizacion.ErrorSinSesion
        val pendientes = dao.obtenerPendientesDeSincronizacion()
        for (entidad in pendientes) {
            val resultado = sincronizarEntidad(identificadorUsuario, entidad)
            if (resultado != ResultadoSincronizacion.Exito) return@withLock resultado
        }
        actualizarCambiosRemotos(identificadorUsuario)
    }

    /** Envía una creación, actualización o eliminación según el estado pendiente de la entidad local. */
    private suspend fun sincronizarEntidad(
        identificadorUsuario: String,
        entidad: EntidadRecordatorio
    ): ResultadoSincronizacion = try {
        when (entidad.estadoSincronizacion) {
            EstadoSincronizacion.PENDIENTE, EstadoSincronizacion.ERROR -> sincronizarGuardado(identificadorUsuario, entidad)
            EstadoSincronizacion.PENDIENTE_ELIMINACION -> sincronizarEliminacion(identificadorUsuario, entidad)
            EstadoSincronizacion.SINCRONIZADO -> ResultadoSincronizacion.Exito
        }
    } catch (error: FirebaseFirestoreException) {
        dao.actualizarEstadoSincronizacion(entidad.identificador, EstadoSincronizacion.ERROR)
        if (error.esReintentable()) ResultadoSincronizacion.ErrorReintentable else ResultadoSincronizacion.ErrorPermanente
    } catch (_: Exception) {
        dao.actualizarEstadoSincronizacion(entidad.identificador, EstadoSincronizacion.ERROR)
        ResultadoSincronizacion.ErrorReintentable
    }

    /** Guarda siempre en el documento definido por el identificador local estable para evitar duplicaciones. */
    private suspend fun sincronizarGuardado(
        identificadorUsuario: String,
        entidad: EntidadRecordatorio
    ): ResultadoSincronizacion {
        fuenteFirebase.guardarRecordatorio(identificadorUsuario, entidad.aModeloFirebase())
        dao.confirmarSincronizacion(entidad.identificador, entidad.identificador)
        return ResultadoSincronizacion.Exito
    }

    /** Confirma una eliminación remota antes de retirar definitivamente la fila local. */
    private suspend fun sincronizarEliminacion(
        identificadorUsuario: String,
        entidad: EntidadRecordatorio
    ): ResultadoSincronizacion {
        fuenteFirebase.eliminarRecordatorio(identificadorUsuario, entidad.identificador)
        dao.eliminarDefinitivamente(entidad.identificador)
        return ResultadoSincronizacion.Exito
    }

    /** Integra los cambios de la cuenta autenticada en Room sin sobrescribir modificaciones locales pendientes. */
    private suspend fun actualizarCambiosRemotos(identificadorUsuario: String): ResultadoSincronizacion = try {
        fuenteFirebase.obtenerRecordatorios(identificadorUsuario).forEach { modeloFirebase ->
            actualizarEntidadDesdeFirebase(modeloFirebase.aEntidadLocal())
        }
        ResultadoSincronizacion.Exito
    } catch (error: FirebaseFirestoreException) {
        if (error.esReintentable()) ResultadoSincronizacion.ErrorReintentable else ResultadoSincronizacion.ErrorPermanente
    } catch (_: Exception) {
        ResultadoSincronizacion.ErrorReintentable
    }

    /** Inserta una novedad cloud o actualiza una fila sincronizada sin reemplazar cambios locales más recientes. */
    private suspend fun actualizarEntidadDesdeFirebase(entidadFirebase: EntidadRecordatorio) {
        val entidadLocal = dao.obtenerPorIdentificador(entidadFirebase.identificador)
        if (entidadLocal != null && entidadLocal.estadoSincronizacion != EstadoSincronizacion.SINCRONIZADO) return
        // Conserva la versión local si ya posee una fecha más reciente que la copia cloud recibida.
        if (entidadLocal != null && entidadLocal.actualizadoEn > entidadFirebase.actualizadoEn) return
        dao.guardar(entidadFirebase)
    }

    /** Clasifica los errores transitorios para permitir que la interfaz conserve los datos locales y reintente. */
    private fun FirebaseFirestoreException.esReintentable(): Boolean = when (code) {
        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
        FirebaseFirestoreException.Code.ABORTED -> true
        else -> false
    }
}

/** Crea las dependencias persistentes una vez para que todos los ViewModel compartan la misma fuente de datos. */
object ContenedorDependencias {
    /** Mantiene la base local inicializada desde la actividad antes de crear cualquier ViewModel. */
    private lateinit var baseDatos: BaseDatosRecordatorios

    /** Expone las preferencias persistentes sin permitir recrear DataStore desde una pantalla. */
    lateinit var repositorioPreferencias: RepositorioPreferencias
        private set

    /** Expone el repositorio local-first que usa Room, la identidad Firebase y Cloud Firestore. */
    lateinit var repositorioRecordatorios: RepositorioRecordatorios
        private set

    /** Expone la conectividad observada para que los ViewModel puedan solicitar sincronización al recuperarse la red. */
    lateinit var observadorConectividad: ObservadorConectividad
        private set

    /** Expone autenticación sin permitir que pantallas Compose creen instancias Firebase directamente. */
    lateinit var repositorioAutenticacion: RepositorioAutenticacion
        private set

    /** Inicializa las instancias únicas usando el contexto de aplicación para evitar fugas de Activity. */
    fun inicializar(contexto: Context) {
        if (::baseDatos.isInitialized) return
        baseDatos = BaseDatosRecordatorios.crear(contexto.applicationContext)
        repositorioPreferencias = RepositorioPreferencias(contexto.applicationContext.almacenPreferencias)
        observadorConectividad = ObservadorConectividad(contexto.applicationContext)
        repositorioAutenticacion = RepositorioAutenticacionFirebase(
            limpiarDatosLocales = { baseDatos.daoRecordatorios().eliminarTodos() }
        )
        repositorioRecordatorios = RepositorioRecordatoriosLocalFirst(
            dao = baseDatos.daoRecordatorios(),
            fuenteFirebase = FuenteRecordatoriosFirebase(),
            proveedorIdentidad = ProveedorIdentidadFirebase()
        )
    }
}
