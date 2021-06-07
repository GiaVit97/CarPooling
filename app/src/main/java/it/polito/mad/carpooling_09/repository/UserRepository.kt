package it.polito.mad.carpooling_09.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import it.polito.mad.carpooling_09.data.*
import it.polito.mad.carpooling_09.data.Booking.Companion.COLLECTION_BOOKINGS
import it.polito.mad.carpooling_09.data.Ratings.Companion.COLLECTION_RATINGS
import it.polito.mad.carpooling_09.data.Trip.Companion.COLLECTION_TRIPS
import it.polito.mad.carpooling_09.data.User.Companion.COLLECTION_USERS
import it.polito.mad.carpooling_09.data.UserPublic.Companion.COLLECTION_USER_PUBLIC
import java.io.File
import java.io.FileInputStream

class UserRepository {
    private val db = Firebase.firestore
    private var storage = Firebase.storage

    companion object {
        private const val TAG = "USER_REPOSITORY"
    }

    /**
     * It will retrieve all the public profile information of a given user
     * @param userID of the user
     * @return UserPublic live data object
     */
    fun getPublicUserProfileById(userID: String): LiveData<UserPublic> {
        val data = MutableLiveData<UserPublic>()

        db.collection(COLLECTION_USERS).document(userID).collection(COLLECTION_USER_PUBLIC)
            .document("profile")
            .addSnapshotListener { value, error ->

                if (error != null) {
                    Log.e(TAG, "Error on 'getPublicUserProfileById': $error")
                }

                if (value != null) {
                    try {
                        data.value = value.toObject(UserPublic::class.java)?.apply {
                            id = userID
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error getPublicUserProfileById: ${e.message}")
                    }
                }
            }
        return data
    }

    /**
     * It will retrieve all the private profile information of a given user
     * @param userID of the user
     * @return User live data object
     */
    fun getPrivateUserProfileById(userID: String): LiveData<User> {
        val data = MutableLiveData<User>()

        db.collection(COLLECTION_USERS).document(userID).addSnapshotListener { value, error ->

            if (error != null) {
                Log.e(TAG, "Error on 'getPrivateUserProfileById': $error")
            }

            if (value != null) {
                try {
                    data.value = value.toObject(User::class.java)?.apply {
                        id = value.id
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error getPrivateUserProfileById: ${e.message}")
                }
            }
        }
        return data
    }

    /**
     * It will create the user inside the DB with the public and private information
     * @param uid : user id
     * @param user : User object with the information to save
     * @param callback : callback that return the success and eventually an error message
     */
    fun createUser(
        uid: String,
        user: User,
        callback: (success: Boolean, errorMessage: String?) -> Unit
    ) {

        val privateProfileRef = db.collection(COLLECTION_USERS).document(uid)
        val publicProfileRef =
            db.collection(COLLECTION_USERS).document(uid).collection(COLLECTION_USER_PUBLIC)
                .document("profile")

        db.runTransaction { transaction ->
            // Update the private profile with the user info
            transaction.set(privateProfileRef, user)

            // Update the public profile information
            transaction.set(
                publicProfileRef,
                UserPublic(
                    id = uid,
                    nickname = user.nickname ?: user.fullName!!,
                    url = user.url,
                    email = user.email
                )
            )
        }
            .addOnSuccessListener {
                Log.d(TAG, "User created successfully")
                callback(true, null)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error on 'createUser': ${e.message}")
                callback(false, e.message)
            }
    }


    /**
     * It will retrieve all the public profile information of a given user once.
     * Do NOT return a live data; data will not change.
     * @param userID of the user
     * @param callback callback that return the userPublic information or an error message
     */
    fun getPublicUserOnce(
        userID: String,
        callback: (userPublic: UserPublic?, success: Boolean, error: String?) -> Unit
    ) {

        var data: UserPublic?

        val docRef =
            db.collection(COLLECTION_USERS).document(userID).collection(COLLECTION_USER_PUBLIC)
                .document("profile")

        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    try {
                        data = document.toObject(UserPublic::class.java)?.apply {
                            id = userID
                        }
                        callback(data, true, null)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error getPublicUserOnce: ${e.message}")
                        callback(null, false, "Generic error")
                    }

                } else {
                    Log.e(TAG, "No such document")
                    callback(null, false, "No such document")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getPublicUserOnce: ${e.message}")
                callback(null, false, e.message)
            }
    }

    /**
     * It will update an existing user into the DB (private and public information)
     * @param user User object that represents the user to update
     * @param callback that return the success of the operation or an error
     */
    fun updateUser(user: User, callback: (success: Boolean, error: String?) -> Unit) {

        if (user.id != null) {

            val privateProfileRef = db.collection(COLLECTION_USERS).document(user.id!!)
            val publicProfileRef =
                db.collection(COLLECTION_USERS).document(user.id!!)
                    .collection(COLLECTION_USER_PUBLIC)
                    .document("profile")

            db.collectionGroup(COLLECTION_BOOKINGS).whereEqualTo(Booking.FIELD_USER_ID, user.id)
                .get()
                .addOnSuccessListener { bookingsSnapshot ->

                    val bookingsList = bookingsSnapshot.documents

                    db.collectionGroup(COLLECTION_RATINGS)
                        .whereEqualTo(Ratings.FIELD_USER_ID, user.id)
                        .get()
                        .addOnSuccessListener { reviewsSnapshot ->

                            val reviewsList = reviewsSnapshot.documents

                            db.runTransaction { transaction ->

                                transaction.set(privateProfileRef, user, SetOptions.merge())

                                transaction.set(
                                    publicProfileRef,
                                    UserPublic(
                                        user.id!!,
                                        user.nickname ?: user.fullName,
                                        user.url,
                                        user.email
                                    ),
                                    SetOptions.merge()
                                )

                                bookingsList.forEach { doc ->
                                    val bookingRef = db.collection(COLLECTION_TRIPS)
                                        .document(doc[Booking.FIELD_TRIP_ID].toString())
                                        .collection(COLLECTION_BOOKINGS)
                                        .document(doc[Booking.FIELD_BOOKING_ID].toString())

                                    transaction.update(
                                        bookingRef,
                                        Booking.FIELD_NICKNAME,
                                        user.nickname,
                                        Booking.FIELD_USER_PHOTO,
                                        user.url
                                    )
                                }

                                reviewsList.forEach { doc ->
                                    val receiverRatingUserId =
                                        doc.reference.parent.parent?.id.toString()

                                    val reviewRef = db.collection(COLLECTION_USERS)
                                        .document(receiverRatingUserId)
                                        .collection(COLLECTION_RATINGS)
                                        .document(doc[Ratings.FIELD_REVIEW_ID].toString())

                                    transaction.update(
                                        reviewRef,
                                        Ratings.FIELD_NICKNAME,
                                        user.nickname,
                                        Ratings.FIELD_URL,
                                        user.url
                                    )

                                }
                            }
                                .addOnSuccessListener {
                                    Log.d(TAG, "User updated successfully")
                                    callback(true, null)
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Error on 'updateUser': ${e.message}")
                                    callback(false, e.message)
                                }


                        }.addOnFailureListener { e ->
                            Log.e(TAG, "UpdateUser failure during review update: ${e.message}")
                            callback(false, "Failure during review update" )
                        }

                }.addOnFailureListener { e ->
                    Log.e(TAG, "UpdateUser failure during booking update: ${e.message}")
                    callback(false, "Failure during booking update")
                }

        } else {
            Log.e(TAG, "Error: user is null")
            callback(false, "User id can't be null")
        }

    }


    /**
     * It will upload the profile image to Firebase storage
     * @param path local file path of the image saved into the device
     * @param userID id of the user associated with that image
     * @param callback that return the url of the just uploaded images or an error
     */
    fun uploadUserPicture(
        path: String,
        userID: String,
        callback: (success: Boolean, imageURL: String?, error: String?) -> Unit
    ) {
        val pathObj = File(path)
        if (!pathObj.exists()) {
            Log.e(TAG, "Error: path doesn't exist")
            callback(false, null, "Path doesn't exist")
        } else {

            val ref = storage.reference.child("userImages/${userID}.jpg")

            ref.putStream(FileInputStream(File(path)))
                .addOnSuccessListener {
                    ref.downloadUrl
                        .addOnSuccessListener { uri ->
                            Log.d(TAG, "Image successfully uploaded: $uri")
                            callback(true, uri.toString(), null)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error on 'uploadUserPicture': ${e.message}")
                            callback(false, null, e.message)
                        }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error on 'uploadUserPicture': ${e.message}")
                    callback(false, null, e.message)
                }
        }
    }

}
