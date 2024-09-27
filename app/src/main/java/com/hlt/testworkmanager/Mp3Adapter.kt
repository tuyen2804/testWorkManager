package com.hlt.testworkmanager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class Mp3Adapter(private var mp3Files: List<File>) : RecyclerView.Adapter<Mp3Adapter.Mp3ViewHolder>() {

    class Mp3ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fileNameTextView: TextView = itemView.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Mp3ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
        return Mp3ViewHolder(view)
    }

    override fun onBindViewHolder(holder: Mp3ViewHolder, position: Int) {
        holder.fileNameTextView.text = mp3Files[position].name
    }

    override fun getItemCount(): Int = mp3Files.size

    fun updateData(newFiles: List<File>) {
        mp3Files = newFiles
        notifyDataSetChanged()
    }
}
