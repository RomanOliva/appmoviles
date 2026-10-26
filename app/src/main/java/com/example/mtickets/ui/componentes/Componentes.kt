package com.example.mtickets.ui.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mtickets.data.Estado
import com.example.mtickets.data.TicketVista
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Convierte una fecha en milisegundos a texto legible (ej: 05-10-2026 14:30).
fun formatearFecha(milisegundos: Long?): String {
    if (milisegundos == null) return "—"
    val formato = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.forLanguageTag("es-CL"))
    return formato.format(Date(milisegundos))
}

// Barra superior reutilizable (igual al ejemplo TopAppBar del profesor).
// Si se entrega onVolver, aparece la flecha para regresar.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraSuperior(
    titulo: String,
    onVolver: (() -> Unit)? = null,
    acciones: @Composable RowScope.() -> Unit = {}
) {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        title = {
            Text(
                text = titulo,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            if (onVolver != null) {
                IconButton(onClick = onVolver) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver"
                    )
                }
            }
        },
        actions = acciones
    )
}

// Lista desplegable (botón + DropdownMenu, como "MenuComunas" del ejemplo de formularios).
// Si se entrega textoVacio, se agrega una opción para dejar el campo sin selección (null).
@Composable
fun <T> Selector(
    etiqueta: String,
    opciones: List<T>,
    seleccionado: T?,
    texto: (T) -> String,
    onSeleccion: (T?) -> Unit,
    textoVacio: String? = null
) {
    var expandido by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box {
            OutlinedButton(
                onClick = { expandido = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (seleccionado != null) texto(seleccionado) else (textoVacio ?: "Seleccionar"),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }

            DropdownMenu(
                expanded = expandido,
                onDismissRequest = { expandido = false }
            ) {
                if (textoVacio != null) {
                    DropdownMenuItem(
                        text = { Text(textoVacio) },
                        onClick = {
                            onSeleccion(null)
                            expandido = false
                        }
                    )
                }
                opciones.forEach { opcion ->
                    DropdownMenuItem(
                        text = { Text(texto(opcion)) },
                        onClick = {
                            onSeleccion(opcion)
                            expandido = false
                        }
                    )
                }
            }
        }
    }
}

// Etiqueta de color con el estado del ticket.
@Composable
fun EstadoChip(estado: Estado) {
    val (fondo, letra) = when (estado) {
        Estado.PENDIENTE -> Color(0xFFFFE0B2) to Color(0xFF6D4C00)
        Estado.REVISADO -> Color(0xFFBBDEFB) to Color(0xFF0D47A1)
        Estado.ASIGNADO -> Color(0xFFD1C4E9) to Color(0xFF311B92)
        Estado.EN_PROCESO -> Color(0xFFFFF59D) to Color(0xFF5F5200)
        Estado.SOLUCIONADO -> Color(0xFFC8E6C9) to Color(0xFF1B5E20)
        Estado.CERRADO -> Color(0xFFCFD8DC) to Color(0xFF263238)
        Estado.CANCELADO -> Color(0xFFFFCDD2) to Color(0xFFB71C1C)
    }
    Surface(
        color = fondo,
        contentColor = letra,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = estado.etiqueta,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

// Tarjeta con el resumen de un ticket (se usa en las listas).
@Composable
fun TarjetaTicket(ticket: TicketVista, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ticket.codigo,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                EstadoChip(ticket.estado)
            }
            Text(
                text = ticket.titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${ticket.categoria} · ${ticket.ubicacion}",
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Prioridad: ${ticket.prioridad?.etiqueta ?: "Sin definir"}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = formatearFecha(ticket.fechaCreacion),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// Tarjeta con título, usada para agrupar secciones en la pantalla de detalle.
@Composable
fun SeccionCard(titulo: String, contenido: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            contenido()
        }
    }
}

// Fila "Etiqueta: valor".
@Composable
fun FilaDato(etiqueta: String, valor: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = etiqueta,
            modifier = Modifier.width(110.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = valor,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
