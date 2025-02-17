package com.example.nutrition_analysis

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.nutrition_analysis.R
import java.io.File

class RecordAdapter(private val recordList: List<NutritionRecord>) :
    RecyclerView.Adapter<RecordAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = recordList[position]

        // Set the nutrition details
        holder.nutritionDetailsTextView.text = record.nutritionDetails

        // Set the image (if the path is valid)
        val imgFile = File(record.imagePath)
        if (imgFile.exists()) {
            val bitmap: Bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
            holder.imageView.setImageBitmap(bitmap)
        } else {
            holder.imageView.setImageResource(android.R.drawable.ic_menu_report_image) // Placeholder image
        }
    }

    override fun getItemCount(): Int = recordList.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val nutritionDetailsTextView: TextView = itemView.findViewById(R.id.nutritionDetailsTextView)
    }
}
