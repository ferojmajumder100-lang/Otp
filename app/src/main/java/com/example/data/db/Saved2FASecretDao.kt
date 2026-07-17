package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface Saved2FASecretDao {
    @Query("SELECT * FROM saved_2fa_secrets ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<Saved2FASecret>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(secret: Saved2FASecret)

    @Delete
    suspend fun delete(secret: Saved2FASecret)

    @Query("DELETE FROM saved_2fa_secrets WHERE id = :id")
    suspend fun deleteById(id: Int)
}
