package it.polito.mad.carpooling_09.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.getField
import com.google.firebase.ktx.Firebase
import it.polito.mad.carpooling_09.data.*

class RatingRepository {
    companion object {
        private const val TAG = "RATING_REPOSITORY"
    }

    private val db = Firebase.firestore

    /**
     * It will retrieve all the trips associated with current authenticated user
     * @return List<Trip> live data object
     */
    fun getRatings(
        userID: String
    ): LiveData<List<Ratings>> {
        val data = MutableLiveData<List<Ratings>>()

        db.collection(User.COLLECTION_USERS)
            .document(userID)
            .collection(Ratings.COLLECTION_RATINGS)
            .addSnapshotListener { values, error ->
                if (error != null) {
                    Log.e(TAG, "Error on 'getYourTrips': $error")
                    throw error
                }

                if (values != null) {
                    val ratingsList = ArrayList<Ratings>()
                    for (doc in values) {
                        ratingsList.add(doc.toObject(Ratings::class.java).apply {
                            id = doc.id
                        })
                    }
                    data.value = ratingsList
                }
            }
        return data
    }

    /**
     * @param userID : user that receive the review
     */
    fun addReview(
        userID: String,
        b: Booking,
        review: Ratings,
        callback: (success: Boolean, documentId: String?, errorMessage: String?) -> Unit
    ) {

        val reviewRef = db.collection(User.COLLECTION_USERS).document(userID)
            .collection(Ratings.COLLECTION_RATINGS).document(review.id!!)

        val bookingRef = db.collection(Trip.COLLECTION_TRIPS).document(b.tripId!!)
            .collection(Booking.COLLECTION_BOOKINGS).document(b.bookingId!!)

        val profileRef = db.collection(User.COLLECTION_USERS).document(userID)

        val publicProfileRef = db.collection(User.COLLECTION_USERS).document(userID)
            .collection(UserPublic.COLLECTION_USER_PUBLIC).document("profile")

        val starsField: String
        val commentField: String
        val numReview: String
        val star: String
        val numReviewPublic: String
        val starPublic: String
        if (review.status == Role.Driver) {
            starsField = Booking.FIELD_DRIVER_STARS
            commentField = Booking.FIELD_DRIVER_COMMENT
            numReview = User.FIELD_NUM_DRIVER_REVIEWS
            star = User.FIELD_DRIVER_STAR
            numReviewPublic = UserPublic.FIELD_NUM_DRIVER_REVIEWS
            starPublic = UserPublic.FIELD_DRIVER_STAR

        } else {
            starsField = Booking.FIELD_PASSENGER_STARS
            commentField = Booking.FIELD_PASSENGER_COMMENT
            numReview = User.FIELD_NUM_PASSENGER_REVIEWS
            star = User.FIELD_PASSENGER_STAR
            numReviewPublic = UserPublic.FIELD_NUM_PASSENGER_REVIEWS
            starPublic = UserPublic.FIELD_PASSENGER_STAR
        }

        db.runTransaction { transaction ->

            val snapshot = transaction.get(profileRef)

            val oldCount = snapshot.getDouble(numReview) ?: 0.0
            val oldStars = snapshot.getDouble(star) ?: 0.0
            val newStar = (oldStars * oldCount + review.star!!) / (oldCount + 1)
            transaction.update(
                profileRef,
                star,
                newStar,
                numReview,
                oldCount + 1
            )
            transaction.update(
                publicProfileRef,
                starPublic,
                newStar,
                numReviewPublic,
                oldCount + 1
            )

            transaction.set(reviewRef, review)
            transaction.update(bookingRef, starsField, review.star)
            transaction.update(bookingRef, commentField, review.comment)

        }.addOnSuccessListener {
            Log.d(TAG, "Review added successfully")
            callback(true, review.id, null)
        }.addOnFailureListener { e ->
            Log.e(TAG, "Error on 'addReview': ${e.message}")
            callback(false, null, e.message)
        }
    }

    fun getAutoID(userID: String): String {
        val ref = db.collection(User.COLLECTION_USERS)
            .document(userID)
            .collection(Ratings.COLLECTION_RATINGS)
            .document()
        return ref.id
    }

}