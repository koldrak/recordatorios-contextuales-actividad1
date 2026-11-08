package com.daille.recordatorios

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.daille.recordatorios.interfaz.PantallaCrearRecordatorio
import com.daille.recordatorios.interfaz.PantallaDetalleRecordatorio
import com.daille.recordatorios.interfaz.PantallaListaRecordatorios
import com.daille.recordatorios.ui.theme.RecordatoriosTheme

/** Es la actividad principal que inicializa la interfaz declarativa de la aplicación. */
class MainActivity : ComponentActivity() {
    /** Configura la ventana y muestra el grafo de navegación de los recordatorios. */
    override fun onCreate(estadoGuardado: Bundle?) {
        super.onCreate(estadoGuardado)
        enableEdgeToEdge()
        setContent {
            // Aplica el tema común antes de mostrar cualquiera de los destinos de la aplicación.
            RecordatoriosTheme {
                AplicacionRecordatorios()
            }
        }
    }
}

/** Centraliza las rutas para evitar cadenas repetidas y conservar identificadores claros en el grafo. */
private object RutasRecordatorios {
    const val listado = "listado"
    const val crear = "crear"
    const val detalle = "detalle/{identificador}"

    /** Construye la ruta de detalle transportando únicamente el identificador requerido. */
    fun detalle(identificador: Int): String = "detalle/$identificador"
}

/** Conecta listado, creación y detalle sin entregar el NavController a componentes reutilizables. */
@Composable
fun AplicacionRecordatorios(modifier: Modifier = Modifier) {
    // El NavController se administra en el nivel superior, responsable de conectar destinos.
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
                }
            )
        }
        composable(RutasRecordatorios.crear) {
            PantallaCrearRecordatorio(
                onGuardar = {
                    // Vuelve al listado existente; este observa el repositorio y recibe el registro guardado.
                    controladorNavegacion.popBackStack()
                },
                onCancelar = { controladorNavegacion.popBackStack() }
            )
        }
        composable(
            route = RutasRecordatorios.detalle,
            arguments = listOf(navArgument("identificador") { type = NavType.IntType })
        ) { entradaPila ->
            // Obtiene solo el identificador desde la ruta; el detalle recupera el objeto desde el repositorio.
            val identificador = entradaPila.arguments?.getInt("identificador") ?: return@composable
            PantallaDetalleRecordatorio(
                identificador = identificador,
                onVolver = { controladorNavegacion.popBackStack() }
            )
        }
    }
}
