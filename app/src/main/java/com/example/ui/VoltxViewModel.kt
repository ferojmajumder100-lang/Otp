package com.example.ui

import android.app.Application
import android.content.Context
import android.media.RingtoneManager
import android.os.Vibrator
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.LiveService
import com.example.data.db.ActiveNumber
import com.example.data.db.Saved2FASecret
import com.example.data.db.FacebookAccount
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.FormBody
import java.util.concurrent.TimeUnit
import com.example.data.repository.VoltxRepository
import com.example.utils.TotpHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ServicesUiState {
    object Loading : ServicesUiState
    data class Success(val services: List<LiveService>) : ServicesUiState
    data class Error(val message: String) : ServicesUiState
}

sealed interface FbCreationState {
    object Idle : FbCreationState
    data class Purchasing(val range: String) : FbCreationState
    data class Registering(val phone: String) : FbCreationState
    data class Success(val name: String, val uid: String, val phone: String, val cookies: String) : FbCreationState
    data class Error(val message: String) : FbCreationState
}

class VoltxViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = VoltxRepository(application)

    // UI States
    private val _servicesState = MutableStateFlow<ServicesUiState>(ServicesUiState.Loading)
    val servicesState: StateFlow<ServicesUiState> = combine(_servicesState, repository.allActiveNumbers) { rawState, numbers ->
        if (rawState is ServicesUiState.Success) {
            val instagramRanges = numbers.filter {
                it.service.lowercase() == "instagram" ||
                it.fullMessage?.lowercase()?.contains("instagram") == true
            }.map { it.rangeCode }.toSet()

            val servicesList = rawState.services.map { service ->
                if (service.sid.lowercase().contains("facebook")) {
                    val fbRanges = service.ranges ?: emptyList()
                    val remainingFbRanges = fbRanges.filter { !instagramRanges.contains(it) }
                    service.copy(ranges = remainingFbRanges)
                } else {
                    service
                }
            }

            val updatedServicesList = if (instagramRanges.isNotEmpty()) {
                val hasInstaInFetched = servicesList.any { it.sid.lowercase() == "instagram" }
                if (hasInstaInFetched) {
                    servicesList.map { service ->
                        if (service.sid.lowercase() == "instagram") {
                            val existingRanges = service.ranges ?: emptyList()
                            val mergedRanges = (existingRanges + instagramRanges).distinct()
                            service.copy(ranges = mergedRanges)
                        } else {
                            service
                        }
                    }
                } else {
                    servicesList + LiveService(sid = "instagram", ranges = instagramRanges.toList())
                }
            } else {
                servicesList
            }

            ServicesUiState.Success(updatedServicesList)
        } else {
            rawState
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ServicesUiState.Loading)

    private val _selectedService = MutableStateFlow<LiveService?>(null)
    val selectedService: StateFlow<LiveService?> = combine(_selectedService, servicesState) { selected, state ->
        if (selected != null && state is ServicesUiState.Success) {
            state.services.find { it.sid.lowercase() == selected.sid.lowercase() } ?: selected
        } else {
            selected
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedRange = MutableStateFlow<String?>(null)
    val selectedRange: StateFlow<String?> = _selectedRange.asStateFlow()

    private val _isPurchasing = MutableStateFlow(false)
    val isPurchasing: StateFlow<Boolean> = _isPurchasing.asStateFlow()

    private val _purchaseMessage = MutableStateFlow<String?>(null)
    val purchaseMessage: StateFlow<String?> = _purchaseMessage.asStateFlow()

    // Database Flows
    val activeNumbers: StateFlow<List<ActiveNumber>> = repository.allActiveNumbers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedSecrets: StateFlow<List<Saved2FASecret>> = repository.savedSecrets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val facebookAccounts: StateFlow<List<FacebookAccount>> = repository.allFacebookAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isCreatingFbAccount = MutableStateFlow(false)
    val isCreatingFbAccount: StateFlow<Boolean> = _isCreatingFbAccount.asStateFlow()

    private val _facebookCreationState = MutableStateFlow<FbCreationState>(FbCreationState.Idle)
    val facebookCreationState: StateFlow<FbCreationState> = _facebookCreationState.asStateFlow()

    private val _fbCreationMessage = MutableStateFlow<String?>(null)
    val fbCreationMessage: StateFlow<String?> = _fbCreationMessage.asStateFlow()

    private val _systemStatus = MutableStateFlow("LOCK")
    val systemStatus: StateFlow<String> = _systemStatus.asStateFlow()

    private val _systemMessage = MutableStateFlow<String?>(null)
    val systemMessage: StateFlow<String?> = _systemMessage.asStateFlow()

    private val _hiddenFbActiveNumbers = MutableStateFlow<Set<String>>(emptySet())
    val hiddenFbActiveNumbers: StateFlow<Set<String>> = _hiddenFbActiveNumbers.asStateFlow()

    fun hideActiveNumberFromFb(phone: String) {
        _hiddenFbActiveNumbers.value = _hiddenFbActiveNumbers.value + phone
    }

    fun clearFbCreationMessage() {
        _fbCreationMessage.value = null
    }

    fun clearFbCreationState() {
        _facebookCreationState.value = FbCreationState.Idle
    }

    fun deleteFacebookAccount(account: FacebookAccount) {
        viewModelScope.launch {
            repository.deleteFacebookAccount(account)
        }
    }

    fun buyAndCreateFacebookAccount(range: String, password: String) {
        if (_systemStatus.value != "ON") {
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _facebookCreationState.value = FbCreationState.Purchasing(range)
            _fbCreationMessage.value = "Purchasing number from range: $range..."
            
            repository.purchaseNumber(range)
                .onSuccess { phone ->
                    val cleanedPhone = phone.replace(Regex("[^0-9]"), "")
                    repository.addActiveNumber(
                        phone = cleanedPhone,
                        service = "facebook",
                        rangeCode = range
                    )
                    
                    _facebookCreationState.value = FbCreationState.Registering(cleanedPhone)
                    _fbCreationMessage.value = "Registering Facebook account on +$cleanedPhone..."
                    
                    try {
                        val frenchNames = listOf(
                            Pair("Jean", "Dupont"), Pair("Marie", "Martin"),
                            Pair("Pierre", "Durand"), Pair("Sophie", "Lefèvre"),
                            Pair("Lucas", "Moreau"), Pair("Emma", "Petit"),
                            Pair("Louis", "Roux"), Pair("Chloé", "Richard"),
                            Pair("Hugo", "Simon"), Pair("Inès", "Laurent")
                        )
                        val randomPair = frenchNames.random()
                        val fname = randomPair.first
                        val lname = randomPair.second
                        val day = (1..28).random()
                        val month = (1..12).random()
                        val year = (1980..2005).random()
                        
                        val regImpressionId = java.util.UUID.randomUUID().toString()
                        val loggerId = java.util.UUID.randomUUID().toString()
                        
                        val formBody = FormBody.Builder()
                            .add("ccp", "2")
                            .add("reg_instance", "3XA5at-YBOFaGHi2xPrg-wka")
                            .add("submission_request", "true")
                            .add("helper", "")
                            .add("reg_impression_id", regImpressionId)
                            .add("ns", "1")
                            .add("zero_header_af_client", "")
                            .add("app_id", "103")
                            .add("logger_id", loggerId)
                            .add("field_names[0]", "firstname")
                            .add("firstname", fname)
                            .add("lastname", lname)
                            .add("field_names[1]", "birthday_wrapper")
                            .add("birthday_day", day.toString())
                            .add("birthday_month", month.toString())
                            .add("birthday_year", year.toString())
                            .add("age_step_input", "")
                            .add("did_use_age", "false")
                            .add("field_names[2]", "reg_email__")
                            .add("reg_email__", cleanedPhone)
                            .add("field_names[3]", "sex")
                            .add("sex", "2")
                            .add("preferred_pronoun", "")
                            .add("custom_gender", "")
                            .add("reg_passwd__", password)
                            .add("name_suggest_elig", "false")
                            .add("was_shown_name_suggestions", "false")
                            .add("did_use_suggested_name", "false")
                            .add("use_custom_gender", "false")
                            .add("guid", "")
                            .add("pre_form_step", "")
                            .add("submit", "Sign up")
                            .add("fb_dtsg", "NAfx5UxG44eai86HC1iwiixBs1mUDFhn3ccN1fj3-SJJc64TeUsEAEg:0:0")
                            .add("jazoest", "24748")
                            .add("lsd", "AdRCh7SdER7Za5PotUuics5fFt0")
                            .add("__dyn", "1Z3pawlEnwm8_Bg9ppoW5UdE4a2i5U4e0C86u7E39x60zU3ex608ewk9E4W0pKq0FE6S0x81vohw73wGwcq1GwqU2YwbK0oi0zE1jU1soG0hi0Lo6-0Co1kU1UU3jwea")
                            .add("__csr", "")
                            .add("__hsdp", "")
                            .add("__hblp", "")
                            .add("__sjsp", "")
                            .add("__req", "g")
                            .add("__fmt", "1")
                            .add("__a", "AYzJ_41FhHOHmeaJtz_y-NZ41BrpCkk8MZbenM7ATpRLY9c4d3QLNQW9sph6SN5jNJBH5tH1yvE_P-EybRqM6tZ_nqLEaV4b3ZU")
                            .add("__user", "0")
                            .build()

                        val cookiesMap = mutableMapOf<String, String>()
                        val client = OkHttpClient.Builder()
                            .followRedirects(true)
                            .addNetworkInterceptor { chain ->
                                val response = chain.proceed(chain.request())
                                val setCookies = response.headers("Set-Cookie")
                                for (cookie in setCookies) {
                                    val parts = cookie.split(";")
                                    if (parts.isNotEmpty()) {
                                        val kv = parts[0].split("=", limit = 2)
                                        if (kv.size == 2) {
                                            cookiesMap[kv[0].trim()] = kv[1].trim()
                                        }
                                    }
                                }
                                response
                            }
                            .connectTimeout(30, TimeUnit.SECONDS)
                            .readTimeout(30, TimeUnit.SECONDS)
                            .build()

                        val androidUa = "Mozilla/5.0 (Linux; Android 12; itel S665L Build/SP1A.210812.016) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.91 Mobile Safari/537.36"
                        val regUrl = android.util.Base64.decode(
                            "aHR0cHM6Ly9saW1pdGVkLmZhY2Vib29rLmNvbS9yZWcvc3VibWl0Lz9wcml2YWN5X211dGF0aW9uX3Rva2VuPWV5SjBlWEJsSWpvd0xDSmpjbVZoZEdsdmJsOTBhVzFsSWpveE56Z3lNVFE1TXpZNExDSmpZV3hzYzJsMFpWOXBaQ0k2T1RBM09USTBOREF5T1RRNE1EVTRmUSUzRCUzRCZhcHBfaWQ9MTAzJm11bHRpX3N0ZXBfZm9ybT0xJnNraXBfc3VtYT0wJnNob3VsZEZvcmNlTVRvdWNoPTE=",
                            android.util.Base64.DEFAULT
                        ).decodeToString()
                        val refererUrl = android.util.Base64.decode(
                            "aHR0cHM6Ly9saW1pdGVkLmZhY2Vib29rLmNvbS9yZWcvP2lzX3R3b19zdGVwc19sb2dpbj0wJmNpZD0xMDMmcmVmc3JjPWRlcHJlY2F0ZWQmc29mdD1oams=",
                            android.util.Base64.DEFAULT
                        ).decodeToString()

                        val request = Request.Builder()
                            .url(regUrl)
                            .addHeader("User-Agent", androidUa)
                            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                            .addHeader("Accept-Language", "fr-FR,fr;q=0.9,en;q=0.8")
                            .addHeader("Accept-Encoding", "gzip, deflate, br, zstd")
                            .addHeader("Connection", "keep-alive")
                            .addHeader("Upgrade-Insecure-Requests", "1")
                            .addHeader("Content-Type", "application/x-www-form-urlencoded")
                            .addHeader("sec-ch-ua-platform", "\"Android\"")
                            .addHeader("sec-ch-ua", "\"Android WebView\";v=\"149\", \"Chromium\";v=\"149\", \"Not)A;Brand\";v=\"24\"")
                            .addHeader("x-response-format", "JSONStream")
                            .addHeader("sec-ch-ua-mobile", "?1")
                            .addHeader("x-asbd-id", "359341")
                            .addHeader("x-fb-lsd", "AdRCh7SdER7Za5PotUuics5fFt0")
                            .addHeader("x-requested-with", "XMLHttpRequest")
                            .addHeader("origin", "https://limited.facebook.com")
                            .addHeader("sec-fetch-site", "same-origin")
                            .addHeader("sec-fetch-mode", "cors")
                            .addHeader("sec-fetch-dest", "empty")
                            .addHeader("referer", refererUrl)
                            .addHeader("priority", "u=1, i")
                            .addHeader("Cookie", "datr=3XA5at-YBOFaGHi2xPrg-wka")
                            .post(formBody)
                            .build()

                        val startTime = System.currentTimeMillis()
                        val response = client.newCall(request).execute()
                        val elapsedTime = (System.currentTimeMillis() - startTime) / 1000.0
                        
                        if (response.isSuccessful) {
                            val uid = cookiesMap["c_user"]
                            if (!uid.isNullOrBlank()) {
                                val cookieKeys = listOf("datr", "sb", "ps_l", "ps_n", "m_pixel_ratio", "wd", "c_user", "fr", "xs")
                                val cookieParts = mutableListOf<String>()
                                for (key in cookieKeys) {
                                    val value = cookiesMap[key]
                                    if (value != null) {
                                        cookieParts.add("$key=${value.replace(" ", "")}")
                                    } else if (key == "datr") {
                                        cookieParts.add("datr=3XA5at-YBOFaGHi2xPrg-wka")
                                    }
                                }
                                val cookieString = cookieParts.joinToString("; ")
                                
                                val newAccount = FacebookAccount(
                                    phone = cleanedPhone,
                                    uid = uid,
                                    name = "$fname $lname",
                                    password = password,
                                    cookies = cookieString
                                )
                                repository.saveFacebookAccount(newAccount)
                                _facebookCreationState.value = FbCreationState.Success(
                                    name = "$fname $lname",
                                    uid = uid,
                                    phone = cleanedPhone,
                                    cookies = cookieString
                                )
                                _fbCreationMessage.value = "Successfully Created:\nName: $fname $lname\nUID: $uid\nPassword: $password"
                                triggerSoundAndVibration()
                            } else {
                                val err = "No c_user found in response cookies."
                                _facebookCreationState.value = FbCreationState.Error(err)
                                _fbCreationMessage.value = "Failed: $err"
                            }
                        } else {
                            val err = "HTTP Error ${response.code}"
                            _facebookCreationState.value = FbCreationState.Error(err)
                            _fbCreationMessage.value = "Failed: $err"
                        }
                    } catch (e: Exception) {
                        Log.e("VoltxViewModel", "Error registering FB account", e)
                        val err = e.message ?: "Unknown error"
                        _facebookCreationState.value = FbCreationState.Error(err)
                        _fbCreationMessage.value = "Failed Error: $err"
                    }
                }
                .onFailure { error ->
                    val err = "Failed to purchase number: ${error.localizedMessage}"
                    _facebookCreationState.value = FbCreationState.Error(err)
                    _fbCreationMessage.value = err
                }
        }
    }

    fun createFacebookAccount(phone: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isCreatingFbAccount.value = true
            _fbCreationMessage.value = "Creating FB account for $phone..."
            
            try {
                val cleanedPhone = phone.replace(Regex("[^0-9]"), "")
                val frenchNames = listOf(
                    Pair("Jean", "Dupont"), Pair("Marie", "Martin"),
                    Pair("Pierre", "Durand"), Pair("Sophie", "Lefèvre"),
                    Pair("Lucas", "Moreau"), Pair("Emma", "Petit"),
                    Pair("Louis", "Roux"), Pair("Chloé", "Richard"),
                    Pair("Hugo", "Simon"), Pair("Inès", "Laurent")
                )
                val randomPair = frenchNames.random()
                val fname = randomPair.first
                val lname = randomPair.second
                val day = (1..28).random()
                val month = (1..12).random()
                val year = (1980..2005).random()
                
                val regImpressionId = java.util.UUID.randomUUID().toString()
                val loggerId = java.util.UUID.randomUUID().toString()
                
                val formBody = FormBody.Builder()
                    .add("ccp", "2")
                    .add("reg_instance", "3XA5at-YBOFaGHi2xPrg-wka")
                    .add("submission_request", "true")
                    .add("helper", "")
                    .add("reg_impression_id", regImpressionId)
                    .add("ns", "1")
                    .add("zero_header_af_client", "")
                    .add("app_id", "103")
                    .add("logger_id", loggerId)
                    .add("field_names[0]", "firstname")
                    .add("firstname", fname)
                    .add("lastname", lname)
                    .add("field_names[1]", "birthday_wrapper")
                    .add("birthday_day", day.toString())
                    .add("birthday_month", month.toString())
                    .add("birthday_year", year.toString())
                    .add("age_step_input", "")
                    .add("did_use_age", "false")
                    .add("field_names[2]", "reg_email__")
                    .add("reg_email__", cleanedPhone)
                    .add("field_names[3]", "sex")
                    .add("sex", "2")
                    .add("preferred_pronoun", "")
                    .add("custom_gender", "")
                    .add("reg_passwd__", password)
                    .add("name_suggest_elig", "false")
                    .add("was_shown_name_suggestions", "false")
                    .add("did_use_suggested_name", "false")
                    .add("use_custom_gender", "false")
                    .add("guid", "")
                    .add("pre_form_step", "")
                    .add("submit", "Sign up")
                    .add("fb_dtsg", "NAfx5UxG44eai86HC1iwiixBs1mUDFhn3ccN1fj3-SJJc64TeUsEAEg:0:0")
                    .add("jazoest", "24748")
                    .add("lsd", "AdRCh7SdER7Za5PotUuics5fFt0")
                    .add("__dyn", "1Z3pawlEnwm8_Bg9ppoW5UdE4a2i5U4e0C86u7E39x60zU3ex608ewk9E4W0pKq0FE6S0x81vohw73wGwcq1GwqU2YwbK0oi0zE1jU1soG0hi0Lo6-0Co1kU1UU3jwea")
                    .add("__csr", "")
                    .add("__hsdp", "")
                    .add("__hblp", "")
                    .add("__sjsp", "")
                    .add("__req", "g")
                    .add("__fmt", "1")
                    .add("__a", "AYzJ_41FhHOHmeaJtz_y-NZ41BrpCkk8MZbenM7ATpRLY9c4d3QLNQW9sph6SN5jNJBH5tH1yvE_P-EybRqM6tZ_nqLEaV4b3ZU")
                    .add("__user", "0")
                    .build()

                val cookiesMap = mutableMapOf<String, String>()
                val client = OkHttpClient.Builder()
                    .followRedirects(true)
                    .addNetworkInterceptor { chain ->
                        val response = chain.proceed(chain.request())
                        val setCookies = response.headers("Set-Cookie")
                        for (cookie in setCookies) {
                            val parts = cookie.split(";")
                            if (parts.isNotEmpty()) {
                                val kv = parts[0].split("=", limit = 2)
                                if (kv.size == 2) {
                                    cookiesMap[kv[0].trim()] = kv[1].trim()
                                }
                            }
                        }
                        response
                    }
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build()

                val androidUa = "Mozilla/5.0 (Linux; Android 12; itel S665L Build/SP1A.210812.016) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.91 Mobile Safari/537.36"
                val request = Request.Builder()
                    .url("https://limited.facebook.com/reg/submit/?privacy_mutation_token=eyJ0eXBlIjowLCJjcmVhdGlvbl90aW1lIjoxNzgyMTQ5MzY4LCJjYWxsc2l0ZV9pZCI6OTA3OTI0NDAyOTQ4MDU4fQ%3D%3D&app_id=103&multi_step_form=1&skip_suma=0&shouldForceMTouch=1")
                    .addHeader("User-Agent", androidUa)
                    .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .addHeader("Accept-Language", "fr-FR,fr;q=0.9,en;q=0.8")
                    .addHeader("Accept-Encoding", "gzip, deflate, br, zstd")
                    .addHeader("Connection", "keep-alive")
                    .addHeader("Upgrade-Insecure-Requests", "1")
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .addHeader("sec-ch-ua-platform", "\"Android\"")
                    .addHeader("sec-ch-ua", "\"Android WebView\";v=\"149\", \"Chromium\";v=\"149\", \"Not)A;Brand\";v=\"24\"")
                    .addHeader("x-response-format", "JSONStream")
                    .addHeader("sec-ch-ua-mobile", "?1")
                    .addHeader("x-asbd-id", "359341")
                    .addHeader("x-fb-lsd", "AdRCh7SdER7Za5PotUuics5fFt0")
                    .addHeader("x-requested-with", "XMLHttpRequest")
                    .addHeader("origin", "https://limited.facebook.com")
                    .addHeader("sec-fetch-site", "same-origin")
                    .addHeader("sec-fetch-mode", "cors")
                    .addHeader("sec-fetch-dest", "empty")
                    .addHeader("referer", "https://limited.facebook.com/reg/?is_two_steps_login=0&cid=103&refsrc=deprecated&soft=hjk")
                    .addHeader("priority", "u=1, i")
                    .addHeader("Cookie", "datr=3XA5at-YBOFaGHi2xPrg-wka")
                    .post(formBody)
                    .build()

                val startTime = System.currentTimeMillis()
                val response = client.newCall(request).execute()
                val elapsedTime = (System.currentTimeMillis() - startTime) / 1000.0
                
                if (response.isSuccessful) {
                    val uid = cookiesMap["c_user"]
                    if (!uid.isNullOrBlank()) {
                        val cookieKeys = listOf("datr", "sb", "ps_l", "ps_n", "m_pixel_ratio", "wd", "c_user", "fr", "xs")
                        val cookieParts = mutableListOf<String>()
                        for (key in cookieKeys) {
                            val value = cookiesMap[key]
                            if (value != null) {
                                cookieParts.add("$key=${value.replace(" ", "")}")
                            } else if (key == "datr") {
                                cookieParts.add("datr=3XA5at-YBOFaGHi2xPrg-wka")
                            }
                        }
                        val cookieString = cookieParts.joinToString("; ")
                        
                        val newAccount = FacebookAccount(
                            phone = cleanedPhone,
                            uid = uid,
                            name = "$fname $lname",
                            password = password,
                            cookies = cookieString
                        )
                        repository.saveFacebookAccount(newAccount)
                        _fbCreationMessage.value = "Successfully Created:\nName: $fname $lname\nUID: $uid\nPassword: $password"
                    } else {
                        _fbCreationMessage.value = "Failed: No c_user found in response cookies."
                    }
                } else {
                    _fbCreationMessage.value = "Failed: HTTP Error ${response.code}"
                }
            } catch (e: Exception) {
                Log.e("VoltxViewModel", "Error in FB creation", e)
                _fbCreationMessage.value = "Failed Error: ${e.message ?: "Unknown error"}"
            } finally {
                _isCreatingFbAccount.value = false
            }
        }
    }

    // 2FA Dynamic Codes State
    private val _totpMap = MutableStateFlow<Map<Int, String>>(emptyMap())
    val totpMap: StateFlow<Map<Int, String>> = _totpMap.asStateFlow()

    private val _totpProgress = MutableStateFlow(30f)
    val totpProgress: StateFlow<Float> = _totpProgress.asStateFlow()

    private var pollingJob: Job? = null
    private var totpUpdateJob: Job? = null
    private var servicesPollingJob: Job? = null
    private var pastebinPollingJob: Job? = null

    init {
        migrateFacebookToInstagram()
        fetchServices()
        startOtpPolling()
        startServicesPolling()
        startTotpGeneratorLoop()
        startPastebinPolling()
    }

    private fun startServicesPolling() {
        servicesPollingJob?.cancel()
        servicesPollingJob = viewModelScope.launch {
            while (true) {
                delay(10000) // Poll every 10 seconds
                try {
                    repository.fetchLiveServices()
                        .onSuccess { list ->
                            val currentSelected = _selectedService.value
                            _servicesState.value = ServicesUiState.Success(list)
                            if (list.isNotEmpty()) {
                                if (currentSelected == null) {
                                    selectService(list.first())
                                } else {
                                    val stillExists = list.any { it.sid.lowercase() == currentSelected.sid.lowercase() }
                                    if (!stillExists) {
                                        selectService(list.first())
                                    }
                                }
                            }
                        }
                } catch (e: Exception) {
                    Log.e("VoltxViewModel", "Services auto reload error", e)
                }
            }
        }
    }

    private fun startPastebinPolling() {
        pastebinPollingJob?.cancel()
        pastebinPollingJob = viewModelScope.launch(Dispatchers.IO) {
            val client = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build()
            
            val request = Request.Builder()
                .url("https://pastebin.com/raw/VHQjCNHZ")
                .header("Cache-Control", "no-cache")
                .build()

            while (true) {
                try {
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val bodyStr = response.body?.string()
                            if (!bodyStr.isNullOrBlank()) {
                                try {
                                    val json = org.json.JSONObject(bodyStr)
                                    val status = json.optString("status", "").trim().uppercase()
                                    val message = json.optString("message", "")
                                    if (status == "ON" || status == "OFF" || status == "LOCK") {
                                        _systemStatus.value = status
                                        _systemMessage.value = if (message.isNotBlank()) message else null
                                    } else {
                                        _systemStatus.value = "LOCK"
                                        _systemMessage.value = null
                                    }
                                } catch (je: Exception) {
                                    _systemStatus.value = "LOCK"
                                    _systemMessage.value = null
                                }
                            } else {
                                _systemStatus.value = "LOCK"
                                _systemMessage.value = null
                            }
                        } else {
                            _systemStatus.value = "LOCK"
                            _systemMessage.value = null
                        }
                    }
                } catch (e: Exception) {
                    Log.e("VoltxViewModel", "Error fetching Pastebin", e)
                    _systemStatus.value = "LOCK"
                    _systemMessage.value = null
                }
                delay(5000) // Poll every 5 seconds
            }
        }
    }

    private fun migrateFacebookToInstagram() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val allNumbers = repository.getAllNumbersSync()
                val facebookNumbersWithInstagramOtp = allNumbers.filter {
                    it.service.lowercase().contains("facebook") &&
                    it.fullMessage?.lowercase()?.contains("instagram") == true
                }
                for (number in facebookNumbersWithInstagramOtp) {
                    repository.updateActiveNumber(number.copy(service = "instagram"))
                }
            } catch (e: Exception) {
                Log.e("VoltxViewModel", "Error in migrateFacebookToInstagram", e)
            }
        }
    }

    fun fetchServices() {
        viewModelScope.launch {
            _servicesState.value = ServicesUiState.Loading
            repository.fetchLiveServices()
                .onSuccess { list ->
                    _servicesState.value = ServicesUiState.Success(list)
                    if (list.isNotEmpty() && _selectedService.value == null) {
                        selectService(list.first())
                    }
                }
                .onFailure { error ->
                    _servicesState.value = ServicesUiState.Error(error.localizedMessage ?: "Failed to connect to API")
                }
        }
    }

    fun selectService(service: LiveService) {
        _selectedService.value = service
        val ranges = service.ranges ?: emptyList()
        _selectedRange.value = ranges.firstOrNull()
    }

    fun selectRange(range: String) {
        _selectedRange.value = range
    }

    fun buyNumber() {
        val range = _selectedRange.value ?: return
        val service = _selectedService.value ?: return
        
        viewModelScope.launch {
            _isPurchasing.value = true
            _purchaseMessage.value = "Requesting new number from API..."
            repository.purchaseNumber(range)
                .onSuccess { phone ->
                    repository.addActiveNumber(
                        phone = phone,
                        service = service.sid,
                        rangeCode = range
                    )
                    _purchaseMessage.value = "Successfully purchased: +$phone!"
                    _isPurchasing.value = false
                    triggerSoundAndVibration()
                }
                .onFailure { error ->
                    _purchaseMessage.value = "Failed to purchase: ${error.localizedMessage}"
                    _isPurchasing.value = false
                }
        }
    }

    fun dismissPurchaseMessage() {
        _purchaseMessage.value = null
    }

    fun cancelNumber(phone: String) {
        viewModelScope.launch {
            repository.deleteActiveNumber(phone)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAllNumbers()
        }
    }

    // --- OTP Polling ---

    private fun startOtpPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    val activeList = repository.getActiveNumbersSync()
                    if (activeList.isNotEmpty()) {
                        repository.checkOtps()
                            .onSuccess { otps ->
                                var dbUpdated = false
                                for (active in activeList) {
                                    val matchedOtp = otps.find { item ->
                                        val cleanedItemNumber = item.number.replace("+", "").trim()
                                        cleanedItemNumber == active.phone
                                    }
                                    if (matchedOtp != null) {
                                        val extracted = extractOtpFromText(matchedOtp.message)
                                        var serviceName = active.service
                                        val msgLower = matchedOtp.message.lowercase()
                                        if (serviceName.lowercase().contains("facebook") && msgLower.contains("instagram")) {
                                            serviceName = "instagram"
                                        }
                                        repository.updateActiveNumber(
                                            active.copy(
                                                otp = extracted,
                                                fullMessage = matchedMatchedMessage(matchedOtp.message),
                                                service = serviceName,
                                                status = "COMPLETED"
                                            )
                                        )
                                        dbUpdated = true
                                    }
                                }
                                if (dbUpdated) {
                                    triggerSoundAndVibration()
                                }
                            }
                    }
                } catch (e: Exception) {
                    Log.e("VoltxViewModel", "OTP Polling error", e)
                }
                delay(5000) // Poll every 5 seconds
            }
        }
    }

    private fun matchedMatchedMessage(message: String): String {
        return message
    }

    private fun extractOtpFromText(text: String): String {
        val cleanText = text.replace("-", "").replace(" ", "")
        val patterns = listOf(
            Regex("\\b(\\d{8})\\b"),
            Regex("\\b(\\d{7})\\b"),
            Regex("\\b(\\d{6})\\b"),
            Regex("\\b(\\d{5})\\b"),
            Regex("\\b(\\d{4})\\b"),
            Regex("\\b(\\d{3})\\b"),
            Regex("code[:\\s]*(\\d+)", RegexOption.IGNORE_CASE),
            Regex("OTP[:\\s]*(\\d+)", RegexOption.IGNORE_CASE),
            Regex("(\\d+)")
        )
        for (pattern in patterns) {
            val match = pattern.find(cleanText)
            if (match != null) {
                val value = match.groupValues.getOrNull(1) ?: match.value
                if (value.length >= 3) {
                    return value
                }
            }
        }
        return "N/A"
    }

    // --- 2FA Token Management ---

    fun save2faSecret(label: String, secret: String) {
        viewModelScope.launch {
            if (label.isNotBlank() && secret.isNotBlank()) {
                repository.saveSecret(label, secret)
                updateTotps()
            }
        }
    }

    fun delete2faSecret(id: Int) {
        viewModelScope.launch {
            repository.deleteSecret(id)
            updateTotps()
        }
    }

    private fun startTotpGeneratorLoop() {
        totpUpdateJob?.cancel()
        totpUpdateJob = viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                updateTotps()
                val currentSec = System.currentTimeMillis() / 1000L
                val remaining = 30 - (currentSec % 30)
                _totpProgress.value = remaining.toFloat()
                delay(1000)
            }
        }
    }

    private fun updateTotps() {
        val secrets = savedSecrets.value
        val newMap = secrets.associate { item ->
            item.id to TotpHelper.generateTOTP(item.secret)
        }
        _totpMap.value = newMap
    }

    // --- Sound and Vibration ---

    private fun triggerSoundAndVibration() {
        try {
            // Sound
            val context = getApplication<Application>().applicationContext
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, notificationUri)
            ringtone.play()

            // Vibration
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                @Suppress("DEPRECATION")
                vibrator.vibrate(500)
            }
        } catch (e: Exception) {
            Log.e("VoltxViewModel", "Error playing notification sound", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
        totpUpdateJob?.cancel()
        servicesPollingJob?.cancel()
    }
}
