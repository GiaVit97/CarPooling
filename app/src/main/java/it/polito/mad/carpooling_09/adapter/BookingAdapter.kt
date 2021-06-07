package it.polito.mad.carpooling_09.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import it.polito.mad.carpooling_09.R
import it.polito.mad.carpooling_09.data.Booking
import it.polito.mad.carpooling_09.data.LongLat
import it.polito.mad.carpooling_09.data.Role
import it.polito.mad.carpooling_09.data.Status
import it.polito.mad.carpooling_09.databinding.BookingLayoutBinding
import it.polito.mad.carpooling_09.ui.interestedTrips.BoughtTripsListFragmentDirections
import it.polito.mad.carpooling_09.ui.interestedTrips.TripsOfInterestListFragmentDirections
import it.polito.mad.carpooling_09.utils.dateIsPassed
import java.text.DecimalFormat
import java.text.Format
import java.text.SimpleDateFormat
import java.util.*

class BookingAdapter(val data: List<Booking>, val boughtTrip: Boolean) :
    RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    inner class BookingViewHolder(v: View) : RecyclerView.ViewHolder(v) {

        val binding = BookingLayoutBinding.bind(v)

        fun bind(p: Booking) {

            val format: Format = SimpleDateFormat("EEE MMMM dd, yyyy", Locale.UK)

            binding.tripDate.text = format.format(p.stops[0].time)

            binding.stopsView.setListStop(p.stops)

            binding.priceBooking.text = "€ ${DecimalFormat("0.00").format(p.price)}"


            binding.singleBooking.setOnClickListener {
                val action = if(boughtTrip) BoughtTripsListFragmentDirections
                    .actionBoughtTripsListFragmentToTripDetailsFragment(p.tripId!!, p.userId!!,
                        LongLat(
                        p.stops[0].location?.latitude,
                            p.stops[0].location?.longitude),
                        LongLat(
                        p.stops[p.stops.size-1].location?.latitude,
                        p.stops[p.stops.size-1].location?.longitude), p.status.status)
                else TripsOfInterestListFragmentDirections
                    .actionTripsOfInterestListFragmentToTripDetailsFragment(p.tripId!!, p.userId!!,
                        LongLat(
                            p.stops[0].location?.latitude,
                            p.stops[0].location?.longitude),
                        LongLat(
                            p.stops[p.stops.size-1].location?.latitude,
                            p.stops[p.stops.size-1].location?.longitude), p.status.status)

                Navigation.findNavController(it).navigate(action)
            }

            // Show the button only if the trip is terminated and doesn't have a review yet
            if(dateIsPassed(p.stops[p.stops.size-1].time!!) && p.status==Status.Accepted){
                if(p.driverStars!=0.0f){
                    binding.reviewButton.visibility = View.GONE
                    binding.ratingBar.rating = p.driverStars
                    binding.ratingBar.visibility = View.VISIBLE
                    if(!p.driverComment.isNullOrEmpty()){
                        binding.comment.text = p.driverComment
                        binding.comment.visibility = View.VISIBLE
                    }
                }else{
                    binding.ratingBar.visibility = View.GONE
                    binding.comment.visibility = View.GONE
                    binding.reviewButton.visibility = View.VISIBLE
                    binding.reviewButton.setOnClickListener{
                        val action =
                            BoughtTripsListFragmentDirections.actionBoughtTripsListFragmentToRatingsFragment(
                                p,
                                Role.Driver.toString()
                            )
                        Navigation.findNavController(itemView).navigate(action)
                    }
                }
            }
        }

        fun unbind() {

        }

    }

    override fun onViewRecycled(holder: BookingViewHolder) {
        super.onViewRecycled(holder)
        holder.unbind()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {

        val layoutInflater = LayoutInflater.from(parent.context)
        val layout = layoutInflater.inflate(R.layout.booking_layout, parent, false)

        return BookingViewHolder(layout)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.booking_layout
    }

}