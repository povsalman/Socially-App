package com.salmankhan.i221285

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.salmankhan.i221285.services.PostService
import kotlinx.coroutines.launch

class PostImageFragment : Fragment() {
    
    private var selectedImageBitmap: Bitmap? = null
    private var previewImageView: ImageView? = null
    
    companion object {
        // Temporary storage for selected image
        var tempSelectedImageBase64: String? = null
    }
    
    // Gallery launcher
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                selectedImageBitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
                if (selectedImageBitmap != null) {
                    // Display selected image
                    previewImageView?.setImageBitmap(selectedImageBitmap)
                    
                    // Store in temp variable
                    tempSelectedImageBase64 = PostService.bitmapToBase64(selectedImageBitmap!!)
                    
                    Toast.makeText(context, "Image selected! Tap Next to add caption", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("PostImageFragment", "Error loading image: ${e.message}", e)
            }
        }
    }
    
    // Permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(context, "Storage permission required to select images", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_post_image, container, false)
        
        // Get preview image view
        previewImageView = view.findViewById(R.id.post_preview_image)
        
        // Handle image selection (tap on preview or select multiple text)
        previewImageView?.setOnClickListener {
            checkPermissionAndOpenGallery()
        }
        
        view.findViewById<TextView>(R.id.select_multiple_text)?.setOnClickListener {
            checkPermissionAndOpenGallery()
        }
        
        // Handle Photo button - open gallery (same as clicking on preview)
        view.findViewById<TextView>(R.id.photo_button)?.setOnClickListener {
            checkPermissionAndOpenGallery()
        }
        
        // Handle Next button - navigate to caption screen
        view.findViewById<TextView>(R.id.next_button)?.setOnClickListener {
            if (tempSelectedImageBase64 != null) {
                // Navigate to post caption activity (we'll create this)
                val intent = Intent(requireContext(), PostCaptionActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(context, "Please select an image first", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Handle Cancel button
        view.findViewById<TextView>(R.id.cancel_button)?.setOnClickListener {
            // Clear temp data and go back
            tempSelectedImageBase64 = null
            selectedImageBitmap = null
            requireActivity().supportFragmentManager.popBackStack()
        }
        
        return view
    }
    
    private fun checkPermissionAndOpenGallery() {
        // For Android 13+ (API 33+), use READ_MEDIA_IMAGES
        // For Android 12 and below, use READ_EXTERNAL_STORAGE
        val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                openGallery()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                Toast.makeText(context, "Storage permission is required to select images", Toast.LENGTH_LONG).show()
                permissionLauncher.launch(permission)
            }
            else -> {
                permissionLauncher.launch(permission)
            }
        }
    }
    
    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        previewImageView = null
    }
}