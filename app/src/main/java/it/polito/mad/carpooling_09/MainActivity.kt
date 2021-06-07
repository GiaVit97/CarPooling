package it.polito.mad.carpooling_09

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import coil.load
import coil.size.Precision
import coil.size.Scale
import coil.transform.CircleCropTransformation
import com.google.android.material.navigation.NavigationView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import it.polito.mad.carpooling_09.databinding.ActivityMainBinding
import it.polito.mad.carpooling_09.viewmodel.ProfileViewModel
import it.polito.mad.carpooling_09.utils.DrawerLocker
import it.polito.mad.carpooling_09.utils.loadImage
import it.polito.mad.carpooling_09.utils.snackBar
import it.polito.mad.carpooling_09.utils.snackBarError
import it.polito.mad.carpooling_09.viewmodel.LoginViewModel

class MainActivity : AppCompatActivity(), DrawerLocker {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView

        //Navigation
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.findNavController()

        // Passing each menu ID as a set of Ids because each menu should be considered as top
        // level destinations. It appears the hamburger menu instead of the back arrow.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.OthersTripListFragment,
                R.id.TripListFragment,
                R.id.BoughtTripsListFragment,
                R.id.TripsOfInterestListFragment,
                R.id.ShowProfileFragment
            ), drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Observe the authentication state of the user and change UI accordingly
        observeAuthenticationState()

        binding.buttonLogout.setOnClickListener {
            // Logout the user
            Firebase.auth.signOut()
            //The observeAuthenticationState will notice it and redirect to LoginFragment
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }


    override fun setDrawerLocked(shouldLock: Boolean) {
        if (shouldLock) {
            binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        } else {
            binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        }
    }

    /**
     * Observes the authentication state and changes the UI accordingly.
     * If there is no logged in user: it will be redirected to LoginFragment
     */
    private fun observeAuthenticationState() {
        val loginViewModel by viewModels<LoginViewModel>()

        loginViewModel.authenticationState.observe(this) { authenticationState ->
            when (authenticationState) {
                LoginViewModel.AuthenticationState.AUTHENTICATED -> {

                    // User is logged-in, we will update the navDrawer header with the user information
                    updateNavDrawerHeader()
                }
                else -> {
                    // Possible authentication state: UNAUTHENTICATED, INVALID_AUTHENTICATION
                    // User is not logged-in anymore, it will be redirected to LoginFragment
                    val action = LoginGraphDirections.actionGlobalLoginGraph()
                    navController.navigateUp()
                    navController.navigate(action)
                }
            }
        }
    }

    private fun updateNavDrawerHeader() {

        val hView = binding.navView.getHeaderView(0)
        val navHeaderImage = hView.findViewById<ImageView>(R.id.navHeaderImage)
        val navHeaderName = hView.findViewById<TextView>(R.id.navHeaderName)
        val navHeaderDesc = hView.findViewById<TextView>(R.id.navHeaderDesc)


        var profileImageUrl: String? = null

        //Ask to the view model the profile user information and load it inside NavigationDrawer
        val vm: ProfileViewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)

        vm.getCurrentUserInfo().observe(this) { User ->

            if (User != null) {
                navHeaderName.text = User.fullName
                navHeaderDesc.text = User.email
                profileImageUrl = User.url
            } else {
                // The user doesn't exist, I create a new one
                vm.createUserWithAuthInfo { success, errorMessage ->
                    if (!success) {
                        Log.e("MainActivity", errorMessage.toString())
                        snackBarError(
                            findViewById(android.R.id.content),
                            "Error during the creation of the profile"
                        )
                    }
                }
            }

            //Check if the user has a profile image, otherwise load a placeholder image
            if (!profileImageUrl.isNullOrEmpty()) {
                //Load the user profile image and cropping to a circle
                loadImage(navHeaderImage, profileImageUrl)
            }
        }
    }

    fun removeTopLevelProfile() {
        setupActionBarWithNavController(navController, appBarConfiguration.apply {
            topLevelDestinations.remove(R.id.ShowProfileFragment)
        })
    }

    fun addTopLevelProfile() {
        setupActionBarWithNavController(navController, appBarConfiguration.apply {
            topLevelDestinations.add(R.id.ShowProfileFragment)
        })
    }

    fun progressBarVisibility(visible: Boolean) {
        if (visible) {
            binding.appBarMain.progressBarTotal.visibility = View.VISIBLE
        } else {
            binding.appBarMain.progressBarTotal.visibility = View.INVISIBLE
        }
    }


}