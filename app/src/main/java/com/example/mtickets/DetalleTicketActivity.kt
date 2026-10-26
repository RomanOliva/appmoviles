package com.example.mtickets

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mtickets.data.AppDatabase
import com.example.mtickets.data.Categoria
import com.example.mtickets.data.Comentario
import com.example.mtickets.data.ComentarioVista
import com.example.mtickets.data.Estado
import com.example.mtickets.data.Evidencia
import com.example.mtickets.data.HistorialVista
import com.example.mtickets.data.Prioridad
import com.example.mtickets.data.Rol
import com.example.mtickets.data.TicketRepositorio
import com.example.mtickets.data.TicketVista
import com.example.mtickets.data.TipoEvidencia
import com.example.mtickets.data.Usuario
import com.example.mtickets.ui.componentes.BarraSuperior
import com.example.mtickets.ui.componentes.EstadoChip
import com.example.mtickets.ui.componentes.FilaDato
import com.example.mtickets.ui.componentes.GaleriaEvidencias
import com.example.mtickets.ui.componentes.SeccionCard
import com.example.mtickets.ui.componentes.Selector
import com.example.mtickets.ui.componentes.SelectorFotos
import com.example.mtickets.ui.componentes.formatearFecha
import com.example.mtickets.ui.theme.MTicketsTheme
import kotlinx.coroutines.launch

class DetalleTicketActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val ticketId = intent.getIntExtra(EXTRA_TICKET_ID, -1)
        val usuarioId = intent.getIntExtra(EXTRA_USUARIO_ID, -1)
        setContent {
            MTicketsTheme {
                PantallaDetalle(
                    ticketId = ticketId,
                    usuarioId = usuarioId,
                    onVolver = { finish() }
                )
            }
        }
    }
}

@Composable
fun PantallaDetalle(ticketId: Int, usuarioId: Int, onVolver: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getInstance(context) }
    val repo = remember { TicketRepositorio(db) }

    var recarga by remember { mutableIntStateOf(0) }

    var usuario by remember { mutableStateOf<Usuario?>(null) }
    var ticket by remember { mutableStateOf<TicketVista?>(null) }
    var comentarios by remember { mutableStateOf<List<ComentarioVista>>(emptyList()) }
    var historial by remember { mutableStateOf<List<HistorialVista>>(emptyList()) }
    var evidencias by remember { mutableStateOf<List<Evidencia>>(emptyList()) }
    var categorias by remember { mutableStateOf<List<Categoria>>(emptyList()) }
    var responsables by remember { mutableStateOf<List<Usuario>>(emptyList()) }
    var cargado by remember { mutableStateOf(false) }

    LaunchedEffect(recarga) {
        val dao = db.ticketDao()
        usuario = db.usuarioDao().buscarPorId(usuarioId)
        ticket = dao.buscarVista(ticketId)
        comentarios = dao.listarComentarios(ticketId)
        historial = dao.listarHistorial(ticketId)
        evidencias = dao.listarEvidencias(ticketId)
        categorias = db.catalogoDao().listarCategorias()
        responsables = db.usuarioDao().listarResponsables()
        cargado = true
    }

    val t = ticket
    val u = usuario

    Scaffold(
        topBar = { BarraSuperior(titulo = t?.codigo ?: "Ticket", onVolver = onVolver) }
    ) { innerPadding ->
        if (t == null || u == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                if (cargado) Text("No se encontró el ticket") else CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ResumenTicket(t)

                GaleriaEvidencias(
                    titulo = "Fotografías del problema",
                    rutas = evidencias.filter { it.tipo == TipoEvidencia.PROBLEMA }.map { it.ruta }
                )
                GaleriaEvidencias(
                    titulo = "Evidencia de la solución",
                    rutas = evidencias.filter { it.tipo == TipoEvidencia.SOLUCION }.map { it.ruta }
                )

                when (u.rol) {
                    Rol.ADMINISTRADOR -> PanelAdministrador(
                        ticket = t,
                        adminId = u.id,
                        categorias = categorias,
                        responsables = responsables,
                        repo = repo,
                        onActualizado = { recarga++ }
                    )

                    Rol.RESPONSABLE -> PanelResponsable(
                        ticket = t,
                        usuarioId = u.id,
                        repo = repo,
                        onActualizado = { recarga++ }
                    )

                    Rol.SOLICITANTE -> {}
                }

                SeccionHistorial(historial)

                val cerrado = t.estado == Estado.CERRADO || t.estado == Estado.CANCELADO
                SeccionComentarios(
                    comentarios = comentarios,
                    habilitado = !cerrado,
                    onEnviar = { texto ->
                        scope.launch {
                            repo.agregarComentario(t.id, u.id, texto)
                            recarga++
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ResumenTicket(t: TicketVista) {
    SeccionCard(titulo = t.titulo) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = t.codigo,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            EstadoChip(t.estado)
        }
        HorizontalDivider()

        val ubicacion = if (t.detalleUbicacion.isNotBlank()) "${t.ubicacion} (${t.detalleUbicacion})" else t.ubicacion
        FilaDato("Categoría", t.categoria)
        FilaDato("Ubicación", ubicacion)
        FilaDato("Prioridad", t.prioridad?.etiqueta ?: "Sin definir")
        FilaDato("Solicitante", "${t.solicitante} · ${t.cargoSolicitante}")
        FilaDato("Responsable", t.responsable ?: "Sin asignar")
        FilaDato("Creado", formatearFecha(t.fechaCreacion))
        if (t.fechaAsignacion != null) FilaDato("Asignado", formatearFecha(t.fechaAsignacion))
        if (t.fechaSolucion != null) FilaDato("Solucionado", formatearFecha(t.fechaSolucion))
        if (t.fechaCierre != null) FilaDato("Cerrado", formatearFecha(t.fechaCierre))
        if (t.materiales.isNotBlank()) FilaDato("Requiere", t.materiales)

        HorizontalDivider()
        Text(text = "Descripción", style = MaterialTheme.typography.labelLarge)
        Text(text = t.descripcion)
    }
}

@Composable
fun SeccionHistorial(historial: List<HistorialVista>) {
    SeccionCard(titulo = "Historial de estados") {
        if (historial.isEmpty()) {
            Text("Sin movimientos")
        }
        historial.forEach { h ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EstadoChip(h.estado)
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = h.usuario, style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = formatearFecha(h.fecha),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SeccionComentarios(
    comentarios: List<ComentarioVista>,
    habilitado: Boolean,
    onEnviar: (String) -> Unit
) {
    var texto by rememberSaveable { mutableStateOf("") }

    SeccionCard(titulo = "Comentarios") {
        if (comentarios.isEmpty()) {
            Text("Aún no hay comentarios")
        }
        comentarios.forEach { c ->
            Column {
                Text(
                    text = "${c.autor} (${c.rol.etiqueta}) · ${formatearFecha(c.fecha)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(text = c.texto)
            }
        }

        HorizontalDivider()

        if (habilitado) {
            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it },
                label = { Text("Escribe un comentario") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    onEnviar(texto.trim())
                    texto = ""
                },
                enabled = texto.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enviar comentario")
            }
        } else {
            Text(
                text = "El ticket está cerrado o cancelado: ya no admite comentarios.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun PanelAdministrador(
    ticket: TicketVista,
    adminId: Int,
    categorias: List<Categoria>,
    responsables: List<Usuario>,
    repo: TicketRepositorio,
    onActualizado: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var categoria by remember(ticket, categorias) { mutableStateOf(categorias.find { it.id == ticket.categoriaId }) }
    var prioridad by remember(ticket) { mutableStateOf(ticket.prioridad) }
    var responsable by remember(ticket, responsables) { mutableStateOf(responsables.find { it.id == ticket.responsableId }) }
    var estado by remember(ticket) { mutableStateOf(ticket.estado) }

    val bloqueado = ticket.estado == Estado.CERRADO || ticket.estado == Estado.CANCELADO

    SeccionCard(titulo = "Gestión del ticket (administrador)") {
        if (bloqueado) {
            Text("Este ticket está ${ticket.estado.etiqueta.lowercase()} y ya no admite cambios.")
        } else {
            Selector(
                etiqueta = "Categoría (confirmar)",
                opciones = categorias,
                seleccionado = categoria,
                texto = { it.nombre },
                onSeleccion = { categoria = it }
            )
            Selector(
                etiqueta = "Prioridad",
                opciones = Prioridad.entries,
                seleccionado = prioridad,
                texto = { it.etiqueta },
                onSeleccion = { prioridad = it },
                textoVacio = "Sin definir"
            )
            Selector(
                etiqueta = "Responsable (asignar o reasignar)",
                opciones = responsables,
                seleccionado = responsable,
                texto = { "${it.nombre} · ${it.cargo}" },
                onSeleccion = { responsable = it },
                textoVacio = "Sin asignar"
            )
            Selector(
                etiqueta = "Estado",
                opciones = Estado.entries,
                seleccionado = estado,
                texto = { it.etiqueta },
                onSeleccion = { if (it != null) estado = it }
            )

            Button(
                onClick = {
                    scope.launch {
                        val error = repo.guardarGestion(
                            ticketId = ticket.id,
                            actorId = adminId,
                            categoriaId = categoria?.id ?: ticket.categoriaId,
                            prioridad = prioridad,
                            responsableId = responsable?.id,
                            estadoDeseado = estado
                        )
                        Toast.makeText(context, error ?: "Cambios guardados", Toast.LENGTH_LONG).show()
                        if (error == null) onActualizado()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar cambios")
            }

            if (ticket.estado == Estado.SOLUCIONADO) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val error = repo.guardarGestion(
                                ticketId = ticket.id,
                                actorId = adminId,
                                categoriaId = categoria?.id ?: ticket.categoriaId,
                                prioridad = prioridad,
                                responsableId = responsable?.id,
                                estadoDeseado = Estado.CERRADO
                            )
                            Toast.makeText(context, error ?: "Ticket cerrado", Toast.LENGTH_LONG).show()
                            if (error == null) onActualizado()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Confirmar solución y cerrar ticket")
                }
            }
        }
    }
}

@Composable
fun PanelResponsable(
    ticket: TicketVista,
    usuarioId: Int,
    repo: TicketRepositorio,
    onActualizado: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var materiales by rememberSaveable { mutableStateOf("") }
    var acciones by rememberSaveable { mutableStateOf("") }
    var fotos by rememberSaveable { mutableStateOf(listOf<String>()) }

    SeccionCard(titulo = "Trabajo del responsable") {
        when (ticket.estado) {
            Estado.ASIGNADO -> {
                Text("Cuando comiences a trabajar en este problema, cambia el estado a \"En proceso\".")
                Button(
                    onClick = {
                        scope.launch {
                            repo.cambiarEstado(ticket.id, usuarioId, Estado.EN_PROCESO)
                            onActualizado()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Iniciar trabajo (En proceso)")
                }
            }

            Estado.EN_PROCESO -> {
                OutlinedTextField(
                    value = materiales,
                    onValueChange = { materiales = it },
                    label = { Text("Materiales o información que necesitas") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            repo.solicitarMateriales(ticket.id, usuarioId, materiales.trim())
                            materiales = ""
                            Toast.makeText(context, "Solicitud registrada", Toast.LENGTH_SHORT).show()
                            onActualizado()
                        }
                    },
                    enabled = materiales.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Informar que necesito materiales")
                }

                HorizontalDivider()

                Text("Evidencia de la reparación (opcional)", style = MaterialTheme.typography.titleSmall)
                SelectorFotos(rutas = fotos, onCambio = { fotos = it })

                OutlinedTextField(
                    value = acciones,
                    onValueChange = { acciones = it },
                    label = { Text("Acciones realizadas") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        scope.launch {
                            repo.marcarSolucionado(ticket.id, usuarioId, acciones.trim(), fotos)
                            acciones = ""
                            fotos = emptyList()
                            Toast.makeText(context, "Ticket marcado como solucionado", Toast.LENGTH_SHORT).show()
                            onActualizado()
                        }
                    },
                    enabled = acciones.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Marcar como solucionado")
                }
            }

            Estado.SOLUCIONADO -> {
                Text("Marcaste este ticket como solucionado. Queda pendiente la revisión y el cierre por parte del administrador.")
            }

            else -> {
                Text("Este ticket está ${ticket.estado.etiqueta.lowercase()}.")
            }
        }
    }
}
