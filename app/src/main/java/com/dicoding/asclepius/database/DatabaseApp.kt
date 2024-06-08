package com.dicoding.asclepius.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [HistoryPrediction::class], version = 2, exportSchema = false)
abstract class DatabaseApp : RoomDatabase(){
    abstract fun historyPredictionDao(): HistoryPredictionDao
    companion object {
        @Volatile
        private var INSTANCE: DatabaseApp? = null
        fun getDatabase(context: Context): DatabaseApp {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DatabaseApp::class.java,
                    "app_database"
                )
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }

}