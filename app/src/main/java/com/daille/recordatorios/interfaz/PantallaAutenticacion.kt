package com.daille.recordatorios.interfaz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.daille.recordatorios.R
import com.daille.recordatorios.presentacion.ErrorAutenticacion
import com.daille.recordatorios.presentacion.EstadoAutenticacion
import com.daille.recordatorios.presentacion.ModeloVistaAutenticacion

/** Muestra el acceso a la aplicación sin llamar a Firebase directamente desde Compose. */
@Composable
fun PantallaAutenticacion(
    modifier: Modifier = Modifier,
    modeloVista: ModeloVistaAutenticacion = viewModel()
) {
    // Recoge un UiState producido por el ViewModel y delega todas las acciones en callbacks.
    val estado = modeloVista.estado.collectAsStateWithLifecycle().value
    if (estado.cargandoSesion) {
        ContenidoCargandoAutenticacion(modifier)
    } else {
        FormularioAutenticacion(
            estado = estado,
            onCorreoChange = modeloVista::cambiarCorreo,
            onContrasenaChange = modeloVista::cambiarContrasena,
            onEnviar = modeloVista::enviarCredenciales,
            onCambiarModo = modeloVista::cambiarModoRegistro,
            modifier = modifier
        )
    }
}

/** Informa que Firebase está comprobando si existe una sesión previamente iniciada. */
@Composable
private fun ContenidoCargandoAutenticacion(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.autenticacion_comprobando_sesion),
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

/** Contiene campos de correo y contraseña que permanecen en memoria durante el acceso o registro. */
@Composable
private fun FormularioAutenticacion(
    estado: EstadoAutenticacion,
    onCorreoChange: (String) -> Unit,
    onContrasenaChange: (String) -> Unit,
    onEnviar: () -> Unit,
    onCambiarModo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val titulo = stringResource(if (estado.modoRegistro) R.string.autenticacion_registro else R.string.autenticacion_inicio)
    val textoAccion = stringResource(if (estado.modoRegistro) R.string.autenticacion_registrar else R.string.autenticacion_ingresar)
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(titulo, style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = estado.correo,
            onValueChange = onCorreoChange,
            label = { Text(stringResource(R.string.autenticacion_correo)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
        )
        OutlinedTextField(
            value = estado.contrasena,
            onValueChange = onContrasenaChange,
            label = { Text(stringResource(R.string.autenticacion_contrasena)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        )
        estado.error?.let { error ->
            Text(
                text = textoErrorAutenticacion(error),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        Button(
            onClick = onEnviar,
            enabled = !estado.operacionEnCurso,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
        ) {
            if (estado.operacionEnCurso) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(textoAccion)
            }
        }
        TextButton(onClick = onCambiarModo, enabled = !estado.operacionEnCurso) {
            Text(stringResource(if (estado.modoRegistro) R.string.autenticacion_ir_inicio else R.string.autenticacion_ir_registro))
        }
    }
}

/** Traduce los errores de acceso sin almacenar textos dependientes del idioma en el ViewModel. */
@Composable
private fun textoErrorAutenticacion(error: ErrorAutenticacion): String = when (error) {
    ErrorAutenticacion.CAMPOS_INVALIDOS -> stringResource(R.string.autenticacion_error_campos)
    ErrorAutenticacion.CREDENCIALES -> stringResource(R.string.autenticacion_error_credenciales)
    ErrorAutenticacion.CONFIGURACION -> stringResource(R.string.autenticacion_error_configuracion)
    ErrorAutenticacion.DESCONOCIDO -> stringResource(R.string.autenticacion_error_desconocido)
}
