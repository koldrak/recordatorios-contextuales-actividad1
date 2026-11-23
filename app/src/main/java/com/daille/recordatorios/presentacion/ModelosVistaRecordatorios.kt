package com.daille.recordatorios.presentacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daille.recordatorios.datos.CatalogoCategorias
import com.daille.recordatorios.datos.ContenedorDependencias
import com.daille.recordatorios.datos.EstadoRecordatorio
import com.daille.recordatorios.datos.Prioridad
import com.daille.recordatorios.datos.Recordatorio
import com.daille.recordatorios.datos.RepositorioRecordatorios
import com.daille.recordatorios.datos.ResultadoSincronizacion
import com.daille.recordatorios.datos.preferencias.CriterioOrdenamiento
import com.daille.recordatorios.datos.preferencias.PreferenciasAplicacion
import com.daille.recordatorios.datos.preferencias.RepositorioPreferencias
import com.daille.recordatorios.datos.red.ObservadorConectividad
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/** Administra el listado local y aplica las preferencias persistentes sin que Compose acceda a Room. */
class ModeloVistaListaRecordatorios(
    private val repositorioRecordatorios: RepositorioRecordatorios = ContenedorDependencias.repositorioRecordatorios,
    private val repositorioPreferencias: RepositorioPreferencias = ContenedorDependencias.repositorioPreferencias,
    private val observadorConectividad: ObservadorConectividad = ContenedorDependencias.observadorConectividad
) : ViewModel() {
    /** Conserva el estado mutable dentro del ViewModel y expone solo una versión de lectura a Compose. */
    private val estadoMutable = MutableStateFlow<EstadoListaRecordatorios>(EstadoListaRecordatorios.Cargando)

    /** Entrega el estado observable del listado con conciencia del ciclo de vida en la interfaz. */
    val estado: StateFlow<EstadoListaRecordatorios> = estadoMutable.asStateFlow()

    /** Guarda la última lista de Room para recalcular filtros, orden y mensajes de sincronización. */
    private var recordatoriosLocales: List<Recordatorio> = emptyList()

    /** Guarda las preferencias actuales recibidas desde DataStore. */
    private var preferenciasActuales = PreferenciasAplicacion()

    /** Evita iniciar una segunda sincronización mientras la primera continúa en ejecución. */
    private var sincronizacionEnCurso = false

    /** Conserva el resultado de la última sincronización para mostrar un mensaje recuperable. */
    private var mensajeSincronizacion: MensajeSincronizacion? = null

    init {
        // Combina Room y DataStore; cualquier cambio en ambos actualiza el estado visible automáticamente.
        observarDatosLocales()
        observarRecuperacionDeRed()
    }

    /** Solicita una sincronización al recuperar Internet sin iniciar operaciones duplicadas. */
    private fun observarRecuperacionDeRed() {
        viewModelScope.launch {
            observadorConectividad.observarConectividad()
                .distinctUntilChanged()
                .collect { hayConexion -> if (hayConexion) sincronizarRecordatorios() }
        }
    }

    /** Observa el flujo local y las preferencias persistentes sin bloquear la interfaz. */
    private fun observarDatosLocales() {
        viewModelScope.launch {
            combine(
                repositorioRecordatorios.observarRecordatorios(),
                repositorioPreferencias.preferencias
            ) { recordatorios, preferencias -> recordatorios to preferencias }
                .catch {
                    estadoMutable.value = EstadoListaRecordatorios.Error(ErrorListaRecordatorios.CARGA)
                }
                .collect { (recordatorios, preferencias) ->
                    recordatoriosLocales = recordatorios
                    preferenciasActuales = preferencias
                    publicarListado()
                }
        }
    }

    /** Solicita sincronizar las operaciones pendientes y conserva Room visible mientras se procesa la red. */
    fun sincronizarRecordatorios() {
        if (sincronizacionEnCurso) return
        sincronizacionEnCurso = true
        mensajeSincronizacion = null
        publicarListado()
        viewModelScope.launch {
            mensajeSincronizacion = when (repositorioRecordatorios.sincronizar()) {
                ResultadoSincronizacion.Exito -> MensajeSincronizacion.SINCRONIZACION_COMPLETA
                ResultadoSincronizacion.ErrorReintentable -> MensajeSincronizacion.ERROR_REINTENTABLE
                ResultadoSincronizacion.ErrorPermanente -> MensajeSincronizacion.ERROR_PERMANENTE
            }
            sincronizacionEnCurso = false
            publicarListado()
        }
    }

    /** Elimina un registro desde el repositorio; Room actualizará el listado mediante Flow. */
    fun eliminarRecordatorio(identificador: String) {
        viewModelScope.launch { repositorioRecordatorios.eliminarRecordatorio(identificador) }
    }

    /** Filtra tareas completadas y ordena el resultado usando exclusivamente los valores recibidos de DataStore. */
    private fun publicarListado() {
        val filtrados = recordatoriosLocales
            .filter { recordatorio -> preferenciasActuales.mostrarCompletados || recordatorio.estado != EstadoRecordatorio.Completado }
            .let { recordatorios ->
                when (preferenciasActuales.criterioOrdenamiento) {
                    CriterioOrdenamiento.FECHA -> recordatorios.sortedBy { recordatorio -> recordatorio.fechaProgramada }
                    CriterioOrdenamiento.PRIORIDAD -> recordatorios.sortedBy { recordatorio -> recordatorio.prioridad.ordinal }
                }
            }
        estadoMutable.value = if (filtrados.isEmpty()) {
            EstadoListaRecordatorios.Vacio(preferenciasActuales)
        } else {
            EstadoListaRecordatorios.ConDatos(
                recordatorios = filtrados,
                preferencias = preferenciasActuales,
                sincronizando = sincronizacionEnCurso,
                mensajeSincronizacion = mensajeSincronizacion
            )
        }
    }
}

/** Administra los campos y validaciones del formulario antes de crear un registro persistente. */
class ModeloVistaFormularioRecordatorio(
    private val repositorioRecordatorios: RepositorioRecordatorios = ContenedorDependencias.repositorioRecordatorios
) : ViewModel() {
    /** Conserva la información modificable del formulario dentro del ViewModel. */
    private val estadoMutable = MutableStateFlow(EstadoFormularioRecordatorio())

    /** Expone una versión de solo lectura para que Compose represente los campos. */
    val estado: StateFlow<EstadoFormularioRecordatorio> = estadoMutable.asStateFlow()

    /** Actualiza y valida el título sin eliminar el texto ingresado por la persona usuaria. */
    fun cambiarTitulo(nuevoTitulo: String) {
        actualizarYValidar { estadoActual -> estadoActual.copy(titulo = nuevoTitulo, guardadoConExito = false) }
    }

    /** Actualiza y valida la descripción sin superar el límite establecido por la actividad. */
    fun cambiarDescripcion(nuevaDescripcion: String) {
        actualizarYValidar { estadoActual -> estadoActual.copy(descripcion = nuevaDescripcion, guardadoConExito = false) }
    }

    /** Selecciona solo categorías incluidas en el catálogo permitido. */
    fun seleccionarCategoria(identificadorCategoria: String) {
        val categoria = CatalogoCategorias.categoriasDisponibles.find { categoria ->
            categoria.identificador == identificadorCategoria
        }
        actualizarYValidar { estadoActual -> estadoActual.copy(categoria = categoria, guardadoConExito = false) }
    }

    /** Conserva la prioridad seleccionada para el nuevo registro. */
    fun seleccionarPrioridad(nuevaPrioridad: Prioridad) {
        actualizarYValidar { estadoActual -> estadoActual.copy(prioridad = nuevaPrioridad, guardadoConExito = false) }
    }

    /** Crea el recordatorio en Room primero y marca el registro como pendiente de sincronización. */
    fun guardarRecordatorio() {
        actualizarYValidar { estadoActual -> estadoActual }
        val estadoActual = estadoMutable.value
        if (!estadoActual.esValido || estadoActual.enviando) return

        estadoMutable.update { estadoFormulario -> estadoFormulario.copy(enviando = true, errorGeneral = null) }
        viewModelScope.launch {
            runCatching {
                val momentoActual = System.currentTimeMillis()
                repositorioRecordatorios.crearRecordatorio(
                    Recordatorio(
                        identificador = UUID.randomUUID().toString(),
                        titulo = estadoActual.titulo.trim(),
                        descripcion = estadoActual.descripcion.trim(),
                        categoria = checkNotNull(estadoActual.categoria),
                        prioridad = checkNotNull(estadoActual.prioridad),
                        estado = EstadoRecordatorio.Pendiente,
                        fechaProgramada = momentoActual,
                        actualizadoEn = momentoActual
                    )
                )
            }.onSuccess {
                estadoMutable.update { estadoFormulario ->
                    estadoFormulario.copy(enviando = false, guardadoConExito = true)
                }
            }.onFailure {
                estadoMutable.update { estadoFormulario ->
                    estadoFormulario.copy(enviando = false, errorGeneral = ErrorGeneralFormulario.GUARDADO)
                }
            }
        }
    }

    /** Consume la señal de éxito después de que la pantalla ejecuta la navegación de regreso. */
    fun consumirGuardadoExitoso() {
        estadoMutable.update { estadoFormulario -> estadoFormulario.copy(guardadoConExito = false) }
    }

    /** Produce una nueva instancia de UiState con los errores correspondientes a cada campo. */
    private fun actualizarYValidar(transformacion: (EstadoFormularioRecordatorio) -> EstadoFormularioRecordatorio) {
        estadoMutable.update { estadoAnterior ->
            val estadoNuevo = transformacion(estadoAnterior)
            estadoNuevo.copy(
                errorTitulo = if (estadoNuevo.titulo.trim().length < 4) ErrorValidacionFormulario.TITULO_CORTO else null,
                errorDescripcion = if (estadoNuevo.descripcion.length > 200) ErrorValidacionFormulario.DESCRIPCION_LARGA else null,
                errorCategoria = if (estadoNuevo.categoria == null) ErrorValidacionFormulario.CATEGORIA_INVALIDA else null,
                errorPrioridad = if (estadoNuevo.prioridad == null) ErrorValidacionFormulario.PRIORIDAD_NO_SELECCIONADA else null
            )
        }
    }
}

/** Observa un recordatorio de Room para mostrar el detalle y permitir cambiar su estado sin perder persistencia. */
class ModeloVistaDetalleRecordatorio(
    private val repositorioRecordatorios: RepositorioRecordatorios = ContenedorDependencias.repositorioRecordatorios
) : ViewModel() {
    /** Conserva el detalle mutable dentro del ViewModel. */
    private val estadoMutable = MutableStateFlow<EstadoDetalleRecordatorio>(EstadoDetalleRecordatorio.Cargando)

    /** Expone el detalle como StateFlow de solo lectura. */
    val estado: StateFlow<EstadoDetalleRecordatorio> = estadoMutable.asStateFlow()

    /** Evita acumular recolectores si la misma pantalla recibe otro identificador. */
    private var trabajoDetalle: Job? = null

    /** Observa el registro seleccionado y actualiza la pantalla si Room modifica sus datos. */
    fun cargarDetalle(identificador: String) {
        trabajoDetalle?.cancel()
        estadoMutable.value = EstadoDetalleRecordatorio.Cargando
        trabajoDetalle = viewModelScope.launch {
            repositorioRecordatorios.observarRecordatorio(identificador).collect { recordatorio ->
                estadoMutable.value = recordatorio?.let { EstadoDetalleRecordatorio.Disponible(it) }
                    ?: EstadoDetalleRecordatorio.NoEncontrado
            }
        }
    }

    /** Alterna entre Pendiente y Completado y deja la modificación lista para sincronizarse. */
    fun alternarCompletado(recordatorio: Recordatorio) {
        val nuevoEstado = if (recordatorio.estado == EstadoRecordatorio.Completado) {
            EstadoRecordatorio.Pendiente
        } else {
            EstadoRecordatorio.Completado
        }
        viewModelScope.launch { repositorioRecordatorios.actualizarEstado(recordatorio.identificador, nuevoEstado) }
    }
}

/** Observa y actualiza las preferencias persistentes desde una pantalla separada de ajustes. */
class ModeloVistaConfiguracion(
    private val repositorioPreferencias: RepositorioPreferencias = ContenedorDependencias.repositorioPreferencias
) : ViewModel() {
    /** Convierte el Flow de DataStore en StateFlow para que Compose respete el ciclo de vida al observarlo. */
    val estado: StateFlow<EstadoConfiguracion> = repositorioPreferencias.preferencias
        .let { preferencias -> kotlinx.coroutines.flow.flow { preferencias.collect { emit(EstadoConfiguracion(it, false)) } } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoConfiguracion())

    /** Guarda el tema solicitado desde una coroutine del ViewModel. */
    fun cambiarModoOscuro(activado: Boolean) {
        viewModelScope.launch { repositorioPreferencias.guardarModoOscuro(activado) }
    }

    /** Guarda si el listado debe mostrar los recordatorios completados. */
    fun cambiarMostrarCompletados(mostrar: Boolean) {
        viewModelScope.launch { repositorioPreferencias.guardarMostrarCompletados(mostrar) }
    }

    /** Guarda el criterio con el que el listado ordena los datos locales. */
    fun cambiarCriterioOrdenamiento(criterio: CriterioOrdenamiento) {
        viewModelScope.launch { repositorioPreferencias.guardarCriterioOrdenamiento(criterio) }
    }
}
