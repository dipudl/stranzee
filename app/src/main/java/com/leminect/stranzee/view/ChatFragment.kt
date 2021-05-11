package com.leminect.stranzee.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
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
import com.leminect.stranzee.R
import com.leminect.stranzee.adapter.ChatListAdapter
import com.leminect.stranzee.adapter.ChatListClickListener
import com.leminect.stranzee.databinding.FragmentChatBinding
import com.leminect.stranzee.model.ChatData
import com.leminect.stranzee.model.SingleChatPerson
import com.leminect.stranzee.model.User
import com.leminect.stranzee.utility.getFromSharedPreferences
import com.leminect.stranzee.utility.showKeyboard
import com.leminect.stranzee.viewmodel.ChatStatus
import com.leminect.stranzee.viewmodel.ChatViewModel
import com.leminect.stranzee.viewmodelfactory.ChatViewModelFactory
import java.util.*

class ChatFragment : Fragment() {

    private lateinit var binding: FragmentChatBinding
    private lateinit var viewModel: ChatViewModel
    private lateinit var adapter: ChatListAdapter
    private var searchEditText: EditText? = null
    private var mainLayout: RelativeLayout? = null
    private var searchLayout: LinearLayout? = null
    private lateinit var token: String
    private lateinit var user: User
    private var filterText: String = ""
    private var showFilter: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_chat, container, false)
        setUpCustomActionBar()

        binding.lifecycleOwner = this
        val pair = getFromSharedPreferences(requireContext())
        token = pair.first
        user = pair.second

        val viewModelFactory = ChatViewModelFactory(token, user.userId)
        viewModel = ViewModelProvider(this, viewModelFactory).get(ChatViewModel::class.java)

        adapter = ChatListAdapter(ChatListClickListener { chatData ->
            startChatActivity(chatData)
        })

        binding.chatListRecyclerView.adapter = adapter

        viewModel.chatList.observe(viewLifecycleOwner, Observer { chatList ->
            chatList?.let {
                // adapter.submitList(chatList)
                filterSearch(chatList)
            }
        })

        binding.reloadButton.setOnClickListener {
            viewModel.getChatList(token, user.userId)
        }

        viewModel.status.observe(viewLifecycleOwner, Observer { status ->
            status?.let {
                when (status) {
                    ChatStatus.LOADING -> {
                        binding.reloadButton.visibility = View.GONE
                        binding.statusAnimation.apply {
                            setAnimation(R.raw.chats_loading_anim)
                            visibility = View.VISIBLE
                            if (!this.isAnimating) this.playAnimation()
                        }
                        binding.chatListRecyclerView.visibility = View.GONE
                        binding.errorTextView.visibility = View.GONE
                    }
                    ChatStatus.ERROR -> {
                        binding.reloadButton.visibility = View.VISIBLE
                        binding.statusAnimation.apply {
                            setAnimation(R.raw.internet_error_anim)
                            visibility = View.VISIBLE
                            if (!this.isAnimating) this.playAnimation()
                        }
                        binding.errorTextView.text =
                            getString(R.string.error_loading_chat_list)
                        binding.errorTextView.visibility = View.VISIBLE
                        binding.chatListRecyclerView.visibility = View.GONE
                    }
                    ChatStatus.FAILED -> {
                        binding.reloadButton.visibility = View.VISIBLE
                        binding.statusAnimation.apply {
                            setAnimation(R.raw.internet_error_anim)
                            visibility = View.VISIBLE
                            if (!this.isAnimating) this.playAnimation()
                        }
                        binding.errorTextView.text = getString(R.string.failed_loading_chat_list)
                        binding.errorTextView.visibility = View.VISIBLE
                    }
                    ChatStatus.EMPTY -> {
                        binding.reloadButton.visibility = View.GONE
                        binding.statusAnimation.apply {
                            setAnimation(R.raw.no_chats_anim)
                            visibility = View.VISIBLE
                            if (!this.isAnimating) this.playAnimation()
                        }
                        binding.errorTextView.text = getString(R.string.no_chats)
                        binding.errorTextView.visibility = View.VISIBLE
                        binding.chatListRecyclerView.visibility = View.GONE
                    }
                    ChatStatus.DONE -> {
                        binding.reloadButton.visibility = View.GONE
                        binding.statusAnimation.visibility = View.GONE
                        binding.errorTextView.visibility = View.GONE
                        binding.chatListRecyclerView.visibility = View.VISIBLE
                    }
                }
            }
        })

        viewModel.navigateToSelectedChat.observe(viewLifecycleOwner, Observer {
            it?.let {
                startChatActivity(it)
                viewModel.onDisplayChatComplete()
            }
        })

        return binding.root
    }

    private fun startChatActivity(chatData: ChatData) {
        val intent: Intent = Intent(context, SingleChatActivity::class.java)
        val chatPerson = SingleChatPerson(chatData.strangeeId,
            chatData.firstName,
            chatData.lastName,
            chatData.imageUrl,
            true,
            chatData.country,
            chatData.gender,
            chatData.interestedIn,
            chatData.birthday,
            chatData.aboutMe,
            chatData.saved)
        intent.putExtra("chat_person", chatPerson)

        backFromFilter()
        startActivity(intent)
    }

    private fun setUpCustomActionBar() {
        val customActionBar = (activity as? AppCompatActivity)?.supportActionBar?.customView
        searchLayout = customActionBar?.findViewById<LinearLayout>(R.id.search_layout)
        mainLayout = customActionBar?.findViewById<RelativeLayout>(R.id.main_layout)
        val fragmentText = customActionBar?.findViewById<TextView>(R.id.fragment_text)
        val searchButton = customActionBar?.findViewById<ImageView>(R.id.custom_right_button)
        val backButton = customActionBar?.findViewById<ImageView>(R.id.back_button)
        searchEditText = customActionBar?.findViewById<EditText>(R.id.search_edit_text)

        mainLayout?.visibility = View.VISIBLE
        searchLayout?.visibility = View.GONE

        fragmentText?.text = getString(R.string.chat_capital)
        searchButton?.apply {
            visibility = View.VISIBLE
            setImageResource(R.drawable.ic_search)
            setOnClickListener {
                searchEditText?.setText("")
                searchEditText?.requestFocus()
                mainLayout?.visibility = View.GONE
                searchLayout?.visibility = View.VISIBLE

                showKeyboard(searchEditText)
            }
        }

        backButton?.setOnClickListener {
            backFromFilter()
        }

        searchEditText?.doOnTextChanged { text, start, before, count ->
            showFilter = true
            filterText = text?.toString()?.toLowerCase(Locale.ROOT) ?: ""
            filterSearch(viewModel.chatList.value)
        }
    }

    private fun backFromFilter() {
        val imm: InputMethodManager =
            requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchEditText?.windowToken, 0)
        adapter.submitList(viewModel.chatList.value)
        mainLayout?.visibility = View.VISIBLE
        searchLayout?.visibility = View.GONE

        showFilter = false
    }

    private fun filterSearch(chatList: List<ChatData>?) {
        if(showFilter) {
            if (chatList != null) {
                val filterList = chatList.filter {
                    "${it.firstName} ${it.lastName[0]}.".toLowerCase(Locale.ROOT)
                        .contains(filterText)
                            || it.message.toLowerCase(Locale.ROOT).contains(filterText)
                }
                adapter.submitList(filterList)
            }
        } else {
            adapter.submitList(chatList)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.getChatList(token, user.userId)
    }
}