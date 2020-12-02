package com.rodvar.timemanager.data.domain

import java.util.*

data class TimeLog(
    val id: Long? = null,
    var userEmail: String? = null,
    var time: Long,
    var hours: Float,
    var note: String
) {
    fun date() = Date(time)

    fun clone() = TimeLog(id, userEmail, time, hours, note)
}