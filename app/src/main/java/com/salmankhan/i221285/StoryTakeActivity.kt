package com.salmankhan.i221285

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class StoryTakeActivity : AppCompatActivity() {
    
    private var capturedImageUri: Uri? = null
    private var capturedBitmap: Bitmap? = null
    
    companion object {
        // Static variable to temporarily store captured image Base64
        var tempCapturedImageBase64: String? = null
    }
    
    // Camera permission launcher
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "Camera permission required to take stories", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Camera launcher
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && capturedImageUri != null) {
            // Convert captured image to bitmap
            try {
                var bitmap = MediaStore.Images.Media.getBitmap(contentResolver, capturedImageUri)
                if (bitmap != null) {
                    // Fix image orientation based on EXIF data
                    bitmap = StoryService.fixImageOrientation(bitmap, capturedImageUri, contentResolver)
                    
                    capturedBitmap = bitmap
                    // Convert bitmap to Base64 and store in companion object (to avoid Intent size limit)
                    tempCapturedImageBase64 = StoryService.bitmapToBase64(bitmap)
                    
                    // Navigate to edit screen
                    val intent = Intent(this, StoryEditOwnActivity::class.java)
                    intent.putExtra("hasImage", true)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error processing image: ${e.message}", Toast.LENGTH_SHORT).show()
                android.util.Log.e("StoryTakeActivity", "Image processing error: ${e.message}", e)
            }
        } else {
            Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_story_take)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val takePhotoButton = findViewById<ImageView>(R.id.capture_button)
        takePhotoButton.setOnClickListener {
            checkCameraPermissionAndCapture()
        }

        // Back arrow → Previous screen
        val backIcon = findViewById<ImageView>(R.id.close_icon)
        backIcon.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
    
    private fun checkCameraPermissionAndCapture() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CAMERA
            ) -> {
                Toast.makeText(this, "Camera permission is required to take stories", Toast.LENGTH_LONG).show()
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
    
    private fun openCamera() {
        try {
            // Create a temporary file for the captured image
            val photoFile = createImageFile()
            capturedImageUri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )
            capturedImageUri?.let { uri ->
                cameraLauncher.launch(uri)
            } ?: run {
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening camera: ${e.message}", Toast.LENGTH_SHORT).show()
            android.util.Log.e("StoryTakeActivity", "Camera error: ${e.message}", e)
        }
    }
    
    private fun createImageFile(): java.io.File {
        val timeStamp = System.currentTimeMillis()
        val imageFileName = "STORY_${timeStamp}"
        val storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        return java.io.File.createTempFile(imageFileName, ".jpg", storageDir)
    }
}