package it.polito.mad.carpooling_09.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import it.polito.mad.carpooling_09.data.Booking
import it.polito.mad.carpooling_09.data.Trip
import it.polito.mad.carpooling_09.repository.BookingRepository
import it.polito.mad.carpooling_09.repository.TripRepository
import it.polito.mad.carpooling_09.repository.UserRepository

class ManagePassengersViewModel(private val tripID: String = " ") : ViewModel() {
    private var tripRepository = TripRepository()
    private var bookingRepository = BookingRepository()

    private val bookingList: MutableLiveData<List<Booking>> =
        bookingRepository.getBookingsByTripID(tripID) as MutableLiveData<List<Booking>>

    private val trip: MutableLiveData<Trip> =
        tripRepository.getTrip(tripID) as MutableLiveData<Trip>

    private var isSaving: Boolean = false

    fun getBookings(): LiveData<List<Booking>> {
        return bookingList
    }

    fun getTrip(): LiveData<Trip> {
        return trip
    }


    fun getTripOnce(callback: (success: Boolean, trip: Trip?, errorMessage: String?) -> Unit) {
        tripRepository.getTripOnce(tripID, callback)
    }

    fun updateBooking(
        booking: Booking,
        tripID: String,
        callback: (success: Boolean, errorMessage: String?) -> Unit
    ) {

        bookingRepository.updateBooking(booking, tripID, callback)

    }

    fun deleteBooking(
        booking: Booking,
        tripID: String,
        wasAccepted: Boolean,
        callback: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        bookingRepository.deleteBooking(booking, tripID, wasAccepted, callback)
    }

    fun setIsSaving(status: Boolean) {
        isSaving = status
    }
}