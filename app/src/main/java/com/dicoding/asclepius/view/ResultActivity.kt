package com.dicoding.asclepius.view

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.dicoding.asclepius.R
import com.dicoding.asclepius.database.DatabaseApp
import com.dicoding.asclepius.database.HistoryPrediction
import com.dicoding.asclepius.databinding.ActivityResultBinding
import com.dicoding.asclepius.helper.ImageClassifierHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.tensorflow.lite.task.vision.classifier.Classifications
import java.io.File
import java.io.FileOutputStream

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding
    companion object {
        const val IMAGE_URI = "img_uri"
        const val TAG = "imagePicker"
        const val RESULT_TEXT = "result_text"
        const val REQUEST_HISTORY_UPDATE = 1
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val imageUriString = intent.getStringExtra(IMAGE_URI)
        if (imageUriString != null) {
            val imageUri = Uri.parse(imageUriString)
            displayImage(imageUri)

            val imageClassifierHelper = ImageClassifierHelper(
                contextValue = this,
                classifierListenerValue = object : ImageClassifierHelper.ClassifierListener {
                    override fun onError(errorMessage: String) {
                        Log.d(TAG, "Error: $errorMessage")
                        showToast("Image analysis failed: $errorMessage")
                    }
                    override fun onResults(results: List<Classifications>?, inferenceTime: Long) {
                        results?.let { showResults(it) }
                    }
                }
            )
            imageClassifierHelper.classifyStaticImage(imageUri)
        } else {
            Log.e(TAG, "No image URI provided")
            showToast("No image URI provided")
            finish()
        }
        binding.saveButton.setOnClickListener {
            val imageUriString = intent.getStringExtra(IMAGE_URI)
            val result = binding.resultText.text.toString()

            if (imageUriString != null) {
                val imageUri = Uri.parse(imageUriString)
                showToast("Data saved")
                savePredictionToDatabase(imageUri, result)
            } else {
                showToast("No image URI provided")
                finish()
            }
        }
    }
    private fun showResults(results: List<Classifications>) {
        val topResult = results[0]
        val label = topResult.categories[0].label
        val score = topResult.categories[0].score

        fun Float.formatToString(): String {
            return String.format("%.2f%%", this * 100)
        }
        binding.resultText.text = "$label ${score.formatToString()}"
    }

    private fun moveToHistory(imageUri: Uri, result: String) {
        val intent = Intent(this, ActivityHistory::class.java)
        intent.putExtra(RESULT_TEXT, result)
        intent.putExtra(IMAGE_URI, imageUri.toString())
        setResult(RESULT_OK, intent)
        startActivity(intent)
        finish()
    }
    private fun displayImage(uri: Uri) {
        Log.d(TAG, "Displaying image: $uri")
        binding.resultImage.setImageURI(uri)
    }
    private fun savePredictionToDatabase(imageUri: Uri, result: String) {
        if (result.isNotEmpty()) {
            val fileName = "cropped_image_${System.currentTimeMillis()}.jpg"
            val destinationUri = Uri.fromFile(File(cacheDir, fileName))
            try {
                contentResolver.openInputStream(imageUri)?.use { input ->
                    FileOutputStream(File(cacheDir, fileName)).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to copy file: $e")
                showToast("Failed to save image")
                return
            }
            val prediction = HistoryPrediction(imagePath = destinationUri.toString(), result = result)
            GlobalScope.launch(Dispatchers.IO) {
                val database = DatabaseApp.getDatabase(applicationContext)
                try {
                    database.historyPredictionDao().predictionInsert(prediction)
                    Log.d(TAG, "Prediction saved successfully: $prediction")
                    val predictions = database.historyPredictionDao().getAllPrediction()
                    Log.d(TAG, "All predictions after save: $predictions")
                    moveToHistory(destinationUri, result)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save prediction: $prediction", e)
                    runOnUiThread {
                        showToast("Failed to save prediction to database")
                    }
                }
            }
        } else {
            Log.e(TAG, "Result is empty, cannot save prediction to database.")
            showToast("Result is empty, cannot save prediction to database.")
        }
    }
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

}