package it.polito.mad.carpooling_09.ui.yourTrips

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import it.polito.mad.carpooling_09.R
import it.polito.mad.carpooling_09.adapter.TripAdapter
import it.polito.mad.carpooling_09.data.Trip
import it.polito.mad.carpooling_09.viewmodel.TripListViewModel
import it.polito.mad.carpooling_09.views.StopsView
import java.text.Format
import java.text.SimpleDateFormat

class TripListFragment : Fragment(R.layout.fragment_trip_list) {

    private val vm: TripListViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val emptyTripList = view.findViewById<LinearLayout>(R.id.empty_tripList)

        recyclerView.layoutManager = LinearLayoutManager(view.context)
        vm.getTrips().observe(viewLifecycleOwner) {

            if (it.isNotEmpty()) {
                recyclerView.adapter = TripAdapter(data = it, createdByCurrentUser = true)
                emptyTripList.visibility = View.GONE
            } else {
                recyclerView.adapter = TripAdapter(data = mutableListOf(), createdByCurrentUser = true)
                emptyTripList.visibility = View.VISIBLE
                view.findViewById<Button>(R.id.add_trip).setOnClickListener {
                    findNavController().navigate(R.id.action_nav_trips_to_tripEditFragment)
                }
            }
        }


        val fab: FloatingActionButton = view.findViewById(R.id.fab)
        fab.setOnClickListener { view ->
            findNavController().navigate(R.id.action_nav_trips_to_tripEditFragment)
        }

    }
}
