package it.polito.mad.carpooling_09.viewmodel

import androidx.lifecycle.*
import it.polito.mad.carpooling_09.data.Trip
import it.polito.mad.carpooling_09.data.TripFilter
import it.polito.mad.carpooling_09.repository.TripRepository
import java.util.*

class OthersTripViewModel : ViewModel() {
    companion object {
        private const val TAG = "OTHERS_TRIP_VIEW_MODEL"
    }

    private var tripRepository = TripRepository()

    // Live Data with the selected filters
    private val filter: MutableLiveData<TripFilter> = MutableLiveData<TripFilter>(TripFilter())

    //https://stackoverflow.com/questions/53994960/android-filter-livedata-list-based-on-selected-item-change-in-viewmodel
    private val trips: MutableLiveData<List<Trip>> =
        Transformations.switchMap(filter) { filter -> tripRepository.getOthersTrips(filter) } as MutableLiveData<List<Trip>>



    fun getTrips(): LiveData<List<Trip>> {
        return trips
    }

    fun getTripFilter(): MutableLiveData<TripFilter> {
        return filter
    }

    fun setTripFilter(tripFilter: TripFilter) {
        filter.value = filter.value?.apply {
            this.departureLocation = tripFilter.departureLocation
            this.arrivalLocation = tripFilter.arrivalLocation
            this.departureDate = tripFilter.departureDate
            this.price = tripFilter.price
            this.smoke = tripFilter.smoke
            this.animals = tripFilter.animals
            this.music = tripFilter.music
        } ?: tripFilter
    }




    /*fun listenTrips(tripFilter: TripFilter? = null) {

        if (tripFilter != null) {
            TripFilter.departureLocation = tripFilter.departureLocation.toString()
            TripFilter.arrivalLocation = tripFilter.arrivalLocation.toString()
        }

        trips.value = tripRepository.getOthersTrips(tripFilter).value


        /*return tripRepository.getOthersTrips(tripFilter).addSnapshotListener { values, error ->
            if (error != null) {
                Log.e(TAG, "Error on 'getOthersTrips': $error")
                throw error
            }

            if (values != null) {
                val tripsList = ArrayList<Trip>()
                for (doc in values) {
                    if (Firebase.auth.currentUser?.uid != doc[FIELD_USERID])
                        if (tripFilter?.price == null || tripFilter.price == 0 || (tripFilter.price!! > 0 && doc[FIELD_PRICE].toString()
                                .toDouble() > tripFilter.price!!.toDouble())
                        ) {
                            try {
                                tripsList.add(doc.toObject(Trip::class.java).apply {
                                    id = doc.id
                                })
                            }
                            catch (e: Exception){
                                Log.e(TAG, doc.id)
                            }
                        }
                }
                //TODO: Change place for this function, this is not the optimal place
                val tripsWithSeats = ArrayList<Trip>()
                for(trip in tripsList) {
                    val availableSeats = isThereAvailableSeats(trip.stops)
                    if(availableSeats)
                        tripsWithSeats.add(trip)
                }

                if (tripFilter?.departureLocation != null && tripFilter?.arrivalLocation != null) {
                    val tripsFilteredList = ArrayList<Trip>()
                    for (trip in tripsWithSeats) {

                        var departure = false
                        var arrival = false

                        //TODO
                        /*for (stop in trip.stops) {
                            if (stop.name == tripFilter?.departureLocation)
                                departure = true
                            if (stop.name == tripFilter?.arrivalLocation && departure)
                                arrival = true
                        }*/
                        if (departure && arrival)
                            tripsFilteredList.add(trip)
                    }
                    trips.value = tripsFilteredList
                } else {
                    trips.value = tripsWithSeats
                }


            }
        }
    }*/
    }*/
}