package com.daille.recordatorios.presentacion

import com.daille.recordatorios.datos.Categoria
import com.daille.recordatorios.datos.Prioridad
import com.daille.recordatorios.datos.Recordatorio
import com.daille.recordatorios.datos.preferencias.CriterioOrdenamiento
import com.daille.recordatorios.datos.preferencias.PreferenciasAplicacion

/** Representa alternativas excluyentes para el contenido principal del listado. */
sealed interface EstadoListaRecordatorios {
    /** Indica que Room o DataStore todavía están entregando la primera emisión. */
    data object Cargando : EstadoListaRecordatorios

    /** Comunica que la base local no contiene recordatorios visibles y permite crear uno. */
    data class Vacio(val preferencias: PreferenciasAplicacion) : EstadoListaRecordatorios

    /** Contiene datos locales disponibles y el estado de una sincronización opcional. */
    data class ConDatos(
        val recordatorios: List<Recordatorio>,
        val preferencias: PreferenciasAplicacion,
        val sincronizando: Boolean = false,
        val mensajeSincronizacion: MensajeSincronizacion? = null
    ) : EstadoListaRecordatorios

    /** Comunica un error recuperable sin reemplazar datos locales previamente disponibles. */
    data class Error(val motivo: ErrorListaRecordatorios) : EstadoListaRecordatorios
}

/** Identifica mensajes de sincronización que Compose traduce mediante recursos localizados. */
enum class MensajeSincronizacion {
    SINCRONIZACION_COMPLETA,
    ERROR_REINTENTABLE,
    ERROR_PERMANENTE,
    // Representa la falta de identidad sin exponer Firebase ni mensajes dependientes del idioma al ViewModel.
    ERROR_SIN_SESION
}

/** Distingue los errores de carga que requieren que la persona usuaria reintente la operación. */
enum class ErrorListaRecordatorios {
    CARGA
}

/** Reúne los valores y errores del formulario antes de crear un nuevo registro persistente. */
data class EstadoFormularioRecordatorio(
    val titulo: String = "",
    val descripcion: String = "",
    val categoria: Categoria? = null,
    val prioridad: Prioridad? = null,
    val errorTitulo: ErrorValidacionFormulario? = null,
    val errorDescripcion: ErrorValidacionFormulario? = null,
    val errorCategoria: ErrorValidacionFormulario? = null,
    val errorPrioridad: ErrorValidacionFormulario? = null,
    val enviando: Boolean = false,
    val guardadoConExito: Boolean = false,
    val errorGeneral: ErrorGeneralFormulario? = null
) {
    /** Verifica que el botón Guardar solo se habilite cuando la información es válida. */
    val esValido: Boolean
        get() = titulo.trim().length >= 4 && descripcion.length <= 200 && categoria != null && prioridad != null
}

/** Identifica una validación del formulario sin guardar textos dependientes de un idioma en el ViewModel. */
enum class ErrorValidacionFormulario {
    TITULO_CORTO,
    DESCRIPCION_LARGA,
    CATEGORIA_INVALIDA,
    PRIORIDAD_NO_SELECCIONADA
}

/** Identifica un error recuperable al persistir un recordatorio en la capa de datos. */
enum class ErrorGeneralFormulario {
    GUARDADO
}

/** Representa la pantalla de detalle según el resultado del Flow de Room. */
sealed interface EstadoDetalleRecordatorio {
    /** Informa que el detalle todavía espera el registro solicitado. */
    data object Cargando : EstadoDetalleRecordatorio

    /** Contiene el recordatorio local que puede consultarse o actualizarse. */
    data class Disponible(val recordatorio: Recordatorio) : EstadoDetalleRecordatorio

    /** Informa que no existe una fila visible con el identificador recibido. */
    data object NoEncontrado : EstadoDetalleRecordatorio
}

/** Conserva las preferencias leídas desde DataStore para la pantalla de ajustes. */
data class EstadoConfiguracion(
    val preferencias: PreferenciasAplicacion = PreferenciasAplicacion(),
    val cargando: Boolean = true
)

/** Expone los valores de orden disponibles en la interfaz sin filtrar datos directamente en Compose. */
val criteriosDisponibles: List<CriterioOrdenamiento> = listOf(
    CriterioOrdenamiento.FECHA,
    CriterioOrdenamiento.PRIORIDAD
)
