package it.polito.mad.carpooling_09.data

import java.util.*

data class Ratings(
    var id: String? = null,
    var bookingID: String? = null,
    var star: Float? = null,
    var comment: String? = null,
    var nickname: String? = null,
    var status: Role? = null,
    var date: Date? = null,
    var url: String? = null,
    var userId: String? = null

    ) {
    companion object {
        const val COLLECTION_RATINGS = "review"
        const val FIELD_STARS = "star"
        const val FIELD_REVIEW_ID = "id"
        const val FIELD_COMMENT = "comment"
        const val FIELD_NICKNAME = "nickname"
        const val FIELD_URL = "url"
        const val FIELD_USER_ID = "userId"
    }
}

enum class Role(val role: String) {
    Passenger("Passenger"),
    Driver("Driver"),
}


