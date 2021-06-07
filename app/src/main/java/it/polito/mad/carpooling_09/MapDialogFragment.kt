package it.polito.mad.carpooling_09

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mapbox.search.record.HistoryRecord
import com.mapbox.search.result.SearchResult
import com.mapbox.search.result.SearchSuggestion
import com.mapbox.search.ui.view.SearchResultsView
import it.polito.mad.carpooling_09.databinding.DialogMapBinding
import it.polito.mad.carpooling_09.utils.closeKeyboard
import it.polito.mad.carpooling_09.utils.coordinatesToAddress
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MapDialogFragment : DialogFragment() {

    companion object {

        private const val PERMISSION_REQUEST_LOCATION = 451

        const val GEO_POINT: String = "GEO_POINT"
    }

    private val args: MapDialogFragmentArgs by navArgs()

    private var _binding: DialogMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var savedStateHandle: SavedStateHandle

    private lateinit var searchView: SearchView
    private lateinit var startMarker: Marker


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogMapBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))

        savedStateHandle = findNavController().previousBackStackEntry!!.savedStateHandle

        binding.map.setTileSource(TileSourceFactory.MAPNIK)
        startMarker = Marker(binding.map, context)

        calculateHeight(binding.map)

        val mapController = binding.map.controller
        mapController.setZoom(9.5)
        val startPoint = if (!args.latitude.isNullOrEmpty()) {
            GeoPoint(args.latitude.toString().toDouble(), args.longitude.toString().toDouble())
        } else {
            GeoPoint(45.0735, 7.6757)
        }

        mapController.setCenter(startPoint)

        if (savedInstanceState != null && savedInstanceState.getDouble("latitude") != 0.0) {
            val savedPoint = GeoPoint(
                savedInstanceState.getDouble("latitude"),
                savedInstanceState.getDouble("longitude")
            )
            startMarker.position = savedPoint
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            startMarker.title =
                context?.let { coordinatesToAddress(it, savedPoint.latitude, savedPoint.longitude) }
            binding.map.overlays.add(startMarker)
            mapController.setCenter(savedPoint)
            mapController.setZoom(16.5)

        } else if (!args.latitude.isNullOrEmpty()) {
            startMarker.position = startPoint
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            // startMarker.icon = resources.getDrawable(R.drawable.avatar)
            startMarker.title =
                context?.let { coordinatesToAddress(it, startPoint.latitude, startPoint.longitude) }
            binding.map.overlays.add(startMarker)
            mapController.setZoom(16.5)
        }


        binding.map.overlays.add(object : Overlay() {
            override fun onSingleTapConfirmed(
                e: MotionEvent,
                mapView: MapView
            ): Boolean {


                val projection = mapView.projection
                val geoPoint = projection.fromPixels(
                    e.x.toInt(),
                    e.y.toInt()
                )

                startMarker.position = geoPoint as GeoPoint
                startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                startMarker.title =
                    context?.let { coordinatesToAddress(it, geoPoint.latitude, geoPoint.longitude) }
                binding.map.overlays.add(startMarker)


                return true
            }
        })


        val rotationGestureOverlay = RotationGestureOverlay(binding.map)
        rotationGestureOverlay.isEnabled
        binding.map.setMultiTouchControls(true)
        binding.map.overlays.add(rotationGestureOverlay)

        binding.centerMap.setOnClickListener {
            requestAccessToMyPosition()
        }


        binding.searchView.addSearchListener(object : SearchResultsView.SearchListener {
            override fun onSearchResult(searchResult: SearchResult) {
                binding.positionLabel.setText(searchResult.name)
                startMarker.position = GeoPoint(
                    searchResult.coordinate!!.latitude(),
                    searchResult.coordinate!!.longitude()
                )
                startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                startMarker.title =
                    coordinatesToAddress(
                        requireContext(), searchResult.coordinate!!.latitude(),
                        searchResult.coordinate!!.longitude()
                    )

                closeKeyboard(requireActivity())

                binding.map.overlays.add(startMarker)
                mapController.setCenter(startMarker.position)

                //We click on a result, so we show map again
                binding.searchView.visibility = View.GONE
                binding.centerMap.visibility = View.VISIBLE
                binding.map.visibility = View.VISIBLE

                calculateHeight(binding.map)

            }

            override fun onHistoryItemClicked(historyRecord: HistoryRecord) {
                if (::searchView.isInitialized) {
                    searchView.setQuery(historyRecord.name, true)
                }
            }

            override fun onPopulateQueryClicked(suggestion: SearchSuggestion) {
                if (::searchView.isInitialized) {
                    searchView.setQuery(suggestion.name, true)
                }
            }
        })

        binding.positionLabel.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (binding.searchView.visibility == View.GONE) {
                    binding.searchView.visibility = View.VISIBLE
                    binding.map.visibility = View.GONE
                    binding.centerMap.visibility = View.GONE

                    calculateHeight(binding.searchView)

                }
                binding.searchView.search(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {
                if (s.toString().isEmpty()) {
                    binding.searchView.visibility = View.GONE
                    binding.centerMap.visibility = View.VISIBLE
                    binding.map.visibility = View.VISIBLE

                    calculateHeight(binding.map)
                }
            }

        })

        binding.closeButton.setOnClickListener {
            dismiss()
        }

        binding.saveButton.setOnClickListener {
            savedStateHandle.set(GEO_POINT, startMarker.position)
            findNavController().popBackStack()
        }


    }

    private fun requestAccessToMyPosition() {
        if (requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)) {

            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Permission is already available, it start camera preview
                loadMyPositions()

            } else {
                // Request the permission. The result will be received in onRequestPermissionResult().
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSION_REQUEST_LOCATION
                )
            }

        } else {
            Toast.makeText(
                requireContext(),
                "We cannot detect your position, sorry",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun loadMyPositions() {


        val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), binding.map)
        locationOverlay.enableMyLocation()
        locationOverlay.enableFollowLocation()
        binding.map.overlays.add(locationOverlay)

        binding.map.controller.setZoom(16.5)
        calculateHeight(binding.map)


        locationOverlay.runOnFirstFix {

            if (locationOverlay.myLocation != null) {
                requireActivity().runOnUiThread {
                    binding.map.controller.setCenter(locationOverlay.myLocation)
                    startMarker.position =
                        GeoPoint(
                            locationOverlay.myLocation.latitude,
                            locationOverlay.myLocation.longitude
                        )
                    startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    startMarker.title = coordinatesToAddress(
                        requireContext(),
                        locationOverlay.myLocation.latitude,
                        locationOverlay.myLocation.longitude
                    )
                    binding.map.overlays.add(startMarker)
                }
            }
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_LOCATION) {
            // Request for camera permission.
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission has been granted. Start camera preview Activity.
                loadMyPositions()

            } else {
                // Permission request was denied.
                Toast.makeText(
                    requireActivity(),
                    R.string.permission_denied,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putDouble("longitude", startMarker.position.longitude)
        outState.putDouble("latitude", startMarker.position.latitude)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onPause() {
        super.onPause()
        binding.map.onPause()
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        binding.searchView.visibility = View.GONE
        binding.centerMap.visibility = View.VISIBLE
        binding.map.visibility = View.VISIBLE
        binding.map.onResume()

        calculateHeight(binding.map)
    }

    private fun calculateHeight(view: View) {
        val layout = view.layoutParams
        layout.height =
            ((Resources.getSystem().displayMetrics.heightPixels - binding.buttons.height - binding.position.height) * 0.6).toInt()
        view.layoutParams = layout
    }
}