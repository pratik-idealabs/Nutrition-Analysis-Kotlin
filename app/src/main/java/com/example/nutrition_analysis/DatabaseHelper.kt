package com.example.nutrition_analysis

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = "CREATE TABLE $TABLE_NAME (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_IMAGE_PATH TEXT, " +
                "$COLUMN_NUTRITION_DETAILS TEXT)"
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertRecord(imagePath: String, nutritionDetails: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_IMAGE_PATH, imagePath)
            put(COLUMN_NUTRITION_DETAILS, nutritionDetails)
        }

        val result = db.insert(TABLE_NAME, null, values)
        db.close()
        return result != -1L  // Return true if inserted successfully
    }

    fun getAllRecords(): List<NutritionRecord> {
        val records = mutableListOf<NutritionRecord>()
        val db = this.readableDatabase

        val cursor: Cursor? = db.rawQuery("SELECT * FROM $TABLE_NAME ORDER BY $COLUMN_ID DESC", null)

        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndexOrThrow(COLUMN_ID))
                val imagePath = it.getString(it.getColumnIndexOrThrow(COLUMN_IMAGE_PATH))
                val nutritionDetails = it.getString(it.getColumnIndexOrThrow(COLUMN_NUTRITION_DETAILS))

                records.add(NutritionRecord(id, imagePath, nutritionDetails))
            }
        }

        db.close()
        return records
    }

    fun clearAllRecords() {
        val db = this.writableDatabase
        db.delete(TABLE_NAME, null, null)
        db.close()
    }

    companion object {
        private const val DATABASE_NAME = "NutritionDB"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "nutrition_records"

        private const val COLUMN_ID = "id"
        private const val COLUMN_IMAGE_PATH = "image_path"
        private const val COLUMN_NUTRITION_DETAILS = "nutrition_details"
    }
}
