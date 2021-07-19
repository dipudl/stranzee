package com.leminect.stranzee.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.leminect.stranzee.R

class SplashScreen : AppCompatActivity() {
    private lateinit var mainIntent: Intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (getSharedPreferences(getString(R.string.shared_prefs_name), MODE_PRIVATE).getString(
                getString(R.string.prefs_user_id),
                null) != null
        ) {
            mainIntent = Intent(this, MainActivity::class.java)
            if(intent.extras != null) {
                for(key in intent.extras!!.keySet()) {
                    mainIntent.putExtra(key, intent.extras!!.get(key).toString())
                }
            }
        }else{
            mainIntent = Intent(this, LoginActivity::class.java)
        }

        mainIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

        startActivity(mainIntent)
        finish()
    }
}