package main.kotlin.domain

import main.kotlin.db.Schema

data class User (
        val id: Long? = null,
        var email: String,
        var password: String,
        var name: String = "",
        var preferredHours: Float = 6F,
        var role: Schema.Roles? = null,
        var lastLogin: Long? = null,
        var sessionKey: String? = null
)