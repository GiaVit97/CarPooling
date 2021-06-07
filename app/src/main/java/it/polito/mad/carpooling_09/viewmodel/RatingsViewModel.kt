package it.polito.mad.carpooling_09.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import it.polito.mad.carpooling_09.data.Booking
import it.polito.mad.carpooling_09.data.Ratings
import it.polito.mad.carpooling_09.data.Role
import it.polito.mad.carpooling_09.data.Trip
import it.polito.mad.carpooling_09.repository.RatingRepository
import it.polito.mad.carpooling_09.repository.TripRepository
import it.polito.mad.carpooling_09.repository.UserRepository
import it.polito.mad.carpooling_09.utils.snackBarError
import java.util.*

class RatingsViewModel(userID: String)  : ViewModel(){
    private var ratingsRepository = RatingRepository()
    private var tripRepository = TripRepository()


    private val ratings: MutableLiveData<List<Ratings>> =
        ratingsRepository.getRatings(userID) as MutableLiveData<List<Ratings>>

    fun getRatings(): LiveData<List<Ratings>> {
        return ratings
    }

    /*fun addPassengerReview(
        b: Booking,
        comment: String,
        stars: Float,
        status: Role,
        callback: (success: Boolean, documentId: String?, errorMessage: String?) -> Unit
    ) {
        val newId = ratingsRepository.getAutoID(b.userId!!)
        //current date: timestamp of the review
        val c: Calendar = Calendar.getInstance()
        val today: Date = c.time

        val userId = Firebase.auth.currentUser?.uid
        UserRepository().getPublicUserOnce(userId!!) { user, success, errorMessage ->
            if (user != null && success) {
                val nickname = user.nickname
                val profileImage = user.url

                val review = Ratings(newId, b.bookingId, stars, comment, nickname, status, today, profileImage, user.id)

                ratingsRepository.addReview(b, review, callback)
            } else {
                callback(false, null, errorMessage)
            }

        }
    }*/

    fun addReview(
        ownerTripID: String?, //idReciverReview: driver: ownerTrip, passenger: b.userId
        b: Booking,
        comment: String,
        stars: Float,
        status: Role,
        callback: (success: Boolean, documentId: String?, errorMessage: String?) -> Unit
    ) {
        val newId = ratingsRepository.getAutoID(b.userId!!)
        val c: Calendar = Calendar.getInstance()
        val today: Date = c.time
        var reviewerNickname = ""
        var reviewerImage: String? = ""
        var reviewerId = ""
        var reviewedUserID = ""

        // Driver add a review to passenger
        if(status.role==Role.Passenger.toString()) {
            reviewedUserID = b.userId!!

            val userId = Firebase.auth.currentUser?.uid
            UserRepository().getPublicUserOnce(userId!!) { user, success, errorMessage ->
                if (user != null && success) {
                    reviewerNickname = user.nickname!!
                    reviewerImage = user.url
                    reviewerId = user.id!!

                    val review = Ratings(newId, b.bookingId, stars, comment, reviewerNickname, status, today, reviewerImage, reviewerId)
                    ratingsRepository.addReview(reviewedUserID, b, review, callback)
                } else {
                  Log.e("RATINGS_VIEW_MODEL", errorMessage.toString())
                }
            }

        // Passenger review the driver
        } else if(status.role==Role.Driver.toString()){
            reviewedUserID = ownerTripID!!
            reviewerNickname = b.nickname!!
            reviewerImage = b.userPhoto
            reviewerId = b.userId!!

            val review = Ratings(newId, b.bookingId, stars, comment, reviewerNickname, status, today, reviewerImage, reviewerId)
            ratingsRepository.addReview(reviewedUserID, b, review, callback)
        }


    }

    fun getTrip(tripID: String): LiveData<Trip> {
        return tripRepository.getTrip(tripID) as MutableLiveData<Trip>
    }
}