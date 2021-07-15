package com.subranil_saha.listmaker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.maxkeppeler.sheets.input.InputSheet
import com.maxkeppeler.sheets.input.type.InputEditText

class ListDetailActivity : AppCompatActivity() {
    private lateinit var list: TaskList
    private lateinit var listItemsRecyclerView: RecyclerView
    private lateinit var addTaskButton: FloatingActionButton
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_detail)
        list = intent.getParcelableExtra(MainActivity.INTENT_LIST_KEY)!!
        title = list.name
        listItemsRecyclerView = findViewById(R.id.lists_items_reyclerview)
        listItemsRecyclerView.adapter = ListItemRecyclerViewAdapter(list)
        listItemsRecyclerView.layoutManager = LinearLayoutManager(this)

        addTaskButton = findViewById(R.id.addTaskButton)
        addTaskButton.setOnClickListener {
            showCreateTaskDialog()
        }
    }

    private fun showCreateTaskDialog() {
        InputSheet().show(this) {
            title(R.string.add_task)
            with(InputEditText {
                required()
                hint("your name of the task")
            })
            onPositive {
                val task = it.getString(0.toString())
                if (task != null) {
                    list.tasks.add(task)
                    val recyclerAdapter =
                        listItemsRecyclerView.adapter as ListItemRecyclerViewAdapter
                    recyclerAdapter.notifyItemInserted(list.tasks.size)
                }
            }
        }
    }

    override fun onBackPressed() {
        val bundle = Bundle()
        bundle.putParcelable(MainActivity.INTENT_LIST_KEY, list)
        val intent = Intent()
        intent.putExtras(bundle)
        setResult(Activity.RESULT_OK, intent)
        super.onBackPressed()
    }
}