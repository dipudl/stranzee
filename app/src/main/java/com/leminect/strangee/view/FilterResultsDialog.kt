package com.leminect.strangee.view

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import com.leminect.strangee.R
import com.leminect.strangee.databinding.FilterResultsDialogBinding
import com.leminect.strangee.utility.Constants

class FilterResultsDialog(private val mContext: Context, private val applyListener: FilterApplyListener) {
    private var alert: AlertDialog.Builder = AlertDialog.Builder(mContext)
    private var binding: FilterResultsDialogBinding
    private val alertDialog: AlertDialog

    init {
        val layoutInflater: LayoutInflater = LayoutInflater.from(mContext)
        binding = DataBindingUtil.inflate(layoutInflater,
            R.layout.filter_results_dialog,
            null,
            false)
        alert.setView(binding.root)
        alertDialog = alert.create()
        alertDialog.setCanceledOnTouchOutside(true)
        alertDialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)

        setUpCountryAndGenderSpinner()
        setUpApplyButtonClick()
        alertDialog.show()
    }

    private fun setUpCountryAndGenderSpinner() {
        val countryAdapter: ArrayAdapter<String> = ArrayAdapter(
            mContext,
            R.layout.custom_spinner_list_item,
            Constants.getCountries("Worldwide"))
        val genderAdapter: ArrayAdapter<String> = ArrayAdapter(
            mContext,
            R.layout.custom_spinner_list_item,
            Constants.getGenders("Any"))

        countryAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item)
        genderAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item)
        binding.countrySpinner.adapter = countryAdapter
        binding.genderSpinner.adapter = genderAdapter
    }

    private fun setUpApplyButtonClick() {
        binding.applyButton.setOnClickListener {
            applyListener.onApply(
                binding.countrySpinner.selectedItem as String,
                binding.genderSpinner.selectedItem as String,
                binding.textInputInterest.editText?.text.toString()
            )

            alertDialog.dismiss()
        }
    }
}

interface FilterApplyListener {
    fun onApply(country: String, gender: String, interest: String)
}