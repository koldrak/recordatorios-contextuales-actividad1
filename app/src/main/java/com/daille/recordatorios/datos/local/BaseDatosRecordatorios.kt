package com.daille.recordatorios.datos.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Upsert
import com.daille.recordatorios.datos.CatalogoCategorias
import com.daille.recordatorios.datos.EstadoRecordatorio
import com.daille.recordatorios.datos.EstadoSincronizacion
import com.daille.recordatorios.datos.Prioridad
import com.daille.recordatorios.datos.Recordatorio
import kotlinx.coroutines.flow.Flow

/** Representa la fila persistida en Room sin incluir decisiones visuales de Compose. */
@Entity(
    tableName = "recordatorios",
    indices = [Index(value = ["identificadorRemoto"], unique = true)]
)
data class EntidadRecordatorio(
    @PrimaryKey val identificador: String,
    val identificadorRemoto: String?,
    val titulo: String,
    val descripcion: String,
    val categoriaIdentificador: String,
    val prioridad: Prioridad,
    val estado: EstadoRecordatorio,
    val fechaProgramada: Long,
    val actualizadoEn: Long,
    val estadoSincronizacion: EstadoSincronizacion,
    val eliminado: Boolean = false
)

/** Declara las operaciones de almacenamiento que puede ejecutar el repositorio sobre Room. */
@Dao
interface DaoRecordatorios {
    /** Emite el listado visible cada vez que una escritura modifica la tabla. */
    @Query("SELECT * FROM recordatorios WHERE eliminado = 0 ORDER BY fechaProgramada ASC")
    fun observarTodos(): Flow<List<EntidadRecordatorio>>

    /** Emite el registro solicitado mientras exista y no haya sido marcado para eliminación. */
    @Query("SELECT * FROM recordatorios WHERE identificador = :identificador AND eliminado = 0")
    fun observarPorIdentificador(identificador: String): Flow<EntidadRecordatorio?>

    /** Recupera un registro puntual para actualizar su estado o resolver una sincronización. */
    @Query("SELECT * FROM recordatorios WHERE identificador = :identificador")
    suspend fun obtenerPorIdentificador(identificador: String): EntidadRecordatorio?

    /** Busca una fila local desde el identificador remoto para actualizar el mismo registro sin duplicarlo. */
    @Query("SELECT * FROM recordatorios WHERE identificadorRemoto = :identificadorRemoto")
    suspend fun obtenerPorIdentificadorRemoto(identificadorRemoto: String): EntidadRecordatorio?

    /** Permite filtrar por prioridad sin entregar la tabla directamente a la interfaz. */
    @Query("SELECT * FROM recordatorios WHERE eliminado = 0 AND prioridad = :prioridad ORDER BY fechaProgramada ASC")
    fun observarPorPrioridad(prioridad: Prioridad): Flow<List<EntidadRecordatorio>>

    /** Obtiene las operaciones que necesitan enviarse al servicio remoto. */
    @Query("SELECT * FROM recordatorios WHERE estadoSincronizacion != 'SINCRONIZADO' ORDER BY actualizadoEn ASC")
    suspend fun obtenerPendientesDeSincronizacion(): List<EntidadRecordatorio>

    /** Inserta un nuevo registro o actualiza uno existente con el mismo identificador estable. */
    @Upsert
    suspend fun guardar(entidad: EntidadRecordatorio)

    /** Actualiza el estado funcional y deja el cambio preparado para la siguiente sincronización. */
    @Query("UPDATE recordatorios SET estado = :estado, actualizadoEn = :actualizadoEn, estadoSincronizacion = :estadoSincronizacion WHERE identificador = :identificador")
    suspend fun actualizarEstado(
        identificador: String,
        estado: EstadoRecordatorio,
        actualizadoEn: Long,
        estadoSincronizacion: EstadoSincronizacion
    )

    /** Marca una eliminación local para que el repositorio pueda enviarla antes de retirar la fila. */
    @Query("UPDATE recordatorios SET eliminado = 1, actualizadoEn = :actualizadoEn, estadoSincronizacion = :estadoSincronizacion WHERE identificador = :identificador")
    suspend fun marcarEliminado(
        identificador: String,
        actualizadoEn: Long,
        estadoSincronizacion: EstadoSincronizacion
    )

    /** Confirma una operación remota y conserva el identificador asignado por el servicio. */
    @Query("UPDATE recordatorios SET identificadorRemoto = :identificadorRemoto, estadoSincronizacion = :estadoSincronizacion WHERE identificador = :identificador")
    suspend fun confirmarSincronizacion(
        identificador: String,
        identificadorRemoto: String?,
        estadoSincronizacion: EstadoSincronizacion = EstadoSincronizacion.SINCRONIZADO
    )

    /** Registra que un cambio local sigue disponible pero necesita que la persona reintente sincronizarlo. */
    @Query("UPDATE recordatorios SET estadoSincronizacion = :estadoSincronizacion WHERE identificador = :identificador")
    suspend fun actualizarEstadoSincronizacion(
        identificador: String,
        estadoSincronizacion: EstadoSincronizacion
    )

    /** Elimina definitivamente una fila solo después de confirmar su eliminación remota. */
    @Query("DELETE FROM recordatorios WHERE identificador = :identificador")
    suspend fun eliminarDefinitivamente(identificador: String)
}

/** Convierte los tipos propios de Kotlin en valores simples que SQLite puede almacenar. */
class ConversoresBaseDatos {
    /** Guarda una prioridad como texto estable. */
    @TypeConverter
    fun prioridadATexto(prioridad: Prioridad): String = prioridad.name

    /** Recupera una prioridad y protege la lectura frente a valores desconocidos. */
    @TypeConverter
    fun textoAPrioridad(valor: String): Prioridad =
        runCatching { Prioridad.valueOf(valor) }.getOrDefault(Prioridad.MEDIA)

    /** Guarda el estado funcional mediante su código persistente. */
    @TypeConverter
    fun estadoATexto(estado: EstadoRecordatorio): String = estado.codigoPersistente

    /** Reconstruye el estado funcional desde el valor almacenado en SQLite. */
    @TypeConverter
    fun textoAEstado(valor: String): EstadoRecordatorio = EstadoRecordatorio.desdeCodigo(valor)

    /** Guarda el estado de sincronización mediante el nombre de la enumeración. */
    @TypeConverter
    fun estadoSincronizacionATexto(estado: EstadoSincronizacion): String = estado.name

    /** Recupera el estado de sincronización y deja la operación pendiente cuando el valor es inválido. */
    @TypeConverter
    fun textoAEstadoSincronizacion(valor: String): EstadoSincronizacion =
        runCatching { EstadoSincronizacion.valueOf(valor) }.getOrDefault(EstadoSincronizacion.PENDIENTE)
}

/** Reúne el esquema de Room y expone el DAO utilizado por el repositorio. */
@Database(entities = [EntidadRecordatorio::class], version = 1, exportSchema = true)
@TypeConverters(ConversoresBaseDatos::class)
abstract class BaseDatosRecordatorios : RoomDatabase() {
    /** Entrega el único DAO que administra las filas de recordatorios. */
    abstract fun daoRecordatorios(): DaoRecordatorios

    companion object {
        /** Construye una sola instancia de Room usando el contexto de la aplicación. */
        fun crear(contexto: Context): BaseDatosRecordatorios =
            Room.databaseBuilder(
                contexto.applicationContext,
                BaseDatosRecordatorios::class.java,
                "recordatorios.db"
            ).build()
    }
}

/** Transforma una entidad de Room en el modelo de dominio que consumen ViewModel y Compose. */
fun EntidadRecordatorio.aDominio(): Recordatorio = Recordatorio(
    identificador = identificador,
    titulo = titulo,
    descripcion = descripcion,
    categoria = CatalogoCategorias.obtenerCategoria(categoriaIdentificador),
    prioridad = prioridad,
    estado = estado,
    fechaProgramada = fechaProgramada,
    actualizadoEn = actualizadoEn,
    identificadorRemoto = identificadorRemoto,
    estadoSincronizacion = estadoSincronizacion
)

/** Transforma un modelo de dominio en una entidad lista para persistirse en Room. */
fun Recordatorio.aEntidad(eliminado: Boolean = false): EntidadRecordatorio = EntidadRecordatorio(
    identificador = identificador,
    identificadorRemoto = identificadorRemoto,
    titulo = titulo,
    descripcion = descripcion,
    categoriaIdentificador = categoria.identificador,
    prioridad = prioridad,
    estado = estado,
    fechaProgramada = fechaProgramada,
    actualizadoEn = actualizadoEn,
    estadoSincronizacion = estadoSincronizacion,
    eliminado = eliminado
)
