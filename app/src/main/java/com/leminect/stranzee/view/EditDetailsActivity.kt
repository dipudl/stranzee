package com.leminect.stranzee.view

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.chip.Chip
import com.leminect.stranzee.R
import com.leminect.stranzee.databinding.ActivityEditDetailsBinding
import com.leminect.stranzee.model.User
import com.leminect.stranzee.network.SocketManager
import com.leminect.stranzee.utility.Constants
import com.leminect.stranzee.utility.getFromSharedPreferences
import com.leminect.stranzee.utility.saveUserToSharedPrefs
import com.leminect.stranzee.viewmodel.*
import com.tsongkha.spinnerdatepicker.DatePickerDialog
import com.tsongkha.spinnerdatepicker.SpinnerDatePickerDialogBuilder
import java.text.SimpleDateFormat
import java.util.*

class EditDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditDetailsBinding
    private lateinit var datePickerDialog: DatePickerDialog
    private lateinit var mDateSetListener: DatePickerDialog.OnDateSetListener
    private lateinit var user: User
    private lateinit var newUser: User
    private var errorEnabled = false
    private var birthday: Long? = null
    private lateinit var failedHybridDialog: HybridDialog
    private lateinit var errorHybridDialog: HybridDialog
    private lateinit var loadingDialog: CustomLoadingDialog
    private var interestList = ArrayList<String>()

    private val viewModel: EditDetailsViewModel by lazy {
        ViewModelProvider(this).get(EditDetailsViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_edit_details)

        setUpActionBar()
        setUpCountryAndGenderSpinner()

        user = intent.getSerializableExtra("user") as User
        interestList.addAll(user.interestedIn)
        birthday = user.birthday
        val token = getFromSharedPreferences(this).first

        setUpDatePikerDialog()
        setUpInterestedInChipGroup(getString(R.string.plus_add), 0, true)

        binding.textInputFirstName.editText?.setText(user.firstName)
        binding.textInputLastName.editText?.setText(user.lastName)
        binding.textInputAboutMe.editText?.setText(
            if(user.aboutMe == "-") "" else user.aboutMe
        )
        binding.birthdayText.text = user.formatTime()
        binding.countrySpinner.setSelection(Constants.getCountries().indexOf(user.country))
        binding.genderSpinner.setSelection(Constants.getGenders().indexOf(user.gender))

        for ((index, text) in interestList.withIndex()) {
            setUpInterestedInChipGroup(text, index)
        }

        loadingDialog = CustomLoadingDialog(this, false, "Updating profile details...")
        errorHybridDialog = HybridDialog(
            this,
            arrayOf(View.VISIBLE, View.GONE, View.GONE, View.VISIBLE, View.GONE),
            arrayOf("Update Error",
                "",
                "",
                "An error occurred while updating the profile details. Please check your internet connection and try again!"),
            true,
            object : OkButtonListener {
                override fun onOkClick(interests: String, dismissDialog: (Boolean) -> Unit) {
                    dismissDialog(true)
                }
            }
        )
        failedHybridDialog = HybridDialog(
            this,
            arrayOf(View.VISIBLE, View.GONE, View.GONE, View.VISIBLE, View.GONE),
            arrayOf("Update Failed",
                "",
                "",
                "An error occurred while updating profile details. Please try again!"),
            true,
            object : OkButtonListener {
                override fun onOkClick(interests: String, dismissDialog: (Boolean) -> Unit) {
                    dismissDialog(true)
                }
            }
        )

        viewModel.status.observe(this, Observer { status ->
            status?.let {
                when (status) {
                    EditDetailsStatus.UPDATING -> loadingDialog.showDialog()
                    EditDetailsStatus.UPDATE_DONE -> {
                        loadingDialog.dismissDialog()
                        Toast.makeText(this, "Profile details updated", Toast.LENGTH_LONG).show()
                        saveUserToSharedPrefs(this, newUser, token, null,false)
                        finish()
                    }
                    EditDetailsStatus.UPDATE_FAILED -> {
                        loadingDialog.dismissDialog()
                        failedHybridDialog.showDialog()
                    }
                    EditDetailsStatus.UPDATE_ERROR -> {
                        loadingDialog.dismissDialog()
                        errorHybridDialog.showDialog()
                    }
                }
            }
        })

        setUpErrorShowingLogic()
        binding.saveDetailsButton.setOnClickListener {
            val firstName = binding.textInputFirstName.editText?.text.toString()
            val lastName = binding.textInputLastName.editText?.text.toString()
            val country = binding.countrySpinner.selectedItem.toString()
            val gender = binding.genderSpinner.selectedItem.toString()
            val aboutMe = binding.textInputAboutMe.editText?.text.toString()

            errorEnabled = true

            if (firstName.trim().isEmpty()) {
                Toast.makeText(this, "Please enter your first name", Toast.LENGTH_LONG).show()
                binding.textInputFirstName.error = "Required"

            } else if (lastName.trim().isEmpty()) {
                Toast.makeText(this, "Please enter your last name", Toast.LENGTH_LONG).show()
                binding.textInputLastName.error = "Required"

            } else if (binding.countrySpinner.selectedItemPosition <= 0 || country.trim()
                    .isEmpty()
            ) {
                Toast.makeText(this, "Please select your country", Toast.LENGTH_LONG).show()

            } else if (binding.genderSpinner.selectedItemPosition <= 0 || gender.trim().isEmpty()) {
                Toast.makeText(this, "Please select your gender", Toast.LENGTH_LONG).show()

            } else if (interestList.size == 0) {
                Toast.makeText(this, "Please add at least one interest", Toast.LENGTH_LONG)
                    .show()

            } else if (birthday == null) {
                Toast.makeText(this, "Please select your birthday", Toast.LENGTH_LONG).show()

            } else if (aboutMe.trim().isEmpty() || aboutMe.length < 30) {
                Toast.makeText(this,
                    "'About me' must contain 30 or more characters",
                    Toast.LENGTH_LONG).show()
                binding.textInputAboutMe.error = "Minimum 30 characters required"

            } else {
                newUser = user.copy(
                    firstName = firstName,
                    lastName = lastName,
                    country = country,
                    gender = gender,
                    interestedIn = interestList.toList(),
                    birthday = birthday!!,
                    aboutMe = aboutMe
                )

                if(newUser == user) {
                    finish()
                } else {
                    viewModel.editDetails(token, newUser)
                }
            }
        }

        binding.selectBirthday.setOnClickListener {
            datePickerDialog.show()
        }
    }

    private fun setUpInterestedInChipGroup(
        chipText: String,
        position: Int,
        isAddChip: Boolean = false,
    ) {
        val chip = layoutInflater.inflate(R.layout.interest_chip_item, binding.interestsChipGroup,
            false) as Chip
        chip.text = chipText
        if (isAddChip) {
            chip.setCloseIconVisible(R.bool._false)
            chip.setOnClickListener {
                HybridDialog(
                    this,
                    arrayOf(View.VISIBLE, View.VISIBLE, View.VISIBLE, View.GONE, View.VISIBLE),
                    arrayOf("Interested in",
                        getString(R.string.interested_in_popup_text),
                        getString(R.string.im_interested_in),
                        ""),
                    false,
                    object : OkButtonListener {
                        override fun onOkClick(
                            interests: String,
                            dismissDialog: (Boolean) -> Unit,
                        ) {
                            dismissDialog(true)

                            if (interests.isBlank()) return

                            for (interest in interests.split(",")) {
                                val i = interest.trim()
                                var showMessage = false;

                                if (interestList.indexOf(i) < 0) {
                                    if (i.length >= 3) {
                                        interestList.add(i)
                                        setUpInterestedInChipGroup(i, interestList.size - 1)
                                    } else if (i.isNotEmpty()) {
                                        showMessage = true;
                                    }
                                }

                                if (showMessage) {
                                    Toast.makeText(this@EditDetailsActivity,
                                        "Cannot add topic of 2 or less characters",
                                        Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                ).showDialog()
            }
        } else {
            chip.setCloseIconVisible(R.bool._true)
            chip.setOnCloseIconClickListener {
                binding.interestsChipGroup.removeView(it)
                interestList.removeAt(interestList.indexOf((it as Chip).text))
            }
        }
        binding.interestsChipGroup.addView(chip, position)
    }

    private fun setUpDatePikerDialog() {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val previous_dob: Calendar = Calendar.getInstance().apply { timeInMillis = user.birthday };

        mDateSetListener = DatePickerDialog.OnDateSetListener { datePicker, mYear, mMonth, mDay ->
            val formattedDate = "${Constants.MONTHS[mMonth]} $mDay, $mYear"

            var isDateCorrect = false
            if (mYear < year) {
                isDateCorrect = true
            } else if (mYear == year) {
                if (mMonth < month) {
                    isDateCorrect = true
                } else if (mMonth == month) {
                    if (mDay <= day)
                        isDateCorrect = true
                }
            }

            if (isDateCorrect) {
                binding.birthdayText.text = formattedDate

                val sdf: SimpleDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                birthday = sdf.parse(formattedDate)!!.time
            } else {
                Toast.makeText(this@EditDetailsActivity,
                    "Please select the correct date",
                    Toast.LENGTH_LONG).show()
            }
        }

        datePickerDialog = SpinnerDatePickerDialogBuilder()
            .context(this@EditDetailsActivity)
            .callback(mDateSetListener)
            .spinnerTheme(R.style.NumberPickerStyle)
            .showTitle(true)
            .showDaySpinner(true)
            .defaultDate(previous_dob.get(Calendar.YEAR),
                previous_dob.get(Calendar.MONTH),
                previous_dob.get(Calendar.DAY_OF_MONTH))
            .build()

    }

    private fun setUpErrorShowingLogic() {
        binding.textInputFirstName.editText?.doOnTextChanged { text, _, _, count ->
            if (text.toString().isEmpty() && errorEnabled)
                binding.textInputFirstName.error = "Required"
            else
                binding.textInputFirstName.error = null
        }
        binding.textInputLastName.editText?.doOnTextChanged { text, _, _, count ->
            if (text.toString().isEmpty() && errorEnabled)
                binding.textInputLastName.error = "Required"
            else
                binding.textInputLastName.error = null
        }
        binding.textInputAboutMe.editText?.doOnTextChanged { text, _, _, count ->
            if (text.toString().length < 30 && errorEnabled)
                binding.textInputAboutMe.error = "Minimum 30 characters required"
            else
                binding.textInputAboutMe.error = null
        }
    }

    private fun setUpActionBar() {
        val actionbar = supportActionBar
        actionbar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        actionbar?.setCustomView(R.layout.sign_up_action_bar)
        actionbar?.customView?.findViewById<TextView>(R.id.action_bar_text)?.text =
            getString(R.string.edit_details_actionbar)

        supportActionBar?.customView?.findViewById<ImageView>(R.id.back_button)
            ?.setOnClickListener {
                onBackPressed()
            }
    }

    private fun setUpCountryAndGenderSpinner() {
        val countryAdapter: ArrayAdapter<String> = ArrayAdapter(
            this,
            R.layout.custom_spinner_list_item,
            Constants.getCountries())
        val genderAdapter: ArrayAdapter<String> = ArrayAdapter(
            this,
            R.layout.custom_spinner_list_item,
            Constants.getGenders())

        countryAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item)
        genderAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item)
        binding.countrySpinner.adapter = countryAdapter
        binding.genderSpinner.adapter = genderAdapter
    }

    override fun onResume() {
        super.onResume()
        SocketManager.setOnline(true)
    }

    override fun onPause() {
        super.onPause()
        SocketManager.setOnline(false)
    }
}