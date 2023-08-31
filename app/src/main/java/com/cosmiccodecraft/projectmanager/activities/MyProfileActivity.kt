package com.cosmiccodecraft.projectmanager.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.cosmiccodecraft.projectmanager.R
import com.cosmiccodecraft.projectmanager.firebase.FireStore
import com.cosmiccodecraft.projectmanager.models.User
import com.cosmiccodecraft.projectmanager.utils.Constants
import com.cosmiccodecraft.projectmanager.utils.Constants.PICK_IMAGE_REQUEST_CODE
import com.cosmiccodecraft.projectmanager.utils.Constants.READ_EXTERNAL_STORAGE_CODE
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class MyProfileActivity : BaseActivity() {

    private var mSelectedImageFileUri: Uri? = null
    private var mProfileImageURL: String = ""
    private lateinit var mUserDetails: User

    private lateinit var userName: EditText
    private lateinit var phoneNumber: EditText
    private lateinit var email: EditText
    private lateinit var image: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_profile)

        setActionBar()

        userName = findViewById(R.id.et_name)
        phoneNumber = findViewById(R.id.et_mobile)
        email = findViewById(R.id.et_email)
        image = findViewById(R.id.iv_profile_user_image)

        FireStore().loadUserData(this)

        image.setOnClickListener {
            checkPermission()
        }

        val updateBtn = findViewById<Button>(R.id.btn_update)

        updateBtn.setOnClickListener {
            if (mSelectedImageFileUri != null) {
                uploadUserImage()
            } else {
                showProgressDialog(resources.getString(R.string.please_wait))
                updateUserProfileData()
            }
        }
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                Constants.showImageChooser(this)
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    READ_EXTERNAL_STORAGE_CODE
                )
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                Constants.showImageChooser(this)
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                    READ_EXTERNAL_STORAGE_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == READ_EXTERNAL_STORAGE_CODE
                && grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                Constants.showImageChooser(this@MyProfileActivity)
            } else {
                Toast.makeText(
                this,
                "You denied the permission please allow it from the settings",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK
            && requestCode == PICK_IMAGE_REQUEST_CODE
            && data!!.data != null
        ) {
            mSelectedImageFileUri = data.data
            try {
                Glide
                    .with(this)
                    .load(Uri.parse(mSelectedImageFileUri.toString()))
                    .centerCrop()
                    .placeholder(R.drawable.ic_image_place_holder)
                    .into(image)
                    .onLoadFailed(resources.getDrawable(R.drawable.ic_image_place_holder))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    private fun setActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_my_profile_activity)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back_button_white)
            actionBar.title = resources.getString(R.string.my_profile_title)
        }

        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    fun setUserDataInUI(user: User) {

        mUserDetails = user

        Glide
            .with(this)
            .load(user.image)
            .centerCrop()
            .placeholder(R.drawable.ic_image_place_holder)
            .into(image)
            .onLoadFailed(resources.getDrawable(R.drawable.ic_image_place_holder))

        userName.setText(user.name)
        email.setText(user.email)
        if (user.mobile != 0L)
            phoneNumber.setText(user.mobile.toString())

    }

    private fun updateUserProfileData() {
        val userHashMap = HashMap<String, Any>()
        var changesMade = false
        if (mProfileImageURL.isNotEmpty() && mProfileImageURL != mUserDetails.image) {
            userHashMap[Constants.IMAGE] = mProfileImageURL
            changesMade = true
        }
        if (userName.text.isNotBlank() && userName.text.toString() != mUserDetails.name) {
            userHashMap[Constants.NAME] = userName.text.toString()
            changesMade = true
        }
        if (phoneNumber.text.isNotBlank() && phoneNumber.text.toString() != mUserDetails.mobile.toString()) {
            userHashMap[Constants.MOBILE] = phoneNumber.text.toString().toLong()
            changesMade = true
        }

        if (changesMade) {
            FireStore().updateUserProfileData(this@MyProfileActivity, userHashMap)
        }

    }

    private fun uploadUserImage() {
        showProgressDialog(resources.getString(R.string.please_wait))
        if (mSelectedImageFileUri != null) {
            val storageRef: StorageReference = FirebaseStorage.getInstance().reference.child(
                "USER_IMAGE" +
                        System.currentTimeMillis() + "." +
                        Constants.getFileExtension(this, mSelectedImageFileUri)
            )

            storageRef.putFile(mSelectedImageFileUri!!)
                .addOnSuccessListener { snapshot ->
                    hideProgressDialog()
                    Log.d(
                        "Firebase Image URL",
                        snapshot.metadata!!.reference!!.downloadUrl.toString()
                    )

                    snapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener { uri ->
                        Log.d("Downloadable Image URL", uri.toString())
                        mProfileImageURL = uri.toString()
                        updateUserProfileData()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                    hideProgressDialog()
                }
        }
    }

    fun profileUpdateSuccess() {
        hideProgressDialog()

        setResult(Activity.RESULT_OK)

        finish()
    }
}