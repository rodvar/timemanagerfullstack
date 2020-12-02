package com.rodvar.timemanager.base

import android.util.Log
import androidx.annotation.CallSuper
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodvar.timemanager.data.domain.User
import com.rodvar.timemanager.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

abstract class BaseViewModel(private val userRepository: UserRepository) : ViewModel() {
    companion object {
        const val MIN_HOURS_PER_WORK_DAY = 1F
        const val MAX_HOURS_PER_WORK_DAY = 16F
        const val MIN_TIME_ENTRY_HOURS = 0.25F
        const val DEFAULT_PREFERRED_HOURS = 6.0F

        val preferredHours = MutableLiveData<Float>()
    }

    val minTimeEntryHours: Float = MIN_TIME_ENTRY_HOURS
    val maxHoursPerDay: Float = MAX_HOURS_PER_WORK_DAY
    val minHoursPerDay: Float = MIN_HOURS_PER_WORK_DAY

    val isAdmin = this.userRepository.loggedUser()?.isAdmin() ?: false
    val isUserAdmin = this.userRepository.loggedUser()?.isUserAdmin() ?: false

    @CallSuper
    fun onResume() {
    }

    /**
     * Ensure user is logged and act accordingly if its not
     */
    fun fetchUser() : User? {
        return try {
            userRepository.loggedUser().let {
                if (it != null) {
                    Log.d("user", "$it")
                    preferredHours.value = it.preferredHours
                }
                it
            }
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "Failed to fetch user", e)
            null
        }
    }

    fun onPreferredHoursUpdate(newPreferredHours: Float, onResponse: (Boolean) -> Unit) {
        viewModelScope.launch {
            userRepository.updateLoggedUserPreferredHours(newPreferredHours,
                {
                    Log.d(javaClass.simpleName, "Updated preferred hours to $newPreferredHours")
                    preferredHours.value = newPreferredHours
                    onResponse(true)
                },
                {
                    Log.e(javaClass.simpleName, "Failed to update preferred hours", it)
                    onResponse(false)
                })
        }
    }

    fun logout(onSuccess: () -> Unit, onError: () -> Unit) {
        GlobalScope.launch (Dispatchers.IO) {
            try {
                Log.d(javaClass.simpleName, "Executing logout..")
                userRepository.logout({
                    onLogout()
                    onSuccess()
                }, {
                    onError()
                })
            } catch (e: Exception) {
                Log.e(javaClass.simpleName, "Failed to logout", e)
                onError()
            }
        }
    }

    open fun onLogout() {}

    fun loggedUser(): User? = this.userRepository.loggedUser()


}