package it.polito.mad.carpooling_09.viewmodel


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import it.polito.mad.carpooling_09.data.User
import it.polito.mad.carpooling_09.data.UserPublic
import it.polito.mad.carpooling_09.repository.BookingRepository
import it.polito.mad.carpooling_09.repository.RatingRepository
import it.polito.mad.carpooling_09.repository.UserRepository

class ProfileViewModel(userID: String = " ") : ViewModel() {

    private var userRepository = UserRepository()
    private var ratingsRepository = RatingRepository()
    private var bookingRepository = BookingRepository()

    private var isSaving: Boolean = false

    private var isPrivateProfile: Boolean = false
    private var imgPath: String? = null

    private val currentUserPrivate: MutableLiveData<User> = userRepository
        .getPrivateUserProfileById(Firebase.auth.currentUser?.uid ?: " ") as MutableLiveData<User>

    private val currentUserPublic: MutableLiveData<UserPublic> = userRepository
        .getPublicUserProfileById(userID) as MutableLiveData<UserPublic>


    /**
     * Retrieve the user public information
     * @return User object
     */
    fun getUserPublicInfo(): LiveData<UserPublic> {
        return currentUserPublic
    }

    /**
     * Retrieve the user logged-in information (private and public one)
     * @return User object
     */
    fun getCurrentUserInfo(): LiveData<User> {
        return currentUserPrivate
    }

    /**
     * Create the user taking the information such as uid, name and email from firebase Auth
     * @param callback : will return a success boolean and eventually an error message
     */
    fun createUserWithAuthInfo(callback: (success: Boolean, errorMessage: String?) -> Unit) {
        val newUser = User(
            Firebase.auth.currentUser?.uid,
            Firebase.auth.currentUser?.displayName,
            Firebase.auth.currentUser?.displayName,
            Firebase.auth.currentUser?.email
        )
        userRepository.createUser(Firebase.auth.currentUser?.uid!!, newUser, callback)
    }


    fun updateUserWithImage(
        user: User,
        callback: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        setIsSaving(true)

        if (!user.url.isNullOrEmpty() && !(user.url as String).startsWith("http")) {
            //We have an image that doesn't start with 'http', so we need to load it

            userRepository.uploadUserPicture(
                user.url!!,
                user.id!!
            ) { success, imageURL, errorMessage ->

                if (success && imageURL != null) {
                    user.url = imageURL
                    userRepository.updateUser(user, callback)
                } else {
                    // There was an error during the image upload, so we return above the
                    // errorMessage given by the uploadUserPicture
                    callback(false, errorMessage)
                }
            }

        } else {
            // The image is null or empty, so it will not be loaded
            userRepository.updateUser(user, callback)
        }
    }


    /**
     * TODO
     * @param status :
     */
    fun setIsSaving(status: Boolean) {
        isSaving = status
    }

    fun isPrivateProfile(): Boolean {
        return isPrivateProfile
    }

    fun setPrivateProfile(bool: Boolean) {
        isPrivateProfile = bool
    }

    fun getImgPath(): String? {
        return imgPath
    }

    fun setImgPath(path: String) {
        imgPath = path
    }
}


