package com.sensibotpro.data.repository

import com.sensibotpro.data.local.ProfileDatabaseHelper
import com.sensibotpro.domain.model.SensitivityProfile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID

class ProfileRepository(
    private val dbHelper: ProfileDatabaseHelper,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val _profiles = MutableStateFlow<List<SensitivityProfile>>(emptyList())
    val profiles: StateFlow<List<SensitivityProfile>> = _profiles.asStateFlow()

    private val _activeProfile = MutableStateFlow<SensitivityProfile?>(null)
    val activeProfile: StateFlow<SensitivityProfile?> = _activeProfile.asStateFlow()

    suspend fun refresh() = withContext(dispatcher) {
        val list = dbHelper.getAllProfiles()
        _profiles.value = list
        val active = dbHelper.getActiveProfile() ?: list.firstOrNull()
        _activeProfile.value = active
    }

    suspend fun saveProfile(profile: SensitivityProfile) = withContext(dispatcher) {
        dbHelper.insertOrUpdateProfile(profile)
        refresh()
    }

    suspend fun duplicateProfile(original: SensitivityProfile) = withContext(dispatcher) {
        val copy = original.copy(
            id = UUID.randomUUID().toString(),
            name = "${original.name} (Copy)",
            isCustom = true,
            isActive = false,
            createdAt = System.currentTimeMillis()
        )
        dbHelper.insertOrUpdateProfile(copy)
        refresh()
    }

    suspend fun deleteProfile(id: String) = withContext(dispatcher) {
        dbHelper.deleteProfile(id)
        refresh()
    }

    suspend fun setActiveProfile(id: String) = withContext(dispatcher) {
        dbHelper.setActiveProfile(id)
        refresh()
    }
}
