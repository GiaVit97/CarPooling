package it.polito.mad.carpooling_09.ui.interestedTrips

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import it.polito.mad.carpooling_09.data.Role
import it.polito.mad.carpooling_09.databinding.RatingsFragmentBinding
import it.polito.mad.carpooling_09.utils.snackBar
import it.polito.mad.carpooling_09.utils.snackBarError
import it.polito.mad.carpooling_09.viewmodel.*

class RatingsFragment : DialogFragment() {

    private var _binding: RatingsFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var vmFactory: RatingsViewModelFactory
    private lateinit var vm: RatingsViewModel

    private val args: RatingsFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.e("user", args.booking.userId!!)
        vmFactory = RatingsViewModelFactory(args.booking.userId!!)
        vm = ViewModelProvider(this, vmFactory).get(RatingsViewModel::class.java)

        _binding = RatingsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.confirmButton.setOnClickListener { onConfirmClicked() }
        binding.cancelButton.setOnClickListener { onCancelClicked() }
    }

    private fun onConfirmClicked() {
        if (binding.ratingBar1.rating == 0.0f) {
            snackBarError(view, "You must insert a rating")
        } else {
            if (args.role == Role.Passenger.toString()) {

                vm.addReview(
                    null,
                    args.booking,
                    binding.writeComment.text.toString(),
                    binding.ratingBar1.rating,
                    Role.valueOf(args.role)
                ) { success, documentID, errorMessage ->
                    if (success) {
                        snackBar(view, "Review added successfully")

                    } else {
                        Log.e("RATING_FRAGMENT", errorMessage.toString())
                        snackBarError(view, "Error while adding review, try again please")
                    }
                }
            }
            else if (args.role == Role.Driver.toString()) {
                vm.getTrip(args.booking.tripId!!).observe(viewLifecycleOwner) {
                    val ownerTripId = it.userID!!
                    vm.addReview(
                        ownerTripId,
                        args.booking,
                        binding.writeComment.text.toString(),
                        binding.ratingBar1.rating,
                        Role.valueOf(args.role)
                    ) { success, documentID, errorMessage ->
                        if (success) {
                            view?.let {
                                Snackbar.make(
                                    it,
                                    "Review added successfully",
                                    Snackbar.LENGTH_LONG
                                )
                                    .show()
                            }
                        } else {
                            Log.e("RATING_FRAGMENT", errorMessage.toString())
                            view?.let {
                                Snackbar.make(
                                    it,
                                    "Error while adding review, try again please",
                                    Snackbar.LENGTH_LONG
                                )
                                    .show()
                            }
                        }
                    }
                }
            }
            dismiss()
        }
    }

    private fun onCancelClicked() {
        dismiss()
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
    }

}