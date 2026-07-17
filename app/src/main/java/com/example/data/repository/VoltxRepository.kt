package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.api.GetNumPayload
import com.example.data.api.LiveService
import com.example.data.api.OtpItem
import com.example.data.api.VoltxApi
import com.example.data.db.ActiveNumber
import com.example.data.db.ActiveNumberDao
import com.example.data.db.AppDatabase
import com.example.data.db.Saved2FASecret
import com.example.data.db.Saved2FASecretDao
import com.example.data.db.FacebookAccount
import com.example.data.db.FacebookAccountDao
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class VoltxRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val activeNumberDao: ActiveNumberDao = db.activeNumberDao()
    private val saved2faSecretDao: Saved2FASecretDao = db.saved2faSecretDao()
    private val facebookAccountDao: FacebookAccountDao = db.facebookAccountDao()

    // Expose flows for UI observation
    val allActiveNumbers: Flow<List<ActiveNumber>> = activeNumberDao.getAllFlow()
    val onlyActiveNumbers: Flow<List<ActiveNumber>> = activeNumberDao.getActiveFlow()
    val savedSecrets: Flow<List<Saved2FASecret>> = saved2faSecretDao.getAllFlow()
    val allFacebookAccounts: Flow<List<FacebookAccount>> = facebookAccountDao.getAllFlow()

    private val api: VoltxApi

    init {
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Trust manager to bypass potential SSL errors as the API server might be self-signed (verify=False in Python)
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("SSL").apply {
            init(null, trustAllCerts, SecureRandom())
        }

        val okHttpClient = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val authKey = android.util.Base64.decode("TVgxUk45WktJSFk=", android.util.Base64.DEFAULT).decodeToString()
                val request = chain.request().newBuilder()
                    .header("mauthapi", authKey)
                    .header("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)
            .build()

        val decodedBaseUrl = android.util.Base64.decode(
            "aHR0cHM6Ly9hcGkuMm9vOS5jbG91ZC9NWFM0N0ZMRlgwVS90bmV2cy9AcHVibGljL2FwaS8=",
            android.util.Base64.DEFAULT
        ).decodeToString()

        val retrofit = Retrofit.Builder()
            .baseUrl(decodedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        api = retrofit.create(VoltxApi::class.java)
    }

    // --- API Network Actions ---

    suspend fun fetchLiveServices(): Result<List<LiveService>> {
        return try {
            val response = api.getLiveAccess()
            if (response.meta.code == 200) {
                val services = response.data?.services ?: emptyList()
                Result.success(services)
            } else {
                Result.failure(Exception("API Error Code: ${response.meta.code}"))
            }
        } catch (e: Exception) {
            Log.e("VoltxRepository", "Error fetching live services", e)
            Result.failure(e)
        }
    }

    suspend fun purchaseNumber(rangeCode: String): Result<String> {
        return try {
            // Remove 'XXX' and 'X' to format standard rid
            val rid = rangeCode.replace("XXX", "").replace("X", "").trim()
            val finalRid = if (rid.isEmpty()) "8801" else rid
            
            val response = api.getNumber(GetNumPayload(rid = finalRid))
            if (response.meta.code == 200) {
                val fullNumber = response.data?.fullNumber ?: response.data?.noPlusNumber
                if (!fullNumber.isNullOrBlank()) {
                    val cleanedPhone = fullNumber.replace("+", "").trim()
                    Result.success(cleanedPhone)
                } else {
                    Result.failure(Exception("No number found in API response"))
                }
            } else {
                Result.failure(Exception("API Error: Code ${response.meta.code}"))
            }
        } catch (e: Exception) {
            Log.e("VoltxRepository", "Error purchasing number", e)
            Result.failure(e)
        }
    }

    suspend fun checkOtps(): Result<List<OtpItem>> {
        return try {
            val response = api.getSuccessOtp()
            if (response.meta.code == 200) {
                val otps = response.data?.otps ?: emptyList()
                Result.success(otps)
            } else {
                Result.failure(Exception("API Error: Code ${response.meta.code}"))
            }
        } catch (e: Exception) {
            Log.e("VoltxRepository", "Error checking OTPs", e)
            Result.failure(e)
        }
    }

    // --- Database Operations ---

    suspend fun addActiveNumber(phone: String, service: String, rangeCode: String) {
        val item = ActiveNumber(
            phone = phone,
            service = service,
            rangeCode = rangeCode,
            status = "ACTIVE"
        )
        activeNumberDao.insert(item)
    }

    suspend fun updateActiveNumber(activeNumber: ActiveNumber) {
        activeNumberDao.update(activeNumber)
    }

    suspend fun deleteActiveNumber(phone: String) {
        activeNumberDao.deleteByPhone(phone)
    }

    suspend fun getActiveNumbersSync(): List<ActiveNumber> {
        return activeNumberDao.getActiveSync()
    }

    suspend fun getAllNumbersSync(): List<ActiveNumber> {
        return activeNumberDao.getAllSync()
    }

    suspend fun saveSecret(label: String, secret: String) {
        saved2faSecretDao.insert(Saved2FASecret(label = label, secret = secret))
    }

    suspend fun deleteSecret(id: Int) {
        saved2faSecretDao.deleteById(id)
    }

    suspend fun clearAllNumbers() {
        activeNumberDao.deleteAll()
    }

    suspend fun saveFacebookAccount(account: FacebookAccount) {
        facebookAccountDao.insert(account)
    }

    suspend fun deleteFacebookAccount(account: FacebookAccount) {
        facebookAccountDao.delete(account)
    }

    suspend fun deleteFacebookAccountById(id: Int) {
        facebookAccountDao.deleteById(id)
    }
}
