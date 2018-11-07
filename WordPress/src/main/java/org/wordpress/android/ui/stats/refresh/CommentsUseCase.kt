package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.CommentsModel
import org.wordpress.android.fluxc.store.InsightsStore
import org.wordpress.android.ui.stats.refresh.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.BlockListItem.Label
import org.wordpress.android.ui.stats.refresh.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.BlockListItem.ListItem
import org.wordpress.android.ui.stats.refresh.BlockListItem.TabsItem
import org.wordpress.android.ui.stats.refresh.BlockListItem.TabsItem.Tab
import org.wordpress.android.ui.stats.refresh.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.BlockListItem.UserItem
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewFollowersStats
import javax.inject.Inject

private const val PAGE_SIZE = 6

class CommentsUseCase
@Inject constructor(
    private val insightsStore: InsightsStore
) {
    private val mutableNavigationTarget = MutableLiveData<NavigationTarget>()
    val navigationTarget: LiveData<NavigationTarget> = mutableNavigationTarget

    suspend fun loadComments(site: SiteModel, forced: Boolean = false): InsightsItem {
        val response = insightsStore.fetchComments(site, forced)
        val model = response.model
        val error = response.error

        return when {
            error != null -> Failed(R.string.stats_view_comments, error.message ?: error.type.name)
            model != null -> loadComments(site, model)
            else -> throw IllegalArgumentException("Unexpected empty body")
        }
    }

    private fun loadComments(site: SiteModel, model: CommentsModel): ListInsightItem {
        val items = mutableListOf<BlockListItem>()
        items.add(Title(string.stats_view_comments))
        items.add(TabsItem(listOf(buildAuthorsTab(model.authors), buildPostsTab(model.posts))))
        items.add(Link(text = string.stats_insights_view_more) {
            mutableNavigationTarget.value = ViewFollowersStats(site.siteId)
        })
        return ListInsightItem(items)
    }

    private fun buildAuthorsTab(authors: List<CommentsModel.Author>): Tab {
        val mutableItems = mutableListOf<BlockListItem>()
        if (authors.isNotEmpty()) {
            mutableItems.add(Label(R.string.stats_comments_author_label, R.string.stats_comments_label))
            mutableItems.addAll(authors.take(PAGE_SIZE).mapIndexed { index, author ->
                UserItem(
                        author.gravatar,
                        author.name,
                        author.comments.toFormattedString(),
                        index < authors.size - 1
                )
            })
        } else {
            mutableItems.add(Empty)
        }
        return Tab(R.string.stats_comments_authors, mutableItems)
    }

    private fun buildPostsTab(posts: List<CommentsModel.Post>): Tab {
        val mutableItems = mutableListOf<BlockListItem>()
        if (posts.isNotEmpty()) {
            mutableItems.add(Label(R.string.stats_comments_title_label, R.string.stats_comments_label))
            mutableItems.addAll(posts.take(PAGE_SIZE).mapIndexed { index, post ->
                ListItem(
                        post.name,
                        post.comments.toFormattedString(),
                        index < posts.size - 1
                )
            })
        } else {
            mutableItems.add(Empty)
        }
        return Tab(R.string.stats_comments_posts_and_pages, mutableItems)
    }
}
