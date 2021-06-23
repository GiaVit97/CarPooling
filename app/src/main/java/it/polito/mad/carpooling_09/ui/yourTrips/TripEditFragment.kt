package it.polito.mad.carpooling_09.ui.yourTrips

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import androidx.preference.PreferenceManager
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import it.polito.mad.carpooling_09.MainActivity
import it.polito.mad.carpooling_09.MapDialogFragment
import it.polito.mad.carpooling_09.R
import it.polito.mad.carpooling_09.adapter.StopEditAdapter
import it.polito.mad.carpooling_09.data.*
import it.polito.mad.carpooling_09.databinding.FragmentTripEditBinding
import it.polito.mad.carpooling_09.ui.profile.EditProfileFragmentDirections
import it.polito.mad.carpooling_09.utils.*
import it.polito.mad.carpooling_09.viewmodel.OthersTripViewModel
import it.polito.mad.carpooling_09.viewmodel.TripEditViewModel
import it.polito.mad.carpooling_09.viewmodel.TripEditViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*


class TripEditFragment : Fragment(R.layout.fragment_trip_edit) {

    companion object {
        private const val TAG = "TripEditFragment"

        const val PERMISSION_REQUEST_CAMERA = 101
        const val PERMISSION_REQUEST_EXTERNAL_STORAGE = 102

        const val REQUEST_IMAGE_CAPTURE = 1
        const val REQUEST_EXTERNAL_STORAGE = 2

        private var stopList: MutableList<Stop> = mutableListOf()
    }

    // Binding
    private var _binding: FragmentTripEditBinding? = null
    private val binding get() = _binding!!

    // Reference to the menu
    private var currentMenu: Menu? = null

    //Safe Args
    private val args: TripEditFragmentArgs by navArgs()

    //ViewModel
    private val vm: TripEditViewModel by viewModels { TripEditViewModelFactory(args.tripID ?: " ") }
    private val othervm: OthersTripViewModel by activityViewModels()

    // Thanks to this we understand if the car image changes or not
    private var isCarImageChanged = false
    private var tempPath = File("")

    private var imageURL: String? = null

    // this is the StopEditAdapter
    private val adapter = StopEditAdapter(stopList,
        onDelete = {
            stopList.remove(it)
            update()
        }, openMap = { location, pos ->
            updatePosition(location, pos, null)
        }, openTimePicker = { tv, pos ->
            updateTimePicker(tv, pos)
        }
    )

    private var isBlocked: Boolean? = null
    private var minAvailableSeats: Int? = null
    private var availableSeatsDB: Int? = null
    private var lastStopSeats: Int? = null

    private var oldTrip: Trip? = null

    //Options array for number seats, smoke, animal and music preferences
    private lateinit var seatsOptions: Array<String>
    private lateinit var preferencesOptions: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        // In this way we can change the title of the fragment
        if (args.tripID.isNullOrEmpty()) {
            (activity as MainActivity).supportActionBar?.title = "Insert new trip"
        } else {
            (activity as MainActivity).supportActionBar?.title = "Edit your trip"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTripEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        seatsOptions = resources.getStringArray(R.array.seats_array)
        val seatsAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, seatsOptions)
        binding.numberSeats.setAdapter(seatsAdapter)
        binding.numberSeats.threshold = 1

        preferencesOptions = resources.getStringArray(R.array.preferences_array)
        val preferencesAdapter =
            ArrayAdapter(requireContext(), R.layout.dropdown_item, preferencesOptions)
        binding.smoke.setAdapter(preferencesAdapter)
        binding.smoke.threshold = 1
        binding.animal.setAdapter(preferencesAdapter)
        binding.animal.threshold = 1
        binding.music.setAdapter(preferencesAdapter)
        binding.music.threshold = 1
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        Configuration.getInstance()
            .load(context, PreferenceManager.getDefaultSharedPreferences(context))

        binding.stopsView.layoutManager = LinearLayoutManager(requireContext())
        binding.stopsView.adapter = adapter

        seatsOptions = resources.getStringArray(R.array.seats_array)
        val seatsAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, seatsOptions)
        preferencesOptions = resources.getStringArray(R.array.preferences_array)
        val preferencesAdapter =
            ArrayAdapter(requireContext(), R.layout.dropdown_item, preferencesOptions)

        binding.estimatedTripDuration.text = getString(R.string.estimated_duration_text)
        //Take the path for the temporary profile image in cache
        if (savedInstanceState != null) {
            isCarImageChanged = savedInstanceState.getBoolean("isCarImageChanged")
            tempPath =
                File(requireContext().cacheDir, savedInstanceState.getString("filePath") ?: "")
        } else if (args.tripID.isNullOrEmpty()) {
            // In this case I have a new trip and savedInstanceState is empty
            binding.numberSeats.setText(seatsAdapter.getItem(0).toString(), false)
            binding.smoke.setText(preferencesAdapter.getItem(0).toString(), false)
            binding.animal.setText(preferencesAdapter.getItem(0).toString(), false)
            binding.music.setText(preferencesAdapter.getItem(0).toString(), false)
        }

        //Takes trips information from ViewModel and initialize the view
        if (!args.tripID.isNullOrEmpty()) {
            vm.getTrip().observe(viewLifecycleOwner) { trip ->

                binding.departureDate.setText(convertUTCToString(trip.departureDate))

                // handle RecyclerView
                if (trip.stops.isNotEmpty()) {

                    binding.departureLocation.setText(trip.stops[0].name)
                    binding.arrivalLocation.setText(trip.stops[trip.stops.size - 1].name)

                    binding.departureTime.setText(convertUTCToString(trip.stops[0].time, "HH:mm"))

                    if (stopList.isEmpty()) {
                        for (i in 1 until trip.stops.size - 1) {

                            stopList.add(trip.stops[i])
                            adapter.notifyItemInserted(stopList.size - 1)
                        }
                    }
                    adapter.notifyDataSetChanged()
                }


                binding.estimatedTripDuration.text = "Estimated duration: ${trip.estimatedDuration}"
                binding.price.setText(DecimalFormat("0.00").format(trip.price).toString())

                when (trip.tripPreferences?.smoke) {
                    PreferencesType.NoPreferences -> binding.smoke.setText(
                        preferencesAdapter.getItem(
                            0
                        ).toString(), false
                    )
                    PreferencesType.Yes -> binding.smoke.setText(
                        preferencesAdapter.getItem(1).toString(), false
                    )
                    PreferencesType.No -> binding.smoke.setText(
                        preferencesAdapter.getItem(2).toString(), false
                    )
                }

                when (trip.tripPreferences?.animals) {
                    PreferencesType.NoPreferences -> binding.animal.setText(
                        preferencesAdapter.getItem(
                            0
                        ).toString(), false
                    )
                    PreferencesType.Yes -> binding.animal.setText(
                        preferencesAdapter.getItem(1).toString(), false
                    )
                    PreferencesType.No -> binding.animal.setText(
                        preferencesAdapter.getItem(2).toString(), false
                    )
                }

                when (trip.tripPreferences?.music) {
                    PreferencesType.NoPreferences -> binding.music.setText(
                        preferencesAdapter.getItem(
                            0
                        ).toString(), false
                    )
                    PreferencesType.Yes -> binding.music.setText(
                        preferencesAdapter.getItem(1).toString(), false
                    )
                    PreferencesType.No -> binding.music.setText(
                        preferencesAdapter.getItem(2).toString(), false
                    )
                }

                binding.carType.setText(trip.carInfo?.model)
                binding.carColor.setText(trip.carInfo?.color)


                binding.numberSeats.setText(
                    seatsAdapter.getItem(trip.availableSeats ?: 0).toString(), false
                )

                binding.additionalInfo.setText(trip.description)


                binding.managePassengersButton.setOnClickListener {
                    val action =
                        TripEditFragmentDirections.actionTripEditFragmentToManagePassengersFragment(
                            args.tripID!!
                        )
                    findNavController().navigate(action)
                }

                imageURL = trip.carInfo?.urlPhoto

                // Check if we have a temporary user profile image in cache to load instead of the DB one
                if (isCarImageChanged && tempPath.exists()) {
                    loadImage(binding.carImageView, tempPath, R.drawable.car)
                } else {
                    trip.carInfo?.urlPhoto?.let {
                        loadImage(binding.carImageView, it, R.drawable.car)
                    }
                }

                isBlocked = trip.blocked
                val minSeats = minAvailableSeats(trip, false, othervm.getTripFilter().value)
                minAvailableSeats = trip.availableSeats!! - minSeats
                availableSeatsDB = trip.availableSeats!!
                lastStopSeats = trip.stops[trip.stops.size - 1].available_seats!!

                oldTrip = trip
                oldTrip?.userID = null
                oldTrip?.stopCities = mutableMapOf()
                oldTrip?.blocked = null
                oldTrip?.id = null
                oldTrip?.estimatedDuration = null

            }
        }

        if (isCarImageChanged && tempPath.exists()) {
            loadImage(binding.carImageView, tempPath, R.drawable.car)
        }

        binding.addStop.setOnClickListener {
            insertStop()
        }

        if (args.tripID.isNullOrEmpty()) {
            binding.managePassengersButton.visibility = View.GONE
        } else {
            binding.managePassengersButton.visibility = View.VISIBLE
        }

        //TimePicker
        binding.departureTime.setOnClickListener {
            timePicker(binding.departureTime)
        }


        //DatePicker
        binding.departureDate.setOnClickListener {
            val date = if (!binding.departureDate.text.isNullOrEmpty()) {
                convertStringToDate(binding.departureDate.text.toString())
            } else Date()

            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select date")
                .setSelection(date?.time)
                .build()

            datePicker.show(parentFragmentManager, TAG)

            datePicker.addOnPositiveButtonClickListener {
                binding.departureDate.setText(convertUTCToString(datePicker.selection))
            }
        }

        binding.departureLocation.setOnClickListener {
            val oldCoordinates = if (binding.departureLocation.text.isNullOrEmpty()) {
                null
            } else {
                addressToCoordinates(
                    requireContext(), binding.departureLocation.text.toString()
                )?.let {
                    com.google.firebase.firestore.GeoPoint(it.latitude, it.longitude)
                }
            }
            updatePosition(oldCoordinates, null, "dep")
        }

        binding.arrivalLocation.setOnClickListener {
            val oldCoordinates = if (binding.arrivalLocation.text.isNullOrEmpty()) {
                null
            } else {
                addressToCoordinates(
                    requireContext(), binding.arrivalLocation.text.toString()
                )?.let {
                    com.google.firebase.firestore.GeoPoint(it.latitude, it.longitude)
                }
            }
            updatePosition(oldCoordinates, null, "arr")
        }

        //Set a floating context menu by clicking the camera icon
        //With this the menu opens even with single click and not only with a long press
        binding.imageButtonCar.setOnClickListener { requireActivity().openContextMenu(it) }
        registerForContextMenu(binding.imageButtonCar)

        validate()

    }

    fun timePicker(tv: TextView, isAdapter: Boolean = false, position: Int = -1) {
        val hours: Int?
        val minutes: Int?

        if (tv.text.toString().isNotEmpty()) {
            hours = tv.text.toString().split(":")[0].toInt()
            minutes = tv.text.toString().split(":")[1].toInt()
        } else {
            val cal = Calendar.getInstance()
            hours = SimpleDateFormat("HH", Locale.getDefault()).format(cal.time).toIntOrNull()
            minutes = SimpleDateFormat("mm", Locale.getDefault()).format(cal.time).toIntOrNull()
        }

        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setTitleText("Select Time")
            .setHour(hours ?: 0)
            .setMinute(minutes ?: 0)
            .build()


        timePicker.show(parentFragmentManager, TAG)

        timePicker.addOnPositiveButtonClickListener {

            val finalHour = DecimalFormat("00").format(timePicker.hour)
            val finalMinutes = DecimalFormat("00").format(timePicker.minute)
            tv.text = "$finalHour:$finalMinutes"
            if (isAdapter && stopList.size > 0) {
                stopList[position].time = convertStringToDate(tv.text.toString(), "HH:mm")
            }
        }

    }

    override fun onDestroyView() {
        closeKeyboard(requireActivity())
        super.onDestroyView()
        _binding = null
        stopList = mutableListOf()
    }

    override fun onPause() {
        closeKeyboard(requireActivity())
        super.onPause()
    }

    private fun insertStop() {

        var seats: Int? = null
        if (availableSeatsDB != -1 && availableSeatsDB != null)
            seats = availableSeatsDB

        val newStop = Stop(null, null, null, null, seats)
        stopList.add(newStop)
        adapter.notifyItemInserted(stopList.size)
    }

    private fun update() {
        adapter.notifyDataSetChanged()
    }

    private lateinit var observer: LifecycleEventObserver

    private fun updatePosition(
        location: com.google.firebase.firestore.GeoPoint?,
        position: Int?,
        field: String?
    ) {
        var action = TripEditFragmentDirections.actionGlobalMapDialog(null, null)

        if (location != null) {

            action = EditProfileFragmentDirections.actionGlobalMapDialog(
                location.latitude.toString(),
                location.longitude.toString()
            )
        }
        findNavController().navigate(action)

        val navBackStackEntry = findNavController().getBackStackEntry(R.id.tripEditFragment)

        if (::observer.isInitialized)
            navBackStackEntry.lifecycle.removeObserver(observer)
        // Create our observer and add it to the NavBackStackEntry's lifecycle
        observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME
                && navBackStackEntry.savedStateHandle.contains(MapDialogFragment.GEO_POINT)
            ) {
                val geoPoint = navBackStackEntry.savedStateHandle.get<GeoPoint>(
                    MapDialogFragment.GEO_POINT
                )
                if (geoPoint == null || (geoPoint.latitude == 0.0 && geoPoint.longitude == 0.0)) {
                    //Nothing is coming back
                } else if (position != null) {
                    stopList[position].location = com.google.firebase.firestore.GeoPoint(
                        geoPoint.latitude,
                        geoPoint.longitude
                    )
                    //TODO: Salvo la via oppure è meglio salvare la città?
                    stopList[position].name = context?.let {
                        coordinatesToAddress(
                            it,
                            geoPoint.latitude,
                            geoPoint.longitude
                        )
                    }
                    adapter.notifyItemChanged(position)
                } else if (field == "dep") {
                    binding.departureLocation.setText(context?.let {
                        coordinatesToAddress(
                            it,
                            geoPoint.latitude,
                            geoPoint.longitude
                        )
                    })
                } else if (field == "arr") {
                    binding.arrivalLocation.setText(context?.let {
                        coordinatesToAddress(
                            it,
                            geoPoint.latitude,
                            geoPoint.longitude
                        )
                    })
                }
            }
        }

        navBackStackEntry.lifecycle.addObserver(observer)

        // As addObserver() does not automatically remove the observer, we
        // call removeObserver() manually when the view lifecycle is destroyed
        viewLifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                navBackStackEntry.lifecycle.removeObserver(observer)
            }
        })
    }

    private fun updateTimePicker(tv: TextView, position: Int) {
        timePicker(tv, true, position)
    }

    //create menu with the save button
    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater)
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.save_delete_menu, menu)
        if (args.tripID.isNullOrEmpty()) {
            menu.findItem(R.id.block).isVisible = false
        }
    }

    //when you click save the application will take you to the trip detail
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                requireActivity().onBackPressedDispatcher.onBackPressed()
                stopList = mutableListOf()
                true
            }
            R.id.save -> {
                val updatedListStop = adapter.getUpdatedStopList()

                // Check if the field are empty or not
                if (!hasError() ||
                    TextUtils.isEmpty(binding.departureDate.text.toString()) ||
                    TextUtils.isEmpty(binding.departureLocation.text.toString()) ||
                    TextUtils.isEmpty(binding.departureTime.text.toString()) ||
                    TextUtils.isEmpty(binding.arrivalLocation.text.toString()) ||
                    TextUtils.isEmpty(binding.price.text.toString()) ||
                    TextUtils.isEmpty(binding.carColor.text.toString()) ||
                    TextUtils.isEmpty(binding.carType.text.toString())
                ) {
                    snackBarError(view, "Please fill all fields")
                    return true
                }

                if (minAvailableSeats != null && minAvailableSeats != -1)
                    if (binding.numberSeats.text.toString().toInt() < minAvailableSeats!!) {
                        snackBarError(
                            view,
                            "You can select at least $minAvailableSeats seats"
                        )

                        return true
                    }


                // I test with a for loop each field of the Stops Array
                for (stop in updatedListStop) {
                    if (stop.name.isNullOrEmpty()
                    ) {
                        snackBarError(view, "Please fill all fields")
                        // In this case don't add the trip
                        return true
                    }
                }
                (requireActivity() as MainActivity).progressBarVisibility(true)

                val prSmoke =
                    when {
                        binding.smoke.text.toString() == PreferencesType.NoPreferences.choice -> PreferencesType.NoPreferences
                        binding.smoke.text.toString() == PreferencesType.Yes.choice -> PreferencesType.Yes
                        else -> PreferencesType.No
                    }

                val prAnimal =
                    when {
                        binding.animal.text.toString() == PreferencesType.NoPreferences.choice -> PreferencesType.NoPreferences
                        binding.animal.text.toString() == PreferencesType.Yes.choice -> PreferencesType.Yes
                        else -> PreferencesType.No
                    }

                val prMusic =
                    when {
                        binding.music.text.toString() == PreferencesType.NoPreferences.choice -> PreferencesType.NoPreferences
                        binding.music.text.toString() == PreferencesType.Yes.choice -> PreferencesType.Yes
                        else -> PreferencesType.No
                    }


                val id: String?
                val isNewTrip: Boolean


                if (vm.getTripID().isEmpty() || vm.getTripID() == " ") {
                    id = null
                    isNewTrip = true
                } else {
                    id = vm.getTripID()
                    isNewTrip = false
                }


                val imagePath = if (isCarImageChanged) {
                    tempPath.toString()
                } else {
                    imageURL
                }

                val depDate = convertStringToDate(binding.departureDate.text.toString())


                val stopsWithoutTime = createStopsArray()
                var finalStopsList: List<Stop> = mutableListOf()

                lifecycleScope.executeAsyncTask(
                    onPreExecute = {},
                    doInBackground = {
                        finalStopsList = calculateDurationForFirestore(stopsWithoutTime)
                    }, onPostExecute = {

                        val nameOfCities = mutableMapOf<String, Boolean>()
                        for (stop in finalStopsList) {
                            nameOfCities[stop.geoHash!!] = true
                        }

                        val differenceDates =
                            finalStopsList[finalStopsList.size - 1].time?.time?.minus(
                                finalStopsList[0].time?.time!!
                            )


                        var estimatedDuration: String? = null
                        differenceDates?.let {
                            val hours = DecimalFormat("00").format(differenceDates / (3600 * 1000))
                            val minutes =
                                DecimalFormat("00").format((differenceDates - hours.toInt() * 3600 * 1000) / (60 * 1000))

                            estimatedDuration = "$hours:$minutes"
                        }

                        val newTrip = Trip(
                            id,
                            Firebase.auth.currentUser?.uid!!,
                            finalStopsList,
                            nameOfCities,
                            binding.price.text.toString().replace(",", ".").toDouble(),
                            CarInfo(
                                imagePath,
                                binding.carColor.text.toString(),
                                binding.carType.text.toString()
                            ),
                            binding.numberSeats.text.toString().toInt(),
                            binding.additionalInfo.text.toString(),
                            TripPreferences(prSmoke, prAnimal, prMusic),
                            estimatedDuration,
                            depDate,
                            isBlocked ?: false
                        )

                        if (isNewTrip) {

                            vm.addTrip(newTrip) { success, documentId, errorMessage ->
                                vm.setIsSaving(false)
                                (requireActivity() as MainActivity).progressBarVisibility(false)

                                if (success && documentId != null) {
                                    snackBar(view, "Trip was created successfully")

                                    //Delete the temporary image before close the activity

                                    tempPath.delete()
                                    isCarImageChanged = false
                                    stopList = mutableListOf()

                                    val action =
                                        TripEditFragmentDirections.actionTripEditFragmentToTripDetailsFragment(
                                            documentId, Firebase.auth.currentUser?.uid!!
                                        )

                                    findNavController().navigate(action)
                                } else {
                                    // There was an error
                                    Log.e(TAG, errorMessage.toString())
                                    snackBarError(
                                        view,
                                        "Error during the creation of the trip, try again please"
                                    )
                                }
                            }

                        } else {

                            modifyTrip(requireContext(), requireActivity()) {
                                vm.deleteAllBookedUsers(newTrip.id) { success, errorMessage ->

                                    if (success) {
                                        vm.updateTrip(newTrip) { success2, errorMessage2 ->


                                            vm.setIsSaving(false)
                                            (requireActivity() as MainActivity).progressBarVisibility(
                                                false
                                            )

                                            if (success2) {
                                                snackBar(view, "Trip was edited successfully")


                                                val action =
                                                    TripEditFragmentDirections.actionTripEditFragmentToTripDetailsFragment(
                                                        newTrip.id!!, newTrip.userID!!
                                                    )
                                                findNavController().navigate(action)

                                                //Delete the temporary image before close the activity

                                                tempPath.delete()
                                                isCarImageChanged = false
                                                stopList = mutableListOf()

                                            } else {
                                                Log.e(TAG, errorMessage2.toString())
                                                snackBarError(
                                                    view,
                                                    "Error during the update of the trip, try again please"
                                                )
                                            }
                                        }
                                    } else {
                                        Log.e(TAG, errorMessage.toString())
                                        snackBarError(
                                            view,
                                            "Error during the update of the trip, try again please"
                                        )
                                    }
                                }
                            }

                        }


                    }
                )
                true
            }
            R.id.block -> {

                vm.blockTrip(
                    args.tripID,
                    !(isBlocked ?: false),
                    oldTrip?.stops,
                    oldTrip?.departureDate
                ) { success, errorMessage ->
                    if (success) {
                        if (isBlocked == true)
                            snackBar(view, "Trip has been unblocked successfully")
                        else
                            snackBar(view, "Trip has been blocked successfully")
                        findNavController().navigate(TripEditFragmentDirections.actionTripEditFragmentToTripListFragment())
                    } else {
                        Log.e(TAG, errorMessage.toString())
                        snackBarError(view, "Error while blocking the trip, try again please")
                    }

                }
                true
            }
            else -> NavigationUI.onNavDestinationSelected(item, requireView().findNavController())
                    || super.onOptionsItemSelected(item)
        }
    }

    private fun hasError(): Boolean {
        return when {
            !binding.departureDateLayout.error.isNullOrEmpty() -> false
            !binding.priceLayout.error.isNullOrEmpty() -> false
            !binding.departureLocationLayout.error.isNullOrEmpty() -> false
            !binding.departureTimeLayout.error.isNullOrEmpty() -> false
            !binding.arrivalLocationLayout.error.isNullOrEmpty() -> false
            !binding.numberSeats.error.isNullOrEmpty() -> false
            !binding.additionalInfoLayout.error.isNullOrEmpty() -> false
            !binding.carTypeLayout.error.isNullOrEmpty() -> false
            !binding.carColorLayout.error.isNullOrEmpty() -> false
            else -> true
        }
    }

    private fun validate() {

        binding.departureDate.doOnTextChanged { text, _, _, _ ->
            val date = convertStringToDate(text.toString())
            date?.let {
                // today
                val today: Calendar = GregorianCalendar()
                // reset hour, minutes, seconds and millis
                today[Calendar.HOUR_OF_DAY] = 0
                today[Calendar.MINUTE] = 0
                today[Calendar.SECOND] = 0
                today[Calendar.MILLISECOND] = 0

                when {
                    it.before(today.time) -> binding.departureDateLayout.error =
                        "Date can't be in past"
                    else -> binding.departureDateLayout.error = null
                }
            }
        }

        binding.price.doOnTextChanged { text, _, _, _ ->
            if (text.isNullOrEmpty()) {
                binding.priceLayout.error = "Field required"
            } else {
                binding.priceLayout.error = null
            }
        }

        binding.departureLocation.doOnTextChanged { text, _, _, _ ->
            if (text.isNullOrEmpty()) {
                binding.departureLocationLayout.error = "Field required"
                calculateDuration()
            } else {
                binding.departureLocationLayout.error = null
                calculateDuration()
            }
        }

        binding.departureTime.doOnTextChanged { text, _, _, _ ->
            if (text.isNullOrEmpty()) {
                binding.departureTimeLayout.error = "Field required"
            } else {
                binding.departureTimeLayout.error = null
            }
        }

        binding.arrivalLocation.doOnTextChanged { text, _, _, _ ->
            if (text.isNullOrEmpty()) {
                binding.arrivalLocationLayout.error = "Field required"
                calculateDuration()
            } else {
                binding.arrivalLocationLayout.error = null
                calculateDuration()
            }
        }

        binding.numberSeats.setOnDismissListener {
            if (binding.numberSeats.text.toString().toInt() == 0) {
                binding.numberSeatsLayout.error = "You can't set 0 available seats"
            } else {
                binding.numberSeatsLayout.error = null
            }
        }

        binding.additionalInfo.doOnTextChanged { text, _, _, _ ->
            if (text!!.length > 140) {
                binding.additionalInfoLayout.error = "Too much character"
            } else {
                binding.additionalInfoLayout.error = null
            }
        }

        binding.carType.doOnTextChanged { text, _, _, _ ->
            if (text.isNullOrEmpty()) {
                binding.carTypeLayout.error = "Field required"
            }
            if (text!!.length > 10) {
                binding.carTypeLayout.error = "Too much character"
            } else if (text.isNotEmpty() && text.length <= 10) {
                binding.carTypeLayout.error = null
            }
        }

        binding.carColor.doOnTextChanged { text, _, _, _ ->
            if (text.isNullOrEmpty()) {
                binding.carColorLayout.error = "Field required"
            }
            if (text!!.length > 10) {
                binding.carColorLayout.error = "Too much character"
            } else if (text.isNotEmpty() && text.length <= 10) {
                binding.carColorLayout.error = null
            }
        }
    }

    private fun calculateDuration() {
        if (!binding.departureLocation.text.isNullOrEmpty() && !binding.arrivalLocation.text.isNullOrEmpty()) {
            val waypoints = ArrayList<GeoPoint>()
            addressToCoordinates(
                requireContext(),
                binding.departureLocation.text.toString()
            )?.let { departure ->
                val depGeopoint = GeoPoint(departure.latitude, departure.longitude)
                waypoints.add(depGeopoint)
            }
            addressToCoordinates(
                requireContext(),
                binding.arrivalLocation.text.toString()
            )?.let { arrival ->
                val arrGeoPoint = GeoPoint(arrival.latitude, arrival.longitude)
                waypoints.add(arrGeoPoint)
            }
            try {
                val roadManager = OSRMRoadManager(requireContext(), "")
                if (waypoints.size == 2) {

                    var totalSecs: Int? = 0
                    lifecycleScope.executeAsyncTask(
                        onPreExecute = {},
                        doInBackground = {
                            totalSecs = roadManager.getRoad(waypoints).mDuration.toInt()
                        }, onPostExecute = {
                            totalSecs?.let {
                                val hours = if (totalSecs!! / 3600 < 100)
                                    DecimalFormat("00").format(totalSecs!! / 3600)
                                else
                                    DecimalFormat("000").format(totalSecs!! / 3600)
                                val minutes =
                                    DecimalFormat("00").format((totalSecs!! - hours.toInt() * 3600) / 60)

                                binding.estimatedTripDuration.text = "Estimated duration: $hours:$minutes"
                            }
                        })
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during duration calculation")
            }

        }
    }

    private fun createStopsArray(): List<Stop> {
        val updatedListStop = adapter.getUpdatedStopList()
        val finalStopsList = mutableListOf<Stop>()

        val depHours = binding.departureTime.text.toString()
        val depDate = binding.departureDate.text.toString()

        val depTime = convertStringToDate("$depDate $depHours", "dd/MM/yyyy HH:mm")
        val depName = binding.departureLocation.text.toString()
        val depLocation = context?.let { addressToCoordinates(it, depName) }

        val depGeoPoint = if (depLocation != null) {
            com.google.firebase.firestore.GeoPoint(depLocation.latitude, depLocation.longitude)
        } else {
            null
        }
        val departureHash = depGeoPoint?.let {
            GeoFireUtils.getGeoHashForLocation(
                GeoLocation(
                    it.latitude,
                    it.longitude
                )
            )
        }
        finalStopsList.add(Stop(depName, depGeoPoint, depTime, departureHash, 0))

        // If the user change the number of available seats, we have to change it into the different stops obj
        val numberAvailableSeats = if (availableSeatsDB != null && availableSeatsDB != -1) {
            availableSeatsDB!! - binding.numberSeats.text.toString().toInt()
        } else {
            binding.numberSeats.text.toString().toInt()
        }

        for (stop in updatedListStop) {
            //If stop.available_seats is not null, we are editing the trip, so if we change
            // the number of available seats, we have to update this number into the stops
            val seats = if (stop.available_seats != null) {
                stop.available_seats!! - numberAvailableSeats
            } else {
                // Else we are creating a new trip, so the number of seats is equal to the number selected by the user
                numberAvailableSeats
            }
            val locationHash = stop.location?.let {
                GeoFireUtils.getGeoHashForLocation(
                    GeoLocation(
                        it.latitude,
                        it.longitude
                    )
                )
            }
            val stopHour = convertUTCToString(stop.time, "HH:mm")
            val stopDate = convertStringToDate("$depDate $stopHour", "dd/MM/yyyy HH:mm")
            val newStop = Stop(stop.name, stop.location, stopDate, locationHash, seats)
            finalStopsList.add(newStop)
        }

        val arrName = binding.arrivalLocation.text.toString()
        val arrLocation = context?.let { addressToCoordinates(it, arrName) }

        val arrGeoPoint = if (arrLocation != null) {
            com.google.firebase.firestore.GeoPoint(arrLocation.latitude, arrLocation.longitude)
        } else {
            null
        }
        val arrivalHash = arrGeoPoint?.let {
            GeoFireUtils.getGeoHashForLocation(
                GeoLocation(
                    it.latitude,
                    it.longitude
                )
            )
        }
        val seatsLastStop = if (lastStopSeats != null && lastStopSeats != -1) {
            lastStopSeats!! - numberAvailableSeats
        } else {
            binding.numberSeats.text.toString().toInt()
        }
        finalStopsList.add(
            Stop(
                arrName,
                arrGeoPoint,
                null,
                arrivalHash,
                seatsLastStop
            )
        )

        return finalStopsList
    }

    private fun calculateDurationForFirestore(stopList: List<Stop>): List<Stop> {
        stopList.drop(1).forEachIndexed { index, stop ->
            val waypoints = ArrayList<GeoPoint>()

            stopList[index].location?.let {
                val depGeopoint = GeoPoint(
                    stopList[index].location!!.latitude,
                    stopList[index].location!!.longitude
                )
                waypoints.add(depGeopoint)
            }
            stop.location?.let {
                val arrGeopoint = GeoPoint(
                    stop.location!!.latitude,
                    stop.location!!.longitude
                )
                waypoints.add(arrGeopoint)
            }

            try {
                val roadManager = OSRMRoadManager(requireContext(), "")
                if (waypoints.size == 2) {

                    val totalSecs = roadManager.getRoad(waypoints).mDuration.toInt()
                    val hours = DecimalFormat("00").format(totalSecs / 3600)
                    val minutes =
                        DecimalFormat("00").format((totalSecs - hours.toInt() * 3600) / 60)

                    // I take the time of the previous stop
                    val oldTime = Calendar.getInstance()
                    oldTime.time = stopList[index].time!!

                    // I add the estimated duration between stops to the time and save it in the actual stop
                    oldTime.add(Calendar.HOUR, hours.toInt())
                    oldTime.add(Calendar.MINUTE, minutes.toInt())

                    stop.time = oldTime.time
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during duration calculation")
            }
        }
        return stopList
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean("isCarImageChanged", isCarImageChanged)
        outState.putString("filePath", tempPath.name)

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {

                    val stopsList = createStopsArray().map { stop ->
                        stop.time = null
                        stop
                    }


                    val prSmoke =
                        when {
                            binding.smoke.text.toString() == PreferencesType.NoPreferences.choice -> PreferencesType.NoPreferences
                            binding.smoke.text.toString() == PreferencesType.Yes.choice -> PreferencesType.Yes
                            else -> PreferencesType.No
                        }

                    val prAnimal =
                        when {
                            binding.animal.text.toString() == PreferencesType.NoPreferences.choice -> PreferencesType.NoPreferences
                            binding.animal.text.toString() == PreferencesType.Yes.choice -> PreferencesType.Yes
                            else -> PreferencesType.No
                        }

                    val prMusic =
                        when {
                            binding.music.text.toString() == PreferencesType.NoPreferences.choice -> PreferencesType.NoPreferences
                            binding.music.text.toString() == PreferencesType.Yes.choice -> PreferencesType.Yes
                            else -> PreferencesType.No
                        }


                    val old = if (oldTrip != null) {
                        val withoutStops = oldTrip
                        val newStops = oldTrip!!.stops.map { stop ->
                            stop.time = null
                            stop
                        }
                        withoutStops?.stops = newStops
                        withoutStops

                    } else {
                        Trip(
                            null,
                            null,
                            mutableListOf(
                                Stop("", null, null, null, 0),
                                Stop("", null, null, null, 0)
                            ),
                            mutableMapOf(),
                            null,
                            CarInfo(null, "", ""),
                            0,
                            "",
                            TripPreferences(
                                PreferencesType.NoPreferences,
                                PreferencesType.NoPreferences,
                                PreferencesType.NoPreferences
                            ),
                            null,
                            null,
                            null
                        )
                    }

                    val depDate = convertStringToDate(binding.departureDate.text.toString())

                    val newTrip = Trip(
                        null,
                        null,
                        stopsList,
                        mutableMapOf(),
                        binding.price.text.toString().replace(",", ".").toDoubleOrNull(),
                        CarInfo(
                            null,
                            binding.carColor.text.toString(),
                            binding.carType.text.toString()
                        ),
                        binding.numberSeats.text.toString().toInt(),
                        binding.additionalInfo.text.toString(),
                        TripPreferences(prSmoke, prAnimal, prMusic),
                        null,
                        depDate,
                        null
                    )
                    if (isCarImageChanged || old != newTrip) {
                        //Show a dialog that confirm the exit without saving
                        backEditDialog(requireContext(), requireActivity()) {
                            tempPath.delete()
                            findNavController().navigateUp()
                        }
                    } else {
                        //closeKeyboard(requireActivity())
                        tempPath.delete()
                        findNavController().navigateUp()
                    }
                }

            })
    }

    //CAMERA
    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val inflater = requireActivity().menuInflater
        inflater.inflate(R.menu.content_menu, menu)

        menu.setHeaderTitle(R.string.choose_image_title)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

            R.id.gallery -> {

                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    // Permission is already available, it starts camera preview
                    useGallery()

                } else {
                    // Request the permission. The result will be received in onRequestPermissionResult().
                    requestPermissions(
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        PERMISSION_REQUEST_EXTERNAL_STORAGE
                    )
                }
                return true
            }

            R.id.camera -> {

                //Check if the device has a camera or not
                if (requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {

                    if (ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        // Permission is already available, it start camera preview
                        useCamera()

                    } else {
                        // Request the permission. The result will be received in onRequestPermissionResult().
                        requestPermissions(
                            arrayOf(Manifest.permission.CAMERA),
                            PERMISSION_REQUEST_CAMERA
                        )
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        R.string.camera_hardware_not_found,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return true
            }
            else -> super.onContextItemSelected(item)
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            // Request for camera permission.
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission has been granted. Start camera preview Activity.
                useCamera()

            } else {
                // Permission request was denied.
                Toast.makeText(
                    requireContext(),
                    R.string.permission_denied,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        if (requestCode == PERMISSION_REQUEST_EXTERNAL_STORAGE) {
            // Request for camera permission.
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission has been granted. Start camera preview Activity.
                useGallery()

            } else {
                // Permission request was denied.
                Toast.makeText(
                    requireContext(),
                    R.string.permission_denied,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    }


    private fun useCamera() {
        //Device has a camera and we create an Intent
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } catch (e: ActivityNotFoundException) {
            snackBarError(view, getString(R.string.generic_problem))
        }
    }

    private fun useGallery() {
        val intent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        startActivityForResult(intent, REQUEST_EXTERNAL_STORAGE)
    }

    private fun saveInCacheAndLoad(bm: Bitmap) {
        tempPath.delete()
        tempPath = File.createTempFile("tempGallery", null, requireContext().cacheDir)

        try {
            FileOutputStream(tempPath).use { out ->
                bm.compress(Bitmap.CompressFormat.JPEG, 70, out)
            }

            isCarImageChanged = true

            //Load the image
            loadImage(binding.carImageView, tempPath, R.drawable.car)

        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
            snackBarError(view, "Error with the image")

            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == AppCompatActivity.RESULT_OK) {
            val bm = data?.extras?.get("data") as Bitmap
            saveInCacheAndLoad(bm)
        }

        if (requestCode == REQUEST_EXTERNAL_STORAGE && resultCode == Activity.RESULT_OK) {

            val input = requireContext().contentResolver.openInputStream(data?.data!!)
            if (input != null) {
                val bm = BitmapFactory.decodeStream(input)
                val bitmap = rotateImageIfRequired(requireContext(), bm, data.data!!.toString())
                saveInCacheAndLoad(bitmap)
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