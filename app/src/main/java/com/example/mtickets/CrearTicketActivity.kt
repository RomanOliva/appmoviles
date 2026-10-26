package com.example.mtickets

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.mtickets.data.AppDatabase
import com.example.mtickets.data.Categoria
import com.example.mtickets.data.TicketRepositorio
import com.example.mtickets.data.Ubicacion
import com.example.mtickets.ui.componentes.BarraSuperior
import com.example.mtickets.ui.componentes.Selector
import com.example.mtickets.ui.componentes.SelectorFotos
import com.example.mtickets.ui.theme.MTicketsTheme
import kotlinx.coroutines.launch

class CrearTicketActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val usuarioId = intent.getIntExtra(EXTRA_USUARIO_ID, -1)
        setContent {
            MTicketsTheme {
                PantallaCrearTicket(
                    usuarioId = usuarioId,
                    onVolver = { finish() }
                )
            }
        }
    }
}

@Composable
fun PantallaCrearTicket(usuarioId: Int, onVolver: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getInstance(context) }
    val repo = remember { TicketRepositorio(db) }

    var categorias by remember { mutableStateOf<List<Categoria>>(emptyList()) }
    var ubicaciones by remember { mutableStateOf<List<Ubicacion>>(emptyList()) }

    // rememberSaveable conserva lo escrito si el teléfono se rota.
    var titulo by rememberSaveable { mutableStateOf("") }
    var descripcion by rememberSaveable { mutableStateOf("") }
    var detalleUbicacion by rememberSaveable { mutableStateOf("") }
    var categoriaId by rememberSaveable { mutableStateOf<Int?>(null) }
    var ubicacionId by rememberSaveable { mutableStateOf<Int?>(null) }
    var fotos by rememberSaveable { mutableStateOf(listOf<String>()) }

    var enviando by remember { mutableStateOf(false) }
    var codigoCreado by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        categorias = db.catalogoDao().listarCategorias()
        ubicaciones = db.catalogoDao().listarUbicaciones()
    }

    Scaffold(
        topBar = { BarraSuperior(titulo = "Nuevo ticket", onVolver = onVolver) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = titulo,
                onValueChange = { titulo = it },
                label = { Text("Título del problema") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Selector(
                etiqueta = "Categoría",
                opciones = categorias,
                seleccionado = categorias.find { it.id == categoriaId },
                texto = { it.nombre },
                onSeleccion = { categoriaId = it?.id }
            )

            // No se usa GPS: el usuario elige o escribe el lugar donde está el problema.
            Selector(
                etiqueta = "Ubicación en la escuela",
                opciones = ubicaciones,
                seleccionado = ubicaciones.find { it.id == ubicacionId },
                texto = { it.nombre },
                onSeleccion = { ubicacionId = it?.id }
            )

            OutlinedTextField(
                value = detalleUbicacion,
                onValueChange = { detalleUbicacion = it },
                label = { Text("Detalle de la ubicación (opcional)") },
                supportingText = { Text("Ej: segundo piso, cerca de la ventana") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripción") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Fotografías (opcional)",
                style = MaterialTheme.typography.titleSmall
            )
            SelectorFotos(
                rutas = fotos,
                onCambio = { fotos = it }
            )

            Text(
                text = "Esta aplicación no reemplaza los procedimientos de emergencia: " +
                        "si hay riesgo inmediato, avisa por los canales que defina la escuela.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = {
                    val cat = categoriaId
                    val ubi = ubicacionId
                    if (cat != null && ubi != null) {
                        enviando = true
                        scope.launch {
                            codigoCreado = repo.crearTicket(
                                titulo = titulo.trim(),
                                descripcion = descripcion.trim(),
                                categoriaId = cat,
                                ubicacionId = ubi,
                                detalleUbicacion = detalleUbicacion.trim(),
                                solicitanteId = usuarioId,
                                rutasFotos = fotos
                            )
                            enviando = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !enviando && validarTicket(titulo, descripcion, categoriaId, ubicacionId)
            ) {
                Text("Enviar reporte")
            }
        }
    }

    val codigo = codigoCreado
    if (codigo != null) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Ticket creado") },
            text = {
                Text("Tu solicitud quedó registrada con el número $codigo. Puedes revisar su avance en \"Mis solicitudes\".")
            },
            confirmButton = {
                TextButton(onClick = onVolver) { Text("Aceptar") }
            }
        )
    }
}

fun validarTicket(titulo: String, descripcion: String, categoriaId: Int?, ubicacionId: Int?): Boolean {
    return titulo.trim().length >= 5 &&
            descripcion.trim().length >= 10 &&
            categoriaId != null &&
            ubicacionId != null
}
