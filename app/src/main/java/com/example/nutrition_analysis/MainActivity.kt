package com.example.nutrition_analysis

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val CAMERA_PERMISSION_CODE = 100
    private val CAMERA_INTENT_CODE = 101

    private lateinit var captureImageButton: Button
    private lateinit var viewRecordsButton: Button
    private lateinit var capturedImageView: ImageView
    private lateinit var visionApiResponse: TextView
    private lateinit var requestQueue: RequestQueue
    private lateinit var databaseHelper: DatabaseHelper
    private var lastCapturedImagePath: String? = null

    private val GOOGLE_VISION_API_KEY = ApiKeys.GOOGLE_VISION_API_KEY
    private val AZURE_OPENAI_API_KEY =  ApiKeys.AZURE_OPENAI_API_KEY
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        captureImageButton = findViewById(R.id.captureImageButton)
        viewRecordsButton = findViewById(R.id.viewRecordsButton)
        capturedImageView = findViewById(R.id.capturedImageView)
        visionApiResponse = findViewById(R.id.visionApiResponse)

        requestQueue = Volley.newRequestQueue(this)
        databaseHelper = DatabaseHelper(this)

        capturedImageView.visibility = View.GONE
        visionApiResponse.visibility = View.GONE

        captureImageButton.setOnClickListener { checkCameraPermission() }
        viewRecordsButton.setOnClickListener { viewSavedRecords() }
    }

    private fun viewSavedRecords() {
        val intent = Intent(this, ViewRecordsActivity::class.java)
        startActivity(intent)
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        } else {
            openCamera()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, CAMERA_INTENT_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_INTENT_CODE && resultCode == RESULT_OK && data != null) {
            val photo = data.extras?.get("data") as Bitmap?
            photo?.let {
                capturedImageView.visibility = View.VISIBLE
                visionApiResponse.visibility = View.VISIBLE
                captureImageButton.text = "Take another Image"
                capturedImageView.setImageBitmap(photo)

                lastCapturedImagePath = saveImageToInternalStorage(photo)
                sendImageToGoogleVision(photo)
            }
        }
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap): String? {
        val directory = filesDir
        val fileName = "captured_${System.currentTimeMillis()}.jpg"
        val imageFile = File(directory, fileName)

        return try {
            FileOutputStream(imageFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            }
            imageFile.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun sendImageToGoogleVision(bitmap: Bitmap) {
        val base64Image = encodeImageToBase64(bitmap)

        try {
            val jsonRequest = JSONObject().apply {
                put("requests", JSONArray().put(JSONObject().apply {
                    put("image", JSONObject().put("content", base64Image))
                    put("features", JSONArray().put(JSONObject().apply {
                        put("type", "OBJECT_LOCALIZATION")
                        put("maxResults", 5)
                    }))
                }))
            }

            val jsonObjectRequest = JsonObjectRequest(
                Request.Method.POST,
                GOOGLE_VISION_API_KEY,
                jsonRequest,
                { response -> handleVisionResponse(response) },
                { error -> Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show() }
            )

            requestQueue.add(jsonObjectRequest)

        } catch (e: JSONException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to create request: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleVisionResponse(response: JSONObject) {
        try {
            val localizedObjectAnnotations = response
                .getJSONArray("responses")
                .getJSONObject(0)
                .optJSONArray("localizedObjectAnnotations")

            if (localizedObjectAnnotations == null || localizedObjectAnnotations.length() == 0) {
                visionApiResponse.text = "Unable to identify the object."
                return
            }

            val detectedObject = localizedObjectAnnotations.getJSONObject(0).getString("name")
            visionApiResponse.text = "Detected object: $detectedObject"
            getNutritionDetailsFromAzureOpenAI(detectedObject)

        } catch (e: JSONException) {
            e.printStackTrace()
            visionApiResponse.text = "Error processing image."
        }
    }

    private fun getNutritionDetailsFromAzureOpenAI(detectedObject: String) {
        val requestBody = JSONObject().apply {
            put("messages", JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", "You are a nutrition assistant."))
                put(JSONObject().put("role", "user").put("content", "Give nutrition info for $detectedObject."))
            })
            put("max_tokens", 150)
            put("temperature", 0.3)
        }

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST,
            AZURE_OPENAI_API_KEY,
            requestBody,
            { response -> visionApiResponse.text = response.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content") },
            { error -> visionApiResponse.text = "Failed to fetch nutrition details." }
        )

        requestQueue.add(jsonObjectRequest)
    }

    private fun encodeImageToBase64(bitmap: Bitmap): String {
        return ByteArrayOutputStream().apply {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, this)
        }.toByteArray().let { Base64.encodeToString(it, Base64.NO_WRAP) }
    }
}
