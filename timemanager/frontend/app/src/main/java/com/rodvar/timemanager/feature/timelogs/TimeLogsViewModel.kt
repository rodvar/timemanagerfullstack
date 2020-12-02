package com.rodvar.timemanager.feature.timelogs

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.rodvar.timemanager.base.BaseViewModel
import com.rodvar.timemanager.data.domain.TimeLog
import com.rodvar.timemanager.data.repository.BaseRepository
import com.rodvar.timemanager.data.repository.TimeLogRepository
import com.rodvar.timemanager.data.repository.UserNotLoggedInException
import com.rodvar.timemanager.data.repository.UserRepository
import kotlinx.coroutines.launch

/**
 * Home screen view model
 */
class TimeLogsViewModel(
    private val timeEntriesRepository: TimeLogRepository,
    private val userRepository: UserRepository
) : BaseViewModel(userRepository) {

    val timeEntriesLiveData = MutableLiveData<BaseRepository.Resource<List<TimeLog>>>()
    val dateFrom = MutableLiveData<Long?>(null)
    val dateTo = MutableLiveData<Long?>(null)

    fun updateTimeEntries() {
        viewModelScope.launch {
            try {
                doUpdateTimeEntries()
            } catch (e: UserNotLoggedInException) {
                timeEntriesLiveData.value = BaseRepository.Resource.error(timeEntriesLiveData.value?.data, e.localizedMessage)
            }
        }
    }

    private suspend fun doUpdateTimeEntries() {
        timeEntriesLiveData.value = BaseRepository.Resource.loading(timeEntriesLiveData.value?.data)
        timeEntriesRepository.getAll({
            Log.d(javaClass.simpleName, "fetched $it")
            timeEntriesLiveData.value = BaseRepository.Resource.success(it)
        }, {
            Log.e(javaClass.simpleName, "Failed to get time entries ${it.localizedMessage}")
            timeEntriesLiveData.value = BaseRepository.Resource.error(null, it.localizedMessage)
        })
    }

    fun onDelete(timeLogToDelete: TimeLog) {
        viewModelScope.launch {
            timeEntriesLiveData.value?.data?.toMutableList()?.let { currentList ->
                timeEntriesLiveData.value = BaseRepository.Resource.loading(currentList)
                timeEntriesRepository.delete(timeLogToDelete,
                    {
                        currentList.remove(timeLogToDelete)
                        timeEntriesLiveData.value = BaseRepository.Resource.success(currentList)
                    },
                    {
                        timeEntriesLiveData.value = BaseRepository.Resource.error(currentList, it.localizedMessage)
                    })
            }
        }
    }

    fun onCreate(timeLog: TimeLog) {
        viewModelScope.launch {
            timeEntriesLiveData.value?.data?.toMutableList()?.let { currentList ->
                timeEntriesLiveData.value = BaseRepository.Resource.loading(currentList)
                timeEntriesRepository.add(timeLog,
                    {
                        currentList.add(it)
                        timeEntriesLiveData.value = BaseRepository.Resource.success(currentList)
                    },
                    {
                        timeEntriesLiveData.value =
                            BaseRepository.Resource.error(currentList, it.localizedMessage)
                    })
            }
        }
    }

    fun onUpdate(newTimeLog: TimeLog) {
        viewModelScope.launch {
            timeEntriesLiveData.value?.data?.let { currentList ->
                timeEntriesLiveData.value = BaseRepository.Resource.loading(currentList)
                timeEntriesRepository.update(
                    newTimeLog,
                    {
                        timeEntriesLiveData.value = BaseRepository.Resource.success(it)
                    },
                    {
                        timeEntriesLiveData.value =
                            BaseRepository.Resource.error(currentList, it.localizedMessage)
                    })
            }
        }
    }

    fun canCrudAll(): Boolean = this.userRepository.canCrudAll()

    fun generateReport(dateFrom: Long?, dateTo: Long?,
                       onSuccess: (html : String) -> Unit,
                       onError: (e : Exception) -> Unit) {
        viewModelScope.launch {
            timeEntriesLiveData.value = BaseRepository.Resource.loading(timeEntriesLiveData.value?.data)
            timeEntriesRepository.report(dateFrom, dateTo, {
                Log.d(javaClass.simpleName, "fetched $it")
                timeEntriesLiveData.value = BaseRepository.Resource.success(timeEntriesLiveData.value?.data
                        ?: listOf())
                onSuccess(it)
            }, {
                Log.e(javaClass.simpleName, "Failed to generate report $dateFrom $dateTo ${it.localizedMessage}")
                timeEntriesLiveData.value = BaseRepository.Resource.success(timeEntriesLiveData.value?.data
                        ?: listOf())
                onError(it)
            })
        }
    }

}