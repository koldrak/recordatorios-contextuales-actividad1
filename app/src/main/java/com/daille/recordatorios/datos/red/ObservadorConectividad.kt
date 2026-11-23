package com.daille.recordatorios.datos.red

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Observa redes validadas por Android y expone cambios de conectividad como un Flow reutilizable. */
class ObservadorConectividad(contexto: Context) {
    /** Obtiene el administrador desde el contexto de aplicación para no conservar una Activity. */
    private val administradorConectividad = contexto.applicationContext
        .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /** Emite si existe una red con Internet validado y libera el callback al cancelar la recolección. */
    fun observarConectividad(): Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            /** Revisa las capacidades de la red que Android acaba de poner a disposición. */
            override fun onCapabilitiesChanged(red: Network, capacidades: NetworkCapabilities) {
                trySend(tieneInternetValidado(capacidades))
            }

            /** Comunica que la red activa se perdió para que la interfaz conserve únicamente los datos locales. */
            override fun onLost(red: Network) {
                trySend(false)
            }
        }
        trySend(tieneInternetActual())
        administradorConectividad.registerDefaultNetworkCallback(callback)
        awaitClose { administradorConectividad.unregisterNetworkCallback(callback) }
    }

    /** Comprueba la red activa inicial antes de esperar cambios en el callback. */
    private fun tieneInternetActual(): Boolean {
        val red = administradorConectividad.activeNetwork ?: return false
        val capacidades = administradorConectividad.getNetworkCapabilities(red) ?: return false
        return tieneInternetValidado(capacidades)
    }

    /** Exige capacidad de Internet y validación del sistema para evitar sincronizar en redes sin salida real. */
    private fun tieneInternetValidado(capacidades: NetworkCapabilities): Boolean =
        capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
