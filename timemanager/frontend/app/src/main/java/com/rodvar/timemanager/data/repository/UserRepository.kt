package com.rodvar.timemanager.data.repository

import android.util.Log
import com.rodvar.timemanager.data.api.TimeManagerAPI
import com.rodvar.timemanager.data.domain.Roles
import com.rodvar.timemanager.data.domain.User
import com.rodvar.timemanager.data.networking.AuthInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository(private val timeManagerAPI: TimeManagerAPI) : BaseRepository() {

    private var user: User? = null

    @Throws(UserNotLoggedInException::class)
    suspend fun login(userName: String, password: String) : User {
        if (user == null) {
            try {
                this.timeManagerAPI.login(User.from(userName, password)).let { loggedUser ->
                    loginUser(loggedUser)
                }
            } catch (e : Exception) {
                e.printStackTrace()
            }
        }
        if (this.user == null)
            throw UserNotLoggedInException("User is not logged in, search for internet connectivity errors")
        return this.user!!
    }

    suspend fun register(user: User,
                         onSuccess: (User: User) -> Unit,
                         onError: (e: Exception) -> Unit) {
        try {
            this.loginUser(this.timeManagerAPI.addUserAsync(user))
            onSuccess(this.user!!)
        } catch (e : Exception) {
            e.printStackTrace()
            onError(e)
        }
    }

    private fun loginUser(loggedUser: User) {
        user = loggedUser
        Log.d(javaClass.simpleName, "Session key ${this.user!!.sessionKey}")
        AuthInterceptor.sessionKey = this.user!!.sessionKey
    }

    suspend fun logout(onSuccess: () -> Unit, onError: () -> Unit) {
        if (user == null)
            onSuccess()
        else {
            try {
                this.timeManagerAPI.logout()
                this.user = null
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e : Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onError()
                }
            }
        }
    }

    /**
     * @return all the time entries available for the logged user. If the user is an admin it will
     * contain all of the time entries
     */
    suspend fun getAll(
        onSuccess: (User: List<User>) -> Unit,
        onError: (e: Exception) -> Unit
    ) {
        try {
            onSuccess(this.timeManagerAPI.getUsersAsync())
        } catch (e: Exception) {
            onError(e)
        }
    }

    suspend fun delete(userToDelete: User,
                       onSuccess: (User: User) -> Unit,
                       onError: (e: Exception) -> Unit) {
        try {
            this.timeManagerAPI.deleteUserAsync(userToDelete.id!!)
            onSuccess(userToDelete)
        } catch (e: Exception) {
            onError(e)
        }
    }

    suspend fun add(user: User,
                       onSuccess: (User) -> Unit,
                       onError: (e: Exception) -> Unit) {
        try {
            onSuccess(this.timeManagerAPI.addUserAsync(user))
        } catch (e: Exception) {
            onError(e)
        }
    }

    suspend fun update(user: User,
                    onSuccess: (List<User>) -> Unit,
                    onError: (e: Exception) -> Unit) {
        try {
            onSuccess(this.timeManagerAPI.updateUserAsync(user))
        } catch (e: Exception) {
            onError(e)
        }
    }

    suspend fun updateLoggedUserPreferredHours(newPreferredHours: Float,
                       onSuccess: (User) -> Unit,
                       onError: (e: Exception) -> Unit) {
        try {
            this.user!!.clone().apply {
                this.preferredHours = newPreferredHours
                update(this,
                    {
                        user?.preferredHours = newPreferredHours
                        onSuccess(this)
                    },
                    {
                        onError(it)
                    })
            }
        } catch (e: NullPointerException) {
            onError(UserNotLoggedInException("Cannot update preferred hours, not logged in", e))
        } catch (e: Exception) {
            onError(e)
        }
    }

    fun canCrudAll() = this.user?.role == Roles.ADMIN
    fun loggedUser(): User? = this.user
}

class UserNotLoggedInException(message: String, e: Exception? = null): Exception(message, e)
