package com.sherdle.universal.providers.overview.ui;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.adsfall.R;
import com.sherdle.universal.HolderActivity;
import com.sherdle.universal.MainActivity;
import com.sherdle.universal.drawer.NavItem;
import com.sherdle.universal.providers.overview.OverviewAdapter;
import com.sherdle.universal.providers.overview.OverviewParser;
import com.sherdle.universal.util.Helper;
import com.sherdle.universal.util.InfiniteRecyclerViewAdapter;

import java.util.ArrayList;

/**
 * This file is part of the Universal template
 * For license information, please check the LICENSE
 * file in the root of this project
 *
 * @author Sherdle
 * Copyright 2021
 */
public class OverviewFragment extends Fragment implements OverviewAdapter.OnOverViewClick {

  //Views
  private RelativeLayout rl;
  private RecyclerView mRecyclerView;
  private ArrayList<NavItem> items;

  private String overviewString;
  private DividerItemDecoration horizontalDec;

  //List
  private OverviewAdapter multipleItemAdapter;

  private ViewTreeObserver.OnGlobalLayoutListener recyclerListener;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    rl = (RelativeLayout) inflater.inflate(R.layout.fragment_list, null);
    setHasOptionsMenu(true);
    mRecyclerView = rl.findViewById(R.id.list);

    final StaggeredGridLayoutManager mLayoutManager = new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL);
    mRecyclerView.setLayoutManager(mLayoutManager);
    items = new ArrayList<>();
    multipleItemAdapter = new OverviewAdapter(items, getContext(), OverviewFragment.this);
    mRecyclerView.setAdapter(multipleItemAdapter);
    multipleItemAdapter.setModeAndNotify(InfiniteRecyclerViewAdapter.MODE_PROGRESS);

    overviewString = this.getArguments().getStringArray(MainActivity.FRAGMENT_DATA)[0];

    recyclerListener = new ViewTreeObserver.OnGlobalLayoutListener() {
      @Override
      public void onGlobalLayout() {
        //Verify that the fragment is still attached
        if (getActivity() == null || !isAdded()) return;

        //Get the view width, and check if it could be valid
        int viewWidth = mRecyclerView.getMeasuredWidth();
        if (viewWidth <= 0) return;

        //Remove the VTO
        mRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

        //Calculate and update the span
        float cardViewWidth = getResources().getDimension(R.dimen.card_width_overview);
        int newSpanCount = Math.max(1, (int) Math.floor(viewWidth / cardViewWidth));
        mLayoutManager.setSpanCount(newSpanCount);
        mLayoutManager.requestLayout();

        if (newSpanCount > 1) {
          mRecyclerView.addItemDecoration(horizontalDec);
        } else {
          mRecyclerView.removeItemDecoration(horizontalDec);
        }
      }
    };
    mRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(recyclerListener);

    horizontalDec = new DividerItemDecoration(mRecyclerView.getContext(),
      DividerItemDecoration.HORIZONTAL);

    DividerItemDecoration verticalDec = new DividerItemDecoration(mRecyclerView.getContext(),
      DividerItemDecoration.VERTICAL);
    mRecyclerView.addItemDecoration(verticalDec);


    //Load items
    loadItems();

    return rl;
  }

  public void loadItems() {
    new OverviewParser(overviewString, getActivity(), new OverviewParser.CallBack() {
      @Override
      public void categoriesLoaded(ArrayList<NavItem> result, boolean failed) {
        //Verify that the fragment is still attached
        if (getActivity() == null || !isAdded()) return;

        if (failed) {
          //If it failed; show an error if we're using a local file, or if we are online & using a remote overview.
          if (!overviewString.contains("http") || Helper.isOnlineShowDialog(getActivity())) {
            Toast.makeText(getActivity(), R.string.invalid_configuration, Toast.LENGTH_LONG).show();
            multipleItemAdapter.setModeAndNotify(InfiniteRecyclerViewAdapter.MODE_EMPTY);
          }
        } else {
          //Add all the new posts to the list and notify the adapter
          items.addAll(result);
          multipleItemAdapter.setModeAndNotify(InfiniteRecyclerViewAdapter.MODE_LIST);
        }
      }
    }).execute();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    mRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(recyclerListener);
  }

  @Override
  public void onOverViewSelected(NavItem item) {
    HolderActivity.startActivity(getActivity(), item.getFragment(), item.getProvider(), item.getData());
  }
}
