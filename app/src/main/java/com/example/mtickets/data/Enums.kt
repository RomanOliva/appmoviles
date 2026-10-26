package com.example.mtickets.data

enum class Rol(val etiqueta: String) {
    SOLICITANTE("Solicitante"),
    ADMINISTRADOR("Administrador (TI)"),
    RESPONSABLE("Responsable de la solución")
}

enum class Estado(val etiqueta: String) {
    PENDIENTE("Pendiente"),
    REVISADO("Revisado"),
    ASIGNADO("Asignado"),
    EN_PROCESO("En proceso"),
    SOLUCIONADO("Solucionado"),
    CERRADO("Cerrado"),
    CANCELADO("Cancelado")
}

enum class Prioridad(val etiqueta: String) {
    BAJA("Baja"),
    MEDIA("Media"),
    ALTA("Alta")
}

enum class TipoEvidencia {
    PROBLEMA,
    SOLUCION
}
