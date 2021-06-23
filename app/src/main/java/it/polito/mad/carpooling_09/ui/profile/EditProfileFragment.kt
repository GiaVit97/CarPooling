package it.polito.mad.carpooling_09.ui.profile

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Address
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil.load
import coil.size.Precision
import coil.size.Scale
import coil.transform.CircleCropTransformation
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.ktx.Firebase
import it.polito.mad.carpooling_09.BuildConfig
import it.polito.mad.carpooling_09.MainActivity
import it.polito.mad.carpooling_09.MapDialogFragment
import it.polito.mad.carpooling_09.R
import it.polito.mad.carpooling_09.data.User
import it.polito.mad.carpooling_09.databinding.FragmentEditProfileBinding
import it.polito.mad.carpooling_09.utils.*
import it.polito.mad.carpooling_09.viewmodel.EditProfileViewModel
import it.polito.mad.carpooling_09.viewmodel.ProfileViewModel
import java.io.File
import java.io.FileOutputStream
import java.util.*


class EditProfileFragment : Fragment(R.layout.fragment_edit_profile) {

    companion object {
        private const val TAG = "EditProfileFragment"

        const val PERMISSION_REQUEST_CAMERA = 101
        const val PERMISSION_REQUEST_EXTERNAL_STORAGE = 102

        const val REQUEST_IMAGE_CAPTURE = 1
        const val REQUEST_EXTERNAL_STORAGE = 2
    }

    // Binding
    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private val bindingEditInfo get() = _binding!!.profileEditInfo

    // Safe Args
    private val args: EditProfileFragmentArgs by navArgs()

    // View Model
    private val vm: ProfileViewModel by activityViewModels()

    // Thanks to this we understand if the profile image changes or not
    private var isProfileImageChanged = false
    private var tempPath = File("")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Load information passed by safe args
        bindingEditInfo.fullName.setText(args.fullName)
        bindingEditInfo.nickname.setText(args.nickName)
        bindingEditInfo.email.setText(args.email)
        bindingEditInfo.location.setText(args.location)
        bindingEditInfo.birthDate.setText(args.birthDate)
        bindingEditInfo.telephone.setText(args.telephone)

        //Take the path for the temporary profile image in cache
        if (savedInstanceState != null) {
            isProfileImageChanged = savedInstanceState.getBoolean("isProfileImageChanged")
            tempPath =
                File(requireContext().cacheDir, savedInstanceState.getString("filePath") ?: "")
        }

        // Check if we have a temporary user profile image in cache to load instead of the DB one
        if (isProfileImageChanged && tempPath.exists()) {
            loadImage(binding.profileImage, tempPath)
        } else args.imageProfileFileName?.let {
            loadImage(binding.profileImage, it)
        }


        bindingEditInfo.birthDate.setOnClickListener {

            val date = if (!bindingEditInfo.birthDate.text.isNullOrEmpty()) {
                convertStringToDate(bindingEditInfo.birthDate.text.toString())
            } else Date()

            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select date")
                .setSelection(date?.time)
                .build()

            datePicker.show(parentFragmentManager, TAG)

            datePicker.addOnPositiveButtonClickListener {
                bindingEditInfo.birthDate.setText(convertUTCToString(datePicker.selection))
            }

        }
        //Set a floating context menu by clicking the camera icon
        //With this the menu opens even with single click and not only with a long press
        binding.imageButton.setOnClickListener { requireActivity().openContextMenu(it) }
        registerForContextMenu(binding.imageButton)


        bindingEditInfo.location.setOnClickListener {

            var latitude: String? = null
            var longitude: String? = null

            if (!bindingEditInfo.location.text.isNullOrEmpty()) {
                val location =
                    addressToCoordinates(requireContext(), bindingEditInfo.location.text.toString())
                latitude = location?.latitude.toString()
                longitude = location?.longitude.toString()
            }

            val action = EditProfileFragmentDirections.actionGlobalMapDialog(latitude, longitude)
            findNavController().navigate(action)
        }

        val navBackStackEntry = findNavController().getBackStackEntry(R.id.editProfileFragment)

        // Create our observer and add it to the NavBackStackEntry's lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME
                && navBackStackEntry.savedStateHandle.contains(MapDialogFragment.GEO_POINT)
            ) {
                val geoPoint = navBackStackEntry.savedStateHandle.get<org.osmdroid.util.GeoPoint>(
                    MapDialogFragment.GEO_POINT
                )
                if (geoPoint == null || (geoPoint.latitude == 0.0 && geoPoint.longitude == 0.0)) {
                    //Nothing is coming back
                } else {
                    bindingEditInfo.location.setText(context?.let {
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
        viewLifecycleOwner.lifecycle.addObserver(LifecycleEventObserver
        { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                navBackStackEntry.lifecycle.removeObserver(observer)
            }
        })

        validate()

    }

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
                    requireActivity(),
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
                    requireActivity(),
                    R.string.permission_denied,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun useCamera() {
        //Device has a camera and we create an Intent
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                requireContext(),
                R.string.generic_problem,
                Toast.LENGTH_SHORT
            ).show()
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

            isProfileImageChanged = true

            //Load the image
            loadImage(binding.profileImage, tempPath)

        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
            snackBarError(view, "Error with the image")
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.save_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                requireActivity().onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.save -> {
                // Save the new information inside Firestore
                if (hasError()) {
                    (requireActivity() as MainActivity).progressBarVisibility(true)
                    saveOnFirestore()
                } else {
                    snackBarError(view, "There are some errors on the page")
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun hasError(): Boolean {

        return when {
            !bindingEditInfo.fullNameLayout.error.isNullOrEmpty() -> false
            !bindingEditInfo.nicknameLayout.error.isNullOrEmpty() -> false
            !bindingEditInfo.emailLayout.error.isNullOrEmpty() -> false
            !bindingEditInfo.birthDateLayout.error.isNullOrEmpty() -> false
            !bindingEditInfo.telephoneLayout.error.isNullOrEmpty() -> false
            !bindingEditInfo.locationLayout.error.isNullOrEmpty() -> false
            else -> true
        }
    }

    private fun validate() {
        bindingEditInfo.nickname.doOnTextChanged { text, _, _, _ ->

            if (text.isNullOrEmpty()) {
                bindingEditInfo.nicknameLayout.error = "Field required"
            }
            if (text!!.length > 20) {
                bindingEditInfo.nicknameLayout.error = "Too much character"
            } else if (text.isNotEmpty() && text.length <= 20) {
                bindingEditInfo.nicknameLayout.error = null
            }
        }

        bindingEditInfo.email.doOnTextChanged { text, _, _, _ ->
            if (text.isNullOrEmpty()) {
                bindingEditInfo.emailLayout.error = "Field required"
            } else if (text.isNotEmpty()) {
                bindingEditInfo.emailLayout.error = null
            }
        }

        bindingEditInfo.fullName.doOnTextChanged { text, _, _, _ ->
            if (text.isNullOrEmpty()) {
                bindingEditInfo.fullNameLayout.error = "Field required"
            } else if (text.isNotEmpty()) {
                bindingEditInfo.fullNameLayout.error = null
            }
        }

        bindingEditInfo.birthDate.doOnTextChanged { text, _, _, _ ->
            val date = convertStringToDate(text.toString())
            date?.let {
                when {
                    getAge(it) < 0 -> bindingEditInfo.birthDateLayout.error =
                        "Date can't be in future"
                    getAge(it) in 0..17 -> bindingEditInfo.birthDateLayout.error =
                        "You must have 18 years"
                    else -> bindingEditInfo.birthDateLayout.error = null
                }
            }
        }

        bindingEditInfo.telephone.doOnTextChanged { text, _, _, _ ->
            if (text!!.length > 10) {
                bindingEditInfo.telephoneLayout.error = "Too much character"
            } else if (text.length <= 10) {
                bindingEditInfo.telephoneLayout.error = null
            }
        }

    }

    private fun saveOnFirestore() {
        val uuid = Firebase.auth.currentUser?.uid
        val urlImage = args.imageProfileFileName
        val date = convertStringToDate(bindingEditInfo.birthDate.text.toString())

        var geoPoint: GeoPoint? = null

        if (!bindingEditInfo.location.text.isNullOrEmpty()) {
            addressToCoordinates(requireContext(), bindingEditInfo.location.text.toString())?.let {
                geoPoint = GeoPoint(it.latitude, it.longitude)
            }
        }

        val userNew = User(
            uuid!!,
            bindingEditInfo.fullName.text.toString(),
            bindingEditInfo.nickname.text.toString(),
            bindingEditInfo.email.text.toString(),
            geoPoint,
            date,
            bindingEditInfo.telephone.text.toString().toLongOrNull(),
            urlImage
        )

        if (isProfileImageChanged) userNew.url = tempPath.toString()

        vm.updateUserWithImage(userNew) { success, errorMessage ->
            //vm.setIsSaving(false)
            (requireActivity() as MainActivity).progressBarVisibility(false)

            if (success) {
                snackBar(view, "User was edited successfully")

                //Delete the temporary image before close the activity
                tempPath.delete()
                isProfileImageChanged = false

                val action =
                    EditProfileFragmentDirections.actionEditProfileFragmentToShowProfile()
                findNavController().navigate(action)

            } else {
                Log.e(TAG, errorMessage.toString())
                snackBarError(view, "Error during the update of the profile, try again please")
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {

                    val oldUser = User(
                        null,
                        args.fullName,
                        args.nickName,
                        args.email,
                        convertAddressToGeoPoint(requireContext(), args.location),
                        convertStringToDate(args.birthDate ?: "01/01/2020"),
                        args.telephone?.toLongOrNull(),
                        null
                    )

                    val newUser = User(
                        null,
                        bindingEditInfo.fullName.text.toString(),
                        bindingEditInfo.nickname.text.toString(),
                        bindingEditInfo.email.text.toString(),
                        convertAddressToGeoPoint(requireContext(), bindingEditInfo.location.text.toString()),
                        convertStringToDate(bindingEditInfo.birthDate.text.toString()),
                        bindingEditInfo.telephone.text.toString().toLongOrNull(),
                        null
                    )

                    if (isProfileImageChanged || oldUser != newUser) {
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


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isProfileImageChanged", isProfileImageChanged)
        outState.putString("filePath", tempPath.name)
    }

    override fun onPause() {
        closeKeyboard(requireActivity())
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}