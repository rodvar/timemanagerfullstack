package com.rodvar.timemanager.base

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.databinding.library.baseAdapters.BR
import androidx.fragment.app.Fragment
import com.rodvar.timemanager.R
import com.rodvar.timemanager.data.domain.User
import com.rodvar.timemanager.feature.login.LoginActivity

abstract class BaseFragment<VDB : ViewDataBinding> : Fragment() {

    @get:LayoutRes
    protected abstract val layoutRes: Int

    protected lateinit var dataBinding: VDB
    open val bindingVariable: Int = BR.viewModel

    @CallSuper
    override fun onResume() {
        super.onResume()
        this.viewModelInstance().onResume()
        this.viewModelInstance().fetchUser().let {  user ->
            if (user == null)
                this.navigateLogin()
        }
    }

    fun navigateLogin() {
        Intent(this.requireContext(), LoginActivity::class.java).let {
            this.startActivity(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dataBinding = DataBindingUtil.inflate(inflater, layoutRes, container, false)
        dataBinding.lifecycleOwner = this
        return dataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dataBinding.setVariable(bindingVariable, viewModelInstance())
        dataBinding.executePendingBindings()
    }

    abstract fun viewModelInstance(): BaseViewModel

    fun toast(message: String) {
        Toast.makeText(this.context, message, Toast.LENGTH_SHORT).show()
    }

    fun showSettings() {
        AlertDialog.Builder(this.requireContext()).let { builder ->
            builder.setTitle(R.string.settings_title)
            val view = LayoutInflater.from(this.requireContext())
                .inflate(R.layout.dialog_setting_preferred_hours, this.view as ViewGroup?, false)
            val inputHours = view!!.findViewById<AutoCompleteTextView>(R.id.preferredHoursInput)
            inputHours.setText(BaseViewModel.preferredHours.value.toString())
            builder.setView(view)
            builder.setPositiveButton(android.R.string.ok) { dialog, _ ->
                when {
                    inputHours.text.isNullOrBlank() -> toast(getString(R.string.error_empty_preferred_hours))
                    inputHours.text.toString()
                        .toFloat() > viewModelInstance().maxHoursPerDay -> toast(
                        String.format(
                            getString(
                                R.string.error_max_preferred_hours
                            ), viewModelInstance().maxHoursPerDay.toString()
                        )
                    )
                    inputHours.text.toString()
                        .toFloat() < viewModelInstance().minHoursPerDay -> toast(
                        String.format(
                            getString(
                                R.string.error_min_preferred_hours
                            ), viewModelInstance().maxHoursPerDay.toString()
                        )
                    )
                    else -> {
                        this.viewModelInstance().onPreferredHoursUpdate(
                            inputHours.text.toString().toFloat()
                        ) { success ->
                            if (success)
                                dialog.dismiss()
                            else
                                toast(getString(R.string.error_update_preferred_hours))
                        }
                    }
                }
            }
            builder.setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }
            builder.show()
        }
    }

    fun loggedUser(): User? = this.viewModelInstance().loggedUser()
}