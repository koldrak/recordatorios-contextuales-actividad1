package com.daille.recordatorios.datos

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Define las operaciones que la capa de presentación necesita para administrar recordatorios. */
interface RepositorioRecordatorios {
    /** Obtiene la lista actual de recordatorios desde la fuente disponible. */
    suspend fun obtenerRecordatorios(): List<Recordatorio>

    /** Busca un recordatorio mediante su identificador único. */
    suspend fun obtenerRecordatorio(identificador: Int): Recordatorio?

    /** Guarda un nuevo recordatorio en la fuente de datos. */
    suspend fun guardarRecordatorio(recordatorio: Recordatorio)

    /** Elimina el recordatorio identificado de la fuente de datos. */
    suspend fun eliminarRecordatorio(identificador: Int)

    /** Expone los cambios de la lista para que las pantallas se actualicen sin recrear la navegación. */
    fun observarRecordatorios(): StateFlow<List<Recordatorio>>
}

/** Implementa un repositorio temporal que conserva los recordatorios solo mientras la aplicación está abierta. */
class RepositorioRecordatoriosEnMemoria : RepositorioRecordatorios {
    /** Mantiene la lista privada que representa los datos disponibles en memoria. */
    private val recordatorios = kotlinx.coroutines.flow.MutableStateFlow<List<Recordatorio>>(emptyList())

    /** Simula una consulta asincrónica sin bloquear la interfaz. */
    override suspend fun obtenerRecordatorios(): List<Recordatorio> {
        delay(450)
        return recordatorios.value
    }

    /** Devuelve un registro específico o null cuando el identificador no existe. */
    override suspend fun obtenerRecordatorio(identificador: Int): Recordatorio? {
        delay(200)
        return recordatorios.value.find { recordatorio -> recordatorio.identificador == identificador }
    }

    /** Simula un guardado asincrónico y agrega el registro al final de la lista. */
    override suspend fun guardarRecordatorio(recordatorio: Recordatorio) {
        delay(550)
        recordatorios.update { listaActual -> listaActual + recordatorio }
    }

    /** Simula una eliminación asincrónica y conserva todos los registros excepto el indicado. */
    override suspend fun eliminarRecordatorio(identificador: Int) {
        delay(350)
        recordatorios.update { listaActual ->
            listaActual.filterNot { recordatorio -> recordatorio.identificador == identificador }
        }
    }

    /** Devuelve una versión de solo lectura de los cambios realizados en memoria. */
    override fun observarRecordatorios(): StateFlow<List<Recordatorio>> = recordatorios.asStateFlow()
}

/** Centraliza las dependencias temporales mientras no se incorpora inyección de dependencias formal. */
object ContenedorDependencias {
    /** Expone una única instancia en memoria compartida por los ViewModel de esta actividad. */
    val repositorioRecordatorios: RepositorioRecordatorios = RepositorioRecordatoriosEnMemoria()
}
