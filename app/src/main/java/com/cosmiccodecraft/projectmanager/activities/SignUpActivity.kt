package com.cosmiccodecraft.projectmanager.activities

import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.cosmiccodecraft.projectmanager.R
import com.cosmiccodecraft.projectmanager.firebase.FireStore
import com.cosmiccodecraft.projectmanager.models.User
import com.google.firebase.auth.FirebaseAuth

class SignUpActivity : BaseActivity() {
    lateinit var toolbar: Toolbar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        toolbar = findViewById(R.id.toolbar_sign_up_activity)
        setupActionBar()

        findViewById<Button>(R.id.btn_sign_up).setOnClickListener {
            hideKeyboard()
            registerUser()
        }
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back_button)
        }

        toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun registerUser() {
        val name: String = findViewById<TextView?>(R.id.et_name).text.toString().trim { it <= ' ' }
        val email: String =
            findViewById<TextView?>(R.id.et_email_signin).text.toString().trim { it <= ' ' }
        val password: String =
            findViewById<TextView?>(R.id.et_password_signin).text.toString().trim { it <= ' ' }

        if (validateForm(name, email, password)) {
            showProgressDialog(resources.getString(R.string.please_wait))
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->

                    if (task.isSuccessful) {
                        val firebaseUser = task.result!!.user!!
                        val registeredEmail = firebaseUser.email!!
                        val user = User(firebaseUser.uid, name, registeredEmail)
                        FireStore().registerUser(this@SignUpActivity, user)
                    }
                    if (task.exception != null) {
                        showErrorSnackBar(task.exception!!.message.toString())
                    }
                }
        }
    }

    private fun validateForm(name: String, email: String, password: String): Boolean {
        return when {
            name.isBlank() -> {
                showErrorSnackBar(resources.getString(R.string.err_msg_enter_name))
                false
            }

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

            (password.length > 6) -> {
                showErrorSnackBar("Enter password of length ${6} or more")
                false
            }

            else -> {
                true
            }
        }
    }

    fun userRegisteredSuccess() {
        Toast.makeText(
            this@SignUpActivity,
            "You have successfully registered",
            Toast.LENGTH_SHORT
        ).show()
        hideProgressDialog()
        FirebaseAuth.getInstance().signOut()
        finish()
    }
}