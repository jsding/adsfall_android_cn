package com.sherdle.universal.providers.wordpress.api;

import android.widget.Toast;

import com.adsfall.R;
import com.sherdle.universal.providers.wordpress.PostItem;
import com.sherdle.universal.providers.wordpress.api.providers.JsonApiProvider;
import com.sherdle.universal.providers.wordpress.api.providers.RestApiProvider;
import com.sherdle.universal.util.Helper;
import com.sherdle.universal.util.InfiniteRecyclerViewAdapter;

import java.util.ArrayList;

/**
 * Simply loads data from an url (gotten from a provider) and loads it into a list.
 * Various attributes of this list and the way to load are defined in a WordpressGetTaskInfo.
 */
public class WordpressPostsLoader implements WordpressPostsTask.WordpressPostsCallback {

  private String url;
  private boolean initialload;
  private WordpressGetTaskInfo info;

  public static String getRecentPosts(WordpressGetTaskInfo info) {
    //Let the provider compose an API url
    String url = info.provider.getRecentPosts(info);

    new WordpressPostsLoader(url, true, info).load();

    return url;
  }

  public static String getTagPosts(WordpressGetTaskInfo info, String tag) {
    //Let the provider compose an API url
    String url = info.provider.getTagPosts(info, tag);

    new WordpressPostsLoader(url, true, info).load();

    return url;
  }

  public static String getCategoryPosts(WordpressGetTaskInfo info, String category) {
    //Let the provider compose an API url
    String url = info.provider.getCategoryPosts(info, category);

    new WordpressPostsLoader(url, true, info).load();

    return url;
  }

  public static String getSearchPosts(WordpressGetTaskInfo info, String query) {
    //A search request might interfere with a current loading therefore
    //we disable loading to ensure we can start a new request
    if (info.isLoading) {
      info.isLoading = false;
    }

    //Let the provider compose an API url
    String url = info.provider.getSearchPosts(info, query);

    new WordpressPostsLoader(url, true, info).load();

    return url;
  }


  public static void loadMorePosts(WordpressGetTaskInfo info, String withUrl) {
    new WordpressPostsLoader(withUrl, false, info).load();
  }

  private WordpressPostsLoader(String url, boolean firstload, WordpressGetTaskInfo info) {
    this.url = url;
    this.initialload = firstload;
    this.info = info;
  }

  private void load() {
    if (info.isLoading) {
      return;
    } else {
      info.isLoading = true;
    }

    if (initialload) {
      //Show the full screen loading layout
      info.adapter.setModeAndNotify(InfiniteRecyclerViewAdapter.MODE_PROGRESS);
      info.adapter.setHasMore(true);
      info.posts.clear();

      //Reset the page parameter and listview
      info.curpage = 0;
    }

    //Fetch the posts
    new WordpressPostsTask(url, info, this).execute();
  }

  private void complete() {
    info.isLoading = false;
  }

  private void updateList(ArrayList<PostItem> posts) {
    if (posts.size() > 0) {
      info.posts.addAll(posts);
    }

    if (info.curpage >= info.pages || info.simpleMode)
      info.adapter.setHasMore(false);

    info.adapter.setModeAndNotify(InfiniteRecyclerViewAdapter.MODE_LIST);
  }

  private void showErrorMessage() {
    String message;
    if (info.baseurl.contains("\"")) {
      message = "Your parameters should not contain the character \". Note that the quotes in the documentation only represent the parameters to enter in the configurator tool.";
    } else if ((info.baseurl.endsWith("/")) && info.provider instanceof JsonApiProvider) {
      message = "Your base url " + info.baseurl + "should not not end with / (slash)";
    } else if (!info.baseurl.endsWith("/v2/") && info.provider instanceof RestApiProvider) {
      message = info.baseurl + " is not a valid base url for the Wordpress REST API. A base url usually ends with wp-json/wp/v2/";
    } else {
      message = "The result of '" + url + info.curpage + "' does not appear to return valid JSON or at least not in the expected format.";
    }

    if (info.posts == null || info.posts.size() == 0)
      info.adapter.setModeAndNotify(InfiniteRecyclerViewAdapter.MODE_EMPTY);

    Helper.noConnection(info.context, message);
  }

  @Override
  public void postsLoaded(ArrayList<PostItem> result) {
    updateList(result);

    //Alert if we have simply 0 posts, but a valid response
    if (null != result && result.size() < 1 && !info.simpleMode) {
      Toast.makeText(
        info.context,
        info.context.getResources().getString(R.string.no_results),
        Toast.LENGTH_LONG).show();
      info.adapter.setModeAndNotify(InfiniteRecyclerViewAdapter.MODE_LIST);
    } else if (result != null && result.size() > 0) {
      info.completedWithPosts();
    }

    complete();
  }

  @Override
  public void postsFailed() {
    showErrorMessage();
    complete();
  }

}
