package com.leminect.strangee.view

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import androidx.databinding.DataBindingUtil
import com.leminect.strangee.R
import com.leminect.strangee.databinding.DialogLayoutBinding

class HybridDialog(
    private val mContext: Context,
    private val visibilities: Array<Int>,
    private val texts: Array<String>,
    dismissOnTouchOutside: Boolean,
    private val okListener: OkButtonListener,
    okButtonText: String = "OK",
    cancelButtonText: String = "Cancel",
) {
    private var alert: AlertDialog.Builder = AlertDialog.Builder(mContext)
    private var binding: DialogLayoutBinding
    private val alertDialog: AlertDialog

    init {
        val layoutInflater: LayoutInflater = LayoutInflater.from(mContext)
        binding = DataBindingUtil.inflate(layoutInflater,
            R.layout.dialog_layout,
            null,
            false)
        alert.setView(binding.root)
        alertDialog = alert.create()
        alertDialog.setCanceledOnTouchOutside(dismissOnTouchOutside)
        alertDialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)

        setUpDialogStyle()
        setUpButtonClick()

        binding.okButton.text = okButtonText
        binding.cancelButton.text = cancelButtonText
    }

    fun showDialog() = alertDialog.show()
    fun dismissDialog() = alertDialog.dismiss()

    private fun setUpDialogStyle() {
        binding.apply {
            titleText.visibility = visibilities[0]
            smallTextView.visibility = visibilities[1]
            textInput.visibility = visibilities[2]
            largeTextView.visibility = visibilities[3]
            cancelButton.visibility = visibilities[4]

            titleText.text = texts[0]
            smallTextView.text = texts[1]
            textInput.hint = texts[2]
            largeTextView.text = texts[3]
        }
    }

    private fun setUpButtonClick() {
        binding.cancelButton.setOnClickListener {
            alertDialog.dismiss()
        }

        binding.okButton.setOnClickListener {
            okListener.onOkClick(
                binding.textInput.editText?.text.toString()
            ) { result: Boolean ->
                if (result) alertDialog.dismiss()
            }
        }
    }
}

interface OkButtonListener {
    fun onOkClick(interests: String, dismissDialog: (Boolean) -> Unit)
}