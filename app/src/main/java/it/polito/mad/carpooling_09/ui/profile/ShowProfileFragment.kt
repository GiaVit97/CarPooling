package it.polito.mad.carpooling_09.ui.profile

import android.graphics.Color
import android.graphics.LightingColorFilter
import android.os.Bundle
import android.util.Log
import androidx.preference.PreferenceManager
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.NavigationUI
import coil.load
import coil.size.Scale
import coil.transform.CircleCropTransformation
import com.google.firebase.auth.FirebaseAuth
import it.polito.mad.carpooling_09.MainActivity
import it.polito.mad.carpooling_09.R
import it.polito.mad.carpooling_09.data.Role
import it.polito.mad.carpooling_09.data.Status
import it.polito.mad.carpooling_09.data.User
import it.polito.mad.carpooling_09.databinding.FragmentShowProfileBinding
import it.polito.mad.carpooling_09.utils.*
import it.polito.mad.carpooling_09.viewmodel.ProfileViewModel
import it.polito.mad.carpooling_09.viewmodel.RatingsViewModel
import it.polito.mad.carpooling_09.viewmodel.UserViewModelFactory
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay


class ShowProfileFragment : Fragment(R.layout.fragment_show_profile) {

    companion object {
        private const val TAG = "ShowProfileFragment"
    }

    // Binding
    private var _binding: FragmentShowProfileBinding? = null
    private val binding get() = _binding!!
    private val bindingUserInfo get() = _binding!!.profileInfo

    // Safe Args
    private val args: ShowProfileFragmentArgs? by navArgs()

    private lateinit var mAuth: FirebaseAuth

    private val vm: ProfileViewModel by viewModels { UserViewModelFactory(userIdToShow) }

    // Profile page need to show the profile of that userId
    private lateinit var userIdToShow: String

    override fun onCreate(savedInstanceState: Bundle?) {
        mAuth = FirebaseAuth.getInstance()

        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // If we open the fragment passing a userId through safeArgs, it means that we want to show
        // the public information of a user; otherwise we want to show the profile of the authenticated user.
        userIdToShow = args?.userId ?: mAuth.currentUser!!.uid

        vm.setPrivateProfile(userIdToShow == mAuth.currentUser!!.uid)

        if (!vm.isPrivateProfile()) {
            // The fragment is open to show a public profile and is not a top level destination.
            // The hamburger menu will be replaces by the arrow back button
            (requireActivity() as MainActivity).removeTopLevelProfile()
        }

        _binding = FragmentShowProfileBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        if (vm.isPrivateProfile()) {
            // We want to show the profile of the authenticated user

            getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
            bindingUserInfo.map.setTileSource(TileSourceFactory.MAPNIK)

            vm.getCurrentUserInfo().observe(viewLifecycleOwner) { user ->
                user?.let { displayInfo(it) }
            }

        } else {
            // I want to show the profile of another user (not the authenticated one)
            bindingUserInfo.fullName.visibility = View.GONE
            bindingUserInfo.location.visibility = View.GONE
            bindingUserInfo.birthDate.visibility = View.GONE
            bindingUserInfo.telephone.visibility = View.GONE
            bindingUserInfo.privateInfoSection.visibility = View.GONE
            bindingUserInfo.map.visibility = View.GONE

            vm.getUserPublicInfo().observe(viewLifecycleOwner) { userPublic ->

                if(userPublic==null){
                    snackBarError(requireView(), "User not found, sorry")
                }
                else {
                    (requireActivity() as AppCompatActivity).supportActionBar?.title =
                        "${userPublic.nickname}'s profile"

                    bindingUserInfo.email.text = userPublic.email
                    bindingUserInfo.nickname.text = userPublic.nickname

                    bindingUserInfo.driverRatingBar.rating = userPublic.driverStar ?: 0f
                    bindingUserInfo.passengerRatingBar.rating = userPublic.passengerStar ?: 0f

                    //Check if the file exists; otherwise it will load a placeholder image
                    userPublic.url?.let {
                        loadImage(binding.profileImage, it)
                    }
                }
            }
        }

        bindingUserInfo.buttonReview.setOnClickListener {

           val action = ShowProfileFragmentDirections.actionShowProfileFragmentToRatingsFragmentProfile(userIdToShow)
            findNavController().navigate(action)
        }
    }


    private fun displayInfo(user: User) {

        bindingUserInfo.fullName.text = user.fullName
        bindingUserInfo.nickname.text = user.nickname
        bindingUserInfo.email.text = user.email
        bindingUserInfo.birthDate.text = convertUTCToString(user.birthday)
        bindingUserInfo.telephone.text = user.telephone?.toString()
        bindingUserInfo.location.text = coordinatesToAddress(
            requireContext(),
            user.location?.latitude,
            user.location?.longitude
        )

        // Show the profile image if the user has one and save the url into viewModel
        user.url?.let { userUrl ->
            vm.setImgPath(userUrl)
            loadImage(binding.profileImage, userUrl)
        }


        user.location?.also { location ->
            // Show the map because the user has set a location
            bindingUserInfo.map.visibility = View.VISIBLE

            val marker = Marker(bindingUserInfo.map, context)
            val rotationGestureOverlay = RotationGestureOverlay(bindingUserInfo.map)
            val icon = ResourcesCompat.getDrawable(
                resources,
                R.drawable.ic_baseline_location_on_24, null
            )
            icon?.colorFilter = LightingColorFilter(Color.RED, Color.RED)

            rotationGestureOverlay.isEnabled
            bindingUserInfo.map.setMultiTouchControls(true)
            bindingUserInfo.map.overlays.add(rotationGestureOverlay)

            marker.icon = icon
            marker.position = GeoPoint(location.latitude, location.longitude)
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            marker.title = context?.let {
                coordinatesToAddress(it, location.latitude, location.longitude)
            }
            bindingUserInfo.map.overlays.add(marker)
            bindingUserInfo.map.controller.setZoom(16.5)
            bindingUserInfo.map.controller.setCenter(
                GeoPoint(location.latitude, location.longitude)
            )
        }

        bindingUserInfo.driverRatingBar.rating = user.driverStar ?: 0f
        bindingUserInfo.passengerRatingBar.rating = user.passengerStar ?: 0f


    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Reset the profile fragment as a top level destination
        (requireActivity() as MainActivity).addTopLevelProfile()
        _binding = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.nav_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)

        // The pencil menu item is visible only if the profile is the one of the authenticated user
        menu.findItem(R.id.nav_edit).isVisible = vm.isPrivateProfile()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.nav_edit -> {
                editProfile()
                true
            }
            else -> NavigationUI.onNavDestinationSelected(
                item, requireView().findNavController()
            ) || super.onOptionsItemSelected(item)
        }
    }

    /**
     * This method will invoke when the pencil button on the top bar is pressed
     * (i.e. the user want to edit the profile)
     */
    private fun editProfile() {

        val action = ShowProfileFragmentDirections.actionShowProfileToEditProfileFragment(
            bindingUserInfo.fullName.text.toString(),
            bindingUserInfo.nickname.text.toString(),
            bindingUserInfo.email.text.toString(),
            bindingUserInfo.location.text.toString(),
            bindingUserInfo.birthDate.text.toString(),
            bindingUserInfo.telephone.text.toString(),
            vm.getImgPath(),
        )
        findNavController().navigate(action)
    }
}