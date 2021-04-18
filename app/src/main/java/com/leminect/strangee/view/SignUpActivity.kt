package com.leminect.strangee.view

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer
import com.google.android.material.chip.Chip
import com.leminect.strangee.R
import com.leminect.strangee.databinding.ActivitySignUpBinding
import com.leminect.strangee.model.User
import com.leminect.strangee.network.CheckRegistrationInput
import com.leminect.strangee.network.StrangeeApi
import com.leminect.strangee.utility.Constants
import com.leminect.strangee.utility.emailCheck
import com.leminect.strangee.utility.hideKeyboard
import com.leminect.strangee.utility.saveUserToSharedPrefs
import com.leminect.strangee.viewmodel.SignUpStatus
import com.leminect.strangee.viewmodel.SignUpViewModel
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import com.tsongkha.spinnerdatepicker.DatePickerDialog
import com.tsongkha.spinnerdatepicker.SpinnerDatePickerDialogBuilder
import java.text.SimpleDateFormat
import java.util.*


class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var datePickerDialog: DatePickerDialog
    private lateinit var mDateSetListener: DatePickerDialog.OnDateSetListener
    private val interestList = ArrayList<String>()
    private lateinit var user: User
    private var photoUri: Uri? = null
    private var birthday: Long? = null
    private var errorEnabled = false
    private lateinit var errorHybridDialog: HybridDialog
    private lateinit var userExistsHybridDialog: HybridDialog
    private lateinit var loadingDialog: CustomLoadingDialog
    private lateinit var accountCreatedDialog: CustomLoadingDialog
    private lateinit var prefsEditor: SharedPreferences.Editor

    private val viewModel: SignUpViewModel by lazy {
        ViewModelProvider(this).get(SignUpViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sign_up)
        binding.lifecycleOwner = this

        loadingDialog = CustomLoadingDialog(this, false, "Creating your account...")
        accountCreatedDialog = CustomLoadingDialog(this,
            false,
            "",
            View.GONE,
            "Congratulations! Your account has been created successfully.",
            View.VISIBLE,
            true)

        errorHybridDialog = HybridDialog(
            this,
            arrayOf(View.VISIBLE, View.GONE, View.GONE, View.VISIBLE, View.GONE),
            arrayOf("Sign Up Error",
                "",
                "",
                "An error occurred while creating your account. Please try again!"),
            true,
            object : OkButtonListener {
                override fun onOkClick(interests: String, dismissDialog: (Boolean) -> Unit) {
                    dismissDialog(true)
                }
            }
        )

        userExistsHybridDialog = HybridDialog(
            this,
            arrayOf(View.VISIBLE, View.GONE, View.GONE, View.VISIBLE, View.GONE),
            arrayOf("Email already registered",
                "",
                "",
                "The email address has been already registered. Please try again with different email."),
            true,
            object : OkButtonListener {
                override fun onOkClick(interests: String, dismissDialog: (Boolean) -> Unit) {
                    dismissDialog(true)
                }
            }
        )

        setUpActionBar()
        setUpCountryAndGenderSpinner()
        setUpDatePikerDialog()
        setUpInterestedInChipGroup(getString(R.string.plus_add), 0)

        binding.pickImageLayout.setOnClickListener {
            // start picker to get image for cropping
            CropImage.startPickImageActivity(this@SignUpActivity)
        }

        viewModel.signUpBackData.observe(this, Observer { signUpBackData ->
            signUpBackData?.let {
                loadingDialog.dismissDialog()

                // store user data & token then move to next activity
                saveUserToSharedPrefs(this, signUpBackData.data, signUpBackData.token)

                hideKeyboard()
                accountCreatedDialog.showDialog()
                object : CountDownTimer(4000, 4000) {
                    override fun onTick(millisUntilFinished: Long) {
                    }

                    override fun onFinish() {
                        accountCreatedDialog.dismissDialog()
                        goToNextActivity()
                    }

                }.start()
            }
        })

        viewModel.status.observe(this, Observer { signUpStatus ->
            signUpStatus?.let {
                when (signUpStatus) {
                    SignUpStatus.CHECKING -> {
                        // start loading dialogue
                        loadingDialog.showDialog()
                    }
                    SignUpStatus.CHECK_ERROR, SignUpStatus.SIGN_UP_ERROR, SignUpStatus.SIGN_UP_FAILED -> {
                        // stop loading dialog, show error popup
                        loadingDialog.dismissDialog()
                        userExistsHybridDialog.dismissDialog()
                        errorHybridDialog.showDialog()
                    }
                    SignUpStatus.CHECK_FAILED -> {
                        // stop loading dialog, show email already exists popup
                        loadingDialog.dismissDialog()
                        errorHybridDialog.dismissDialog()
                        userExistsHybridDialog.showDialog()
                    }
                }
            }
        })

        setUpErrorShowingLogic()

        binding.createAccountButton.setOnClickListener {
            val firstName = binding.textInputFirstName.editText?.text.toString()
            val lastName = binding.textInputLastName.editText?.text.toString()
            val country = binding.countrySpinner.selectedItem.toString()
            val gender = binding.genderSpinner.selectedItem.toString()
            val aboutMe = binding.textInputAboutMe.editText?.text.toString()
            val email = binding.textInputEmail.editText?.text.toString()
            val password = binding.textInputPassword.editText?.text.toString()
            val rePassword = binding.textInputReEnterPassword.editText?.text.toString()

            errorEnabled = true

            if (photoUri == null) {
                Toast.makeText(this, "Please add your profile picture", Toast.LENGTH_LONG).show()

            } else if (firstName.trim().isEmpty()) {
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

            } else if (aboutMe.trim().isEmpty() || aboutMe.length < 50) {
                Toast.makeText(this,
                    "'About me' must contain 50 or more characters",
                    Toast.LENGTH_LONG).show()
                binding.textInputAboutMe.error = "Minimum 50 characters required"

            } else if (!emailCheck(email)) {
                Toast.makeText(this, "Please enter correct email address", Toast.LENGTH_LONG).show()
                binding.textInputEmail.error = "Invalid email"

            } else if (password.length < getString(R.string.minimum_password_length).toInt()) {
                Toast.makeText(this,
                    "Password must contain ${getString(R.string.minimum_password_length).toInt()} or more characters",
                    Toast.LENGTH_LONG).show()
                binding.textInputPassword.error =
                    "Minimum ${getString(R.string.minimum_password_length).toInt()} characters required"

            } else if (password != rePassword) {
                Toast.makeText(this, "Both passwords must be same", Toast.LENGTH_LONG).show()
                binding.textInputReEnterPassword.error = "Passwords must match"

            } else {
                user = User(
                    firstName,
                    lastName,
                    photoUri.toString(),
                    country,
                    gender,
                    interestList.toList(),
                    birthday!!,
                    aboutMe,
                    email,
                    password
                )

                if(viewModel.status.value != SignUpStatus.CHECKING || viewModel.status.value != SignUpStatus.SIGNING_UP) {
                    viewModel.checkAndSignUpUser(CheckRegistrationInput(email), user)
                }
            }
        }

        binding.selectBirthday.setOnClickListener {
            datePickerDialog.show()
        }
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
            if (text.toString().length < 50 && errorEnabled)
                binding.textInputAboutMe.error = "Minimum 50 characters required"
            else
                binding.textInputAboutMe.error = null
        }
        binding.textInputEmail.editText?.doOnTextChanged { text, _, _, count ->
            if (!emailCheck(text.toString()) && errorEnabled)
                binding.textInputEmail.error = "Invalid email"
            else
                binding.textInputEmail.error = null
        }
        binding.textInputPassword.editText?.doOnTextChanged { text, _, _, count ->
            if (text.toString().length < 6 && errorEnabled)
                binding.textInputPassword.error =
                    "Minimum ${getString(R.string.minimum_password_length).toInt()} characters required"
            else
                binding.textInputPassword.error = null
        }
        binding.textInputReEnterPassword.editText?.doOnTextChanged { text, _, _, count ->
            if (text.toString() != binding.textInputPassword.editText?.text.toString() && errorEnabled)
                binding.textInputReEnterPassword.error = "Passwords must match"
            else
                binding.textInputReEnterPassword.error = null
        }

    }

    private fun goToNextActivity() {
        val intent: Intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun setUpInterestedInChipGroup(chipText: String, position: Int) {
        val chip = layoutInflater.inflate(R.layout.interest_chip_item, binding.interestsChipGroup,
            false) as Chip
        chip.text = chipText
        if (position == interestList.size) {
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
                                    Toast.makeText(this@SignUpActivity,
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
                binding.birthdayText.visibility = View.VISIBLE
                binding.selectBirthday.text = getString(R.string.change_birthday)

                val sdf: SimpleDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                birthday = sdf.parse(formattedDate)!!.time
            } else {
                Toast.makeText(this@SignUpActivity,
                    "Please select the correct date",
                    Toast.LENGTH_LONG).show()
            }
        }

        datePickerDialog = SpinnerDatePickerDialogBuilder()
            .context(this@SignUpActivity)
            .callback(mDateSetListener)
            .spinnerTheme(R.style.NumberPickerStyle)
            .showTitle(true)
            .showDaySpinner(true)
            .defaultDate(year, month, day)
            .build()

    }

    private fun setUpActionBar() {
        val actionbar = supportActionBar
        actionbar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        actionbar?.setCustomView(R.layout.sign_up_action_bar)

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE && resultCode == RESULT_OK) {

            val imageUri: Uri = CropImage.getPickImageResultUri(this@SignUpActivity, data);

            if (CropImage.isReadExternalStoragePermissionsRequired(this@SignUpActivity,
                    imageUri)
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 0);
                }
            } else {
                CropImage.activity(imageUri)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setCropShape(CropImageView.CropShape.OVAL)
                    .setAspectRatio(1, 1)
                    .setRequestedSize(500, 500)
                    .start(this@SignUpActivity);
            }
        } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (data != null) {
                val result: CropImage.ActivityResult = CropImage.getActivityResult(data);

                if (resultCode == RESULT_OK && result != null) {
                    photoUri = result.uri;
                    binding.accountCircleImage.setImageURI(photoUri);
                } else {
                    Toast.makeText(this@SignUpActivity, "Failed to load image!", Toast.LENGTH_SHORT)
                        .show();
                }
            } else {
                Toast.makeText(this@SignUpActivity, "Failed to load image!", Toast.LENGTH_SHORT)
                    .show();
            }

        }
    }
}