package com.rodvar.timemanager.feature.users

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.rodvar.timemanager.R
import com.rodvar.timemanager.base.BaseFragment
import com.rodvar.timemanager.base.BaseViewModel
import com.rodvar.timemanager.base.BaseViewModel.Companion.DEFAULT_PREFERRED_HOURS
import com.rodvar.timemanager.data.domain.Roles
import com.rodvar.timemanager.data.domain.User
import com.rodvar.timemanager.data.repository.BaseRepository
import com.rodvar.timemanager.databinding.FragmentUsersBinding
import com.rodvar.timemanager.feature.timelogs.MainActivity
import com.rodvar.timemanager.utils.DateUtils
import com.rodvar.timemanager.utils.uihelpers.EditUserDialogHelper
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class UsersFragment : BaseFragment<FragmentUsersBinding>() {

    override val layoutRes: Int
        get() = R.layout.fragment_users
    private val viewModel: UsersViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        this.setupUsersAdapter()
        this.setupDataObservers()
    }

    private fun setupUsersAdapter() {
        this.dataBinding.usersList.apply {
            layoutManager = LinearLayoutManager(this@UsersFragment.context)
            adapter = UsersAdapter(this@UsersFragment,
                { new ->
                    viewModel.onUpdate(new)
                },
                { userToDelete ->
                    viewModel.onDelete(userToDelete)
                })
            addItemDecoration(
                DividerItemDecoration(
                    this@UsersFragment.context,
                    (layoutManager as LinearLayoutManager).orientation
                )
            )
        }
    }

    private fun setupDataObservers() {
        this.viewModel.usersLiveData.observe(this.viewLifecycleOwner) { resource ->
            when (resource.status) {
                BaseRepository.Status.SUCCESS -> {
                    this.dataBinding.progressBar.visibility = View.GONE
                    (this.dataBinding.usersList.adapter as UsersAdapter).updateTimeEntries(
                        resource.data!!
                    )
                }
                BaseRepository.Status.ERROR -> {
                    this.dataBinding.progressBar.visibility = View.GONE
                    resource.message?.let { this.toast(resource.message) }
                }
                BaseRepository.Status.LOADING -> {
                    this.dataBinding.progressBar.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(javaClass.simpleName, "Resumed User Fragment")
        (this.requireActivity() as MainActivity).subMenuMode()
        this.viewModel.updateUsers()
    }

    fun onCreateUser() {
        EditUserDialogHelper().apply {
            title = getString(R.string.alert_title_add_user)
            show(this@UsersFragment) {
                viewModel.onCreate(it)
            }
        }
    }

    fun showError(message: String) {
        toast(message)
    }

    override fun viewModelInstance(): BaseViewModel = this.viewModel

}