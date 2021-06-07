package it.polito.mad.carpooling_09.ui.findTrip

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import it.polito.mad.carpooling_09.MapDialogFragment
import it.polito.mad.carpooling_09.R
import it.polito.mad.carpooling_09.data.PreferencesType
import it.polito.mad.carpooling_09.data.TripFilter
import it.polito.mad.carpooling_09.databinding.DialogFilterBinding
import it.polito.mad.carpooling_09.utils.*
import it.polito.mad.carpooling_09.viewmodel.OthersTripViewModel
import org.osmdroid.util.GeoPoint
import java.util.*

class FilterDialogFragment : DialogFragment() {

    companion object {
        private const val TAG = "FilterDialogFragment"
    }

    private var _binding: DialogFilterBinding? = null
    private val binding get() = _binding!!

    private val vm: OthersTripViewModel by activityViewModels()


    private lateinit var filters: TripFilter

    //Options array for number seats, smoke, animal and music preferences
    private lateinit var preferencesOptions: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        filters = vm.getTripFilter().value!!
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferencesOptions = resources.getStringArray(R.array.preferences_array)
        val preferencesAdapter =
            ArrayAdapter(requireContext(), R.layout.dropdown_item, preferencesOptions)

        val smoke = when (filters.smoke) {
            PreferencesType.Yes -> {
                1
            }
            PreferencesType.No -> {
                2
            }
            else -> {
                0
            }
        }

        val animals = when (filters.animals) {
            PreferencesType.Yes -> {
                1
            }
            PreferencesType.No -> {
                2
            }
            else -> {
                0
            }
        }

        val music = when (filters.music) {
            PreferencesType.Yes -> {
                1
            }
            PreferencesType.No -> {
                2
            }
            else -> {
                0
            }
        }


        val departureLocation = filters.departureLocation?.let {
            coordinatesToAddress(requireContext(), it.latitude, it.longitude)
        }

        binding.departureLocation.setText(departureLocation)

        val arrivalLocation = filters.arrivalLocation?.let {
            coordinatesToAddress(requireContext(), it.latitude, it.longitude)
        }

        binding.arrivalLocation.setText(arrivalLocation)

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
            updatePosition(oldCoordinates, binding.departureLocation)
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
            updatePosition(oldCoordinates, binding.arrivalLocation)
        }

        val filterDate = convertUTCToString(filters.departureDate)
        binding.departureDate.setText(filterDate)

        binding.rangePrice.progress = filters.price ?: 0
        binding.rangePriceText.text = getPriceText(binding.rangePrice.progress)


        binding.smoke.setText(preferencesAdapter.getItem(smoke).toString(), false)
        binding.animal.setText(preferencesAdapter.getItem(animals).toString(), false)
        binding.music.setText(preferencesAdapter.getItem(music).toString(), false)


        binding.applyButton.setOnClickListener { onSearchClicked() }
        binding.cancelButton.setOnClickListener { onCancelClicked() }
        binding.closeButton.setOnClickListener { onCloseClicked() }


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

        binding.rangePrice.setOnSeekBarChangeListener(
            @SuppressLint("AppCompatCustomView")
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    binding.rangePriceText.text = getPriceText(progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

        validate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

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

    private fun onSearchClicked() {

        /*if (binding.departureLocation.text.isNullOrEmpty() ||
            binding.arrivalLocation.text.isNullOrEmpty()) {
                snackBarError(view, "Locations can't be null")
        } else*/
        if (!hasError()) {
            snackBarError(view, "Please fill fields correctly")
        } else {

            var departureGeoPoint : com.google.firebase.firestore.GeoPoint? = null
            var arrivalGeoPoint : com.google.firebase.firestore.GeoPoint? = null
            if(!binding.departureLocation.text.isNullOrEmpty() && !binding.arrivalLocation.text.isNullOrEmpty()) {
                val departureLocation =
                    addressToCoordinates(
                        requireContext(),
                        binding.departureLocation.text.toString()
                    )
                departureGeoPoint = com.google.firebase.firestore.GeoPoint(
                    departureLocation!!.latitude,
                    departureLocation.longitude
                )

                val arrivalLocation =
                    addressToCoordinates(requireContext(), binding.arrivalLocation.text.toString())
                arrivalGeoPoint = com.google.firebase.firestore.GeoPoint(
                    arrivalLocation!!.latitude,
                    arrivalLocation.longitude
                )
            }

            val smoke =
                when {
                    binding.smoke.text.toString() == PreferencesType.Yes.choice -> PreferencesType.Yes
                    binding.smoke.text.toString() == PreferencesType.No.choice -> PreferencesType.No
                    else -> PreferencesType.NoPreferences
                }

            val animals =
                when {
                    binding.animal.text.toString() == PreferencesType.Yes.choice -> PreferencesType.Yes
                    binding.animal.text.toString() == PreferencesType.No.choice -> PreferencesType.No
                    else -> PreferencesType.NoPreferences
                }

            val music =
                when {
                    binding.music.text.toString() == PreferencesType.Yes.choice -> PreferencesType.Yes
                    binding.music.text.toString() == PreferencesType.No.choice -> PreferencesType.No
                    else -> PreferencesType.NoPreferences
                }

            val date = convertStringToDate(binding.departureDate.text.toString())

            vm.setTripFilter(
                TripFilter(
                    departureGeoPoint,
                    arrivalGeoPoint,
                    date,
                    binding.rangePrice.progress,
                    smoke,
                    animals,
                    music
                )
            )

            val action = FilterDialogFragmentDirections.actionFilterDialogFragmentToOthersTripListFragment()
            findNavController().navigate(action)
        }
    }

    private fun onCloseClicked() {
        val action = FilterDialogFragmentDirections.actionFilterDialogFragmentToOthersTripListFragment()
        findNavController().navigate(action)
    }

    private fun onCancelClicked() {
        preferencesOptions = resources.getStringArray(R.array.preferences_array)
        val preferencesAdapter =
            ArrayAdapter(requireContext(), R.layout.dropdown_item, preferencesOptions)

        binding.departureLocation.text = null
        binding.arrivalLocation.text = null
        binding.departureDate.text = null
        binding.rangePrice.progress = 0
        binding.smoke.setText(preferencesAdapter.getItem(0).toString(), false)
        binding.animal.setText(preferencesAdapter.getItem(0).toString(), false)
        binding.music.setText(preferencesAdapter.getItem(0).toString(), false)

        vm.setTripFilter(TripFilter())
        dismiss()
    }

    private fun getPriceText(price: Int): String {
        return if (price > 0) {
            "Trip price less than: €${price}"
        } else {
            "Filter by price"
        }
    }

    private lateinit var observer: LifecycleEventObserver

    private fun updatePosition(
        location: com.google.firebase.firestore.GeoPoint?,
        tv: TextView
    ) {
        var action = FilterDialogFragmentDirections.actionGlobalMapDialog(null, null)

        if (location != null) {

            action = FilterDialogFragmentDirections.actionGlobalMapDialog(
                location.latitude.toString(),
                location.longitude.toString()
            )
        }
        findNavController().navigate(action)

        val navBackStackEntry = findNavController().getBackStackEntry(R.id.filterDialogFragment)

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
                } else {
                    tv.text = coordinatesToAddress(
                        requireContext(),
                        geoPoint.latitude,
                        geoPoint.longitude
                    )
                }
            }
        }


        navBackStackEntry.lifecycle.addObserver(observer)

        // As addObserver() does not automatically remove the observer, we
        // call removeObserver() manually when the view lifecycle is destroyed
        viewLifecycleOwner.lifecycle.addObserver(LifecycleEventObserver
        { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                navBackStackEntry.lifecycle.removeObserver(observer)
            }
        })
    }

    private fun hasError(): Boolean {

        return when {
            !binding.departureDateLayout.error.isNullOrEmpty() -> false
            !binding.departureLocationLayout.error.isNullOrEmpty() -> false
            !binding.arrivalLocationLayout.error.isNullOrEmpty() -> false
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


        binding.departureLocation.doOnTextChanged { departure, _, _, _ ->

            if (departure.isNullOrEmpty() && !binding.arrivalLocation.text.isNullOrEmpty()) {
                binding.departureLocationLayout.error =
                    "Field required if you have an arrival location"
                binding.arrivalLocationLayout.error = null
            } else if (!departure.isNullOrEmpty() && binding.arrivalLocation.text.isNullOrEmpty()) {
                binding.departureLocationLayout.error = null
                binding.arrivalLocationLayout.error =
                    "Field required if you have a departure location"
            } else {
                binding.departureLocationLayout.error = null
                binding.arrivalLocationLayout.error = null
            }

        }

        binding.arrivalLocation.doOnTextChanged { arrival, _, _, _ ->
            if (binding.departureLocation.text.isNullOrEmpty() && !arrival.isNullOrEmpty()) {
                binding.departureLocationLayout.error =
                    "Field required if you have an arrival location"
                binding.arrivalLocationLayout.error = null
            } else if (!binding.departureLocation.text.isNullOrEmpty() && arrival.isNullOrEmpty()) {
                binding.departureLocationLayout.error = null
                binding.arrivalLocationLayout.error =
                    "Field required if you have a departure location"
            } else {
                binding.departureLocationLayout.error = null
                binding.arrivalLocationLayout.error = null
            }
        }
    }

}