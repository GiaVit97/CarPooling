package it.polito.mad.carpooling_09.ui.yourTrips

import android.content.Context
import android.graphics.Color
import android.graphics.LightingColorFilter
import android.os.AsyncTask
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import it.polito.mad.carpooling_09.R
import it.polito.mad.carpooling_09.databinding.FragmentMapRouteBinding
import it.polito.mad.carpooling_09.utils.coordinatesToAddress
import it.polito.mad.carpooling_09.viewmodel.TripDetailsViewModel
import it.polito.mad.carpooling_09.viewmodel.TripDetailsViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.bonuspack.routing.*
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import java.util.*
import kotlin.collections.ArrayList


class MapRouteFragment : Fragment() {

    private val args: MapRouteFragmentArgs by navArgs()

    private var _binding: FragmentMapRouteBinding? = null
    private val binding get() = _binding!!

    private var tripID: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            tripID = it.getString(TRIP_ID)
        }
    }

    private val vm: TripDetailsViewModel by viewModels {
        TripDetailsViewModelFactory(
            tripID ?: args.tripID!!
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapRouteBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))

        binding.map.setTileSource(TileSourceFactory.MAPNIK)

        val rotationGestureOverlay = RotationGestureOverlay(binding.map)
        rotationGestureOverlay.isEnabled
        binding.map.setMultiTouchControls(true)
        binding.map.overlays.add(rotationGestureOverlay)
        binding.map.controller.setZoom(7.5)

        val roadManager = OSRMRoadManager(context, "")
        val waypoints = ArrayList<GeoPoint>()

        val icon = ResourcesCompat.getDrawable(resources, R.drawable.ic_circle, null)
        icon?.colorFilter = LightingColorFilter(Color.BLACK, Color.BLACK)

        vm.getTrip().observe(viewLifecycleOwner) { trip ->
            if (trip != null) {
                for (stop in trip.stops) {
                    if (stop.location != null) {
                        val gp = GeoPoint(stop.location!!.latitude, stop.location!!.longitude)
                        binding.map.controller.setCenter(gp)
                        waypoints.add(gp)

                        val marker = Marker(binding.map)
                        marker.title = context?.let {
                            coordinatesToAddress(
                                it,
                                stop.location!!.latitude,
                                stop.location!!.longitude
                            )
                        }
                        marker.icon = icon
                        marker.position =
                            GeoPoint(stop.location!!.latitude, stop.location!!.longitude)
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)

                        binding.map.overlays.add(marker)
                    }
                }
                if (waypoints.size > 0) {
                    try {
                        var roadOverlay: Polyline? = null
                        lifecycleScope.executeAsyncTask(
                            onPreExecute = {},
                            doInBackground = {
                                val road = roadManager.getRoad(waypoints)
                                roadOverlay = RoadManager.buildRoadOverlay(road)
                            }, onPostExecute = {
                                binding.map.overlays.add(roadOverlay)
                                binding.map.invalidate()
                            })
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during the route's loading")
                    }
                } else {
                    binding.map.controller.setCenter(GeoPoint(45.0735, 7.6757))
                }
            }
        }

    }

    companion object {
        private const val TAG = "MapRouteFragment"
        private const val TRIP_ID = "tripID"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param tripID id of the trip we are watching.
         * @return A new instance of fragment MapRouteFragment.
         */
        @JvmStatic
        fun newInstance(tripID: String) =
            MapRouteFragment().apply {
                arguments = Bundle().apply {
                    putString(TRIP_ID, tripID)
                }
            }
    }

    private fun <R> CoroutineScope.executeAsyncTask(
        onPreExecute: () -> Unit,
        doInBackground: () -> R,
        onPostExecute: (R) -> Unit
    ) = launch {
        onPreExecute()
        val result =
            withContext(Dispatchers.IO) { // runs in background thread without blocking the Main Thread
                doInBackground()
            }
        onPostExecute(result)
    }

}