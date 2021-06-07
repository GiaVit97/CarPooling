package it.polito.mad.carpooling_09.ui.interestedTrips

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import it.polito.mad.carpooling_09.R
import it.polito.mad.carpooling_09.adapter.BookingAdapter
import it.polito.mad.carpooling_09.databinding.FragmentTripsOfInterestListBinding
import it.polito.mad.carpooling_09.viewmodel.BoughtTripsListViewModel

class TripsOfInterestListFragment : Fragment(R.layout.fragment_trips_of_interest_list) {

    private val vm: BoughtTripsListViewModel by activityViewModels()

    private var _binding: FragmentTripsOfInterestListBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTripsOfInterestListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.layoutManager = LinearLayoutManager(view.context)

        vm.getUserPendingBooking().observe(viewLifecycleOwner) {

            if (!it.isNullOrEmpty()) {
                binding.recyclerView.adapter = BookingAdapter(it, false)
                binding.emptyTripList.visibility = View.GONE
            } else {
                binding.recyclerView.adapter = BookingAdapter(mutableListOf(), false)
                binding.emptyTripList.visibility = View.VISIBLE
            }

        }

    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }



}