package com.subranil_saha.listmaker

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * A simple [Fragment] subclass.
 * Use the [ListSelectionFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ListSelectionFragment : Fragment(),
    ListSelectionRecyclerViewAdapter.ListSelectionRecyclerViewClickListener {
    private var listener: OnListItemFragmentInteractionListener? = null
    lateinit var listDataManager: ListDataManager
    lateinit var listRecyclerView: RecyclerView


    interface OnListItemFragmentInteractionListener {
        fun onListItemClicked(list: TaskList)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d("ListSelectionFragment", "onAttach")

        if (context is OnListItemFragmentInteractionListener) {
            listener = context
            listDataManager = ListDataManager(context)
        } else {
            throw RuntimeException(context.toString() + "must implement OnListItemFragmentInteractionListener")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("ListSelectionFragment", "onViewCreate")
        val lists = listDataManager.readList()
        view.let {
            listRecyclerView = it.findViewById(R.id.list_recyclerView)
            listRecyclerView.layoutManager = LinearLayoutManager(activity)
            listRecyclerView.adapter = ListSelectionRecyclerViewAdapter(lists, this)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("ListSelectionFragment", "onCreateView")
        return inflater.inflate(R.layout.fragment_list_selection, container, false)
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    companion object {
        @JvmStatic
        fun newInstance(): ListSelectionFragment {
            return ListSelectionFragment()
        }
    }

    override fun listItemClicked(list: TaskList) {
        listener?.onListItemClicked(list)
    }

    fun addList(list: TaskList) {
        listDataManager.saveList(list)
        val recyclerAdapter = listRecyclerView.adapter as ListSelectionRecyclerViewAdapter
        recyclerAdapter.addList(list)
    }

    fun saveList(list: TaskList) {
        listDataManager.saveList(list)
        updateLists()
    }

    private fun updateLists() {
        val lists = listDataManager.readList()
        listRecyclerView.adapter = ListSelectionRecyclerViewAdapter(lists, this)
    }
}