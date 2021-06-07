package it.polito.mad.carpooling_09.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQueryBounds
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import it.polito.mad.carpooling_09.data.*
import it.polito.mad.carpooling_09.utils.isThereAvailableSeats
import it.polito.mad.carpooling_09.utils.stopsNext
import java.io.File
import java.io.FileInputStream
import java.util.*
import kotlin.collections.ArrayList


class TripRepository {

    companion object {
        const val TAG = "TRIP_REPOSITORY"
    }

    private val db = Firebase.firestore
    private var storage = Firebase.storage

    /**
     * It will retrieve all the trips associated with current authenticated user
     * @return List<Trip> live data object
     */
    fun getYourTrips(): LiveData<List<Trip>> {
        val data = MutableLiveData<List<Trip>>()

        db.collection(Trip.COLLECTION_TRIPS)
            .whereEqualTo(Trip.FIELD_USERID, Firebase.auth.currentUser?.uid)
            .addSnapshotListener { values, error ->
                if (error != null) {
                    Log.e(TAG, "Error on 'getYourTrips': $error")
                    throw error
                }

                if (values != null) {
                    val tripsList = ArrayList<Trip>()
                    for (doc in values) {

                        try {
                            tripsList.add(doc.toObject(Trip::class.java).apply {
                                id = doc.id
                            })
                        } catch (e: Exception) {
                            Log.e(TAG, "Error converting document ${doc.id}")
                        }
                    }
                    data.value = tripsList
                }
            }
        return data
    }


    /**
     * It will retrieve all the trips available inside DB except the authenticated user ones
     * @return List<Trip> live data object
     */
    fun getOthersTrips(tripFilter: TripFilter): LiveData<List<Trip>> {

        val data = MutableLiveData<List<Trip>>()
        val radiusInM = 5 * 1000.0

        tripFilter.departureLocation?.also { departure ->
            tripFilter.arrivalLocation?.also { arrival ->

                //Empty the list before add again all the elements
                val tripsList = ArrayList<Trip>()

                val departureBounds: List<GeoQueryBounds> =
                    GeoFireUtils.getGeoHashQueryBounds(
                        GeoLocation(
                            departure.latitude,
                            departure.longitude
                        ), radiusInM
                    )

                val arrivalBounds: List<GeoQueryBounds> =
                    GeoFireUtils.getGeoHashQueryBounds(
                        GeoLocation(
                            arrival.latitude,
                            arrival.longitude
                        ), radiusInM
                    )

                val tasksDep: MutableList<Task<QuerySnapshot>> = ArrayList()
                val tasksArr: MutableList<Task<QuerySnapshot>> = ArrayList()

                for (b in departureBounds) {
                    val q: Query = db.collection(GeoHashStops.COLLECTION_GEO_HASH)
                        .orderBy(GeoHashStops.FIELD_GEO_HASH)
                        .startAt(b.startHash)
                        .endAt(b.endHash)

                    tasksDep.add(q.get())
                }

                for (b in arrivalBounds) {
                    val q: Query = db.collection(GeoHashStops.COLLECTION_GEO_HASH)
                        .orderBy(GeoHashStops.FIELD_GEO_HASH)
                        .startAt(b.startHash)
                        .endAt(b.endHash)

                    tasksArr.add(q.get())
                }

                Tasks.whenAllComplete(tasksDep)
                    .addOnCompleteListener {
                        val matchingDepDocs: MutableSet<String> = mutableSetOf()
                        for (task in tasksDep) {
                            val snap = task.result
                            if (snap != null) {
                                for (doc in snap.documents) {
                                    val geoPoint = doc.getGeoPoint(GeoHashStops.FIELD_GEO_POINT)!!


                                    // We have to filter out a few false positives due to GeoHash
                                    // accuracy, but most will match
                                    val docLocation =
                                        GeoLocation(geoPoint.latitude, geoPoint.longitude)
                                    val distanceInM =
                                        GeoFireUtils.getDistanceBetween(
                                            docLocation, GeoLocation(
                                                departure.latitude,
                                                departure.longitude
                                            )
                                        )
                                    if (distanceInM <= radiusInM) {
                                        val depDate = doc.getDate(GeoHashStops.FIELD_DATE)!!
                                        if (depDate >= Date()) {
                                            matchingDepDocs.add(doc[GeoHashStops.FIELD_TRIP_ID].toString())
                                        }
                                    }
                                }
                            }


                        }
                        // Here I have all the right departure, now I have to take the arrival
                        Tasks.whenAllComplete(tasksArr)
                            .addOnCompleteListener {
                                val matchingArrDocs: MutableSet<String> = mutableSetOf()
                                for (task in tasksArr) {
                                    val snap = task.result
                                    if (snap != null) {
                                        for (doc in snap.documents) {
                                            val geoPoint =
                                                doc.getGeoPoint(GeoHashStops.FIELD_GEO_POINT)!!


                                            // We have to filter out a few false positives due to GeoHash
                                            // accuracy, but most will match
                                            val docLocation =
                                                GeoLocation(
                                                    geoPoint.latitude,
                                                    geoPoint.longitude
                                                )
                                            val distanceInM =
                                                GeoFireUtils.getDistanceBetween(
                                                    docLocation, GeoLocation(
                                                        arrival.latitude,
                                                        arrival.longitude
                                                    )
                                                )
                                            if (distanceInM <= radiusInM) {
                                                val depDate = doc.getDate(GeoHashStops.FIELD_DATE)!!
                                                if (depDate >= Date()) {
                                                    matchingArrDocs.add(doc[GeoHashStops.FIELD_TRIP_ID].toString())
                                                }
                                            }
                                        }
                                    }


                                }
                                // Here I have all the right arrival, now I have to check with departure bounds

                                val tripsID = matchingDepDocs.intersect(matchingArrDocs)
                                db.runTransaction { transaction ->
                                    tripsID.forEach { trip ->
                                        val tripRef =
                                            db.collection(Trip.COLLECTION_TRIPS).document(trip)
                                        val snapshot = transaction.get(tripRef)
                                        snapshot.toObject(Trip::class.java)?.apply {
                                            id = snapshot.id
                                        }?.let { it1 -> tripsList.add(it1) }
                                    }
                                }.addOnSuccessListener {
                                    data.value = filter(tripsList, tripFilter)
                                }
                            }
                    }
            }
        } ?: run {


            db.collection(Trip.COLLECTION_TRIPS).addSnapshotListener { values, error ->
                if (error != null) {
                    Log.e(TAG, "Error on 'getOthersTrips': $error")
                    throw error
                }

                //Empty the list before add again all the elements
                val tripsList = ArrayList<Trip>()

                if (values != null) {
                    for (doc in values) {

                        try {
                            tripsList.add(doc.toObject(Trip::class.java).apply {
                                id = doc.id
                            })
                        } catch (e: Exception) {
                            Log.e(TAG, "Error converting document ${doc.id}")
                        }
                    }
                    val result = filter(tripsList, tripFilter)
                    data.value = result
                }

            }
        }
        return data
    }

    /**
     * It will retrieve the trip details given an tripId
     * @param tripID id of the trip to retrieve
     * @return Trip live data object
     */
    fun getTrip(tripID: String): LiveData<Trip> {
        val data = MutableLiveData<Trip>()

        db.collection(Trip.COLLECTION_TRIPS).document(tripID).addSnapshotListener { value, error ->
            if (error != null) {
                Log.e(TAG, "Error on 'getTrip': $error")
                throw error
            }

            if (value != null) {
                try {
                    data.value = value.toObject(Trip::class.java)?.apply {
                        id = value.id
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error getTrip: ${e.message}")
                }
            }
        }
        return data
    }

    fun getTripOnce(
        tripID: String,
        callback: (success: Boolean, trip: Trip?, errorMessage: String?) -> Unit
    ) {

        var data: Trip?

        db.collection(Trip.COLLECTION_TRIPS).document(tripID).get()
            .addOnSuccessListener { document ->

                if (document != null) {
                    try {
                        data = document.toObject(Trip::class.java)?.apply {
                            id = document.id
                        }
                        callback(true, data, null)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error getTripOnce: ${e.message}")
                        callback(false, null, "Generic error")
                    }

                } else {
                    Log.e(TAG, "Error getTripOnce: No such document")
                    callback(false, null, "No such document")
                }

            }.addOnFailureListener { e ->
                Log.e(TAG, "Error getTripOnce ${e.message}")
                callback(false, null, e.message)
            }
    }


    /**
     * It will add a new trip into the DB
     * @param trip Trip object that represents the new trip
     * @param callback that return the documentId of the just uploaded trip or an error
     */
    fun addTrip(
        idDocument: String,
        trip: Trip,
        callback: (success: Boolean, documentId: String?, errorMessage: String?) -> Unit
    ) {

        val tripRef = db.collection(Trip.COLLECTION_TRIPS)
            .document(idDocument)

        db.runTransaction { transaction ->

            transaction.set(tripRef, trip)

            for (stop in trip.stops) {
                val geoHashRef =
                    db.collection(GeoHashStops.COLLECTION_GEO_HASH).document()
                val obj =
                    GeoHashStops(idDocument, trip.departureDate, stop.geoHash, stop.location)

                transaction.set(geoHashRef, obj)
            }
        }.addOnSuccessListener {
            Log.d(TAG, "Trip added successfully")
            callback(true, idDocument, null)
        }.addOnFailureListener { e ->
            Log.e(TAG, "Error addTrip: ${e.message}")
            callback(false, null, e.message)
        }

    }

    /**
     * It will update an existing trip into the DB
     * @param trip Trip object that represents the new trip
     * @param callback that return the success of the operation or an error
     */
    fun updateTrip(trip: Trip, callback: (success: Boolean, error: String?) -> Unit) {

        if (trip.id != null) {

            val tripRef = db.collection(Trip.COLLECTION_TRIPS).document(trip.id!!)

            db.collectionGroup(GeoHashStops.COLLECTION_GEO_HASH)
                .whereEqualTo(GeoHashStops.FIELD_TRIP_ID, trip.id).get()
                .addOnSuccessListener { value ->
                    if (value != null) {
                        db.runTransaction { transaction ->

                            transaction.set(tripRef, trip)

                            // Delete old stops
                            value.forEach { transaction.delete(it.reference) }

                            // Add again all stops
                            for (stop in trip.stops) {
                                val geoHashRef =
                                    db.collection(GeoHashStops.COLLECTION_GEO_HASH).document()
                                val obj = GeoHashStops(
                                    trip.id,
                                    trip.departureDate,
                                    stop.geoHash,
                                    stop.location
                                )
                                transaction.set(geoHashRef, obj)
                            }
                        }.addOnSuccessListener {
                            if (!value.metadata.hasPendingWrites()) {
                                Log.d(TAG, "Trip updated successfully")
                                callback(true, null)
                            }
                        }.addOnFailureListener { e ->
                            Log.e(TAG, "Error updateTrip: ${e.message}")
                            callback(false, e.message)
                        }

                    }
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Error updateTrip: ${e.message}")
                    callback(false, e.message)
                }

        } else {
            Log.e(TAG, "Error UpdateTrip: trip is null")
            callback(false, "Trip id can't be null")
        }

    }

    /**
     * It will upload the car image to Firebase storage
     * @param path local file path of the image saved into the device
     * @param tripID id of the trip associated with that image
     * @param callback that return the url of the just uploaded images or an error
     */
    fun uploadCarPicture(
        path: String,
        tripID: String,
        callback: (success: Boolean, imageURL: String?, error: String?) -> Unit
    ) {
        val pathObj = File(path)
        if (!pathObj.exists()) {
            callback(false, null, "Path doesn't exist")
        } else {

            val ref = storage.reference.child("carPhoto/${tripID}.jpg")

            ref.putStream(FileInputStream(File(path)))
                .addOnSuccessListener {
                    ref.downloadUrl
                        .addOnSuccessListener { uri ->
                            Log.d(TAG, "Upload car picture successfully")
                            callback(true, uri.toString(), null)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error uploadCarPicture: ${e.message}")
                            callback(false, null, e.message)
                        }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error uploadCarPicture: ${e.message}")
                    callback(false, null, e.message)
                }
        }
    }

    /**
     * It will block/unblock the trip
     * @param tripID : tripID to be blocked/unblocked
     * @param blocked : true if the trip is to be blocked, false to be unblocked
     */
    fun blockTrip(
        tripID: String?,
        blocked: Boolean,
        listStops: List<Stop>?,
        depDate: Date?,
        callback: (success: Boolean, errorMessage: String?) -> Unit
    ) {

        if (tripID != null) {

            val tripRef = db.collection(Trip.COLLECTION_TRIPS).document(tripID)


            if (blocked) {

                db.collection(Trip.COLLECTION_TRIPS).document(tripID)
                    .collection(Booking.COLLECTION_BOOKINGS).get()
                    .addOnSuccessListener { snapshot ->

                        val bookings = snapshot.documents

                        // It's true, so we want to block a trip;
                        // If we block a trip we have to remove all the geoHashes for that trip and update the trip
                        db.collectionGroup(GeoHashStops.COLLECTION_GEO_HASH)
                            .whereEqualTo(GeoHashStops.FIELD_TRIP_ID, tripID).get()
                            .addOnSuccessListener { value ->
                                if (value != null) {

                                    db.runTransaction { transaction ->

                                        // Delete all the documents
                                        value.forEach { transaction.delete(it.reference) }

                                        // Change the trip to blocked
                                        transaction.update(tripRef, Trip.FIELD_BLOCKED, blocked)

                                        // Delete all the bookings associated to that trip
                                        bookings.forEach { transaction.delete(it.reference) }

                                    }.addOnSuccessListener {
                                        Log.d(TAG, "Trip blocked successfully")
                                        callback(true, null)

                                    }.addOnFailureListener { e ->
                                        Log.e(TAG, "Error blockTrip: ${e.message}")
                                        callback(false, e.message)
                                    }
                                }

                            }.addOnFailureListener { e ->
                                Log.e(TAG, "Error on blockTrip: ${e.message}")
                                callback(false, e.message)
                            }

                    }.addOnFailureListener { e ->
                        Log.e(TAG, "Error on blockTrip: ${e.message}")
                        callback(false, e.message)
                    }

            } else {
                // It's false, we want to unblock the trip; We have to add again all the stops of the trip
                db.runTransaction { transaction ->

                    // Change the block status to false
                    transaction.update(tripRef, Trip.FIELD_BLOCKED, blocked)

                    if (listStops != null) {
                        for (stop in listStops) {
                            val geoHashRef =
                                db.collection(GeoHashStops.COLLECTION_GEO_HASH).document()
                            val obj = GeoHashStops(tripID, depDate, stop.geoHash, stop.location)
                            transaction.set(geoHashRef, obj)
                        }
                    }

                }.addOnSuccessListener {
                    Log.d(TAG, "Trip unblocked successfully")
                    callback(true, null)
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Error on 'blockTrip': ${e.message}")
                    callback(false, e.message)
                }
            }

        } else {
            Log.e(TAG, "Error blockTrip: trip is null")
            callback(false, "Trip id can't be null")
        }

    }

    fun getAutoID(): String {
        val ref = db.collection(Trip.COLLECTION_TRIPS).document()
        return ref.id
    }

    private fun filter(
        tripsList: ArrayList<Trip>,
        tripFilter: TripFilter?
    ): List<Trip> {

        return tripsList
            .asSequence()
            .filter {
                if (tripFilter?.departureDate != null) {
                    val res = sameDay(it.departureDate!!, tripFilter.departureDate!!)
                    res
                } else true
            }
            .filter {
                if (tripFilter?.price != null && tripFilter.price != 0) {
                    it.price!! <= tripFilter.price!!
                } else {
                    true
                }
            }
            .filter {
                if (tripFilter?.smoke != null) {
                    if (tripFilter.smoke == PreferencesType.Yes && it.tripPreferences?.smoke == PreferencesType.No)
                        false
                    else !(tripFilter.smoke == PreferencesType.No && it.tripPreferences?.smoke == PreferencesType.Yes)
                } else true
            }
            .filter {
                if (tripFilter?.animals != null) {
                    if (tripFilter.animals == PreferencesType.Yes && it.tripPreferences?.animals == PreferencesType.No)
                        false
                    else !(tripFilter.animals == PreferencesType.No && it.tripPreferences?.animals == PreferencesType.Yes)
                } else true
            }
            .filter {
                if (tripFilter?.music != null) {
                    if (tripFilter.music == PreferencesType.Yes && it.tripPreferences?.music == PreferencesType.No)
                        false
                    else !(tripFilter.music == PreferencesType.No && it.tripPreferences?.music == PreferencesType.Yes)
                } else true
            }
            .filter { Firebase.auth.currentUser?.uid != it.userID }
            .filter { isThereAvailableSeats(it.stops, tripFilter) }
            .filter { it.blocked == false }
            .filter {
                if (tripFilter?.departureLocation != null && tripFilter.arrivalLocation != null) {

                    val departureIndex = stopsNext(it.stops, tripFilter.departureLocation!!)
                    val arrivalIndex = stopsNext(it.stops, tripFilter.arrivalLocation!!)

                    departureIndex < arrivalIndex

                } else true
            }
            .toList()
    }

    /**
     * Understand if two dates has the same day month and year
     * @return true / false
     */
    private fun sameDay(date1: Date, date2: Date): Boolean {
        val cal1: Calendar = Calendar.getInstance()
        val cal2: Calendar = Calendar.getInstance()
        cal1.time = date1
        cal2.time = date2
        return cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR) &&
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
    }

}