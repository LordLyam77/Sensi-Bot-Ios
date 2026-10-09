package com.sensibotpro.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sensibotpro.chat.ChatEngine
import com.sensibotpro.chat.ChatMessage
import com.sensibotpro.chat.QuickChip
import com.sensibotpro.chat.NeuralChatEngine
import com.sensibotpro.chat.RuleBasedChatEngine
import com.sensibotpro.chat.HybridChatEngine
import com.sensibotpro.chat.AiConfig
import com.sensibotpro.chat.AiProvider
import com.sensibotpro.chat.OnlineAiClient
import com.sensibotpro.chat.AiCoachingMode
import com.sensibotpro.chat.LocalModelManager
import com.sensibotpro.chat.ModelStatus
import com.sensibotpro.chat.DeviceCapabilityResult
import com.sensibotpro.data.local.ProfileDatabaseHelper
import com.sensibotpro.data.repository.ProfileRepository
import com.sensibotpro.device.DeviceInfoProvider
import com.sensibotpro.domain.model.*
import com.sensibotpro.domain.recommendation.CalibrationEngine
import com.sensibotpro.domain.recommendation.CalibrationFeedback
import com.sensibotpro.domain.recommendation.CalibrationResult
import com.sensibotpro.domain.recommendation.RecommendationEngine
import com.sensibotpro.license.ActivationResult
import com.sensibotpro.license.LicenseInfo
import com.sensibotpro.license.LicenseRepository
import com.sensibotpro.license.MockLicenseRepository
import com.sensibotpro.license.SupabaseLicenseRepository
import com.sensibotpro.service.CrosshairOverlayService
import com.sensibotpro.service.FloatingSensiService
import com.sensibotpro.service.TargetZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val deviceInfoProvider = DeviceInfoProvider(application)
    val deviceSpecs: DeviceSpecs = deviceInfoProvider.getDeviceSpecs()

    val profileRepository = ProfileRepository(ProfileDatabaseHelper(application))
    val activeProfile: StateFlow<SensitivityProfile?> = profileRepository.activeProfile

    private val _analysisStep = MutableStateFlow<String?>(null)
    val analysisStep: StateFlow<String?> = _analysisStep.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _recommendedProfile = MutableStateFlow<SensitivityProfile?>(null)
    val recommendedProfile: StateFlow<SensitivityProfile?> = _recommendedProfile.asStateFlow()

    val quickCoachingTip = "If your drag consistently goes above the head, slightly reduce General sensitivity by 4-6 points and shorten your upward swipe distance."

    init {
        viewModelScope.launch {
            profileRepository.refresh()
            _recommendedProfile.value = profileRepository.activeProfile.value
        }
    }

    fun startQuickAnalysis() {
        if (_isAnalyzing.value) return
        viewModelScope.launch {
            _isAnalyzing.value = true
            val steps = listOf(
                "Analyzing Device...",
                "Analyzing Playstyle...",
                "Optimizing Sensitivity...",
                "Building Profile...",
                "Profile Ready!"
            )
            for (step in steps) {
                _analysisStep.value = step
                delay(400)
            }
            val result = RecommendationEngine.generateRecommendation(
                deviceSpecs = deviceSpecs,
                playstyle = Playstyle.AGGRESSIVE,
                fingerSetup = FingerSetup.TWO_FINGER,
                weaponType = WeaponType.SHOTGUN,
                problem = AimProblem.OVERSHOOTING_HEAD
            )
            _recommendedProfile.value = result.profile
            _isAnalyzing.value = false
            _analysisStep.value = null
        }
    }
}

class SensiFinderViewModel(application: Application) : AndroidViewModel(application) {
    private val deviceInfoProvider = DeviceInfoProvider(application)
    val deviceSpecs: DeviceSpecs = deviceInfoProvider.getDeviceSpecs()
    val profileRepository = ProfileRepository(ProfileDatabaseHelper(application))

    private val _currentStep = MutableStateFlow(1)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    var selectedPlaystyle = MutableStateFlow(Playstyle.AGGRESSIVE)
    var selectedFingerSetup = MutableStateFlow(FingerSetup.TWO_FINGER)
    var selectedWeapon = MutableStateFlow(WeaponType.SHOTGUN)
    var selectedProblem = MutableStateFlow(AimProblem.OVERSHOOTING_HEAD)

    var currentGeneralInput = MutableStateFlow("185")
    var currentRedDotInput = MutableStateFlow("180")

    private val _generatedRecommendation = MutableStateFlow<SensitivityRecommendation?>(null)
    val generatedRecommendation: StateFlow<SensitivityRecommendation?> = _generatedRecommendation.asStateFlow()

    fun nextStep() {
        if (_currentStep.value < 6) {
            _currentStep.value += 1
        } else {
            generateRecommendation()
        }
    }

    fun prevStep() {
        if (_currentStep.value > 1) {
            _currentStep.value -= 1
        }
    }

    fun generateRecommendation() {
        val gen = currentGeneralInput.value.toIntOrNull()
        val red = currentRedDotInput.value.toIntOrNull()

        val recommendation = RecommendationEngine.generateRecommendation(
            deviceSpecs = deviceSpecs,
            playstyle = selectedPlaystyle.value,
            fingerSetup = selectedFingerSetup.value,
            weaponType = selectedWeapon.value,
            problem = selectedProblem.value,
            currentGeneral = gen,
            currentRedDot = red
        )
        _generatedRecommendation.value = recommendation
        _currentStep.value = 7 // Result view
    }

    fun saveAsProfile(onComplete: () -> Unit) {
        val rec = _generatedRecommendation.value ?: return
        viewModelScope.launch {
            val custom = rec.profile.copy(isCustom = true, isActive = true)
            profileRepository.saveProfile(custom)
            profileRepository.setActiveProfile(custom.id)
            onComplete()
        }
    }

    fun resetWizard() {
        _currentStep.value = 1
        _generatedRecommendation.value = null
    }
}

class CalibrationViewModel(application: Application) : AndroidViewModel(application) {
    private val profileRepository = ProfileRepository(ProfileDatabaseHelper(application))
    private val _activeProfile = MutableStateFlow<SensitivityProfile?>(null)
    val activeProfile: StateFlow<SensitivityProfile?> = _activeProfile.asStateFlow()

    private val _calibrationResult = MutableStateFlow<CalibrationResult?>(null)
    val calibrationResult: StateFlow<CalibrationResult?> = _calibrationResult.asStateFlow()

    init {
        viewModelScope.launch {
            profileRepository.refresh()
            _activeProfile.value = profileRepository.activeProfile.value
        }
    }

    fun applyFeedback(feedback: CalibrationFeedback) {
        val profile = _activeProfile.value ?: return
        val result = CalibrationEngine.calibrate(profile, feedback)
        _calibrationResult.value = result
        _activeProfile.value = result.updatedProfile

        viewModelScope.launch {
            profileRepository.saveProfile(result.updatedProfile)
            profileRepository.setActiveProfile(result.updatedProfile.id)
        }
    }
}

class SensiBotViewModel(application: Application) : AndroidViewModel(application) {
    private val hybridEngine = HybridChatEngine(application)
    private val chatEngine: ChatEngine = hybridEngine
    val onlineClient: OnlineAiClient = hybridEngine.onlineClient

    private val _aiConfig = MutableStateFlow(onlineClient.getConfig())
    val aiConfig: StateFlow<AiConfig> = _aiConfig.asStateFlow()

    private val deviceInfoProvider = DeviceInfoProvider(application)
    val deviceSpecs: DeviceSpecs = deviceInfoProvider.getDeviceSpecs()

    private val profileRepository = ProfileRepository(ProfileDatabaseHelper(application))
    private val activeProfile: StateFlow<SensitivityProfile?> = profileRepository.activeProfile

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    val chips: List<QuickChip> = chatEngine.getSuggestedChips()

    fun updateAiConfig(config: AiConfig) {
        onlineClient.saveConfig(config)
        _aiConfig.value = config
    }

    suspend fun testAiConnection(provider: AiProvider, apiKey: String): Result<String> {
        return onlineClient.testConnection(provider, apiKey)
    }

    val localModelManager: LocalModelManager = hybridEngine.localModelManager
    val modelStatus: StateFlow<ModelStatus> = localModelManager.modelStatus
    val deviceCapability: DeviceCapabilityResult = localModelManager.checkDeviceCapability()

    private val _aiCoachingMode = MutableStateFlow(hybridEngine.getAiMode())
    val aiCoachingMode: StateFlow<AiCoachingMode> = _aiCoachingMode.asStateFlow()

    fun setAiCoachingMode(mode: AiCoachingMode) {
        hybridEngine.setAiMode(mode)
        _aiCoachingMode.value = mode
    }

    init {
        viewModelScope.launch {
            profileRepository.refresh()
            localModelManager.refreshModelStatus()
            val isOnline = _aiConfig.value.isOnlineEnabled && _aiConfig.value.apiKey.isNotBlank()
            val isLocalQwen = localModelManager.checkModelExists()
            val activeBadge = when {
                _aiCoachingMode.value == AiCoachingMode.RULE_BASED -> "SMART RULES (Rule-Based Coach • 100% Offline)"
                isLocalQwen -> "LOCAL AI (Qwen3-0.6B On-Device • 100% Offline)"
                isOnline -> "CLOUD AI (${_aiConfig.value.provider.displayName})"
                else -> "SMART COACH (Hybrid Rules • 100% Offline)"
            }

            _messages.value = listOf(
                ChatMessage(
                    text = "Hey! I'm SENSI BOT — official Lyam FF Free Fire sensitivity & aim coach. 🎯\n\n🟢 $activeBadge\n\nI can help you with:\n• M1887, Desert Eagle, MP40 & sniper drag techniques\n• DPI (Smallest Width) & general sensitivity tuning\n• Overshooting or body-shot recovery\n• Custom HUD button placement & claw setups\n• Character skill combos for rush gameplay\n\nWhat would you like to improve today?",
                    isUser = false,
                    thinkingText = "Coach Mode: ${_aiCoachingMode.value.badgeLabel} • Tap ⚙️ to configure AI Mode"
                )
            )
        }
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val userMsg = ChatMessage(text = trimmed, isUser = true)
        val currentList = _messages.value.toMutableList()
        currentList.add(userMsg)
        _messages.value = currentList

        viewModelScope.launch {
            // 1. Show thinking placeholder immediately
            val placeholderId = java.util.UUID.randomUUID().toString()
            val thinkingPlaceholder = ChatMessage(
                id = placeholderId,
                text = "",
                isUser = false,
                thinkingText = "Processing your coaching request...",
                isStreaming = true
            )
            val withPlaceholder = _messages.value.toMutableList()
            withPlaceholder.add(thinkingPlaceholder)
            _messages.value = withPlaceholder

            // 2. Call suspend processMessage — acts exactly like an async API call
            //    NeuralChatEngine simulates variable latency internally based on complexity
            val botResponse = chatEngine.processMessage(trimmed, activeProfile.value, deviceSpecs)
            val fullText = botResponse.text
            val words = fullText.split(" ")

            // 3. Update placeholder with thinking text now that we have the response
            val withThinking = _messages.value.toMutableList()
            val thinkIdx = withThinking.indexOfFirst { it.id == placeholderId }
            if (thinkIdx != -1) {
                withThinking[thinkIdx] = thinkingPlaceholder.copy(
                    id = botResponse.id,
                    thinkingText = botResponse.thinkingText ?: "Analyzing drag profile...",
                    isStreaming = true,
                    actionSuggestion = botResponse.actionSuggestion,
                    sensitivityAdjustment = botResponse.sensitivityAdjustment
                )
                _messages.value = withThinking
            }

            delay(120) // Brief pause before streaming starts

            // 4. Stream tokens word by word — simulates real LLM token streaming
            val streamedBuilder = StringBuilder()
            for (i in words.indices) {
                streamedBuilder.append(words[i])
                if (i < words.size - 1) streamedBuilder.append(" ")

                val updatedList = _messages.value.toMutableList()
                val lastIdx = updatedList.indexOfFirst { it.id == botResponse.id }
                if (lastIdx != -1) {
                    updatedList[lastIdx] = ChatMessage(
                        id = botResponse.id,
                        text = streamedBuilder.toString(),
                        isUser = false,
                        thinkingText = botResponse.thinkingText,
                        isStreaming = true,
                        actionSuggestion = botResponse.actionSuggestion,
                        sensitivityAdjustment = botResponse.sensitivityAdjustment
                    )
                    _messages.value = updatedList
                }
                // Variable token speed: faster for short words, slight pause for punctuation
                val tokenDelay = when {
                    words[i].endsWith(".") || words[i].endsWith("!") || words[i].endsWith("?") -> 45L
                    words[i].endsWith(",") || words[i].endsWith(":") -> 30L
                    words[i].endsWith("\n") -> 55L
                    else -> 14L
                }
                delay(tokenDelay)
            }

            // 5. Mark streaming complete
            val finalList = _messages.value.toMutableList()
            val finalIdx = finalList.indexOfFirst { it.id == botResponse.id }
            if (finalIdx != -1) {
                finalList[finalIdx] = botResponse.copy(text = fullText, isStreaming = false)
                _messages.value = finalList
            }
        }
    }

    fun clearChat() {
        chatEngine.resetConversation()
        _messages.value = listOf(
            ChatMessage(
                text = "Chat cleared. Tell me what you'd like to work on next!",
                isUser = false
            )
        )
    }

    override fun onCleared() {
        super.onCleared()
        // Release local LLM resources so memory is completely free for Free Fire gameplay
        hybridEngine.releaseResources()
    }
}

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    val repository = ProfileRepository(ProfileDatabaseHelper(application))
    val licenseRepository: LicenseRepository = SupabaseLicenseRepository(application)
    val deviceInfoProvider = DeviceInfoProvider(application)

    val profiles: StateFlow<List<SensitivityProfile>> = repository.profiles
    val activeProfile: StateFlow<SensitivityProfile?> = repository.activeProfile
    val licenseInfo: StateFlow<LicenseInfo> = licenseRepository.licenseState
    val deviceSpecs: DeviceSpecs = deviceInfoProvider.getDeviceSpecs()

    init {
        viewModelScope.launch {
            repository.refresh()
            licenseRepository.checkLicenseStatus()
        }
    }

    fun setActive(id: String) {
        viewModelScope.launch {
            repository.setActiveProfile(id)
        }
    }

    fun duplicate(profile: SensitivityProfile) {
        viewModelScope.launch {
            repository.duplicateProfile(profile)
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            repository.deleteProfile(id)
        }
    }

    fun saveCustomProfile(profile: SensitivityProfile) {
        viewModelScope.launch {
            repository.saveProfile(profile)
        }
    }
}

class LicenseViewModel(application: Application) : AndroidViewModel(application) {
    val licenseRepo: LicenseRepository = SupabaseLicenseRepository(application)
    val licenseState: StateFlow<LicenseInfo> = licenseRepo.licenseState

    private val _activationStatusMessage = MutableStateFlow<String?>(null)
    val activationStatusMessage: StateFlow<String?> = _activationStatusMessage.asStateFlow()

    private val _isError = MutableStateFlow(false)
    val isError: StateFlow<Boolean> = _isError.asStateFlow()

    fun activate(key: String) {
        viewModelScope.launch {
            when (val result = licenseRepo.activateLicense(key)) {
                is ActivationResult.Success -> {
                    _activationStatusMessage.value = "License verified and active! Bound to this device."
                    _isError.value = false
                }
                is ActivationResult.Error -> {
                    _activationStatusMessage.value = result.message
                    _isError.value = true
                }
            }
        }
    }

    fun requestDeviceTransfer(reason: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = licenseRepo.requestDeviceTransfer(reason)
            onResult(result.getOrDefault("Request submitted."))
        }
    }
}

class OverlayViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application
    private val prefs: SharedPreferences = context.getSharedPreferences(CrosshairOverlayService.PREFS_NAME, Context.MODE_PRIVATE)

    var hasOverlayPermission = MutableStateFlow(checkOverlayPermission())

    var isFloatingSensiRunning = MutableStateFlow(FloatingSensiService.isRunning)
    var isCrosshairRunning = MutableStateFlow(CrosshairOverlayService.isRunning)

    var crosshairStyle = MutableStateFlow(prefs.getString(CrosshairOverlayService.KEY_STYLE, "Classic Cross") ?: "Classic Cross")
    var crosshairSize = MutableStateFlow(prefs.getInt(CrosshairOverlayService.KEY_SIZE, 24))
    var crosshairColor = MutableStateFlow(prefs.getString(CrosshairOverlayService.KEY_COLOR, "#FF2A4D") ?: "#FF2A4D")
    var crosshairThickness = MutableStateFlow(prefs.getInt(CrosshairOverlayService.KEY_THICKNESS, 3))
    var crosshairOpacity = MutableStateFlow(prefs.getFloat(CrosshairOverlayService.KEY_OPACITY, 0.9f))

    fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun refreshPermission() {
        hasOverlayPermission.value = checkOverlayPermission()
        refreshServiceRunningState()
    }

    fun refreshServiceRunningState() {
        isFloatingSensiRunning.value = FloatingSensiService.isRunning
        isCrosshairRunning.value = CrosshairOverlayService.isRunning
    }

    fun applyAssistantZone(zone: TargetZone) {
        viewModelScope.launch(Dispatchers.IO) {
            val dbHelper = ProfileDatabaseHelper(context)
            val active = dbHelper.getActiveProfile() ?: return@launch
            val (newGen, newRed, newBtn) = when (zone) {
                TargetZone.HEAD -> Triple(98, 94, 42)
                TargetZone.BODY -> Triple(92, 88, 48)
                TargetZone.LEGS -> Triple(88, 84, 52)
            }
            val updated = active.copy(
                general = newGen,
                redDot = newRed,
                fireButtonSize = newBtn
            )
            dbHelper.insertOrUpdateProfile(updated)
        }
    }

    fun toggleFloatingSensi() {
        if (!checkOverlayPermission()) return
        val intent = Intent(context, FloatingSensiService::class.java)
        if (FloatingSensiService.isRunning) {
            context.stopService(intent)
            isFloatingSensiRunning.value = false
        } else {
            context.startService(intent)
            isFloatingSensiRunning.value = true
        }
    }

    fun toggleCrosshair() {
        if (!checkOverlayPermission()) return
        val intent = Intent(context, CrosshairOverlayService::class.java)
        if (CrosshairOverlayService.isRunning) {
            context.stopService(intent)
            isCrosshairRunning.value = false
        } else {
            context.startService(intent)
            isCrosshairRunning.value = true
        }
    }

    fun updateCrosshairPreferences(
        style: String = crosshairStyle.value,
        size: Int = crosshairSize.value,
        color: String = crosshairColor.value,
        thickness: Int = crosshairThickness.value,
        opacity: Float = crosshairOpacity.value
    ) {
        crosshairStyle.value = style
        crosshairSize.value = size
        crosshairColor.value = color
        crosshairThickness.value = thickness
        crosshairOpacity.value = opacity

        prefs.edit()
            .putString(CrosshairOverlayService.KEY_STYLE, style)
            .putInt(CrosshairOverlayService.KEY_SIZE, size)
            .putString(CrosshairOverlayService.KEY_COLOR, color)
            .putInt(CrosshairOverlayService.KEY_THICKNESS, thickness)
            .putFloat(CrosshairOverlayService.KEY_OPACITY, opacity)
            .apply()

        if (CrosshairOverlayService.isRunning) {
            val intent = Intent(context, CrosshairOverlayService::class.java).apply {
                action = CrosshairOverlayService.ACTION_UPDATE
            }
            context.startService(intent)
        }
    }
}
