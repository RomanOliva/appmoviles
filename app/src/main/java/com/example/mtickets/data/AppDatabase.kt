package com.example.mtickets.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Usuario::class,
        Categoria::class,
        Ubicacion::class,
        Ticket::class,
        Comentario::class,
        HistorialEstado::class,
        Evidencia::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDAO
    abstract fun catalogoDao(): CatalogoDAO
    abstract fun ticketDao(): TicketDAO

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mtickets.db"
                ).build().also { instancia = it }
            }
        }
    }
}
