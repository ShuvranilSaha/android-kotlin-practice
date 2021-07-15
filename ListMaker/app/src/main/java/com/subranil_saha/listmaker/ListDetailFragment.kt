package com.subranil_saha.listmaker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * A simple [Fragment] subclass.
 * Use the [ListDetailFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ListDetailFragment : Fragment() {
    lateinit var list: TaskList
    private lateinit var listItemsRecyclerView: RecyclerView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        list = arguments?.getParcelable(MainActivity.INTENT_LIST_KEY)!!
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_list_detail, container, false)
        view?.let {
            listItemsRecyclerView = it.findViewById(R.id.lists_items_reyclerview)
            listItemsRecyclerView.adapter = ListItemRecyclerViewAdapter(list)
            listItemsRecyclerView.layoutManager = LinearLayoutManager(activity)
        }
        return view
    }

    fun addTask(item: String) {
        list.tasks.add(item)
        val recyclerAdapter = listItemsRecyclerView.adapter as ListItemRecyclerViewAdapter
        recyclerAdapter.list = list
        recyclerAdapter.notifyDataSetChanged()
    }

    companion object {
        @JvmStatic
        fun newInstance(list: TaskList): ListDetailFragment {
            val fragment = ListDetailFragment()
            val args = Bundle()
            args.putParcelable(MainActivity.INTENT_LIST_KEY, list)
            fragment.arguments = args
            return fragment
        }
    }
}