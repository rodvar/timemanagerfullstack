package com.rodvar.timemanager.feature.timelogs

import android.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rodvar.timemanager.R
import com.rodvar.timemanager.data.domain.TimeLog
import com.rodvar.timemanager.utils.DateUtils
import com.rodvar.timemanager.utils.uihelpers.EditTimeEntryDialogHelper
import kotlinx.android.synthetic.main.item_time_entry.view.*
import kotlinx.android.synthetic.main.item_time_entry_with_title.view.*


/**
 * Adapter for main recycler view (time entries)
 */
class TimeLogAdapter constructor(
    private var fragment: TimeLogsFragment?,
    private val onUpdate: (old: TimeLog, new: TimeLog) -> Unit,
    private val onDelete: (TimeLog) -> Unit
) : RecyclerView.Adapter<TimeLogAdapter.BaseTimeEntryViewHolder>() {

    private var dateFrom: Long? = null
    private var dateTo: Long? = null

    var preferredHours: Float? = null
    var originalTimeEntries: List<TimeLog>? = null
    var timeEntries: List<TimeLog>? = null
    private lateinit var timeEntriesByDate: Map<String, List<TimeLog>>

    fun updateTimeEntries(timeEntries: List<TimeLog>) {
        this.originalTimeEntries = timeEntries
        this.timeEntries = timeEntries.let { originalList ->
            when {
                dateFrom != null && dateTo != null -> originalList.filter { it.time in dateFrom!!..dateTo!! }
                dateFrom != null -> originalList.filter { it.time >= dateFrom!! }
                dateTo != null -> originalList.filter { it.time <= dateTo!! }
                else -> originalList
            }
        }
        this.timeEntriesByDate = this.timeEntries!!.groupBy { DateUtils.format(it.date()) }
        this.timeEntries = this.timeEntriesByDate.flatMap { (_, value) -> value }
        this.notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
//        Log.d(javaClass.simpleName, "getting view time for position $position")
        getItem(position).let { item ->
            return when (position) {
                0 -> 1
                else -> {
                    if (DateUtils.sameDay(timeEntries!![position - 1].time, item!!.time))
                        0
                    else
                        1
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseTimeEntryViewHolder {
        Log.d(javaClass.simpleName, "new view holder type $viewType")
        return when (viewType) {
            0 -> {
                LayoutInflater.from(this.fragment?.context)
                    .inflate(R.layout.item_time_entry, parent, false).let { view ->
                        BaseTimeEntryViewHolder.TimeEntryViewHolder(view)
                    }
            }
            else -> {
                LayoutInflater.from(this.fragment?.context)
                    .inflate(R.layout.item_time_entry_with_title, parent, false).let { view ->
                        BaseTimeEntryViewHolder.TimeEntryTitleViewHolder(view)
                    }
            }
        }
    }

    override fun onBindViewHolder(holder: BaseTimeEntryViewHolder, position: Int) {
        getItem(position)?.let { timeEntry ->
            if (holder is BaseTimeEntryViewHolder.TimeEntryTitleViewHolder) {
                timeEntry.date().let { date ->
                    holder.dateTitle.text = when {
                        DateUtils.isToday(date) -> "Today (${DateUtils.format(timeEntry.date())})"
                        else -> DateUtils.format(timeEntry.date())
                    }
                }
            }
            (holder as BaseTimeEntryViewHolder.TimeEntryViewHolder).apply {
                this.avatar.setOnClickListener {
                    fragment?.getString(R.string.error_not_admin)?.let { text ->
                        if (fragment?.canCrudAll() == true)
                            fragment?.toast("Task of user: ${timeEntry.userEmail}")
                    }
                }
                this.deleteAction.setOnClickListener {
                    showDeleteConfirmation(timeEntry)
                }
                this.hours.text = String.format("%.2f", timeEntry.hours)
                this.note.text = timeEntry.note

                this.hours.setOnClickListener {
                    showUpdateHours(timeEntry)
                }
                this.note.setOnClickListener {
                    showUpdateNote(timeEntry)
                }

                if (preferredHours != null) {
                    timeEntriesByDate[DateUtils.format(timeEntry.date())]?.fold(0f, { acc, timeLog ->
                        acc + timeLog.hours
                    })?.let { hoursThatDay ->
//                        Log.d("NOW", "date ${DateUtils.format(timeEntry.date())} hours $hoursThatDay preferred $preferredHours")
                        if (hoursThatDay < preferredHours!!)
                            (holder.avatar.parent as View).setBackgroundColor(fragment?.requireContext()!!.resources.getColor(R.color.red))
                        else
                            (holder.avatar.parent as View).setBackgroundColor(fragment?.requireContext()!!.resources.getColor(R.color.green))
                    }
                }
                if (fragment?.canCrudAll() == false)
                    this.avatar.visibility = View.GONE
            }
        }
    }

    private fun showDeleteConfirmation(timeEntry: TimeLog) {
        AlertDialog.Builder(fragment?.context)
            .setTitle(fragment?.getString(R.string.alert_title_confirm_delete))
            .setMessage(fragment?.getString(R.string.alert_message_confirm_delete))
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(
                android.R.string.yes
            ) { dialog, _ ->
                onDelete(timeEntry)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.no, null).show()
    }

    private fun showUpdateHours(timeEntry: TimeLog) {
        this.editTimeEntry(timeEntry,
            fragment?.getString(R.string.alert_title_edit_entry_hours).toString(),
            showHours = true, showDescription = false
        )
    }

    private fun showUpdateNote(timeEntry: TimeLog) {
        this.editTimeEntry(timeEntry,
            fragment?.getString(R.string.alert_title_edit_entry_description).toString(),
            showHours = false, showDescription = true
        )
    }

    private fun editTimeEntry(timeEntry: TimeLog, title: String,
                              showHours : Boolean, showDescription: Boolean) {
        EditTimeEntryDialogHelper().apply {
            this.title = title
            this.showHours = showHours
            this.showDescription = showDescription
            this.showDate = true
            this.date = timeEntry.time
            hours = timeEntry.hours.toString()
            description = timeEntry.note
            show(fragment!!, {
                fragment?.getString(R.string.error_empty_hours)
                    ?.let { fragment?.toast(String.format(it, fragment!!.minEntryHours())) }
            }, { hours, note, time ->
                if (hours <= fragment!!.minEntryHours())
                    fragment?.getString(R.string.error_empty_hours)
                        ?.let { fragment?.toast(String.format(it, fragment!!.minEntryHours())) }
                else
                    timeEntry.clone().apply {
                        this.hours = hours
                        this.note = note
                        this.time = time
                        onUpdate(timeEntry, this)
                    }
            })
        }
    }

    private fun getItem(position: Int): TimeLog? = timeEntries?.get(position)

    override fun getItemCount(): Int {
        return timeEntries?.size ?: 0
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.fragment = null
        super.onDetachedFromRecyclerView(recyclerView)
    }

    fun filterFrom(dateFrom: Long?) {
        this.dateFrom = dateFrom
        if (this.originalTimeEntries != null)
            this.updateTimeEntries(this.originalTimeEntries!!)
    }

    fun filterTo(dateTo: Long?) {
        this.dateTo = dateTo
        if (this.originalTimeEntries != null)
            this.updateTimeEntries(this.originalTimeEntries!!)
    }

    sealed class BaseTimeEntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        open class TimeEntryViewHolder(itemView: View) : BaseTimeEntryViewHolder(itemView) {

            val avatar: ImageView = itemView.avatar
            val hours: TextView = itemView.hours
            val note: TextView = itemView.note
            val deleteAction: TextView = itemView.deleteAction
        }

        class TimeEntryTitleViewHolder(itemView: View) : TimeEntryViewHolder(itemView) {
            val dateTitle: TextView = itemView.dateTitle
        }
    }
}
