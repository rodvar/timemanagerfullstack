package com.rodvar.timemanager.feature.report

import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.rodvar.timemanager.R
import kotlinx.android.synthetic.main.activity_times_report.*

/**
 * Helper Activity that queries the HTML report and shows the data in a web view
 */
class TimesReportActivity : AppCompatActivity() {

    companion object {
        const val HTML_REPORT = "html_report"
    }

    private var report : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
        this.setContentView(R.layout.activity_times_report)
        this.parseParameters()
        Log.d(javaClass.simpleName, "Loading report $report")
        if (report != null)
            this.web_view.loadData(this.report!!, "text/html; charset=utf-8", "UTF-8");
    }

    override fun onSupportNavigateUp(): Boolean {
        this.onBackPressed()
        return true
    }

    override fun onBackPressed() {
        this.web_view?.stopLoading()
        this.finish()
    }


    private fun parseParameters() {
        val b = intent.extras?.let { parameters ->
            parameters.getString(HTML_REPORT)?.let { report ->
                this.report = report
            }
        }
    }
}