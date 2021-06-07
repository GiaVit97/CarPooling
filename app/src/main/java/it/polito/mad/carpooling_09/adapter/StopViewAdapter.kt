package it.polito.mad.carpooling_09.adapter

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.GeoPoint
import it.polito.mad.carpooling_09.R
import it.polito.mad.carpooling_09.data.Stop
import it.polito.mad.carpooling_09.data.Trip
import it.polito.mad.carpooling_09.data.TripFilter
import it.polito.mad.carpooling_09.databinding.StopViewLayoutBinding
import it.polito.mad.carpooling_09.utils.convertUTCToString


class StopViewAdapter(
    val data: List<Trip>,
    val departureLocation: GeoPoint?,
    val arrivalLocation: GeoPoint?
) :
    RecyclerView.Adapter<StopViewAdapter.TripViewHolder>() {

    inner class TripViewHolder(v: View) : RecyclerView.ViewHolder(v) {

        val binding = StopViewLayoutBinding.bind(v)

        // bind take an actual piece of data and use it to fill the view
        fun bind(trip: Trip) {




            binding.stopsView.setListStop(trip.stops, departureLocation, arrivalLocation)

        }

    }

    // when the item become invisible (ghost item position), it will detached all this listener
    override fun onViewRecycled(holder: TripViewHolder) {
        super.onViewRecycled(holder)
    }

    /**
     * Each item in the data set is presented in a recyclable visual hierarchy which is managed by a ViewHolder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {

        // we want to inflate the corresponding layout
        val layoutInflater = LayoutInflater.from(parent.context)
        val layout = layoutInflater.inflate(R.layout.stop_view_layout, parent, false)

        return TripViewHolder(layout)
    }

    override fun onBindViewHolder(holder: StopViewAdapter.TripViewHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.stop_view_layout
    }
}
