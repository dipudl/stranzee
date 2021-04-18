package com.leminect.strangee.view

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.databinding.DataBindingUtil
import com.leminect.strangee.R
import com.leminect.strangee.adapter.StrangeeClickListener
import com.leminect.strangee.adapter.StrangeeGridAdapter
import com.leminect.strangee.databinding.ActivityWhoCheckedMeBinding
import com.leminect.strangee.model.Strangee

class WhoCheckedMeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWhoCheckedMeBinding
    private val placeholderText = "I am Dylan, one of the highly passionate blogger and tech enthusiast. " +
            "Interested in making new friends worldwide."
    private val placeholderImage = listOf(
        "https://images.unsplash.com/photo-1456327102063-fb5054efe647?ixlib=rb-0.3.5&q=80&fm=jpg&crop=faces&fit=crop&h=200&w=200&s=f05c14dd4db49f08a789e6449604c490",
        "https://images.unsplash.com/photo-1464863979621-258859e62245?ixlib=rb-0.3.5&q=80&fm=jpg&crop=faces&fit=crop&h=200&w=200&s=d1ff5086e5ca75cda4bcc8e470d8af11",
        "https://images.pexels.com/photos/61100/pexels-photo-61100.jpeg?crop=faces&fit=crop&h=200&w=200&auto=compress&cs=tinysrgb"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_who_checked_me)

        setUpActionBar()
        val adapter = StrangeeGridAdapter(StrangeeClickListener({ strangee ->
            Toast.makeText(this@WhoCheckedMeActivity, strangee.toString(), Toast.LENGTH_SHORT).show()
        },{},{ strangee ->
            Toast.makeText(this@WhoCheckedMeActivity, strangee.saved.toString(), Toast.LENGTH_SHORT).show()
        }), true)

        binding.checkedRecyclerView.adapter = adapter

        val listData = listOf<Strangee>(
            Strangee("abcd", "Edmund", "Paul", placeholderImage[0],
                "United States", "Male", listOf("Swimming", "Technology", "Music", "Blogging"),
                885566388000, placeholderText, true),
            Strangee("abcd", "Edmund", "Paul", placeholderImage[1],
                "United States", "Male", listOf("Swimming", "Technology", "Music", "Blogging"),
                885566388000, placeholderText, true),
            Strangee("abcd", "Edmund", "Paul", placeholderImage[2],
                "United States", "Male", listOf("Swimming", "Technology", "Music", "Blogging"),
                885566388000, placeholderText, true)
        )
        adapter.submitList(listData)
    }

    private fun setUpActionBar() {
        val actionbar = supportActionBar
        actionbar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        actionbar?.setCustomView(R.layout.sign_up_action_bar)
        actionbar?.customView?.findViewById<TextView>(R.id.action_bar_text)?.text =
            getString(R.string.who_checked_me)

        supportActionBar?.customView?.findViewById<ImageView>(R.id.back_button)
            ?.setOnClickListener {
                onBackPressed()
            }
    }
}