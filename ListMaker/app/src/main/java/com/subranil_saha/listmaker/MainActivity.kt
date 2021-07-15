package com.subranil_saha.listmaker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.maxkeppeler.sheets.input.InputSheet
import com.maxkeppeler.sheets.input.type.InputEditText

class MainActivity : AppCompatActivity(),
    ListSelectionFragment.OnListItemFragmentInteractionListener {
    private lateinit var fab: FloatingActionButton
    private var listSelectionFragment: ListSelectionFragment = ListSelectionFragment.newInstance()
    private var fragmentContainer: FrameLayout? = null
    private var largeScreen = false
    private var listDetailFragment: ListDetailFragment? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fab = findViewById(R.id.fab)
        listSelectionFragment =
            supportFragmentManager.findFragmentById(R.id.list_selection_fragment) as ListSelectionFragment

        fragmentContainer = findViewById(R.id.fragment_container)
        largeScreen = fragmentContainer != null

        fab.setOnClickListener {
            showCreateListDialog()
        }
    }

    private val getContent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val data: Intent? = it.data
                data?.let {
                    listSelectionFragment.saveList(data.getParcelableExtra(INTENT_LIST_KEY)!!)
                }
            }
        }

    private fun createTaskDialog() {
        InputSheet().show(this) {
            title(R.string.add_task)
            with(InputEditText {
                required()
                hint("your name of the task")
            })
            onPositive {
                val task = it.getString(0.toString())
                if (task != null) {
                    listDetailFragment?.addTask(task)
                }
            }
        }
    }

    private fun showCreateListDialog() {
        val dialogTitle = getString(R.string.name_of_the_list)
        InputSheet().show(this) {
            title(dialogTitle)
            with(InputEditText {
                required()
                hint("Your name of the list")
            })
            onPositive {
                val list = TaskList(it.getString(0.toString()))
                listSelectionFragment.addList(list)
                showListDetail(list)
            }
        }
    }

    private fun showListDetail(list: TaskList) {
        if (!largeScreen) {
            val listDetailIntent = Intent(this, ListDetailActivity::class.java)
            listDetailIntent.putExtra(INTENT_LIST_KEY, list)
            getContent.launch(listDetailIntent)
        } else {
            title = list.name
            listDetailFragment = ListDetailFragment.newInstance(list)
            supportFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragment_container,
                    listDetailFragment!!,
                    getString(R.string.list_fragment_tag)
                )
                .addToBackStack(null)
                .commit()

            fab.setOnClickListener {
                createTaskDialog()
            }
        }
    }

    companion object {
        const val INTENT_LIST_KEY = "list"
    }

    override fun onListItemClicked(list: TaskList) {
        showListDetail(list)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        title = resources.getString(R.string.app_name)
        listDetailFragment?.list?.let {
            listSelectionFragment.listDataManager.saveList(it)
        }
        if (listDetailFragment != null) {
            supportFragmentManager.beginTransaction()
                .remove(listDetailFragment!!)
                .commit()
            listDetailFragment = null
        }

        fab.setOnClickListener {
            showCreateListDialog()
        }
    }
}