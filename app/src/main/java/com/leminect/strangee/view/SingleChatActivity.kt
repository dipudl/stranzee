package com.leminect.strangee.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.leminect.strangee.R
import com.leminect.strangee.adapter.ChatMessageClickListener
import com.leminect.strangee.adapter.SingleChatAdapter
import com.leminect.strangee.databinding.ActivitySingleChatBinding
import com.leminect.strangee.databinding.SingleChatActionBarBinding
import com.leminect.strangee.model.Message
import com.leminect.strangee.model.SingleChatPerson
import com.leminect.strangee.model.User
import com.leminect.strangee.network.SocketManager
import com.leminect.strangee.utility.FetchPath
import com.leminect.strangee.utility.getFromSharedPreferences
import com.leminect.strangee.viewmodel.SingleChatViewModel
import com.leminect.strangee.viewmodelfactory.SingleChatViewModelFactory
import hani.momanii.supernova_emoji_library.Actions.EmojIconActions


class SingleChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySingleChatBinding
    private lateinit var singleChatPerson: SingleChatPerson
    private lateinit var emojIconActions: EmojIconActions
    private lateinit var viewModel: SingleChatViewModel
    private lateinit var user: User
    private lateinit var token: String
    private val PICK_IMAGE_REQUEST = 1012
    private val PERMISSIONS_REQUEST = 1023
    private val mimeTypes = arrayOf("image/jpeg", "image/jpg", "image/png")

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == PERMISSIONS_REQUEST) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                ) {
                    pickImage()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySingleChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        singleChatPerson = intent.getSerializableExtra("chat_person") as SingleChatPerson
        val userData = getFromSharedPreferences(this)
        user = userData.second
        token = userData.first

        val viewModelFactory =
            SingleChatViewModelFactory(token, user.userId, singleChatPerson.userId)
        viewModel =
            ViewModelProvider(this, viewModelFactory).get(SingleChatViewModel::class.java)
        setUpActionBar()

        val adapter = SingleChatAdapter(user.userId, ChatMessageClickListener { message ->
            Toast.makeText(this@SingleChatActivity, message.type, Toast.LENGTH_SHORT).show()
        })

        binding.chatMessageRecyclerView.adapter = adapter

        viewModel.messageList.observe(this, Observer { messageList ->
            adapter.submitList(messageList)
            if (viewModel.initialOldMessageLoad.value == true) {
                binding.chatMessageRecyclerView.scrollToPosition(messageList.size - 1)
                viewModel.onInitialOldMessageLoadComplete()
            }

            if (messageList[messageList.size - 1].userId == user.userId) {

                object : CountDownTimer(200, 200) {
                    override fun onTick(millisUntilFinished: Long) {
                    }

                    override fun onFinish() {
                        binding.chatMessageRecyclerView.scrollToPosition(messageList.size - 1)
                    }

                }.start()
            }
        })

        emojIconActions = EmojIconActions(applicationContext,
            binding.root,
            binding.messageInput,
            binding.sendEmojiButton)
        emojIconActions.setIconsIds(R.drawable.ic_keyboard, R.drawable.ic_send_emoji)
        emojIconActions.setUseSystemEmoji(true)

        var firstTimeEmojiClicked = true
        binding.sendEmojiButton.setOnClickListener {
            if (firstTimeEmojiClicked) {
                binding.sendEmojiButton.setOnClickListener(null)
                emojIconActions.ShowEmojIcon()
                binding.sendEmojiButton.performClick()

                firstTimeEmojiClicked = false
            }
        }

        viewModel.imageUploadUrl.observe(this, Observer { imageUrl ->
            imageUrl?.let {
                val message = Message(user.userId,
                    singleChatPerson.userId,
                    null,
                    "image",
                    imageUrl,
                    System.currentTimeMillis(),
                    System.currentTimeMillis().toString())

                viewModel.onImageUrlUsed()
                viewModel.sendMessage(message)
            }
        })

        binding.sendImage.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
                    || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
                ) {
                    ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        PERMISSIONS_REQUEST)
                } else {
                    pickImage()
                }
            } else {
                pickImage()
            }
        }

        binding.sendMessageButton.setOnClickListener {
            val text = binding.messageInput.text.toString()

            if (text.trim().isNotEmpty()) {
                val message = Message(user.userId,
                    singleChatPerson.userId,
                    text,
                    "text",
                    null,
                    System.currentTimeMillis(),
                    System.currentTimeMillis().toString())
                viewModel.sendMessage(message)
            }

            binding.messageInput.setText("")
        }

        /*binding.chatMessageRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (viewModel.scrollPaginationEnabled.value == true) {
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager?
                    if (layoutManager != null) {
                        val totalItemCount = layoutManager.itemCount
                        val firstVisible = layoutManager.findFirstVisibleItemPosition()
                        val startHasBeenReached = (firstVisible - 5) <= 0

                        if ((totalItemCount > 0) && startHasBeenReached) {
                            //you have reached to the start of recycler view, load previous messages
                            viewModel.getStrangeeList(token, user, false)
                        }
                    }
                }
            }
        })*/
    }

    private fun pickImage() {
        val imageIntent = Intent()
            .setType("image/*")
            .setAction(Intent.ACTION_GET_CONTENT)
            .putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        startActivityForResult(imageIntent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && data != null && data.data != null) {
            if (requestCode == PICK_IMAGE_REQUEST) {
                val imageUri: Uri = data.data!!
                val imagePath: String? = FetchPath.getPath(this, imageUri)

                // use imagePath to upload image
                if(imagePath != null) {
                    viewModel.uploadImage(imagePath)
                } else {
                    Toast.makeText(this, "Failed to upload image. Try again!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setUpActionBar() {
        val actionbarBinding: SingleChatActionBarBinding =
            SingleChatActionBarBinding.inflate(layoutInflater)
        actionbarBinding.lifecycleOwner = this
        actionbarBinding.chatPerson = singleChatPerson
        actionbarBinding.singleChatViewModel = viewModel

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

    override fun onResume() {
        super.onResume()
        SocketManager.setOnline(true)
    }

    override fun onPause() {
        super.onPause()
        SocketManager.setOnline(false)
    }
}