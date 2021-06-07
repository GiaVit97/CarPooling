package it.polito.mad.carpooling_09.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

/**
 * POJO Data class that model the user booking for a trip
 */
@Parcelize
data class Booking(

    var bookingId: String? = null,
    var tripId: String ? = null,

    var userId: String? = null,
    var userPhoto: String? = null,
    var nickname: String? = null,

    var stops: @RawValue List<Stop> = mutableListOf(),

    var price: Double? = null,

    var status: Status = Status.Pending,
    var passengerStars: Float = 0f,
    var passengerComment: String? = null,
    var driverStars: Float = 0f,
    var driverComment: String? = null

    ): Parcelable {
    companion object {
        const val COLLECTION_BOOKINGS = "bookings"

        const val FIELD_BOOKING_ID = "bookingId"
        const val FIELD_TRIP_ID = "tripId"
        const val FIELD_USER_ID = "userId"
        const val FIELD_USER_PHOTO = "userPhoto"
        const val FIELD_NICKNAME = "nickname"
        const val FIELD_DEPARTURE_LOCATION = "departureLocation"
        const val FIELD_ARRIVAL_LOCATION = "arrivalLocation"
        const val FIELD_PRICE = "price"
        const val FIELD_STATUS = "status"
        const val FIELD_PASSENGER_STARS = "passengerStars"
        const val FIELD_PASSENGER_COMMENT = "passengerComment"
        const val FIELD_DRIVER_STARS = "driverStars"
        const val FIELD_DRIVER_COMMENT = "driverComment"
    }
}

/**
 * Represent the status of the booking
 * accepted: the trip owner accept the booking
 * pending:  the trip owner has not yet answered
 * rejected: the trip owner refuse the booking
 */
enum class Status(val status: String) {
    Accepted("accepted"),
    Pending("pending"),
    Rejected("rejected")
}
