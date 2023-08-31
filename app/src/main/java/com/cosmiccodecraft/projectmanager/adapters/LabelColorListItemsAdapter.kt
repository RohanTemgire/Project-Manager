package com.cosmiccodecraft.projectmanager.adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cosmiccodecraft.projectmanager.R

class LabelColorListItemsAdapter(
    private val context: Context,
    private val list: ArrayList<String>,
    private val mSelectedColor: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var onItemClickListener: OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.item_label_color,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = list[position]
        if(holder is ViewHolder){
            holder.itemView.findViewById<View>(R.id.view_main).setBackgroundColor(Color.parseColor(item))
            if(item == mSelectedColor){
                holder.itemView.findViewById<View>(R.id.iv_selected_color).visibility = View.VISIBLE
            }else{
                holder.itemView.findViewById<View>(R.id.iv_selected_color).visibility = View.GONE
            }

            holder.itemView.findViewById<View>(R.id.view_main).setOnClickListener {
                if(onItemClickListener != null){
                    onItemClickListener!!.onClick(position, item)
                }
            }
        }
    }

    private class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    interface OnItemClickListener {
        fun onClick(position: Int, color: String)
    }
}