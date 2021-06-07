package it.polito.mad.carpooling_09.adapter

import android.app.TimePickerDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView
import it.polito.mad.carpooling_09.R
import it.polito.mad.carpooling_09.data.Stop
import it.polito.mad.carpooling_09.databinding.StopEditLayoutBinding
import it.polito.mad.carpooling_09.utils.convertStringToDate
import it.polito.mad.carpooling_09.utils.convertUTCToString
import okhttp3.internal.UTC
import java.util.*


class StopEditAdapter(
    val data: MutableList<Stop>, private var onDelete: (Stop) -> Unit,
    private var openMap: (com.google.firebase.firestore.GeoPoint?, Int) -> Unit,
    private var openTimePicker: (TextView, Int) -> Unit
) :
    RecyclerView.Adapter<StopEditAdapter.StopEditHolder>() {

    private lateinit var context: Context
    private var adapterStopList: MutableList<Stop> = data

    inner class StopEditHolder(
        v: View,
        private var onDelete: (Stop) -> Unit,
        private var openMap: (com.google.firebase.firestore.GeoPoint?, Int) -> Unit,
        private var openTimePicker: (TextView, Int) -> Unit
    ) :
        RecyclerView.ViewHolder(v) {

        val binding = StopEditLayoutBinding.bind(v)

        // bind take an actual piece of data and use it to fill the view
        fun bind(s: Stop) {


            if (!s.name.isNullOrEmpty())
                binding.locationName.setText(s.name)


            binding.locationName.setOnClickListener {
                openMap(s.location, bindingAdapterPosition)
            }

            binding.refuseButton.setOnClickListener {
                onDelete(s)
            }

        }

        fun unbind() {
            binding.locationName.text = null
            binding.locationName.setOnClickListener { }
            binding.refuseButton.setOnClickListener { }
        }

    }

    // when the item become invisible (ghost item position), it will detached all this listener
    override fun onViewRecycled(holder: StopEditHolder) {
        super.onViewRecycled(holder)
        holder.unbind()
    }

    /**
     * Each item in the data set is presented in a recyclable visual hierarchy which is managed by a ViewHolder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StopEditHolder {

        // we want to inflate the corresponding layout
        val layoutInflater = LayoutInflater.from(parent.context)
        val layout = layoutInflater.inflate(R.layout.stop_edit_layout, parent, false)
        context = parent.context

        return StopEditHolder(layout, onDelete, openMap, openTimePicker)
    }

    override fun onBindViewHolder(holder: StopEditAdapter.StopEditHolder, position: Int) {
        holder.bind(adapterStopList[position])


    }

    override fun getItemCount(): Int {
        return adapterStopList.size
    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.stop_edit_layout
    }

    fun getUpdatedStopList(): MutableList<Stop> {
        return adapterStopList
    }
}