package com.rodvar.timemanager.data.api

import com.rodvar.timemanager.data.domain.TimeLog
import com.rodvar.timemanager.data.domain.User
import retrofit2.http.*

interface TimeManagerAPI {
    
    @GET("times")
    suspend fun getTimeLogsAsync(): List<TimeLog>

    @POST("times")
    suspend fun addTimeLogAsync(@Body timeLog: TimeLog) : TimeLog

    @PUT("times")
    suspend fun updateTimeLogAsync(@Body timeLog: TimeLog) : List<TimeLog>

    @DELETE("times/{id}")
    suspend fun deleteTimeLogAsync(@Path("id") id: Long) : TimeLog


    @GET("users")
    suspend fun getUsersAsync(): List<User>

    @POST("users")
    suspend fun addUserAsync(@Body User: User) : User

    @PUT("users")
    suspend fun updateUserAsync(@Body User: User) : List<User>

    @DELETE("users/{id}")
    suspend fun deleteUserAsync(@Path("id") id: Long) : User

    @GET("report")
    suspend fun report(@Query("dateFrom") dateFrom: Long?, @Query("dateTo") dateTo: Long?): String

    @POST("users/login")
    suspend fun login(@Body user: User) : User

    @POST("users/logout")
    suspend fun logout() : String
}