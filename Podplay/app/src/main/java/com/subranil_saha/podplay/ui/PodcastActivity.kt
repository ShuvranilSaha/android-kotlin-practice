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
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.subranil_saha.podplay.R
import com.subranil_saha.podplay.adapter.PodcastListAdapter
import com.subranil_saha.podplay.databinding.ActivityPodcastBinding
import com.subranil_saha.podplay.repository.ItunesRepo
import com.subranil_saha.podplay.repository.PodcastRepo
import com.subranil_saha.podplay.service.ItunesService
import com.subranil_saha.podplay.service.RssFeedService
import com.subranil_saha.podplay.viewmodel.PodcastViewModel
import com.subranil_saha.podplay.viewmodel.SearchViewModel
import kotlinx.coroutines.*

class PodcastActivity : AppCompatActivity(), PodcastListAdapter.PodcastListAdapterListener,
    PodcastDetailsFragment.OnPodcastDetailsListener {

    private lateinit var dataBinding: ActivityPodcastBinding
    private val searchViewModel by viewModels<SearchViewModel>()
    private lateinit var podcastListAdapter: PodcastListAdapter
    private lateinit var searchMenuItem: MenuItem
    private val podcastViewModel by viewModels<PodcastViewModel>()

    @InternalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataBinding = ActivityPodcastBinding.inflate(layoutInflater)
        setContentView(dataBinding.root)
        setupToolbar()
        setupViewModels()
        updateControls()
        handleIntent(intent)
        setupPodcastListView()
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
        searchMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                showSubscribedPodcast()
                return true
            }

        })
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

    @InternalCoroutinesApi
    private fun setupViewModels() {
        val service = ItunesService.instance
        searchViewModel.itunesRepo = ItunesRepo(service)
        podcastViewModel.podcastRepo =
            PodcastRepo(RssFeedService.instance, podcastViewModel.podcastDao)
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
        podcastSummaryViewData.feedUrl ?: return
        showProgressBar()
        podcastViewModel.viewModelScope.launch(context = Dispatchers.Main) {
            podcastViewModel.getPodcast(podcastSummaryViewData)
            hideProgressBar()
            showDetailsFragment()
        }
    }

    private fun showProgressBar() {
        dataBinding.progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        dataBinding.progressBar.visibility = View.INVISIBLE
    }

    companion object {
        private const val TAG_DETAILS_FRAGMENT = "DetailsFragment"
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

    @DelicateCoroutinesApi
    override fun onSubscribe() {
        podcastViewModel.saveActivePodcast()
        supportFragmentManager.popBackStack()
    }

    @DelicateCoroutinesApi
    override fun onUnsubscribe() {
        podcastViewModel.deleteActivePodcast()
        supportFragmentManager.popBackStack()
    }

    private fun showSubscribedPodcast() {
        val podcasts = podcastViewModel.getPodcasts()?.value
        if (podcasts != null) {
            dataBinding.toolbar.title = getString(R.string.subscribed_podcast)
            podcastListAdapter.setSearchData(podcasts)
        }
    }

    private fun setupPodcastListView() {
        podcastViewModel.getPodcasts()?.observe(this, {
            if (it != null) {
                showSubscribedPodcast()
            }
        })
    }
}