package com.cosmiccodecraft.projectmanager.activities

import android.app.Activity
import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cosmiccodecraft.projectmanager.R
import com.cosmiccodecraft.projectmanager.adapters.CardMemberListItemAdapter
import com.cosmiccodecraft.projectmanager.dialogs.LabelColorListDialog
import com.cosmiccodecraft.projectmanager.firebase.FireStore
import com.cosmiccodecraft.projectmanager.models.Board
import com.cosmiccodecraft.projectmanager.models.Card
import com.cosmiccodecraft.projectmanager.models.SelectedMembers
import com.cosmiccodecraft.projectmanager.models.Task
import com.cosmiccodecraft.projectmanager.models.User
import com.cosmiccodecraft.projectmanager.utils.Constants
import com.projemanag.dialogs.MembersListDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CardDetailsActivity : BaseActivity() {

    private var mSelectedColor: String = ""
    private lateinit var mBoardDetails: Board
    private var mCardPosition = -1
    private var mTaskListPosition = -1
    private lateinit var mCardName: EditText
    private lateinit var mMembersDetailList: ArrayList<User>
    private var mSelectedDueDateMilliSecs: Long = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_details)
        getIntentData()
        setActionBar()

        mCardName = findViewById(R.id.et_name_card_details)
        mCardName.setText(mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].name)
        mCardName.setSelection(mCardName.text.toString().length)
        mSelectedColor = mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].labelColor
        mSelectedDueDateMilliSecs = mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].dueDate

        if(mSelectedDueDateMilliSecs > 0){
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
            val selectedDate = sdf.format(Date(mSelectedDueDateMilliSecs))
            findViewById<TextView>(R.id.tv_select_due_date).text = selectedDate
        }

        if (mSelectedColor.isNotEmpty()) {
            setColor()
        }

        findViewById<Button>(R.id.btn_update_card_details).setOnClickListener {
            if (mCardName.text.toString().isNotBlank()) {
                updateCardDetails()
            } else {
                hideKeyboard()
                showErrorSnackBar("Please enter a card name")
            }
        }

        findViewById<TextView>(R.id.tv_select_label_color).setOnClickListener {
            labelColorsListDialog()
        }

        findViewById<TextView>(R.id.tv_select_members).setOnClickListener {
            membersListDialog()
        }
        setUpSelectedMembersList()

        findViewById<TextView>(R.id.tv_select_due_date).setOnClickListener {
            datePicker()
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_delete_card, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_delete_card -> {
                alertDialogForDeleteCard(mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].name)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    private fun setActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_card_details_activity)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back_button_white)
            actionBar.title = mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].name
        }

        toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun getIntentData() {
        if (intent.hasExtra(Constants.BOARD_DETAIL)) {
            mBoardDetails = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                intent.getParcelableExtra(Constants.BOARD_DETAIL, Board::class.java)!!
            else {
                intent.getParcelableExtra<Board>(Constants.BOARD_DETAIL)!!
            }
        }
        if (intent.hasExtra(Constants.TASK_LIST_ITEM_POSITION)) {
            mTaskListPosition = intent.getIntExtra(Constants.TASK_LIST_ITEM_POSITION, -1)
        }
        if (intent.hasExtra(Constants.CARD_LIST_ITEM_POSITION)) {
            mCardPosition = intent.getIntExtra(Constants.CARD_LIST_ITEM_POSITION, -1)
        }
        if (intent.hasExtra(Constants.BOARD_MEMBERS_LIST)) {
            mMembersDetailList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                intent.getParcelableArrayListExtra(Constants.BOARD_MEMBERS_LIST, User::class.java)!!
            else {
                intent.getParcelableArrayListExtra<User>(Constants.BOARD_MEMBERS_LIST)!!
            }
        }
    }

    private fun updateCardDetails() {
        val card = Card(
            name = mCardName.text.toString().trim(),
            createdBy = mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].createdBy,
            assignedTo = mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].assignedTo,
            labelColor = mSelectedColor,
            dueDate = mSelectedDueDateMilliSecs
        )

        mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition] = card
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size - 1)

        showProgressDialog(resources.getString(R.string.please_wait))
        FireStore().addUpdateTaskList(this@CardDetailsActivity, mBoardDetails)
    }

    private fun deleteCard() {
        val cardsList: ArrayList<Card> = mBoardDetails.taskList[mTaskListPosition].cards
        cardsList.removeAt(mCardPosition)

        // because our task list also have an add task button at the end
        // we have to delete it before updating the task list in firebase
        val taskList: ArrayList<Task> = mBoardDetails.taskList
        taskList.removeAt(taskList.size - 1)

        // update task list with updated list
        taskList[mTaskListPosition].cards = cardsList
        showProgressDialog(resources.getString(R.string.please_wait))
        FireStore().addUpdateTaskList(this@CardDetailsActivity, mBoardDetails)

    }

    fun addUpdateTaskListSuccess() {
        hideProgressDialog()
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun alertDialogForDeleteCard(cardName: String) {
        val builder = AlertDialog.Builder(this@CardDetailsActivity)

        builder.setTitle("Alert")
        builder.setMessage("Are you sure you want to delete: $cardName")
        builder.setIcon(android.R.drawable.ic_dialog_alert)
        builder.setPositiveButton("Yes") { dialogInterface, _ ->
            dialogInterface.dismiss()
            deleteCard()
        }
        builder.setNegativeButton("No") { dialogInterface, _ ->
            dialogInterface.dismiss()
        }
        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    private fun colorsList(): ArrayList<String> {
        val colorsList: ArrayList<String> = ArrayList()
        colorsList.add("#43C86F")
        colorsList.add("#0C90F1")
        colorsList.add("#F72400")
        colorsList.add("#7A8089")
        colorsList.add("#D57C1D")
        colorsList.add("#770000")
        colorsList.add("#0022F8")

        return colorsList
    }

    private fun setColor() {
        val selectedLabel = findViewById<TextView>(R.id.tv_select_label_color)
        selectedLabel.text = ""
        selectedLabel.setBackgroundColor(Color.parseColor(mSelectedColor))
    }

    private fun labelColorsListDialog() {
        val colorsList: ArrayList<String> = colorsList()
        val listDialog = object : LabelColorListDialog(
            this@CardDetailsActivity,
            colorsList,
            resources.getString(R.string.str_select_label_color),
            mSelectedColor
        ) {
            override fun onItemSelected(color: String) {
                mSelectedColor = color
                setColor()
            }
        }
        listDialog.show()
    }

    private fun membersListDialog() {
        val cardAssignedMemberList =
            mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].assignedTo

        if (cardAssignedMemberList.size > 0) {
            for (i in mMembersDetailList.indices) {
                for (j in cardAssignedMemberList) {
                    if (mMembersDetailList[i].id == j) {
                        mMembersDetailList[i].selected = true
                    }
                }
            }
        } else {
            for (i in mMembersDetailList.indices) {
                mMembersDetailList[i].selected = false
            }
        }

        val listDialog = object : MembersListDialog(
            this@CardDetailsActivity,
            mMembersDetailList,
            resources.getString(R.string.select_members)
        ) {
            override fun onItemSelected(user: User, action: String) {
                if (action == Constants.SELECT) {
                    if (!mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].assignedTo.contains(
                            user.id
                        )
                    ) {
                        mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].assignedTo.add(
                            user.id
                        )
                    }
                } else {
                    mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].assignedTo.remove(
                        user.id
                    )
                    for (i in mMembersDetailList.indices) {
                        if (mMembersDetailList[i].id == user.id) {
                            mMembersDetailList[i].selected = false
                        }
                    }
                }
                setUpSelectedMembersList()
            }
        }
        listDialog.show()
    }


    private fun setUpSelectedMembersList() {
        val cardAssignedMemberList =
            mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].assignedTo

        val selectedMembersList: ArrayList<SelectedMembers> = ArrayList()
        for (i in mMembersDetailList.indices) {
            for (j in cardAssignedMemberList) {
                if (mMembersDetailList[i].id == j) {
                    val selectedMember = SelectedMembers(
                        mMembersDetailList[i].id,
                        mMembersDetailList[i].image
                    )
                    selectedMembersList.add(selectedMember)
                }
            }
        }
        if (selectedMembersList.size > 0) {
            selectedMembersList.add(SelectedMembers("", ""))

            findViewById<TextView>(R.id.tv_select_members).visibility = View.GONE

            val rvList = findViewById<RecyclerView>(R.id.rv_selected_members_list)
            rvList.visibility = View.VISIBLE
            rvList.layoutManager = GridLayoutManager(this@CardDetailsActivity, 6)

            val adapter =
                CardMemberListItemAdapter(this@CardDetailsActivity, selectedMembersList, true)

            rvList.adapter = adapter

            adapter.setOnClickListener(
                object : CardMemberListItemAdapter.OnClickListener {
                    override fun onClick() {
                        membersListDialog()
                    }
                }
            )
        } else {
            findViewById<TextView>(R.id.tv_select_members).visibility = View.VISIBLE
            findViewById<RecyclerView>(R.id.rv_selected_members_list).visibility = View.GONE
        }
    }

    private fun datePicker() {
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        val dpd = DatePickerDialog(
            this,
            { view, year, monthOfYear, dayOfMonth ->
                val sDayOfMonth = if (dayOfMonth < 10) "0$dayOfMonth" else "$dayOfMonth"
                val sMonthOfYear =
                    if ((monthOfYear + 1) < 10) "0${monthOfYear + 1}" else "${monthOfYear + 1}"

                val selectedDate = "$sDayOfMonth/$sMonthOfYear/$year"
                findViewById<TextView>(R.id.tv_select_due_date).text = selectedDate

                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
                val theDate = sdf.parse(selectedDate)
                mSelectedDueDateMilliSecs = theDate!!.time
            },
            year,
            month,
            day
        )
        dpd.show()
    }


}