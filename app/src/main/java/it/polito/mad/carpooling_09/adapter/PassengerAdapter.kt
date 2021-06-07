package it.polito.mad.carpooling_09.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.size.Scale
import coil.transform.CircleCropTransformation
import it.polito.mad.carpooling_09.R
import it.polito.mad.carpooling_09.data.Booking
import it.polito.mad.carpooling_09.data.Role
import it.polito.mad.carpooling_09.data.Status
import it.polito.mad.carpooling_09.databinding.PassengerLayoutBinding
import it.polito.mad.carpooling_09.ui.yourTrips.ManagePassengersFragmentDirections
import it.polito.mad.carpooling_09.utils.dateIsPassed
import it.polito.mad.carpooling_09.utils.loadImage
import java.text.DecimalFormat
import java.util.*

class PassengerAdapter(
    private val data: List<Booking>,
    private var onDelete: (Booking) -> Unit,
    private var onAdd: (Booking) -> Unit,
    private var openProfile: (Booking) -> Unit
) : RecyclerView.Adapter<PassengerAdapter.PassengerViewHolder>() {

    class PassengerViewHolder(
        v: View,
        private var onDelete: (Booking) -> Unit,
        private var onAdd: (Booking) -> Unit,
        private var openProfile: (Booking) -> Unit
    ) : RecyclerView.ViewHolder(v) {

        val binding = PassengerLayoutBinding.bind(v)
        private val ctx: Context = v.context

        private var currentBooking: Booking? = null

        init {
            binding.singleUser.setOnClickListener {
                currentBooking?.let {
                    openProfile(it)
                }
            }

             binding.acceptButton.setOnClickListener {
                  currentBooking?.let {
                      onAdd(it)
                  }
              }

            binding.rejectButton.setOnClickListener {
                currentBooking?.let {
                    onDelete(it)
                }
            }
        }


        fun bind(booking: Booking) {
            currentBooking = booking

            if (!booking.userPhoto.isNullOrEmpty())
                loadImage(binding.imageUser, booking.userPhoto)


            binding.stopsViewPass.setListStop(booking.stops)

            binding.name.text = booking.nickname
            val es = ctx.getString(R.string.price_details_trip)
            val pv = DecimalFormat("0.00").format(booking.price)
            binding.price.text = String.format(es, pv)

            if (booking.status == Status.Accepted) {
                binding.acceptButton.visibility = View.GONE
                binding.rejectButton.text = ctx.getString(R.string.remove)

                if (dateIsPassed(booking.stops[booking.stops.size-1].time!!)) {
                    binding.rejectButton.visibility = View.GONE
                    if (booking.passengerStars != 0.0f && booking.passengerStars != null) {
                        binding.rateButton.visibility = View.GONE
                        binding.ratingBar1.rating = booking.passengerStars
                        binding.ratingBar1.visibility = View.VISIBLE
                        if (!booking.passengerComment.isNullOrEmpty()) {
                            binding.comment.text = booking.passengerComment
                            binding.comment.visibility = View.VISIBLE
                        }
                    } else {
                        binding.ratingBar1.visibility = View.GONE
                        binding.comment.visibility = View.GONE
                        binding.rateButton.visibility = View.VISIBLE
                        binding.rateButton.setOnClickListener {
                            val action =
                                ManagePassengersFragmentDirections.actionManagePassengersFragmentToRatingsFragment(
                                    booking,
                                    Role.Passenger.toString()
                                )
                            Navigation.findNavController(itemView).navigate(action)
                        }
                    }
                }
            }

        }

        fun unbind() {
            binding.singleUser.setOnClickListener { null }
            binding.acceptButton.setOnClickListener { null }
            binding.rejectButton.setOnClickListener { null }
        }
    }

    // when the item become invisible (ghost item position), it will detached all this listener
    override fun onViewRecycled(holder: PassengerViewHolder) {
        super.onViewRecycled(holder)
        holder.unbind()
    }

    /**
     * Each item in the data set is presented in a recyclable visual hierarchy which is managed by a ViewHolder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PassengerViewHolder {

        // we want to inflate the corresponding layout
        val layoutInflater = LayoutInflater.from(parent.context)
        //inflate everything in manage_passenger_model
        val layout = layoutInflater.inflate(R.layout.passenger_layout, parent, false)

        return PassengerViewHolder(layout, onDelete, onAdd, openProfile)
    }

    override fun onBindViewHolder(holder: PassengerViewHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun getItemCount(): Int {
        return data.size
    }
}

