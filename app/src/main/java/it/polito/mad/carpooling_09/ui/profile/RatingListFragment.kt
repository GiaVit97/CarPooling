package it.polito.mad.carpooling_09.ui.profile

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.GridLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import it.polito.mad.carpooling_09.R
import it.polito.mad.carpooling_09.adapter.RatingsAdapter
import it.polito.mad.carpooling_09.data.Ratings
import it.polito.mad.carpooling_09.data.Role
import it.polito.mad.carpooling_09.databinding.FragmentRatingListBinding
import it.polito.mad.carpooling_09.viewmodel.RatingsViewModel
import it.polito.mad.carpooling_09.viewmodel.RatingsViewModelFactory


class RatingListFragment : Fragment(R.layout.fragment_rating_list) {

    companion object {
        private const val TAG = "RatingsFragmentProfile"
    }

    // Safe Args
    private val args: RatingListFragmentArgs by navArgs()

    // View Model
    private val vm: RatingsViewModel by viewModels { RatingsViewModelFactory(args.userID) }

    // Binding
    private var _binding: FragmentRatingListBinding? = null
    private val binding get() = _binding!!

    private lateinit var options: Array<String>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRatingListBinding.inflate(inflater, container, false)

        return binding.root
    }


    override fun onResume() {
        super.onResume()
        options = resources.getStringArray(R.array.review_array)
        val arrayAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, options)
        binding.autoCompleteTextView.setAdapter(arrayAdapter)
        binding.autoCompleteTextView.threshold = 1
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            binding.rvRatings.layoutManager = GridLayoutManager(view.context, 2)
        } else {
            binding.rvRatings.layoutManager = LinearLayoutManager(view.context)
        }


        options = resources.getStringArray(R.array.review_array)
        val arrayAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, options)

        if (savedInstanceState == null) {
            binding.autoCompleteTextView.setText(arrayAdapter.getItem(0).toString(), false)
        }

        retrieveReview()

        binding.autoCompleteTextView.setOnDismissListener {
            retrieveReview()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun retrieveReview() {

        vm.getRatings().observe(viewLifecycleOwner, {
            val list = ArrayList<Ratings>()
            val options = resources.getStringArray(R.array.review_array)

            for (rate in it) {

                when (binding.autoCompleteTextView.text.toString()) {
                    options[0] -> list.add(rate)
                    options[1] -> {

                        if (rate.status == Role.Driver) list.add(rate)
                    }
                    options[2] -> {
                        if (rate.status == Role.Passenger) list.add(rate)
                    }
                    else -> list.add(rate)
                }
            }

            if (list.isNullOrEmpty()) {
                binding.rvRatings.visibility = View.GONE
                binding.tvNoRatings.visibility = View.VISIBLE
            } else {
                binding.tvNoRatings.visibility = View.GONE
                binding.rvRatings.visibility = View.VISIBLE
                binding.rvRatings.adapter = RatingsAdapter(list, openProfile = { uuid ->
                    val action = RatingListFragmentDirections.actionRatingsFragmentProfileToShowProfileFragment(uuid)
                    findNavController().navigate(action)
                })
            }

        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("saved", 1)
    }

}