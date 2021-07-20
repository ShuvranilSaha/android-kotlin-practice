package com.subranil_saha.podplay.ui

import androidx.appcompat.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.subranil_saha.podplay.R
import com.subranil_saha.podplay.adapter.PodcastListAdapter
import com.subranil_saha.podplay.databinding.ActivityPodcastBinding
import com.subranil_saha.podplay.repository.ItunesRepo
import com.subranil_saha.podplay.repository.PodcastRepo
import com.subranil_saha.podplay.service.ItunesService
import com.subranil_saha.podplay.viewmodel.PodcastViewModel
import com.subranil_saha.podplay.viewmodel.SearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PodcastActivity : AppCompatActivity(), PodcastListAdapter.PodcastListAdapterListener {

    private lateinit var dataBinding: ActivityPodcastBinding
    private val searchViewModel by viewModels<SearchViewModel>()
    private lateinit var podcastListAdapter: PodcastListAdapter
    private lateinit var searchMenuItem: MenuItem
    private val podcastViewModel by viewModels<PodcastViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataBinding = ActivityPodcastBinding.inflate(layoutInflater)
        setContentView(dataBinding.root)
        setupToolbar()
        setupViewModels()
        updateControls()
        handleIntent(intent)
        addBackStackListener()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_search, menu)
        searchMenuItem = menu.findItem(R.id.search_item)
        val searchView = searchMenuItem.actionView as SearchView
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        if (supportFragmentManager.backStackEntryCount > 0) {
            dataBinding.podcastRecyclerView.visibility = View.INVISIBLE
        }
        if (dataBinding.podcastRecyclerView.visibility == View.INVISIBLE) {
            searchMenuItem.isVisible = false
        }
        return true
    }

    private fun performSearch(term: String) {
        showProgressBar()
        GlobalScope.launch {
            val results = searchViewModel.searchPodcasts(term)
            withContext(Dispatchers.Main) {
                hideProgressBar()
                dataBinding.toolbar.title = term
                podcastListAdapter.setSearchData(results)
            }
        }
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action === Intent.ACTION_SEARCH) {
            val query = intent.getStringExtra(SearchManager.QUERY) ?: return
            performSearch(query)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(dataBinding.toolbar)
    }

    private fun setupViewModels() {
        val service = ItunesService.instance
        searchViewModel.itunesRepo = ItunesRepo(service)
        podcastViewModel.podcastRepo = PodcastRepo()
    }

    private fun updateControls() {
        dataBinding.podcastRecyclerView.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(this)

        dataBinding.podcastRecyclerView.layoutManager = layoutManager
        val dividerItemDecoration = DividerItemDecoration(
            dataBinding.podcastRecyclerView.context, layoutManager.orientation
        )
        dataBinding.podcastRecyclerView.addItemDecoration(dividerItemDecoration)

        podcastListAdapter = PodcastListAdapter(null, this, this)
        dataBinding.podcastRecyclerView.adapter = podcastListAdapter

    }

    override fun onShowDetails(podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData) {
        val feedUrl = podcastSummaryViewData.feedUrl ?: return
        showProgressBar()
        val podcast = podcastViewModel.getPodcast(podcastSummaryViewData)
        hideProgressBar()
        if (podcast != null) {
            showDetailsFragment()
        } else {
            showError("Error load $feedUrl")
        }
    }

    private fun showProgressBar() {
        dataBinding.progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        dataBinding.progressBar.visibility = View.INVISIBLE
    }

    private fun createPodcastDetailsFragment(): PodcastDetailsFragment {
        var podcastDetailsFragment =
            supportFragmentManager.findFragmentByTag(TAG_DETAILS_FRAGMENT) as PodcastDetailsFragment?
        if (podcastDetailsFragment == null) {
            podcastDetailsFragment = PodcastDetailsFragment.newInstance()
        }
        return podcastDetailsFragment
    }

    private fun showDetailsFragment() {
        val podcastDetailsFragment = createPodcastDetailsFragment()
        supportFragmentManager.beginTransaction()
            .add(R.id.podcast_details_container, podcastDetailsFragment, TAG_DETAILS_FRAGMENT)
            .addToBackStack("DetailsFragment")
            .commit()
        dataBinding.podcastRecyclerView.visibility = View.INVISIBLE
        searchMenuItem.isVisible = false
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok_btn), null)
            .create().show()
    }

    private fun addBackStackListener() {
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                dataBinding.podcastRecyclerView.visibility = View.VISIBLE
            }
        }
    }

    companion object {
        private const val TAG_DETAILS_FRAGMENT = "DetailsFragment"
    }
}