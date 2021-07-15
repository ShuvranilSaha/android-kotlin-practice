package com.subranil_saha.listmaker

import android.content.Context
import androidx.preference.PreferenceManager
import java.util.*
import kotlin.collections.ArrayList

class ListDataManager(private val context: Context) {
    fun saveList(list: TaskList) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context).edit()
        sharedPreferences.putStringSet(list.name, list.tasks.toHashSet())
        sharedPreferences.apply()
    }

    fun readList(): ArrayList<TaskList> {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val sharePrefContents = sharedPreferences.all
        val taskLists = ArrayList<TaskList>()
        for (taskList in sharePrefContents) {
            val itemHashSet = taskList.value as HashSet<String>
            val list = TaskList(taskList.key, ArrayList(itemHashSet))
            taskLists.add(list)
        }
        return taskLists
    }
}