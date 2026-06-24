package com.example.mygallery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.io.File

class MediaAdapter(
    private val groupedItems: List<Pair<String, List<MediaItem>>>,
    private val onItemClick: (MediaItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        var pos = position
        for (group in groupedItems) {
            if (pos == 0) return TYPE_HEADER
            pos--
            if (pos < group.second.size) return TYPE_ITEM
            pos -= group.second.size
        }
        return TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_header, parent, false)
                HeaderViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_media, parent, false)
                MediaViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        var pos = position
        for (group in groupedItems) {
            if (pos == 0) {
                if (holder is HeaderViewHolder) {
                    holder.bind(group.first)
                }
                return
            }
            pos--
            if (pos < group.second.size) {
                if (holder is MediaViewHolder) {
                    holder.bind(group.second[pos], onItemClick)
                }
                return
            }
            pos -= group.second.size
        }
    }

    override fun getItemCount(): Int {
        var count = 0
        groupedItems.forEach { group ->
            count += 1
            count += group.second.size
        }
        return count
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val headerText: TextView = itemView.findViewById(R.id.headerText)

        fun bind(date: String) {
            headerText.text = date
        }
    }

    class MediaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageView)
        private val videoIcon: ImageView = itemView.findViewById(R.id.videoIcon)

        fun bind(item: MediaItem, onItemClick: (MediaItem) -> Unit) {
            Glide.with(itemView.context)
                .load(File(item.path))
                .centerCrop()
                .thumbnail(0.5f)
                .into(imageView)

            videoIcon.visibility = if (item.type == MediaType.VIDEO) View.VISIBLE else View.GONE

            itemView.setOnClickListener { onItemClick(item) }
        }
    }
}
