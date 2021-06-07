package it.polito.mad.carpooling_09.utils

import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.ktx.Firebase
import it.polito.mad.carpooling_09.data.Stop
import it.polito.mad.carpooling_09.data.Trip
import it.polito.mad.carpooling_09.data.TripFilter


fun isThereAvailableSeats(stopList: List<Stop>, tripFilter: TripFilter?): Boolean {

    val filteredListStop = ArrayList<Stop>()

    if (tripFilter?.departureLocation == null && tripFilter?.arrivalLocation == null)
        for (i in 1 until stopList.size)
            filteredListStop.add(stopList[i])
    else {
        // I have to filter the list of stops to pass to minAvailableSeats function
        val initialPosition = stopsNext(stopList, tripFilter.departureLocation!!)
        val finalPosition = stopsNext(stopList, tripFilter.arrivalLocation!!)

        for (i in initialPosition + 1..finalPosition)
            filteredListStop.add(stopList[i])
    }

    for (stop in filteredListStop) {
        if (stop.available_seats == 0) {
            return false
        }
    }
    return true
}

fun minAvailableSeats(trip: Trip, checkFilter: Boolean, tripFilter: TripFilter?): Int {

    val filteredListStop = ArrayList<Stop>()

    if (trip.userID == Firebase.auth.currentUser?.uid || !checkFilter || (tripFilter?.departureLocation == null && tripFilter?.arrivalLocation == null))
    // User want to book the trip entirely (from departure to arrival)

        // Go through the stop excluded the first one (in the first one the seats are always 0)
        trip.stops.drop(1).forEach { stop ->
            filteredListStop.add(stop)
        }

    else {
        // User want to book only some intermediate stops

        // I need to find if there is one place free in all the intermediate stops he want to stay
        val initialPosition = stopsNext(trip.stops, tripFilter.departureLocation!!)
        val finalPosition = stopsNext(trip.stops, tripFilter.arrivalLocation!!)

        for (i in initialPosition + 1..finalPosition)
            filteredListStop.add(trip.stops[i])
    }

    var availableSeats = 10
    for (stop in filteredListStop) {
        if (stop.available_seats!! < availableSeats)
            availableSeats = stop.available_seats!!
    }

    return availableSeats
}


fun stopsNext(stops: List<Stop>, location: GeoPoint): Int {

    val docLocation = GeoLocation(location.latitude, location.longitude)

    var min = 1000000000.0
    var ind = 0
    stops.forEachIndexed { index, stop ->

        val thisStop = GeoLocation(stop.location!!.latitude, stop.location!!.longitude)
        val distance = GeoFireUtils.getDistanceBetween(thisStop, docLocation)

        if (distance < min) {
            min = distance
            ind = index
        }
    }
    return ind
}


fun listSelectedTrip(stopList: List<Stop>, bookStops: List<Stop>): ArrayList<Stop> {
    val filteredListStop = ArrayList<Stop>()
    var initialPosition = 0
    var finalPosition = 0
    for (i in stopList.indices) {
        if (bookStops[0].name == stopList[i].name)
            initialPosition = i
        if (bookStops[bookStops.size - 1].name == stopList[i].name)
            finalPosition = i
    }
    for (i in initialPosition + 1..finalPosition)
        filteredListStop.add(stopList[i])

    return filteredListStop
}