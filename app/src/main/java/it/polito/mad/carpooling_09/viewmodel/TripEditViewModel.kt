package it.polito.mad.carpooling_09.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import it.polito.mad.carpooling_09.data.Booking
import it.polito.mad.carpooling_09.data.Stop
import it.polito.mad.carpooling_09.data.Trip
import it.polito.mad.carpooling_09.repository.BookingRepository
import it.polito.mad.carpooling_09.repository.TripRepository
import java.util.*

class TripEditViewModel(private val tripID: String = " ") : ViewModel() {

    private val tripRepository = TripRepository()
    private val bookingRepository = BookingRepository()

    private val trip: MutableLiveData<Trip> =
        tripRepository.getTrip(tripID) as MutableLiveData<Trip>

    private var isSaving: Boolean = false

    fun getTrip(): LiveData<Trip> {
        return trip
    }

    fun getTripID(): String {
        return tripID
    }

    fun addTrip(
        trip: Trip,
        callback: (success: Boolean, documentId: String?, errorMessage: String?) -> Unit
    ) {
        setIsSaving(true)
        val newId = tripRepository.getAutoID()

        if (!trip.carInfo?.urlPhoto.isNullOrEmpty()) {

            tripRepository.uploadCarPicture(
                trip.carInfo?.urlPhoto!!,
                newId
            ) { success, imageURL, errorMessage ->

                // Only if uploadCarPicture return us success and and imageURL we add the trip
                if (success && imageURL != null) {
                    trip.carInfo?.urlPhoto = imageURL
                    tripRepository.addTrip(newId, trip, callback)
                } else {
                    // There was an error during the image upload, so we return above the
                    // errorMessage given by the uploadCarPicture
                    callback(false, null, errorMessage)
                }
            }

        } else {
            tripRepository.addTrip(newId, trip, callback)
        }
    }

    fun updateTrip(trip: Trip, callback: (success: Boolean, errorMessage: String?) -> Unit) {
        setIsSaving(true)

        if(!trip.carInfo?.urlPhoto.isNullOrEmpty() && !(trip.carInfo?.urlPhoto as String).startsWith("http")) {
            //We have an image that doesn't start with 'http', so we need to load it

            tripRepository.uploadCarPicture(trip.carInfo?.urlPhoto!!, trip.id!!) { success, imageURL, errorMessage ->

                if (success && imageURL != null) {
                    trip.carInfo?.urlPhoto = imageURL
                    tripRepository.updateTrip(trip, callback)
                } else {
                    // There was an error during the image upload, so we return above the
                    // errorMessage given by the uploadCarPicture
                    callback(false, errorMessage)
                }
            }

        } else {
            // The image is null or empty, so it will not be loaded
            tripRepository.updateTrip(trip, callback)
        }
    }

    fun setIsSaving(status: Boolean) {
        isSaving = status
    }

    fun blockTrip(
        tripID : String?,
        blocked: Boolean,
        listStops : List<Stop>?,
        depDate : Date?,
        callback: (success: Boolean, errorMessage: String?) -> Unit) {
        tripRepository.blockTrip(tripID, blocked, listStops, depDate, callback)
    }

    fun deleteAllBookedUsers(tripID: String?, callback: (success: Boolean, errorMessage: String?) -> Unit) {
        bookingRepository.deleteAllBookedUsers(tripID, callback)
    }

}