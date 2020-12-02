package main.kotlin

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.util.pipeline.*
import main.kotlin.db.Schema
import main.kotlin.domain.TimeLog
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.util.Random
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Adds a new time entry to the DB
 * @param timeLog of the time log to eliminate from the db
 * @param userId of the user requesting this deletion
 * @param requestingUser pair of user id (long) and usesr role (Roles)
 */
suspend fun PipelineContext<Unit, ApplicationCall>.addTimeEntry(timeLog: TimeLog, userId: Long, requestingUser : Pair<Long, Schema.Roles>) {
    var unauth = false
    var newTimeLog: TimeLog? = null
    transaction {
        if (userId != requestingUser.first && requestingUser.second != Schema.Roles.ADMIN)
            unauth = true
        Schema.TIME_ENTRIES.insert { insertStatement ->
            insertStatement[date] = DateTime(timeLog.time)
            insertStatement[time] = timeLog.time
            insertStatement[note] = timeLog.note.substring(0, min(timeLog.note.length, Schema.DEFAULT_TEXT_SIZE))
            insertStatement[hours] = timeLog.hours
            insertStatement[Schema.TIME_ENTRIES.userId] = userId
        }.let { entryTx ->
            Schema.TIME_ENTRIES.select { Schema.TIME_ENTRIES.id eq (entryTx.generatedKey?.toLong() ?: 0L) }
                    .singleOrNull()
                    ?.let {
                        newTimeLog = timeEntry(it)
                    }
        }
    }
    when {
        unauth -> call.respond(HttpStatusCode.Unauthorized, "This user cannot add $timeLog")
        newTimeLog == null -> call.respond(HttpStatusCode.NotAcceptable, "Failed to create timelog $timeLog")
        else -> call.respond(newTimeLog!!)
    }
}

/**
 * Generates a success Http response with all the time entries of the given userId (or all of the user if
 * the user role is Admin)
 * @param userId of the requesting user
 * @param role of the requesting user
 * @param dateFrom to filter the result if not null
 * @param dateTo to filter the result if not null
 */
suspend fun PipelineContext<Unit, ApplicationCall>.getTimeEntries(userId: Long, role: Schema.Roles,
                                                                  dateFrom: Long = -1L, dateTo: Long = -1L) {
    var allOutcomes: List<TimeLog> = ArrayList()
    transaction {
        try {
            allOutcomes = getTimeEntriesQuery(userId, role, dateFrom, dateTo)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }.apply {
        call.respond(allOutcomes)
    }
}

fun getTimeEntriesQuery(userId: Long, role: Schema.Roles, dateFrom: Long, dateTo: Long): List<TimeLog> {
    return (when {
        (role == Schema.Roles.ADMIN) -> {
            if (dateFrom < 0 && dateTo < 0)
                Schema.TIME_ENTRIES.selectAll()
            else
                Schema.TIME_ENTRIES.select {
                    when {
                        dateFrom >= 0 && dateTo >= 0 -> (Schema.TIME_ENTRIES.time greaterEq dateFrom) and (Schema.TIME_ENTRIES.time lessEq dateTo)
                        dateFrom >= 0 -> (Schema.TIME_ENTRIES.time greaterEq dateFrom)
                        else -> (Schema.TIME_ENTRIES.time lessEq dateTo!!)
                    }
                }
        }
        else -> Schema.TIME_ENTRIES.select {
            when {
                dateFrom < 0 && dateTo < 0 -> Schema.TIME_ENTRIES.userId eq userId
                dateFrom >= 0 && dateTo >= 0 -> ((Schema.TIME_ENTRIES.userId eq userId) and (Schema.TIME_ENTRIES.time greaterEq dateFrom) and (Schema.TIME_ENTRIES.time lessEq dateTo))
                dateFrom >= 0 -> ((Schema.TIME_ENTRIES.userId eq userId) and (Schema.TIME_ENTRIES.time greaterEq dateFrom))
                else -> ((Schema.TIME_ENTRIES.userId eq userId) and (Schema.TIME_ENTRIES.time lessEq dateTo!!))
            }
        }
    }).orderBy(Schema.TIME_ENTRIES.time to SortOrder.DESC).map { result ->
        timeEntry(result)
    }
}

/**
 * Update the time log associated with entryId if user has permissions.
 * @param timeLog to update
 * @param userId of the user requesting this deletion
 * @param requestingUser of the user requesting this deletion
 */
suspend fun PipelineContext<Unit, ApplicationCall>.updateTimeEntry(timeLog: TimeLog, userId: Long, requestingUser : Pair<Long, Schema.Roles>) {
    var unauth = false
    transaction {
        if (timeLog.id == null || (userId != requestingUser.first && requestingUser.second != Schema.Roles.ADMIN))
            unauth = true
        Schema.TIME_ENTRIES.update({ Schema.TIME_ENTRIES.id eq timeLog.id!! }) { updateStatement ->
            updateStatement[note] = timeLog.note.substring(0, min(timeLog.note.length, Schema.DEFAULT_TEXT_SIZE))
            updateStatement[hours] = timeLog.hours
            updateStatement[time] = timeLog.time
            updateStatement[date] = DateTime(timeLog.time)
            if (timeLog.userId != null)
                updateStatement[Schema.TIME_ENTRIES.userId] = timeLog.userId!!
        }
    }
    when {
        unauth -> call.respond(HttpStatusCode.Unauthorized, "This user cannot add $timeLog")
        else -> getTimeEntries(requestingUser.first, requestingUser.second)
    }
}

/**
 * Deletes the time log associated with entryId if user has permissions.
 * @param entryId of the time log to eliminate from the db
 * @param userId of the user requesting this deletion
 * @param userRole of the user requesting this deletion
 */
suspend fun PipelineContext<Unit, ApplicationCall>.deleteTimeEntry(entryId: Long, userId: Long, userRole: Schema.Roles) {
    println("Delete $entryId $userRole")
    var outcome : TimeLog? = null
    var errorMessage = ""
    transaction {
        Schema.TIME_ENTRIES.select { Schema.TIME_ENTRIES.id eq entryId }.singleOrNull().let { result ->
            if (result == null ||
                    (userRole != Schema.Roles.ADMIN && result[Schema.TIME_ENTRIES.userId] != userId)) {
                errorMessage = when (result) {
                    null -> "No such time entry"
                    else -> "Cannot delete an entry that doesn't belong to you when you are not admin"
                }
            } else {
                println("Found time entry, deleting")
                Schema.TIME_ENTRIES.deleteWhere { Schema.TIME_ENTRIES.id eq entryId }
                outcome = timeEntry(result)
            }
        }
    }.apply {
        if (errorMessage != "")
            call.respond(HttpStatusCode.NotFound, errorMessage)
        else if (outcome != null)
            call.respond(outcome!!)
    }
}
/*******************************/

/** HELPER METHODS **/
/**
 * Transforms a db time entry result into the TimeEntry KOJO.
 */
private fun timeEntry(result: ResultRow): TimeLog {
    return TimeLog(id = result[Schema.TIME_ENTRIES.id],
            userEmail = Schema.USER.select { Schema.USER.id eq result[Schema.TIME_ENTRIES.userId] }.single()[Schema.USER.email],
            time = result[Schema.TIME_ENTRIES.time],
            hours = result[Schema.TIME_ENTRIES.hours],
            note = result[Schema.TIME_ENTRIES.note])
}