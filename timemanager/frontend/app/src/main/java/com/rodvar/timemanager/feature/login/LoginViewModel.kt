package com.rodvar.timemanager.feature.login

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.util.Patterns
import androidx.lifecycle.viewModelScope
import com.rodvar.timemanager.R
import com.rodvar.timemanager.data.domain.User
import com.rodvar.timemanager.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginForm

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    fun login(username: String, password: String) {
        Log.d(javaClass.simpleName, "Logging in $username / $password")
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val user = userRepository.login(username, password)

                Log.d(javaClass.simpleName, "Logging in success ${user.email}")
                withContext(Dispatchers.Main){
                    _loginResult.value =
                        LoginResult(success = LoggedInUserView(displayName = user.name))
                }
            } catch (e: Exception) {
                Log.e(javaClass.simpleName, "Logging failed", e)
                withContext(Dispatchers.Main) {
                    _loginResult.value = LoginResult(error = R.string.login_failed)
                }
            }
        }
    }

    fun register(username: String, password: String, name: String, preferredHours: Float) {
        Log.d(javaClass.simpleName, "Register in $username / $password")
        this.viewModelScope.launch {
            try {
                User(email = username, password = password, name = name, preferredHours = preferredHours).let { newUser ->
                    userRepository.register(newUser, { user ->
                        Log.d(javaClass.simpleName, "Registration in success ${user.email}")
                        _loginResult.value =
                            LoginResult(success = LoggedInUserView(displayName = user.name))
                    }, {
                        Log.e(javaClass.simpleName, "Registration failed", it)
                        _loginResult.value = LoginResult(error = R.string.registration_failed)
                    })
                }
            } catch (e: Exception) {
                Log.e(javaClass.simpleName, "Registration failed", e)
                _loginResult.value = LoginResult(error = R.string.registration_failed)
            }
        }
    }

    fun loginDataChanged(username: String, password: String) {
        if (!isUserNameValid(username)) {
            _loginForm.value = LoginFormState(usernameError = R.string.invalid_username)
        } else if (!isPasswordValid(password)) {
            _loginForm.value = LoginFormState(passwordError = R.string.invalid_password)
        } else {
            _loginForm.value = LoginFormState(isDataValid = true)
        }
    }

    // A placeholder username validation check
    private fun isUserNameValid(username: String): Boolean {
        return if (username.contains('@')) {
            Patterns.EMAIL_ADDRESS.matcher(username).matches()
        } else {
            username.isNotBlank()
        }
    }

    // A placeholder password validation check
    private fun isPasswordValid(password: String): Boolean {
        return password.length > 4
    }
}