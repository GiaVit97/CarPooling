package it.polito.mad.carpooling_09.data

import com.google.firebase.firestore.GeoPoint
import java.util.*

data class GeoHashStops(
    var tripId : String? = null,
    var date: Date? = null,
    var geoHash: String? = null,
    var geoPoint: GeoPoint? = null
) {
    companion object {
        const val COLLECTION_GEO_HASH = "geoHashes"
        const val COLLECTION_GEO_HASH_STOPS = "geoHashStops"
        const val FIELD_TRIP_ID = "tripId"
        const val FIELD_DATE = "date"
        const val FIELD_GEO_HASH = "geoHash"
        const val FIELD_GEO_POINT = "geoPoint"
    }
}
