package com.cosmiccodecraft.projectmanager.activities

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowInsets
import android.view.WindowManager
import com.cosmiccodecraft.projectmanager.R
import com.cosmiccodecraft.projectmanager.firebase.FireStore

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_acitivity)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        Handler(Looper.getMainLooper()).postDelayed({
            val currentUserId = FireStore().getCurrentUserId()
            if (currentUserId.isNotEmpty())
                startActivity(Intent(this, MainActivity::class.java))
            else
                startActivity(Intent(this, IntroActivity::class.java))
            finish()
        }, 2500)

    }

}