package com.example.nutrition_analysis

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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

    private val AZURE_OPENAI_API_URL = "https://x6-robertpalazzolo.openai.azure.com/openai/deployments/gpt-4o/chat/completions?api-version=2024-08-01-preview&api-key=7mwyy8WQXJ05TWsjI6uOm11GgUZFPu1v0S5A4rB1Co6bHaV9ihHoJQQJ99BAACYeBjFXJ3w3AAABACOGRNl2"
    private val GOOGLE_VISION_API_URL = "https://vision.googleapis.com/v1/images:annotate?key=AIzaSyAIuT8XaPMzF8sh_Sd5FTA-oXT-DyvnsN8\n "

    private lateinit var captureImageButton: Button
    private lateinit var viewRecordsButton: Button
    private lateinit var capturedImageView: ImageView
    private lateinit var visionApiResponse: TextView
    private lateinit var requestQueue: RequestQueue
    private lateinit var databaseHelper: DatabaseHelper
    private var lastCapturedImagePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        captureImageButton = findViewById(R.id.captureImageButton)
        viewRecordsButton = findViewById(R.id.viewRecordsButton)
        capturedImageView = findViewById(R.id.capturedImageView)
        visionApiResponse = findViewById(R.id.visionApiResponse)

        requestQueue = Volley.newRequestQueue(this)
        databaseHelper = DatabaseHelper(this)

        // Initially hide the ImageView and API response
        capturedImageView.visibility = View.GONE
        visionApiResponse.visibility = View.GONE

        captureImageButton.setOnClickListener { checkCameraPermission() }
        viewRecordsButton.setOnClickListener { viewSavedRecords() }
    }
    private fun viewSavedRecords() {
        val intent = Intent(this, ViewRecordsActivity::class.java)
        startActivity(intent)
    }
    private fun saveNutritionDetails(imagePath: String?, nutritionDetails: String) {
        if (!imagePath.isNullOrEmpty() && nutritionDetails.isNotEmpty()) {
            val success = databaseHelper.insertRecord(imagePath, nutritionDetails)
            if (success) {
                Toast.makeText(this, "Record saved successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to save record", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Invalid data. Unable to save.", Toast.LENGTH_SHORT).show()
        }
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
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show()
                openCamera()
            } else {
                Toast.makeText(this, "Camera permission is required to take pictures", Toast.LENGTH_LONG).show()
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

            if (photo != null) {
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
            val jsonRequest = JSONObject()
            val requestsArray = JSONArray()
            val requestObject = JSONObject()
            val imageObject = JSONObject()
            val featureObject = JSONObject()

            imageObject.put("content", base64Image)
            featureObject.put("type", "OBJECT_LOCALIZATION")
            featureObject.put("maxResults", 5)

            requestObject.put("image", imageObject)
            requestObject.put("features", JSONArray().put(featureObject))
            requestsArray.put(requestObject)
            jsonRequest.put("requests", requestsArray)

            val jsonObjectRequest = JsonObjectRequest(
                Request.Method.POST,
                GOOGLE_VISION_API_URL,
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
                visionApiResponse.text = "Unable to identify the object. Please retake the image."
                return
            }

            val detectedObject = localizedObjectAnnotations.getJSONObject(0).getString("name")

            if (detectedObject.equals("fruit", ignoreCase = true)
                || detectedObject.equals("food", ignoreCase = true)
                || detectedObject.equals("plant", ignoreCase = true)
            ) {
                visionApiResponse.text = "Unable to identify the exact item. Please retake the image."
            } else {
                visionApiResponse.text = "Detected object: $detectedObject"
                getNutritionDetailsFromAzureOpenAI(detectedObject)
            }

        } catch (e: JSONException) {
            e.printStackTrace()
            visionApiResponse.text = "Error processing image. Please retake it."
        }
    }

    private fun getNutritionDetailsFromAzureOpenAI(detectedObject: String) {
        val prompt = "Provide only the nutritional facts (calories, protein, fat, carbohydrates, fiber, sugars) of $detectedObject. " +
                "If it has no nutrition, say: '$detectedObject does not contain any nutritional value.' No additional details."

        val requestBody = JSONObject().apply {
            put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", "You are a nutrition assistant that provides precise answers."))
                .put(JSONObject().put("role", "user").put("content", prompt))
            )
            put("max_tokens", 150)
            put("temperature", 0.3)
        }

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST,
            AZURE_OPENAI_API_URL,
            requestBody,
            { response ->
                try {
                    val completion = response.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")

                    visionApiResponse.text = completion
                    lastCapturedImagePath?.let { saveNutritionDetails(it, completion) }
                } catch (e: JSONException) {
                    e.printStackTrace()
                    visionApiResponse.text = "Error parsing OpenAI response."
                }
            },
            { error -> visionApiResponse.text = "Failed to fetch nutrition details." }
        )

        requestQueue.add(jsonObjectRequest)
    }

    private fun encodeImageToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}
