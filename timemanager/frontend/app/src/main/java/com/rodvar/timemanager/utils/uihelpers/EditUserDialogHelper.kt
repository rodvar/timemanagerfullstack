package com.rodvar.timemanager.utils.uihelpers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import com.rodvar.timemanager.R
import com.rodvar.timemanager.base.BaseViewModel
import com.rodvar.timemanager.data.domain.Roles
import com.rodvar.timemanager.data.domain.User
import com.rodvar.timemanager.feature.users.UsersFragment
import com.rodvar.timemanager.feature.users.UsersViewModel

/**
 * Helper to reuse view to add/edit User
 */
class EditUserDialogHelper {

    private var view: View? = null

    private var selectedRole: Roles? = null
    var edit = false
    var title = ""
    var userName = ""
    var email = ""
    var role = Roles.REGULAR
    var hours = BaseViewModel.DEFAULT_PREFERRED_HOURS
    var user : User? = null

    /**
     * @param fragment holding the dialog
     * @param onEmptyHours handle input error function
     * @param onEdit receiver function for the user input values
     */
    fun show(fragment: UsersFragment, onAction: (User) -> Unit) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(fragment.requireContext())
        builder.setTitle(title)
        this.view = LayoutInflater.from(fragment.requireContext())
            .inflate(R.layout.dialog_add_user, fragment.view as ViewGroup?, false)
        val inputPreferredHours = view!!.findViewById<AutoCompleteTextView>(R.id.preferredHours)
        val inputName =
            view!!.findViewById<AutoCompleteTextView>(R.id.userName)
        val inputEmail =
            view!!.findViewById<AutoCompleteTextView>(R.id.userEmail)
        val inputPassword =
            view!!.findViewById<AutoCompleteTextView>(R.id.userPassword)
        val inputRole =
            view!!.findViewById<Spinner>(R.id.roles_spinner)

        inputPassword.setOnClickListener {
            inputPassword.setText("")
        }

        if (this.email.isNotEmpty()) {
            inputEmail.setText(this.email)
            inputName.setText(this.userName)
            inputPreferredHours.setText(this.hours.toString())
            inputRole.setSelection(Roles.values().indexOf(this.selectedRole))
        }

        ArrayAdapter.createFromResource(
            fragment.requireContext(),
            R.array.roles_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            inputRole.adapter = adapter
        }
        inputRole.setSelection(0)
        inputRole.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                Roles.values()[pos].let { selectedRole ->
                    this@EditUserDialogHelper.selectedRole = selectedRole
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedRole = null
            }

        }
        builder.setView(view)
        builder.setPositiveButton(android.R.string.ok) { dialog, _ ->
            when {
                inputEmail.text.isNullOrBlank() -> fragment.showError(fragment.getString(R.string.error_empty_user_email))
                (!inputPassword.text.isBlank() && inputPassword.text.length < UsersViewModel.MIN_PASS_CHARS) -> fragment.showError(
                    fragment.getString(R.string.error_empty_user_password)
                )
                else -> {
                    dialog.dismiss()
                    onAction(
                        if (user == null) {
                            User(
                                email = inputEmail.text.toString(),
                                password = if (inputPassword.text.isEmpty()) null else inputPassword.text.toString(),
                                name = inputName.text.toString(),
                                role = selectedRole ?: Roles.REGULAR,
                                preferredHours = inputPreferredHours.text?.toString()?.toFloat()
                                    ?: BaseViewModel.DEFAULT_PREFERRED_HOURS
                            )
                        } else {
                            this.user!!.let {
                                it.email = inputEmail.text.toString()
                                it.password =
                                    if (inputPassword.text.isEmpty()) null else inputPassword.text.toString()
                                it.name = inputName.text.toString()
                                it.role = selectedRole ?: Roles.REGULAR
                                it.preferredHours = inputPreferredHours.text?.toString()?.toFloat()
                                    ?: BaseViewModel.DEFAULT_PREFERRED_HOURS
                                it
                            }
                        }
                    )
                }
            }


        }
        builder.setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }
        builder.show()
    }

}