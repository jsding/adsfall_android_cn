package com.sherdle.universal.providers.videos.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SearchView.OnQueryTextListener;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.adsfall.R;
import com.sherdle.universal.MainActivity;
import com.sherdle.universal.providers.Provider;
import com.sherdle.universal.providers.videos.VideosAdapter;
import com.sherdle.universal.providers.videos.api.VideoProvider;
import com.sherdle.universal.providers.videos.api.VideosCallback;
import com.sherdle.universal.providers.videos.api.WordpressClient;
import com.sherdle.universal.providers.videos.api.object.Video;
import com.sherdle.universal.util.Helper;
import com.sherdle.universal.util.InfiniteRecyclerViewAdapter;
import com.sherdle.universal.util.ThemeUtils;
import com.sherdle.universal.util.ViewModeUtils;

import java.util.ArrayList;

/**
 * This activity is used to display a list of vidoes
 */
public class VideosFragment extends Fragment implements InfiniteRecyclerViewAdapter.LoadMoreListener, VideosCallback {

  //Layout references
  private RecyclerView listView;
  private RelativeLayout ll;
  private Activity mAct;
  private SwipeRefreshLayout swipeRefreshLayout;

  //Stores information
  private ArrayList<Video> videoList;
  private VideosAdapter videoAdapter;
  private VideoProvider videoApiClient;
  private ViewModeUtils viewModeUtils;

  //Keeping track of location & status
  private String upcomingPageToken;
  private String searchQuery;

  private boolean DIRECT_PLAY = false;

  @SuppressLint("InflateParams")
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ll = (RelativeLayout) inflater.inflate(R.layout.fragment_list_refresh, container, false);

    return ll;

  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setHasOptionsMenu(true);

    OnItemClickListener listener = (new OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Video video = videoList.get(position);

        String provider = VideosFragment.this.getArguments().getString(MainActivity.FRAGMENT_PROVIDER);

        if (videoApiClient.isYoutubeLive() || DIRECT_PLAY) {
        } else {
          Intent intent = new Intent(mAct, VideoDetailActivity.class);
          intent.putExtra(VideoDetailActivity.EXTRA_VIDEO, video);
          intent.putExtra(VideoDetailActivity.EXTRA_PROVIDER, provider);
          intent.putExtra(VideoDetailActivity.EXTRA_PARAMS, getPassedData());
          startActivity(intent);
        }
      }
    });

    listView = ll.findViewById(R.id.list);
    swipeRefreshLayout = ll.findViewById(R.id.swipeRefreshLayout);
    videoList = new ArrayList<>();
    videoAdapter = new VideosAdapter(getContext(), videoList, this, listener);
    videoAdapter.setModeAndNotify(InfiniteRecyclerViewAdapter.MODE_PROGRESS);
    listView.setAdapter(videoAdapter);
    listView.setLayoutManager(new LinearLayoutManager(ll.getContext(), LinearLayoutManager.VERTICAL, false));

    swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
      @Override
      public void onRefresh() {
        refreshItems();
      }
    });
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    mAct = getActivity();

    String providerString = this.getArguments().getString(MainActivity.FRAGMENT_PROVIDER);
    if (providerString.equals(Provider.YOUTUBE)) {
    } else if (providerString.equals(Provider.VIMEO)) {
    } else {
      videoApiClient = new WordpressClient(getPassedData(), mAct, this);
    }

    //Load the videos
    refreshItems();
  }

  private void refreshItems() {
    videoList.clear();
    videoAdapter.setHasMore(true);
    videoAdapter.setModeAndNotify(InfiniteRecyclerViewAdapter.MODE_PROGRESS);

    if ((
      getPassedData().length < 3 || getPassedData()[2].isEmpty())) {
      Toast.makeText(getActivity(), "Update your config.json file", Toast.LENGTH_LONG).show();
    } else {
      videoApiClient.requestVideos(null, searchQuery);
    }

  }

  private void updateList(ArrayList<Video> videos) {
    if (videos.size() > 0) {
      videoList.addAll(videos);
    }

    videoAdapter.setModeAndNotify(InfiniteRecyclerViewAdapter.MODE_LIST);
    swipeRefreshLayout.setRefreshing(false);
  }

  private String[] getPassedData() {
    return getArguments().getStringArray(MainActivity.FRAGMENT_DATA);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    viewModeUtils = new ViewModeUtils(getContext(), getClass());
    viewModeUtils.inflateOptionsMenu(menu, inflater);

    inflater.inflate(R.menu.menu_search, menu);

    //set & get the search button in the actionbar
    final SearchView searchView = new SearchView(mAct);
    searchView.setQueryHint(getResources().getString(R.string.search_hint));
    searchView.setOnQueryTextListener(new OnQueryTextListener() {

      @Override
      public boolean onQueryTextSubmit(String query) {
        searchQuery = query;
        refreshItems();
        searchView.clearFocus();
        return true;
      }

      @Override
      public boolean onQueryTextChange(String newText) {
        return false;
      }

    });

    searchView.addOnAttachStateChangeListener(new OnAttachStateChangeListener() {

      @Override
      public void onViewDetachedFromWindow(View arg0) {
        searchQuery = null;
        refreshItems();
      }

      @Override
      public void onViewAttachedToWindow(View arg0) {
        // search was opened
      }
    });


    if (videoApiClient.supportsSearch()) {
      menu.findItem(R.id.menu_search)
        .setActionView(searchView);
    } else {
      menu.findItem(R.id.menu_search).setVisible(false);
    }
    ThemeUtils.tintAllIcons(menu, mAct);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    viewModeUtils.handleSelection(item, new ViewModeUtils.ChangeListener() {
      @Override
      public void modeChanged() {
        videoAdapter.notifyDataSetChanged();
      }
    });
    switch (item.getItemId()) {
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void onMoreRequested() {
    if (null != upcomingPageToken) {
      videoApiClient.requestVideos(upcomingPageToken, searchQuery);
    }
  }

  @Override
  public void completed(ArrayList<Video> videos, boolean canLoadMore, String nextPageToken) {
    upcomingPageToken = nextPageToken;
    videoAdapter.setHasMore(canLoadMore);
    updateList(videos);

    if (videoApiClient.isYoutubeLive()) {
      if (videos.size() > 0) {
        LayoutInflater.from(mAct).inflate(R.layout.fragment_youtube_livefooter, ll);
        View liveBottomView = ll.findViewById(R.id.youtube_live_bottom);
        if (videos.size() == 1) {
          liveBottomView.setVisibility(View.VISIBLE);
        } else if (liveBottomView.getVisibility() == View.VISIBLE) {
          liveBottomView.setVisibility(View.GONE);
        }
      } else {
        //Emptyview set title
        videoAdapter.setEmptyViewText(
          getString(R.string.video_no_live_title),
          getString(R.string.video_no_live));
        videoAdapter.setModeAndNotify(InfiniteRecyclerViewAdapter.MODE_EMPTY);
        swipeRefreshLayout.setRefreshing(false);
      }
    }
  }

  @Override
  public void failed() {
    Helper.noConnection(mAct);
    videoAdapter.setModeAndNotify(InfiniteRecyclerViewAdapter.MODE_EMPTY);
    swipeRefreshLayout.setRefreshing(false);
  }
}