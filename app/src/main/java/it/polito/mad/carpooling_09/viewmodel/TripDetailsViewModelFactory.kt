package it.polito.mad.carpooling_09.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class TripDetailsViewModelFactory(private val tripID : String, private val userID : String = " ") : ViewModelProvider.Factory {


    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TripDetailsViewModel::class.java)) {
            return TripDetailsViewModel(tripID, userID) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }

}