package com.example.mtickets.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

private const val SELECT_TICKET = """
    SELECT t.id AS id, t.codigo AS codigo, t.titulo AS titulo, t.descripcion AS descripcion,
           t.categoriaId AS categoriaId, c.nombre AS categoria,
           t.ubicacionId AS ubicacionId, l.nombre AS ubicacion,
           t.detalleUbicacion AS detalleUbicacion,
           t.solicitanteId AS solicitanteId, s.nombre AS solicitante, s.cargo AS cargoSolicitante,
           t.prioridad AS prioridad, t.estado AS estado,
           t.responsableId AS responsableId, r.nombre AS responsable,
           t.materiales AS materiales,
           t.fechaCreacion AS fechaCreacion, t.fechaAsignacion AS fechaAsignacion,
           t.fechaSolucion AS fechaSolucion, t.fechaCierre AS fechaCierre
    FROM tickets t
    INNER JOIN categorias c ON c.id = t.categoriaId
    INNER JOIN ubicaciones l ON l.id = t.ubicacionId
    INNER JOIN usuarios s ON s.id = t.solicitanteId
    LEFT JOIN usuarios r ON r.id = t.responsableId
"""

@Dao
interface UsuarioDAO {

    @Insert
    suspend fun insertar(usuario: Usuario): Long

    // Devuelve el usuario si el correo y la contraseña coinciden (o null).
    @Query("SELECT * FROM usuarios WHERE LOWER(correo) = LOWER(:correo) AND password = :password LIMIT 1")
    suspend fun login(correo: String, password: String): Usuario?

    @Query("SELECT * FROM usuarios WHERE id = :id")
    suspend fun buscarPorId(id: Int): Usuario?

    // Quienes pueden resolver tickets: encargado de TI y personal auxiliar.
    @Query("SELECT * FROM usuarios WHERE rol IN ('ADMINISTRADOR', 'RESPONSABLE') ORDER BY nombre")
    suspend fun listarResponsables(): List<Usuario>

    @Query("SELECT COUNT(*) FROM usuarios")
    suspend fun contar(): Int
}

@Dao
interface CatalogoDAO {

    @Insert
    suspend fun insertarCategoria(categoria: Categoria): Long

    @Insert
    suspend fun insertarUbicacion(ubicacion: Ubicacion): Long

    @Query("SELECT * FROM categorias ORDER BY nombre")
    suspend fun listarCategorias(): List<Categoria>

    @Query("SELECT * FROM ubicaciones ORDER BY nombre")
    suspend fun listarUbicaciones(): List<Ubicacion>
}

@Dao
interface TicketDAO {

    // ----- Tickets -----

    @Insert
    suspend fun insertarTicket(ticket: Ticket): Long

    @Update
    suspend fun actualizarTicket(ticket: Ticket)

    @Query("UPDATE tickets SET codigo = :codigo WHERE id = :id")
    suspend fun actualizarCodigo(id: Int, codigo: String)

    @Query("SELECT * FROM tickets WHERE id = :id")
    suspend fun buscarTicket(id: Int): Ticket?

    @Query(SELECT_TICKET + " WHERE t.id = :id")
    suspend fun buscarVista(id: Int): TicketVista?

    // Tickets creados por un solicitante.
    @Query(SELECT_TICKET + " WHERE t.solicitanteId = :usuarioId ORDER BY t.fechaCreacion DESC")
    suspend fun listarDeSolicitante(usuarioId: Int): List<TicketVista>

    // Solo los tickets asignados a un responsable.
    @Query(SELECT_TICKET + " WHERE t.responsableId = :usuarioId ORDER BY t.fechaCreacion DESC")
    suspend fun listarAsignados(usuarioId: Int): List<TicketVista>

    // Bandeja del administrador: todos los tickets, con filtros opcionales (null = sin filtro).
    @Query(
        SELECT_TICKET + """
        WHERE (:estado IS NULL OR t.estado = :estado)
          AND (:prioridad IS NULL OR t.prioridad = :prioridad)
          AND (:categoriaId IS NULL OR t.categoriaId = :categoriaId)
          AND (:ubicacionId IS NULL OR t.ubicacionId = :ubicacionId)
          AND (:responsableId IS NULL OR t.responsableId = :responsableId)
          AND (:desde IS NULL OR t.fechaCreacion >= :desde)
        ORDER BY t.fechaCreacion DESC
        """
    )
    suspend fun listarTodos(
        estado: String?,
        prioridad: String?,
        categoriaId: Int?,
        ubicacionId: Int?,
        responsableId: Int?,
        desde: Long?
    ): List<TicketVista>

    @Query("SELECT COUNT(*) FROM tickets")
    suspend fun contarTickets(): Int

    // ----- Comentarios -----

    @Insert
    suspend fun insertarComentario(comentario: Comentario)

    @Query(
        """
        SELECT c.id AS id, u.nombre AS autor, u.rol AS rol, c.texto AS texto, c.fecha AS fecha
        FROM comentarios c
        INNER JOIN usuarios u ON u.id = c.autorId
        WHERE c.ticketId = :ticketId
        ORDER BY c.fecha ASC, c.id ASC
        """
    )
    suspend fun listarComentarios(ticketId: Int): List<ComentarioVista>

    // ----- Historial de estados -----

    @Insert
    suspend fun insertarHistorial(historial: HistorialEstado)

    @Query(
        """
        SELECT h.estado AS estado, u.nombre AS usuario, h.fecha AS fecha
        FROM historial_estados h
        INNER JOIN usuarios u ON u.id = h.usuarioId
        WHERE h.ticketId = :ticketId
        ORDER BY h.fecha ASC, h.id ASC
        """
    )
    suspend fun listarHistorial(ticketId: Int): List<HistorialVista>

    // ----- Evidencias (fotografías) -----

    @Insert
    suspend fun insertarEvidencia(evidencia: Evidencia)

    @Query("SELECT * FROM evidencias WHERE ticketId = :ticketId ORDER BY fecha ASC, id ASC")
    suspend fun listarEvidencias(ticketId: Int): List<Evidencia>
}
