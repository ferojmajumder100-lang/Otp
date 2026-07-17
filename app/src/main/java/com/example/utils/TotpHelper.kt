package com.example.utils

import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.and

object TotpHelper {
    fun decodeBase32(base32: String): ByteArray {
        val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val cleanSecret = base32.uppercase().replace(" ", "").replace("-", "")
        if (cleanSecret.isEmpty()) return ByteArray(0)
        
        var l = 0
        var valBuffer = 0
        val outBytes = mutableListOf<Byte>()
        
        for (char in cleanSecret) {
            val index = allowedChars.indexOf(char)
            if (index == -1) continue // Skip non-base32 chars
            
            valBuffer = (valBuffer shl 5) or index
            l += 5
            if (l >= 8) {
                outBytes.add(((valBuffer shr (l - 8)) and 0xFF).toByte())
                l -= 8
            }
        }
        return outBytes.toByteArray()
    }

    fun generateTOTP(secretBase32: String, timeStep: Long = 30L): String {
        try {
            val key = decodeBase32(secretBase32)
            if (key.isEmpty()) return "000000"
            val currentTime = System.currentTimeMillis() / 1000L
            val counter = currentTime / timeStep
            
            val buffer = ByteBuffer.allocate(8).putLong(counter).array()
            val mac = Mac.getInstance("HmacSHA1")
            mac.init(SecretKeySpec(key, "HmacSHA1"))
            val hash = mac.doFinal(buffer)
            
            val offset = (hash.last() and 0x0F).toInt()
            val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                    ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                    ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                    (hash[offset + 3].toInt() and 0xFF)
            
            val otp = binary % 1000000
            return String.format("%06d", otp)
        } catch (e: Exception) {
            e.printStackTrace()
            return "000000"
        }
    }
}
