package com.daille.recordatorios.datos.preferencias

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Crea una única instancia de Preferences DataStore para toda la aplicación. */
val Context.almacenPreferencias: DataStore<Preferences> by preferencesDataStore(name = "preferencias_recordatorios")

/** Define el criterio con el que el listado ordena los recordatorios almacenados en Room. */
enum class CriterioOrdenamiento {
    FECHA,
    PRIORIDAD
}

/** Reúne las preferencias simples que deben mantenerse después de cerrar la aplicación. */
data class PreferenciasAplicacion(
    val modoOscuro: Boolean = false,
    val mostrarCompletados: Boolean = true,
    val criterioOrdenamiento: CriterioOrdenamiento = CriterioOrdenamiento.FECHA
)

/** Centraliza nombres estables para evitar perder valores al modificar la implementación. */
private object ClavesPreferencias {
    /** Identifica la preferencia que controla el tema visual. */
    val modoOscuro = booleanPreferencesKey("modo_oscuro")

    /** Identifica la preferencia que decide si se muestran tareas completadas. */
    val mostrarCompletados = booleanPreferencesKey("mostrar_completados")

    /** Identifica la preferencia que conserva el orden elegido para el listado. */
    val criterioOrdenamiento = stringPreferencesKey("criterio_ordenamiento")
}

/** Traduce Preferences DataStore a un modelo propio y evita que la interfaz conozca las claves internas. */
class RepositorioPreferencias(
    private val almacen: DataStore<Preferences>
) {
    /** Emite preferencias actualizadas y utiliza valores seguros ante una lectura recuperable inválida. */
    val preferencias: Flow<PreferenciasAplicacion> = almacen.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { valores ->
            val criterio = valores[ClavesPreferencias.criterioOrdenamiento]
                ?.let { valor -> runCatching { CriterioOrdenamiento.valueOf(valor) }.getOrNull() }
                ?: CriterioOrdenamiento.FECHA
            PreferenciasAplicacion(
                modoOscuro = valores[ClavesPreferencias.modoOscuro] ?: false,
                mostrarCompletados = valores[ClavesPreferencias.mostrarCompletados] ?: true,
                criterioOrdenamiento = criterio
            )
        }

    /** Guarda el tema seleccionado mediante una edición suspendida y atómica. */
    suspend fun guardarModoOscuro(activado: Boolean) {
        almacen.edit { valores -> valores[ClavesPreferencias.modoOscuro] = activado }
    }

    /** Guarda la decisión de mostrar u ocultar recordatorios completados. */
    suspend fun guardarMostrarCompletados(mostrar: Boolean) {
        almacen.edit { valores -> valores[ClavesPreferencias.mostrarCompletados] = mostrar }
    }

    /** Guarda el nombre del criterio elegido para reconstruirlo de forma segura durante la lectura. */
    suspend fun guardarCriterioOrdenamiento(criterio: CriterioOrdenamiento) {
        almacen.edit { valores -> valores[ClavesPreferencias.criterioOrdenamiento] = criterio.name }
    }
}
