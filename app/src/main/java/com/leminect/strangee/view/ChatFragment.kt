package com.leminect.strangee.view

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
import com.leminect.strangee.R
import com.leminect.strangee.adapter.ChatListAdapter
import com.leminect.strangee.adapter.ChatListClickListener
import com.leminect.strangee.adapter.StrangeeClickListener
import com.leminect.strangee.adapter.StrangeeGridAdapter
import com.leminect.strangee.databinding.FragmentChatBinding
import com.leminect.strangee.model.ChatData
import com.leminect.strangee.model.SingleChatPerson
import com.leminect.strangee.model.Strangee

class ChatFragment : Fragment() {

    private lateinit var binding: FragmentChatBinding
    private val placeholderText = "Interested in making new friends worldwide."
    private val placeholderImage = listOf(
        "https://images.pexels.com/photos/61100/pexels-photo-61100.jpeg?crop=faces&fit=crop&h=200&w=200&auto=compress&cs=tinysrgb",
        "https://images.unsplash.com/photo-1456327102063-fb5054efe647?ixlib=rb-0.3.5&q=80&fm=jpg&crop=faces&fit=crop&h=200&w=200&s=f05c14dd4db49f08a789e6449604c490",
        "https://images.unsplash.com/photo-1507081323647-4d250478b919?ixlib=rb-0.3.5&q=80&fm=jpg&crop=faces&fit=crop&h=200&w=200&s=b717a6d0469694bbe6400e6bfe45a1da",
        "https://images.unsplash.com/photo-1464863979621-258859e62245?ixlib=rb-0.3.5&q=80&fm=jpg&crop=faces&fit=crop&h=200&w=200&s=d1ff5086e5ca75cda4bcc8e470d8af11",
        "https://images.unsplash.com/photo-1511424187101-2aaa60069357?ixlib=rb-0.3.5&q=80&fm=jpg&crop=faces&fit=crop&h=200&w=200&s=d2e1a84f397a4f01795661a2bf6f0f01",
        "https://uifaces.co/our-content/donated/Te-0H20q.png"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_chat, container, false)
        setUpCustomActionBar()

        val adapter = ChatListAdapter(ChatListClickListener{ chatData ->
            startChatActivity(chatData)
        })

        binding.chatListRecyclerView.adapter = adapter

        val listData = listOf<ChatData>(
            ChatData("abcd", "Edmund", "Paul", placeholderImage[0],
                885566388000, placeholderText),
            ChatData("abcd", "Edmund", "Paul", placeholderImage[1],
                885566388000, placeholderText),
            ChatData("abcd", "Edmund", "Paul", placeholderImage[2],
                885566388000, placeholderText),
            ChatData("abcd", "Edmund", "Paul", placeholderImage[3],
                885566388000, placeholderText),
            ChatData("abcd", "Edmund", "Paul", placeholderImage[4],
                885566388000, placeholderText),
            ChatData("abcd", "Edmund", "Paul", placeholderImage[5],
                885566388000, placeholderText)
        )
        adapter.submitList(listData)

        return binding.root
    }

    private fun startChatActivity(chatData: ChatData) {
        val intent: Intent = Intent(context, SingleChatActivity::class.java)
        // TODO: put exact value of isOnline
        val chatPerson = SingleChatPerson(chatData.userId, chatData.firstName, chatData.lastName, chatData.imageUrl, true)
        intent.putExtra("chat_person", chatPerson)
        startActivity(intent)
    }

    private fun setUpCustomActionBar() {
        val customActionBar = (activity as? AppCompatActivity)?.supportActionBar?.customView
        val searchLayout = customActionBar?.findViewById<LinearLayout>(R.id.search_layout)
        val mainLayout = customActionBar?.findViewById<RelativeLayout>(R.id.main_layout)
        val fragmentText = customActionBar?.findViewById<TextView>(R.id.fragment_text)
        val searchButton = customActionBar?.findViewById<ImageView>(R.id.custom_right_button)
        val backButton = customActionBar?.findViewById<ImageView>(R.id.back_button)
        val searchEditText = customActionBar?.findViewById<EditText>(R.id.search_edit_text)

        mainLayout?.visibility = View.VISIBLE
        searchLayout?.visibility = View.GONE

        fragmentText?.text = getString(R.string.chat_capital)
        searchButton?.apply {
            visibility = View.VISIBLE
            setImageResource(R.drawable.ic_search)
            setOnClickListener{
                searchEditText?.setText("")
                searchEditText?.requestFocus()
                mainLayout?.visibility = View.GONE
                searchLayout?.visibility = View.VISIBLE
            }
        }

        backButton?.setOnClickListener{
            val imm: InputMethodManager =
                requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(searchEditText?.windowToken, 0)
            mainLayout?.visibility = View.VISIBLE
            searchLayout?.visibility = View.GONE
        }

        searchEditText?.doOnTextChanged { text, start, before, count ->
            // filterSearch(text.toString())
        }
    }
}