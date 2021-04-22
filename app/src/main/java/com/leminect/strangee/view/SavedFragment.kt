package com.leminect.strangee.view

import android.app.Activity
import android.content.Intent
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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.leminect.strangee.R
import com.leminect.strangee.adapter.StrangeeClickListener
import com.leminect.strangee.adapter.StrangeeGridAdapter
import com.leminect.strangee.databinding.FragmentSavedBinding
import com.leminect.strangee.model.Strangee
import com.leminect.strangee.utility.getFromSharedPreferences
import com.leminect.strangee.utility.showKeyboard
import com.leminect.strangee.viewmodel.FindViewModel
import com.leminect.strangee.viewmodel.SavedStatus
import com.leminect.strangee.viewmodel.SavedViewModel
import com.leminect.strangee.viewmodelfactory.FindViewModelFactory
import com.leminect.strangee.viewmodelfactory.SavedViewModelFactory
import java.util.*


class SavedFragment : Fragment() {

    lateinit var binding: FragmentSavedBinding
    private lateinit var viewModel: SavedViewModel
    private lateinit var adapter: StrangeeGridAdapter
    private var filterText: String = ""
    private var showFilter: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_saved, container, false)
        setUpCustomActionBar()

        binding.lifecycleOwner = this
        val pair = getFromSharedPreferences(requireContext())
        val token = pair.first
        val user = pair.second

        val viewModelFactory = SavedViewModelFactory(token, user.userId)
        viewModel = ViewModelProvider(this, viewModelFactory).get(SavedViewModel::class.java)

        adapter = StrangeeGridAdapter(StrangeeClickListener({ strangee ->
            viewModel.displaySavedProfile(strangee)
        }, {}, { strangee ->
            viewModel.removeSavedProfile(token, strangee.userId)
        }), true)

        binding.savedRecyclerView.adapter = adapter

        viewModel.savedList.observe(viewLifecycleOwner, Observer { savedList ->
            savedList?.let {
//                adapter.submitList(savedList)
                filterSearch(savedList)
            }
        })

        binding.reloadButton.setOnClickListener {
            viewModel.getSavedList(token, user.userId)
        }

        viewModel.status.observe(viewLifecycleOwner, Observer { status ->
            status?.let {
                when (status) {
                    SavedStatus.LOADING -> {
                        binding.reloadButton.visibility = View.GONE
                        binding.statusAnimation.apply {
                            setAnimation(R.raw.loading_animation)
                            visibility = View.VISIBLE
                            if (!this.isAnimating) this.playAnimation()
                        }
                        binding.errorTextView.visibility = View.GONE
                    }
                    SavedStatus.ERROR -> {
                        binding.reloadButton.visibility = View.VISIBLE
                        binding.statusAnimation.apply {
                            setAnimation(R.raw.internet_error_anim)
                            visibility = View.VISIBLE
                            if (!this.isAnimating) this.playAnimation()
                        }
                        binding.errorTextView.text =
                            getString(R.string.error_loading_saved_profiles)
                        binding.errorTextView.visibility = View.VISIBLE
                    }
                    SavedStatus.FAILED -> {
                        binding.reloadButton.visibility = View.VISIBLE
                        binding.statusAnimation.apply {
                            setAnimation(R.raw.internet_error_anim)
                            visibility = View.VISIBLE
                            if (!this.isAnimating) this.playAnimation()
                        }
                        binding.errorTextView.text = getString(R.string.failed_loading_saved)
                        binding.errorTextView.visibility = View.VISIBLE
                    }
                    SavedStatus.EMPTY -> {
                        binding.reloadButton.visibility = View.GONE
                        binding.statusAnimation.apply {
                            setAnimation(R.raw.no_results_found)
                            visibility = View.VISIBLE
                            if (!this.isAnimating) this.playAnimation()
                        }
                        binding.errorTextView.text = getString(R.string.no_profile_saved)
                        binding.errorTextView.visibility = View.VISIBLE
                    }
                    SavedStatus.DONE -> {
                        binding.reloadButton.visibility = View.GONE
                        binding.statusAnimation.visibility = View.GONE
                        binding.errorTextView.visibility = View.GONE
                    }
                }
            }
        })

        viewModel.navigateToSelectedProfile.observe(viewLifecycleOwner, Observer {
            it?.let {
                goToSavedProfile(it)
                viewModel.onDisplaySavedProfileComplete()
            }
        })

        return binding.root
    }

    private fun goToSavedProfile(strangee: Strangee) {
        val intent: Intent = Intent(context, StrangeeProfileActivity::class.java)
        intent.putExtra("strangee_data", strangee)
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

        fragmentText?.text = getString(R.string.saved_capital)
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
            val imm: InputMethodManager =
                requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(searchEditText?.windowToken, 0)
            adapter.submitList(viewModel.savedList.value)
            mainLayout?.visibility = View.VISIBLE
            searchLayout?.visibility = View.GONE

            showFilter = false
        }

        searchEditText?.doOnTextChanged { text, start, before, count ->
            showFilter = true
            filterText = text?.toString()?.toLowerCase(Locale.ROOT) ?: ""
            filterSearch(viewModel.savedList.value)
        }
    }

    private fun filterSearch(savedList: List<Strangee>?) {
        if(showFilter) {
            if (savedList != null) {
                val filterList = savedList.filter {
                    "${it.firstName} ${it.lastName[0]}.".toLowerCase(Locale.ROOT)
                        .contains(filterText)
                            || it.country.toLowerCase(Locale.ROOT).contains(filterText)
                }
                adapter.submitList(filterList)
            }
        } else {
            adapter.submitList(savedList)
        }
    }
}
