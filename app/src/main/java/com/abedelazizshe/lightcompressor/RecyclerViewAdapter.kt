package com.abedelazizshe.lightcompressor

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class RecyclerViewAdapter(private val context: Context, private val list: List<VideoDetailsModel>) :
    RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_view_item, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val itemsViewModel = list[position]
        val originalSize = "Original size: ${itemsViewModel.originalSize}"
        val newSize = "Size after compression: ${itemsViewModel.newSize}"
        val timeTaken = "Duration: ${itemsViewModel.timeTaken}"

        holder.originalSize.text = originalSize
        holder.newSize.text = newSize
        holder.timeTaken.text = timeTaken
        Glide.with(context).load(itemsViewModel.uri).into(holder.videoImage)

        holder.itemView.setOnClickListener {
            VideoPlayerActivity.start(
                it.context,
                itemsViewModel.playableVideoPath
            )
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val videoImage: ImageView = itemView.findViewById(R.id.videoImage)
        val originalSize: TextView = itemView.findViewById(R.id.originalSize)
        val newSize: TextView = itemView.findViewById(R.id.newSize)
        val timeTaken: TextView = itemView.findViewById(R.id.timeTaken)
    }
}
