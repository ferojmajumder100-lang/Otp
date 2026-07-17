package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "active_numbers")
data class ActiveNumber(
    @PrimaryKey val phone: String,
    val service: String,
    val rangeCode: String,
    val timestamp: Long = System.currentTimeMillis(),
    val otp: String? = null,
    val fullMessage: String? = null,
    val status: String = "ACTIVE" // "ACTIVE", "COMPLETED", "EXPIRED"
)
