package com.daille.recordatorios.presentacion

import com.daille.recordatorios.datos.Categoria
import com.daille.recordatorios.datos.Prioridad
import com.daille.recordatorios.datos.Recordatorio

/** Representa los estados excluyentes que puede mostrar el listado de recordatorios. */
sealed interface EstadoListaRecordatorios {
    /** Indica que la aplicación está obteniendo datos del repositorio. */
    data object Cargando : EstadoListaRecordatorios

    /** Indica que no existen recordatorios disponibles todavía. */
    data object Vacio : EstadoListaRecordatorios

    /** Contiene la información disponible para mostrar en la lista. */
    data class ConDatos(val recordatorios: List<Recordatorio>) : EstadoListaRecordatorios

    /** Comunica un error recuperable y un mensaje comprensible para la persona usuaria. */
    data class Error(val motivo: ErrorListaRecordatorios) : EstadoListaRecordatorios
}

/** Distingue los mensajes de error del listado para que la interfaz los localice en cada idioma. */
enum class ErrorListaRecordatorios {
    CARGA,
    SIMULADO
}

/** Reúne los valores y resultados del formulario de creación de un recordatorio. */
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
    /** Determina si todos los datos requeridos son válidos antes de habilitar el guardado. */
    val esValido: Boolean
        get() = titulo.trim().length >= 4 &&
            descripcion.length <= 200 &&
            categoria != null &&
            prioridad != null
}

/** Identifica un fallo general de guardado sin incluir texto dependiente de un idioma. */
enum class ErrorGeneralFormulario {
    GUARDADO
}

/** Representa el estado de la pantalla de detalle según la disponibilidad del registro solicitado. */
sealed interface EstadoDetalleRecordatorio {
    /** Indica que se está buscando el registro solicitado. */
    data object Cargando : EstadoDetalleRecordatorio

    /** Contiene el recordatorio encontrado para mostrar sus datos completos. */
    data class Disponible(val recordatorio: Recordatorio) : EstadoDetalleRecordatorio

    /** Informa que no se encontró el identificador recibido por navegación. */
    data object NoEncontrado : EstadoDetalleRecordatorio
}
