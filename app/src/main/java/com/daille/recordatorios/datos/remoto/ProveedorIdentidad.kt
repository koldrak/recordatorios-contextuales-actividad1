package com.daille.recordatorios.datos.remoto

import com.google.firebase.auth.FirebaseAuth

/** Expone únicamente el uid que el repositorio necesita, sin revelar Firebase a ViewModel o Compose. */
interface ProveedorIdentidad {
    /** Devuelve el uid de la sesión vigente o null cuando todavía no existe una identidad autenticada. */
    fun obtenerIdentificadorUsuarioActual(): String?
}

/** Lee la sesión de Firebase Authentication de forma diferida para no bloquear la apertura local sin configuración cloud. */
class ProveedorIdentidadFirebase(
    // La instancia se obtiene solo al consultar la sesión y no durante la creación del contenedor de dependencias.
    private val obtenerAutenticacion: () -> FirebaseAuth = { FirebaseAuth.getInstance() }
) : ProveedorIdentidad {
    /** Consulta el uid sin exponer la API de Firebase fuera de la capa de datos. */
    override fun obtenerIdentificadorUsuarioActual(): String? =
        runCatching { obtenerAutenticacion().currentUser?.uid }.getOrNull()
}
