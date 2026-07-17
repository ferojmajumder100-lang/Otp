package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "facebook_accounts")
data class FacebookAccount(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val phone: String,
    val uid: String,
    val name: String,
    val password: String,
    val cookies: String,
    val timestamp: Long = System.currentTimeMillis()
)
