package com.leminect.stranzee.view

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.leminect.stranzee.R
import com.leminect.stranzee.adapter.ChatMessageClickListener
import com.leminect.stranzee.adapter.SingleChatAdapter
import com.leminect.stranzee.databinding.ActivitySingleChatBinding
import com.leminect.stranzee.databinding.SingleChatActionBarBinding
import com.leminect.stranzee.model.Message
import com.leminect.stranzee.model.SingleChatPerson
import com.leminect.stranzee.model.Strangee
import com.leminect.stranzee.model.User
import com.leminect.stranzee.network.SocketManager
import com.leminect.stranzee.utility.FetchPath
import com.leminect.stranzee.utility.getFromSharedPreferences
import com.leminect.stranzee.viewmodel.MessageLoad
import com.leminect.stranzee.viewmodel.SingleChatStatus
import com.leminect.stranzee.viewmodel.SingleChatViewModel
import com.leminect.stranzee.viewmodelfactory.SingleChatViewModelFactory
import hani.momanii.supernova_emoji_library.Actions.EmojIconActions
import java.lang.Exception


class SingleChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySingleChatBinding
    private lateinit var singleChatPerson: SingleChatPerson
    private lateinit var emojIconActions: EmojIconActions
    private lateinit var viewModel: SingleChatViewModel
    private lateinit var user: User
    private lateinit var token: String
    private var isKeyboardHidden: Boolean = true
    private val PICK_IMAGE_REQUEST = 1012
    private val PERMISSIONS_REQUEST = 1023
    private val mimeTypes = arrayOf("image/jpeg", "image/jpg", "image/png")

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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSIONS_REQUEST) {
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

        currentActivity = this

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
            if (message.type == "image") {
                startActivity(Intent(this, ImageShowActivity::class.java).putExtra("image",
                    message.imageUrl))
            }
        })

        binding.chatMessageRecyclerView.adapter = adapter

        viewModel.isBlocked.observe(this, Observer { blocked ->
            blocked?.let {
                if(blocked) {
                    binding.statusAnimation.apply {
                        setAnimation(R.raw.blocked_anim)
                        visibility = View.VISIBLE
                        if (!this.isAnimating) this.playAnimation()
                    }
                    binding.errorTextView.text =
                        getString(R.string.blocked_message)
                    binding.errorTextView.visibility = View.VISIBLE
                    binding.chatMessageRecyclerView.visibility = View.GONE
                } else {
                    binding.statusAnimation.visibility = View.GONE
                    binding.errorTextView.visibility = View.GONE
                    binding.chatMessageRecyclerView.visibility = View.VISIBLE
                }

                binding.reloadButton.visibility = View.GONE
            }
        })

        viewModel.messageList.observe(this, Observer { messageList ->
            if (messageList.isNotEmpty()) {
                adapter.submitList(messageList)
                if (viewModel.initialOldMessageLoad.value == true) {
                    binding.chatMessageRecyclerView.scrollToPosition(messageList.size - 1)
                    viewModel.onInitialOldMessageLoadComplete()
                }

                if (viewModel.fetchedMessageIsNew.value == true && viewModel.scrollToNewMessage.value == true) {
                    object : CountDownTimer(200, 200) {
                        override fun onTick(millisUntilFinished: Long) {
                        }

                        override fun onFinish() {
                            binding.chatMessageRecyclerView.scrollToPosition(messageList.size - 1)
                        }

                    }.start()
                }
            }
        })

        binding.reloadButton.setOnClickListener {
            viewModel.checkBlocked()
        }

        viewModel.messageLoadStatus.observe(this, Observer { status ->
            status?.let {
                if ((viewModel.messageList.value?.size ?: 0) == 0) {
                    when (status) {
                        MessageLoad.LOADING -> {
                            binding.reloadButton.visibility = View.GONE
                            binding.statusAnimation.apply {
                                setAnimation(R.raw.chats_loading_anim)
                                visibility = View.VISIBLE
                                if (!this.isAnimating) this.playAnimation()
                            }
                            binding.errorTextView.visibility = View.GONE
                        }
                        MessageLoad.LOADING_ERROR -> {
                            binding.reloadButton.visibility = View.VISIBLE
                            binding.statusAnimation.apply {
                                setAnimation(R.raw.internet_error_anim)
                                visibility = View.VISIBLE
                                if (!this.isAnimating) this.playAnimation()
                            }
                            binding.errorTextView.text =
                                getString(R.string.error_loading_messages)
                            binding.errorTextView.visibility = View.VISIBLE
                        }
                        MessageLoad.LOADING_FAILED -> {
                            binding.reloadButton.visibility = View.VISIBLE
                            binding.statusAnimation.apply {
                                setAnimation(R.raw.internet_error_anim)
                                visibility = View.VISIBLE
                                if (!this.isAnimating) this.playAnimation()
                            }
                            binding.errorTextView.text =
                                getString(R.string.failed_loading_messages)
                            binding.errorTextView.visibility = View.VISIBLE
                        }
                        MessageLoad.EMPTY -> {
                            binding.reloadButton.visibility = View.GONE
                            binding.statusAnimation.apply {
                                setAnimation(R.raw.no_results_found)
                                visibility = View.VISIBLE
                                if (!this.isAnimating) this.playAnimation()
                            }
                            binding.errorTextView.text =
                                getString(R.string.no_messages, singleChatPerson.firstName)
                            binding.errorTextView.visibility = View.VISIBLE
                        }
                        MessageLoad.LOADING_DONE -> {
                            binding.reloadButton.visibility = View.GONE
                            binding.statusAnimation.visibility = View.GONE
                            binding.errorTextView.visibility = View.GONE
                        }
                    }
                } else {
                    binding.reloadButton.visibility = View.GONE
                    binding.statusAnimation.visibility = View.GONE
                    binding.errorTextView.visibility = View.GONE
                }
            }
        })

        if (binding.messageInput.hasFocus()) {
            binding.messageInput.clearFocus()
        }

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

        viewModel.status.observe(this, Observer { status ->
            status?.let {
                when(status) {
                    SingleChatStatus.UPLOADING -> {
                        binding.imageUploadingLayout.visibility = View.VISIBLE
                    }
                    SingleChatStatus.UPLOAD_FAILED, SingleChatStatus.UPLOAD_ERROR -> {
                        Toast.makeText(this, "Failed to upload image. Try again!", Toast.LENGTH_LONG).show()
                        binding.imageUploadingLayout.visibility = View.GONE
                        viewModel.clearUploadStatus()
                    }
                    SingleChatStatus.UPLOAD_DONE -> {
                        binding.imageUploadingLayout.visibility = View.GONE
                        viewModel.clearUploadStatus()
                    }
                }
            }
        })

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
            when (viewModel.isBlocked.value) {
                false -> {
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
                true -> {
                    Toast.makeText(this, "Cannot send message because this user has blocked you.", Toast.LENGTH_LONG).show()
                }
                else -> {
                    Toast.makeText(this, "Loading messages. Please wait...", Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.chatMessageRecyclerView.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager?
                if (layoutManager != null) {
                    val totalItemCount = layoutManager.itemCount
                    val firstVisible = layoutManager.findFirstVisibleItemPosition()

                    if (layoutManager.findLastVisibleItemPosition() + 5 >= totalItemCount) {
                        viewModel.setScrollToNewMessage(true)
                    } else {
                        viewModel.setScrollToNewMessage(false)
                    }

                    if (viewModel.scrollPaginationEnabled.value == true) {
                        val startHasBeenReached = (firstVisible - 5) <= 0

                        if ((totalItemCount > 0) && startHasBeenReached) {
                            //you have reached to the start of recycler view, load previous messages
                            viewModel.getOlderMessages()
                        }
                    }
                }
            }
        })

        binding.mainLayout.viewTreeObserver.addOnGlobalLayoutListener {
            val heightDiff: Int = binding.mainLayout.rootView.height - binding.mainLayout.height
            Log.i("SingleChatActivityKeybo", "Height diff: $heightDiff")
            if (heightDiff > 500) {
                //keyboard is open
                Log.i("SingleChatActivityKeybo", "Keyboard open")
                if (isKeyboardHidden) {
                    isKeyboardHidden = false
                    viewModel.messageList.value?.let {
                        if (it.isNotEmpty())
                            binding.chatMessageRecyclerView.scrollToPosition(it.size - 1)
                    }
                }
                if (!binding.messageInput.hasFocus()) {
                    binding.messageInput.requestFocus()
                }
            } else {
                //keyboard is hidden
                isKeyboardHidden = true

                Log.i("SingleChatActivityKeybo", "Keyboard hidden")
                if (binding.messageInput.hasFocus()) {
                    binding.messageInput.clearFocus()
                }
            }
        }
    }

    private fun pickImage() {
        when (viewModel.isBlocked.value) {
            false -> {
                if(viewModel.status.value != SingleChatStatus.UPLOADING) {
                    val imageIntent = Intent()
                        .setType("image/*")
                        .setAction(Intent.ACTION_GET_CONTENT)
                        .putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
                    startActivityForResult(imageIntent, PICK_IMAGE_REQUEST)
                } else {
                    Toast.makeText(this, "Uploading previous image. Please wait...", Toast.LENGTH_LONG).show()
                }
            }
            true -> {
                Toast.makeText(this, "Cannot send image because this user has blocked you.", Toast.LENGTH_LONG).show()
            }
            else -> {
                Toast.makeText(this, "Loading messages. Please wait...", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && data != null && data.data != null) {
            if (requestCode == PICK_IMAGE_REQUEST) {
                val imageUri: Uri = data.data!!
                val imagePath: String? = FetchPath.getPath(this, imageUri)

                // use imagePath to upload image
                if (imagePath != null) {
                    viewModel.uploadImage(imagePath)
                } else {
                    Toast.makeText(this, "Failed to upload image. Try again!", Toast.LENGTH_SHORT)
                        .show()
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
            R.id.show_profile -> {
                StrangeeProfileActivity.finishActivity()
                goToStrangeeProfile()
            }
            // R.id.single_chat_block -> Toast.makeText(this, "Blocked", Toast.LENGTH_SHORT).show()
            // R.id.single_chat_report -> Toast.makeText(this, "Reported", Toast.LENGTH_SHORT).show()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun goToStrangeeProfile() {
        val strangee: Strangee = Strangee(
            singleChatPerson.userId,
            singleChatPerson.firstName,
            singleChatPerson.lastName,
            singleChatPerson.imageUrl,
            singleChatPerson.country,
            singleChatPerson.gender,
            singleChatPerson.interestedIn,
            singleChatPerson.birthday,
            singleChatPerson.aboutMe,
            singleChatPerson.saved
        )

        val intent: Intent = Intent(this, StrangeeProfileActivity::class.java)
        intent.putExtra("strangee_data", strangee)
        startActivity(intent)
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