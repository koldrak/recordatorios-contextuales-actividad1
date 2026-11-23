package com.daille.recordatorios

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.daille.recordatorios.datos.ContenedorDependencias
import com.daille.recordatorios.interfaz.PantallaAutenticacion
import com.daille.recordatorios.interfaz.PantallaConfiguracion
import com.daille.recordatorios.interfaz.PantallaCrearRecordatorio
import com.daille.recordatorios.interfaz.PantallaDetalleRecordatorio
import com.daille.recordatorios.interfaz.PantallaListaRecordatorios
import com.daille.recordatorios.presentacion.ModeloVistaConfiguracion
import com.daille.recordatorios.presentacion.ModeloVistaAutenticacion
import com.daille.recordatorios.ui.theme.RecordatoriosTheme

/** Inicializa dependencias persistentes y muestra el grafo de navegación de la aplicación. */
class MainActivity : ComponentActivity() {
    /** Crea las instancias únicas de Room y DataStore antes de que Compose solicite los ViewModel. */
    override fun onCreate(estadoGuardado: Bundle?) {
        super.onCreate(estadoGuardado)
        ContenedorDependencias.inicializar(applicationContext)
        enableEdgeToEdge()
        setContent { AplicacionRecordatorios() }
    }
}

/** Centraliza las rutas para evitar cadenas repetidas y transportar solo identificadores entre destinos. */
private object RutasRecordatorios {
    const val listado = "listado"
    const val crear = "crear"
    const val configuracion = "configuracion"
    const val detalle = "detalle/{identificador}"

    /** Construye la ruta de detalle sin enviar el objeto completo mediante navegación. */
    fun detalle(identificador: String): String = "detalle/$identificador"
}

/** Conecta las pantallas y aplica el tema persistido en DataStore a toda la interfaz. */
@Composable
fun AplicacionRecordatorios(modifier: Modifier = Modifier) {
    // El modelo de configuración observa DataStore y mantiene el tema incluso al recrear la actividad.
    val modeloVistaConfiguracion: ModeloVistaConfiguracion = viewModel()
    val estadoConfiguracion = modeloVistaConfiguracion.estado.collectAsStateWithLifecycle().value
    // El modelo de autenticación decide qué contenido puede consultar la persona según la sesión Firebase vigente.
    val modeloVistaAutenticacion: ModeloVistaAutenticacion = viewModel()
    val estadoAutenticacion = modeloVistaAutenticacion.estado.collectAsStateWithLifecycle().value

    RecordatoriosTheme(darkTheme = estadoConfiguracion.preferencias.modoOscuro) {
        if (estadoAutenticacion.sesion == null) {
            // La pantalla de acceso se muestra al iniciar, cerrar o perder una sesión válida.
            PantallaAutenticacion(modifier = modifier)
        } else {
            NavegacionRecordatorios(
                onCerrarSesion = modeloVistaAutenticacion::cerrarSesion,
                modifier = modifier
            )
        }
    }
}

/** Navega por recordatorios únicamente cuando Firebase confirmó una identidad autenticada. */
@Composable
private fun NavegacionRecordatorios(onCerrarSesion: () -> Unit, modifier: Modifier = Modifier) {
    val controladorNavegacion = rememberNavController()
    NavHost(
        navController = controladorNavegacion,
        startDestination = RutasRecordatorios.listado,
        modifier = modifier
    ) {
        composable(RutasRecordatorios.listado) {
            PantallaListaRecordatorios(
                onCrearRecordatorio = { controladorNavegacion.navigate(RutasRecordatorios.crear) },
                onRecordatorioClick = { identificador ->
                    controladorNavegacion.navigate(RutasRecordatorios.detalle(identificador))
                },
                onAbrirConfiguracion = { controladorNavegacion.navigate(RutasRecordatorios.configuracion) },
                onCerrarSesion = onCerrarSesion
            )
        }
        composable(RutasRecordatorios.crear) {
            PantallaCrearRecordatorio(
                onGuardar = { controladorNavegacion.popBackStack() },
                onCancelar = { controladorNavegacion.popBackStack() }
            )
        }
        composable(RutasRecordatorios.configuracion) {
            PantallaConfiguracion(onVolver = { controladorNavegacion.popBackStack() })
        }
        composable(
            route = RutasRecordatorios.detalle,
            arguments = listOf(navArgument("identificador") { type = NavType.StringType })
        ) { entradaPila ->
            // Recupera únicamente el identificador requerido para que el detalle consulte Room mediante el repositorio.
            val identificador = entradaPila.arguments?.getString("identificador") ?: return@composable
            PantallaDetalleRecordatorio(
                identificador = identificador,
                onVolver = { controladorNavegacion.popBackStack() }
            )
        }
    }
}
