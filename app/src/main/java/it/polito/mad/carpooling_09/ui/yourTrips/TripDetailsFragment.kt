package it.polito.mad.carpooling_09.ui.yourTrips

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.ktx.Firebase
import it.polito.mad.carpooling_09.MainActivity
import it.polito.mad.carpooling_09.R
import it.polito.mad.carpooling_09.adapter.StopViewAdapter
import it.polito.mad.carpooling_09.data.*
import it.polito.mad.carpooling_09.databinding.FragmentTripDetailsBinding
import it.polito.mad.carpooling_09.utils.*
import it.polito.mad.carpooling_09.viewmodel.*
import kotlinx.coroutines.*
import java.text.DecimalFormat
import java.util.*

class TripDetailsFragment : Fragment(R.layout.fragment_trip_details) {

    companion object{
        const val TAG = "TripDetailsFragment"
    }

    // Safe Args
    private val args: TripDetailsFragmentArgs by navArgs()

    // Binding
    private var _binding: FragmentTripDetailsBinding? = null
    private val binding get() = _binding!!

    private val vmProfile: OthersTripViewModel by activityViewModels()

    private val vm: TripDetailsViewModel by viewModels {
        TripDetailsViewModelFactory(
            args.tripID,
            args.userID
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentTripDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as MainActivity).progressBarVisibility(true)

        binding.stopsView.layoutManager = LinearLayoutManager(view.context)

        vm.getTrip().observe(viewLifecycleOwner) { trip ->

            if (trip != null) {


                if (trip.userID == Firebase.auth.currentUser?.uid &&
                    dateIsPassed(trip.stops[trip.stops.size - 1].time!!)
                ) {
                    binding.tripOverLayout.visibility = View.VISIBLE
                    binding.reviewPassengersButton.setOnClickListener {
                        val action =
                            TripDetailsFragmentDirections.actionTripDetailsFragmentToManagePassengersFragment(
                                trip.id!!
                            )
                        findNavController().navigate(action)
                    }
                } else {
                    binding.tripOverLayout.visibility = View.GONE
                }


                // If the trip is created by the user, he will see the edit menu for modifying it
                setMenuVisibility(
                    trip.userID == Firebase.auth.currentUser?.uid && !dateIsPassed(
                        trip.stops[trip.stops.size - 1].time!!
                    )
                )

                binding.departureDate.text =
                    convertUTCToString(trip.departureDate, "EEE MMMM dd, yyyy")

                binding.viewMap.setOnClickListener {
                    val action =
                        TripDetailsFragmentDirections.actionTripDetailsFragmentToMapRouteFragment(
                            trip.id!!
                        )
                    findNavController().navigate(action)
                }


                binding.estimatedDuration.text = trip.estimatedDuration
                binding.price.text = DecimalFormat("0.00").format(trip.price)

                // Show departure and arrival stop with a different style and color
                var departure: GeoPoint? = null
                var arrival: GeoPoint? = null
                if (args.bookingArrival != null && args.bookingDeparture != null) {

                    departure = GeoPoint(
                        args.bookingDeparture?.lat!!,
                        args.bookingDeparture?.long!!
                    )
                    arrival = GeoPoint(
                        args.bookingArrival?.lat!!,
                        args.bookingArrival?.long!!
                    )

                } else {

                    if (vmProfile.getTripFilter().value?.departureLocation == null || vmProfile.getTripFilter().value?.arrivalLocation == null) {
                        departure = trip.stops[0].location
                        arrival = trip.stops[trip.stops.size - 1].location
                    } else {
                        departure = trip.stops[stopsNext(
                            trip.stops,
                            vmProfile.getTripFilter().value?.departureLocation!!
                        )].location
                        arrival = trip.stops[stopsNext(
                            trip.stops,
                            vmProfile.getTripFilter().value?.arrivalLocation!!
                        )].location
                    }
                }

                binding.stopsView.adapter = StopViewAdapter(listOf(trip), departure, arrival)

                // The number of available seats change when people are accepted on the trip
                // If user doesn't filter the trip, I show the minimum number of seats in all trip
                // Else, I show the minimum number of trips in his research
                // If I own the trip, I see the minimum number in general
                val availableSeats = minAvailableSeats(trip, true, vmProfile.getTripFilter().value)

                val nS = getString(R.string.seats_details_trip)
                binding.numberSeats.text = String.format(nS, availableSeats)


                if (trip.tripPreferences?.smoke == PreferencesType.NoPreferences &&
                    trip.tripPreferences?.animals == PreferencesType.NoPreferences &&
                    trip.tripPreferences?.music == PreferencesType.NoPreferences &&
                    trip.description.isNullOrEmpty()
                ) {
                    binding.additionalInfoTitle.visibility = View.GONE
                }

                when (trip.tripPreferences?.smoke) {
                    PreferencesType.NoPreferences -> {
                        binding.smoke.visibility = View.GONE
                    }
                    PreferencesType.Yes -> {
                        binding.smoke.setCompoundDrawablesWithIntrinsicBounds(
                            ContextCompat.getDrawable(
                                requireContext(),
                                R.drawable.trip_smoke_yes
                            ), null, null, null
                        )
                        binding.smoke.setText(R.string.smoke_yes)
                    }
                    PreferencesType.No -> {
                        binding.smoke.setCompoundDrawablesWithIntrinsicBounds(
                            ContextCompat.getDrawable(
                                requireContext(),
                                R.drawable.trip_smoke_no
                            ), null, null, null
                        )
                        binding.smoke.setText(R.string.smoke_no)
                    }
                }

                when (trip.tripPreferences?.animals) {
                    PreferencesType.NoPreferences -> {
                        binding.animal.visibility = View.GONE
                    }
                    PreferencesType.Yes -> {
                        binding.animal.setCompoundDrawablesWithIntrinsicBounds(
                            ContextCompat.getDrawable(
                                requireContext(),
                                R.drawable.trip_animal_yes
                            ), null, null, null
                        )
                        binding.animal.setText(R.string.animal_yes)
                    }
                    PreferencesType.No -> {
                        binding.animal.setCompoundDrawablesWithIntrinsicBounds(
                            ContextCompat.getDrawable(
                                requireContext(),
                                R.drawable.trip_animal_no
                            ), null, null, null
                        )
                        binding.animal.setText(R.string.animal_no)
                    }
                }

                when (trip.tripPreferences?.music) {
                    PreferencesType.NoPreferences -> {
                        binding.music.visibility = View.GONE
                    }
                    PreferencesType.Yes -> {
                        binding.music.setCompoundDrawablesWithIntrinsicBounds(
                            ContextCompat.getDrawable(
                                requireContext(),
                                R.drawable.trip_music_yes
                            ), null, null, null
                        )
                        binding.music.setText(R.string.music_yes)
                    }
                    PreferencesType.No -> {
                        binding.music.setCompoundDrawablesWithIntrinsicBounds(
                            ContextCompat.getDrawable(
                                requireContext(),
                                R.drawable.trip_music_no
                            ), null, null, null
                        )
                        binding.music.setText(R.string.music_no)
                    }
                }

                if (trip.description.isNullOrEmpty()) {
                    binding.additionalInfo.visibility = View.GONE
                } else {
                    binding.additionalInfo.text = trip.description
                }

                binding.carType.text = trip.carInfo?.model
                binding.carColor.text = trip.carInfo?.color


                trip.carInfo?.urlPhoto?.let {
                    loadImage(binding.carImageView, trip.carInfo?.urlPhoto!!, R.drawable.car)
                }

                binding.fab.visibility = View.GONE
                binding.fabCancel.visibility = View.GONE
                if (availableSeats == 0 || trip.blocked == true || dateIsPassed(trip.stops[trip.stops.size - 1].time!!)) {
                    binding.fab.visibility = View.GONE
                    binding.fabCancel.visibility = View.GONE
                } else if (args.bookingArrival != null && args.bookingArrival != null && args.bookingStatus != null && (args.bookingStatus == Status.Accepted.status || args.bookingStatus == Status.Pending.status)) {
                    binding.fabCancel.visibility = View.VISIBLE
                } else {
                    //manage fab Button
                    fabButtonVisibility(trip.userID != Firebase.auth.currentUser?.uid, trip.stops)
                }

                binding.fab.setOnClickListener {

                    //Save in a list all the stops the user does
                    val filteredListStop = ArrayList<Stop>()
                    var initialPosition = 0
                    var finalPosition = 0
                    for (i in trip.stops.indices) {
                        if (departure == trip.stops[i].location)
                            initialPosition = i
                        if (arrival == trip.stops[i].location)
                            finalPosition = i
                    }
                    for (i in initialPosition..finalPosition)
                        filteredListStop.add(trip.stops[i])

                    vm.addBooking(
                        filteredListStop,
                        trip.price!!,
                        trip.id!!
                    ) { success, errorMessage ->
                        if (success) {
                            snackBar(view, "Booking completed")
                        } else {
                            snackBarError(view, errorMessage.toString())
                        }
                    }
                }

                binding.fabCancel.setOnClickListener {

                    deleteBookingDialog(requireContext(), requireActivity()) {

                        vm.deleteUserBookingWithTripID(trip.id, Firebase.auth.currentUser?.uid) { success, error ->
                            if (success) {
                                snackBar(view, "Booking has been removed successfully")
                                fabButtonVisibility(trip.userID != Firebase.auth.currentUser?.uid, trip.stops)
                                binding.fabCancel.visibility = View.GONE
                                binding.fab.visibility = View.VISIBLE
                            } else {
                                Log.e(TAG, error.toString())
                                snackBar(view, "Error during elimination of the booking, try again please")
                            }
                        }
                    }
                }

                MainScope().launch {
                    withContext(Dispatchers.IO) {
                        childFragmentManager.commit {
                            add(R.id.mapRouteFragment, MapRouteFragment.newInstance(trip.id!!))
                        }
                    }
                }

            }
        }

        //User Information (nickname and rating)
        vm.getPublicUser().observe(viewLifecycleOwner)
        { user ->
            if (user != null) {
                binding.nickname.text = user.nickname
                binding.ratingBar.rating = user.driverStar ?: 0f

                user.url?.let {
                    loadImage(binding.profileImage, user.url)
                }
                binding.linearProfile.setOnClickListener {
                    val action = TripDetailsFragmentDirections.actionTripDetailsFragmentToShowProfileFragment(user.id)
                    findNavController().navigate(action)
                }
            }
        }

        (requireActivity() as MainActivity).progressBarVisibility(false)


    }

    private fun fabButtonVisibility(isNotYourTrip: Boolean, stopsList: List<Stop>) {
        var notBooked = true
        binding.fab.visibility = View.VISIBLE
        binding.fabCancel.visibility = View.GONE
        if (args.tripID.isNotEmpty()) {

            if (!isNotYourTrip) {
                binding.fab.visibility = View.GONE
            } else {

                vm.getBookings().observe(viewLifecycleOwner) { bookingsList ->
                    if (!bookingsList.isNullOrEmpty()) {


                        if (vmProfile.getTripFilter().value?.departureLocation != null || vmProfile.getTripFilter().value?.arrivalLocation != null) {
                            val filteredListStop = ArrayList<Stop>()

                            val initialPosition = stopsNext(
                                stopsList,
                                vmProfile.getTripFilter().value?.departureLocation!!
                            )
                            val finalPosition = stopsNext(
                                stopsList,
                                vmProfile.getTripFilter().value?.arrivalLocation!!
                            )

                            for (i in initialPosition + 1..finalPosition)
                                filteredListStop.add(stopsList[i])

                            for (booking in bookingsList) {
                                if (booking.userId == Firebase.auth.currentUser?.uid && booking.status != Status.Rejected) {
                                    for (stop in filteredListStop) {
                                        if (stop.name == booking.stops[0].name || stop.name == booking.stops[booking.stops.size - 1].name) {
                                            binding.fab.visibility = View.GONE
                                            binding.fabCancel.visibility = View.VISIBLE
                                            notBooked = false
                                            break
                                        }
                                    }
                                }
                            }
                        } else {
                            for (booking in bookingsList) {
                                if (booking.userId == Firebase.auth.currentUser?.uid && booking.status != Status.Rejected) {
                                    binding.fab.visibility = View.GONE
                                    binding.fabCancel.visibility = View.VISIBLE
                                    notBooked = false
                                    break
                                }
                            }
                        }

                        if (notBooked) {
                            binding.fab.visibility = View.VISIBLE
                            binding.fabCancel.visibility = View.GONE
                        }

                    }
                }

            }


        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.nav_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.nav_edit -> {

                // Pass to TripEditFragment the same data that I receive from RecyclerView
                val action =
                    TripDetailsFragmentDirections.actionTripDetailsFragmentToTripEditFragment(
                        args.tripID
                    )
                findNavController().navigate(action)
                true
            }

            else -> NavigationUI.onNavDestinationSelected(
                item, requireView().findNavController()
            ) || super.onOptionsItemSelected(item)

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}