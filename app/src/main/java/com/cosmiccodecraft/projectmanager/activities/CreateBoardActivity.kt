package com.cosmiccodecraft.projectmanager.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
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
import com.cosmiccodecraft.projectmanager.models.Board
import com.cosmiccodecraft.projectmanager.utils.Constants
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class CreateBoardActivity : BaseActivity() {

    private var mSelectedImageFileUri: Uri? = null
    private lateinit var image: ImageView
    private lateinit var createBtn: Button
    private lateinit var boardName: EditText

    private lateinit var mUserName: String

    private var mBoardImageUrl: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_board)
        setActionBar()

        image = findViewById(R.id.iv_board_image)
        createBtn = findViewById(R.id.btn_create)
        boardName = findViewById(R.id.et_board_name)

        if (intent.hasExtra(Constants.NAME)) {
            mUserName = intent.getStringExtra(Constants.NAME)!!
        }

        image.setOnClickListener {
            checkPermission()
        }

        createBtn.setOnClickListener {
            if(boardName.text.toString().isNotBlank()){
                if (mSelectedImageFileUri != null) {
                    uploadBoardImage()
                } else {
                    showProgressDialog(resources.getString(R.string.please_wait))
                    createBoard()
                }
            }else{
                hideKeyboard()
                showErrorSnackBar("Please enter a board name")
            }
        }
    }




    private fun setActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_create_board_activity)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back_button_white)
            actionBar.title = resources.getString(R.string.create_board_title)
        }

        toolbar.setNavigationOnClickListener { onBackPressed() }
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
                    Constants.READ_EXTERNAL_STORAGE_CODE
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
                    Constants.READ_EXTERNAL_STORAGE_CODE
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

        if (requestCode == Constants.READ_EXTERNAL_STORAGE_CODE
            && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            Constants.showImageChooser(this)
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
            && requestCode == Constants.PICK_IMAGE_REQUEST_CODE
            && data!!.data != null
        ) {
            mSelectedImageFileUri = data.data
            try {
                Glide
                    .with(this)
                    .load(mSelectedImageFileUri)
                    .centerCrop()
                    .placeholder(R.drawable.ic_board_place_holder)
                    .into(image)
                    .onLoadFailed(resources.getDrawable(R.drawable.ic_board_place_holder))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun boardCreatedSuccessfully() {
        hideProgressDialog()
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun createBoard() {
        val assignedUsersArrayList: ArrayList<String> = ArrayList()
        assignedUsersArrayList.add(getCurrentUserId())

        val board = Board(
            boardName.text.toString().trim(),
            mBoardImageUrl,
            mUserName,
            assignedUsersArrayList
        )

        FireStore().createBoard(this, board)
    }

    private fun uploadBoardImage() {
        showProgressDialog(resources.getString(R.string.please_wait))
        val storageRef: StorageReference = FirebaseStorage.getInstance().reference.child(
            "BOARD_IMAGE" +
                    System.currentTimeMillis() + "." +
                    Constants.getFileExtension(this, mSelectedImageFileUri)
        )

        storageRef.putFile(mSelectedImageFileUri!!)
            .addOnSuccessListener { snapshot ->
                hideProgressDialog()
                Log.d(
                    "Board Image URL",
                    snapshot.metadata!!.reference!!.downloadUrl.toString()
                )

                snapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener { uri ->
                    Log.d("Downloadable Image URL", uri.toString())
                    mBoardImageUrl = uri.toString()
                    createBoard()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                hideProgressDialog()
            }
    }

}