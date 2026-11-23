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
import androidx.compose.material3.Switch
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
import com.daille.recordatorios.datos.CatalogoCategorias
import com.daille.recordatorios.datos.EstadoRecordatorio
import com.daille.recordatorios.datos.EstadoSincronizacion
import com.daille.recordatorios.datos.Prioridad
import com.daille.recordatorios.datos.Recordatorio
import com.daille.recordatorios.datos.preferencias.CriterioOrdenamiento
import com.daille.recordatorios.presentacion.ErrorGeneralFormulario
import com.daille.recordatorios.presentacion.ErrorListaRecordatorios
import com.daille.recordatorios.presentacion.ErrorValidacionFormulario
import com.daille.recordatorios.presentacion.EstadoConfiguracion
import com.daille.recordatorios.presentacion.EstadoDetalleRecordatorio
import com.daille.recordatorios.presentacion.EstadoFormularioRecordatorio
import com.daille.recordatorios.presentacion.EstadoListaRecordatorios
import com.daille.recordatorios.presentacion.MensajeSincronizacion
import com.daille.recordatorios.presentacion.ModeloVistaConfiguracion
import com.daille.recordatorios.presentacion.ModeloVistaDetalleRecordatorio
import com.daille.recordatorios.presentacion.ModeloVistaFormularioRecordatorio
import com.daille.recordatorios.presentacion.ModeloVistaListaRecordatorios
import com.daille.recordatorios.presentacion.criteriosDisponibles
import java.text.DateFormat
import java.util.Date

/** Asocia categorías con colores constantes para complementar, sin reemplazar, sus etiquetas de texto. */
private val coloresPorCategoria: Map<String, Color> = mapOf(
    "estudio" to Color(0xFF6750A4),
    "trabajo" to Color(0xFF006C4C),
    "personal" to Color(0xFFB3261E)
)

/** Muestra los recordatorios persistidos en Room y comunica eventos mediante callbacks al ViewModel. */
@Composable
fun PantallaListaRecordatorios(
    onCrearRecordatorio: () -> Unit,
    onRecordatorioClick: (String) -> Unit,
    onAbrirConfiguracion: () -> Unit,
    modifier: Modifier = Modifier,
    modeloVista: ModeloVistaListaRecordatorios = viewModel()
) {
    // Observa el StateFlow sin recolectar datos cuando el destino no está activo.
    val estado = modeloVista.estado.collectAsStateWithLifecycle().value

    Scaffold(
        modifier = modifier,
        topBar = {
            BarraSuperiorListado(
                onSincronizar = modeloVista::sincronizarRecordatorios,
                onAbrirConfiguracion = onAbrirConfiguracion
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCrearRecordatorio) {
                Text(text = "+", style = MaterialTheme.typography.headlineSmall)
            }
        }
    ) { rellenoInterno ->
        when (estado) {
            EstadoListaRecordatorios.Cargando -> ContenidoCentrado(
                mensaje = stringResource(R.string.cargando_recordatorios),
                rellenoInterno = rellenoInterno
            )

            is EstadoListaRecordatorios.Vacio -> ContenidoVacio(
                rellenoInterno = rellenoInterno,
                onCrearRecordatorio = onCrearRecordatorio
            )

            is EstadoListaRecordatorios.ConDatos -> ListaRecordatorios(
                recordatorios = estado.recordatorios,
                sincronizando = estado.sincronizando,
                mensajeSincronizacion = estado.mensajeSincronizacion,
                rellenoInterno = rellenoInterno,
                onRecordatorioClick = onRecordatorioClick,
                onEliminarRecordatorio = modeloVista::eliminarRecordatorio
            )

            is EstadoListaRecordatorios.Error -> ContenidoError(
                motivo = estado.motivo,
                rellenoInterno = rellenoInterno,
                onReintentar = modeloVista::sincronizarRecordatorios
            )
        }
    }
}

/** Reúne sincronización y ajustes en un menú para mantener limpia la barra superior. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BarraSuperiorListado(
    onSincronizar: () -> Unit,
    onAbrirConfiguracion: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Conserva el estado del menú contextual mientras la pantalla permanece compuesta.
    var menuVisible by remember { mutableStateOf(false) }
    val descripcionMenu = stringResource(R.string.descripcion_menu_acciones)

    TopAppBar(
        modifier = modifier,
        title = { Text(text = stringResource(R.string.titulo_pantalla)) },
        actions = {
            IconButton(onClick = { menuVisible = true }) {
                Text(
                    text = "⋮",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { contentDescription = descripcionMenu }
                )
            }
            DropdownMenu(expanded = menuVisible, onDismissRequest = { menuVisible = false }) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.accion_sincronizar)) },
                    onClick = {
                        menuVisible = false
                        onSincronizar()
                    }
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.accion_configuracion)) },
                    onClick = {
                        menuVisible = false
                        onAbrirConfiguracion()
                    }
                )
            }
        }
    )
}

/** Dibuja el listado observado desde Room y un aviso no bloqueante sobre el resultado de sincronización. */
@Composable
private fun ListaRecordatorios(
    recordatorios: List<Recordatorio>,
    sincronizando: Boolean,
    mensajeSincronizacion: MensajeSincronizacion?,
    rellenoInterno: PaddingValues,
    onRecordatorioClick: (String) -> Unit,
    onEliminarRecordatorio: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(rellenoInterno),
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
        if (sincronizando) {
            item { TextoSincronizacion(stringResource(R.string.sincronizando_recordatorios)) }
        }
        mensajeSincronizacion?.let { mensaje ->
            item { TextoSincronizacion(textoMensajeSincronizacion(mensaje)) }
        }
        items(recordatorios, key = { recordatorio -> recordatorio.identificador }) { recordatorio ->
            TarjetaRecordatorio(
                recordatorio = recordatorio,
                onClick = { onRecordatorioClick(recordatorio.identificador) },
                onEliminar = { onEliminarRecordatorio(recordatorio.identificador) }
            )
        }
    }
}

/** Comunica un estado de sincronización mediante texto además de conservar los datos visibles. */
@Composable
private fun TextoSincronizacion(texto: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.secondaryContainer) {
        Text(
            text = texto,
            modifier = Modifier.padding(12.dp),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/** Presenta un mensaje mientras se obtienen los datos persistidos o un detalle aún no disponible. */
@Composable
private fun ContenidoCentrado(mensaje: String, rellenoInterno: PaddingValues, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(rellenoInterno).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = mensaje, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Comunica que Room no tiene registros visibles y ofrece iniciar el flujo de creación. */
@Composable
private fun ContenidoVacio(
    rellenoInterno: PaddingValues,
    onCrearRecordatorio: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(rellenoInterno).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = stringResource(R.string.lista_vacia), style = MaterialTheme.typography.titleMedium)
        Button(onClick = onCrearRecordatorio, modifier = Modifier.padding(top = 16.dp)) {
            Text(text = stringResource(R.string.agregar_recordatorio))
        }
    }
}

/** Muestra un error de carga separado del estado vacío y ofrece un reintento explícito. */
@Composable
private fun ContenidoError(
    motivo: ErrorListaRecordatorios,
    rellenoInterno: PaddingValues,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(rellenoInterno).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = textoErrorLista(motivo), style = MaterialTheme.typography.titleMedium)
        Button(onClick = onReintentar, modifier = Modifier.padding(top = 16.dp)) {
            Text(text = stringResource(R.string.reintentar))
        }
    }
}

/** Representa un recordatorio reutilizable con acceso al detalle y una acción de eliminación persistente. */
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
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = recordatorio.titulo,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onEliminar) {
                    Text(text = "🗑", modifier = Modifier.semantics { contentDescription = descripcionEliminar })
                }
            }
            FilaEtiquetas(recordatorio, colorCategoria)
            Text(
                text = stringResource(R.string.fecha_recordatorio, formatearFecha(recordatorio.fechaProgramada)),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/** Agrupa etiquetas de categoría, prioridad, estado y sincronización para que su significado no dependa del color. */
@Composable
private fun FilaEtiquetas(recordatorio: Recordatorio, colorCategoria: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Etiqueta(stringResource(recordatorio.categoria.recursoNombre), colorCategoria)
            Etiqueta(stringResource(recordatorio.prioridad.recursoNombre), colorPrioridad(recordatorio.prioridad))
            Etiqueta(stringResource(recordatorio.estado.recursoNombre), colorEstado(recordatorio.estado))
        }
        Etiqueta(stringResource(recordatorio.estadoSincronizacion.recursoNombre), colorSincronizacion(recordatorio.estadoSincronizacion))
    }
}

/** Muestra una etiqueta de texto acompañada por un color semántico. */
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

/** Presenta el formulario que crea el registro en Room antes de cualquier sincronización remota. */
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
            onCancelar = onCancelar
        )
    }
}

/** Construye los controles validados y comunica sus eventos al ViewModel mediante funciones recibidas. */
@Composable
private fun FormularioRecordatorio(
    estado: EstadoFormularioRecordatorio,
    rellenoInterno: PaddingValues,
    onTituloChange: (String) -> Unit,
    onDescripcionChange: (String) -> Unit,
    onCategoriaChange: (String) -> Unit,
    onPrioridadChange: (Prioridad) -> Unit,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(rellenoInterno),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            CampoTextoValidado(stringResource(R.string.etiqueta_titulo), estado.titulo, estado.errorTitulo, onTituloChange)
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
            Text(stringResource(R.string.etiqueta_categoria), style = MaterialTheme.typography.titleSmall)
            SelectorCategoria(estado.categoria?.identificador, onCategoriaChange, estado.errorCategoria)
        }
        item {
            Text(stringResource(R.string.etiqueta_prioridad), style = MaterialTheme.typography.titleSmall)
            SelectorPrioridad(estado.prioridad, onPrioridadChange, estado.errorPrioridad)
        }
        estado.errorGeneral?.let { errorGeneral ->
            item { Text(textoErrorGeneral(errorGeneral), color = MaterialTheme.colorScheme.error) }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onCancelar, modifier = Modifier.weight(1f), enabled = !estado.enviando) {
                    Text(stringResource(R.string.cancelar))
                }
                Button(onClick = onGuardar, modifier = Modifier.weight(1f), enabled = estado.esValido && !estado.enviando) {
                    Text(stringResource(if (estado.enviando) R.string.guardando else R.string.guardar))
                }
            }
        }
    }
}

/** Representa un campo controlado con errores visibles y semánticos para tecnologías de asistencia. */
@Composable
private fun CampoTextoValidado(
    etiqueta: String,
    valor: String,
    errorValidacion: ErrorValidacionFormulario?,
    onValorChange: (String) -> Unit,
    lineasMinimas: Int = 1,
    modifier: Modifier = Modifier
) {
    val textoError = errorValidacion?.let { errorActual -> textoErrorValidacion(errorActual) }
    OutlinedTextField(
        value = valor,
        onValueChange = onValorChange,
        modifier = modifier.fillMaxWidth().semantics { textoError?.let { mensaje -> error(mensaje) } },
        label = { Text(etiqueta) },
        supportingText = { textoError?.let { mensaje -> Text(mensaje) } },
        isError = textoError != null,
        minLines = lineasMinimas
    )
}

/** Permite seleccionar únicamente categorías disponibles en el catálogo de dominio. */
@Composable
private fun SelectorCategoria(
    categoriaSeleccionada: String?,
    onCategoriaChange: (String) -> Unit,
    errorValidacion: ErrorValidacionFormulario?,
    modifier: Modifier = Modifier
) {
    val textoError = errorValidacion?.let { errorActual -> textoErrorValidacion(errorActual) }
    Column(modifier = modifier.semantics { textoError?.let { mensaje -> error(mensaje) } }) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogoCategorias.categoriasDisponibles.forEach { categoria ->
                FilterChip(
                    selected = categoria.identificador == categoriaSeleccionada,
                    onClick = { onCategoriaChange(categoria.identificador) },
                    label = { Text(stringResource(categoria.recursoNombre)) }
                )
            }
        }
        textoError?.let { mensaje -> Text(mensaje, color = MaterialTheme.colorScheme.error) }
    }
}

/** Permite seleccionar una prioridad y comunica el valor al ViewModel sin conservar estado local. */
@Composable
private fun SelectorPrioridad(
    prioridadSeleccionada: Prioridad?,
    onPrioridadChange: (Prioridad) -> Unit,
    errorValidacion: ErrorValidacionFormulario?,
    modifier: Modifier = Modifier
) {
    val textoError = errorValidacion?.let { errorActual -> textoErrorValidacion(errorActual) }
    Column(modifier = modifier.semantics { textoError?.let { mensaje -> error(mensaje) } }) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Prioridad.entries.forEach { prioridad ->
                FilterChip(
                    selected = prioridad == prioridadSeleccionada,
                    onClick = { onPrioridadChange(prioridad) },
                    label = { Text(stringResource(prioridad.recursoNombre)) }
                )
            }
        }
        textoError?.let { mensaje -> Text(mensaje, color = MaterialTheme.colorScheme.error) }
    }
}

/** Muestra y observa el detalle de un registro persistido, incluso cuando Room lo actualiza. */
@Composable
fun PantallaDetalleRecordatorio(
    identificador: String,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    modeloVista: ModeloVistaDetalleRecordatorio = viewModel()
) {
    val estado = modeloVista.estado.collectAsStateWithLifecycle().value
    LaunchedEffect(identificador) { modeloVista.cargarDetalle(identificador) }
    Scaffold(modifier = modifier, topBar = { BarraSuperiorSimple(R.string.detalle_recordatorio, onVolver) }) { rellenoInterno ->
        when (estado) {
            EstadoDetalleRecordatorio.Cargando -> ContenidoCentrado(stringResource(R.string.cargando_recordatorios), rellenoInterno)
            EstadoDetalleRecordatorio.NoEncontrado -> ContenidoCentrado(stringResource(R.string.recordatorio_no_encontrado), rellenoInterno)
            is EstadoDetalleRecordatorio.Disponible -> ContenidoDetalle(
                recordatorio = estado.recordatorio,
                rellenoInterno = rellenoInterno,
                onAlternarCompletado = { modeloVista.alternarCompletado(estado.recordatorio) }
            )
        }
    }
}

/** Organiza la información completa y permite modificar el estado de una fila guardada en Room. */
@Composable
private fun ContenidoDetalle(
    recordatorio: Recordatorio,
    rellenoInterno: PaddingValues,
    onAlternarCompletado: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(rellenoInterno).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(recordatorio.titulo, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(recordatorio.descripcion, style = MaterialTheme.typography.bodyLarge)
        FilaEtiquetas(recordatorio, coloresPorCategoria[recordatorio.categoria.identificador] ?: MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.fecha_recordatorio, formatearFecha(recordatorio.fechaProgramada)))
        Button(onClick = onAlternarCompletado) {
            val recurso = if (recordatorio.estado == EstadoRecordatorio.Completado) R.string.marcar_pendiente else R.string.marcar_completado
            Text(stringResource(recurso))
        }
    }
}

/** Permite modificar las tres preferencias requeridas y comprobar que DataStore las mantiene entre sesiones. */
@Composable
fun PantallaConfiguracion(
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    modeloVista: ModeloVistaConfiguracion = viewModel()
) {
    val estado = modeloVista.estado.collectAsStateWithLifecycle().value
    Scaffold(modifier = modifier, topBar = { BarraSuperiorSimple(R.string.titulo_configuracion, onVolver) }) { rellenoInterno ->
        ContenidoConfiguracion(
            estado = estado,
            rellenoInterno = rellenoInterno,
            onModoOscuroChange = modeloVista::cambiarModoOscuro,
            onMostrarCompletadosChange = modeloVista::cambiarMostrarCompletados,
            onCriterioChange = modeloVista::cambiarCriterioOrdenamiento
        )
    }
}

/** Representa controles que escriben preferencias mediante callbacks suspendidos por el ViewModel. */
@Composable
private fun ContenidoConfiguracion(
    estado: EstadoConfiguracion,
    rellenoInterno: PaddingValues,
    onModoOscuroChange: (Boolean) -> Unit,
    onMostrarCompletadosChange: (Boolean) -> Unit,
    onCriterioChange: (CriterioOrdenamiento) -> Unit,
    modifier: Modifier = Modifier
) {
    if (estado.cargando) {
        ContenidoCentrado(stringResource(R.string.cargando_preferencias), rellenoInterno, modifier)
        return
    }
    Column(
        modifier = modifier.fillMaxSize().padding(rellenoInterno).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        FilaInterruptor(
            titulo = stringResource(R.string.modo_oscuro),
            activo = estado.preferencias.modoOscuro,
            onCambio = onModoOscuroChange
        )
        FilaInterruptor(
            titulo = stringResource(R.string.mostrar_completados),
            activo = estado.preferencias.mostrarCompletados,
            onCambio = onMostrarCompletadosChange
        )
        Text(stringResource(R.string.ordenar_por), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            criteriosDisponibles.forEach { criterio ->
                FilterChip(
                    selected = criterio == estado.preferencias.criterioOrdenamiento,
                    onClick = { onCriterioChange(criterio) },
                    label = { Text(stringResource(recursoCriterio(criterio))) }
                )
            }
        }
        Text(stringResource(R.string.texto_preferencias_persistentes), style = MaterialTheme.typography.bodySmall)
    }
}

/** Agrupa un texto y su interruptor para que la preferencia sea comprensible y accesible. */
@Composable
private fun FilaInterruptor(titulo: String, activo: Boolean, onCambio: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(titulo, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = activo, onCheckedChange = onCambio)
    }
}

/** Muestra una barra reutilizable que solicita volver sin conocer el NavController. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BarraSuperiorSimple(titulo: Int, onVolver: () -> Unit, modifier: Modifier = Modifier) {
    TopAppBar(
        modifier = modifier,
        title = { Text(stringResource(titulo)) },
        navigationIcon = { TextButton(onClick = onVolver) { Text(stringResource(R.string.volver)) } }
    )
}

/** Traduce las validaciones de dominio a recursos de texto visibles para la interfaz actual. */
@Composable
private fun textoErrorValidacion(errorValidacion: ErrorValidacionFormulario): String = when (errorValidacion) {
    ErrorValidacionFormulario.TITULO_CORTO -> stringResource(R.string.error_titulo_corto)
    ErrorValidacionFormulario.DESCRIPCION_LARGA -> stringResource(R.string.error_descripcion_larga)
    ErrorValidacionFormulario.CATEGORIA_INVALIDA -> stringResource(R.string.error_categoria)
    ErrorValidacionFormulario.PRIORIDAD_NO_SELECCIONADA -> stringResource(R.string.error_prioridad)
}

/** Traduce el error de persistencia sin colocar textos dependientes de idioma en el ViewModel. */
@Composable
private fun textoErrorGeneral(errorGeneral: ErrorGeneralFormulario): String = when (errorGeneral) {
    ErrorGeneralFormulario.GUARDADO -> stringResource(R.string.error_guardado)
}

/** Traduce un error de carga recuperable. */
@Composable
private fun textoErrorLista(errorLista: ErrorListaRecordatorios): String = when (errorLista) {
    ErrorListaRecordatorios.CARGA -> stringResource(R.string.error_carga)
}

/** Traduce el resultado de sincronización preservando la separación entre lógica y recursos visuales. */
@Composable
private fun textoMensajeSincronizacion(mensaje: MensajeSincronizacion): String = when (mensaje) {
    MensajeSincronizacion.SINCRONIZACION_COMPLETA -> stringResource(R.string.sincronizacion_completa)
    MensajeSincronizacion.ERROR_REINTENTABLE -> stringResource(R.string.sincronizacion_error_reintentable)
    MensajeSincronizacion.ERROR_PERMANENTE -> stringResource(R.string.sincronizacion_error_permanente)
}

/** Selecciona el recurso visible de cada criterio de orden persistido en DataStore. */
private fun recursoCriterio(criterio: CriterioOrdenamiento): Int = when (criterio) {
    CriterioOrdenamiento.FECHA -> R.string.orden_fecha
    CriterioOrdenamiento.PRIORIDAD -> R.string.orden_prioridad
}

/** Convierte la fecha almacenada como milisegundos en un texto legible para la persona usuaria. */
private fun formatearFecha(fecha: Long): String = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(fecha))

/** Devuelve un color complementario para cada prioridad. */
private fun colorPrioridad(prioridad: Prioridad): Color = when (prioridad) {
    Prioridad.ALTA -> Color(0xFFB3261E)
    Prioridad.MEDIA -> Color(0xFF9A6700)
    Prioridad.BAJA -> Color(0xFF006E1C)
}

/** Devuelve un color complementario para cada estado funcional. */
private fun colorEstado(estado: EstadoRecordatorio): Color = when (estado) {
    EstadoRecordatorio.Pendiente -> Color(0xFF2457D6)
    EstadoRecordatorio.Completado -> Color(0xFF006E1C)
    EstadoRecordatorio.Vencido -> Color(0xFFB3261E)
}

/** Devuelve un color complementario para el estado de sincronización local-first. */
private fun colorSincronizacion(estado: EstadoSincronizacion): Color = when (estado) {
    EstadoSincronizacion.SINCRONIZADO -> Color(0xFF006E1C)
    EstadoSincronizacion.PENDIENTE -> Color(0xFF2457D6)
    EstadoSincronizacion.PENDIENTE_ELIMINACION -> Color(0xFF9A6700)
    EstadoSincronizacion.ERROR -> Color(0xFFB3261E)
}
