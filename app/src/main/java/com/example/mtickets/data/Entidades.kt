package com.example.mtickets.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usuarios")
data class Usuario(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val cargo: String,
    val correo: String,
    val password: String,
    val rol: Rol
)

@Entity(tableName = "categorias")
data class Categoria(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String
)

@Entity(tableName = "ubicaciones")
data class Ubicacion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String
)

@Entity(tableName = "tickets")
data class Ticket(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val codigo: String = "",
    val titulo: String,
    val descripcion: String,
    val categoriaId: Int,
    val ubicacionId: Int,
    val detalleUbicacion: String = "",
    val solicitanteId: Int,
    val prioridad: Prioridad? = null,
    val estado: Estado = Estado.PENDIENTE,
    val responsableId: Int? = null,
    val materiales: String = "",
    val fechaCreacion: Long,
    val fechaAsignacion: Long? = null,
    val fechaSolucion: Long? = null,
    val fechaCierre: Long? = null
)

@Entity(tableName = "comentarios")
data class Comentario(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ticketId: Int,
    val autorId: Int,
    val texto: String,
    val fecha: Long
)

@Entity(tableName = "historial_estados")
data class HistorialEstado(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ticketId: Int,
    val estado: Estado,
    val usuarioId: Int,
    val fecha: Long
)

@Entity(tableName = "evidencias")
data class Evidencia(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ticketId: Int,
    val ruta: String,
    val tipo: TipoEvidencia,
    val usuarioId: Int,
    val fecha: Long
)

data class TicketVista(
    val id: Int,
    val codigo: String,
    val titulo: String,
    val descripcion: String,
    val categoriaId: Int,
    val categoria: String,
    val ubicacionId: Int,
    val ubicacion: String,
    val detalleUbicacion: String,
    val solicitanteId: Int,
    val solicitante: String,
    val cargoSolicitante: String,
    val prioridad: Prioridad?,
    val estado: Estado,
    val responsableId: Int?,
    val responsable: String?,
    val materiales: String,
    val fechaCreacion: Long,
    val fechaAsignacion: Long?,
    val fechaSolucion: Long?,
    val fechaCierre: Long?
)

data class ComentarioVista(
    val id: Int,
    val autor: String,
    val rol: Rol,
    val texto: String,
    val fecha: Long
)

data class HistorialVista(
    val estado: Estado,
    val usuario: String,
    val fecha: Long
)
