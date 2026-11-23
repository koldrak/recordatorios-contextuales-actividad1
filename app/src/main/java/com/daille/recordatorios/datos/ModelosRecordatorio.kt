package com.daille.recordatorios.datos

import androidx.annotation.StringRes
import com.daille.recordatorios.R

/** Representa una categoría válida que puede asignarse a un recordatorio. */
data class Categoria(
    val identificador: String,
    @StringRes val recursoNombre: Int
)

/** Define los niveles de importancia disponibles para ordenar y filtrar recordatorios. */
enum class Prioridad(@StringRes val recursoNombre: Int) {
    ALTA(R.string.prioridad_alta),
    MEDIA(R.string.prioridad_media),
    BAJA(R.string.prioridad_baja)
}

/** Restringe el estado funcional de un recordatorio a opciones conocidas por la aplicación. */
sealed class EstadoRecordatorio(@StringRes val recursoNombre: Int, val codigoPersistente: String) {
    /** Indica que la tarea aún debe realizarse. */
    data object Pendiente : EstadoRecordatorio(R.string.estado_pendiente, "PENDIENTE")

    /** Indica que la tarea fue realizada por la persona usuaria. */
    data object Completado : EstadoRecordatorio(R.string.estado_completado, "COMPLETADO")

    /** Indica que la fecha programada ya pasó sin completar la tarea. */
    data object Vencido : EstadoRecordatorio(R.string.estado_vencido, "VENCIDO")

    companion object {
        /** Recupera un estado válido desde Room y utiliza Pendiente ante un dato desconocido. */
        fun desdeCodigo(codigo: String): EstadoRecordatorio = when (codigo) {
            Completado.codigoPersistente -> Completado
            Vencido.codigoPersistente -> Vencido
            else -> Pendiente
        }
    }
}

/** Identifica si un cambio local ya fue enviado o todavía necesita sincronizarse con el servicio remoto. */
enum class EstadoSincronizacion(@StringRes val recursoNombre: Int) {
    SINCRONIZADO(R.string.sincronizacion_sincronizado),
    PENDIENTE(R.string.sincronizacion_pendiente),
    PENDIENTE_ELIMINACION(R.string.sincronizacion_pendiente_eliminacion),
    ERROR(R.string.sincronizacion_error)
}

/** Contiene la información de negocio utilizada por ViewModel y Compose, independiente de Room y Retrofit. */
data class Recordatorio(
    val identificador: String,
    val titulo: String,
    val descripcion: String,
    val categoria: Categoria,
    val prioridad: Prioridad,
    val estado: EstadoRecordatorio,
    val fechaProgramada: Long,
    val actualizadoEn: Long,
    val identificadorRemoto: String? = null,
    val estadoSincronizacion: EstadoSincronizacion = EstadoSincronizacion.PENDIENTE
)

/** Centraliza las categorías permitidas para evitar valores repetidos o inválidos en la aplicación. */
object CatalogoCategorias {
    /** Usa un Set para impedir categorías repetidas según los valores de la data class. */
    val categoriasDisponibles: Set<Categoria> = setOf(
        Categoria("estudio", R.string.categoria_estudio),
        Categoria("trabajo", R.string.categoria_trabajo),
        Categoria("personal", R.string.categoria_personal)
    )

    /** Convierte el identificador almacenado en una categoría válida y usa Personal como valor seguro. */
    fun obtenerCategoria(identificador: String): Categoria =
        categoriasDisponibles.find { categoria -> categoria.identificador == identificador }
            ?: categoriasDisponibles.first { categoria -> categoria.identificador == "personal" }
}
