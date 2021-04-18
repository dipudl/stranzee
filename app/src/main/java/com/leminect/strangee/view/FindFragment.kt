package com.leminect.strangee.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.leminect.strangee.R
import com.leminect.strangee.adapter.StrangeeClickListener
import com.leminect.strangee.adapter.StrangeeGridAdapter
import com.leminect.strangee.databinding.FragmentFindBinding
import com.leminect.strangee.model.Strangee
import com.leminect.strangee.model.User
import com.leminect.strangee.utility.getFromSharedPreferences
import com.leminect.strangee.viewmodel.FindStatus
import com.leminect.strangee.viewmodel.FindViewModel
import com.leminect.strangee.viewmodelfactory.FindViewModelFactory

class FindFragment : Fragment() {

    lateinit var binding: FragmentFindBinding
    private lateinit var viewModel: FindViewModel
    private lateinit var token: String
    private lateinit var user: User

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_find, container, false)
        setUpCustomActionBar()

        // Allows Data Binding to Observe LiveData with the lifecycle of this Fragment
        binding.lifecycleOwner = this

        val pair = getFromSharedPreferences(requireContext())
        token = pair.first
        user = pair.second

        val viewModelFactory = FindViewModelFactory(token, user)
        viewModel = ViewModelProvider(this, viewModelFactory).get(FindViewModel::class.java)

//        viewModel.getStrangeeList(token, user, filterEnabled)
        viewModel.status.observe(viewLifecycleOwner, Observer { status ->
            status?.let {
                when (status) {
                    FindStatus.LOADING -> {
                        binding.reloadButton.visibility = View.GONE
                        binding.statusAnimation.apply {
                            setAnimation(R.raw.magnifying_glass_anim)
                            visibility = View.VISIBLE
                            if (!this.isAnimating) this.playAnimation()
                        }
                        binding.errorTextView.visibility = View.GONE
                    }
                    FindStatus.ERROR -> {
                        binding.reloadButton.text = getString(R.string.try_again)
                        binding.reloadButton.visibility = View.VISIBLE
                        binding.statusAnimation.apply {
                            setAnimation(R.raw.internet_error_anim)
                            visibility = View.VISIBLE
                            if (!this.isAnimating) this.playAnimation()
                        }
                        binding.errorTextView.text = getString(R.string.load_strangee_error_text)
                        binding.errorTextView.visibility = View.VISIBLE
                    }
                    FindStatus.DONE -> {
                        binding.reloadButton.visibility = View.GONE
                        binding.statusAnimation.visibility = View.GONE
                        binding.errorTextView.visibility = View.GONE
                    }
                    FindStatus.FILTER_NOT_FOUND -> {
                        binding.reloadButton.text = getString(R.string.reload_without_filter)
                        binding.reloadButton.visibility = View.VISIBLE
                        binding.statusAnimation.apply {
                            setAnimation(R.raw.not_found_anim)
                            visibility = View.VISIBLE
                            if (!this.isAnimating) this.playAnimation()
                        }
                        binding.errorTextView.text = getString(R.string.no_profile_found)
                        binding.errorTextView.visibility = View.VISIBLE
                    }
                }
            }
        })

        val adapter = StrangeeGridAdapter(StrangeeClickListener({ strangee ->
            viewModel.displayStrangeeProfile(strangee)
        }, { strangee ->
            val s = strangee.copy()
            val position = viewModel.findAndUpdateListIndex(strangee.userId, !strangee.saved)
            if (position >= 0) {
                (binding.findRecyclerView.adapter)?.notifyItemChanged(position)
            }

            // s is copy of strangee(not same instance) to prevent alteration of value by findAndUpdateListIndex to the strangee object
            // In simple words: Above line of codes change strangee object's saved value so we make a copy of it with different instance (memory location)
            viewModel.saveProfile(token, s)
        }, {}))

        binding.findRecyclerView.adapter = adapter
        binding.findViewModel = viewModel

        viewModel.saveBackData.observe(viewLifecycleOwner, Observer { saveResult ->
            saveResult?.let {
                val position = viewModel.findAndUpdateListIndex(saveResult.userId,
                    saveResult.saveStatus)
                if (position >= 0) {
                    (binding.findRecyclerView.adapter)?.notifyItemChanged(position)
                }
                if (saveResult.error) {
                    Toast.makeText(context, "Error saving selected profile", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        })

        viewModel.navigateToSelectedStrangee.observe(viewLifecycleOwner, Observer {
            it?.let {
                goToStrangeeProfile(it)
                viewModel.onDisplayStrangeeProfileComplete()
            }
        })

        binding.reloadButton.setOnClickListener {
            user = pair.second
            viewModel.setFilterEnabled(false)
            viewModel.getStrangeeList(token, user)
        }

        binding.findRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (viewModel.scrollPaginationEnabled.value == true) {
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager?
                    if (layoutManager != null) {
                        val totalItemCount = layoutManager.itemCount
                        val lastVisible = layoutManager.findLastVisibleItemPosition()
                        val endHasBeenReached = lastVisible + 5 >= totalItemCount

                        if ((totalItemCount > 0) && (totalItemCount % getString(R.string.strangee_pagination_count).toInt() == 0) && endHasBeenReached) {
                            //you have reached to the bottom of recycler view
                            viewModel.getStrangeeList(token, user, false)
                        }
                    }
                }
            }
        })

        return binding.root
    }

    private fun goToStrangeeProfile(strangee: Strangee) {
        val intent: Intent = Intent(context, StrangeeProfileActivity::class.java)
        intent.putExtra("strangee_data", strangee)
        startActivity(intent)
    }

    private fun setUpCustomActionBar() {
        val customActionBar = (activity as? AppCompatActivity)?.supportActionBar?.customView
        val searchLayout = customActionBar?.findViewById<LinearLayout>(R.id.search_layout)
        val mainLayout = customActionBar?.findViewById<RelativeLayout>(R.id.main_layout)
        val fragmentText = customActionBar?.findViewById<TextView>(R.id.fragment_text)
        val filterButton = customActionBar?.findViewById<ImageView>(R.id.custom_right_button)

        mainLayout?.visibility = View.VISIBLE
        searchLayout?.visibility = View.GONE
        fragmentText?.text = getString(R.string.strangee)

        filterButton?.apply {
            visibility = View.VISIBLE
            setImageResource(R.drawable.ic_filter)
            setOnClickListener {
                FilterResultsDialog(context, object : FilterApplyListener {
                    override fun onApply(country: String, gender: String, interest: String) {

                        val interestFilter = ArrayList<String>()
                        if (interest.isNotBlank()) {
                            for (item in interest.split(",")) {
                                val i = item.trim()
                                if (interestFilter.indexOf(i) < 0 && i.length >= 3) {
                                    interestFilter.add(i)
                                }
                            }
                        }

                        user = user.copy(country = country, gender = gender, interestedIn = interestFilter.toList())
                        viewModel.setFilterEnabled(true)
                        viewModel.getStrangeeList(token, user)
                    }
                })
            }
        }
    }
}