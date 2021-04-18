package com.leminect.strangee.view

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.leminect.strangee.R

class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (getSharedPreferences(getString(R.string.shared_prefs_name), MODE_PRIVATE).getString(
                getString(R.string.prefs_user_id),
                null) != null
        ) {
            startActivity(Intent(this, MainActivity::class.java))
        }else{
            startActivity(Intent(this, LoginActivity::class.java))
        }

        finish()
    }
}