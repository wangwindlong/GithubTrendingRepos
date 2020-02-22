package com.gojek.trendingrepos.features.trendingrepo

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.gojek.trendingrepos.R
import com.gojek.trendingrepos.base.BaseActivity
import com.gojek.trendingrepos.commons.Error
import com.gojek.trendingrepos.commons.Loading
import com.gojek.trendingrepos.commons.Success
import com.gojek.trendingrepos.models.SortType
import com.gojek.trendingrepos.models.TrendingRepositoryUiModel
import com.gojek.trendingrepos.util.*
import jp.wasabeef.recyclerview.adapters.ScaleInAnimationAdapter
import kotlinx.android.synthetic.main.activity_trending_repository.*
import kotlinx.android.synthetic.main.layout_error_status_notifier.*
import kotlinx.android.synthetic.main.layout_status_loading.*
import javax.inject.Inject

class TrendingRepoSearchActivity : BaseActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val trendingRepositoryViewModel: TrendingRepositoryViewModel by viewModels { viewModelFactory }

    private var mBackPressed: Long = 0

    private val trendingRepoSearchResultAdapter: TrendingRepoSearchResultAdapter by lazy {
        TrendingRepoSearchResultAdapter {
            launchUrl(this, it.url)
        }
    }

    override fun initComponents(savedInstanceState: Bundle?) {
        enableHomeUp(R.string.title_trending)
        observeUiState()
        observeSearchResults()
        addListeners()
        fetchTrendingRepositories()
    }

    override fun getLayoutId() = R.layout.activity_trending_repository

    private fun observeUiState() {
        trendingRepositoryViewModel.uiState.observe(this, Observer {
            when (it) {
                is Loading -> displayLoadingState()
                is Success -> hideLoadingState()
                is Error -> displayErrorState(it.error)
            }
        })
    }

    private fun observeSearchResults() {
        trendingRepositoryViewModel.searchResultsTrendingRepositories.observe(this, Observer {
            displaySearchResults(it)
        })
    }

    private fun addListeners() {
        swipeRepoRefresh.setOnRefreshListener {
            fetchTrendingRepositories(true)
            swipeRepoRefresh.isRefreshing = false
        }
        lookUpButton.setOnClickListener {
            fetchTrendingRepositories()
        }

        vInvisible.setOnClickListener {
            showAlertMessage(getString(R.string.error_offline_test_scenario))
        }
    }

    private fun fetchTrendingRepositories(forceRefresh: Boolean = false) {
        trendingRepositoryViewModel.executeTrendingRepositorySearch(forceRefresh)
    }

    private fun displayLoadingState() {
        rvRepository.hide()
        containerShimmer.show()
        containerShimmer.showShimmer(true)
    }

    private fun hideLoadingState() {
        rvRepository.show()
        containerShimmer.hide()
        containerShimmer.stopShimmer()
    }

    private fun displaySearchResults(repoSearchResult: List<TrendingRepositoryUiModel>) {
        if (repoSearchResult.isNotEmpty()) {
            if (layoutError.isVisible) {
                layoutError.hide()
            }

            rvRepository.apply {
                adapter =
                    ScaleInAnimationAdapter(trendingRepoSearchResultAdapter.apply {
                        submitList(
                            repoSearchResult
                        )
                    })
                initRecyclerViewWithLineDecoration(this@TrendingRepoSearchActivity)
            }
        } else displayNoSearchResults()
    }

    private fun displayNoSearchResults() {
        showSnackbar(
            rvRepository,
            getString(R.string.label_no_times)
        )
    }

    private fun displayErrorState(error: Throwable) {
        hideLoadingState()
        layoutError.show()
        showSnackbar(rvRepository, "${error.message}")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sort_by_stars -> {
                trendingRepositoryViewModel.sortTrendingRepoResult(SortType.SortByStars)
                showSnackbar(rvRepository, getString(R.string.label_sorted_by_star))
                true
            }
            R.id.action_sort_by_name -> {
                trendingRepositoryViewModel.sortTrendingRepoResult(SortType.SortByName)
                showSnackbar(rvRepository, getString(R.string.label_sorted_repo_name))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (mBackPressed + 2000 > System.currentTimeMillis()) {
            super.onBackPressed()
        } else {
            toast(getString(R.string.info_again_exit))
        }
        mBackPressed = System.currentTimeMillis()
    }

}
