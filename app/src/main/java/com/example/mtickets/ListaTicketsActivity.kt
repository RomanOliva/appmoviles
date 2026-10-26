package com.example.mtickets

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mtickets.data.AppDatabase
import com.example.mtickets.data.Categoria
import com.example.mtickets.data.Estado
import com.example.mtickets.data.Prioridad
import com.example.mtickets.data.Rol
import com.example.mtickets.data.TicketVista
import com.example.mtickets.data.Ubicacion
import com.example.mtickets.data.Usuario
import com.example.mtickets.ui.componentes.BarraSuperior
import com.example.mtickets.ui.componentes.Selector
import com.example.mtickets.ui.componentes.TarjetaTicket
import com.example.mtickets.ui.theme.MTicketsTheme
import java.util.Calendar

class ListaTicketsActivity : ComponentActivity() {

    private var recarga by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val usuarioId = intent.getIntExtra(EXTRA_USUARIO_ID, -1)
        setContent {
            MTicketsTheme {
                PantallaLista(
                    usuarioId = usuarioId,
                    recarga = recarga,
                    onCerrarSesion = { finish() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        recarga++
    }
}

enum class Periodo(val etiqueta: String) {
    HOY("Hoy"),
    SIETE_DIAS("Últimos 7 días"),
    TREINTA_DIAS("Últimos 30 días");

    fun desde(): Long {
        val calendario = Calendar.getInstance()
        calendario.set(Calendar.HOUR_OF_DAY, 0)
        calendario.set(Calendar.MINUTE, 0)
        calendario.set(Calendar.SECOND, 0)
        calendario.set(Calendar.MILLISECOND, 0)
        when (this) {
            HOY -> {}
            SIETE_DIAS -> calendario.add(Calendar.DAY_OF_YEAR, -6)
            TREINTA_DIAS -> calendario.add(Calendar.DAY_OF_YEAR, -29)
        }
        return calendario.timeInMillis
    }
}

@Composable
fun PantallaLista(usuarioId: Int, recarga: Int, onCerrarSesion: () -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }

    var usuario by remember { mutableStateOf<Usuario?>(null) }
    var tickets by remember { mutableStateOf<List<TicketVista>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }

    var categorias by remember { mutableStateOf<List<Categoria>>(emptyList()) }
    var ubicaciones by remember { mutableStateOf<List<Ubicacion>>(emptyList()) }
    var responsables by remember { mutableStateOf<List<Usuario>>(emptyList()) }

    var filtroEstado by remember { mutableStateOf<Estado?>(null) }
    var filtroPrioridad by remember { mutableStateOf<Prioridad?>(null) }
    var filtroCategoria by remember { mutableStateOf<Categoria?>(null) }
    var filtroUbicacion by remember { mutableStateOf<Ubicacion?>(null) }
    var filtroResponsable by remember { mutableStateOf<Usuario?>(null) }
    var filtroPeriodo by remember { mutableStateOf<Periodo?>(null) }

    var mostrarFiltros by remember { mutableStateOf(false) }
    var menuAbierto by remember { mutableStateOf(false) }

    LaunchedEffect(usuarioId, recarga, filtroEstado, filtroPrioridad, filtroCategoria, filtroUbicacion, filtroResponsable, filtroPeriodo) {
        val u = db.usuarioDao().buscarPorId(usuarioId)
        usuario = u
        if (u != null) {
            val dao = db.ticketDao()
            tickets = when (u.rol) {
                Rol.SOLICITANTE -> dao.listarDeSolicitante(u.id)
                Rol.RESPONSABLE -> dao.listarAsignados(u.id)
                Rol.ADMINISTRADOR -> dao.listarTodos(
                    estado = filtroEstado?.name,
                    prioridad = filtroPrioridad?.name,
                    categoriaId = filtroCategoria?.id,
                    ubicacionId = filtroUbicacion?.id,
                    responsableId = filtroResponsable?.id,
                    desde = filtroPeriodo?.desde()
                )
            }
        }
        cargando = false
    }

    LaunchedEffect(Unit) {
        categorias = db.catalogoDao().listarCategorias()
        ubicaciones = db.catalogoDao().listarUbicaciones()
        responsables = db.usuarioDao().listarResponsables()
    }

    val u = usuario
    val esAdmin = u?.rol == Rol.ADMINISTRADOR
    val esSolicitante = u?.rol == Rol.SOLICITANTE

    val titulo = when (u?.rol) {
        Rol.SOLICITANTE -> "Mis solicitudes"
        Rol.ADMINISTRADOR -> "Bandeja de tickets"
        Rol.RESPONSABLE -> "Mis tickets asignados"
        null -> "MTickets"
    }

    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = titulo,
                acciones = {
                    if (esAdmin) {
                        IconButton(onClick = { mostrarFiltros = !mostrarFiltros }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filtros")
                        }
                    }
                    Box {
                        IconButton(onClick = { menuAbierto = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                        }
                        DropdownMenu(
                            expanded = menuAbierto,
                            onDismissRequest = { menuAbierto = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Cerrar sesión") },
                                onClick = {
                                    menuAbierto = false
                                    onCerrarSesion()
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (esSolicitante && u != null) {
                ExtendedFloatingActionButton(
                    onClick = {
                        val intent = Intent(context, CrearTicketActivity::class.java)
                        intent.putExtra(EXTRA_USUARIO_ID, u.id)
                        context.startActivity(intent)
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Nuevo ticket") }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (u != null) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
                    Text(
                        text = u.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${u.cargo} · ${u.rol.etiqueta}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (esAdmin && mostrarFiltros) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Selector(
                            etiqueta = "Estado",
                            opciones = Estado.entries,
                            seleccionado = filtroEstado,
                            texto = { it.etiqueta },
                            onSeleccion = { filtroEstado = it },
                            textoVacio = "Todos"
                        )
                        Selector(
                            etiqueta = "Prioridad",
                            opciones = Prioridad.entries,
                            seleccionado = filtroPrioridad,
                            texto = { it.etiqueta },
                            onSeleccion = { filtroPrioridad = it },
                            textoVacio = "Todas"
                        )
                        Selector(
                            etiqueta = "Categoría",
                            opciones = categorias,
                            seleccionado = filtroCategoria,
                            texto = { it.nombre },
                            onSeleccion = { filtroCategoria = it },
                            textoVacio = "Todas"
                        )
                        Selector(
                            etiqueta = "Ubicación",
                            opciones = ubicaciones,
                            seleccionado = filtroUbicacion,
                            texto = { it.nombre },
                            onSeleccion = { filtroUbicacion = it },
                            textoVacio = "Todas"
                        )
                        Selector(
                            etiqueta = "Responsable",
                            opciones = responsables,
                            seleccionado = filtroResponsable,
                            texto = { it.nombre },
                            onSeleccion = { filtroResponsable = it },
                            textoVacio = "Todos"
                        )
                        Selector(
                            etiqueta = "Fecha de creación",
                            opciones = Periodo.entries,
                            seleccionado = filtroPeriodo,
                            texto = { it.etiqueta },
                            onSeleccion = { filtroPeriodo = it },
                            textoVacio = "Todas las fechas"
                        )
                        TextButton(
                            onClick = {
                                filtroEstado = null
                                filtroPrioridad = null
                                filtroCategoria = null
                                filtroUbicacion = null
                                filtroResponsable = null
                                filtroPeriodo = null
                            }
                        ) {
                            Text("Limpiar filtros")
                        }
                    }
                }
            }

            if (!cargando) {
                Text(
                    text = "${tickets.size} ticket(s)",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                )
            }

            if (!cargando && tickets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (u?.rol) {
                            Rol.SOLICITANTE -> "Aún no has creado tickets. Usa el botón \"Nuevo ticket\"."
                            Rol.RESPONSABLE -> "No tienes tickets asignados por ahora."
                            else -> "No hay tickets que coincidan con los filtros."
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(tickets, key = { it.id }) { ticket ->
                        TarjetaTicket(ticket = ticket) {
                            val intent = Intent(context, DetalleTicketActivity::class.java)
                            intent.putExtra(EXTRA_TICKET_ID, ticket.id)
                            intent.putExtra(EXTRA_USUARIO_ID, usuarioId)
                            context.startActivity(intent)
                        }
                    }
                }
            }
        }
    }
}
