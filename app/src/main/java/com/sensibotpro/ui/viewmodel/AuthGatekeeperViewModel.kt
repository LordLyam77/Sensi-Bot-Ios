package com.sensibotpro.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sensibotpro.license.ActivationResult
import com.sensibotpro.license.LicenseRepository
import com.sensibotpro.license.SupabaseLicenseRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthGateState {
    object Checking : AuthGateState()
    object Unauthenticated : AuthGateState()
    object Authenticated : AuthGateState()
}

/**
 * AuthGatekeeperViewModel is the single authority for app access.
 *
 * Requirements enforced:
 * 1. Lock the entire app behind login.
 * 2. On app launch (cold start), validate stored token against Supabase.
 * 3. On resume (onResume), silently re-validate to catch revoked or reset keys immediately.
 * 4. If validation fails, immediately force the user to the license screen and wipe session.
 */
class AuthGatekeeperViewModel(application: Application) : AndroidViewModel(application) {

    val repository: SupabaseLicenseRepository = SupabaseLicenseRepository(application)

    private val _gateState = MutableStateFlow<AuthGateState>(AuthGateState.Checking)
    val gateState: StateFlow<AuthGateState> = _gateState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    val deviceId: String
        get() = repository.secureDeviceManager.getOrCreateDeviceId()

    private var activeCheckJob: Job? = null

    init {
        performInitialValidation()
    }

    /**
     * Cold start validation. Blocks app navigation until complete.
     */
    fun performInitialValidation() {
        activeCheckJob?.cancel()
        activeCheckJob = viewModelScope.launch {
            _gateState.value = AuthGateState.Checking
            val token = repository.secureDeviceManager.getSessionToken()
            if (token.isNullOrBlank()) {
                _gateState.value = AuthGateState.Unauthenticated
                return@launch
            }

            val status = repository.checkLicenseStatus()
            if (status.isLicensed) {
                _gateState.value = AuthGateState.Authenticated
            } else {
                _gateState.value = AuthGateState.Unauthenticated
            }
        }
    }

    /**
     * Periodic re-validation called whenever app returns to foreground (onResume).
     * If admin revoked key or reset the device in Supabase, kicks user to login immediately.
     */
    fun checkOnResume() {
        // If already unauthenticated or checking, ignore
        if (_gateState.value != AuthGateState.Authenticated) return

        viewModelScope.launch {
            val status = repository.checkLicenseStatus()
            if (!status.isLicensed) {
                // Instantly revoke access and lock app
                _gateState.value = AuthGateState.Unauthenticated
                _errorMessage.value = "Your license session was revoked or expired. Please enter your key again."
            }
        }
    }

    /**
     * Called when user enters a license key on the Login Gate screen.
     */
    fun submitLicenseKey(key: String) {
        if (_isSubmitting.value) return
        val cleanKey = key.trim().uppercase()

        if (cleanKey.isEmpty()) {
            _errorMessage.value = "Please enter your license key."
            return
        }

        viewModelScope.launch {
            _isSubmitting.value = true
            _errorMessage.value = null

            when (val res = repository.activateLicense(cleanKey)) {
                is ActivationResult.Success -> {
                    _errorMessage.value = null
                    _gateState.value = AuthGateState.Authenticated
                }
                is ActivationResult.Error -> {
                    _errorMessage.value = res.message
                    _gateState.value = AuthGateState.Unauthenticated
                }
            }
            _isSubmitting.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Manual logout / unlink session if triggered from settings.
     */
    fun logout() {
        repository.secureDeviceManager.clearSessionToken()
        _gateState.value = AuthGateState.Unauthenticated
        _errorMessage.value = null
    }
}
