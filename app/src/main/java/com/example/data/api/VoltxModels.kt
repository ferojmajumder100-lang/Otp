package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VoltxMeta(
    @Json(name = "code") val code: Int
)

@JsonClass(generateAdapter = true)
data class LiveService(
    @Json(name = "sid") val sid: String,
    @Json(name = "ranges") val ranges: List<String>?
)

@JsonClass(generateAdapter = true)
data class LiveAccessData(
    @Json(name = "services") val services: List<LiveService>?
)

@JsonClass(generateAdapter = true)
data class LiveAccessResponse(
    @Json(name = "meta") val meta: VoltxMeta,
    @Json(name = "data") val data: LiveAccessData?
)

@JsonClass(generateAdapter = true)
data class GetNumPayload(
    @Json(name = "rid") val rid: String
)

@JsonClass(generateAdapter = true)
data class GetNumData(
    @Json(name = "full_number") val fullNumber: String?,
    @Json(name = "no_plus_number") val noPlusNumber: String?
)

@JsonClass(generateAdapter = true)
data class GetNumResponse(
    @Json(name = "meta") val meta: VoltxMeta,
    @Json(name = "data") val data: GetNumData?
)

@JsonClass(generateAdapter = true)
data class OtpItem(
    @Json(name = "number") val number: String,
    @Json(name = "message") val message: String
)

@JsonClass(generateAdapter = true)
data class SuccessOtpData(
    @Json(name = "otps") val otps: List<OtpItem>?
)

@JsonClass(generateAdapter = true)
data class SuccessOtpResponse(
    @Json(name = "meta") val meta: VoltxMeta,
    @Json(name = "data") val data: SuccessOtpData?
)
