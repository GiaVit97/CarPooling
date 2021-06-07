package it.polito.mad.carpooling_09.data

import com.google.firebase.firestore.GeoPoint
import kotlinx.parcelize.Parcelize
import java.util.*

/**
 * POJO Data class that model the user information
 */

data class User(
    var id: String? = null,
    var fullName: String? = null,
    var nickname: String? = null,
    var email: String? = null,
    var location: GeoPoint? = null,
    var birthday: Date? = null,
    var telephone: Long? = null,
    var url: String? = null,
    var driverStar: Float = 0.0f,
    var numDriverReviews: Int = 0,
    var passengerStar: Float = 0.0f,
    var numPassengerReviews: Int = 0

){

    companion object {
       const val COLLECTION_USERS = "users"

       const val FIELD_EMAIL = "email"
       const val FIELD_LOCATION = "location"
       const val FIELD_NAME = "fullName"
       const val FIELD_NICKNAME = "nickname"
       const val FIELD_TELEPHONE_NUMBER = "telephone"
       const val FIELD_BIRTHDATE = "birthday"
       const val FIELD_PROFILE_IMAGE_URL = "url"

       const val FIELD_DRIVER_STAR = "driverStar"
       const val FIELD_NUM_DRIVER_REVIEWS = "numDriverReviews"
       const val FIELD_PASSENGER_STAR = "passengerStar"
       const val FIELD_NUM_PASSENGER_REVIEWS = "numPassengerReviews"
    }
}
