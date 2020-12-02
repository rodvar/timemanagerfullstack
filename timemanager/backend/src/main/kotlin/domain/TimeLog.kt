package main.kotlin.domain

import java.util.*

data class TimeLog(
        val id: Long? = null,
        var userEmail: String? = null,
        var time: Long,
        var hours: Float,
        var note: String
) {
    var userId: Long? = null

    fun date() = Date(time)
}