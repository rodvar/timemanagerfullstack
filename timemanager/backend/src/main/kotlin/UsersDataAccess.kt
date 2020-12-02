package main.kotlin

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.util.pipeline.*
import main.kotlin.db.Schema
import main.kotlin.db.Security
import main.kotlin.domain.User
import main.kotlin.utils.DateUtils
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Adds a new user to the DB. Only admins can do this
 * @param user to add
 */
suspend fun PipelineContext<Unit, ApplicationCall>.addUser(user: User) {
    var newUser: User? = null
    transaction {
        Schema.USER.insert {
            it[name] = user.name
            it[email] = user.email
            it[password] = Security.encrypt(user.password)
            it[preferredHours] = user.preferredHours
            if (user.role == null) { // registration
                it[role] = Schema.Roles.REGULAR
                DateUtils.now().let {now ->
                    it[lastLogin] = now.time
                    it[sessionKey] = Security.encryptRandom("${user.email}${now.time}")
                }
            } else
                it[role] = user.role!!
        }.let { entryTx ->
            Schema.USER.select { Schema.USER.id eq (entryTx.generatedKey?.toLong() ?: 0L) }
                    .singleOrNull()
                    ?.let {
                        newUser = user(it)
                        newUser!!.password = "" // for security
                    }
        }
    }
    when (newUser) {
        null -> call.respond(HttpStatusCode.NotAcceptable, "Failed to create timelog $user")
        else -> call.respond(newUser!!)
    }
}

/**
 * @param requestingRole role of the requesting user
 * @return all the app users only of the user requesting is admin
 */
suspend fun PipelineContext<Unit, ApplicationCall>.getUsers(requestingRole: Schema.Roles) {
    var allOutcomes: List<User>? = null
    transaction {
        try {
            allOutcomes = (when {
                (requestingRole == Schema.Roles.ADMIN ||
                        requestingRole == Schema.Roles.MANAGER) -> Schema.USER.selectAll().map { user(it) }
                else -> listOf()
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }.apply {
        if (allOutcomes == null)
            call.respond(HttpStatusCode.Unauthorized, "Only admins can see users")
        else
            call.respond(allOutcomes!!)
    }
}

/**
 * @param email of the user to find
 * @return the user found
 */
suspend fun PipelineContext<Unit, ApplicationCall>.findUser(email: String) : User? {
    var user: User? = null
    transaction {
        try {
            user = Schema.USER.select { Schema.USER.email eq email }.singleOrNull().let { result ->
                if (result == null)
                    null
                else
                    user(result)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return user
}

/**
* Update the user associated with userId if user has permissions.
* @param user from where to take the new values
* @param requestingUser of the user requesting this update
*/
suspend fun PipelineContext<Unit, ApplicationCall>.updateUser(user: User, requestingUser : Pair<Long, Schema.Roles>) {
    var unauth = false
    var error = false
    transaction {
        if (user.id == null || (user.id != requestingUser.first && requestingUser.second != Schema.Roles.ADMIN))
            unauth = true
        try {
            Schema.USER.update({ Schema.USER.id eq user.id!! }) { updateStatement ->
                updateStatement[email] = user.email
                if (!user.password.isNullOrEmpty())
                    updateStatement[password] = Security.encrypt(user.password)
                updateStatement[name] = user.name
                updateStatement[preferredHours] = user.preferredHours
                if (user.role != null)
                    updateStatement[role] = user.role!!
            }
        } catch (e: Exception) {
            e.printStackTrace()
            error = true
        }
    }
    when {
        unauth -> call.respond(HttpStatusCode.Unauthorized, "This user cannot update $user")
        error -> call.respond(HttpStatusCode.NotAcceptable, "One or more changes are unnaceptable, rollback.")
        else -> getUsers(requestingUser.second)
    }
}
/**
 * Deletes the iser associated with userId if user has permissions, with all of his time entries
 * @param userId of the user requesting this deletion
 * @param requestingUserId of the user requesting this deletion
 * @param requestingUserRole of the user requesting this deletion
 */
suspend fun PipelineContext<Unit, ApplicationCall>.deleteUser(userId: Long, requestingUserRole: Schema.Roles) {
    println("Delete $userId $requestingUserRole")
    var outcome : User? = null
    var errorMessage = ""
    transaction {
        Schema.USER.select { Schema.USER.id eq userId }.singleOrNull().let { result ->
            if (result == null || requestingUserRole != Schema.Roles.ADMIN) {
                errorMessage = when (result) {
                    null -> "No such user"
                    else -> "Only admins can delete users"
                }
            } else {
                println("Found user, deleting")
                Schema.TIME_ENTRIES.deleteWhere { Schema.TIME_ENTRIES.userId eq userId }
                Schema.USER.deleteWhere { Schema.USER.id eq userId }
                outcome = user(result)
            }
        }
    }.apply {
        if (errorMessage != "")
            call.respond(HttpStatusCode.NotFound, errorMessage)
        else if (outcome != null)
            call.respond(outcome!!)
    }
}

fun user(result: ResultRow): User {
    return User(id = result[Schema.USER.id],
            email = result[Schema.USER.email],
            password = result[Schema.USER.password],
            name = result[Schema.USER.name],
            preferredHours =  result[Schema.USER.preferredHours],
            role =  result[Schema.USER.role],
            lastLogin = result[Schema.USER.lastLogin],
            sessionKey = result[Schema.USER.sessionKey])
}