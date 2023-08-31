package com.cosmiccodecraft.projectmanager.adapters

import android.content.Context
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cosmiccodecraft.projectmanager.R
import com.cosmiccodecraft.projectmanager.activities.TaskListActivity
import com.cosmiccodecraft.projectmanager.models.Card
import com.cosmiccodecraft.projectmanager.models.Task
import java.util.Collections

open class TaskListItemsAdapter(
    private val context: Context,
    private var list: ArrayList<Task>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var mDraggedFrom = -1
    private var mDraggedTo = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_task, parent, false)

        // defining the width up to 70% of the screen
        // its used to deal with the xml layouts, so that we can adjust everything
        val layoutParams = LinearLayout.LayoutParams(
            (parent.width * 0.7).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        layoutParams.setMargins(15.toDP().toPx(), 0, 40.toDP().toPx(), 0)

        view.layoutParams = layoutParams
        return MyViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        val model = list[position]
        if (holder is MyViewHolder) {
            if (position == list.size - 1) {
                holder.itemView.findViewById<View>(R.id.tv_add_task_list).visibility = View.VISIBLE
                holder.itemView.findViewById<View>(R.id.ll_task_item).visibility = View.GONE
            } else {
                holder.itemView.findViewById<View>(R.id.tv_add_task_list).visibility = View.GONE
                holder.itemView.findViewById<View>(R.id.ll_task_item).visibility = View.VISIBLE
            }

            holder.itemView.findViewById<TextView>(R.id.tv_task_list_title).text = model.title
            holder.itemView.findViewById<TextView>(R.id.tv_add_task_list).setOnClickListener {
                holder.itemView.findViewById<View>(R.id.tv_add_task_list).visibility = View.GONE
                holder.itemView.findViewById<View>(R.id.cv_add_task_list_name).visibility =
                    View.VISIBLE
            }

            // setting on click listener for the add task name so that we can make it invisible and present new card view
            // for close we reverse the prev process
            holder.itemView.findViewById<ImageButton>(R.id.ib_close_list_name).setOnClickListener {
                holder.itemView.findViewById<View>(R.id.tv_add_task_list).visibility = View.VISIBLE
                holder.itemView.findViewById<View>(R.id.cv_add_task_list_name).visibility =
                    View.GONE
            }
            holder.itemView.findViewById<ImageButton>(R.id.ib_done_list_name).setOnClickListener {

                val listName =
                    holder.itemView.findViewById<TextView>(R.id.et_task_list_name).text.toString()
                if (listName.isNotEmpty()) {
                    if (context is TaskListActivity) {
                        context.createTaskList(listName)
                    }
                } else {
                    Toast.makeText(
                        context,
                        "Please enter a list name.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            // setting up editing the task
            holder.itemView.findViewById<ImageButton>(R.id.ib_edit_list_name).setOnClickListener {
                holder.itemView.findViewById<EditText>(R.id.et_edit_task_list_name)
                    .setText(model.title)
                holder.itemView.findViewById<LinearLayout>(R.id.ll_title_view).visibility =
                    View.GONE
                holder.itemView.findViewById<CardView>(R.id.cv_edit_task_list_name).visibility =
                    View.VISIBLE
            }
            // close edit text view
            holder.itemView.findViewById<ImageButton>(R.id.ib_close_editable_view)
                .setOnClickListener {
                    holder.itemView.findViewById<View>(R.id.ll_title_view).visibility = View.VISIBLE
                    holder.itemView.findViewById<View>(R.id.cv_edit_task_list_name).visibility =
                        View.GONE
                }
            // when done editing
            holder.itemView.findViewById<ImageButton>(R.id.ib_done_edit_list_name)
                .setOnClickListener {
                    val listName =
                        holder.itemView.findViewById<EditText>(R.id.et_edit_task_list_name).text.toString()
                    if (listName.isNotEmpty()) {
                        if (context is TaskListActivity) {
                            context.updateTaskList(position, listName, model)
                        }
                    } else {
                        Toast.makeText(context, "Please enter a list name.", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

            // setting up deleting the task
            holder.itemView.findViewById<ImageButton>(R.id.ib_delete_list).setOnClickListener {
                alertDialog(position, model.title)
            }


            // adding cards to the task list
            // show card view
            holder.itemView.findViewById<TextView>(R.id.tv_add_card).setOnClickListener {
                holder.itemView.findViewById<TextView>(R.id.tv_add_card).visibility = View.GONE
                holder.itemView.findViewById<CardView>(R.id.cv_add_card).visibility = View.VISIBLE

                // close card view
                holder.itemView.findViewById<ImageButton>(R.id.ib_close_card_name)
                    .setOnClickListener {
                        holder.itemView.findViewById<TextView>(R.id.tv_add_card).visibility =
                            View.VISIBLE
                        holder.itemView.findViewById<CardView>(R.id.cv_add_card).visibility =
                            View.GONE
                    }

                // when done adding card
                holder.itemView.findViewById<ImageButton>(R.id.ib_done_card_name)
                    .setOnClickListener {
                        val cardName =
                            holder.itemView.findViewById<EditText>(R.id.et_card_name).text.toString()
                        if (cardName.isNotEmpty()) {
                            if (context is TaskListActivity) {
                                context.addCardToTaskList(position, cardName)
                            }
                        } else {
                            Toast.makeText(
                                context,
                                "Please enter a card name.",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        }
                    }
            }


            //setting up card list recyclerView
            val rvCardList = holder.itemView.findViewById<RecyclerView>(R.id.rv_card_list)

            rvCardList.layoutManager = LinearLayoutManager(context)
            rvCardList.setHasFixedSize(true)

            val adapter = CardListItemsAdapter(context, model.cards)
            rvCardList.adapter = adapter

            adapter.setOnClickListener(
                object : CardListItemsAdapter.OnClickListener {
                    override fun onClick(cardPosition: Int) {
                        if (context is TaskListActivity) {
                            context.cardDetails(position, cardPosition)
                        }
                    }
                }
            )

            // setting up the drag and drop feature
            val dividerItemDecoration =
                DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            rvCardList.addItemDecoration(dividerItemDecoration)
            // whenever want to deal with drag and drop we need to use ItemTouchHelper
            val helper = ItemTouchHelper(
                object : ItemTouchHelper.SimpleCallback(
                    ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
                ) {
                    override fun onMove(
                        recyclerView: RecyclerView,
                        dragged: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder
                    ): Boolean {
                        val draggedPosition = dragged.adapterPosition
                        val targetPosition = target.adapterPosition

                        if (mDraggedFrom == -1) {
                            mDraggedFrom = draggedPosition
                        }
                        mDraggedTo = targetPosition
                        Collections.swap(list[position].cards, draggedPosition, targetPosition)
                        adapter.notifyItemMoved(draggedPosition, targetPosition)
                        return false
                    }

                    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

                    override fun clearView(
                        recyclerView: RecyclerView,
                        viewHolder: RecyclerView.ViewHolder
                    ) {
                        super.clearView(recyclerView, viewHolder)
                        if (mDraggedFrom != -1 && mDraggedTo != -1 && mDraggedFrom != mDraggedTo) {
                            (context as TaskListActivity).updateCardsInTaskList(
                                position,
                                list[position].cards
                            )
                        }
                        mDraggedFrom = -1
                        mDraggedTo = -1
                    }
                }
            )
            helper.attachToRecyclerView(rvCardList)
        }
    }

    private fun Int.toDP(): Int = (this / Resources.getSystem().displayMetrics.density).toInt()

    private fun Int.toPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view)

    private fun alertDialog(position: Int, title: String) {
        val builder = AlertDialog.Builder(context)

        builder.setTitle("Alert")
        builder.setMessage("Are you sure you want to delete: $title")
        builder.setIcon(android.R.drawable.ic_dialog_alert)
        builder.setPositiveButton("Yes") { dialogInterface, _ ->
            dialogInterface.dismiss()
            if (context is TaskListActivity) {
                context.deleteTaskList(position)
            }
        }
        builder.setNegativeButton("No") { dialogInterface, _ ->
            dialogInterface.dismiss()
        }
        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }
}