package com.leminect.stranzee.view

import android.app.AlertDialog
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.leminect.stranzee.R
import com.leminect.stranzee.databinding.RateUsDialogBinding
import com.leminect.stranzee.utility.openEmailClient
import com.leminect.stranzee.utility.openPlayStore

class RateUsDialog(private val mContext: Context, private val hideDontAskAgain: Boolean = false) {
    private val alert: AlertDialog.Builder = AlertDialog.Builder(mContext)
    private val binding: RateUsDialogBinding
    private val alertDialog: AlertDialog
    private var currentRating: Int = 0

    init {
        val layoutInflater: LayoutInflater = LayoutInflater.from(mContext)
        binding = DataBindingUtil.inflate(layoutInflater,
            R.layout.rate_us_dialog,
            null,
            false)
        alert.setView(binding.root)
        alertDialog = alert.create()
        alertDialog.setCanceledOnTouchOutside(true)
        alertDialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)

        binding.ratingBar.setOnRatingBarChangeListener { ratingBar, rating, fromUser ->
            currentRating = rating.toInt()
            if(currentRating < 5) {
                binding.rateButton.text = mContext.getString(R.string.feedback)
            } else {
                binding.rateButton.text = mContext.getString(R.string.rate_us)
            }
        }

        binding.cancelButton.setOnClickListener {
            alertDialog.dismiss()
            if(binding.checkbox.isChecked) {
                ratingGiven()
            }
        }

        binding.rateButton.setOnClickListener {
            onRateButtonClick()
        }

        if(hideDontAskAgain) {
            binding.checkbox.visibility = View.GONE
        }

        alertDialog.show()
    }

    private fun ratingGiven() {
        mContext.getSharedPreferences(mContext.getString(R.string.shared_prefs_name), MODE_PRIVATE)
            .edit().putBoolean(mContext.getString(R.string.prefs_rating_given), true).apply()
    }

    private fun onRateButtonClick() {
        when {
            currentRating == 0 -> {
                // rating not given
                Toast.makeText(mContext, "Please give a valid rating. Click on the stars to rate us!", Toast.LENGTH_LONG).show()
                return
            }
            currentRating < 5 -> {
                // open email intent for sending feedback
                openEmailClient(mContext)
            }
            else -> {
                // open play store for rating
                openPlayStore(mContext)
            }
        }

        ratingGiven()
        alertDialog.dismiss()
    }
}