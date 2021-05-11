package com.leminect.stranzee.view

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.databinding.DataBindingUtil
import com.airbnb.lottie.LottieDrawable
import com.leminect.stranzee.R
import com.leminect.stranzee.databinding.LoadingDialogBinding

class CustomLoadingDialog(
    private val mContext: Context,
    dismissOnTouchOutside: Boolean,
    horizontalText: String,
    horizontalVisibility: Int = View.VISIBLE,
    verticalText: String = "",
    verticalVisibility: Int = View.GONE,
    loopVerticalAnim: Boolean = true,
    loopHorizontalAnim: Boolean = true,
) {
    private var alert: AlertDialog.Builder = AlertDialog.Builder(mContext)
    private var binding: LoadingDialogBinding
    private val alertDialog: AlertDialog

    init {
        val layoutInflater: LayoutInflater = LayoutInflater.from(mContext)
        binding = DataBindingUtil.inflate(layoutInflater,
            R.layout.loading_dialog,
            null,
            false)
        alert.setView(binding.root)
        alertDialog = alert.create()
        alertDialog.setCanceledOnTouchOutside(dismissOnTouchOutside)
        alertDialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)

        binding.horizontalAnim.repeatCount = if (loopHorizontalAnim) LottieDrawable.INFINITE else 0
        binding.verticalAnim.repeatCount = if (loopVerticalAnim) LottieDrawable.INFINITE else 0

        binding.horizontalLayout.visibility = horizontalVisibility
        binding.verticalLayout.visibility = verticalVisibility
        binding.horizontalTextView.text = horizontalText
        binding.verticalTextView.text = verticalText
    }

    fun showDialog() = alertDialog.show()
    fun dismissDialog() = alertDialog.dismiss()

    fun setVerticalSpeed(speed: Float) {
        binding.verticalAnim.speed = speed
    }

    fun setHorizontalSpeed(speed: Float) {
        binding.horizontalAnim.speed = speed
    }
}