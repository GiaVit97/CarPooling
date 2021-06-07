package it.polito.mad.carpooling_09.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class RatingsViewModelFactory(private val userID : String) : ViewModelProvider.Factory {


    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RatingsViewModel::class.java)) {
            return RatingsViewModel(userID) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }

}