package it.polito.mad.carpooling_09.data

import com.google.firebase.firestore.GeoPoint
import java.util.*

data class Stop(
    var name: String? = null,
    var location: GeoPoint? = null,
    var time: Date? = null,
    var geoHash: String ? = null,
    var available_seats : Int? = null

) {
    companion object{
        const val FIELD_STOPS_NAME = "name"
        const val FIELD_STOPS_LOCATION = "location"
        const val FIELD_STOP_TIME = "time"
        const val FIELD_STOP_GEO_HASH = "geoHash"
        const val FIELD_STOP_AVAILABLE_SEATS = "available_seats"
    }
}
