package com.leminect.stranzee.view

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.chip.Chip
import com.leminect.stranzee.R
import com.leminect.stranzee.databinding.ActivityStrangeeProfileBinding
import com.leminect.stranzee.model.SingleChatPerson
import com.leminect.stranzee.model.Strangee
import com.leminect.stranzee.model.User
import com.leminect.stranzee.network.SocketManager
import com.leminect.stranzee.utility.getFromSharedPreferences
import com.leminect.stranzee.viewmodel.StrangeeProfileStatus
import com.leminect.stranzee.viewmodel.StrangeeProfileViewModel
import com.leminect.stranzee.viewmodelfactory.StrangeeProfileViewModelFactory
import java.lang.Exception

class StrangeeProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStrangeeProfileBinding
    private lateinit var viewModel: StrangeeProfileViewModel
    private lateinit var loadingDialog: CustomLoadingDialog
    private lateinit var errorDialog: HybridDialog
    private lateinit var failedDialog: HybridDialog
    private lateinit var strangee: Strangee
    private lateinit var user: User
    private lateinit var token: String
    private var menu: Menu? = null

    companion object {
        var currentActivity: Activity? = null
        fun finishActivity() {
            try {
                currentActivity?.finish()
                currentActivity = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpActionBar()

        binding = DataBindingUtil.setContentView(this, R.layout.activity_strangee_profile)
        binding.lifecycleOwner = this
        strangee = intent.getSerializableExtra("strangee_data") as Strangee
        binding.strangee = strangee

        currentActivity = this

        val userData = getFromSharedPreferences(this)
        user = userData.second
        token = userData.first

        val viewModelFactory = StrangeeProfileViewModelFactory(token, user.userId, strangee.userId)
        viewModel =
            ViewModelProvider(this, viewModelFactory).get(StrangeeProfileViewModel::class.java)
        binding.strangeeProfileViewModel = viewModel

        binding.aboutMe.visibility = if(strangee.aboutMe == "-") View.GONE else View.VISIBLE
        binding.aboutMeTitleText.visibility = if(strangee.aboutMe == "-") View.GONE else View.VISIBLE

        strangee.interestedIn.forEach { interest ->
            val chip =
                layoutInflater.inflate(R.layout.interest_chip_item, binding.interestsChipGroup,
                    false) as Chip
            chip.text = interest
            chip.setCloseIconVisible(R.bool._false)
            binding.interestsChipGroup.addView(chip)
        }

        loadingDialog = CustomLoadingDialog(this, false, "Reporting...")
        failedDialog = HybridDialog(this,
            arrayOf(View.VISIBLE, View.GONE, View.GONE, View.VISIBLE, View.GONE),
            arrayOf("Failed",
                "",
                "",
                "An error occurred while performing the selected action. Please try again!"),
            true,
            object : OkButtonListener {
                override fun onOkClick(interests: String, dismissDialog: (Boolean) -> Unit) {
                    dismissDialog(true)
                }

            })

        errorDialog = HybridDialog(this,
            arrayOf(View.VISIBLE, View.GONE, View.GONE, View.VISIBLE, View.GONE),
            arrayOf("Error",
                "",
                "",
                "An error occurred. Please check your internet connection and try again!"),
            true,
            object : OkButtonListener {
                override fun onOkClick(interests: String, dismissDialog: (Boolean) -> Unit) {
                    dismissDialog(true)
                }

            })

        viewModel.saveBackData.observe(this, Observer { saveStrangeeBackData ->
            saveStrangeeBackData?.let {
                if (saveStrangeeBackData.error) {
                    strangee.saved = saveStrangeeBackData.saveStatus
                    binding.invalidateAll()
                    menu?.findItem(R.id.profile_save)?.title =
                        if (strangee.saved) "Unsave profile" else "Save profile"
                    Toast.makeText(this,
                        "Failed to ${if (saveStrangeeBackData.saveStatus) "unsave" else "save"} profile",
                        Toast.LENGTH_SHORT).show()
                }
            }
        })

        viewModel.blockedBackData.observe(this, Observer { blocked ->
            blocked?.let {
                if (blocked) {
                    menu?.findItem(R.id.profile_block)?.title = "Unblock"
                } else {
                    menu?.findItem(R.id.profile_block)?.title = "Block"
                }
            }
        })

        viewModel.status.observe(this, Observer { status ->
            status?.let {
                when (status) {
                    StrangeeProfileStatus.ERROR -> {
                        viewModel.onPopupComplete()
                        loadingDialog.dismissDialog()
                        errorDialog.showDialog()
                    }
                    StrangeeProfileStatus.FAILED -> {
                        viewModel.onPopupComplete()
                        loadingDialog.dismissDialog()
                        failedDialog.showDialog()
                    }
                    StrangeeProfileStatus.REPORTING -> loadingDialog.showDialog()
                    StrangeeProfileStatus.REPORT_DONE -> {
                        loadingDialog.dismissDialog()
                        Toast.makeText(this, "Reported", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })

        viewModel.blockingProcessBackData.observe(this, Observer { data ->
            data?.let {
                if (data.error) {
//                    menu?.findItem(R.id.profile_block)?.title = if(data.blockedStatus) "Unblock" else "Block"
                    Toast.makeText(this,
                        "Failed to ${if (data.blockedStatus) "unblock" else "block"}",
                        Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this,
                        if (data.blockedStatus) "Blocked" else "Unblocked",
                        Toast.LENGTH_LONG).show()
                }
            }
        })

        binding.chatButton.setOnClickListener {
            startChatActivity()
        }

        binding.saveProfileButton.setOnClickListener {
            saveOrUnsaveProfile()
        }
    }

    private fun saveOrUnsaveProfile() {
        val s = strangee.copy()
        menu?.findItem(R.id.profile_save)?.title =
            if (strangee.saved) "Save profile" else "Unsave profile"
        strangee.saved = !strangee.saved
        binding.invalidateAll()

        viewModel.saveOrUnSaveProfile(token, s)
    }

    private fun startChatActivity() {
        SingleChatActivity.finishActivity()

        val intent: Intent = Intent(this, SingleChatActivity::class.java)
        val chatPerson = SingleChatPerson(strangee.userId,
            strangee.firstName,
            strangee.lastName,
            strangee.imageUrl,
            viewModel.isOnline.value?: false,
            strangee.country,
            strangee.gender,
            strangee.interestedIn,
            strangee.birthday,
            strangee.aboutMe,
            strangee.saved)
        intent.putExtra("chat_person", chatPerson)
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        this.menu = menu
        menuInflater.inflate(R.menu.strangee_profile_menu, menu)
        menu?.findItem(R.id.profile_save)?.title =
            if (strangee.saved) "Unsave profile" else "Save profile"
        if (viewModel.blockedBackData.value == true) menu?.findItem(R.id.profile_block)?.title =
            "Unblock"
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.profile_chat -> startChatActivity()
            R.id.profile_save -> saveOrUnsaveProfile()
            R.id.profile_block -> {
                if (viewModel.blockedBackData.value == true) {
                    viewModel.blockUser(token)
                } else {
                    HybridDialog(
                        this,
                        arrayOf(View.VISIBLE, View.GONE, View.GONE, View.VISIBLE, View.VISIBLE),
                        arrayOf("Block user",
                            "",
                            "",
                            "Are you sure you want to block this user?"),
                        false,
                        object : OkButtonListener {
                            override fun onOkClick(
                                interests: String,
                                dismissDialog: (Boolean) -> Unit,
                            ) {
                                dismissDialog(true)
                                viewModel.blockUser(token)
                            }
                        },
                        "Yes"
                    ).showDialog()
                }
            }
            R.id.profile_report -> {
                HybridDialog(
                    this,
                    arrayOf(View.VISIBLE, View.VISIBLE, View.VISIBLE, View.GONE, View.VISIBLE),
                    arrayOf("Report user",
                        "Enter the proper cause for reporting the user",
                        "Message",
                        ""),
                    false,
                    object : OkButtonListener {
                        override fun onOkClick(
                            interests: String,
                            dismissDialog: (Boolean) -> Unit,
                        ) {
                            if (interests.isBlank()) {
                                Toast.makeText(this@StrangeeProfileActivity,
                                    "Please enter the message",
                                    Toast.LENGTH_LONG).show()
                                dismissDialog(false)
                            } else {
                                dismissDialog(true)
                                viewModel.reportUser(token, user.userId, interests)
                            }
                        }
                    },
                    "Report"
                ).showDialog()
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun setUpActionBar() {
        val actionbar = supportActionBar
        actionbar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        actionbar?.setDisplayShowCustomEnabled(true)
        actionbar?.setCustomView(R.layout.strangee_profile_action_bar)

        supportActionBar?.customView?.findViewById<ImageView>(R.id.back_button)
            ?.setOnClickListener {
                onBackPressed()
            }
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