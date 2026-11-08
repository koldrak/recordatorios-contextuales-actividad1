package com.daille.recordatorios.datos

import androidx.annotation.StringRes
import com.daille.recordatorios.R

data class Categoria(
    val identificador: String,
    @StringRes val recursoNombre: Int
)

/** Define los niveles de urgencia. */
enum class Prioridad(@StringRes val recursoNombre: Int) {
    ALTA(R.string.prioridad_alta),
    MEDIA(R.string.prioridad_media),
    BAJA(R.string.prioridad_baja)
}

/** Limita los estados posibles para evitar estados inválidos en los recordatorios. */
sealed class EstadoRecordatorio(@StringRes val recursoNombre: Int) {
    /** Indica que el recordatorio aún debe realizarse. */
    object Pendiente : EstadoRecordatorio(R.string.estado_pendiente)

    /** Indica que el recordatorio ya fue finalizado. */
    object Completado : EstadoRecordatorio(R.string.estado_completado)

    /** Indica que la fecha límite del recordatorio ya pasó. */
    object Vencido : EstadoRecordatorio(R.string.estado_vencido)
}

/** Contiene un aviso opcional que puede asociarse a una ubicación concreta. */
data class AvisoContextual(
    @StringRes val recursoMensaje: Int,
    @StringRes val recursoUbicacion: Int? = null
) {
    init {
        // Comprueba que siempre exista un texto localizado para el aviso.
        require(recursoMensaje != 0) { "El aviso debe tener un mensaje." }
    }
}

/** Agrupa los datos inmutables que describen una tarea pendiente o completada. */
data class Recordatorio(
    val identificador: Int,
    @StringRes val recursoTitulo: Int? = null,
    @StringRes val recursoDescripcion: Int? = null,
    val tituloPersonalizado: String? = null,
    val descripcionPersonalizada: String? = null,
    val categoria: Categoria,
    val prioridad: Prioridad,
    val estado: EstadoRecordatorio,
    val aviso: AvisoContextual? = null
) {
    init {
        // Evita identificadores no válidos para diferenciar cada tarea.
        require(identificador > 0) { "El identificador debe ser mayor que cero." }
        // Exige un título localizado o escrito por la persona usuaria.
        require(recursoTitulo != null || !tituloPersonalizado.isNullOrBlank()) {
            "El recordatorio debe tener un título."
        }
    }
}

/** Provee las categorías disponibles para crear recordatorios mientras no se implementa persistencia local. */
object FuenteRecordatoriosSimulados {
    /** Usa un Set para impedir categorías repetidas según la igualdad de la data class. */
    val categoriasDisponibles: Set<Categoria> = setOf(
        Categoria("estudio", R.string.categoria_estudio),
        Categoria("trabajo", R.string.categoria_trabajo),
        Categoria("personal", R.string.categoria_personal)
    )

}
