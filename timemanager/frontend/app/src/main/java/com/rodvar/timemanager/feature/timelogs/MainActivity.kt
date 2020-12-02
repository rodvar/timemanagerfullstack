package com.rodvar.timemanager.feature.timelogs

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.rodvar.timemanager.R
import com.rodvar.timemanager.base.BaseFragment
import com.rodvar.timemanager.feature.report.TimesReportActivity
import com.rodvar.timemanager.feature.users.UsersFragment
import com.rodvar.timemanager.utils.uihelpers.lifecycleOwner
import org.koin.java.KoinJavaComponent.inject
import java.lang.Exception


class MainActivity : AppCompatActivity() {

    private var menu: Menu? = null
    private val viewModel: MainViewModel by inject(MainViewModel::class.java)

    private var showMainScreenActions = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
            this.currentFragment().let { fragment ->
                when (fragment) {
                    is TimeLogsFragment -> fragment.onCreateTimeEntry()
                    else -> (fragment as UsersFragment).onCreateUser()
                }
            }
        }
    }

    override fun onDestroy() {
        this.menu = null
        super.onDestroy()
    }

    fun mainMenuMode() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(false)
            setDisplayShowHomeEnabled(false)

        }
        showMainScreenActions = true
        invalidateOptionsMenu()
    }

    fun subMenuMode() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
        showMainScreenActions = false
        invalidateOptionsMenu()
    }

    private fun currentFragment() =
        navHostFragment().childFragmentManager.fragments[0] as BaseFragment<*>

    private fun navHostFragment() =
        supportFragmentManager.findFragmentById(R.id.nav_host_fragment)!!

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        this.menu = menu
        menuInflater.inflate(R.menu.menu_main, menu)
        menu.setGroupVisible(R.id.mainScreenActionsGroup, showMainScreenActions)
        menu.setGroupVisible(R.id.adminActionsGroup, this.viewModel.isUserAdmin)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_report -> {
                currentFragment().let { fragment ->
                    if (fragment is TimeLogsFragment)
                        fragment.generateReport()
                }
                true
            }
            R.id.action_settings -> {
                this.currentFragment().showSettings()
                true
            }
            R.id.action_users -> {
                try {
                    (navHostFragment() as NavHostFragment).findNavController()
                        .navigate(TimeLogsFragmentDirections.actionMainFragmentToUsers())
                } catch (e: Exception) {
                    Log.e(javaClass.simpleName, "fail;ed to navigate", e)
                }
                true
            }
            R.id.action_logout -> {
                this.viewModel.logout({
                    this.currentFragment().navigateLogin()
                    finish()
                }, {
                    Toast.makeText(this, "Failed to logout, try again", Toast.LENGTH_SHORT).show()
                })
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        this.navHostFragment().findNavController().popBackStack(R.id.MainFragment, false)
        return true
    }

}