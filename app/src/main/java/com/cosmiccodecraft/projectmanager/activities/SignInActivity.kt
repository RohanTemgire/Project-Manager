package com.cosmiccodecraft.projectmanager.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.cosmiccodecraft.projectmanager.R
import com.cosmiccodecraft.projectmanager.firebase.FireStore
import com.cosmiccodecraft.projectmanager.models.User
import com.google.firebase.auth.FirebaseAuth

class SignInActivity : BaseActivity() {
    lateinit var toolbar: Toolbar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        toolbar = findViewById(R.id.toolbar_sign_in_activity)
        setupActionBar()

        findViewById<Button>(R.id.btn_sign_in).setOnClickListener {
            hideKeyboard()
            signInUser()
        }
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back_button)
        }

        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun signInUser() {
        val email: String =
            findViewById<TextView>(R.id.et_email_signin).text.toString().trim { it <= ' ' }
        val password: String =
            findViewById<TextView>(R.id.et_password_signin).text.toString().trim { it <= ' ' }

        if (validateForm(email, password)) {
            showProgressDialog(resources.getString(R.string.please_wait))
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener {
                    hideProgressDialog()
                    if (it.isSuccessful) {
                        FireStore().loadUserData(this@SignInActivity)
                    } else {
                        showErrorSnackBar(it.exception!!.message.toString())
                    }
                }
        }
    }

    private fun validateForm(email: String, password: String): Boolean {
        return when {
            email.isBlank() -> {
                showErrorSnackBar(resources.getString(R.string.err_msg_enter_email))
                false
            }

            (!email.contains("@") && !email.contains(".")) -> {
                showErrorSnackBar(resources.getString(R.string.err_msg_enter_valid_email))
                false
            }

            password.isBlank() -> {
                showErrorSnackBar(resources.getString(R.string.err_msg_enter_password))
                false
            }

            else -> {
                true
            }
        }
    }

    fun signInSuccess(loggedInUserData: User) {
        hideProgressDialog()
        startActivity(Intent(this@SignInActivity, MainActivity::class.java))
        finish()
    }
}