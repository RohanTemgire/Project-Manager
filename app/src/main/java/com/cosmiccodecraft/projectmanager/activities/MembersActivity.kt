package com.cosmiccodecraft.projectmanager.activities

import android.app.Activity
import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cosmiccodecraft.projectmanager.R
import com.cosmiccodecraft.projectmanager.adapters.MemberListItemsAdapter
import com.cosmiccodecraft.projectmanager.firebase.FireStore
import com.cosmiccodecraft.projectmanager.models.Board
import com.cosmiccodecraft.projectmanager.models.User
import com.cosmiccodecraft.projectmanager.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

class MembersActivity : BaseActivity() {

    private lateinit var mBoardDetails: Board
    private lateinit var mAssignedMembersList: ArrayList<User>
    private var anyChangesMade: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_members)

        if (intent.hasExtra(Constants.BOARD_DETAIL)) {
            mBoardDetails = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                intent.getParcelableExtra(Constants.BOARD_DETAIL, Board::class.java)!!
            else {
                intent.getParcelableExtra<Board>(Constants.BOARD_DETAIL)!!
            }
        }

        setActionBar()
        showProgressDialog(resources.getString(R.string.please_wait))
        FireStore().getAssignedMembersListDetails(this@MembersActivity, mBoardDetails.assignedTo)
    }

    override fun onBackPressed() {
        if (anyChangesMade) {
            setResult(Activity.RESULT_OK)
        }
        super.onBackPressed()
    }

    private fun setActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_members_activity)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back_button_white)
            actionBar.title = resources.getString(R.string.members)
        }

        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    fun setUpMembersList(list: ArrayList<User>) {
        hideProgressDialog()

        mAssignedMembersList = list

        val rvMemberList = findViewById<RecyclerView>(R.id.rv_members_list)
        rvMemberList.layoutManager = LinearLayoutManager(this@MembersActivity)

        val adapter = MemberListItemsAdapter(this@MembersActivity, list)
        rvMemberList.adapter = adapter
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_add_member, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_Add_member -> {
                dialogSearchMember()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun dialogSearchMember() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_search_member)
        dialog.findViewById<TextView>(R.id.tv_add).setOnClickListener(View.OnClickListener {
            val email = dialog.findViewById<EditText>(R.id.et_email_search_member).text.toString()

            if (email.isNotBlank()) {
                dialog.dismiss()
                showProgressDialog(resources.getString(R.string.please_wait))
                FireStore().getMemberDetails(this, email.trim())
            } else {
                showErrorSnackBar("Please enter members email address")
            }

        })
        dialog.findViewById<TextView>(R.id.tv_cancel).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()

    }

    fun memberDetails(user: User) {
        if (mBoardDetails.assignedTo.contains(user.id)) {
            hideProgressDialog()
            Toast.makeText(this, "This member is already assigned", Toast.LENGTH_SHORT).show()
            return
        }
        hideProgressDialog()
        mBoardDetails.assignedTo.add(user.id)
        FireStore().assignMemberToBoard(this, mBoardDetails, user)
    }

    fun memberAssignSuccess(user: User) {
        hideProgressDialog()
        mAssignedMembersList.add(user)
        anyChangesMade = true
        setUpMembersList(mAssignedMembersList)

        SendNotificationsToUser(mBoardDetails.name, user.fcmToken).startApiCall()

    }

    private inner class SendNotificationsToUser(val boardName: String, val token: String) {

        fun startApiCall() {
            showProgressDialog(resources.getString(R.string.please_wait))
            lifecycleScope.launch(Dispatchers.IO) {
                val stringResult = makeApiCall()

                Log.i("JSON RESPONSE RESULT", stringResult)
            }
            hideProgressDialog()
        }

        private suspend fun makeApiCall(): String {
            Log.i("****makeApiCall","from MemberActivity - Receivers FCM key = $token")
            var result: String

            var connection: HttpURLConnection? = null
            try {
                val url = URL(Constants.FCM_BASE_URL)
                connection = withContext(Dispatchers.IO) {
                    url.openConnection()
                } as HttpURLConnection
                connection.doInput = true
                connection.doOutput = true
                connection.instanceFollowRedirects = false
                connection.requestMethod = "POST"

                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("charset", "utf-8")
                connection.setRequestProperty("Accept", "application/json")

                connection.setRequestProperty(
                    Constants.FCM_AUTHORIZATION,
                    "${Constants.FCM_KEY}=${Constants.FCM_SERVER_KEY}"
                )
                connection.useCaches = false

                val wr = DataOutputStream(connection.outputStream)
                val jsonRequest = JSONObject()
                val dataObject = JSONObject()
                dataObject.put(Constants.FCM_KEY_TITLE, "Assigned to board : $boardName")
                dataObject.put(
                    Constants.FCM_KEY_MESSAGE,
                    "You have been assigned to a new board by ${mAssignedMembersList[0].name}"
                )
                jsonRequest.put(Constants.FCM_KEY_DATA, dataObject)
                jsonRequest.put(Constants.FCM_KEY_TO, token)


                withContext(Dispatchers.IO) {
                    wr.writeBytes(jsonRequest.toString())
                }
                withContext(Dispatchers.IO) {
                    wr.flush()
                }
                withContext(Dispatchers.IO) {
                    wr.close()
                }

                val httpResult: Int = connection.responseCode
                println(" ****** SendNotificationToUser httpResult: $httpResult")

                if (httpResult == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val sb = StringBuilder()
                    var line: String?
                    try {
                        while (withContext(Dispatchers.IO) {
                                reader.readLine()
                            }.also { line = it } != null) {
                            sb.append(line + "\n")
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } finally {
                        try {
                            withContext(Dispatchers.IO) {
                                inputStream.close()
                            }
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    result = sb.toString()
                    println("****** SendNotificationToUser result $result")
                } else {
                    result = connection.responseMessage
                }
            } catch (e: SocketTimeoutException) {
                result = "Connection timeout"
            } catch (e: Exception) {
                result = "Error : ${e.message}"
            } finally {
                connection?.disconnect()
            }
            return result
        }
    }


}
