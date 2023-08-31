package com.cosmiccodecraft.projectmanager.firebase

import android.app.Activity
import android.util.Log
import android.widget.Toast
import com.cosmiccodecraft.projectmanager.activities.CardDetailsActivity
import com.cosmiccodecraft.projectmanager.activities.CreateBoardActivity
import com.cosmiccodecraft.projectmanager.activities.MainActivity
import com.cosmiccodecraft.projectmanager.activities.MembersActivity
import com.cosmiccodecraft.projectmanager.activities.MyProfileActivity
import com.cosmiccodecraft.projectmanager.activities.SignInActivity
import com.cosmiccodecraft.projectmanager.activities.SignUpActivity
import com.cosmiccodecraft.projectmanager.activities.TaskListActivity
import com.cosmiccodecraft.projectmanager.models.Board
import com.cosmiccodecraft.projectmanager.models.User
import com.cosmiccodecraft.projectmanager.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class FireStore {
    private val mFireStore = FirebaseFirestore.getInstance()

    fun registerUser(activity: SignUpActivity, userInfo: User) {
        mFireStore.collection(Constants.USERS)
            .document(getCurrentUserId())
            .set(userInfo, SetOptions.merge())
            .addOnSuccessListener {
                activity.userRegisteredSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(
                    activity.javaClass.simpleName,
                    "Error while registering the user.",
                    e
                )
            }
    }

    fun createBoard(activity: CreateBoardActivity, board: Board) {
        mFireStore.collection(Constants.BOARDS)
            .document()
            .set(board, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(activity.javaClass.simpleName, "Board created successfully!")
                Toast.makeText(activity, "Board created successfully!", Toast.LENGTH_SHORT).show()
                activity.boardCreatedSuccessfully()
            }
            .addOnFailureListener { e ->
                Log.d(activity.javaClass.simpleName, e.message.toString())
                activity.hideProgressDialog()
                Toast.makeText(activity, "Error while creating a board.", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    fun loadUserData(activity: Activity, readBoardsList: Boolean = false) {
        mFireStore.collection(Constants.USERS)
            .document(getCurrentUserId())
            .get()
            .addOnSuccessListener { document ->
                val loggedInUserData = document.toObject(User::class.java)
                if (loggedInUserData != null) {
                    when (activity) {
                        is SignInActivity -> {
                            activity.signInSuccess(loggedInUserData)
                        }

                        is MainActivity -> {
                            activity.updateNavigationUserDetails(loggedInUserData, readBoardsList)
                        }

                        is MyProfileActivity -> {
                            activity.setUserDataInUI(loggedInUserData)
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                when (activity) {
                    is SignInActivity -> {
                        activity.hideProgressDialog()
                    }

                    is MainActivity -> {
                        activity.hideProgressDialog()
                    }

                    is MyProfileActivity -> {
                        activity.hideProgressDialog()
                    }
                }
                Log.e(
                    activity.javaClass.simpleName,
                    "Error while getting the user data.",
                    e
                )
            }
    }


    fun updateUserProfileData(activity: Activity, userHashMap: HashMap<String, Any>) {
        mFireStore.collection(Constants.USERS)
            .document(getCurrentUserId())
            .update(userHashMap)
            .addOnSuccessListener {
                Log.d(activity.javaClass.simpleName, "Profile Data Updated Successfully!")
                Toast.makeText(activity, "Profile updated successfully!", Toast.LENGTH_SHORT)
                    .show()
                when (activity) {
                    is MainActivity -> {
                        activity.tokenUpdateSuccess()
                    }

                    is MyProfileActivity -> {
                        activity.profileUpdateSuccess()
                    }
                }
            }
            .addOnFailureListener { e ->
                when (activity) {
                    is MainActivity -> {
                        activity.hideProgressDialog()
                    }

                    is MyProfileActivity -> {
                        activity.hideProgressDialog()
                    }
                }
                Toast.makeText(activity, "Error when updating the profile", Toast.LENGTH_SHORT)
                    .show()
                Log.e(activity.javaClass.simpleName, "Error when updating the profile", e)
            }
    }

    fun getBoardsList(activity: MainActivity) {
        mFireStore.collection(Constants.BOARDS)
            .whereArrayContains(Constants.ASSIGNED_TO, getCurrentUserId())
            .get()
            .addOnSuccessListener { document ->
                Log.i(activity.javaClass.simpleName, document.documents.toString())
                val boardsList: ArrayList<Board> = ArrayList()
                for (i in document.documents) {
                    val board = i.toObject(Board::class.java)!!
                    board.documentId = i.id
                    boardsList.add(board)
                }
                activity.populateBoardsListToUI(boardsList)
            }
            .addOnFailureListener { e ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error while creating a board.", e)
                Toast.makeText(activity, "Error while creating a board.", Toast.LENGTH_SHORT)
                    .show()
            }
    }


    fun getCurrentUserId(): String {
        val currentUser = FirebaseAuth.getInstance().currentUser
        var currentUserId = ""
        if (currentUser != null) {
            currentUserId = currentUser.uid
        }
        return currentUserId
    }

    fun getBoardDetails(activity: TaskListActivity, documentId: String) {
        mFireStore.collection(Constants.BOARDS)
            .document(documentId)
            .get()
            .addOnSuccessListener { document ->
                Log.i(activity.javaClass.simpleName, document.toString())
                val board = document.toObject(Board::class.java)!!
                board.documentId = document.id
                activity.boardDetails(board)

            }
            .addOnFailureListener { e ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error while loading a board.", e)
                Toast.makeText(activity, "Error while loading a board.", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    fun addUpdateTaskList(activity: Activity, board: Board) {
        val taskListHashMap = HashMap<String, Any>()
        taskListHashMap[Constants.TASK_LIST] = board.taskList

        mFireStore.collection(Constants.BOARDS)
            .document(board.documentId)
            .update(taskListHashMap)
            .addOnSuccessListener {
                Log.i(activity.javaClass.simpleName, "TaskList updated successfully!")
                if (activity is TaskListActivity)
                    activity.addUpdateTaskListSuccess()
                if (activity is CardDetailsActivity)
                    activity.addUpdateTaskListSuccess()
            }
            .addOnFailureListener { e ->
                if (activity is TaskListActivity)
                    activity.hideProgressDialog()
                if (activity is CardDetailsActivity)
                    activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error while creating a board.", e)
                Toast.makeText(activity, "Error while creating a board.", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    fun getAssignedMembersListDetails(activity: Activity, assignedTo: ArrayList<String>) {
        mFireStore.collection(Constants.USERS)
            .whereIn(Constants.ID, assignedTo)
            .get()
            .addOnSuccessListener { document ->
                val usersList: ArrayList<User> = ArrayList()
                for (i in document.documents) {
                    val user = i.toObject(User::class.java)!!
                    usersList.add(user)
                }
                if (activity is MembersActivity) {
                    activity.setUpMembersList(usersList)
                } else if (activity is TaskListActivity)
                    activity.boardMembersDetailList(usersList)
            }
            .addOnFailureListener {
                if (activity is MembersActivity) {
                    activity.hideProgressDialog()
                    activity.showErrorSnackBar("Error while loading members list")

                } else if (activity is TaskListActivity) {
                    activity.hideProgressDialog()
                    activity.showErrorSnackBar("Error while loading members list")
                }
            }
    }

    fun getMemberDetails(activity: MembersActivity, email: String) {
        mFireStore.collection(Constants.USERS)
            .whereEqualTo(Constants.EMAIL, email)
            .get()
            .addOnSuccessListener { document ->
                if (document.documents.size > 0) {
                    val user = document.documents[0].toObject(User::class.java)!!
                    activity.memberDetails(user)
                } else {
                    activity.hideProgressDialog()
                    activity.showErrorSnackBar("No such member found")
                }
            }
            .addOnFailureListener {
                activity.hideProgressDialog()
                activity.showErrorSnackBar("Error while getting user details")
            }
    }

    fun assignMemberToBoard(activity: MembersActivity, board: Board, user: User) {
        val hashMap: HashMap<String, Any> = HashMap()
        hashMap[Constants.ASSIGNED_TO] = board.assignedTo

        mFireStore.collection(Constants.BOARDS)
            .document(board.documentId)
            .update(hashMap)
            .addOnSuccessListener {
                activity.memberAssignSuccess(user)
            }
            .addOnFailureListener {
                activity.hideProgressDialog()
                activity.showErrorSnackBar("Error while assigning member to board")
            }

    }


}