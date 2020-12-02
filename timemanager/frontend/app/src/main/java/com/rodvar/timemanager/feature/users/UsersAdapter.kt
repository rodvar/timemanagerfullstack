package com.rodvar.timemanager.feature.users

import android.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rodvar.timemanager.R
import com.rodvar.timemanager.data.domain.Roles
import com.rodvar.timemanager.data.domain.TimeLog
import com.rodvar.timemanager.data.domain.User
import com.rodvar.timemanager.utils.DateUtils
import com.rodvar.timemanager.utils.uihelpers.EditTimeEntryDialogHelper
import com.rodvar.timemanager.utils.uihelpers.EditUserDialogHelper
import kotlinx.android.synthetic.main.item_time_entry.view.*
import kotlinx.android.synthetic.main.item_time_entry.view.deleteAction
import kotlinx.android.synthetic.main.item_time_entry_with_title.view.*
import kotlinx.android.synthetic.main.item_user.view.*


/**
 * Adapter for main recycler view (time entries)
 */
class UsersAdapter constructor(
    private var fragment: UsersFragment?,
    private val onUpdate: (new: User) -> Unit,
    private val onDelete: (User) -> Unit
) : RecyclerView.Adapter<UsersAdapter.UserViewHolder>() {

    var users: List<User>? = null

    fun updateTimeEntries(users: List<User>) {
        this.users = users
        this.notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        LayoutInflater.from(this.fragment?.context)
            .inflate(R.layout.item_user, parent, false).let { view ->
                return UserViewHolder(view)
            }
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        getItem(position)?.let { user ->
            holder.userName.text = user.name
            holder.userEmail.text = user.email
            holder.preferredHours.text = user.preferredHours.toString()
            holder.role.text = user.role.toString()
            holder.deleteAction.setOnClickListener {
                showDeleteConfirmation(user)
            }
            (holder.userEmail.parent as View).setOnClickListener {
                showEditUser(user)
            }
            if (user.id == fragment?.loggedUser()?.id)
                fragment?.resources?.getColor(R.color.grey)?.let {
                    (holder.userEmail.parent as View).setBackgroundColor(
                        it
                    )
                }
        }
    }

    private fun showEditUser(user: User) {
        EditUserDialogHelper().apply {
            this.title = fragment!!.getString(R.string.alert_title_edit_user)
            this.edit = true
            this.email = user.email
            this.userName = user.name
            this.role = user.role ?: Roles.REGULAR
            this.hours = user.preferredHours
            this.user = user
            this.show(fragment!!) {
                onUpdate(it)
            }
        }
    }

    private fun showDeleteConfirmation(user: User) {
        AlertDialog.Builder(fragment?.context)
            .setTitle(fragment?.getString(R.string.alert_title_confirm_delete))
            .setMessage(fragment?.getString(R.string.alert_message_confirm_delete))
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(
                android.R.string.yes
            ) { dialog, _ ->
                onDelete(user)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.no, null).show()
    }

    private fun getItem(position: Int): User? = this.users?.get(position)

    override fun getItemCount(): Int {
        return users?.size ?: 0
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.fragment = null
        super.onDetachedFromRecyclerView(recyclerView)
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var userName: TextView = itemView.userName
        var userEmail: TextView = itemView.userEmail
        var preferredHours: TextView = itemView.preferredHours
        var role: TextView = itemView.role
        var deleteAction: TextView = itemView.deleteAction

    }
}
