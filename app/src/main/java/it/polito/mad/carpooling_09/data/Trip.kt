package it.polito.mad.carpooling_09.data

import java.util.*

/**
 * POJO Data class that model the trip details
 */
data class Trip(
    var id: String? = null,
    var userID: String? = null,

    var stops: List<Stop> = mutableListOf(),
    var stopCities: MutableMap<String, Boolean> = mutableMapOf(),

    var price: Double? = null,
    var carInfo: CarInfo? = null,
    var availableSeats: Int? = null,
    var description: String? = null,
    var tripPreferences: TripPreferences? = null,
    var estimatedDuration: String? = null, //TODO: Lo mettiamo noi o lo dobbiamo calcolare?
    var departureDate : Date? = null,
    var blocked : Boolean? = null,
) {
    companion object {
        const val COLLECTION_TRIPS = "trips"

        const val FIELD_USERID = "userID"
        const val FIELD_STOPS = "stops"
        const val FIELD_STOP_CITIES = "stopCities"
        const val FIELD_PRICE = "price"
        const val FIELD_CAR_INFO = "carInfo"
        const val FIELD_AVAILABLE_SEATS = "availableSeats"
        const val FIELD_DESCRIPTION = "description"
        const val FIELD_ADDITIONAL_INFO = "additionalInfo"
        const val FIELD_ESTIMATED_DURATION = "estimatedDuration"
        const val FIELD_DEPARTURE_DATE = "departureDate"
        const val FIELD_BLOCKED = "blocked"

    }
}