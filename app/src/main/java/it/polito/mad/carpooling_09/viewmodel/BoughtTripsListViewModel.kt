package it.polito.mad.carpooling_09.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import it.polito.mad.carpooling_09.data.Booking
import it.polito.mad.carpooling_09.data.Status
import it.polito.mad.carpooling_09.repository.BookingRepository

class BoughtTripsListViewModel : ViewModel() {

    private var bookingRepository = BookingRepository()

    private val acceptedUserBooking: LiveData<List<Booking>> = bookingRepository
        .getBookingsByUserIdAndStatus(
            Firebase.auth.currentUser?.uid ?: " ",
            Status.Accepted
        )

    private val userPendingBooking: LiveData<List<Booking>> = bookingRepository
        .getBookingsByUserIdAndStatus(
            Firebase.auth.currentUser?.uid ?: " ",
            Status.Pending
        )

    fun getUserAcceptedBooking(): LiveData<List<Booking>>{
        return acceptedUserBooking
    }

    fun getUserPendingBooking(): LiveData<List<Booking>>{
        return userPendingBooking
    }


}