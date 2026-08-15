package cu.christianrvdv.sumador.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SavedSumEntity::class, CustomDenominationEntity::class],
    version = 3, // Incrementada a 3 para añadir columna isCoin
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savedSumDao(): SavedSumDao
    abstract fun customDenominationDao(): CustomDenominationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migración de la versión 2 a la 3
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Añadir columna isCoin con valor por defecto 0 (false)
                database.execSQL("ALTER TABLE custom_denominations ADD COLUMN isCoin INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sumador_database"
                )
                    .addMigrations(MIGRATION_2_3) // Añadir la migración
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}