package org.example.project.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room Database for TyreGuard application.
 * 
 * Stores tyre analysis results persistently.
 */
@Database(
    entities = [TyreAnalysisResult::class],
    version = 1,
    exportSchema = false
)
abstract class TyreGuardDatabase : RoomDatabase() {
    
    abstract fun tyreAnalysisDao(): TyreAnalysisDao
    
    companion object {
        private const val DATABASE_NAME = "tyreguard_database"
        
        @Volatile
        private var INSTANCE: TyreGuardDatabase? = null
        
        /**
         * Get the singleton database instance.
         */
        fun getInstance(context: Context): TyreGuardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TyreGuardDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
