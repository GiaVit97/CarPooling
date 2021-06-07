package it.polito.mad.carpooling_09.ui.interestedTrips

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import it.polito.mad.carpooling_09.R
import it.polito.mad.carpooling_09.adapter.BookingAdapter
import it.polito.mad.carpooling_09.adapter.PassengerAdapter
import it.polito.mad.carpooling_09.databinding.FragmentBoughtTripsListBinding
import it.polito.mad.carpooling_09.viewmodel.BoughtTripsListViewModel

class BoughtTripsListFragment : Fragment(R.layout.fragment_bought_trips_list) {

    private val vm: BoughtTripsListViewModel by activityViewModels()

    private var _binding: FragmentBoughtTripsListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBoughtTripsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.layoutManager = LinearLayoutManager(view.context)

        vm.getUserAcceptedBooking().observe(viewLifecycleOwner) {

            if (!it.isNullOrEmpty()) {
                binding.recyclerView.adapter = BookingAdapter(it, true)
                binding.emptyTripList.visibility = View.GONE
            } else {
                binding.recyclerView.adapter = BookingAdapter(mutableListOf(), true)
                binding.emptyTripList.visibility = View.VISIBLE
            }

        }

    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}


