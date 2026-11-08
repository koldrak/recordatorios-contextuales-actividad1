package com.daille.recordatorios.interfaz

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.daille.recordatorios.R
import com.daille.recordatorios.datos.Categoria
import com.daille.recordatorios.datos.EstadoRecordatorio
import com.daille.recordatorios.datos.FuenteRecordatoriosSimulados
import com.daille.recordatorios.datos.Prioridad
import com.daille.recordatorios.datos.Recordatorio
import com.daille.recordatorios.presentacion.ErrorGeneralFormulario
import com.daille.recordatorios.presentacion.ErrorListaRecordatorios
import com.daille.recordatorios.presentacion.ErrorValidacionFormulario
import com.daille.recordatorios.presentacion.EstadoDetalleRecordatorio
import com.daille.recordatorios.presentacion.EstadoFormularioRecordatorio
import com.daille.recordatorios.presentacion.EstadoListaRecordatorios
import com.daille.recordatorios.presentacion.ModeloVistaDetalleRecordatorio
import com.daille.recordatorios.presentacion.ModeloVistaFormularioRecordatorio
import com.daille.recordatorios.presentacion.ModeloVistaListaRecordatorios

/** Asocia las categorías con colores consistentes para facilitar su reconocimiento en toda la aplicación. */
private val coloresPorCategoria: Map<String, Color> = mapOf(
    "estudio" to Color(0xFF6750A4),
    "trabajo" to Color(0xFF006C4C),
    "personal" to Color(0xFFB3261E)
)

/** Presenta el listado y delega las acciones al ViewModel mediante callbacks claros. */
@Composable
fun PantallaListaRecordatorios(
    onCrearRecordatorio: () -> Unit,
    onRecordatorioClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    modeloVista: ModeloVistaListaRecordatorios = viewModel()
) {
    // Observa el StateFlow respetando el ciclo de vida de la pantalla.
    val estado = modeloVista.estado.collectAsStateWithLifecycle().value

    Scaffold(
        modifier = modifier,
        topBar = {
            BarraSuperiorListado(
                onSimularError = modeloVista::simularEstadoError,
                onRecargar = modeloVista::cargarRecordatorios
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCrearRecordatorio) {
                Text(text = "+")
            }
        }
    ) { rellenoInterno ->
        when (estado) {
            EstadoListaRecordatorios.Cargando -> ContenidoEstado(
                mensaje = stringResource(R.string.cargando_recordatorios),
                rellenoInterno = rellenoInterno
            )

            EstadoListaRecordatorios.Vacio -> ContenidoVacio(
                rellenoInterno = rellenoInterno,
                onCrearRecordatorio = onCrearRecordatorio
            )

            is EstadoListaRecordatorios.ConDatos -> ListaRecordatorios(
                recordatorios = estado.recordatorios,
                rellenoInterno = rellenoInterno,
                onRecordatorioClick = onRecordatorioClick,
                onEliminarRecordatorio = modeloVista::eliminarRecordatorio
            )

            is EstadoListaRecordatorios.Error -> ContenidoError(
                motivo = estado.motivo,
                rellenoInterno = rellenoInterno,
                onReintentar = modeloVista::cargarRecordatorios
            )
        }
    }
}

/** Muestra en un menú las acciones de simulación necesarias para comprobar los estados requeridos por la actividad. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BarraSuperiorListado(
    onSimularError: () -> Unit,
    onRecargar: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Conserva si el menú contextual de estados se encuentra abierto o cerrado.
    var menuEstadosVisible by remember { mutableStateOf(false) }
    // Obtiene el texto localizado antes del bloque semántico, que no es componible.
    val descripcionMenuEstados = stringResource(R.string.descripcion_menu_estados)

    TopAppBar(
        modifier = modifier,
        title = { Text(text = stringResource(R.string.titulo_pantalla)) },
        actions = {
            // Agrupa las acciones auxiliares para mantener una barra superior limpia.
            IconButton(onClick = { menuEstadosVisible = true }) {
                Text(
                    text = "⋮",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics {
                        contentDescription = descripcionMenuEstados
                    }
                )
            }
            DropdownMenu(
                expanded = menuEstadosVisible,
                onDismissRequest = { menuEstadosVisible = false }
            ) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.accion_error)) },
                    onClick = {
                        menuEstadosVisible = false
                        onSimularError()
                    }
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.accion_recargar)) },
                    onClick = {
                        menuEstadosVisible = false
                        onRecargar()
                    }
                )
            }
        }
    )
}

/** Representa la lista desplazable de recordatorios disponibles. */
@Composable
private fun ListaRecordatorios(
    recordatorios: List<Recordatorio>,
    rellenoInterno: PaddingValues,
    onRecordatorioClick: (Int) -> Unit,
    onEliminarRecordatorio: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(rellenoInterno),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.subtitulo_pantalla),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(items = recordatorios, key = { recordatorio -> recordatorio.identificador }) { recordatorio ->
            TarjetaRecordatorio(
                recordatorio = recordatorio,
                onClick = { onRecordatorioClick(recordatorio.identificador) },
                onEliminar = { onEliminarRecordatorio(recordatorio.identificador) }
            )
        }
    }
}

/** Representa un aviso mientras se carga contenido o no existe una pantalla especializada. */
@Composable
private fun ContenidoEstado(mensaje: String, rellenoInterno: PaddingValues, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(rellenoInterno)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = mensaje, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Comunica que todavía no hay datos y ofrece una acción para iniciar su creación. */
@Composable
private fun ContenidoVacio(
    rellenoInterno: PaddingValues,
    onCrearRecordatorio: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(rellenoInterno)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = stringResource(R.string.lista_vacia), style = MaterialTheme.typography.titleMedium)
        Button(onClick = onCrearRecordatorio, modifier = Modifier.padding(top = 16.dp)) {
            Text(text = stringResource(R.string.agregar_recordatorio))
        }
    }
}

/** Presenta un error visible, con texto y una acción de reintento accesible. */
@Composable
private fun ContenidoError(
    motivo: ErrorListaRecordatorios,
    rellenoInterno: PaddingValues,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(rellenoInterno)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = textoErrorLista(motivo), style = MaterialTheme.typography.titleMedium)
        Button(onClick = onReintentar, modifier = Modifier.padding(top = 16.dp)) {
            Text(text = stringResource(R.string.reintentar))
        }
    }
}

/** Dibuja una tarjeta reutilizable y comunica la selección o eliminación mediante lambdas. */
@Composable
fun TarjetaRecordatorio(
    recordatorio: Recordatorio,
    onClick: () -> Unit,
    onEliminar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorCategoria = coloresPorCategoria[recordatorio.categoria.identificador] ?: MaterialTheme.colorScheme.primary
    val descripcionEliminar = stringResource(R.string.descripcion_eliminar_recordatorio)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Separa la acción destructiva para impedir que abra el detalle al tocar el ícono.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = textoTitulo(recordatorio),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onEliminar) {
                    Text(
                        text = "🗑",
                        modifier = Modifier.semantics { contentDescription = descripcionEliminar }
                    )
                }
            }
            FilaEtiquetas(recordatorio = recordatorio, colorCategoria = colorCategoria)
        }
    }
}

/** Agrupa las etiquetas visuales de categoría, prioridad y estado. */
@Composable
private fun FilaEtiquetas(recordatorio: Recordatorio, colorCategoria: Color, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Etiqueta(texto = stringResource(recordatorio.categoria.recursoNombre), color = colorCategoria)
        Etiqueta(texto = stringResource(recordatorio.prioridad.recursoNombre), color = colorPrioridad(recordatorio.prioridad))
        Etiqueta(texto = stringResource(recordatorio.estado.recursoNombre), color = colorEstado(recordatorio.estado))
    }
}

/** Muestra una etiqueta de texto cuyo significado no depende solamente de su color. */
@Composable
private fun Etiqueta(texto: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = color.copy(alpha = 0.16f), shape = MaterialTheme.shapes.small) {
        Text(
            text = texto,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/** Muestra el formulario controlado y comunica guardar, cancelar y cambio de campos al ViewModel. */
@Composable
fun PantallaCrearRecordatorio(
    onGuardar: () -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier,
    modeloVista: ModeloVistaFormularioRecordatorio = viewModel()
) {
    val estado = modeloVista.estado.collectAsStateWithLifecycle().value
    LaunchedEffect(estado.guardadoConExito) {
        if (estado.guardadoConExito) {
            modeloVista.consumirGuardadoExitoso()
            onGuardar()
        }
    }
    Scaffold(modifier = modifier, topBar = { BarraSuperiorSimple(R.string.crear_recordatorio, onCancelar) }) { rellenoInterno ->
        FormularioRecordatorio(
            estado = estado,
            rellenoInterno = rellenoInterno,
            onTituloChange = modeloVista::cambiarTitulo,
            onDescripcionChange = modeloVista::cambiarDescripcion,
            onCategoriaChange = modeloVista::seleccionarCategoria,
            onPrioridadChange = modeloVista::seleccionarPrioridad,
            onGuardar = modeloVista::guardarRecordatorio,
            onSimularErrorGuardado = modeloVista::simularErrorDeGuardado,
            onCancelar = onCancelar
        )
    }
}

/** Construye los campos validados, las selecciones y las acciones del formulario. */
@Composable
private fun FormularioRecordatorio(
    estado: EstadoFormularioRecordatorio,
    rellenoInterno: PaddingValues,
    onTituloChange: (String) -> Unit,
    onDescripcionChange: (String) -> Unit,
    onCategoriaChange: (String) -> Unit,
    onPrioridadChange: (Prioridad) -> Unit,
    onGuardar: () -> Unit,
    onSimularErrorGuardado: () -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(rellenoInterno),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            CampoTextoValidado(
                etiqueta = stringResource(R.string.etiqueta_titulo),
                valor = estado.titulo,
                errorValidacion = estado.errorTitulo,
                onValorChange = onTituloChange
            )
        }
        item {
            CampoTextoValidado(
                etiqueta = stringResource(R.string.etiqueta_descripcion),
                valor = estado.descripcion,
                errorValidacion = estado.errorDescripcion,
                onValorChange = onDescripcionChange,
                lineasMinimas = 3
            )
        }
        item {
            Text(text = stringResource(R.string.etiqueta_categoria), style = MaterialTheme.typography.titleSmall)
            SelectorCategoria(estado.categoria?.identificador, onCategoriaChange, estado.errorCategoria)
        }
        item {
            Text(text = stringResource(R.string.etiqueta_prioridad), style = MaterialTheme.typography.titleSmall)
            SelectorPrioridad(estado.prioridad, onPrioridadChange, estado.errorPrioridad)
        }
        estado.errorGeneral?.let { errorGeneral ->
            item { Text(text = textoErrorGeneral(errorGeneral), color = MaterialTheme.colorScheme.error) }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onCancelar, modifier = Modifier.weight(1f), enabled = !estado.enviando) {
                    Text(text = stringResource(R.string.cancelar))
                }
                Button(
                    onClick = onGuardar,
                    modifier = Modifier.weight(1f),
                    enabled = estado.esValido && !estado.enviando
                ) {
                    Text(text = stringResource(if (estado.enviando) R.string.guardando else R.string.guardar))
                }
            }
        }
        item {
            TextButton(
                onClick = onSimularErrorGuardado,
                enabled = estado.esValido && !estado.enviando,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.simular_error_guardado))
            }
        }
    }
}

/** Representa un campo de texto controlado con error visible y semántico cuando la validación falla. */
@Composable
private fun CampoTextoValidado(
    etiqueta: String,
    valor: String,
    errorValidacion: ErrorValidacionFormulario?,
    onValorChange: (String) -> Unit,
    lineasMinimas: Int = 1,
    modifier: Modifier = Modifier
) {
    val textoError = errorValidacion?.let { textoErrorValidacion(it) }
    OutlinedTextField(
        value = valor,
        onValueChange = onValorChange,
        modifier = modifier
            .fillMaxWidth()
            .semantics { textoError?.let { mensaje -> error(mensaje) } },
        label = { Text(text = etiqueta) },
        supportingText = { textoError?.let { mensaje -> Text(text = mensaje) } },
        isError = textoError != null,
        minLines = lineasMinimas
    )
}

/** Permite seleccionar una categoría válida mediante componentes controlados. */
@Composable
private fun SelectorCategoria(
    categoriaSeleccionada: String?,
    onCategoriaChange: (String) -> Unit,
    errorValidacion: ErrorValidacionFormulario?,
    modifier: Modifier = Modifier
) {
    // Resuelve el texto antes de configurar la semántica, porque la lambda semántica no es componible.
    val textoError = errorValidacion?.let { errorActual -> textoErrorValidacion(errorActual) }
    Column(modifier = modifier.semantics { textoError?.let { mensaje -> error(mensaje) } }) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FuenteRecordatoriosSimulados.categoriasDisponibles.forEach { categoria ->
                FilterChip(
                    selected = categoria.identificador == categoriaSeleccionada,
                    onClick = { onCategoriaChange(categoria.identificador) },
                    label = { Text(text = stringResource(categoria.recursoNombre)) }
                )
            }
        }
        textoError?.let { mensaje -> Text(text = mensaje, color = MaterialTheme.colorScheme.error) }
    }
}

/** Permite seleccionar una prioridad y comunica el valor seleccionado mediante una lambda. */
@Composable
private fun SelectorPrioridad(
    prioridadSeleccionada: Prioridad?,
    onPrioridadChange: (Prioridad) -> Unit,
    errorValidacion: ErrorValidacionFormulario?,
    modifier: Modifier = Modifier
) {
    // Resuelve el texto antes de configurar la semántica, porque la lambda semántica no es componible.
    val textoError = errorValidacion?.let { errorActual -> textoErrorValidacion(errorActual) }
    Column(modifier = modifier.semantics { textoError?.let { mensaje -> error(mensaje) } }) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Prioridad.entries.forEach { prioridad ->
                FilterChip(
                    selected = prioridad == prioridadSeleccionada,
                    onClick = { onPrioridadChange(prioridad) },
                    label = { Text(text = stringResource(prioridad.recursoNombre)) }
                )
            }
        }
        textoError?.let { mensaje -> Text(text = mensaje, color = MaterialTheme.colorScheme.error) }
    }
}

/** Muestra la información completa del recordatorio seleccionado mediante su identificador. */
@Composable
fun PantallaDetalleRecordatorio(
    identificador: Int,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    modeloVista: ModeloVistaDetalleRecordatorio = viewModel()
) {
    val estado = modeloVista.estado.collectAsStateWithLifecycle().value
    LaunchedEffect(identificador) { modeloVista.cargarDetalle(identificador) }
    Scaffold(modifier = modifier, topBar = { BarraSuperiorSimple(R.string.detalle_recordatorio, onVolver) }) { rellenoInterno ->
        when (estado) {
            EstadoDetalleRecordatorio.Cargando -> ContenidoEstado(
                mensaje = stringResource(R.string.cargando_recordatorios),
                rellenoInterno = rellenoInterno
            )
            EstadoDetalleRecordatorio.NoEncontrado -> ContenidoEstado(
                mensaje = stringResource(R.string.recordatorio_no_encontrado),
                rellenoInterno = rellenoInterno
            )
            is EstadoDetalleRecordatorio.Disponible -> ContenidoDetalle(
                recordatorio = estado.recordatorio,
                rellenoInterno = rellenoInterno
            )
        }
    }
}

/** Organiza los datos completos de un registro seleccionado en una vista legible. */
@Composable
private fun ContenidoDetalle(recordatorio: Recordatorio, rellenoInterno: PaddingValues, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(rellenoInterno)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = textoTitulo(recordatorio), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(text = textoDescripcion(recordatorio), style = MaterialTheme.typography.bodyLarge)
        FilaEtiquetas(
            recordatorio = recordatorio,
            colorCategoria = coloresPorCategoria[recordatorio.categoria.identificador] ?: MaterialTheme.colorScheme.primary
        )
        recordatorio.aviso?.let { aviso ->
            Text(
                text = stringResource(R.string.detalle_aviso, stringResource(aviso.recursoMensaje)),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/** Muestra una barra superior reutilizable que ofrece regresar al destino anterior. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BarraSuperiorSimple(titulo: Int, onVolver: () -> Unit, modifier: Modifier = Modifier) {
    TopAppBar(
        modifier = modifier,
        title = { Text(text = stringResource(titulo)) },
        navigationIcon = {
            TextButton(onClick = onVolver) { Text(text = stringResource(R.string.volver)) }
        }
    )
}

/** Obtiene el título localizado o el texto escrito durante la creación del recordatorio. */
@Composable
private fun textoTitulo(recordatorio: Recordatorio): String =
    recordatorio.recursoTitulo?.let { recurso -> stringResource(recurso) } ?: recordatorio.tituloPersonalizado.orEmpty()

/** Obtiene la descripción localizada o el texto escrito durante la creación del recordatorio. */
@Composable
private fun textoDescripcion(recordatorio: Recordatorio): String =
    recordatorio.recursoDescripcion?.let { recurso -> stringResource(recurso) }
        ?: recordatorio.descripcionPersonalizada.orEmpty()

/** Traduce cada error de validación sin colocar textos dependientes del idioma dentro del ViewModel. */
@Composable
private fun textoErrorValidacion(error: ErrorValidacionFormulario): String = when (error) {
    ErrorValidacionFormulario.TITULO_CORTO -> stringResource(R.string.error_titulo_corto)
    ErrorValidacionFormulario.DESCRIPCION_LARGA -> stringResource(R.string.error_descripcion_larga)
    ErrorValidacionFormulario.CATEGORIA_INVALIDA -> stringResource(R.string.error_categoria)
    ErrorValidacionFormulario.PRIORIDAD_NO_SELECCIONADA -> stringResource(R.string.error_prioridad)
}

/** Traduce el motivo de error del listado para la interfaz actual. */
@Composable
private fun textoErrorLista(error: ErrorListaRecordatorios): String = when (error) {
    ErrorListaRecordatorios.CARGA -> stringResource(R.string.error_carga)
    ErrorListaRecordatorios.SIMULADO -> stringResource(R.string.error_simulado)
}

/** Traduce el error general ocurrido durante el guardado. */
@Composable
private fun textoErrorGeneral(error: ErrorGeneralFormulario): String = when (error) {
    ErrorGeneralFormulario.GUARDADO -> stringResource(R.string.error_guardado)
}

/** Devuelve un color semántico para cada nivel de prioridad. */
private fun colorPrioridad(prioridad: Prioridad): Color = when (prioridad) {
    Prioridad.ALTA -> Color(0xFFB3261E)
    Prioridad.MEDIA -> Color(0xFF9A6700)
    Prioridad.BAJA -> Color(0xFF006E1C)
}

/** Devuelve un color semántico para cada estado permitido del recordatorio. */
private fun colorEstado(estado: EstadoRecordatorio): Color = when (estado) {
    EstadoRecordatorio.Pendiente -> Color(0xFF2457D6)
    EstadoRecordatorio.Completado -> Color(0xFF006E1C)
    EstadoRecordatorio.Vencido -> Color(0xFFB3261E)
}
