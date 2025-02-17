package com.example.nutrition_analysis

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.nutrition_analysis.R

class ViewRecordsActivity : AppCompatActivity() {

    private lateinit var recordsRecyclerView: RecyclerView
    private lateinit var adapter: RecordAdapter
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var clearRecordsButton: Button
    private var recordList: MutableList<NutritionRecord> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_records)

        recordsRecyclerView = findViewById(R.id.recordsRecyclerView)
        recordsRecyclerView.layoutManager = LinearLayoutManager(this)

        clearRecordsButton = findViewById(R.id.clearRecordsButton)
        databaseHelper = DatabaseHelper(this)

        // Load and display the records initially
        loadRecords()

        // Clear all records when the button is clicked
        clearRecordsButton.setOnClickListener { clearAllRecords() }
    }

    private fun loadRecords() {
        recordList = databaseHelper.getAllRecords().toMutableList()
        adapter = RecordAdapter(recordList)
        recordsRecyclerView.adapter = adapter
    }

    private fun clearAllRecords() {
        if (recordList.isEmpty()) {
            Toast.makeText(this, "No records to clear.", Toast.LENGTH_SHORT).show()
            return
        }

        databaseHelper.clearAllRecords()
        recordList.clear()
        adapter.notifyDataSetChanged()  // Refresh the RecyclerView
        Toast.makeText(this, "All records cleared successfully.", Toast.LENGTH_SHORT).show()
    }
}
