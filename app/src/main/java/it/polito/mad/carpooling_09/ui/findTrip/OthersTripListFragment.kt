package it.polito.mad.carpooling_09.ui.findTrip

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.LinearLayoutManager
import it.polito.mad.carpooling_09.R
import it.polito.mad.carpooling_09.adapter.TripAdapter
import it.polito.mad.carpooling_09.databinding.FragmentOthersTripListBinding
import it.polito.mad.carpooling_09.viewmodel.OthersTripViewModel


class OthersTripListFragment : Fragment(R.layout.fragment_others_trip_list) {

    companion object {
        private const val TAG = "OthersTripListFragment"
    }

    private val vm: OthersTripViewModel by activityViewModels()

    private lateinit var filterDialog: FilterDialogFragment

    private var _binding: FragmentOthersTripListBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOthersTripListBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        filterDialog = FilterDialogFragment()

        binding.recyclerView.layoutManager = LinearLayoutManager(view.context)

        vm.getTrips().observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                binding.recyclerView.adapter =
                    TripAdapter(data = it, createdByCurrentUser = false, filters = vm.getTripFilter().value)
                binding.emptyTripList.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
            } else {
                binding.recyclerView.visibility = View.GONE
                binding.emptyTripList.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.filter_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.nav_filter -> {
                // User clicked on the filter icon to filter the list of trips
                val action =
                    OthersTripListFragmentDirections.actionOthersTripListFragmentToFilterDialogFragment()
                findNavController().navigate(action)

                true
            }

            else -> NavigationUI.onNavDestinationSelected(
                item, requireView().findNavController()
            ) || super.onOptionsItemSelected(item)

        }
    }
}