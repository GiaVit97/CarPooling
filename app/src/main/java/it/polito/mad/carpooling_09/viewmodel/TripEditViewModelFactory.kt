package it.polito.mad.carpooling_09.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class TripEditViewModelFactory(private val tripID : String = " ") : ViewModelProvider.Factory {


    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TripEditViewModel::class.java)) {
            return TripEditViewModel(tripID) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }

}