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
    @StringRes val recursoTitulo: Int,
    val categoria: Categoria,
    val prioridad: Prioridad,
    val estado: EstadoRecordatorio,
    val aviso: AvisoContextual? = null
) {
    init {
        // Evita identificadores no válidos para diferenciar cada tarea.
        require(identificador > 0) { "El identificador debe ser mayor que cero." }
    }
}

/** Provee los datos simulados requeridos mientras no se implementa persistencia local. */
object FuenteRecordatoriosSimulados {
    /** Usa un Set para impedir categorías repetidas según la igualdad de la data class. */
    val categoriasDisponibles: Set<Categoria> = setOf(
        Categoria("estudio", R.string.categoria_estudio),
        Categoria("trabajo", R.string.categoria_trabajo),
        Categoria("personal", R.string.categoria_personal)
    )

    /** Usa una List inmutable para representar los recordatorios iniciales de la aplicación. */
    val recordatorios: List<Recordatorio> = listOf(
        Recordatorio(
            identificador = 1,
            recursoTitulo = R.string.recordatorio_entrega,
            categoria = categoriaPorIdentificador("estudio"),
            prioridad = Prioridad.ALTA,
            estado = EstadoRecordatorio.Pendiente,
            aviso = AvisoContextual(R.string.aviso_entrega, R.string.ubicacion_casa)
        ),
        Recordatorio(
            identificador = 2,
            recursoTitulo = R.string.recordatorio_reunion,
            categoria = categoriaPorIdentificador("trabajo"),
            prioridad = Prioridad.MEDIA,
            estado = EstadoRecordatorio.Pendiente
        ),
        Recordatorio(
            identificador = 3,
            recursoTitulo = R.string.recordatorio_compras,
            categoria = categoriaPorIdentificador("personal"),
            prioridad = Prioridad.BAJA,
            estado = EstadoRecordatorio.Completado
        )
    )

    /** Filtra las tareas para obtener solamente las que poseen un estado específico. */
    fun filtrarPorEstado(estadoBuscado: EstadoRecordatorio): List<Recordatorio> =
        recordatorios.filter { recordatorio -> recordatorio.estado == estadoBuscado }

    /** Filtra las tareas para obtener solamente las que poseen una prioridad específica. */
    fun filtrarPorPrioridad(prioridadBuscada: Prioridad): List<Recordatorio> =
        recordatorios.filter { recordatorio -> recordatorio.prioridad == prioridadBuscada }

    /** Busca una categoría válida antes de asignarla a un recordatorio. */
    private fun categoriaPorIdentificador(identificadorBuscado: String): Categoria =
        checkNotNull(categoriasDisponibles.find { categoria -> categoria.identificador == identificadorBuscado }) {
            "La categoría solicitada no existe."
        }
}
