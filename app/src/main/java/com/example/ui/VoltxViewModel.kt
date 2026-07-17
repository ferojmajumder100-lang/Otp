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

    // 2FA Dynamic Codes State
    private val _totpMap = MutableStateFlow<Map<Int, String>>(emptyMap())
    val totpMap: StateFlow<Map<Int, String>> = _totpMap.asStateFlow()

    private val _totpProgress = MutableStateFlow(30f)
    val totpProgress: StateFlow<Float> = _totpProgress.asStateFlow()

    private var pollingJob: Job? = null
    private var totpUpdateJob: Job? = null
    private var servicesPollingJob: Job? = null

    init {
        migrateFacebookToInstagram()
        fetchServices()
        startOtpPolling()
        startServicesPolling()
        startTotpGeneratorLoop()
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
