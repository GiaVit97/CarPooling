package it.polito.mad.carpooling_09.data

import com.google.firebase.firestore.GeoPoint
import java.util.*

data class TripFilter(
    var departureLocation: GeoPoint? = null,
    var arrivalLocation: GeoPoint? = null,
    var departureDate: Date? = null,
    var price: Int? = null,
    var smoke: PreferencesType? = null,
    var animals: PreferencesType? = null,
    var music: PreferencesType? = null
)