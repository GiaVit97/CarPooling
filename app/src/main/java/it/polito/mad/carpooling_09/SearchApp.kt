package it.polito.mad.carpooling_09

import android.app.Application
import com.mapbox.search.MapboxSearchSdk
import com.mapbox.search.location.DefaultLocationProvider

class SearchApp : Application() {

    override fun onCreate() {
        super.onCreate()
        MapboxSearchSdk.initialize(
            this,
            getString(R.string.mapbox_access_token),
            DefaultLocationProvider(this)
        )
    }
}