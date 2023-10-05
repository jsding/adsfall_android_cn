package com.sherdle.universal.providers.wordpress.api;

import android.os.AsyncTask;

import com.sherdle.universal.providers.wordpress.PostItem;

import java.util.ArrayList;

/**
 * Simply loads data from an url (gotten from a provider) and loads it into a list.
 * Various attributes of this list and the way to load are defined in a WordpressGetTaskInfo.
 */
public class WordpressPostsTask extends AsyncTask<String, Integer, ArrayList<PostItem>> {

  private String url;
  private WordpressGetTaskInfo info;
  private WordpressPostsCallback callback;

  public static final int PER_PAGE = 15;
  public static final int PER_PAGE_RELATED = 4;


  public WordpressPostsTask(String url, WordpressGetTaskInfo info, WordpressPostsCallback callback) {
    this.url = url;
    this.info = info;
    this.callback = callback;
  }

  @Override
  protected void onPreExecute() {
  }

  @Override
  protected ArrayList<PostItem> doInBackground(String... params) {
    info.curpage = info.curpage + 1;
    url = url + Integer.toString(info.curpage);

    return info.provider.parsePostsFromUrl(info, url);
  }

  @Override
  protected void onPostExecute(ArrayList<PostItem> result) {

    //Check if the response was null
    if (null != result) {
      callback.postsLoaded(result);
    } else {
      callback.postsFailed();
    }
  }

  public interface WordpressPostsCallback {
    void postsLoaded(ArrayList<PostItem> result);

    void postsFailed();
  }

}
