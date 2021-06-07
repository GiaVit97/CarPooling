package it.polito.mad.carpooling_09.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import it.polito.mad.carpooling_09.data.Trip
import it.polito.mad.carpooling_09.repository.TripRepository

class TripListViewModel : ViewModel() {
    private var tripRepository = TripRepository()

    private val trips : MutableLiveData<List<Trip>> = tripRepository.getYourTrips() as MutableLiveData<List<Trip>>

    fun getTrips() : LiveData<List<Trip>> {
        return trips
    }

}