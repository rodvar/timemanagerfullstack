package com.rodvar.timemanager.feature.users

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.rodvar.timemanager.base.BaseViewModel
import com.rodvar.timemanager.data.domain.User
import com.rodvar.timemanager.data.repository.BaseRepository
import com.rodvar.timemanager.data.repository.UserRepository
import kotlinx.coroutines.launch

/**
 * Home screen view model
 */
class UsersViewModel(private val usersRepository: UserRepository) : BaseViewModel(usersRepository) {

    companion object {
        const val MIN_PASS_CHARS = 6
    }

    val usersLiveData = MutableLiveData<BaseRepository.Resource<List<User>>>()

    fun updateUsers() {
        viewModelScope.launch {
            usersLiveData.value = BaseRepository.Resource.loading(usersLiveData.value?.data)
            usersRepository.getAll({
                Log.d(javaClass.simpleName, "fetched $it")
                usersLiveData.value = BaseRepository.Resource.success(it)
            }, {
                Log.e(javaClass.simpleName, "Failed to get users ${it.localizedMessage}")
                usersLiveData.value = BaseRepository.Resource.error(null, it.localizedMessage)
            })
        }
    }

    fun onDelete(userToDelete: User) {
        viewModelScope.launch {
            usersLiveData.value?.data?.toMutableList()?.let { currentList ->
                usersLiveData.value = BaseRepository.Resource.loading(currentList)
                usersRepository.delete(userToDelete,
                    {
                        currentList.remove(userToDelete)
                        usersLiveData.value = BaseRepository.Resource.success(currentList)
                    },
                    {
                        usersLiveData.value = BaseRepository.Resource.error(currentList, it.localizedMessage)
                    })
            }
        }
    }

    fun onCreate(user: User) {
        viewModelScope.launch {
            usersLiveData.value?.data?.toMutableList()?.let { currentList ->
                usersLiveData.value = BaseRepository.Resource.loading(currentList)
                usersRepository.add(user,
                    {
                        currentList.add(it)
                        usersLiveData.value = BaseRepository.Resource.success(currentList)
                    },
                    {
                        usersLiveData.value =
                            BaseRepository.Resource.error(currentList, it.localizedMessage)
                    })
            }
        }
    }

    fun onUpdate(user: User) {
        viewModelScope.launch {
            usersLiveData.value?.data?.let { currentList ->
                usersLiveData.value = BaseRepository.Resource.loading(currentList)
                usersRepository.update(
                    user,
                    {
                        preferredHours.value = user.preferredHours
                        usersLiveData.value = BaseRepository.Resource.success(it)
                    },
                    {
                        usersLiveData.value =
                            BaseRepository.Resource.error(currentList, it.localizedMessage)
                    })
            }
//            viewModelScope.launch {
//                userRepository.updateLoggedUserPreferredHours(newPreferredHours,
//                    {
//                        preferredHours.value = newPreferredHours
//                        onResponse(true)
//                    },
//                    {
//                        Log.e(javaClass.simpleName, "Failed to update preferred hours", it)
//                        onResponse(false)
//                    })
//            }
        }
    }

}