package it.polito.mad.carpooling_09.ui.yourTrips

import android.content.Context
import android.os.Bundle
import android.provider.SyncStateContract.Helpers.update
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.GeoPoint
import com.google.gson.Gson
import it.polito.mad.carpooling_09.R
import it.polito.mad.carpooling_09.adapter.BookingAdapter
import it.polito.mad.carpooling_09.adapter.StopEditAdapter
import it.polito.mad.carpooling_09.viewmodel.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.math.BigDecimal
import java.text.Format
import java.text.SimpleDateFormat
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import it.polito.mad.carpooling_09.adapter.PassengerAdapter
import it.polito.mad.carpooling_09.data.*
import it.polito.mad.carpooling_09.databinding.FragmentManagePassengersBinding
import it.polito.mad.carpooling_09.databinding.FragmentTripDetailsBinding
import it.polito.mad.carpooling_09.utils.isThereAvailableSeats
import it.polito.mad.carpooling_09.utils.listSelectedTrip
import it.polito.mad.carpooling_09.utils.snackBar
import it.polito.mad.carpooling_09.utils.snackBarError
import java.util.*

class ManagePassengersFragment : Fragment(R.layout.fragment_manage_passengers) {

    companion object {
        private val TAG = "MANAGE_PASSENGERS_FRAGMENT"
    }


    //Safe Args
    private val args: ManagePassengersFragmentArgs by navArgs()

    private var _binding: FragmentManagePassengersBinding? = null
    private val binding get() = _binding!!

    private lateinit var vmFactory: ManagePassengersViewModelFactory
    private lateinit var vm: ManagePassengersViewModel

    private val othervm: OthersTripViewModel by activityViewModels()

    private var acceptedBookingsList: MutableList<Booking> = mutableListOf()
    private var pendingInvitesList: MutableList<Booking> = mutableListOf()


    private val acceptedBookingsAdapter = PassengerAdapter(acceptedBookingsList,
        onDelete = {
            deleteBooking(it, true)
        }, onAdd = {

        }, openProfile = { booking ->
            val action =
                ManagePassengersFragmentDirections.actionManagePassengersFragmentToShowProfileFragment5(
                    booking.userId!!
                )
            findNavController().navigate(action)
        })

    private val pendingInvitesAdapter = PassengerAdapter(pendingInvitesList,
        onDelete = {
            deleteBooking(it, false)
        }, onAdd = {

            vm.getTripOnce() { success, trip, errorMessage ->
                if(success && trip!=null){
                    val stops = trip.stops
                    val selectedStops = listSelectedTrip(stops, it.stops)
                    if (isThereAvailableSeats(selectedStops, othervm.getTripFilter().value)) {
                        addBooking(it)
                    } else {
                        snackBarError(view, "You don't have available seats to accept another passenger")
                    }
                }
                else {
                    snackBarError(view, errorMessage.toString())
                }
            }
        }, openProfile = { booking ->
            val action =
                ManagePassengersFragmentDirections.actionManagePassengersFragmentToShowProfileFragment5(
                    booking.userId!!
                )
            findNavController().navigate(action)
        })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentManagePassengersBinding.inflate(inflater, container, false)
        val view = binding.root

        vmFactory = ManagePassengersViewModelFactory(args.tripID)
        vm = ViewModelProvider(this, vmFactory).get(ManagePassengersViewModel::class.java)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerViewAccepted.layoutManager = LinearLayoutManager(view.context)
        binding.recyclerViewRequests.layoutManager = LinearLayoutManager(view.context)


        //Takes trips information from ViewModel
        vm.getBookings().observe(viewLifecycleOwner) { bookingsList ->

            if (bookingsList != null) {
                pendingInvitesList.clear()
                acceptedBookingsList.clear()
                for (booking in bookingsList) {
                    booking.tripId = args.tripID
                    if (booking.status == Status.Accepted) { //populate accepted bookings
                        acceptedBookingsList.add(booking)
                        acceptedBookingsAdapter.notifyItemInserted(acceptedBookingsList.size - 1)
                    } else if (booking.status == Status.Pending) { //populate pending invites
                        pendingInvitesList.add(booking)
                        pendingInvitesAdapter.notifyItemInserted(pendingInvitesList.size - 1)
                    }
                }
                binding.recyclerViewAccepted.adapter = acceptedBookingsAdapter
                binding.recyclerViewRequests.adapter = pendingInvitesAdapter
            }
            emptyListMessage()
        }

    }

    private fun addBooking(booking: Booking) {
        vm.updateBooking(booking, args.tripID) { success, errorMessage ->
            if (success) {
                snackBar(view, "Booking was accepted successfully")

                pendingInvitesAdapter.notifyDataSetChanged()
                acceptedBookingsAdapter.notifyDataSetChanged()
            } else {
                Log.e(TAG, errorMessage.toString())
                snackBarError(view, "Error during the acceptation of the booking, try again please")
            }

        }


    }

    private fun deleteBooking(booking: Booking, wasAccepted: Boolean) {
        vm.deleteBooking(booking, args.tripID, wasAccepted) { success, errorMessage ->
            if (success) {
                view?.let {
                    Snackbar.make(
                        it,
                        "Booking has been deleted successfully",
                        Snackbar.LENGTH_LONG
                    )
                        .show()

                    if (wasAccepted) {
                        acceptedBookingsAdapter.notifyDataSetChanged()
                    } else {
                        pendingInvitesAdapter.notifyDataSetChanged()
                    }
                }
            } else {
                Log.e(TAG, errorMessage.toString())
                view?.let {
                    Snackbar.make(
                        it,
                        "Error during deleting the booking, try again please",
                        Snackbar.LENGTH_LONG
                    )
                        .show()
                }
            }

        }


    }

    private fun emptyListMessage() {
        if (acceptedBookingsList.isNullOrEmpty()) {
            binding.bookedEmptyList.root.visibility = View.VISIBLE
            //binding.emptyAcceptedMessageBody.visibility = View.VISIBLE
            //binding.imageView.visibility = View.VISIBLE
        } else {
            binding.bookedEmptyList.root.visibility = View.GONE
        }
        //no requests from users
        if (pendingInvitesList.isNullOrEmpty()) {
            binding.emptyPendingMessageTitle.visibility = View.VISIBLE
            binding.emptyPendingMessageBody.visibility = View.VISIBLE
            binding.imageView2.visibility = View.VISIBLE
        } else {
            binding.emptyPendingMessageTitle.visibility = View.GONE
            binding.emptyPendingMessageBody.visibility = View.GONE
            binding.imageView2.visibility = View.GONE
        }
    }
}