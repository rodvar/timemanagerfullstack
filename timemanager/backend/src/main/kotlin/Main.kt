package main.kotlin

import at.favre.lib.crypto.bcrypt.BCrypt
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import main.kotlin.db.Schema
import main.kotlin.db.Schema.DEFAULT_PREFERRED_TIME
import main.kotlin.db.Schema.TIME_ENTRIES
import main.kotlin.db.Schema.TIME_ENTRIES.index
import main.kotlin.db.Schema.USER
import main.kotlin.db.Security
import main.kotlin.domain.TimeLog
import main.kotlin.domain.User
import main.kotlin.utils.DateUtils
import org.celtric.kotlin.html.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.event.Level
import java.text.SimpleDateFormat
import java.util.Date

// Server config
const val PORT = 8080
const val DEFAULT_DATE_PATTERN = "yyyy-MM-dd"
const val API_KEY_KEY = "x-api-key"

lateinit var db : HikariDataSource

/**
 * Starting point of the server
 */
fun main() {
    embeddedServer(Netty, port = PORT) {
        install(ContentNegotiation) {
            gson {
                setPrettyPrinting()
                setDateFormat(DEFAULT_DATE_PATTERN)
            }
        }
        install(CallLogging) {
            level = Level.TRACE
        }
        install(DataConversion) {
            convert<Date> { // this: DelegatingConversionService
                val format = SimpleDateFormat(DEFAULT_DATE_PATTERN)

                decode { values, _ -> // converter: (values: List<String>, type: Type) -> Any?
                    values.singleOrNull()?.let { format.parse(it) }
                }

                encode { value -> // converter: (value: Any?) -> List<String>
                    when (value) {
                        null -> listOf()
                        is Date -> listOf(SimpleDateFormat(DEFAULT_DATE_PATTERN).format(value))
                        else -> throw DataConversionException("Cannot convert $value as Date")
                    }
                }
            }
        }
        routing {
            /** ROUTING TIMES **/
            post("times") {
                try {
                    val user = this.validateSession()
                    if (user.first > 0)
                        call.receive<TimeLog>().let { timeLog ->
                            println("Request to add time log $timeLog")
                            addTimeEntry(timeLog, user.first, user)
                        }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            get("times") {
                try {
                    val user = this.validateSession()

//                    if (firstRequest) {
//                        firstRequest = false
//                        addRandomEntries(user)
//                    } else {
                        if (user.first > 0)
                            getTimeEntries(user.first, user.second)
//                    }
                } catch (e : Exception) {
                    e.printStackTrace()
                }
            }
            put("times") {
                try {
                    val user = this.validateSession()
                    if (user.first > 0)
                        call.receive<TimeLog>().let { timeLog ->
                            println("Request to update time log $timeLog")
                            updateTimeEntry(timeLog, user.first, user)
                        }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            delete("times/{id}") {
                try {
                    val user = this.validateSession()
                    if (user.first > 0)
                        call.parameters["id"].let {
                            if (it == null)
                                call.respond(HttpStatusCode.NotFound, "No entry with id $it")
                            else
                                deleteTimeEntry(it.toLong(), user.first, user.second)
                        }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            /******************/

            /** ROUTING USERS**/
            post("users") {
                try {
                    call.receive<User>().let { user ->
                        println("Request to add user $user")
                        addUser(user)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            get("users") {
                    try {
                        val user = this.validateSession()
                        if (user.first > 0)
                            getUsers(user.second)
                    } catch (e : Exception) {
                        e.printStackTrace()
                    }
//                }
            }
            put("users") {
                try {
                    val sessionUser = this.validateSession()
                    if (sessionUser.first > 0)
                        call.receive<User>().let { user ->
                            println("Request to update user $user")
                            updateUser(user, sessionUser)
                        }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            delete("users/{id}") {
                try {
                    val user = this.validateSession()
                    if (user.first > 0)
                        call.parameters["id"].let {
                            if (it == null)
                                call.respond(HttpStatusCode.NotFound, "No user with id $it")
                            else
                                deleteUser(it.toLong(), user.second)
                        }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            // FIRE AND FORGET logout
            post ("users/logout") {
                try {
                    val sessionUser = this.validateSession()
                    if (sessionUser.first > 0) {
                        var logout = false
                        transaction {
                            USER.update({ USER.id eq sessionUser.first }) { updateStatement ->
                                DateUtils.now().let {now ->
                                    updateStatement[lastLogin] = null
                                    updateStatement[sessionKey] = null
                                }
                            }
                            logout = true
                        }.apply {
                            if (logout)
                                call.respond(HttpStatusCode.OK, "Logged out")
                            else
                                call.respond(HttpStatusCode.Unauthorized, "Logout failed for the logged user")
                        }
                    }
                } catch (e: Exception) {
                    println("Failed to loout")
                    e.printStackTrace()
                }
            }
            post("users/login") {
                try {
                    call.receive<User>().let { user ->
                        println("Requesting to login $user")
                        findUser(user.email).let { foundUser ->
                            var loggedUser : User? = null
                            var error = ""
                            transaction {
                                when {
                                    foundUser == null -> error = "No user matching the provided credentials"
                                    foundUser.password != Security.encrypt(user.password) -> error = "No user matching the provided credentials"
                                    else -> {
                                        loggedUser = foundUser
                                        loggedUser!!.password = ""
                                        DateUtils.now().let { now ->
                                            loggedUser!!.lastLogin = now.time
                                            loggedUser!!.sessionKey = Security.encryptRandom("${loggedUser!!.email}${now.time}")
                                        }
                                        USER.update({ USER.id eq loggedUser!!.id!! }) { updateStatement ->
                                            updateStatement[lastLogin] = loggedUser!!.lastLogin
                                            updateStatement[sessionKey] = loggedUser!!.sessionKey
                                        }
                                    }
                                }
                            }.apply {
                                if (error.isEmpty())
                                    call.respond(foundUser!!)
                                else
                                    call.respond(HttpStatusCode.Unauthorized, error)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            /******************/

            /***** REPORT *****/
            get("report") {
                try {
                    val sessionUser = this.validateSession()
                    if (sessionUser.first > 0) {
                        (call.parameters["dateFrom"]?.toLong() ?: -1L).let { dateFromTimestamp ->
                            (call.parameters["dateTo"]?.toLong() ?: -1L).let { dateToTimestamp ->
                                println("Generating times report for user ${sessionUser.first} with dates $dateFromTimestamp - $dateToTimestamp")
                                // filter user times considering the date timestamp if its >= 0 and generate the HTML
                                lateinit var outcomes: List<TimeLog>
                                transaction {
                                    outcomes = getTimeEntriesQuery(sessionUser.first, sessionUser.second, dateFromTimestamp, dateToTimestamp)
                                }.apply {
                                    generateReport(outcomes).render().let { html ->
                                        println("Generated HTML\n$html")
                                        call.respondText(html, ContentType.Text.Html)
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }.apply {
        // TODO setup persistant DB and only initialise if empty
        connectDB()
        transaction {
            create(USER)
            create(TIME_ENTRIES)
        }
        this.start(wait = true)
    }
}

fun connectDB() {
//    Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
    HikariConfig().apply {
        jdbcUrl = "jdbc:mysql://localhost:3306/timemanager?serverTimezone=Australia/Sydney"
        username = "timemanager"
        password = "timemanager"
        driverClassName = "com.mysql.cj.jdbc.Driver"
        maximumPoolSize = 3

//        driverClassName = "com.mysql.jdbc.Driver" //deprecated

        db = HikariDataSource(this)
        Database.connect(db)
    }
}

private fun generateReport(timeEntries: List<TimeLog>): List<Node> {
//    println(timeEntries)
    val rawContent = MutableList<List<Node>>(0) { listOf() }
            timeEntries.groupBy { DateUtils.format(it.date()) }.forEach { (date, entries) ->
                rawContent.add(
                        p {
                            "<b>Date</b>: $date<br>" +
                            "<b>Total time</b>: ${String.format("%.2f", entries.fold(0F) {acc, timeLog -> acc + timeLog.hours})}h<br>" +
                            "<b>Notes</b>:<br>"
                        } +
                        ul {
                            entries.map { li(it.note) }
                        })
            }
    val content = rawContent.fold<List<Node>, List<Node>>(listOf()) { acc, nodes ->
        acc + nodes
    }
    return doctype("html") + html {
        head {
            title("Time Entries Report") +
                    meta(charset = "utf-8")
        } +
        body {
            div(classes = "container") {
                h1("Your filtered time entries") + content
            }
        }
    }
}

/**
 * @return the session hash in the incoming request
 */
private fun PipelineContext<Unit, ApplicationCall>.requestSessionHash() = call.request.headers[API_KEY_KEY]

/**
 * Validates that the incoming API request has a valid session and if so returns the details of the involved
 * user.
 * Otherwise will terminate the interaction by responding with 403 (Forbidden)
 * @return pair with user id (first) and user role (second)
 */
private suspend fun PipelineContext<Unit, ApplicationCall>.validateSession(): Pair<Long, Schema.Roles> {
    requestSessionHash().let { sessionHash ->
        println("Validate Session hash: $sessionHash")
        var unauth = false
        var user : Pair<Long, Schema.Roles> = Pair(-1L, Schema.Roles.REGULAR)
        if (sessionHash == null)
            unauth = true
        transaction {
            if (sessionHash == "ALL") // for testing
                user = Pair(1, Schema.Roles.ADMIN)
            else {
                USER.selectAll().map { user(it) }.forEach { println("hash ${it.sessionKey}")}
                USER.select { USER.sessionKey eq sessionHash }.singleOrNull().let { resultRow ->
                    if (resultRow == null)
                        unauth = true
                    else
                        user = Pair(resultRow[USER.id], resultRow[USER.role])
                }
            }
        }.apply {
            if (unauth) {
                println("Session hash not found")
                call.respond(HttpStatusCode.Unauthorized, "No session with hash $sessionHash")
            }
        }
        return user
    }
}