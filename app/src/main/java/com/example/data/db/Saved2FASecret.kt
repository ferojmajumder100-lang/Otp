package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_2fa_secrets")
data class Saved2FASecret(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String,
    val secret: String,
    val timestamp: Long = System.currentTimeMillis()
)
