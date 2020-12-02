package main.kotlin.db

import org.jetbrains.exposed.sql.Table
import org.joda.time.DateTimeZone

/**
 * DB definition and its operations
 */
object Schema {
    const val DEFAULT_TEXT_SIZE = 64
    const val HASH_SIZE = 128
    const val DEFAULT_PREFERRED_TIME = 6.0f
    val SERVER_TIMEZONE = DateTimeZone.forID("Australia/Sydney")

    enum class Roles { REGULAR, MANAGER, ADMIN }

    object USER : Table() {
        val id = long("id").autoIncrement().primaryKey()
        val name = varchar("name", DEFAULT_TEXT_SIZE)
        val email = varchar("email", DEFAULT_TEXT_SIZE).uniqueIndex()
        val password = varchar("password", HASH_SIZE)
        val role = enumeration("role", Roles::class.java).default(Roles.REGULAR)
        val preferredHours = float("preferred_hrs").default(DEFAULT_PREFERRED_TIME)
        val lastLogin = long("last_login_ts").nullable()
        val sessionKey = varchar("session_key", HASH_SIZE).nullable()
    }

    object TIME_ENTRIES : Table() {
        val id = long("id").autoIncrement().primaryKey()
        val userId = (long("user_id") references USER.id)
        val date = date("date").index("date_idx")
        val time = long("timestamp")
        val hours = float("hours")
        val note = varchar("note", DEFAULT_TEXT_SIZE)

        init {
            index("date_user_idx", false, userId, date)
        }
    }
}