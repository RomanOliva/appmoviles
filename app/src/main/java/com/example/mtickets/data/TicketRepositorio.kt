package com.example.mtickets.data

import androidx.room.withTransaction

class TicketRepositorio(private val db: AppDatabase) {

    private val dao = db.ticketDao()

    suspend fun crearTicket(
        titulo: String,
        descripcion: String,
        categoriaId: Int,
        ubicacionId: Int,
        detalleUbicacion: String,
        solicitanteId: Int,
        rutasFotos: List<String>
    ): String {
        var codigo = ""
        db.withTransaction {
            val ahora = System.currentTimeMillis()
            val id = dao.insertarTicket(
                Ticket(
                    titulo = titulo,
                    descripcion = descripcion,
                    categoriaId = categoriaId,
                    ubicacionId = ubicacionId,
                    detalleUbicacion = detalleUbicacion,
                    solicitanteId = solicitanteId,
                    fechaCreacion = ahora
                )
            ).toInt()

            codigo = "MP-" + id.toString().padStart(4, '0')
            dao.actualizarCodigo(id, codigo)

            dao.insertarHistorial(
                HistorialEstado(ticketId = id, estado = Estado.PENDIENTE, usuarioId = solicitanteId, fecha = ahora)
            )
            rutasFotos.forEach { ruta ->
                dao.insertarEvidencia(
                    Evidencia(ticketId = id, ruta = ruta, tipo = TipoEvidencia.PROBLEMA, usuarioId = solicitanteId, fecha = ahora)
                )
            }
        }
        return codigo
    }

    suspend fun guardarGestion(
        ticketId: Int,
        actorId: Int,
        categoriaId: Int,
        prioridad: Prioridad?,
        responsableId: Int?,
        estadoDeseado: Estado
    ): String? {
        val ticket = dao.buscarTicket(ticketId) ?: return "El ticket no existe"

        if (ticket.estado == Estado.CERRADO || ticket.estado == Estado.CANCELADO) {
            return "El ticket ya está ${ticket.estado.etiqueta.lowercase()} y no puede modificarse"
        }

        var estado = estadoDeseado
        val cambioResponsable = responsableId != ticket.responsableId

        if (cambioResponsable && responsableId != null &&
            (estado == Estado.PENDIENTE || estado == Estado.REVISADO)
        ) {
            estado = Estado.ASIGNADO
        }

        val requiereResponsable = estado == Estado.ASIGNADO ||
                estado == Estado.EN_PROCESO ||
                estado == Estado.SOLUCIONADO
        if (requiereResponsable && responsableId == null) {
            return "Debe asignar un responsable para el estado \"${estado.etiqueta}\""
        }
        if (estado == Estado.ASIGNADO && prioridad == null) {
            return "Defina la prioridad antes de asignar el ticket"
        }
        if (estado == Estado.CERRADO && ticket.estado != Estado.SOLUCIONADO) {
            return "Solo se puede cerrar un ticket que esté solucionado"
        }

        val ahora = System.currentTimeMillis()
        val actualizado = ticket.copy(
            categoriaId = categoriaId,
            prioridad = prioridad,
            responsableId = responsableId,
            estado = estado,
            fechaAsignacion = if (cambioResponsable && responsableId != null) ahora else ticket.fechaAsignacion,
            fechaSolucion = if (estado == Estado.SOLUCIONADO && ticket.estado != Estado.SOLUCIONADO) ahora else ticket.fechaSolucion,
            fechaCierre = if (estado == Estado.CERRADO) ahora else ticket.fechaCierre
        )

        val nombreResponsable = if (cambioResponsable && responsableId != null) {
            db.usuarioDao().buscarPorId(responsableId)?.nombre
        } else null

        db.withTransaction {
            dao.actualizarTicket(actualizado)
            if (estado != ticket.estado) {
                dao.insertarHistorial(
                    HistorialEstado(ticketId = ticketId, estado = estado, usuarioId = actorId, fecha = ahora)
                )
            }
            if (nombreResponsable != null) {
                dao.insertarComentario(
                    Comentario(ticketId = ticketId, autorId = actorId, texto = "Ticket asignado a $nombreResponsable.", fecha = ahora)
                )
            }
        }
        return null
    }

    suspend fun cambiarEstado(ticketId: Int, actorId: Int, nuevoEstado: Estado) {
        val ticket = dao.buscarTicket(ticketId) ?: return
        val ahora = System.currentTimeMillis()
        db.withTransaction {
            dao.actualizarTicket(ticket.copy(estado = nuevoEstado))
            dao.insertarHistorial(
                HistorialEstado(ticketId = ticketId, estado = nuevoEstado, usuarioId = actorId, fecha = ahora)
            )
        }
    }

    suspend fun marcarSolucionado(ticketId: Int, actorId: Int, acciones: String, rutasFotos: List<String>) {
        val ticket = dao.buscarTicket(ticketId) ?: return
        val ahora = System.currentTimeMillis()
        db.withTransaction {
            dao.actualizarTicket(ticket.copy(estado = Estado.SOLUCIONADO, fechaSolucion = ahora))
            dao.insertarHistorial(
                HistorialEstado(ticketId = ticketId, estado = Estado.SOLUCIONADO, usuarioId = actorId, fecha = ahora)
            )
            dao.insertarComentario(
                Comentario(ticketId = ticketId, autorId = actorId, texto = "Acciones realizadas: $acciones", fecha = ahora)
            )
            rutasFotos.forEach { ruta ->
                dao.insertarEvidencia(
                    Evidencia(ticketId = ticketId, ruta = ruta, tipo = TipoEvidencia.SOLUCION, usuarioId = actorId, fecha = ahora)
                )
            }
        }
    }

    suspend fun solicitarMateriales(ticketId: Int, actorId: Int, texto: String) {
        val ticket = dao.buscarTicket(ticketId) ?: return
        val ahora = System.currentTimeMillis()
        db.withTransaction {
            dao.actualizarTicket(ticket.copy(materiales = texto))
            dao.insertarComentario(
                Comentario(ticketId = ticketId, autorId = actorId, texto = "Requiere materiales o información: $texto", fecha = ahora)
            )
        }
    }

    suspend fun agregarComentario(ticketId: Int, autorId: Int, texto: String) {
        dao.insertarComentario(
            Comentario(ticketId = ticketId, autorId = autorId, texto = texto, fecha = System.currentTimeMillis())
        )
    }
}
