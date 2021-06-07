package it.polito.mad.carpooling_09.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import it.polito.mad.carpooling_09.repository.FirebaseUserLiveData

class LoginViewModel : ViewModel() {

    /**
     * Possible state of the user authentication
     */
    enum class AuthenticationState {
        AUTHENTICATED, UNAUTHENTICATED, INVALID_AUTHENTICATION
    }

    /**
     * It will transform a LiveData<FirebaseUser?> into a LiveData<AuthenticationState>
     * @return LiveData<AuthenticationState> which represents if the user is authenticated or not
     */
    val authenticationState = FirebaseUserLiveData().map { user ->
        if (user != null) {
            AuthenticationState.AUTHENTICATED
        } else {
            AuthenticationState.UNAUTHENTICATED
        }
    }

}
