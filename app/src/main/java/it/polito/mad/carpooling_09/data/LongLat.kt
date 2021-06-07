package it.polito.mad.carpooling_09.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LongLat(
    var lat : Double? = null,
    var long : Double? = null
): Parcelable
