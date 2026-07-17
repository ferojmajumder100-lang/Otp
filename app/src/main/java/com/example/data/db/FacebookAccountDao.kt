package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FacebookAccountDao {
    @Query("SELECT * FROM facebook_accounts ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<FacebookAccount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: FacebookAccount)

    @Delete
    suspend fun delete(account: FacebookAccount)

    @Query("DELETE FROM facebook_accounts WHERE id = :id")
    suspend fun deleteById(id: Int)
}
