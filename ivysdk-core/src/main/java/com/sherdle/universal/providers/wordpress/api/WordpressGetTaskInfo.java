package com.sherdle.universal.providers.wordpress.api;

import android.app.Activity;

import androidx.recyclerview.widget.RecyclerView;

import com.sherdle.universal.providers.wordpress.PostItem;
import com.sherdle.universal.providers.wordpress.WordpressListAdapter;
import com.sherdle.universal.providers.wordpress.api.providers.JetPackProvider;
import com.sherdle.universal.providers.wordpress.api.providers.JsonApiProvider;
import com.sherdle.universal.providers.wordpress.api.providers.RestApiProvider;
import com.sherdle.universal.providers.wordpress.api.providers.WordpressProvider;

import java.util.ArrayList;

public class WordpressGetTaskInfo {

  //Paging and status
  public Integer pages;
  public Integer curpage = 0;
  public boolean isLoading;

  public WordpressListAdapter adapter = null;
  public ArrayList<PostItem> posts;

  //Static information about this instance
  public String baseurl;
  public Boolean simpleMode;
  public WordpressProvider provider = null;
  public Long ignoreId = 0L; //ID of post not to add

  //Views to track
  public RecyclerView listView = null;

  //References
  public Activity context;
  private ListListener listener;

  public WordpressGetTaskInfo(RecyclerView listView,
                              Activity context,
                              String baseurl,
                              Boolean simpleMode) {
    this.listView = listView;
    this.posts = new ArrayList<>();
    this.context = context;
    this.baseurl = baseurl;
    this.simpleMode = simpleMode;

    //We'll assume that sitenames don't contain http. Only sitesnames are accepted by the JetPack API.
    if (!baseurl.startsWith("http"))
      this.provider = new JetPackProvider();
    else if (baseurl.contains("wp-json/wp/v2/"))
      this.provider = new RestApiProvider();
    else
      this.provider = new JsonApiProvider();
  }

  public interface ListListener {
    void completedWithPosts();
  }

  public void setListener(ListListener listener) {
    this.listener = listener;
  }

  public void completedWithPosts() {
    if (this.listener != null) listener.completedWithPosts();
  }

}
