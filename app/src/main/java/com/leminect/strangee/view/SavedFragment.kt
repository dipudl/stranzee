package com.leminect.strangee.view

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.leminect.strangee.R
import com.leminect.strangee.adapter.StrangeeClickListener
import com.leminect.strangee.adapter.StrangeeGridAdapter
import com.leminect.strangee.databinding.FragmentSavedBinding
import com.leminect.strangee.model.Strangee


class SavedFragment : Fragment() {

    lateinit var binding: FragmentSavedBinding
    private val placeholderText = "I am Dylan, one of the highly passionate blogger and tech enthusiast. " +
            "Interested in making new friends worldwide."
    private val placeholderImage = listOf(
        "https://images.unsplash.com/photo-1456327102063-fb5054efe647?ixlib=rb-0.3.5&q=80&fm=jpg&crop=faces&fit=crop&h=200&w=200&s=f05c14dd4db49f08a789e6449604c490",
        "https://images.unsplash.com/photo-1464863979621-258859e62245?ixlib=rb-0.3.5&q=80&fm=jpg&crop=faces&fit=crop&h=200&w=200&s=d1ff5086e5ca75cda4bcc8e470d8af11",
        "https://images.pexels.com/photos/61100/pexels-photo-61100.jpeg?crop=faces&fit=crop&h=200&w=200&auto=compress&cs=tinysrgb"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_saved, container, false)
        setUpCustomActionBar()

        val adapter = StrangeeGridAdapter(StrangeeClickListener({ strangee ->
            Toast.makeText(context, strangee.toString(), Toast.LENGTH_SHORT).show()
        }, {}, { strangee ->
            Toast.makeText(context, strangee.saved.toString(), Toast.LENGTH_SHORT).show()
        }), true)

        binding.savedRecyclerView.adapter = adapter

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

        return binding.root
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

        fragmentText?.text = getString(R.string.saved_capital)
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
