package com.abedelazizshe.lightcompressor

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
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
        val newSize = "Size after compression: ${itemsViewModel.newSize}"
        val progress = "${itemsViewModel.progress.toLong()}%"

        if (itemsViewModel.progress > 0 && itemsViewModel.progress < 100) {
            holder.progress.visibility = View.VISIBLE
            holder.progress.text = progress

            holder.progressBar.visibility = View.VISIBLE
            holder.progressBar.progress = itemsViewModel.progress.toInt()
        } else {
            holder.progress.visibility = View.GONE
            holder.progressBar.visibility = View.GONE
        }

        if (itemsViewModel.newSize.isNotBlank()) {
            holder.newSize.text = newSize
            holder.newSize.visibility = View.VISIBLE
        } else {
            holder.newSize.visibility = View.GONE
        }

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
        val newSize: TextView = itemView.findViewById(R.id.newSize)
        val progress: TextView = itemView.findViewById(R.id.progress)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
    }
}
