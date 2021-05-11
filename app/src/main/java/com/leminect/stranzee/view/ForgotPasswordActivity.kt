package com.leminect.stranzee.view

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.leminect.stranzee.R
import com.leminect.stranzee.databinding.ActivityForgotPasswordBinding
import com.leminect.stranzee.utility.emailCheck
import com.leminect.stranzee.utility.hideKeyboard
import com.leminect.stranzee.viewmodel.ForgotPasswordStatus
import com.leminect.stranzee.viewmodel.ForgotPasswordViewModel

class ForgotPasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var failedDialog: HybridDialog
    private lateinit var errorDialog: HybridDialog
    private lateinit var emailNotFoundDialog: HybridDialog
    private lateinit var successDialog: HybridDialog
    private lateinit var loadingDialog: CustomLoadingDialog
    private val viewModel: ForgotPasswordViewModel by lazy {
        ViewModelProvider(this).get(ForgotPasswordViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_forgot_password)

        setUpActionBar()
        loadingDialog = CustomLoadingDialog(this, false, "Sending request...")
        errorDialog = HybridDialog(
            this,
            arrayOf(View.VISIBLE, View.GONE, View.GONE, View.VISIBLE, View.GONE),
            arrayOf("Request Failed",
                "",
                "",
                "An error occurred while sending your request. Please check your internet connection and try again!"),
            true,
            object : OkButtonListener {
                override fun onOkClick(interests: String, dismissDialog: (Boolean) -> Unit) {
                    dismissDialog(true)
                }
            }
        )
        emailNotFoundDialog = HybridDialog(
            this,
            arrayOf(View.VISIBLE, View.GONE, View.GONE, View.VISIBLE, View.GONE),
            arrayOf("Email Not Found",
                "",
                "",
                "The provided email address isn't registered in this app. Please try again with correct email!"),
            true,
            object : OkButtonListener {
                override fun onOkClick(interests: String, dismissDialog: (Boolean) -> Unit) {
                    dismissDialog(true)
                }
            }
        )
        failedDialog = HybridDialog(
            this,
            arrayOf(View.VISIBLE, View.GONE, View.GONE, View.VISIBLE, View.GONE),
            arrayOf("Request Failed",
                "",
                "",
                "An error occurred while sending your request. Please try again!"),
            true,
            object : OkButtonListener {
                override fun onOkClick(interests: String, dismissDialog: (Boolean) -> Unit) {
                    dismissDialog(true)
                }
            }
        )
        successDialog = HybridDialog(
            this,
            arrayOf(View.VISIBLE, View.GONE, View.GONE, View.VISIBLE, View.GONE),
            arrayOf("Request Successful",
                "",
                "",
                "The reset link has been successfully sent to your email address."),
            false,
            object : OkButtonListener {
                override fun onOkClick(interests: String, dismissDialog: (Boolean) -> Unit) {
                    dismissDialog(true)
                    finish()
                }
            }
        )

        viewModel.status.observe(this, Observer { status ->
            status?.let {
                Log.i("ForgotPasswordViewModel", "status got ${status.name}")
                when(status) {
                    ForgotPasswordStatus.SENDING -> {
                        loadingDialog.showDialog()
                    }
                    ForgotPasswordStatus.SENT -> {
                        loadingDialog.dismissDialog()
                        successDialog.showDialog()
                        viewModel.onStatusChecked()
                    }
                    ForgotPasswordStatus.SENDING_ERROR -> {
                        loadingDialog.dismissDialog()
                        errorDialog.showDialog()
                        viewModel.onStatusChecked()
                    }
                    ForgotPasswordStatus.SENDING_FAILED -> {
                        loadingDialog.dismissDialog()
                        failedDialog.showDialog()
                        viewModel.onStatusChecked()
                    }
                    ForgotPasswordStatus.EMAIL_NOT_FOUND -> {
                        loadingDialog.dismissDialog()
                        emailNotFoundDialog.showDialog()
                        viewModel.onStatusChecked()
                    }
                }
            }
        })

        binding.getLinkButton.setOnClickListener{
            val email: String = binding.textInputEmail.editText?.text.toString()
            binding.textInputEmail.editText?.doOnTextChanged { text, _, _, count ->
                if (!emailCheck(text.toString())) {
                    binding.textInputEmail.error = "Invalid email"
                    binding.textInputEmail.isErrorEnabled = true
                } else {
                    binding.textInputEmail.error = null
                    binding.textInputEmail.isErrorEnabled = false
                }
            }

            if(emailCheck(email)) {
                hideKeyboard()
                viewModel.sendResetLink(email)
            } else {
                binding.textInputEmail.error = "Invalid email"
                binding.textInputEmail.isErrorEnabled = true
                Toast.makeText(this, "Please enter valid email", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setUpActionBar() {
        val actionbar = supportActionBar
        actionbar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        actionbar?.setCustomView(R.layout.sign_up_action_bar)
        actionbar?.customView?.findViewById<TextView>(R.id.action_bar_text)?.text =
            getString(R.string.forgot_password_actionbar)

        supportActionBar?.customView?.findViewById<ImageView>(R.id.back_button)
            ?.setOnClickListener {
                onBackPressed()
            }
    }
}