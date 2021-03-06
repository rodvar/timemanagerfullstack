package com.rodvar.timemanager.feature.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import com.rodvar.timemanager.R
import com.rodvar.timemanager.feature.timelogs.MainActivity
import com.rodvar.timemanager.utils.uihelpers.afterTextChanged
import org.koin.androidx.viewmodel.ext.android.viewModel

class LoginActivity : AppCompatActivity() {

    private val loginViewModel: LoginViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        val username = findViewById<EditText>(R.id.username)
        val password = findViewById<EditText>(R.id.password)
        val login = findViewById<Button>(R.id.login)
        val loading = findViewById<ProgressBar>(R.id.loading)
        val name = findViewById<TextView>(R.id.name)
        val preferredHours = findViewById<TextView>(R.id.preferredHours)
        val noAccountLink = findViewById<TextView>(R.id.noAccountLink)

        noAccountLink.movementMethod = LinkMovementMethod.getInstance()
        noAccountLink.setOnClickListener {
            preferredHours.visibility = View.VISIBLE
            name.visibility = View.VISIBLE
            noAccountLink.visibility = View.GONE
            login.text = getString(R.string.action_sign_in)
        }

        loginViewModel.loginFormState.observe(this@LoginActivity, Observer {
            val loginState = it ?: return@Observer

            // disable login button unless both username / password is valid
            login.isEnabled = loginState.isDataValid

            if (loginState.usernameError != null) {
                username.error = getString(loginState.usernameError)
            }
            if (loginState.passwordError != null) {
                password.error = getString(loginState.passwordError)
            }
        })

        loginViewModel.loginResult.observe(this@LoginActivity, Observer {
            val loginResult = it ?: return@Observer

            loading.visibility = View.GONE
            if (loginResult.error != null) {
                showLoginFailed(loginResult.error)
            }
            if (loginResult.success != null) {
                updateUiWithUser(loginResult.success)
                setResult(Activity.RESULT_OK)
                //Complete and destroy login activity once successful
                Intent(this, MainActivity::class.java).let { intent ->
                    this.startActivity(intent)
                    finish()
                }
            }
        })

        username.afterTextChanged {
            loginViewModel.loginDataChanged(
                username.text.toString(),
                password.text.toString()
            )
        }

        password.apply {
            afterTextChanged {
                loginViewModel.loginDataChanged(
                    username.text.toString(),
                    password.text.toString()
                )
            }

            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        loginViewModel.login(
                            username.text.toString(),
                            password.text.toString()
                        )
                }
                false
            }

            login.setOnClickListener {
                loading.visibility = View.VISIBLE
                if (noAccountLink.isVisible)
                    loginViewModel.login(username.text.toString(), password.text.toString())
                else
                    loginViewModel.register(
                        username.text.toString(),
                        password.text.toString(),
                        name.text.toString(),
                        preferredHours.text?.toString()?.toFloat() ?: 6F
                    )
            }
        }
    }

    private fun updateUiWithUser(model: LoggedInUserView) {
        val welcome = String.format(getString(R.string.welcome),  model.displayName)
        // TODO : initiate successful logged in experience
        Toast.makeText(
            applicationContext,
            "$welcome",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showLoginFailed(@StringRes errorString: Int) {
        Toast.makeText(applicationContext, errorString, Toast.LENGTH_SHORT).show()
    }
}