# MTickets
App móvil para Android donde se pueden crear y gestionar tickets de soporte dentro de un colegio. Si algo se echó a perder (un proyector, una silla, una luz, el computador de la biblioteca), alguien crea un ticket, se revisa y asigna a la persona que lo vaya a supervisar

# Integrantes
Roman Oliva
Gerardo Silva

# ¿Qué hace la app?

Hay tres tipos de usuario y cada uno ve cosas distintas:

Solicitante: crea tickets y ve el estado de los suyos
Administrador: ve todos los tickets, los revisa, define prioridad y asigna un responsable
Responsable de la solución: ve solo los tickets que le asignaron, los pone en proceso y los marca como solucionados

# Funcionalidades implementadas
- Login con validación
- Lista de tickets según rol 
- Filtros por estado, prioridad, categoría, ubicación, responsable y período
- Crear ticket con título, descripción, categoría, ubicación y detalle del lugar
- Detalle del ticket
- Estados que cambian al interactuar: Pendiente, Revisado, Asignado, En proceso, Solucionado, Cerrado y Cancelado.
- Espacios de escritura para pedir materiales
- Funcionalidad de camara para las pruebas
- Persistencia local : los datos se mantienen aunque cierres la app.
- Datos de prueba


# Como iniciarlo

Abre la carpeta del proyecto con Android Studio
Espera a que Gradle termine de sincronizar
Elige un emulador o conecta tu celular
Dale al botón Run

# Usuarios de prueba

Los usuarios de prueba falsos, los cuales comparten una contraseña de prueba: 123456

Administrador (TI)	ti@mpaz.test

Responsable	luis@mpaz.test

Responsable	marta@mpaz.test

Solicitante	andrea@mpaz.test

Solicitante	jorge@mpaz.test

Solicitante	paula@mpaz.test


