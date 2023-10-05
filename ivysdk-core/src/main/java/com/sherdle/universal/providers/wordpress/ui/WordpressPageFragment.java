package com.sherdle.universal.providers.wordpress.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.fragment.app.Fragment;

import com.adsfall.R;
import com.sherdle.universal.MainActivity;
import com.sherdle.universal.inherit.CollapseControllingFragment;
import com.sherdle.universal.providers.web.WebviewFragment;
import com.sherdle.universal.providers.wordpress.PostItem;
import com.sherdle.universal.providers.wordpress.api.WordpressGetTaskInfo;
import com.sherdle.universal.util.ThemeUtils;
import com.sherdle.universal.util.WebHelper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;


/**
 * This file is part of the Universal template
 * For license information, please check the LICENSE
 * file in the root of this project
 *
 * @author Sherdle
 * Copyright 2019
 */

public class WordpressPageFragment extends Fragment implements CollapseControllingFragment {

  private Activity mAct;
  private FrameLayout ll;

  //The arguments we started this fragment with
  private String[] arguments;

  @SuppressLint("InflateParams")
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ll = (FrameLayout) inflater.inflate(R.layout.fragment_host,
      container, false);
    setHasOptionsMenu(true);

    arguments = this.getArguments().getStringArray(MainActivity.FRAGMENT_DATA);

    //Create a new request for the page, and execute it on the background
    WordpressGetTaskInfo info = new WordpressGetTaskInfo(null, null, arguments[0], false);
    String requestUrl = info.provider.getPage(info, arguments[1]);

    Runnable runnable = new Runnable() {
      @Override
      public void run() {
        System.out.println("Requesting: " + requestUrl);
        ArrayList<PostItem> posts = info.provider.parsePostsFromUrl(info,
          requestUrl);

        if (posts != null && posts.size() > 0) {
          Fragment fragment;
          fragment = new WebviewFragment();

          Document doc = Jsoup.parse(posts.get(0).getContent());
          String html = WebHelper.docToBetterHTML(doc, getActivity(), false);

          // adding the data
          Bundle bundle = new Bundle();
          bundle.putStringArray(MainActivity.FRAGMENT_DATA, new String[]{""});
          bundle.putBoolean(WebviewFragment.HIDE_NAVIGATION, true);
          bundle.putString(WebviewFragment.LOAD_DATA, html);
          fragment.setArguments(bundle);
          getChildFragmentManager().beginTransaction().replace(R.id.root_frame, fragment).commit();

        } else {
          System.out.println("No results");
          mAct.runOnUiThread(new Runnable() {
            @Override
            public void run() {
              View error = getLayoutInflater().inflate(R.layout.listview_empty, null);
              ll.findViewById(R.id.progressBarHolder).setVisibility(View.GONE);
              ll.addView(error);
            }
          });
        }
      }
    };
    AsyncTask.execute(runnable);

    return ll;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    mAct = getActivity();
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    ThemeUtils.tintAllIcons(menu, mAct);
  }

  @Override
  public boolean supportsCollapse() {
    return true;
  }

  @Override
  public boolean dynamicToolbarElevation() {
    return false;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    switch (item.getItemId()) {
      default:
        return super.onOptionsItemSelected(item);
    }
  }

}
