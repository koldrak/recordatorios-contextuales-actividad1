package com.daille.recordatorios.presentacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daille.recordatorios.datos.ContenedorDependencias
import com.daille.recordatorios.datos.FuenteRecordatoriosSimulados
import com.daille.recordatorios.datos.Prioridad
import com.daille.recordatorios.datos.Recordatorio
import com.daille.recordatorios.datos.RepositorioRecordatorios
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/** Identifica las validaciones posibles sin acoplar la lógica a un idioma de la interfaz. */
enum class ErrorValidacionFormulario {
    TITULO_CORTO,
    DESCRIPCION_LARGA,
    CATEGORIA_INVALIDA,
    PRIORIDAD_NO_SELECCIONADA
}

/** Administra el estado y los eventos de la pantalla de listado. */
class ModeloVistaListaRecordatorios(
    private val repositorio: RepositorioRecordatorios = ContenedorDependencias.repositorioRecordatorios
) : ViewModel() {
    /** Guarda internamente el estado modificable del listado. */
    private val estadoMutable = MutableStateFlow<EstadoListaRecordatorios>(EstadoListaRecordatorios.Cargando)

    /** Expone una vista de solo lectura para que la interfaz observe el estado. */
    val estado: StateFlow<EstadoListaRecordatorios> = estadoMutable.asStateFlow()

    /** Indica que la interfaz está mostrando una simulación y no debe reemplazarse por el flujo real. */
    private var estadoSimulado = false

    /** Evita reemplazar el estado de carga inicial antes de que la consulta simulada finalice. */
    private var cargaInicialFinalizada = false

    init {
        // Inicia la carga al crear el ViewModel para separar la lógica de la función componible.
        observarCambiosRepositorio()
        cargarRecordatorios()
    }

    /** Consulta el repositorio y representa carga, contenido, vacío o error según el resultado. */
    fun cargarRecordatorios() {
        estadoSimulado = false
        cargaInicialFinalizada = false
        estadoMutable.value = EstadoListaRecordatorios.Cargando
        viewModelScope.launch {
            runCatching { repositorio.obtenerRecordatorios() }
                .onSuccess { recordatorios ->
                    cargaInicialFinalizada = true
                    publicarRecordatorios(recordatorios)
                }
                .onFailure {
                    estadoMutable.value = EstadoListaRecordatorios.Error(ErrorListaRecordatorios.CARGA)
                }
        }
    }

    /** Expone un estado de error temporal para comprobar el mensaje y la acción de reintento. */
    fun simularEstadoError() {
        estadoSimulado = true
        estadoMutable.value = EstadoListaRecordatorios.Error(ErrorListaRecordatorios.SIMULADO)
    }

    /** Escucha los cambios del repositorio para actualizar el listado después de guardar y volver atrás. */
    private fun observarCambiosRepositorio() {
        viewModelScope.launch {
            repositorio.observarRecordatorios().collect { recordatorios ->
                if (cargaInicialFinalizada && !estadoSimulado) {
                    publicarRecordatorios(recordatorios)
                }
            }
        }
    }

    /** Convierte la lista del repositorio en el estado visible correspondiente. */
    private fun publicarRecordatorios(recordatorios: List<Recordatorio>) {
        estadoMutable.value = if (recordatorios.isEmpty()) {
            EstadoListaRecordatorios.Vacio
        } else {
            EstadoListaRecordatorios.ConDatos(recordatorios)
        }
    }

    /** Elimina un registro mediante el repositorio; el StateFlow actualizará la lista resultante. */
    fun eliminarRecordatorio(identificador: Int) {
        viewModelScope.launch {
            repositorio.eliminarRecordatorio(identificador)
        }
    }
}

/** Administra los campos, validaciones y guardado del formulario de creación. */
class ModeloVistaFormularioRecordatorio(
    private val repositorio: RepositorioRecordatorios = ContenedorDependencias.repositorioRecordatorios
) : ViewModel() {
    /** Conserva la versión modificable del formulario dentro del ViewModel. */
    private val estadoMutable = MutableStateFlow(EstadoFormularioRecordatorio())

    /** Expone el estado inmutable que la pantalla observa mediante StateFlow. */
    val estado: StateFlow<EstadoFormularioRecordatorio> = estadoMutable.asStateFlow()

    /** Solicita que el siguiente intento de guardado termine con un error para fines de verificación. */
    private var forzarErrorGuardado = false

    /** Actualiza y valida el título ingresado por la persona usuaria. */
    fun cambiarTitulo(nuevoTitulo: String) {
        actualizarYValidar { estadoActual -> estadoActual.copy(titulo = nuevoTitulo, guardadoConExito = false) }
    }

    /** Actualiza y valida la descripción ingresada por la persona usuaria. */
    fun cambiarDescripcion(nuevaDescripcion: String) {
        actualizarYValidar { estadoActual -> estadoActual.copy(descripcion = nuevaDescripcion, guardadoConExito = false) }
    }

    /** Selecciona una categoría y verifica que pertenezca a las opciones permitidas. */
    fun seleccionarCategoria(identificadorCategoria: String) {
        val categoria = FuenteRecordatoriosSimulados.categoriasDisponibles.find { elemento ->
            elemento.identificador == identificadorCategoria
        }
        actualizarYValidar { estadoActual -> estadoActual.copy(categoria = categoria, guardadoConExito = false) }
    }

    /** Selecciona una prioridad para el nuevo recordatorio. */
    fun seleccionarPrioridad(nuevaPrioridad: Prioridad) {
        actualizarYValidar { estadoActual -> estadoActual.copy(prioridad = nuevaPrioridad, guardadoConExito = false) }
    }

    /** Valida el formulario y guarda un nuevo registro mediante una operación asincrónica. */
    fun guardarRecordatorio() {
        actualizarYValidar { estadoActual -> estadoActual }
        val estadoActual = estadoMutable.value
        if (!estadoActual.esValido || estadoActual.enviando) return

        estadoMutable.update { estadoFormulario -> estadoFormulario.copy(enviando = true, errorGeneral = null) }
        viewModelScope.launch {
            runCatching {
                if (forzarErrorGuardado) {
                    forzarErrorGuardado = false
                    delay(550)
                    throw IllegalStateException("Error de guardado simulado")
                }
                val siguienteIdentificador = (repositorio.obtenerRecordatorios().maxOfOrNull { it.identificador } ?: 0) + 1
                Recordatorio(
                    identificador = siguienteIdentificador,
                    tituloPersonalizado = estadoActual.titulo.trim(),
                    descripcionPersonalizada = estadoActual.descripcion.trim(),
                    categoria = checkNotNull(estadoActual.categoria),
                    prioridad = checkNotNull(estadoActual.prioridad),
                    estado = com.daille.recordatorios.datos.EstadoRecordatorio.Pendiente
                ).also { recordatorio -> repositorio.guardarRecordatorio(recordatorio) }
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

    /** Ejecuta un envío que finaliza con error para comprobar el mensaje general del formulario. */
    fun simularErrorDeGuardado() {
        forzarErrorGuardado = true
        guardarRecordatorio()
    }

    /** Restablece el resultado de éxito una vez que la pantalla procesa la navegación de regreso. */
    fun consumirGuardadoExitoso() {
        estadoMutable.update { estadoFormulario -> estadoFormulario.copy(guardadoConExito = false) }
    }

    /** Actualiza el formulario creando una nueva instancia y calculando todos los errores de validación. */
    private fun actualizarYValidar(transformacion: (EstadoFormularioRecordatorio) -> EstadoFormularioRecordatorio) {
        estadoMutable.update { estadoAnterior ->
            val estadoNuevo = transformacion(estadoAnterior)
            estadoNuevo.copy(
                errorTitulo = if (estadoNuevo.titulo.trim().length < 4) ErrorValidacionFormulario.TITULO_CORTO else null,
                errorDescripcion = if (estadoNuevo.descripcion.length > 200) ErrorValidacionFormulario.DESCRIPCION_LARGA else null,
                errorCategoria = if (
                    estadoNuevo.categoria == null ||
                    estadoNuevo.categoria !in FuenteRecordatoriosSimulados.categoriasDisponibles
                ) {
                    ErrorValidacionFormulario.CATEGORIA_INVALIDA
                } else {
                    null
                },
                errorPrioridad = if (estadoNuevo.prioridad == null) {
                    ErrorValidacionFormulario.PRIORIDAD_NO_SELECCIONADA
                } else {
                    null
                }
            )
        }
    }
}

/** Recupera el registro que debe mostrarse en la pantalla de detalle. */
class ModeloVistaDetalleRecordatorio(
    private val repositorio: RepositorioRecordatorios = ContenedorDependencias.repositorioRecordatorios
) : ViewModel() {
    /** Almacena el resultado de buscar el identificador recibido por navegación. */
    private val estadoMutable = MutableStateFlow<EstadoDetalleRecordatorio>(EstadoDetalleRecordatorio.Cargando)

    /** Expone un flujo de solo lectura para que la pantalla represente el detalle. */
    val estado: StateFlow<EstadoDetalleRecordatorio> = estadoMutable.asStateFlow()

    /** Busca el registro indicado y actualiza el estado con una alternativa mutuamente excluyente. */
    fun cargarDetalle(identificador: Int) {
        estadoMutable.value = EstadoDetalleRecordatorio.Cargando
        viewModelScope.launch {
            estadoMutable.value = repositorio.obtenerRecordatorio(identificador)?.let { recordatorio ->
                EstadoDetalleRecordatorio.Disponible(recordatorio)
            } ?: EstadoDetalleRecordatorio.NoEncontrado
        }
    }
}
