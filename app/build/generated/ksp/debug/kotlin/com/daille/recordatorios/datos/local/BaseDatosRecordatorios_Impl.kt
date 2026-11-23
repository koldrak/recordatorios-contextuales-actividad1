package com.daille.recordatorios.datos.local

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class BaseDatosRecordatorios_Impl : BaseDatosRecordatorios() {
  private val _daoRecordatorios: Lazy<DaoRecordatorios> = lazy {
    DaoRecordatorios_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(1, "85c2be76216794e6d82bff2cc76bacf9", "9a9165358d60acab146e2b64e9e8262d") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `recordatorios` (`identificador` TEXT NOT NULL, `identificadorRemoto` TEXT, `titulo` TEXT NOT NULL, `descripcion` TEXT NOT NULL, `categoriaIdentificador` TEXT NOT NULL, `prioridad` TEXT NOT NULL, `estado` TEXT NOT NULL, `fechaProgramada` INTEGER NOT NULL, `actualizadoEn` INTEGER NOT NULL, `estadoSincronizacion` TEXT NOT NULL, `eliminado` INTEGER NOT NULL, PRIMARY KEY(`identificador`))")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_recordatorios_identificadorRemoto` ON `recordatorios` (`identificadorRemoto`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '85c2be76216794e6d82bff2cc76bacf9')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `recordatorios`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection): RoomOpenDelegate.ValidationResult {
        val _columnsRecordatorios: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsRecordatorios.put("identificador", TableInfo.Column("identificador", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRecordatorios.put("identificadorRemoto", TableInfo.Column("identificadorRemoto", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRecordatorios.put("titulo", TableInfo.Column("titulo", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRecordatorios.put("descripcion", TableInfo.Column("descripcion", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRecordatorios.put("categoriaIdentificador", TableInfo.Column("categoriaIdentificador", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRecordatorios.put("prioridad", TableInfo.Column("prioridad", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRecordatorios.put("estado", TableInfo.Column("estado", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRecordatorios.put("fechaProgramada", TableInfo.Column("fechaProgramada", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRecordatorios.put("actualizadoEn", TableInfo.Column("actualizadoEn", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRecordatorios.put("estadoSincronizacion", TableInfo.Column("estadoSincronizacion", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRecordatorios.put("eliminado", TableInfo.Column("eliminado", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysRecordatorios: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesRecordatorios: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesRecordatorios.add(TableInfo.Index("index_recordatorios_identificadorRemoto", true, listOf("identificadorRemoto"), listOf("ASC")))
        val _infoRecordatorios: TableInfo = TableInfo("recordatorios", _columnsRecordatorios, _foreignKeysRecordatorios, _indicesRecordatorios)
        val _existingRecordatorios: TableInfo = read(connection, "recordatorios")
        if (!_infoRecordatorios.equals(_existingRecordatorios)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |recordatorios(com.daille.recordatorios.datos.local.EntidadRecordatorio).
              | Expected:
              |""".trimMargin() + _infoRecordatorios + """
              |
              | Found:
              |""".trimMargin() + _existingRecordatorios)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "recordatorios")
  }

  public override fun clearAllTables() {
    super.performClear(false, "recordatorios")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(DaoRecordatorios::class, DaoRecordatorios_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>): List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun daoRecordatorios(): DaoRecordatorios = _daoRecordatorios.value
}
