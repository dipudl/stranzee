package com.leminect.strangee.view

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.chip.Chip
import com.leminect.strangee.R
import com.leminect.strangee.databinding.FragmentProfileBinding
import com.leminect.strangee.model.User
import com.leminect.strangee.utility.emailCheck
import com.leminect.strangee.utility.getFromSharedPreferences
import com.leminect.strangee.viewmodel.ProfileUpdateStatus
import com.leminect.strangee.viewmodel.ProfileViewModel
import com.leminect.strangee.viewmodel.SignUpViewModel
import com.canhub.cropper.CropImage
import com.canhub.cropper.CropImageView
import java.util.ArrayList

class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private var photoUri: Uri? = null
    private lateinit var failedHybridDialog: HybridDialog
    private lateinit var errorHybridDialog: HybridDialog
    private lateinit var loadingDialog: CustomLoadingDialog
    private lateinit var user: User
    private lateinit var prefs: SharedPreferences
    private lateinit var token: String
    private var goneToEditDetails = false

    private val viewModel: ProfileViewModel by lazy {
        ViewModelProvider(this).get(ProfileViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_profile, container, false)
        binding.lifecycleOwner = this
        setUpCustomActionBar()

        prefs = requireContext().getSharedPreferences(getString(R.string.shared_prefs_name), AppCompatActivity.MODE_PRIVATE)
        populateViewWithData()

        loadingDialog = CustomLoadingDialog(requireContext(), false, "Updating profile picture...")
        errorHybridDialog = HybridDialog(
            requireContext(),
            arrayOf(View.VISIBLE, View.GONE, View.GONE, View.VISIBLE, View.GONE),
            arrayOf("Update Error",
                "",
                "",
                "An error occurred while updating the profile picture. Please check your internet connection and try again!"),
            true,
            object : OkButtonListener {
                override fun onOkClick(interests: String, dismissDialog: (Boolean) -> Unit) {
                    dismissDialog(true)
                }
            }
        )
        failedHybridDialog = HybridDialog(
            requireContext(),
            arrayOf(View.VISIBLE, View.GONE, View.GONE, View.VISIBLE, View.GONE),
            arrayOf("Update Failed",
                "",
                "",
                "An error occurred while updating the profile picture. Please try again!"),
            true,
            object : OkButtonListener {
                override fun onOkClick(interests: String, dismissDialog: (Boolean) -> Unit) {
                    dismissDialog(true)
                }
            }
        )

        viewModel.status.observe(viewLifecycleOwner, Observer { status ->
            status?.let {
                when(status) {
                    ProfileUpdateStatus.UPDATING -> loadingDialog.showDialog()
                    ProfileUpdateStatus.UPDATE_DONE -> {
                        loadingDialog.dismissDialog()
                        binding.profileCircleImageView.setImageURI(photoUri)
                        Toast.makeText(context, "Profile picture updated", Toast.LENGTH_SHORT).show()

                        // reset glide cache for profile image
                        prefs.edit().putString(getString(R.string.prefs_signature), System.currentTimeMillis().toString()).apply()
                    }
                    ProfileUpdateStatus.UPDATE_FAILED -> {
                        loadingDialog.dismissDialog()
                        failedHybridDialog.showDialog()
                    }
                    ProfileUpdateStatus.UPDATE_ERROR -> {
                        loadingDialog.dismissDialog()
                        errorHybridDialog.showDialog()
                    }
                }
            }
        })

        binding.changeProfilePicture.setOnClickListener {
            // start picker to get image for cropping
            CropImage.startPickImageActivity(requireContext(), this@ProfileFragment)
        }

        binding.editProfileLayout.setOnClickListener{
            val intent: Intent = Intent(context, EditDetailsActivity::class.java)
            intent.putExtra("token", token)
            intent.putExtra("user", user)

            goneToEditDetails = true
            startActivity(intent)
        }

        return binding.root
    }

    private fun populateViewWithData() {
        val userData = getFromSharedPreferences(requireContext())
        token = userData.first
        user = userData.second

        binding.user = user

        binding.interestsChipGroup.removeAllViews()
        user.interestedIn.forEach { interest ->
            val chip = layoutInflater.inflate(R.layout.interest_chip_item, binding.interestsChipGroup,
                false) as Chip
            chip.text = interest
            chip.setCloseIconVisible(R.bool._false)
            binding.interestsChipGroup.addView(chip)
        }
    }

    override fun onStart() {
        super.onStart()

        if(goneToEditDetails) {
            goneToEditDetails = false

            // update details from shared prefs
            populateViewWithData()
        }
    }

    private fun setUpCustomActionBar() {
        val customActionBar = (activity as? AppCompatActivity)?.supportActionBar?.customView
        val searchLayout = customActionBar?.findViewById<LinearLayout>(R.id.search_layout)
        val mainLayout = customActionBar?.findViewById<RelativeLayout>(R.id.main_layout)
        val fragmentText = customActionBar?.findViewById<TextView>(R.id.fragment_text)

        mainLayout?.visibility = View.VISIBLE
        searchLayout?.visibility = View.GONE
        fragmentText?.text = getString(R.string.profile_capital)
        customActionBar?.findViewById<ImageView>(R.id.custom_right_button)?.visibility = View.INVISIBLE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE && resultCode == AppCompatActivity.RESULT_OK) {

            val imageUri: Uri = CropImage.getPickImageResultUriContent(requireContext(), data)

            if (CropImage.isReadExternalStoragePermissionsRequired(requireContext(), imageUri)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 0)
                }
            } else {
                CropImage.activity(imageUri)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setCropShape(CropImageView.CropShape.OVAL)
                    .setAspectRatio(1, 1)
                    .setRequestedSize(500, 500)
                    .start(requireContext(), this@ProfileFragment)
            }
        } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (data != null) {
                val result = CropImage.getActivityResult(data)

                if (resultCode == AppCompatActivity.RESULT_OK && result != null) {
                    photoUri = result.uriContent
                    // binding.profileCircleImageView.setImageURI(photoUri)

                    // upload photo
                    viewModel.updateProfileImage(token, result.getUriFilePath(requireContext())!!, user.userId)

                } else {
                    Toast.makeText(context, "Failed to load image!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Failed to load image!", Toast.LENGTH_SHORT).show()
            }

        }
    }

}