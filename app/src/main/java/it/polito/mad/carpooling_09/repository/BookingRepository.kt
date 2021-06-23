package it.polito.mad.carpooling_09.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.getField
import com.google.firebase.ktx.Firebase
import it.polito.mad.carpooling_09.data.*
import it.polito.mad.carpooling_09.data.Booking.Companion.COLLECTION_BOOKINGS
import it.polito.mad.carpooling_09.data.Booking.Companion.FIELD_NICKNAME
import it.polito.mad.carpooling_09.data.Booking.Companion.FIELD_STATUS
import it.polito.mad.carpooling_09.data.Booking.Companion.FIELD_USER_ID
import it.polito.mad.carpooling_09.data.Booking.Companion.FIELD_USER_PHOTO
import it.polito.mad.carpooling_09.data.Trip.Companion.COLLECTION_TRIPS

class BookingRepository {

    companion object {
        const val TAG = "BOOKING_REPOSITORY"
    }

    private val db = Firebase.firestore

    /**
     * Get all the bookings of that trip
     * @param tripID : id of the trip to which retrieve the bookings
     */
    fun getBookingsByTripID(tripID: String): LiveData<List<Booking>> {
        val data = MutableLiveData<List<Booking>>()

        db.collection(COLLECTION_TRIPS).document(tripID).collection(COLLECTION_BOOKINGS)
            .addSnapshotListener { values, error ->
                if (error != null) {
                    Log.e(TAG, "Error on 'getBookings': $error")
                    throw error
                }

                if (values != null) {
                    val bookingList = ArrayList<Booking>()
                    for (doc in values) {

                        try {
                            bookingList.add(doc.toObject(Booking::class.java).apply {
                                bookingId = doc.id
                            })
                        } catch (e: Exception) {
                            Log.e(TAG, "Error converting document ${doc.id}")
                        }


                    }
                    data.value = bookingList
                }
            }
        return data
    }


    /**
     * Get the bookings of a given userID and with a given status (accepted, rejected, pending)
     * @param userID
     * @param status
     * @return LiveData list of bookings
     */
    fun getBookingsByUserIdAndStatus(userID: String, status: Status): LiveData<List<Booking>> {
        val data = MutableLiveData<List<Booking>>()

        db.collectionGroup(COLLECTION_BOOKINGS).whereEqualTo(FIELD_USER_ID, userID)
            .whereEqualTo(FIELD_STATUS, status).addSnapshotListener { values, error ->
                if (error != null) {
                    Log.e(TAG, "Error on 'getBookingsByUserId': $error")
                    throw error
                }

                if (values != null) {
                    val bookingList = ArrayList<Booking>()
                    for (doc in values) {
                        bookingList.add(doc.toObject(Booking::class.java).apply {
                            bookingId = doc.id
                        })
                    }
                    data.value = bookingList
                }
            }
        return data
    }


    /**
     * Update a booking setting the status to accepted. Change also the number of available seats
     * inside the trip stops.
     * @param booking
     * @param tripID
     * @param callback
     */
    fun updateBooking(
        booking: Booking, tripID: String, callback: (success: Boolean, error: String?) -> Unit
    ) {

        val tripRef = db.collection(COLLECTION_TRIPS).document(tripID)

        val bookingRef = db.collection(COLLECTION_TRIPS).document(tripID)
            .collection(COLLECTION_BOOKINGS).document(booking.bookingId!!)

        tripRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    Log.d(TAG, "DocumentSnapshot data: ${document.data}")

                    val trip = document.toObject(Trip::class.java)?.apply {
                        id = userID
                    }

                    // Update the available seats for all the stops that the user booked
                    booking.stops.forEach { stopBooked ->

                        trip?.stops?.forEach { stop ->

                            if (stopBooked.name == stop.name && stop.available_seats!! > 0) {
                                stop.available_seats = stop.available_seats?.minus(1)
                            }

                        }
                    }

                    //Transaction
                    db.runTransaction { transaction ->

                        if (trip != null) {
                            transaction.update(bookingRef, FIELD_STATUS, Status.Accepted)
                            transaction.set(tripRef, trip)
                        }


                    }
                        .addOnSuccessListener {
                            Log.d(TAG, "Booking updated successfully")
                            callback(true, null)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error on 'updateBooking': ${e.message}")
                            callback(false, e.message)
                        }

                } else {
                    Log.d(TAG, "No such document")
                    callback(false, "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
                callback(false, exception.message)
            }

    }

    fun getAutoID(tripID: String): String {
        val ref = db.collection(COLLECTION_TRIPS).document(tripID)
            .collection(COLLECTION_BOOKINGS).document()
        return ref.id
    }

    fun deleteBooking(
        booking: Booking,
        tripID: String,
        wasAccepted: Boolean,
        callback: (success: Boolean, errorMessage: String?) -> Unit
    ) {

        if (wasAccepted) {
            //The trip was accepted, we need to update the number of available seats once the booking is eliminated
            var trip: Trip?

            val docRef = db.collection(COLLECTION_TRIPS).document(tripID)

            val bookingRef = db.collection(COLLECTION_TRIPS).document(tripID)
                .collection(COLLECTION_BOOKINGS).document(booking.bookingId!!)

            docRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        Log.d(TAG, "DocumentSnapshot data: ${document.data}")

                        trip = document.toObject(Trip::class.java)?.apply {
                            id = userID
                        }

                        for (stop in trip?.stops!!) {
                            for (i in 1 until booking.stops.size) {
                                if (stop.name == booking.stops[i].name) {
                                    stop.available_seats = stop.available_seats?.plus(1)
                                }
                            }
                        }

                        //Transaction
                        db.runTransaction { transaction ->
                            transaction.update(
                                bookingRef,
                                FIELD_STATUS,
                                Status.Rejected
                            )
                            transaction.set(docRef, trip!!)


                        }
                            .addOnSuccessListener {
                                Log.d(TAG, "Booking deleted successfully")
                                callback(true, null)
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error on 'deleteBooking': ${e.message}")
                                callback(false, e.message)
                            }

                    } else {
                        Log.d(TAG, "No such document")
                        callback(false, "No such document")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d(TAG, "get failed with ", exception)
                    callback(false, exception.message)
                }


        } else {
            // The trip was pending, so it didn't modify the number of available seats
            db.collection(COLLECTION_TRIPS).document(tripID)
                .collection(COLLECTION_BOOKINGS).document(booking.bookingId!!)
                .update(FIELD_STATUS, Status.Rejected)
                .addOnSuccessListener {
                    Log.d(TAG, "Booking deleted successfully")
                    callback(true, null)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error on 'deleteBooking': ${e.message}")
                    callback(false, e.message)
                }
        }

    }


    fun addBooking(
        tripID: String,
        bookingID: String,
        booking: Booking,
        callback: (success: Boolean, error: String?) -> Unit
    ) {
        db.collection(COLLECTION_TRIPS).document(tripID)
            .collection(COLLECTION_BOOKINGS).document(bookingID)
            .set(booking)
            .addOnSuccessListener {
                Log.d(TAG, "Booking added successfully")
                callback(true, null)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error on 'addBooking': ${e.message}")
                callback(false, e.message)
            }
    }

    fun deleteBookingWithTripID(
        tripID: String?,
        userID: String?,
        callback: (success: Boolean, error: String?) -> Unit
    ) {

        if (tripID != null && userID != null) {
            db.collection(Trip.COLLECTION_TRIPS).document(tripID)
                .collection(Booking.COLLECTION_BOOKINGS).whereEqualTo(Booking.FIELD_USER_ID, userID)
                .get().addOnSuccessListener { snapshot ->
                    db.runTransaction { transaction ->
                        snapshot.documents.forEach {
                            transaction.update(
                                it.reference,
                                Booking.FIELD_STATUS,
                                Status.Rejected
                            )
                        }
                    }.addOnSuccessListener {
                        callback(true, null)
                    }.addOnFailureListener { e ->
                        Log.e(TAG, "Error on deleteBookingWithTripID: ${e.message}")
                        callback(false, e.message)
                    }
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Error on deleteBookingWithTripID: ${e.message}")
                    callback(false, e.message)
                }
        } else {
            Log.e(TAG, "Trip ID can't be null")
        }
    }

    fun deleteAllBookedUsers(
        tripID: String?,
        callback: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        if (tripID != null) {
            db.collection(Trip.COLLECTION_TRIPS).document(tripID)
                .collection(Booking.COLLECTION_BOOKINGS).get().addOnSuccessListener { snapshot ->
                    db.runTransaction { transaction ->
                        snapshot.documents.forEach { transaction.delete(it.reference) }

                    }.addOnSuccessListener {
                        callback(true, null)

                    }.addOnFailureListener { e ->
                        Log.e(TAG, "Error on deleteAllBookedUsers: ${e.message}")
                        callback(false, e.message)

                    }
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Error on deleteAllBookedUsers: ${e.message}")
                    callback(false, e.message)

                }
        } else {
            Log.e(TAG, "Trip ID can't be null")
        }
    }
}