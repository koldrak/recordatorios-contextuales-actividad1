package com.daille.recordatorios.datos.remoto

import com.daille.recordatorios.datos.CatalogoCategorias
import com.daille.recordatorios.datos.EstadoRecordatorio
import com.daille.recordatorios.datos.EstadoSincronizacion
import com.daille.recordatorios.datos.Prioridad
import com.daille.recordatorios.datos.local.EntidadRecordatorio
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Representa el documento cloud y evita exponer entidades Room a Firebase. */
data class ModeloRecordatorioFirebase(
    val identificador: String = "",
    val titulo: String = "",
    val descripcion: String = "",
    val categoriaIdentificador: String = "personal",
    val prioridad: String = Prioridad.MEDIA.name,
    val estado: String = "PENDIENTE",
    val fechaProgramada: Long = 0L,
    val actualizadoEn: Long = 0L
)

/** Encapsula las rutas y operaciones de Cloud Firestore dentro de la capa de datos. */
class FuenteRecordatoriosFirebase(
    // La instancia se obtiene al ejecutar una operación para que la app local abra aun sin google-services.json.
    private val obtenerFirestore: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() }
) {
    /** Guarda o actualiza el mismo documento estable dentro de users/uid/reminders/reminderId. */
    suspend fun guardarRecordatorio(
        identificadorUsuario: String,
        recordatorio: ModeloRecordatorioFirebase
    ) {
        coleccionRecordatorios(identificadorUsuario)
            .document(recordatorio.identificador)
            .set(recordatorio)
            .await()
    }

    /** Elimina el documento remoto después de que el repositorio haya marcado la eliminación local. */
    suspend fun eliminarRecordatorio(identificadorUsuario: String, identificadorRecordatorio: String) {
        coleccionRecordatorios(identificadorUsuario)
            .document(identificadorRecordatorio)
            .delete()
            .await()
    }

    /** Recupera los documentos de una sola cuenta para evitar consultas globales de recordatorios. */
    suspend fun obtenerRecordatorios(identificadorUsuario: String): List<ModeloRecordatorioFirebase> =
        coleccionRecordatorios(identificadorUsuario)
            .get()
            .await()
            .documents
            .mapNotNull { documento -> documento.toObject(ModeloRecordatorioFirebase::class.java) }

    /** Construye la ruta privada de Firestore con el uid autenticado y no con un valor recibido desde Compose. */
    private fun coleccionRecordatorios(identificadorUsuario: String) = obtenerFirestore()
        .collection("users")
        .document(identificadorUsuario)
        .collection("reminders")
}

/** Convierte la entidad persistida en Room al documento que se transmite a Firestore. */
fun EntidadRecordatorio.aModeloFirebase(): ModeloRecordatorioFirebase = ModeloRecordatorioFirebase(
    identificador = identificador,
    titulo = titulo,
    descripcion = descripcion,
    categoriaIdentificador = categoriaIdentificador,
    prioridad = prioridad.name,
    estado = estado.codigoPersistente,
    fechaProgramada = fechaProgramada,
    actualizadoEn = actualizadoEn
)

/** Convierte un documento Firestore en una entidad local sincronizada sin trasladar el modelo cloud a Compose. */
fun ModeloRecordatorioFirebase.aEntidadLocal(): EntidadRecordatorio = EntidadRecordatorio(
    identificador = identificador,
    identificadorRemoto = identificador,
    titulo = titulo,
    descripcion = descripcion,
    categoriaIdentificador = CatalogoCategorias.obtenerCategoria(categoriaIdentificador).identificador,
    prioridad = runCatching { Prioridad.valueOf(prioridad) }.getOrDefault(Prioridad.MEDIA),
    estado = EstadoRecordatorio.desdeCodigo(estado),
    fechaProgramada = fechaProgramada,
    actualizadoEn = actualizadoEn,
    estadoSincronizacion = EstadoSincronizacion.SINCRONIZADO
)
