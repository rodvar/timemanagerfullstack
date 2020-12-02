package com.rodvar.timemanager.data.repository

import com.rodvar.timemanager.data.api.TimeManagerAPI
import com.rodvar.timemanager.data.domain.TimeLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TimeLogRepository(private val timeManagerAPI: TimeManagerAPI) : BaseRepository() {

    /**
     * @return all the time entries available for the logged user. If the user is an admin it will
     * contain all of the time entries
     */
    suspend fun getAll(
        onSuccess: (timeLog: List<TimeLog>) -> Unit,
        onError: (e: Exception) -> Unit
    ) {
        try {
            this.timeManagerAPI.getTimeLogsAsync().let {
                withContext(Dispatchers.Main) {
                    onSuccess(it)
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError(e)
            }
        }
    }

    suspend fun delete(timeLogToDelete: TimeLog,
                       onSuccess: (timeLog: TimeLog) -> Unit,
                       onError: (e: Exception) -> Unit) {
        try {
            this.timeManagerAPI.deleteTimeLogAsync(timeLogToDelete.id!!)
            onSuccess(timeLogToDelete)
        } catch (e: Exception) {
            onError(e)
        }
    }

    suspend fun add(timeLog: TimeLog,
                       onSuccess: (TimeLog) -> Unit,
                       onError: (e: Exception) -> Unit) {
        try {
            onSuccess(this.timeManagerAPI.addTimeLogAsync(timeLog))
        } catch (e: Exception) {
            onError(e)
        }
    }

    suspend fun update(timeLog: TimeLog,
                    onSuccess: (List<TimeLog>) -> Unit,
                    onError: (e: Exception) -> Unit) {
        try {
            onSuccess(this.timeManagerAPI.updateTimeLogAsync(timeLog))
        } catch (e: Exception) {
            onError(e)
        }
    }

    suspend fun report(dateFrom: Long?, dateTo: Long?,
               onSuccess: (html: String) -> Unit,
               onError: (e: Exception) -> Unit) {
        try {
            onSuccess(this.timeManagerAPI.report(dateFrom, dateTo))
        } catch (e: Exception) {
            onError(e)
        }
    }

}