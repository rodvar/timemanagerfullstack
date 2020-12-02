package com.rodvar.timemanager.feature.timelogs

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.rodvar.timemanager.R
import com.rodvar.timemanager.base.BaseFragment
import com.rodvar.timemanager.base.BaseViewModel
import com.rodvar.timemanager.data.domain.TimeLog
import com.rodvar.timemanager.data.repository.BaseRepository
import com.rodvar.timemanager.databinding.FragmentTimeLogsBinding
import com.rodvar.timemanager.feature.report.TimesReportActivity
import com.rodvar.timemanager.utils.DateUtils
import com.rodvar.timemanager.utils.uihelpers.EditTimeEntryDialogHelper
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class TimeLogsFragment : BaseFragment<FragmentTimeLogsBinding>() {

    override val layoutRes: Int
        get() = R.layout.fragment_time_logs

    private val viewModel: TimeLogsViewModel by viewModel()

    private val dateFromListener =
        DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            viewModel.dateFrom.value = DateUtils.toDate(year, monthOfYear, dayOfMonth).time
        }
    private val dateToListener =
        DatePickerDialog.OnDateSetListener { datePicker, year, monthOfYear, dayOfMonth ->
            viewModel.dateTo.value = DateUtils.toDate(year, monthOfYear, dayOfMonth).time
        }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        this.setupEntriesListAdapter()
        this.setupDataObservers()
        this.setupFilters()
    }

    private fun setupFilters() {
        this.dataBinding.dateFromContainer.setOnClickListener {
            (if (this.dateFrom() == null)
                DateUtils.now().time
            else
                this.dateFrom())!!.let { from ->
                this.viewModel.dateFrom.value = null // reset on click
                DatePickerDialog(
                    this.requireContext(), dateFromListener, DateUtils.year(from),
                    DateUtils.month(from),
                    DateUtils.date(from)
                ).let {
                    it.datePicker.maxDate = DateUtils.now().time
                    it.show()
                }
            }
        }
        this.dataBinding.dateToContainer.setOnClickListener {
            (if (this.dateTo() == null)
                DateUtils.now().time
            else
                this.dateTo())!!.let { to ->
                this.viewModel.dateTo.value = null // reset on click
                DatePickerDialog(
                    this.requireContext(), dateToListener, DateUtils.year(to),
                    DateUtils.month(to),
                    DateUtils.date(to)
                ).let {
                    it.datePicker.maxDate = DateUtils.tomorrow().time
                    it.show()
                }
            }
        }
    }

    private fun setupEntriesListAdapter() {
        this.dataBinding.timeEntriesList.apply {
            layoutManager = LinearLayoutManager(this@TimeLogsFragment.context)
            adapter = TimeLogAdapter(this@TimeLogsFragment,
                { _, new ->
                    viewModel.onUpdate(new)
                },
                { timeLogToDelete ->
                    viewModel.onDelete(timeLogToDelete)
                })
            addItemDecoration(
                DividerItemDecoration(
                    this@TimeLogsFragment.context,
                    (layoutManager as LinearLayoutManager).orientation
                )
            )
        }
    }

    private fun setupDataObservers() {
        this.viewModel.timeEntriesLiveData.observe(this.viewLifecycleOwner) { resource ->
            when (resource.status) {
                BaseRepository.Status.SUCCESS -> {
                    this.dataBinding.progressBar.visibility = View.GONE
                    (this.dataBinding.timeEntriesList.adapter as TimeLogAdapter).updateTimeEntries(
                        resource.data!!
                    )
                    if (resource.data.isEmpty())
                        this.dataBinding.noEntriesError.visibility = View.VISIBLE
                    else
                        this.dataBinding.noEntriesError.visibility = View.GONE
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
        BaseViewModel.preferredHours.observe(this.viewLifecycleOwner) { preferredHours ->
            (this.dataBinding.timeEntriesList.adapter as TimeLogAdapter).apply {
                this.preferredHours = preferredHours
                this.notifyDataSetChanged()
            }
        }
        this.viewModel.dateFrom.observe(this.viewLifecycleOwner) { dateFrom ->
            (this.dataBinding.timeEntriesList.adapter as TimeLogAdapter).filterFrom(dateFrom)
            this.dataBinding.dateFrom.text = if (dateFrom == null)
                ""
            else
                DateUtils.format(Date(dateFrom))
        }
        this.viewModel.dateTo.observe(this.viewLifecycleOwner) { dateTo ->
            (this.dataBinding.timeEntriesList.adapter as TimeLogAdapter).filterTo(dateTo)
            this.dataBinding.dateTo.text = if (dateTo == null)
                ""
            else
                DateUtils.format(Date(dateTo))
        }
    }

    override fun onResume() {
        super.onResume()
        (this.requireActivity() as MainActivity).mainMenuMode()
        this.viewModel.updateTimeEntries()
    }

    override fun viewModelInstance(): BaseViewModel = this.viewModel

    fun onCreateTimeEntry() {
        EditTimeEntryDialogHelper().apply {
            title = getString(R.string.alert_title_add_entry)
            showDescription = true
            showHours = true
            showDate = true
            this.date = DateUtils.now().time
            show(this@TimeLogsFragment, {
                toast(String.format(getString(R.string.error_empty_hours), minEntryHours()))
            }, { hours, description, _ ->
                if (hours <= minEntryHours())
                    toast(String.format(getString(R.string.error_empty_hours), minEntryHours()))
                else
                    viewModel.onCreate(
                        TimeLog(
                            time = date,
                            hours = hours,
                            note = description
                        )
                    )
            })
        }
    }

    fun canCrudAll() = this.viewModel.canCrudAll()
    fun dateFrom() = this.viewModel.dateFrom.value
    fun dateTo() = this.viewModel.dateTo.value
    fun minEntryHours() = this.viewModel.minTimeEntryHours
    fun generateReport() {
        this.viewModel.generateReport(dateFrom(), dateTo(), { report ->
            Intent(this.requireContext(), TimesReportActivity::class.java).let { intent ->
                Bundle().let { parameters ->
                    parameters.putString(
                        TimesReportActivity.HTML_REPORT,
                        report
                    )
                    intent.putExtras(parameters)
                    this.startActivity(intent)
                }
            }
        }, {
            it.printStackTrace()
        })
    }

}