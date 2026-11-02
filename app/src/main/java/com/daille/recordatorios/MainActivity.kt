package com.daille.recordatorios

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.daille.recordatorios.datos.Categoria
import com.daille.recordatorios.datos.EstadoRecordatorio
import com.daille.recordatorios.datos.FuenteRecordatoriosSimulados
import com.daille.recordatorios.datos.Prioridad
import com.daille.recordatorios.datos.Recordatorio
import com.daille.recordatorios.ui.theme.RecordatoriosTheme

class MainActivity : ComponentActivity() {
    /** Prepara una interfaz Compose que puede ocupar toda la pantalla del dispositivo. */
    override fun onCreate(estadoGuardado: Bundle?) {
        super.onCreate(estadoGuardado)
        enableEdgeToEdge()
        setContent {
            // Aplica el tema Material 3
            RecordatoriosTheme {
                AplicacionRecordatorios()
            }
        }
    }
}

/** Asocia cada categoría con un color para su identificación visual usando un Map. */
private val coloresPorCategoria: Map<Categoria, Color> = mapOf(
    Categoria("estudio", R.string.categoria_estudio) to Color(0xFF6750A4),
    Categoria("trabajo", R.string.categoria_trabajo) to Color(0xFF006C4C),
    Categoria("personal", R.string.categoria_personal) to Color(0xFFB3261E)
)

/** Conecta los recordatorios simulados con la pantalla principal de la aplicación. */
@Composable
fun AplicacionRecordatorios() {
    // Lee la lista simulada que posteriormente podrá provenir de una base de datos.
    val recordatorios = FuenteRecordatoriosSimulados.recordatorios

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = { BotonAgregarRecordatorio() }
    ) { rellenoInterno ->
        PantallaRecordatorios(recordatorios = recordatorios, rellenoInterno = rellenoInterno)
    }
}

/** Muestra un botón fijo en la parte inferior como preparación para el futuro formulario. */
@Composable
private fun BotonAgregarRecordatorio() {
    // Separa el botón de la lista y de la barra de gestos del sistema Android.
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 4.dp,
        shadowElevation = 6.dp
    ) {
        Button(
            onClick = {
                // La acción se implementará cuando se agregue el formulario de creación.
            },
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(text = stringResource(R.string.agregar_recordatorio))
        }
    }
}

/** Construye el encabezado y la lista desplazable de recordatorios. */
@Composable
private fun PantallaRecordatorios(recordatorios: List<Recordatorio>, rellenoInterno: PaddingValues) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(rellenoInterno),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.titulo_pantalla),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.subtitulo_pantalla),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (recordatorios.isEmpty()) {
            item { Text(text = stringResource(R.string.sin_recordatorios)) }
        } else {
            items(items = recordatorios, key = { recordatorio -> recordatorio.identificador }) { recordatorio ->
                TarjetaRecordatorio(recordatorio = recordatorio)
            }
        }
    }
}

/** Presenta los atributos requeridos de un recordatorio mediante una tarjeta de Material 3. */
@Composable
private fun TarjetaRecordatorio(recordatorio: Recordatorio) {
    // Usa el color primario si en el futuro aparece una categoría que no está en el Map.
    val colorCategoria = coloresPorCategoria[recordatorio.categoria] ?: MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(recordatorio.recursoTitulo),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Etiqueta(texto = stringResource(recordatorio.categoria.recursoNombre), color = colorCategoria)
                Etiqueta(texto = stringResource(recordatorio.prioridad.recursoNombre), color = colorPrioridad(recordatorio.prioridad))
                Etiqueta(texto = stringResource(recordatorio.estado.recursoNombre), color = colorEstado(recordatorio.estado))
            }
        }
    }
}

/** Dibuja una etiqueta breve para destacar una categoría, una prioridad o un estado. */
@Composable
private fun Etiqueta(texto: String, color: Color) {
    Surface(color = color.copy(alpha = 0.16f), shape = MaterialTheme.shapes.small) {
        Text(
            text = texto,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/** Devuelve un color semántico para cada nivel de prioridad. */
private fun colorPrioridad(prioridad: Prioridad): Color = when (prioridad) {
    Prioridad.ALTA -> Color(0xFFB3261E)
    Prioridad.MEDIA -> Color(0xFF9A6700)
    Prioridad.BAJA -> Color(0xFF006E1C)
}

/** Devuelve un color semántico para cada estado de la sealed class. */
private fun colorEstado(estado: EstadoRecordatorio): Color = when (estado) {
    EstadoRecordatorio.Pendiente -> Color(0xFF2457D6)
    EstadoRecordatorio.Completado -> Color(0xFF006E1C)
    EstadoRecordatorio.Vencido -> Color(0xFFB3261E)
}

/** Genera una vista previa de la interfaz directamente desde Android Studio. */
@Preview(showBackground = true)
@Composable
private fun VistaPreviaAplicacionRecordatorios() {
    RecordatoriosTheme { AplicacionRecordatorios() }
}
