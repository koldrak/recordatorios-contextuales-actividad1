package com.daille.recordatorios.datos.local

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.EntityUpsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.daille.recordatorios.datos.EstadoRecordatorio
import com.daille.recordatorios.datos.EstadoSincronizacion
import com.daille.recordatorios.datos.Prioridad
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class DaoRecordatorios_Impl(
  __db: RoomDatabase,
) : DaoRecordatorios {
  private val __db: RoomDatabase

  private val __upsertAdapterOfEntidadRecordatorio: EntityUpsertAdapter<EntidadRecordatorio>

  private val __conversoresBaseDatos: ConversoresBaseDatos = ConversoresBaseDatos()
  init {
    this.__db = __db
    this.__upsertAdapterOfEntidadRecordatorio = EntityUpsertAdapter<EntidadRecordatorio>(object : EntityInsertAdapter<EntidadRecordatorio>() {
      protected override fun createQuery(): String = "INSERT INTO `recordatorios` (`identificador`,`identificadorRemoto`,`titulo`,`descripcion`,`categoriaIdentificador`,`prioridad`,`estado`,`fechaProgramada`,`actualizadoEn`,`estadoSincronizacion`,`eliminado`) VALUES (?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: EntidadRecordatorio) {
        statement.bindText(1, entity.identificador)
        val _tmpIdentificadorRemoto: String? = entity.identificadorRemoto
        if (_tmpIdentificadorRemoto == null) {
          statement.bindNull(2)
        } else {
          statement.bindText(2, _tmpIdentificadorRemoto)
        }
        statement.bindText(3, entity.titulo)
        statement.bindText(4, entity.descripcion)
        statement.bindText(5, entity.categoriaIdentificador)
        val _tmp: String = __conversoresBaseDatos.prioridadATexto(entity.prioridad)
        statement.bindText(6, _tmp)
        val _tmp_1: String = __conversoresBaseDatos.estadoATexto(entity.estado)
        statement.bindText(7, _tmp_1)
        statement.bindLong(8, entity.fechaProgramada)
        statement.bindLong(9, entity.actualizadoEn)
        val _tmp_2: String = __conversoresBaseDatos.estadoSincronizacionATexto(entity.estadoSincronizacion)
        statement.bindText(10, _tmp_2)
        val _tmp_3: Int = if (entity.eliminado) 1 else 0
        statement.bindLong(11, _tmp_3.toLong())
      }
    }, object : EntityDeleteOrUpdateAdapter<EntidadRecordatorio>() {
      protected override fun createQuery(): String = "UPDATE `recordatorios` SET `identificador` = ?,`identificadorRemoto` = ?,`titulo` = ?,`descripcion` = ?,`categoriaIdentificador` = ?,`prioridad` = ?,`estado` = ?,`fechaProgramada` = ?,`actualizadoEn` = ?,`estadoSincronizacion` = ?,`eliminado` = ? WHERE `identificador` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: EntidadRecordatorio) {
        statement.bindText(1, entity.identificador)
        val _tmpIdentificadorRemoto: String? = entity.identificadorRemoto
        if (_tmpIdentificadorRemoto == null) {
          statement.bindNull(2)
        } else {
          statement.bindText(2, _tmpIdentificadorRemoto)
        }
        statement.bindText(3, entity.titulo)
        statement.bindText(4, entity.descripcion)
        statement.bindText(5, entity.categoriaIdentificador)
        val _tmp: String = __conversoresBaseDatos.prioridadATexto(entity.prioridad)
        statement.bindText(6, _tmp)
        val _tmp_1: String = __conversoresBaseDatos.estadoATexto(entity.estado)
        statement.bindText(7, _tmp_1)
        statement.bindLong(8, entity.fechaProgramada)
        statement.bindLong(9, entity.actualizadoEn)
        val _tmp_2: String = __conversoresBaseDatos.estadoSincronizacionATexto(entity.estadoSincronizacion)
        statement.bindText(10, _tmp_2)
        val _tmp_3: Int = if (entity.eliminado) 1 else 0
        statement.bindLong(11, _tmp_3.toLong())
        statement.bindText(12, entity.identificador)
      }
    })
  }

  public override suspend fun guardar(entidad: EntidadRecordatorio): Unit = performSuspending(__db, false, true) { _connection ->
    __upsertAdapterOfEntidadRecordatorio.upsert(_connection, entidad)
  }

  public override fun observarTodos(): Flow<List<EntidadRecordatorio>> {
    val _sql: String = "SELECT * FROM recordatorios WHERE eliminado = 0 ORDER BY fechaProgramada ASC"
    return createFlow(__db, false, arrayOf("recordatorios")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfIdentificador: Int = getColumnIndexOrThrow(_stmt, "identificador")
        val _columnIndexOfIdentificadorRemoto: Int = getColumnIndexOrThrow(_stmt, "identificadorRemoto")
        val _columnIndexOfTitulo: Int = getColumnIndexOrThrow(_stmt, "titulo")
        val _columnIndexOfDescripcion: Int = getColumnIndexOrThrow(_stmt, "descripcion")
        val _columnIndexOfCategoriaIdentificador: Int = getColumnIndexOrThrow(_stmt, "categoriaIdentificador")
        val _columnIndexOfPrioridad: Int = getColumnIndexOrThrow(_stmt, "prioridad")
        val _columnIndexOfEstado: Int = getColumnIndexOrThrow(_stmt, "estado")
        val _columnIndexOfFechaProgramada: Int = getColumnIndexOrThrow(_stmt, "fechaProgramada")
        val _columnIndexOfActualizadoEn: Int = getColumnIndexOrThrow(_stmt, "actualizadoEn")
        val _columnIndexOfEstadoSincronizacion: Int = getColumnIndexOrThrow(_stmt, "estadoSincronizacion")
        val _columnIndexOfEliminado: Int = getColumnIndexOrThrow(_stmt, "eliminado")
        val _result: MutableList<EntidadRecordatorio> = mutableListOf()
        while (_stmt.step()) {
          val _item: EntidadRecordatorio
          val _tmpIdentificador: String
          _tmpIdentificador = _stmt.getText(_columnIndexOfIdentificador)
          val _tmpIdentificadorRemoto: String?
          if (_stmt.isNull(_columnIndexOfIdentificadorRemoto)) {
            _tmpIdentificadorRemoto = null
          } else {
            _tmpIdentificadorRemoto = _stmt.getText(_columnIndexOfIdentificadorRemoto)
          }
          val _tmpTitulo: String
          _tmpTitulo = _stmt.getText(_columnIndexOfTitulo)
          val _tmpDescripcion: String
          _tmpDescripcion = _stmt.getText(_columnIndexOfDescripcion)
          val _tmpCategoriaIdentificador: String
          _tmpCategoriaIdentificador = _stmt.getText(_columnIndexOfCategoriaIdentificador)
          val _tmpPrioridad: Prioridad
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfPrioridad)
          _tmpPrioridad = __conversoresBaseDatos.textoAPrioridad(_tmp)
          val _tmpEstado: EstadoRecordatorio
          val _tmp_1: String
          _tmp_1 = _stmt.getText(_columnIndexOfEstado)
          _tmpEstado = __conversoresBaseDatos.textoAEstado(_tmp_1)
          val _tmpFechaProgramada: Long
          _tmpFechaProgramada = _stmt.getLong(_columnIndexOfFechaProgramada)
          val _tmpActualizadoEn: Long
          _tmpActualizadoEn = _stmt.getLong(_columnIndexOfActualizadoEn)
          val _tmpEstadoSincronizacion: EstadoSincronizacion
          val _tmp_2: String
          _tmp_2 = _stmt.getText(_columnIndexOfEstadoSincronizacion)
          _tmpEstadoSincronizacion = __conversoresBaseDatos.textoAEstadoSincronizacion(_tmp_2)
          val _tmpEliminado: Boolean
          val _tmp_3: Int
          _tmp_3 = _stmt.getLong(_columnIndexOfEliminado).toInt()
          _tmpEliminado = _tmp_3 != 0
          _item = EntidadRecordatorio(_tmpIdentificador,_tmpIdentificadorRemoto,_tmpTitulo,_tmpDescripcion,_tmpCategoriaIdentificador,_tmpPrioridad,_tmpEstado,_tmpFechaProgramada,_tmpActualizadoEn,_tmpEstadoSincronizacion,_tmpEliminado)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observarPorIdentificador(identificador: String): Flow<EntidadRecordatorio?> {
    val _sql: String = "SELECT * FROM recordatorios WHERE identificador = ? AND eliminado = 0"
    return createFlow(__db, false, arrayOf("recordatorios")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, identificador)
        val _columnIndexOfIdentificador: Int = getColumnIndexOrThrow(_stmt, "identificador")
        val _columnIndexOfIdentificadorRemoto: Int = getColumnIndexOrThrow(_stmt, "identificadorRemoto")
        val _columnIndexOfTitulo: Int = getColumnIndexOrThrow(_stmt, "titulo")
        val _columnIndexOfDescripcion: Int = getColumnIndexOrThrow(_stmt, "descripcion")
        val _columnIndexOfCategoriaIdentificador: Int = getColumnIndexOrThrow(_stmt, "categoriaIdentificador")
        val _columnIndexOfPrioridad: Int = getColumnIndexOrThrow(_stmt, "prioridad")
        val _columnIndexOfEstado: Int = getColumnIndexOrThrow(_stmt, "estado")
        val _columnIndexOfFechaProgramada: Int = getColumnIndexOrThrow(_stmt, "fechaProgramada")
        val _columnIndexOfActualizadoEn: Int = getColumnIndexOrThrow(_stmt, "actualizadoEn")
        val _columnIndexOfEstadoSincronizacion: Int = getColumnIndexOrThrow(_stmt, "estadoSincronizacion")
        val _columnIndexOfEliminado: Int = getColumnIndexOrThrow(_stmt, "eliminado")
        val _result: EntidadRecordatorio?
        if (_stmt.step()) {
          val _tmpIdentificador: String
          _tmpIdentificador = _stmt.getText(_columnIndexOfIdentificador)
          val _tmpIdentificadorRemoto: String?
          if (_stmt.isNull(_columnIndexOfIdentificadorRemoto)) {
            _tmpIdentificadorRemoto = null
          } else {
            _tmpIdentificadorRemoto = _stmt.getText(_columnIndexOfIdentificadorRemoto)
          }
          val _tmpTitulo: String
          _tmpTitulo = _stmt.getText(_columnIndexOfTitulo)
          val _tmpDescripcion: String
          _tmpDescripcion = _stmt.getText(_columnIndexOfDescripcion)
          val _tmpCategoriaIdentificador: String
          _tmpCategoriaIdentificador = _stmt.getText(_columnIndexOfCategoriaIdentificador)
          val _tmpPrioridad: Prioridad
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfPrioridad)
          _tmpPrioridad = __conversoresBaseDatos.textoAPrioridad(_tmp)
          val _tmpEstado: EstadoRecordatorio
          val _tmp_1: String
          _tmp_1 = _stmt.getText(_columnIndexOfEstado)
          _tmpEstado = __conversoresBaseDatos.textoAEstado(_tmp_1)
          val _tmpFechaProgramada: Long
          _tmpFechaProgramada = _stmt.getLong(_columnIndexOfFechaProgramada)
          val _tmpActualizadoEn: Long
          _tmpActualizadoEn = _stmt.getLong(_columnIndexOfActualizadoEn)
          val _tmpEstadoSincronizacion: EstadoSincronizacion
          val _tmp_2: String
          _tmp_2 = _stmt.getText(_columnIndexOfEstadoSincronizacion)
          _tmpEstadoSincronizacion = __conversoresBaseDatos.textoAEstadoSincronizacion(_tmp_2)
          val _tmpEliminado: Boolean
          val _tmp_3: Int
          _tmp_3 = _stmt.getLong(_columnIndexOfEliminado).toInt()
          _tmpEliminado = _tmp_3 != 0
          _result = EntidadRecordatorio(_tmpIdentificador,_tmpIdentificadorRemoto,_tmpTitulo,_tmpDescripcion,_tmpCategoriaIdentificador,_tmpPrioridad,_tmpEstado,_tmpFechaProgramada,_tmpActualizadoEn,_tmpEstadoSincronizacion,_tmpEliminado)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun obtenerPorIdentificador(identificador: String): EntidadRecordatorio? {
    val _sql: String = "SELECT * FROM recordatorios WHERE identificador = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, identificador)
        val _columnIndexOfIdentificador: Int = getColumnIndexOrThrow(_stmt, "identificador")
        val _columnIndexOfIdentificadorRemoto: Int = getColumnIndexOrThrow(_stmt, "identificadorRemoto")
        val _columnIndexOfTitulo: Int = getColumnIndexOrThrow(_stmt, "titulo")
        val _columnIndexOfDescripcion: Int = getColumnIndexOrThrow(_stmt, "descripcion")
        val _columnIndexOfCategoriaIdentificador: Int = getColumnIndexOrThrow(_stmt, "categoriaIdentificador")
        val _columnIndexOfPrioridad: Int = getColumnIndexOrThrow(_stmt, "prioridad")
        val _columnIndexOfEstado: Int = getColumnIndexOrThrow(_stmt, "estado")
        val _columnIndexOfFechaProgramada: Int = getColumnIndexOrThrow(_stmt, "fechaProgramada")
        val _columnIndexOfActualizadoEn: Int = getColumnIndexOrThrow(_stmt, "actualizadoEn")
        val _columnIndexOfEstadoSincronizacion: Int = getColumnIndexOrThrow(_stmt, "estadoSincronizacion")
        val _columnIndexOfEliminado: Int = getColumnIndexOrThrow(_stmt, "eliminado")
        val _result: EntidadRecordatorio?
        if (_stmt.step()) {
          val _tmpIdentificador: String
          _tmpIdentificador = _stmt.getText(_columnIndexOfIdentificador)
          val _tmpIdentificadorRemoto: String?
          if (_stmt.isNull(_columnIndexOfIdentificadorRemoto)) {
            _tmpIdentificadorRemoto = null
          } else {
            _tmpIdentificadorRemoto = _stmt.getText(_columnIndexOfIdentificadorRemoto)
          }
          val _tmpTitulo: String
          _tmpTitulo = _stmt.getText(_columnIndexOfTitulo)
          val _tmpDescripcion: String
          _tmpDescripcion = _stmt.getText(_columnIndexOfDescripcion)
          val _tmpCategoriaIdentificador: String
          _tmpCategoriaIdentificador = _stmt.getText(_columnIndexOfCategoriaIdentificador)
          val _tmpPrioridad: Prioridad
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfPrioridad)
          _tmpPrioridad = __conversoresBaseDatos.textoAPrioridad(_tmp)
          val _tmpEstado: EstadoRecordatorio
          val _tmp_1: String
          _tmp_1 = _stmt.getText(_columnIndexOfEstado)
          _tmpEstado = __conversoresBaseDatos.textoAEstado(_tmp_1)
          val _tmpFechaProgramada: Long
          _tmpFechaProgramada = _stmt.getLong(_columnIndexOfFechaProgramada)
          val _tmpActualizadoEn: Long
          _tmpActualizadoEn = _stmt.getLong(_columnIndexOfActualizadoEn)
          val _tmpEstadoSincronizacion: EstadoSincronizacion
          val _tmp_2: String
          _tmp_2 = _stmt.getText(_columnIndexOfEstadoSincronizacion)
          _tmpEstadoSincronizacion = __conversoresBaseDatos.textoAEstadoSincronizacion(_tmp_2)
          val _tmpEliminado: Boolean
          val _tmp_3: Int
          _tmp_3 = _stmt.getLong(_columnIndexOfEliminado).toInt()
          _tmpEliminado = _tmp_3 != 0
          _result = EntidadRecordatorio(_tmpIdentificador,_tmpIdentificadorRemoto,_tmpTitulo,_tmpDescripcion,_tmpCategoriaIdentificador,_tmpPrioridad,_tmpEstado,_tmpFechaProgramada,_tmpActualizadoEn,_tmpEstadoSincronizacion,_tmpEliminado)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun obtenerPorIdentificadorRemoto(identificadorRemoto: String): EntidadRecordatorio? {
    val _sql: String = "SELECT * FROM recordatorios WHERE identificadorRemoto = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, identificadorRemoto)
        val _columnIndexOfIdentificador: Int = getColumnIndexOrThrow(_stmt, "identificador")
        val _columnIndexOfIdentificadorRemoto: Int = getColumnIndexOrThrow(_stmt, "identificadorRemoto")
        val _columnIndexOfTitulo: Int = getColumnIndexOrThrow(_stmt, "titulo")
        val _columnIndexOfDescripcion: Int = getColumnIndexOrThrow(_stmt, "descripcion")
        val _columnIndexOfCategoriaIdentificador: Int = getColumnIndexOrThrow(_stmt, "categoriaIdentificador")
        val _columnIndexOfPrioridad: Int = getColumnIndexOrThrow(_stmt, "prioridad")
        val _columnIndexOfEstado: Int = getColumnIndexOrThrow(_stmt, "estado")
        val _columnIndexOfFechaProgramada: Int = getColumnIndexOrThrow(_stmt, "fechaProgramada")
        val _columnIndexOfActualizadoEn: Int = getColumnIndexOrThrow(_stmt, "actualizadoEn")
        val _columnIndexOfEstadoSincronizacion: Int = getColumnIndexOrThrow(_stmt, "estadoSincronizacion")
        val _columnIndexOfEliminado: Int = getColumnIndexOrThrow(_stmt, "eliminado")
        val _result: EntidadRecordatorio?
        if (_stmt.step()) {
          val _tmpIdentificador: String
          _tmpIdentificador = _stmt.getText(_columnIndexOfIdentificador)
          val _tmpIdentificadorRemoto: String?
          if (_stmt.isNull(_columnIndexOfIdentificadorRemoto)) {
            _tmpIdentificadorRemoto = null
          } else {
            _tmpIdentificadorRemoto = _stmt.getText(_columnIndexOfIdentificadorRemoto)
          }
          val _tmpTitulo: String
          _tmpTitulo = _stmt.getText(_columnIndexOfTitulo)
          val _tmpDescripcion: String
          _tmpDescripcion = _stmt.getText(_columnIndexOfDescripcion)
          val _tmpCategoriaIdentificador: String
          _tmpCategoriaIdentificador = _stmt.getText(_columnIndexOfCategoriaIdentificador)
          val _tmpPrioridad: Prioridad
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfPrioridad)
          _tmpPrioridad = __conversoresBaseDatos.textoAPrioridad(_tmp)
          val _tmpEstado: EstadoRecordatorio
          val _tmp_1: String
          _tmp_1 = _stmt.getText(_columnIndexOfEstado)
          _tmpEstado = __conversoresBaseDatos.textoAEstado(_tmp_1)
          val _tmpFechaProgramada: Long
          _tmpFechaProgramada = _stmt.getLong(_columnIndexOfFechaProgramada)
          val _tmpActualizadoEn: Long
          _tmpActualizadoEn = _stmt.getLong(_columnIndexOfActualizadoEn)
          val _tmpEstadoSincronizacion: EstadoSincronizacion
          val _tmp_2: String
          _tmp_2 = _stmt.getText(_columnIndexOfEstadoSincronizacion)
          _tmpEstadoSincronizacion = __conversoresBaseDatos.textoAEstadoSincronizacion(_tmp_2)
          val _tmpEliminado: Boolean
          val _tmp_3: Int
          _tmp_3 = _stmt.getLong(_columnIndexOfEliminado).toInt()
          _tmpEliminado = _tmp_3 != 0
          _result = EntidadRecordatorio(_tmpIdentificador,_tmpIdentificadorRemoto,_tmpTitulo,_tmpDescripcion,_tmpCategoriaIdentificador,_tmpPrioridad,_tmpEstado,_tmpFechaProgramada,_tmpActualizadoEn,_tmpEstadoSincronizacion,_tmpEliminado)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observarPorPrioridad(prioridad: Prioridad): Flow<List<EntidadRecordatorio>> {
    val _sql: String = "SELECT * FROM recordatorios WHERE eliminado = 0 AND prioridad = ? ORDER BY fechaProgramada ASC"
    return createFlow(__db, false, arrayOf("recordatorios")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: String = __conversoresBaseDatos.prioridadATexto(prioridad)
        _stmt.bindText(_argIndex, _tmp)
        val _columnIndexOfIdentificador: Int = getColumnIndexOrThrow(_stmt, "identificador")
        val _columnIndexOfIdentificadorRemoto: Int = getColumnIndexOrThrow(_stmt, "identificadorRemoto")
        val _columnIndexOfTitulo: Int = getColumnIndexOrThrow(_stmt, "titulo")
        val _columnIndexOfDescripcion: Int = getColumnIndexOrThrow(_stmt, "descripcion")
        val _columnIndexOfCategoriaIdentificador: Int = getColumnIndexOrThrow(_stmt, "categoriaIdentificador")
        val _columnIndexOfPrioridad: Int = getColumnIndexOrThrow(_stmt, "prioridad")
        val _columnIndexOfEstado: Int = getColumnIndexOrThrow(_stmt, "estado")
        val _columnIndexOfFechaProgramada: Int = getColumnIndexOrThrow(_stmt, "fechaProgramada")
        val _columnIndexOfActualizadoEn: Int = getColumnIndexOrThrow(_stmt, "actualizadoEn")
        val _columnIndexOfEstadoSincronizacion: Int = getColumnIndexOrThrow(_stmt, "estadoSincronizacion")
        val _columnIndexOfEliminado: Int = getColumnIndexOrThrow(_stmt, "eliminado")
        val _result: MutableList<EntidadRecordatorio> = mutableListOf()
        while (_stmt.step()) {
          val _item: EntidadRecordatorio
          val _tmpIdentificador: String
          _tmpIdentificador = _stmt.getText(_columnIndexOfIdentificador)
          val _tmpIdentificadorRemoto: String?
          if (_stmt.isNull(_columnIndexOfIdentificadorRemoto)) {
            _tmpIdentificadorRemoto = null
          } else {
            _tmpIdentificadorRemoto = _stmt.getText(_columnIndexOfIdentificadorRemoto)
          }
          val _tmpTitulo: String
          _tmpTitulo = _stmt.getText(_columnIndexOfTitulo)
          val _tmpDescripcion: String
          _tmpDescripcion = _stmt.getText(_columnIndexOfDescripcion)
          val _tmpCategoriaIdentificador: String
          _tmpCategoriaIdentificador = _stmt.getText(_columnIndexOfCategoriaIdentificador)
          val _tmpPrioridad: Prioridad
          val _tmp_1: String
          _tmp_1 = _stmt.getText(_columnIndexOfPrioridad)
          _tmpPrioridad = __conversoresBaseDatos.textoAPrioridad(_tmp_1)
          val _tmpEstado: EstadoRecordatorio
          val _tmp_2: String
          _tmp_2 = _stmt.getText(_columnIndexOfEstado)
          _tmpEstado = __conversoresBaseDatos.textoAEstado(_tmp_2)
          val _tmpFechaProgramada: Long
          _tmpFechaProgramada = _stmt.getLong(_columnIndexOfFechaProgramada)
          val _tmpActualizadoEn: Long
          _tmpActualizadoEn = _stmt.getLong(_columnIndexOfActualizadoEn)
          val _tmpEstadoSincronizacion: EstadoSincronizacion
          val _tmp_3: String
          _tmp_3 = _stmt.getText(_columnIndexOfEstadoSincronizacion)
          _tmpEstadoSincronizacion = __conversoresBaseDatos.textoAEstadoSincronizacion(_tmp_3)
          val _tmpEliminado: Boolean
          val _tmp_4: Int
          _tmp_4 = _stmt.getLong(_columnIndexOfEliminado).toInt()
          _tmpEliminado = _tmp_4 != 0
          _item = EntidadRecordatorio(_tmpIdentificador,_tmpIdentificadorRemoto,_tmpTitulo,_tmpDescripcion,_tmpCategoriaIdentificador,_tmpPrioridad,_tmpEstado,_tmpFechaProgramada,_tmpActualizadoEn,_tmpEstadoSincronizacion,_tmpEliminado)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun obtenerPendientesDeSincronizacion(): List<EntidadRecordatorio> {
    val _sql: String = "SELECT * FROM recordatorios WHERE estadoSincronizacion != 'SINCRONIZADO' ORDER BY actualizadoEn ASC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfIdentificador: Int = getColumnIndexOrThrow(_stmt, "identificador")
        val _columnIndexOfIdentificadorRemoto: Int = getColumnIndexOrThrow(_stmt, "identificadorRemoto")
        val _columnIndexOfTitulo: Int = getColumnIndexOrThrow(_stmt, "titulo")
        val _columnIndexOfDescripcion: Int = getColumnIndexOrThrow(_stmt, "descripcion")
        val _columnIndexOfCategoriaIdentificador: Int = getColumnIndexOrThrow(_stmt, "categoriaIdentificador")
        val _columnIndexOfPrioridad: Int = getColumnIndexOrThrow(_stmt, "prioridad")
        val _columnIndexOfEstado: Int = getColumnIndexOrThrow(_stmt, "estado")
        val _columnIndexOfFechaProgramada: Int = getColumnIndexOrThrow(_stmt, "fechaProgramada")
        val _columnIndexOfActualizadoEn: Int = getColumnIndexOrThrow(_stmt, "actualizadoEn")
        val _columnIndexOfEstadoSincronizacion: Int = getColumnIndexOrThrow(_stmt, "estadoSincronizacion")
        val _columnIndexOfEliminado: Int = getColumnIndexOrThrow(_stmt, "eliminado")
        val _result: MutableList<EntidadRecordatorio> = mutableListOf()
        while (_stmt.step()) {
          val _item: EntidadRecordatorio
          val _tmpIdentificador: String
          _tmpIdentificador = _stmt.getText(_columnIndexOfIdentificador)
          val _tmpIdentificadorRemoto: String?
          if (_stmt.isNull(_columnIndexOfIdentificadorRemoto)) {
            _tmpIdentificadorRemoto = null
          } else {
            _tmpIdentificadorRemoto = _stmt.getText(_columnIndexOfIdentificadorRemoto)
          }
          val _tmpTitulo: String
          _tmpTitulo = _stmt.getText(_columnIndexOfTitulo)
          val _tmpDescripcion: String
          _tmpDescripcion = _stmt.getText(_columnIndexOfDescripcion)
          val _tmpCategoriaIdentificador: String
          _tmpCategoriaIdentificador = _stmt.getText(_columnIndexOfCategoriaIdentificador)
          val _tmpPrioridad: Prioridad
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfPrioridad)
          _tmpPrioridad = __conversoresBaseDatos.textoAPrioridad(_tmp)
          val _tmpEstado: EstadoRecordatorio
          val _tmp_1: String
          _tmp_1 = _stmt.getText(_columnIndexOfEstado)
          _tmpEstado = __conversoresBaseDatos.textoAEstado(_tmp_1)
          val _tmpFechaProgramada: Long
          _tmpFechaProgramada = _stmt.getLong(_columnIndexOfFechaProgramada)
          val _tmpActualizadoEn: Long
          _tmpActualizadoEn = _stmt.getLong(_columnIndexOfActualizadoEn)
          val _tmpEstadoSincronizacion: EstadoSincronizacion
          val _tmp_2: String
          _tmp_2 = _stmt.getText(_columnIndexOfEstadoSincronizacion)
          _tmpEstadoSincronizacion = __conversoresBaseDatos.textoAEstadoSincronizacion(_tmp_2)
          val _tmpEliminado: Boolean
          val _tmp_3: Int
          _tmp_3 = _stmt.getLong(_columnIndexOfEliminado).toInt()
          _tmpEliminado = _tmp_3 != 0
          _item = EntidadRecordatorio(_tmpIdentificador,_tmpIdentificadorRemoto,_tmpTitulo,_tmpDescripcion,_tmpCategoriaIdentificador,_tmpPrioridad,_tmpEstado,_tmpFechaProgramada,_tmpActualizadoEn,_tmpEstadoSincronizacion,_tmpEliminado)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun actualizarEstado(
    identificador: String,
    estado: EstadoRecordatorio,
    actualizadoEn: Long,
    estadoSincronizacion: EstadoSincronizacion,
  ) {
    val _sql: String = "UPDATE recordatorios SET estado = ?, actualizadoEn = ?, estadoSincronizacion = ? WHERE identificador = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: String = __conversoresBaseDatos.estadoATexto(estado)
        _stmt.bindText(_argIndex, _tmp)
        _argIndex = 2
        _stmt.bindLong(_argIndex, actualizadoEn)
        _argIndex = 3
        val _tmp_1: String = __conversoresBaseDatos.estadoSincronizacionATexto(estadoSincronizacion)
        _stmt.bindText(_argIndex, _tmp_1)
        _argIndex = 4
        _stmt.bindText(_argIndex, identificador)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun marcarEliminado(
    identificador: String,
    actualizadoEn: Long,
    estadoSincronizacion: EstadoSincronizacion,
  ) {
    val _sql: String = "UPDATE recordatorios SET eliminado = 1, actualizadoEn = ?, estadoSincronizacion = ? WHERE identificador = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, actualizadoEn)
        _argIndex = 2
        val _tmp: String = __conversoresBaseDatos.estadoSincronizacionATexto(estadoSincronizacion)
        _stmt.bindText(_argIndex, _tmp)
        _argIndex = 3
        _stmt.bindText(_argIndex, identificador)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun confirmarSincronizacion(
    identificador: String,
    identificadorRemoto: String?,
    estadoSincronizacion: EstadoSincronizacion,
  ) {
    val _sql: String = "UPDATE recordatorios SET identificadorRemoto = ?, estadoSincronizacion = ? WHERE identificador = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        if (identificadorRemoto == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, identificadorRemoto)
        }
        _argIndex = 2
        val _tmp: String = __conversoresBaseDatos.estadoSincronizacionATexto(estadoSincronizacion)
        _stmt.bindText(_argIndex, _tmp)
        _argIndex = 3
        _stmt.bindText(_argIndex, identificador)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun actualizarEstadoSincronizacion(identificador: String, estadoSincronizacion: EstadoSincronizacion) {
    val _sql: String = "UPDATE recordatorios SET estadoSincronizacion = ? WHERE identificador = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: String = __conversoresBaseDatos.estadoSincronizacionATexto(estadoSincronizacion)
        _stmt.bindText(_argIndex, _tmp)
        _argIndex = 2
        _stmt.bindText(_argIndex, identificador)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun eliminarDefinitivamente(identificador: String) {
    val _sql: String = "DELETE FROM recordatorios WHERE identificador = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, identificador)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
