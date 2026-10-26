package com.example.mtickets.data

import androidx.room.withTransaction

object DatosPrueba {

    const val PASSWORD = "123456"

    val usuariosDemo = listOf(
        Usuario(nombre = "Carlos Rojas", cargo = "Encargado de Tecnología de la Información", correo = "ti@mpaz.test", password = PASSWORD, rol = Rol.ADMINISTRADOR),
        Usuario(nombre = "Luis Pérez", cargo = "Auxiliar de mantención", correo = "luis@mpaz.test", password = PASSWORD, rol = Rol.RESPONSABLE),
        Usuario(nombre = "Marta Soto", cargo = "Auxiliar de mantención", correo = "marta@mpaz.test", password = PASSWORD, rol = Rol.RESPONSABLE),
        Usuario(nombre = "Andrea Vidal", cargo = "Docente de Lenguaje", correo = "andrea@mpaz.test", password = PASSWORD, rol = Rol.SOLICITANTE),
        Usuario(nombre = "Jorge Núñez", cargo = "Docente de Matemática", correo = "jorge@mpaz.test", password = PASSWORD, rol = Rol.SOLICITANTE),
        Usuario(nombre = "Paula Araya", cargo = "Secretaria", correo = "paula@mpaz.test", password = PASSWORD, rol = Rol.SOLICITANTE)
    )

    private val categorias = listOf("Infraestructura", "Tecnología", "Mobiliario", "Otra")

    private val ubicaciones = listOf(
        "Sala de clases 1° básico", "Sala de clases 3° básico", "Sala de clases 6° básico",
        "Oficina de dirección", "Sala de profesores", "Biblioteca", "Laboratorio de computación",
        "Sala sensorial", "CIEMP", "Sala de robótica", "Patio", "Baños", "Huerto", "Otra ubicación (ver detalle)"
    )

    suspend fun sembrarSiVacio(db: AppDatabase) {
        if (db.usuarioDao().contar() > 0) return

        db.withTransaction {
            val usuarioDao = db.usuarioDao()
            val catalogoDao = db.catalogoDao()
            val ticketDao = db.ticketDao()

            categorias.forEach { catalogoDao.insertarCategoria(Categoria(nombre = it)) }
            ubicaciones.forEach { catalogoDao.insertarUbicacion(Ubicacion(nombre = it)) }

            usuariosDemo.forEach { usuarioDao.insertar(it) }
            val ti = 1
            val luis = 2
            val marta = 3
            val andrea = 4
            val jorge = 5
            val paula = 6

            val dia = 24L * 60 * 60 * 1000
            val ahora = System.currentTimeMillis()

            suspend fun crear(
                titulo: String, descripcion: String, categoria: Int, ubicacion: Int, detalle: String,
                solicitante: Int, hace: Long, prioridad: Prioridad?, estado: Estado, responsable: Int?
            ): Int {
                val fecha = ahora - hace * dia / 2
                val id = ticketDao.insertarTicket(
                    Ticket(
                        titulo = titulo, descripcion = descripcion, categoriaId = categoria,
                        ubicacionId = ubicacion, detalleUbicacion = detalle, solicitanteId = solicitante,
                        prioridad = prioridad, estado = Estado.PENDIENTE, fechaCreacion = fecha
                    )
                ).toInt()
                ticketDao.actualizarCodigo(id, "MP-" + id.toString().padStart(4, '0'))
                ticketDao.insertarHistorial(HistorialEstado(ticketId = id, estado = Estado.PENDIENTE, usuarioId = solicitante, fecha = fecha))

                val pasos = listOf(Estado.REVISADO, Estado.ASIGNADO, Estado.EN_PROCESO, Estado.SOLUCIONADO, Estado.CERRADO)
                var actual = ticketDao.buscarTicket(id)!!
                var tiempo = fecha
                if (estado != Estado.PENDIENTE) {
                    for (paso in pasos) {
                        tiempo += dia / 8
                        val quienCambia = if (paso == Estado.EN_PROCESO || paso == Estado.SOLUCIONADO) (responsable ?: ti) else ti
                        actual = actual.copy(
                            estado = paso,
                            responsableId = if (paso.ordinal >= Estado.ASIGNADO.ordinal) responsable else null,
                            fechaAsignacion = if (paso == Estado.ASIGNADO) tiempo else actual.fechaAsignacion,
                            fechaSolucion = if (paso == Estado.SOLUCIONADO) tiempo else actual.fechaSolucion,
                            fechaCierre = if (paso == Estado.CERRADO) tiempo else actual.fechaCierre
                        )
                        ticketDao.insertarHistorial(HistorialEstado(ticketId = id, estado = paso, usuarioId = quienCambia, fecha = tiempo))
                        if (paso == estado) break
                    }
                }
                ticketDao.actualizarTicket(actual.copy(prioridad = prioridad))
                return id
            }

            val t1 = crear("Proyector no enciende", "El proyector de la sala no enciende ni muestra luz indicadora. Se probó con otro cable.",
                2, 2, "Techo, cerca de la pizarra", andrea, 2, null, Estado.PENDIENTE, null)

            crear("Silla de estudiante quebrada", "Una silla tiene la pata suelta y es peligrosa para el estudiante.",
                3, 1, "Fila del fondo", jorge, 4, Prioridad.BAJA, Estado.REVISADO, null)

            val t3 = crear("Filtración de agua en el techo", "Cuando llueve cae agua cerca de los estantes de libros.",
                1, 6, "Sector de cuentos", paula, 6, Prioridad.ALTA, Estado.ASIGNADO, luis)

            val t4 = crear("Sin internet en sala de profesores", "El notebook no se conecta a la red desde ayer en la mañana.",
                2, 5, "", andrea, 8, Prioridad.MEDIA, Estado.EN_PROCESO, ti)

            val t5 = crear("Llave de baño gotea", "La llave del último lavamanos no cierra bien y gotea constantemente.",
                1, 12, "Baño de varones, primer piso", jorge, 12, Prioridad.MEDIA, Estado.SOLUCIONADO, marta)

            val t6 = crear("Impresora no imprime", "La impresora de secretaría muestra error de atasco, pero no hay papel atascado.",
                2, 4, "", paula, 20, Prioridad.ALTA, Estado.CERRADO, ti)

            fun hace(horas: Long) = ahora - horas * 60 * 60 * 1000

            suspend fun comentar(ticket: Int, autor: Int, texto: String, horas: Long) =
                ticketDao.insertarComentario(Comentario(ticketId = ticket, autorId = autor, texto = texto, fecha = hace(horas)))

            comentar(t1, andrea, "Es urgente, mañana tengo una clase con presentación.", 20)
            comentar(t3, ti, "Se asigna a Luis. Poner un balde y proteger los libros mientras tanto.", 70)
            comentar(t3, luis, "Voy a revisar el techo mañana temprano.", 60)
            comentar(t4, ti, "Se revisa el router del segundo piso.", 30)
            comentar(t4, ti, "Requiere materiales o información: cable de red nuevo.", 28)
            comentar(t5, marta, "Acciones realizadas: se cambió el empaque de la llave y se probó el cierre.", 90)
            comentar(t6, ti, "Acciones realizadas: se retiró un trozo de papel del rodillo y se reinició el equipo.", 200)
            comentar(t6, paula, "Funciona perfecto, gracias.", 190)
        }
    }
}
