package com.dicoding.asclepius.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface HistoryPredictionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun predictionInsert(prediction: HistoryPrediction)

    @Query("SELECT * FROM history_prediction")
    suspend fun getAllPrediction(): List<HistoryPrediction>

    @Delete
    suspend fun predictionDelete(prediction: HistoryPrediction)
}