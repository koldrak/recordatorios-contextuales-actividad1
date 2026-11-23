package com.daille.recordatorios.presentacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daille.recordatorios.datos.ContenedorDependencias
import com.daille.recordatorios.datos.autenticacion.RepositorioAutenticacion
import com.daille.recordatorios.datos.autenticacion.ResultadoAutenticacion
import com.daille.recordatorios.datos.autenticacion.SesionUsuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Reúne el estado de sesión y del formulario sin persistir contraseñas en almacenamiento local. */
data class EstadoAutenticacion(
    val sesion: SesionUsuario? = null,
    val cargandoSesion: Boolean = true,
    val correo: String = "",
    val contrasena: String = "",
    val modoRegistro: Boolean = false,
    val operacionEnCurso: Boolean = false,
    val error: ErrorAutenticacion? = null
)

/** Identifica errores traducibles por la interfaz sin filtrar detalles internos de Firebase. */
enum class ErrorAutenticacion {
    CAMPOS_INVALIDOS,
    CREDENCIALES,
    CONFIGURACION,
    DESCONOCIDO
}

/** Administra el registro, el inicio, la restauración y el cierre de sesión desde la capa de presentación. */
class ModeloVistaAutenticacion(
    private val repositorioAutenticacion: RepositorioAutenticacion = ContenedorDependencias.repositorioAutenticacion
) : ViewModel() {
    /** Mantiene los datos sensibles solo durante la composición y expone un flujo de lectura a la interfaz. */
    private val estadoMutable = MutableStateFlow(EstadoAutenticacion())

    /** Entrega el estado que Compose representa sin conocer las API de Firebase. */
    val estado: StateFlow<EstadoAutenticacion> = estadoMutable.asStateFlow()

    init {
        // Escucha la sesión restaurada por Firebase al abrir la app y también sus pérdidas o cierres posteriores.
        viewModelScope.launch {
            repositorioAutenticacion.observarSesion().collect { sesionActual ->
                estadoMutable.update { estadoAnterior ->
                    estadoAnterior.copy(
                        sesion = sesionActual,
                        cargandoSesion = false,
                        operacionEnCurso = false,
                        contrasena = if (sesionActual != null) "" else estadoAnterior.contrasena
                    )
                }
            }
        }
    }

    /** Conserva el correo en memoria para el formulario sin escribirlo en preferencias. */
    fun cambiarCorreo(nuevoCorreo: String) {
        estadoMutable.update { estadoAnterior -> estadoAnterior.copy(correo = nuevoCorreo, error = null) }
    }

    /** Conserva temporalmente la contraseña; nunca se guarda en Room, DataStore ni registros. */
    fun cambiarContrasena(nuevaContrasena: String) {
        estadoMutable.update { estadoAnterior -> estadoAnterior.copy(contrasena = nuevaContrasena, error = null) }
    }

    /** Alterna entre el flujo de registro y el de inicio de sesión sin modificar la sesión activa. */
    fun cambiarModoRegistro() {
        estadoMutable.update { estadoAnterior ->
            estadoAnterior.copy(modoRegistro = !estadoAnterior.modoRegistro, error = null)
        }
    }

    /** Envía las credenciales al repositorio y muestra solamente el resultado apto para la persona usuaria. */
    fun enviarCredenciales() {
        val estadoActual = estadoMutable.value
        if (!credencialesValidas(estadoActual.correo, estadoActual.contrasena) || estadoActual.operacionEnCurso) {
            estadoMutable.update { estadoAnterior -> estadoAnterior.copy(error = ErrorAutenticacion.CAMPOS_INVALIDOS) }
            return
        }
        estadoMutable.update { estadoAnterior -> estadoAnterior.copy(operacionEnCurso = true, error = null) }
        viewModelScope.launch {
            val resultado = if (estadoActual.modoRegistro) {
                repositorioAutenticacion.registrar(estadoActual.correo.trim(), estadoActual.contrasena)
            } else {
                repositorioAutenticacion.iniciarSesion(estadoActual.correo.trim(), estadoActual.contrasena)
            }
            estadoMutable.update { estadoAnterior ->
                estadoAnterior.copy(
                    operacionEnCurso = false,
                    contrasena = if (resultado == ResultadoAutenticacion.Exito) "" else estadoAnterior.contrasena,
                    error = resultado.aErrorPresentacion()
                )
            }
        }
    }

    /** Solicita el cierre de sesión y deja que el flujo de Firebase dirija nuevamente a la pantalla de acceso. */
    fun cerrarSesion() {
        viewModelScope.launch { repositorioAutenticacion.cerrarSesion() }
    }

    /** Valida el formato mínimo antes de llamar a Firebase y evita solicitudes claramente inválidas. */
    private fun credencialesValidas(correo: String, contrasena: String): Boolean =
        correo.contains("@") && contrasena.length >= 6
}

/** Convierte resultados del repositorio en errores que la interfaz puede traducir mediante recursos. */
private fun ResultadoAutenticacion.aErrorPresentacion(): ErrorAutenticacion? = when (this) {
    ResultadoAutenticacion.Exito -> null
    ResultadoAutenticacion.ErrorCredenciales -> ErrorAutenticacion.CREDENCIALES
    ResultadoAutenticacion.ErrorConfiguracion -> ErrorAutenticacion.CONFIGURACION
    ResultadoAutenticacion.ErrorDesconocido -> ErrorAutenticacion.DESCONOCIDO
}
