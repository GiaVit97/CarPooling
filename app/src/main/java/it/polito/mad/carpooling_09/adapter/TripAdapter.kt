package it.polito.mad.carpooling_09.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import it.polito.mad.carpooling_09.R
import it.polito.mad.carpooling_09.data.PreferencesType
import it.polito.mad.carpooling_09.data.Trip
import it.polito.mad.carpooling_09.data.TripFilter
import it.polito.mad.carpooling_09.databinding.TripLayoutBinding
import it.polito.mad.carpooling_09.ui.findTrip.OthersTripListFragmentDirections
import it.polito.mad.carpooling_09.ui.yourTrips.TripListFragmentDirections
import it.polito.mad.carpooling_09.utils.convertUTCToString
import it.polito.mad.carpooling_09.utils.dateIsPassed
import it.polito.mad.carpooling_09.utils.findDepartureAndArrivalStop
import java.text.DecimalFormat
import java.util.*


class TripAdapter(
    val data: List<Trip>,
    val createdByCurrentUser: Boolean,
    val filters: TripFilter? = null
) :
    RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    inner class TripViewHolder(v: View) : RecyclerView.ViewHolder(v) {

        val binding = TripLayoutBinding.bind(v)

        // bind take an actual piece of data and use it to fill the view
        fun bind(trip: Trip) {

            binding.tripDate.text = convertUTCToString(trip.stops[0].time, "EEE MMMM dd, yyyy")

            if(trip.blocked != null && trip.blocked == true) {
                binding.blockedIcon.visibility = View.VISIBLE
            } else {
                binding.blockedIcon.visibility = View.GONE
            }

            // If we have some filters it means that the user search for a given departure and arrival location
            // The stopsView will show to the user the stop near the searched location as departure and arrival for the trip
            if (filters != null) {
                val result = findDepartureAndArrivalStop(
                    trip.stops,
                    filters.departureLocation,
                    filters.arrivalLocation
                )
                binding.stopsView.setListStop(result)
            } else {
                binding.stopsView.setListStop(trip.stops)
            }

            binding.priceTrip.text = "€ ${DecimalFormat("0.00").format(trip.price)}"

            binding.singleTrip.setOnClickListener {
                val action =
                    if (createdByCurrentUser) TripListFragmentDirections.actionNavTripsToTripDetailsFragment(
                        trip.id!!,
                        trip.userID!!
                    )
                    else OthersTripListFragmentDirections.actionOthersTripListFragmentToTripDetailsFragment(
                        trip.id!!,
                        trip.userID!!
                    )
                Navigation.findNavController(it).navigate(action)
            }

            if (createdByCurrentUser) {
                // If the trip is created by the user we show the edit trip and manage passenger button

                // If the trip is not over, the edit trip button should appear
                if(!dateIsPassed(trip.stops[trip.stops.size-1].time!!)) {
                    binding.editTrip.visibility = View.VISIBLE
                    binding.editTrip.setOnClickListener {
                        val action =
                            TripListFragmentDirections.actionNavTripsToTripEditFragment(trip.id)
                        Navigation.findNavController(it).navigate(action)
                    }
                } else {
                    binding.passengerBtn.text = "Rate Passenger"
                    binding.tripOver.visibility = View.VISIBLE
                }

                binding.passengerBtn.visibility = View.VISIBLE
                binding.passengerBtn.setOnClickListener {
                    val action = TripListFragmentDirections.actionTripListFragmentToManagePassengersFragment(trip.id!!)
                    Navigation.findNavController(it).navigate(action)
                }
            }


            when (trip.tripPreferences?.smoke) {
                PreferencesType.NoPreferences -> binding.smokeIcon.visibility = View.GONE
                PreferencesType.Yes -> binding.smokeIcon.setImageResource(R.drawable.trip_smoke_yes)
                PreferencesType.No -> binding.smokeIcon.setImageResource(R.drawable.trip_smoke_no)
            }

            when (trip.tripPreferences?.animals) {
                PreferencesType.NoPreferences -> binding.animalIcon.visibility = View.GONE
                PreferencesType.Yes -> binding.animalIcon.setImageResource(R.drawable.trip_animal_yes)
                PreferencesType.No -> binding.animalIcon.setImageResource(R.drawable.trip_animal_no)
            }

            when (trip.tripPreferences?.music) {
                PreferencesType.NoPreferences -> binding.musicIcon.visibility = View.GONE
                PreferencesType.Yes -> binding.musicIcon.setImageResource(R.drawable.trip_music_yes)
                PreferencesType.No -> binding.musicIcon.setImageResource(R.drawable.trip_music_no)
            }


        }

        fun unbind() {
            binding.singleTrip.setOnClickListener { null }
            binding.editTrip.setOnClickListener { null }
        }
    }

    // when the item become invisible (ghost item position), it will detached all this listener
    override fun onViewRecycled(holder: TripViewHolder) {
        super.onViewRecycled(holder)
        holder.unbind()
    }

    /**
     * Each item in the data set is presented in a recyclable visual hierarchy which is managed by a ViewHolder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {

        val layoutInflater = LayoutInflater.from(parent.context)
        val layout = layoutInflater.inflate(R.layout.trip_layout, parent, false)

        return TripViewHolder(layout)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.trip_layout
    }
}
