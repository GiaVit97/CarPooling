package it.polito.mad.carpooling_09.utils

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.location.Address
import android.location.Geocoder
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import coil.load
import coil.size.Scale
import coil.transform.CircleCropTransformation
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.GeoPoint
import it.polito.mad.carpooling_09.MainActivity
import it.polito.mad.carpooling_09.R
import it.polito.mad.carpooling_09.data.Stop
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


fun backEditDialog(context: Context, activity: Activity, positiveAction: () -> Unit) {
    closeKeyboard(activity)
    MaterialAlertDialogBuilder(context)
        .setTitle("Do you want to go back?")
        .setMessage("There are unsaved changes, do you want to exit anyway?")
        /*.setNeutralButton(resources.getString(R.string.cancel)) { dialog, which ->
            // Respond to neutral button press
        }*/
        .setNegativeButton("cancel") { dialog, which ->
            // Respond to negative button press
        }
        .setPositiveButton("exit") { dialog, which ->
            // Respond to positive button press
            positiveAction()
        }
        .show()
}


fun convertStringToDate(date: String, pattern: String = "dd/MM/yyyy"): Date? {

    val format = SimpleDateFormat(pattern, Locale.UK)
    format.timeZone = TimeZone.getTimeZone("UTC")

    return try {
        val parse: Date? = format.parse(date)
        parse
    } catch (e: Exception) {
        null
    }
}

fun convertUTCToString(date: Date?, pattern: String = "dd/MM/yyyy"): String? {
    if (date == null) return null
    return try {
        val format = SimpleDateFormat(pattern, Locale.UK)
        format.timeZone = TimeZone.getTimeZone("UTC")
        format.format(date)
    } catch (e: Exception) {
        null
    }
}

fun convertUTCToString(millis: Long?, pattern: String = "dd/MM/yyyy"): String? {
    if (millis == null) return null
    return try {
        val format = SimpleDateFormat(pattern, Locale.UK)
        format.timeZone = TimeZone.getTimeZone("UTC")
        format.format(millis)
    } catch (e: Exception) {
        null
    }
}

fun closeKeyboard(activity: Activity) {
    (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
        .hideSoftInputFromWindow(activity.currentFocus?.windowToken, 0)
}

/**
 * Show an error snackBar
 * @param view
 * @param message to display inside the snackBar
 */
fun snackBarError(view: View?, message: String) {
    view?.let {
        Snackbar.make(
            it, message, Snackbar.LENGTH_LONG
        )
            .show()
    }
}

/**
 * Show a normal snackBar
 * @param view
 * @param message to display inside the snackBar
 */
fun snackBar(view: View?, message: String) {
    view?.let {
        Snackbar.make(
            it, message, Snackbar.LENGTH_LONG
        )
            .show()
    }
}

fun coordinatesToAddress(c: Context, lat: Double?, long: Double?): String? {
    //In this way I transform coordinates into address String
    if (lat == null || long == null) return null

    return try {
        val geocoder = Geocoder(c, Locale.getDefault())
        val address = geocoder.getFromLocation(lat, long, 1)
        address[0].getAddressLine(0)
    } catch (e: Exception) {
        null
    }
}


fun addressToCoordinates(context: Context, address: String?): Address? {

    if (address.isNullOrEmpty()) return null

    return try {
        //In this way I transform the address String into coordinates
        val coder = Geocoder(context)
        coder.getFromLocationName(address, 5)[0]
    } catch (e: Exception) {
        null
    }
}

fun convertAddressToGeoPoint(context: Context, address: String?): GeoPoint? {
    val coordinate = addressToCoordinates(context, address)

    return if (coordinate != null) {
        GeoPoint(coordinate.latitude, coordinate.longitude)
    } else {
        null
    }

}

fun formatSimpleAddress(c: Context, lat: Double?, long: Double?, completeAdd: Boolean): String {
    //In this way I transform coordinates into address String
    if (lat == null || long == null) return "Unnamed road"

    return try {
        val geocoder = Geocoder(c, Locale.getDefault())
        val address = geocoder.getFromLocation(lat, long, 1)

        val streetName = address[0].thoroughfare ?: address[0].featureName

        val number = try {
            address[0].subThoroughfare.toInt()
        } catch (e: Exception) {
            null
        }

        val locality = address[0].locality ?: ""

        val cap = address[0].postalCode ?: ""


        var addressFormatted = when {

            completeAdd && number != null -> "$streetName $number, $locality $cap"
            completeAdd && number == null -> "$streetName, $locality $cap"
            !completeAdd && number != null -> "$streetName $number, $locality "
            !completeAdd && number == null -> "$streetName, $locality"
            else -> "$streetName, $locality"
        }

        if (!completeAdd && addressFormatted.length > 35) {
            if (number == null) {
                var maybe = "$streetName $locality"
                if (maybe.length > 35) {
                    maybe = streetName
                }
                addressFormatted = maybe
            } else {
                var maybe = "$streetName $number"
                if (maybe.length > 35) {
                    maybe = streetName
                }
                addressFormatted = maybe
            }
        }

        addressFormatted
    } catch (e: Exception) {
        "Unnamed road"
    }
}


fun loadImage(imageView: ImageView, url: String?, placeholder: Int = R.drawable.avatar) {
    if (url.isNullOrEmpty()) {
        Log.e("UTILS: ", "loadImage profile url is empty or null")
        imageView.load(placeholder) {
            placeholder(placeholder)
            precision(coil.size.Precision.EXACT)
            scale(Scale.FILL)
            transformations(CircleCropTransformation())
        }
    }
    imageView.load(url) {
        placeholder(placeholder)
        precision(coil.size.Precision.EXACT)
        scale(Scale.FILL)
        transformations(CircleCropTransformation())
    }
}

fun loadImage(imageView: ImageView, path: File, placeholder: Int = R.drawable.avatar) {
    imageView.load(path) {
        placeholder(placeholder)
        precision(coil.size.Precision.EXACT)
        scale(Scale.FILL)
        transformations(CircleCropTransformation())
    }
}

fun getAge(date: Date): Int {
    // create a calendar
    // create a calendar
    val cal = Calendar.getInstance()
    cal.time = date //use java.util.Date object as argument

    // get the value of all the calendar date fields.
    // get the value of all the calendar date fields.
    val year = cal[Calendar.YEAR]
    val month = cal[Calendar.MONTH]
    val day = cal[Calendar.DATE]

    val dob = Calendar.getInstance()
    val today = Calendar.getInstance()
    dob[year, month] = day
    var age = today[Calendar.YEAR] - dob[Calendar.YEAR]
    if (today[Calendar.DAY_OF_YEAR] < dob[Calendar.DAY_OF_YEAR]) {
        age--
    }
    return age
}

// Use EXIF to determine if the image must be rotated
fun rotateImageIfRequired(context: Context, bitmap: Bitmap, path: String): Bitmap {
    val ei = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        ExifInterface(context.contentResolver.openInputStream(Uri.parse(path))!!)
    } else {
        ExifInterface(Uri.parse(path).path!!)
    }
    return when (ei.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL
    )) {
        ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90)
        ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180)
        ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270)
        else -> bitmap
    }
}

// Rotate bitmap
fun rotateImage(bitmap: Bitmap, angle: Int): Bitmap {
    val matrix = Matrix().apply {
        postRotate(angle.toFloat())
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

fun findDepartureAndArrivalStop(
    stops: List<Stop>,
    depFilter: GeoPoint?,
    arrFilter: GeoPoint?
): List<Stop> {

    val departure: Stop?
    val arrival: Stop?

    // If we don't pass any filter, the departure is the first stop and the arrival the last
    if (depFilter == null || arrFilter == null) {
        departure = stops[0]
        arrival = stops[stops.size - 1]
    } else {
        // Otherwise we find the stop next to the location inside the filters
        departure = stops[stopsNext(stops, depFilter)]
        arrival = stops[stopsNext(stops, arrFilter)]
    }

    return listOf(departure, arrival)
}

fun dateIsPassed(tripDate: Date): Boolean {

    //Pass the tripDate to UTC
    val cal = Calendar.getInstance()
    cal.time = tripDate
    cal.add(Calendar.HOUR, -2)
    val twoHourBack = cal.time

    return twoHourBack.before(Date())
}

fun deleteBookingDialog(context: Context, activity: Activity, positiveAction: () -> Unit) {
    closeKeyboard(activity)
    MaterialAlertDialogBuilder(context)
        .setTitle("Do you want to remove the booking?")
        .setMessage("The booking request will be removed, do you want to proceed anyway?")
        /*.setNeutralButton(resources.getString(R.string.cancel)) { dialog, which ->
         // Respond to neutral button press
        }*/
        .setNegativeButton("cancel") { dialog, which ->
            // Respond to negative button press
        }
        .setPositiveButton("Confirm") { dialog, which ->
            // Respond to positive button press
            positiveAction()
        }
        .show()
}

fun modifyTrip(context: Context, activity: Activity, positiveAction: () -> Unit) {
    closeKeyboard(activity)
    MaterialAlertDialogBuilder(context)
        .setTitle("Do you want to modify the trip?")
        .setMessage("All the booked passenger will be removed if you modify the trip")
        /*.setNeutralButton(resources.getString(R.string.cancel)) { dialog, which ->
         // Respond to neutral button press
        }*/
        .setNegativeButton("cancel") { dialog, which ->
            // Respond to negative button press
            (activity as MainActivity).progressBarVisibility(false)
        }
        .setPositiveButton("Confirm") { dialog, which ->
            // Respond to positive button press
            positiveAction()
        }
        .show()
}
