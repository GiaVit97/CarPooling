package it.polito.mad.carpooling_09.viewmodel

import androidx.lifecycle.ViewModel
import it.polito.mad.carpooling_09.data.User
import it.polito.mad.carpooling_09.repository.UserRepository

class EditProfileViewModel : ViewModel() {
    private var userRepository = UserRepository()
    private var isSaving: Boolean = false

    fun updateUser(user: User, callback: (success: Boolean, errorMessage: String?) -> Unit) {
        setIsSaving(true)
        userRepository.updateUser(user, callback)
    }


    fun updateUserWithImage(user: User, callback: (success: Boolean, errorMessage: String?) -> Unit) {
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

    fun setIsSaving(status: Boolean) {
        isSaving = status
    }
}