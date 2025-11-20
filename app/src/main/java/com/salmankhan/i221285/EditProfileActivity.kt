package com.salmankhan.i221285

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.salmankhan.i221285.services.UserService
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch

class EditProfileActivity : AppCompatActivity() {

    private lateinit var profileImageView: CircleImageView
    private lateinit var changePhotoText: TextView
    private lateinit var nameValue: TextView
    private lateinit var usernameValue: TextView
    private lateinit var websiteValue: TextView
    private lateinit var bioValue: TextView
    private lateinit var emailValue: TextView
    private lateinit var phoneValue: TextView
    private lateinit var genderValue: TextView

    private var currentUserId: String? = null
    private var selectedImageUri: Uri? = null

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                selectedImageUri = it
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
                profileImageView.setImageBitmap(bitmap)
                Toast.makeText(this, "Image selected. Click Done to save.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("EditProfile", "Error loading image: ${e.message}", e)
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(this, "Storage permission required to select image", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_profile)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize views
        profileImageView = findViewById(R.id.profile_picture)
        changePhotoText = findViewById(R.id.change_photo_text)
        nameValue = findViewById(R.id.name_value)
        usernameValue = findViewById(R.id.username_value)
        websiteValue = findViewById(R.id.website_value)
        bioValue = findViewById(R.id.bio_value)
        emailValue = findViewById(R.id.email_value)
        phoneValue = findViewById(R.id.phone_value)
        genderValue = findViewById(R.id.gender_value)

        val cancelButton = findViewById<TextView>(R.id.cancel_button)
        val doneButton = findViewById<TextView>(R.id.done_button)

        // Get current user
        currentUserId = AuthService.currentUser()?.uid
        if (currentUserId == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load current profile data
        loadProfileData()

        // Handle cancel button
        cancelButton.setOnClickListener {
            finish()
        }

        // Handle done button
        doneButton.setOnClickListener {
            saveProfileChanges()
        }

        // Handle profile picture change
        changePhotoText.setOnClickListener {
            checkPermissionAndOpenGallery()
        }

        profileImageView.setOnClickListener {
            checkPermissionAndOpenGallery()
        }

        // Handle field clicks to edit
        nameValue.setOnClickListener { showEditDialog("Name", nameValue.text.toString()) { newValue ->
            nameValue.text = newValue
        }}

        usernameValue.setOnClickListener { showEditDialog("Username", usernameValue.text.toString()) { newValue ->
            usernameValue.text = newValue
        }}

        websiteValue.setOnClickListener { showEditDialog("Website", websiteValue.text.toString()) { newValue ->
            websiteValue.text = newValue
        }}

        bioValue.setOnClickListener { showEditDialog("Bio", bioValue.text.toString()) { newValue ->
            bioValue.text = newValue
        }}

        emailValue.setOnClickListener { showEditDialog("Email", emailValue.text.toString()) { newValue ->
            emailValue.text = newValue
        }}

        phoneValue.setOnClickListener { showEditDialog("Phone", phoneValue.text.toString()) { newValue ->
            phoneValue.text = newValue
        }}

        genderValue.setOnClickListener {
            showGenderDialog()
        }
    }

    private fun loadProfileData() {
        lifecycleScope.launch {
            try {
                val userId = currentUserId ?: return@launch
                val result = UserService.getUserProfile(userId)
                
                if (result.isSuccess) {
                    val profile = result.getOrNull() ?: return@launch
                    
                    runOnUiThread {
                        // Set profile picture using Picasso for URL loading
                        val profileImageUrl = profile["profileImage"] as? String
                        if (!profileImageUrl.isNullOrEmpty() && profileImageUrl != "default") {
                            com.squareup.picasso.Picasso.get()
                                .load("http://192.168.100.189$profileImageUrl")
                                .placeholder(R.drawable.person1)
                                .error(R.drawable.person1)
                                .into(profileImageView)
                        }

                        // Set text fields
                        val firstName = profile["firstName"] as? String ?: ""
                        val lastName = profile["lastName"] as? String ?: ""
                        val fullName = "$firstName $lastName".trim()
                        nameValue.text = if (fullName.isEmpty()) "Add name" else fullName

                        usernameValue.text = profile["username"] as? String ?: "Add username"
                        websiteValue.text = profile["website"] as? String ?: "Add website"
                        bioValue.text = profile["bio"] as? String ?: "Add bio"
                        emailValue.text = profile["email"] as? String ?: "Add email"
                        phoneValue.text = profile["phone"] as? String ?: "Add phone"
                        genderValue.text = profile["gender"] as? String ?: "Prefer not to say"
                    }
                } else {
                    Log.e("EditProfile", "Error loading profile: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e("EditProfile", "Error loading profile: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this@EditProfileActivity, "Error loading profile", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveProfileChanges() {
        lifecycleScope.launch {
            try {
                val userId = currentUserId ?: return@launch
                
                // Parse name into firstName and lastName
                val fullName = nameValue.text.toString()
                val nameParts = fullName.split(" ", limit = 2)
                val firstName = nameParts.getOrNull(0) ?: ""
                val lastName = nameParts.getOrNull(1) ?: ""

                val username = usernameValue.text.toString()
                val website = if (websiteValue.text.toString() == "Add website") "" else websiteValue.text.toString()
                val bio = if (bioValue.text.toString() == "Add bio") "" else bioValue.text.toString()
                val email = if (emailValue.text.toString() == "Add email") "" else emailValue.text.toString()
                val phone = if (phoneValue.text.toString() == "Add phone") "" else phoneValue.text.toString()
                val gender = if (genderValue.text.toString() == "Prefer not to say") "" else genderValue.text.toString()

                runOnUiThread {
                    Toast.makeText(this@EditProfileActivity, "Saving profile...", Toast.LENGTH_SHORT).show()
                }

                // Upload profile image first if one was selected
                if (selectedImageUri != null) {
                    Log.d("EditProfile", "Uploading profile image...")
                    val imageUploadResult = UserService.uploadProfileImage(selectedImageUri!!)
                    if (imageUploadResult.isFailure) {
                        runOnUiThread {
                            Toast.makeText(this@EditProfileActivity, "Failed to upload image: ${imageUploadResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                        }
                        return@launch
                    }
                    Log.d("EditProfile", "Profile image uploaded successfully: ${imageUploadResult.getOrNull()}")
                }

                // Update profile with other fields
                val result = UserService.updateUserProfile(
                    userId = userId,
                    username = username,
                    firstName = firstName,
                    lastName = lastName,
                    bio = bio,
                    website = website,
                    email = email,
                    phone = phone,
                    gender = gender,
                    profileImage = null
                )

                runOnUiThread {
                    if (result.isSuccess) {
                        Toast.makeText(this@EditProfileActivity, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@EditProfileActivity, "Failed to update profile: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("EditProfile", "Error saving profile: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this@EditProfileActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showEditDialog(fieldName: String, currentValue: String, onSave: (String) -> Unit) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_field, null)
        val editText = dialogView.findViewById<EditText>(R.id.edit_field_input)
        editText.setText(if (currentValue.startsWith("Add ")) "" else currentValue)
        editText.hint = "Enter $fieldName"

        AlertDialog.Builder(this)
            .setTitle("Edit $fieldName")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newValue = editText.text.toString().trim()
                if (newValue.isNotEmpty()) {
                    onSave(newValue)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showGenderDialog() {
        val genders = arrayOf("Male", "Female", "Other", "Prefer not to say")
        AlertDialog.Builder(this)
            .setTitle("Select Gender")
            .setItems(genders) { _, which ->
                genderValue.text = genders[which]
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun checkPermissionAndOpenGallery() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                openGallery()
            }
            else -> {
                permissionLauncher.launch(permission)
            }
        }
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }
}