package com.leminect.strangee.view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.databinding.DataBindingUtil
import com.google.android.material.chip.Chip
import com.leminect.strangee.R
import com.leminect.strangee.databinding.ActivityStrangeeProfileBinding
import com.leminect.strangee.model.SingleChatPerson
import com.leminect.strangee.model.Strangee

class StrangeeProfileActivity : AppCompatActivity() {
    private  lateinit var binding:ActivityStrangeeProfileBinding
    private lateinit var strangee: Strangee

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpActionBar()

        binding = DataBindingUtil.setContentView(this, R.layout.activity_strangee_profile)
        strangee = intent.getSerializableExtra("strangee_data") as Strangee
        binding.strangee = strangee

        strangee.interestedIn.forEach { interest ->
            val chip = layoutInflater.inflate(R.layout.interest_chip_item, binding.interestsChipGroup,
                false) as Chip
            chip.text = interest
            chip.setCloseIconVisible(R.bool._false)
            binding.interestsChipGroup.addView(chip)
        }

        binding.chatButton.setOnClickListener{
            startChatActivity()
        }

        binding.saveProfileButton.setOnClickListener{
            saveOrUnsaveProfile()
        }
    }

    private fun saveOrUnsaveProfile() {
        strangee.saved = !strangee.saved
        binding.invalidateAll()
    }

    private fun startChatActivity() {
        val intent: Intent = Intent(this, SingleChatActivity::class.java)
        // TODO: put exact value of isOnline
        val chatPerson = SingleChatPerson(strangee.userId, strangee.firstName, strangee.lastName, strangee.imageUrl, true)
        intent.putExtra("chat_person", chatPerson)
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.strangee_profile_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.profile_chat -> startChatActivity()
            R.id.profile_save -> saveOrUnsaveProfile()
            R.id.profile_block -> Toast.makeText(this, "Blocked", Toast.LENGTH_SHORT).show()
            R.id.profile_report -> Toast.makeText(this, "Reported", Toast.LENGTH_SHORT).show()
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
}