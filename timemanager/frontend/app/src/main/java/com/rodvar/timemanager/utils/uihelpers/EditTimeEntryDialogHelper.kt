package com.rodvar.timemanager.utils.uihelpers

import android.app.DatePickerDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.DatePicker
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.rodvar.timemanager.R
import com.rodvar.timemanager.utils.DateUtils
import java.util.*

/**
 * Helper to reuse view to add/edit Time entry.
 */
class EditTimeEntryDialogHelper : DatePickerDialog.OnDateSetListener {

    private var view: View? = null

    var title = ""
    var showHours = false
    var showDescription = false
    var showDate = false
    var hours = ""
    var description = ""
    var date = 0L

    /**
     * @param fragment holding the dialog
     * @param onEmptyHours handle input error function
     * @param onEdit receiver function for the user input values
     */
    fun show(fragment: Fragment, onEmptyHours: () -> Unit, onEdit: (Float, String, Long) -> Unit) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(fragment.requireContext())
        builder.setTitle(this.title)
        this.view = LayoutInflater.from(fragment.requireContext())
            .inflate(R.layout.dialog_add_time_log, fragment.view as ViewGroup?, false)
        val inputHours = view!!.findViewById<AutoCompleteTextView>(R.id.hoursInput)
        val inputDescription =
            view!!.findViewById<AutoCompleteTextView>(R.id.descriptionInput)
        val inputDate =
            view!!.findViewById<TextView>(R.id.dateInput)
        inputHours.setText(hours)
        inputDescription.setText(description)
        inputDate.text = DateUtils.format(Date(date))
        inputDate.setOnClickListener {
            DatePickerDialog(
                fragment.requireContext(), this, DateUtils.year(date),
                DateUtils.month(date),
                DateUtils.date(date)
            ).let {
                it.datePicker.maxDate = DateUtils.now().time
                it.show()
            }
        }
        if (!showHours)
            (inputHours.parent as View).visibility = View.GONE
        if (!showDescription)
            (inputDescription.parent as View).visibility = View.GONE
        if (!showDate)
            (inputDate.parent as View).visibility = View.GONE
        builder.setView(view)
        builder.setPositiveButton(android.R.string.ok) { dialog, _ ->
            if (showHours && inputHours.text.isNullOrBlank())
                onEmptyHours()
            else {
                dialog.dismiss()
                onEdit(
                    inputHours.text.toString().toFloat(),
                    inputDescription.text.toString(),
                    date
                )
            }
        }
        builder.setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    override fun onDateSet(p0: DatePicker?, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        DateUtils.toDate(year, monthOfYear, dayOfMonth).let { date ->
            this.date = date.time
            view!!.findViewById<TextView>(R.id.dateInput).text = DateUtils.format(date)
        }
    }
}