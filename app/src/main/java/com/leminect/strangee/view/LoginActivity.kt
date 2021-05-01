package com.leminect.strangee.view

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.leminect.strangee.R
import com.leminect.strangee.databinding.ActivityLoginBinding
import com.leminect.strangee.network.BASE_URL
import com.leminect.strangee.utility.emailCheck
import com.leminect.strangee.utility.hideKeyboard
import com.leminect.strangee.utility.saveUserToSharedPrefs
import com.leminect.strangee.viewmodel.LoginStatus
import com.leminect.strangee.viewmodel.LoginViewModel
import com.leminect.strangee.viewmodel.SignUpViewModel
import java.lang.Exception

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var loadingDialog: CustomLoadingDialog
    private lateinit var loginErrorDialog: HybridDialog
    private lateinit var loginFailedDialog: HybridDialog
    private val viewModel: LoginViewModel by lazy {
        ViewModelProvider(this).get(LoginViewModel::class.java)
    }

    companion object {
        var currentActivity: Activity? = null
        fun finishActivity() {
            try {
                currentActivity?.finish()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)
        binding.lifecycleOwner = this

        currentActivity = this

        val ss: SpannableString = SpannableString(getString(R.string.agree_terms_and_policy))

        ss.setSpan(InnerTextClick(BASE_URL + getString(R.string.terms_of_service_link)),
            31, 47, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        ss.setSpan(InnerTextClick(BASE_URL + getString(R.string.privacy_policy_link)),
            52, 66, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)

        binding.termsAndPolicyText.text = ss
        binding.termsAndPolicyText.movementMethod = LinkMovementMethod.getInstance()

        loadingDialog = CustomLoadingDialog(this, false, "Logging in...")
        loginFailedDialog = HybridDialog(this,
            arrayOf(View.VISIBLE, View.GONE, View.GONE, View.VISIBLE, View.GONE),
            arrayOf("Authentication Failed",
                "",
                "",
                "The email or password you entered is incorrect. Please try again!"),
            true,
            object : OkButtonListener {
                override fun onOkClick(interests: String, dismissDialog: (Boolean) -> Unit) {
                    dismissDialog(true)
                }

            })

        loginErrorDialog = HybridDialog(this,
            arrayOf(View.VISIBLE, View.GONE, View.GONE, View.VISIBLE, View.GONE),
            arrayOf("Authentication Error",
                "",
                "",
                "An error occurred during login process. Please check your internet connection and try again!"),
            true,
            object : OkButtonListener {
                override fun onOkClick(interests: String, dismissDialog: (Boolean) -> Unit) {
                    dismissDialog(true)
                }

            })

        viewModel.loginBackData.observe(this, Observer { returnedData ->
            returnedData?.let {
                loadingDialog.dismissDialog()

                // store user data & token then move to next activity
                saveUserToSharedPrefs(this,
                    returnedData.data,
                    returnedData.token,
                    returnedData.refreshToken)
                hideKeyboard()
                goToNextActivity()
            }
        })

        viewModel.status.observe(this, Observer { status ->
            status?.let {
                when (status) {
                    LoginStatus.LOGGING_IN -> loadingDialog.showDialog()
                    LoginStatus.LOGIN_FAILED -> {
                        loadingDialog.dismissDialog()
                        loginFailedDialog.showDialog()
                    }
                    LoginStatus.LOGIN_ERROR -> {
                        loadingDialog.dismissDialog()
                        loginErrorDialog.showDialog()
                    }
                }
            }
        })

        binding.loginButton.setOnClickListener {
            val email = binding.textInputEmail.editText!!.text.toString()
            val password = binding.textInputPassword.editText!!.text.toString()

            if (!emailCheck(email) || password.length < getString(R.string.minimum_password_length).toInt()) {
                loginFailedDialog.showDialog()
            } else {
                // begin login
                if (viewModel.status.value != LoginStatus.LOGGING_IN) {
                    loadingDialog.showDialog()
                    viewModel.loginUser(email, password)
                }
            }
        }

        binding.signUpButton.setOnClickListener {
            val intent: Intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        binding.forgotPassword.setOnClickListener{
            val forgotPasswordIntent: Intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(forgotPasswordIntent)
        }
    }

    private fun goToNextActivity() {
        val intent: Intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    inner class InnerTextClick(val link: String) : ClickableSpan() {
        override fun onClick(widget: View) {
            val browserIntent = Intent(Intent.ACTION_VIEW)
            browserIntent.data = Uri.parse(link)
            startActivity(browserIntent)
        }

        override fun updateDrawState(ds: TextPaint) {
            super.updateDrawState(ds)
            ds.color = Color.WHITE //ContextCompat.getColor(this@LandingActivity, R.color.white)
        }
    }
}