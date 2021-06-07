package it.polito.mad.carpooling_09.data


/**
 * Public profile information POJO
 */
data class UserPublic(
    var id: String? = null,
    var nickname: String? = null,
    var url: String? = null,
    var email: String? = null,
    var driverStar: Float = 0.0f,
    var numDriverReviews: Int= 0,
    var passengerStar: Float = 0.0f,
    var numPassengerReviews: Int= 0,

) {

    companion object{
        const val COLLECTION_USER_PUBLIC= "public_profile"
        const val COLLECTION_REVIEW= "review"

        const val FIELD_ID = "id"
        const val FIELD_NICKNAME = "nickname"
        const val FIELD_PROFILE_IMAGE_URL = "url"
        const val FIELD_EMAIL = "email"
        const val FIELD_DRIVER_STAR = "driverStar"
        const val FIELD_NUM_DRIVER_REVIEWS = "numDriverReviews"
        const val FIELD_PASSENGER_STAR = "passengerStar"
        const val FIELD_NUM_PASSENGER_REVIEWS = "numPassengerReviews"
    }
}