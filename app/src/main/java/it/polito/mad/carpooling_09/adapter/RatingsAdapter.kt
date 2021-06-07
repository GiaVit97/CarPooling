package it.polito.mad.carpooling_09.adapter

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import it.polito.mad.carpooling_09.data.Ratings
import it.polito.mad.carpooling_09.R
import it.polito.mad.carpooling_09.data.Booking
import it.polito.mad.carpooling_09.data.User
import it.polito.mad.carpooling_09.databinding.RatingLayoutProfileBinding
import it.polito.mad.carpooling_09.utils.convertUTCToString
import it.polito.mad.carpooling_09.utils.loadImage
import java.io.File
import java.io.FileInputStream
import java.util.*

class RatingsAdapter(
    private val data: ArrayList<Ratings>,
    private var openProfile: (String) -> Unit
) :
    RecyclerView.Adapter<RatingsAdapter.RatingsViewHolder>() {


    inner class RatingsViewHolder(v: View, private var openProfile: (String) -> Unit) :
        RecyclerView.ViewHolder(v) {

        val binding = RatingLayoutProfileBinding.bind(v)

        private var currentRating: Ratings? = null

        init {
            binding.cvRate.setOnClickListener {
                currentRating?.let { rating ->
                    rating.userId?.let {
                        openProfile(it)
                    }
                }
            }
        }


        fun bind(rating: Ratings) {
            currentRating = rating


            binding.tvNickname.text = rating.nickname
            binding.rbRating.rating = rating.star ?: 0f
            binding.tvDescription.text = rating.comment
            binding.tvDate.text = convertUTCToString(rating.date)
            rating.url?.let {
                loadImage(binding.imageUser, it)
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RatingsViewHolder {

        val layoutInflater = LayoutInflater.from(parent.context)
        val layout = layoutInflater.inflate(R.layout.rating_layout_profile, parent, false)

        return RatingsViewHolder(layout, openProfile)
    }

    override fun onBindViewHolder(holder: RatingsViewHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.rating_layout_profile
    }
}