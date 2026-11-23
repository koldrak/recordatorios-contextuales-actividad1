package com.daille.recordatorios.datos.autenticacion

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Representa la identidad mínima que necesita la aplicación sin exponer el objeto FirebaseUser a la interfaz. */
data class SesionUsuario(
    val identificador: String,
    val correo: String?
)

/** Resume el resultado de una operación de registro o inicio de sesión para el ViewModel. */
sealed interface ResultadoAutenticacion {
    /** Comunica que Firebase confirmó la identidad y devolvió una sesión válida. */
    data object Exito : ResultadoAutenticacion

    /** Comunica credenciales inválidas, una cuenta existente o datos de acceso no aceptados. */
    data object ErrorCredenciales : ResultadoAutenticacion

    /** Comunica que la app aún no tiene una configuración Firebase válida. */
    data object ErrorConfiguracion : ResultadoAutenticacion

    /** Comunica una falla no clasificada sin trasladar excepciones de Firebase a la presentación. */
    data object ErrorDesconocido : ResultadoAutenticacion
}

/** Define las operaciones de autenticación que puede utilizar la capa de presentación. */
interface RepositorioAutenticacion {
    /** Emite la sesión vigente, incluso al reabrir la app, o null cuando no existe una identidad válida. */
    fun observarSesion(): Flow<SesionUsuario?>

    /** Registra una cuenta mediante correo y contraseña sin persistir la contraseña en el dispositivo. */
    suspend fun registrar(correo: String, contrasena: String): ResultadoAutenticacion

    /** Inicia sesión mediante Firebase Authentication y devuelve un resultado independiente del proveedor. */
    suspend fun iniciarSesion(correo: String, contrasena: String): ResultadoAutenticacion

    /** Cierra la sesión y elimina los recordatorios cacheados de la cuenta anterior. */
    suspend fun cerrarSesion()
}

/** Adapta Firebase Authentication a la interfaz del repositorio y mantiene sus detalles fuera de Compose. */
class RepositorioAutenticacionFirebase(
    // La instancia se crea al usarla para que la app informe configuración faltante sin cerrarse inesperadamente.
    private val obtenerAutenticacion: () -> FirebaseAuth = { FirebaseAuth.getInstance() },
    // La caché Room se limpia para impedir que una segunda cuenta vea datos de la sesión anterior.
    private val limpiarDatosLocales: suspend () -> Unit
) : RepositorioAutenticacion {
    /** Observa los cambios de FirebaseAuth, incluidos cierre, restauración o pérdida de una sesión. */
    override fun observarSesion(): Flow<SesionUsuario?> = callbackFlow {
        val autenticacion = runCatching { obtenerAutenticacion() }.getOrElse {
            trySend(null)
            close()
            return@callbackFlow
        }
        val observadorSesion = FirebaseAuth.AuthStateListener { fuente ->
            val usuario = fuente.currentUser
            trySend(usuario?.let { SesionUsuario(it.uid, it.email) })
        }
        autenticacion.addAuthStateListener(observadorSesion)
        awaitClose { autenticacion.removeAuthStateListener(observadorSesion) }
    }

    /** Crea una cuenta y limpia cualquier caché previa antes de asociar los datos con la nueva identidad. */
    override suspend fun registrar(correo: String, contrasena: String): ResultadoAutenticacion {
        limpiarDatosLocales()
        return ejecutarOperacion { autenticacion ->
            autenticacion.createUserWithEmailAndPassword(correo, contrasena).await()
        }
    }

    /** Inicia una cuenta existente y evita que datos locales de otra persona queden visibles durante el cambio. */
    override suspend fun iniciarSesion(correo: String, contrasena: String): ResultadoAutenticacion {
        limpiarDatosLocales()
        return ejecutarOperacion { autenticacion ->
            autenticacion.signInWithEmailAndPassword(correo, contrasena).await()
        }
    }

    /** Borra la caché local y luego elimina la sesión que Firebase conserva de forma segura. */
    override suspend fun cerrarSesion() {
        limpiarDatosLocales()
        runCatching { obtenerAutenticacion().signOut() }
    }

    /** Traduce excepciones técnicas en resultados aptos para que el ViewModel produzca un estado visible. */
    private suspend fun ejecutarOperacion(
        operacion: suspend (FirebaseAuth) -> Unit
    ): ResultadoAutenticacion = try {
        operacion(obtenerAutenticacion())
        ResultadoAutenticacion.Exito
    } catch (_: FirebaseAuthException) {
        ResultadoAutenticacion.ErrorCredenciales
    } catch (_: IllegalStateException) {
        ResultadoAutenticacion.ErrorConfiguracion
    } catch (_: Exception) {
        ResultadoAutenticacion.ErrorDesconocido
    }
}
