package com.rodvar.timemanager.feature.timelogs

import com.rodvar.timemanager.base.BaseViewModel
import com.rodvar.timemanager.data.repository.UserRepository

/**
 * Home screen view model
 */
class MainViewModel(
    private val userRepository: UserRepository
) : BaseViewModel(userRepository) {

}