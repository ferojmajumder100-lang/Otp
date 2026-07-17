package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ActiveNumberDao {
    @Query("SELECT * FROM active_numbers ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<ActiveNumber>>

    @Query("SELECT * FROM active_numbers WHERE status = 'ACTIVE' ORDER BY timestamp DESC")
    fun getActiveFlow(): Flow<List<ActiveNumber>>

    @Query("SELECT * FROM active_numbers WHERE status = 'ACTIVE'")
    suspend fun getActiveSync(): List<ActiveNumber>

    @Query("SELECT * FROM active_numbers")
    suspend fun getAllSync(): List<ActiveNumber>

    @Query("SELECT * FROM active_numbers WHERE phone = :phone LIMIT 1")
    suspend fun getByPhone(phone: String): ActiveNumber?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(activeNumber: ActiveNumber)

    @Update
    suspend fun update(activeNumber: ActiveNumber)

    @Delete
    suspend fun delete(activeNumber: ActiveNumber)

    @Query("DELETE FROM active_numbers WHERE phone = :phone")
    suspend fun deleteByPhone(phone: String)

    @Query("DELETE FROM active_numbers")
    suspend fun deleteAll()
}
