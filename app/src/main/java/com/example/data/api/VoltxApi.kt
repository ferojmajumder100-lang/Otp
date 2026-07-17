package com.example.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface VoltxApi {
    @GET("liveaccess")
    suspend fun getLiveAccess(): LiveAccessResponse

    @POST("getnum")
    suspend fun getNumber(@Body payload: GetNumPayload): GetNumResponse

    @GET("success-otp")
    suspend fun getSuccessOtp(): SuccessOtpResponse
}
