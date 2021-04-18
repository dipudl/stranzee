package com.leminect.strangee.view

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import com.leminect.strangee.R
import com.leminect.strangee.adapter.ChatMessageClickListener
import com.leminect.strangee.adapter.SingleChatAdapter
import com.leminect.strangee.databinding.ActivitySingleChatBinding
import com.leminect.strangee.databinding.SingleChatActionBarBinding
import com.leminect.strangee.model.Message
import com.leminect.strangee.model.SingleChatPerson
import com.leminect.strangee.model.Strangee
import hani.momanii.supernova_emoji_library.Actions.EmojIconActions

class SingleChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySingleChatBinding
    private lateinit var singleChatPerson: SingleChatPerson
    private lateinit var emojIconActions: EmojIconActions
    private val PICK_IMAGE_REQUEST = 1012

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySingleChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        singleChatPerson = intent.getSerializableExtra("chat_person") as SingleChatPerson
        setUpActionBar()

        val adapter = SingleChatAdapter(ChatMessageClickListener { message ->
            Toast.makeText(this@SingleChatActivity, message.type, Toast.LENGTH_SHORT).show()
        })

        binding.chatMessageRecyclerView.adapter = adapter

        val messageList = listOf<Message>(
            Message("test", "Hello! How are you?", "text", null, 1617001412000, "38HE89R3OIS"),
            Message("random", "I'm fine! What about you?", "text", null, 1617001423000, "IR9RUTWEIO"),
            Message("test", null, "image",
                "https://www.awakenthegreatnesswithin.com/wp-content/uploads/2018/08/Nature-Quotes-1.jpg",
                1617911001000, "FJHSK48UE8O"),
            Message("random", null, "image",
                "https://images.unsplash.com/photo-1503023345310-bd7c1de61c7d?ixid=MXwxMjA3fDB8MHxzZWFyY2h8MXx8aHVtYW58ZW58MHx8MHw%3D&ixlib=rb-1.2.1&w=1000&q=80",
                1617911002000, "EJIDF983O4U8"),
            Message("test", "I'm pretty good. Thinking of throwing a huge party this Christmas.",
                "text", null, 1617911727000, "UOIOD8949843"),
            Message("test", "Will you be able to join?",
                "text", null, 1617911727001, "FDHG76NGFH"),
        )

        adapter.submitList(messageList)
        binding.chatMessageRecyclerView.scrollToPosition(messageList.size - 1)

        emojIconActions = EmojIconActions(applicationContext, binding.root, binding.messageInput, binding.sendEmojiButton);
        emojIconActions.setIconsIds(R.drawable.ic_keyboard, R.drawable.ic_send_emoji)
        emojIconActions.setUseSystemEmoji(true)

        var firstTimeEmojiClicked = true
        binding.sendEmojiButton.setOnClickListener{
                if(firstTimeEmojiClicked) {
                    binding.sendEmojiButton.setOnClickListener(null)
                    emojIconActions.ShowEmojIcon()
                    binding.sendEmojiButton.performClick()

                    firstTimeEmojiClicked = false
                }
        }

        binding.sendImage.setOnClickListener{
            val imageIntent = Intent();
            imageIntent.type = "image/*";
            imageIntent.action = Intent.ACTION_GET_CONTENT;
            startActivityForResult(imageIntent, PICK_IMAGE_REQUEST);
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(resultCode == RESULT_OK && data != null && data.data != null) {
            if (requestCode == PICK_IMAGE_REQUEST) {
                val imagePath: Uri = data.data!!

                // use imagePath to upload image
            }
        }
    }

    private fun setUpActionBar() {
        val actionbarBinding: SingleChatActionBarBinding =
            SingleChatActionBarBinding.inflate(layoutInflater)
        actionbarBinding.chatPerson = singleChatPerson

        val actionbar = supportActionBar
        actionbar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        actionbar?.setDisplayShowCustomEnabled(true)
        actionbar?.customView = actionbarBinding.root

        actionbarBinding.backButton.setOnClickListener {
            onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.single_chat_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.show_profile -> Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show()
            R.id.single_chat_block -> Toast.makeText(this, "Blocked", Toast.LENGTH_SHORT).show()
            R.id.single_chat_report -> Toast.makeText(this, "Reported", Toast.LENGTH_SHORT).show()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}