package it.polito.mad.carpooling_09.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.ktx.Firebase
import it.polito.mad.carpooling_09.data.*
import it.polito.mad.carpooling_09.repository.BookingRepository
import it.polito.mad.carpooling_09.repository.TripRepository
import it.polito.mad.carpooling_09.repository.UserRepository

class TripDetailsViewModel(tripID: String, userID: String) : ViewModel() {

    private var tripRepository = TripRepository()
    private var bookingRepository = BookingRepository()
    private var userRepository = UserRepository()

    private val trip: MutableLiveData<Trip> =
        tripRepository.getTrip(tripID) as MutableLiveData<Trip>

    private val bookingList: MutableLiveData<List<Booking>> =
        bookingRepository.getBookingsByTripID(tripID) as MutableLiveData<List<Booking>>

    private val userPublic : MutableLiveData<UserPublic> =
        userRepository.getPublicUserProfileById(userID) as MutableLiveData<UserPublic>

    fun getTrip(): LiveData<Trip> {
        return trip
    }

    //to retrieve the list of bookings and check if a user is already booked
    fun getBookings(): LiveData<List<Booking>> {
        return bookingList
    }

    fun getPublicUser(): LiveData<UserPublic> {
        return userPublic
    }

    fun addBooking(
        stopsList : List<Stop>,
        price: Double,
        tripID: String,
        callback: (success: Boolean, errorMessage: String?) -> Unit
    ) {

        /**
         * Booking(id, userPhoto, nickname, departureLocation ,arrivalLocation, price, status)
         *
         *      id -> getAutoID
         *      userID -> current user
         *      url -> photo of the user -> take it from userRepository with getUser
         *      nickname -> nickname of the user -> take it from userRepository with getUser
         *      stopsList -> the list of all the stop the user does
         *      price -> view pass it, it is the price of the trip
         *
         *      status -> when created, the status is always PENDING
         *
         */

        val newId = bookingRepository.getAutoID(tripID)
        val userId = Firebase.auth.currentUser?.uid


        UserRepository().getPublicUserOnce(userId!!) { user, success, errorMessage ->
            if (user != null && success) {
                val url = user.url
                val nickname = user.nickname


                val b = Booking(
                    newId, tripID, userId, url, nickname,
                    stopsList, price, Status.Pending, 0.0f, ""
                )

                bookingRepository.addBooking(tripID, newId, b, callback)
            } else {
                callback(false, errorMessage)
            }


        }


    }
}