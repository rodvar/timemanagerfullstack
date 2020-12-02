package com.rodvar.timemanager.data.domain

enum class Roles { REGULAR, MANAGER, ADMIN }

data class User(
    val id: Long? = null,
    var email: String,
    var password: String? = null,
    var name: String = "",
    var preferredHours: Float = 6f,
    var role: Roles? = null,
    var lastLogin: Long? = null,
    var sessionKey: String? = null
) {
    companion object {
        fun from(userName: String, password: String) : User = User(email = userName, password = password)
    }
    fun clone() = User(id, email, password, name, preferredHours, role, lastLogin, sessionKey)
    fun isAdmin() = this.role == Roles.ADMIN
    fun isManager() = this.role == Roles.MANAGER
    fun isUserAdmin() = isAdmin() || isManager()
}