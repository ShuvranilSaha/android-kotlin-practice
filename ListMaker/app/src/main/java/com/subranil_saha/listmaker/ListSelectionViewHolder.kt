package com.subranil_saha.listmaker

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ListSelectionViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
    val listPosition: TextView = itemView.findViewById(R.id.itemNumber)
    val listTitle: TextView = itemView.findViewById(R.id.itemString)
}