package com.cosmiccodecraft.projectmanager.activities

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cosmiccodecraft.projectmanager.R
import com.cosmiccodecraft.projectmanager.adapters.BoardItemsAdapter
import com.cosmiccodecraft.projectmanager.firebase.FireStore
import com.cosmiccodecraft.projectmanager.models.Board
import com.cosmiccodecraft.projectmanager.models.User
import com.cosmiccodecraft.projectmanager.utils.Constants
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.iid.FirebaseInstanceIdReceiver
import com.google.firebase.iid.internal.FirebaseInstanceIdInternal
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    companion object {
        const val MY_PROFILE_REQUEST_CODE: Int = 21
        const val CREATE_BOARD_REQUEST_CODE: Int = 22
    }

    private lateinit var mUsername: String

    private lateinit var mSharedPreferences: SharedPreferences


    private lateinit var drawerLayout: DrawerLayout
    private lateinit var fab: FloatingActionButton
    private lateinit var rvBoardsList: RecyclerView
    private lateinit var tvNoBoards: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mSharedPreferences = this.getSharedPreferences(
            Constants.PROJECTMANAGER_PREFS,
            MODE_PRIVATE
        )

        // this is used to get the token from shared preferences, to know if the token is updated or not
        val tokenUpdated = mSharedPreferences.getBoolean(Constants.FCM_TOKEN_UPDATED, false)

        if (tokenUpdated) {
            showProgressDialog(resources.getString(R.string.please_wait))
            FireStore().loadUserData(this, true)

            Log.e("FCM token", "already exist From main activity")
            println("****** FCM token already exist From main activity")

        } else {
            // Get the current logged in user details.
            showProgressDialog(resources.getString(R.string.please_wait))
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    updateFCMToken(token)
                    Log.e("FCM token", "From main activity new created $token")
                    println("******** FCM token From main activity new created $token")
                }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkPermissionforNotification()
        }

        drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        fab = findViewById(R.id.fab_create_board)
        rvBoardsList = findViewById(R.id.rv_boards_list)
        tvNoBoards = findViewById(R.id.tv_no_boards_available)

        setupActionBar()

        val navView = findViewById<NavigationView>(R.id.nav_view)
        navView.setNavigationItemSelectedListener(this)

        FireStore().loadUserData(this, true)

        fab.setOnClickListener {
            val intent = Intent(this, CreateBoardActivity::class.java)
            intent.putExtra(
                Constants.NAME, mUsername
            )
            startActivityForResult(intent, CREATE_BOARD_REQUEST_CODE)
        }
    }

    private fun checkPermissionforNotification() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                Constants.NOTIFICATION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == Constants.NOTIFICATION_REQUEST_CODE
            && grantResults.isNotEmpty()
            && grantResults[0] != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(
                this,
                "Please allow the notification permission to receive notifications",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    private fun setupActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_main_activity)
        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_action_navigation_menu)

        toolbar.setNavigationOnClickListener { toggleDrawer() }
    }


    private fun toggleDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            doubleBackToExit()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (MY_PROFILE_REQUEST_CODE == requestCode && resultCode == RESULT_OK) {
            FireStore().loadUserData(this)
        } else if (CREATE_BOARD_REQUEST_CODE == requestCode && resultCode == RESULT_OK) {
            FireStore().getBoardsList(this)
        } else {
            Log.e("Cancelled", "Cancelled")
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_my_profile -> {
                startActivityForResult(
                    Intent(this, MyProfileActivity::class.java),
                    MY_PROFILE_REQUEST_CODE
                )
            }

            R.id.nav_sign_out -> {
                FirebaseAuth.getInstance().signOut()

                mSharedPreferences.edit().clear().apply()


                val intent = Intent(this, IntroActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    fun updateNavigationUserDetails(loggedInUserData: User, readBoardsList: Boolean) {
        hideProgressDialog()
        mUsername = loggedInUserData.name

        val imageView = findViewById<ImageView>(R.id.nav_user_image)
        val userName = findViewById<TextView>(R.id.tv_username)
        Glide
            .with(this)
            .load(loggedInUserData.image)
            .centerCrop()
            .placeholder(R.drawable.ic_image_place_holder)
            .into(imageView);

        userName.text = loggedInUserData.name

        if (readBoardsList) {
            showProgressDialog(resources.getString(R.string.please_wait))
            FireStore().getBoardsList(this)
        }


    }


    fun populateBoardsListToUI(boardsList: ArrayList<Board>) {
        hideProgressDialog()

        if (boardsList.size > 0) {
            rvBoardsList.visibility = View.VISIBLE
            tvNoBoards.visibility = View.GONE

            rvBoardsList.layoutManager = LinearLayoutManager(this)
            rvBoardsList.setHasFixedSize(true)

            val adapter = BoardItemsAdapter(this, boardsList)
            rvBoardsList.adapter = adapter


            adapter.setOnClickListener(
                object : BoardItemsAdapter.OnClickListener {
                    override fun onClick(position: Int, model: Board) {
                        val intent = Intent(this@MainActivity, TaskListActivity::class.java)
                        intent.putExtra(Constants.DOCUMENT_ID, model.documentId)
                        startActivity(intent)
                    }
                }
            )

        } else {
            rvBoardsList.visibility = View.GONE
            tvNoBoards.visibility = View.VISIBLE
        }

    }

    fun tokenUpdateSuccess() {
        hideProgressDialog()
        val editor: SharedPreferences.Editor = mSharedPreferences.edit()
        editor.putBoolean(Constants.FCM_TOKEN_UPDATED, true)
        editor.apply()
        showProgressDialog(resources.getString(R.string.please_wait))
        FireStore().loadUserData(this, true)
    }

    private fun updateFCMToken(token: String) {
        hideProgressDialog()
        val userHashMap = HashMap<String, Any>()
        userHashMap[Constants.FCM_TOKEN] = token

        showProgressDialog(resources.getString(R.string.please_wait))
        FireStore().updateUserProfileData(this, userHashMap)
    }
}