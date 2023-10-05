package com.sherdle.universal.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.adsfall.R;

/**
 * Created by imac on 31/08/2017.
 */

public abstract class InfiniteRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  public static final int MODE_LIST = 1;
  public static final int MODE_EMPTY = 2;
  public static final int MODE_PROGRESS = 3;

  // Assuming there is a single view type default getItemViewType() implementation returns 0
  private static final int VIEW_TYPE_LOAD_MORE = -1;
  private static final int VIEW_TYPE_PROGRESS = -2;
  private static final int VIEW_TYPE_EMPTY = -3;

  private int mMoreResourceId;
  private int mEmptyResourceId;
  private int mProgressResourceId;

  private LoadMoreListener mListener;
  private boolean mHasMore;
  private LayoutInflater mInflater;
  private int mCurrentMode = MODE_LIST; // This is the default mode in which it shows the list

  private String emptyViewTitle;
  private String emptyViewSubTitle;
  private String emptyViewButtonTitle;
  private View.OnClickListener emptyViewButtonListener;
  private String emptyViewSecondaryButtonTitle;
  private View.OnClickListener emptyViewSecondaryButtonListener;

  public InfiniteRecyclerViewAdapter(Context context, LoadMoreListener loadMoreListener) {
    mInflater = LayoutInflater.from(context);
    mListener = loadMoreListener;
    mMoreResourceId = R.layout.listview_footer;
    mEmptyResourceId = R.layout.listview_empty;
    mProgressResourceId = R.layout.listview_loading;
  }

  @Override
  public final RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    if (viewType == VIEW_TYPE_LOAD_MORE) {
      View moreView = mInflater.inflate(mMoreResourceId, parent, false);
      return new RecyclerView.ViewHolder(moreView) {
      };
    } else if (viewType == VIEW_TYPE_EMPTY) {
      View emptyView = mInflater.inflate(mEmptyResourceId, parent, false);
      emptyView.setMinimumHeight(parent.getHeight()); // This is required else the height o
      return new RecyclerView.ViewHolder(emptyView) {
      };
    } else if (viewType == VIEW_TYPE_PROGRESS) {
      View progressView = mInflater.inflate(mProgressResourceId, parent, false);
      progressView.setMinimumHeight(parent.getHeight()); // This is required else the height o
      return new RecyclerView.ViewHolder(progressView) {
      };
    } else {
      return getViewHolder(parent, viewType);
    }
  }

  @Override
  public final void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    if (holder.getItemViewType() == VIEW_TYPE_LOAD_MORE ||
      holder.getItemViewType() == VIEW_TYPE_EMPTY ||
      holder.getItemViewType() == VIEW_TYPE_PROGRESS) {
      requestFullSpan(holder);
    }

    if (holder.getItemViewType() == VIEW_TYPE_EMPTY &&
      emptyViewTitle != null &&
      emptyViewSubTitle != null) {
      ((TextView) holder.itemView.findViewById(R.id.title)).setText(emptyViewTitle);
      ((TextView) holder.itemView.findViewById(R.id.subtitle)).setText(emptyViewSubTitle);
    }

    if (holder.getItemViewType() == VIEW_TYPE_EMPTY &&
      emptyViewButtonTitle != null &&
      emptyViewButtonListener != null) {
      holder.itemView.findViewById(R.id.cloud_footer).setVisibility(View.GONE);

      Button button = holder.itemView.findViewById(R.id.empty_button);
      button.setVisibility(View.VISIBLE);
      button.setText(emptyViewButtonTitle);
      button.setOnClickListener(emptyViewButtonListener);
    }

    if (holder.getItemViewType() == VIEW_TYPE_EMPTY &&
      emptyViewSecondaryButtonTitle != null &&
      emptyViewSecondaryButtonListener != null) {
      holder.itemView.findViewById(R.id.cloud_footer).setVisibility(View.GONE);

      Button button = holder.itemView.findViewById(R.id.empty_button_secondary);
      button.setVisibility(View.VISIBLE);
      button.setText(emptyViewSecondaryButtonTitle);
      button.setOnClickListener(emptyViewSecondaryButtonListener);
    }

    if (holder.getItemViewType() == VIEW_TYPE_LOAD_MORE) {
      if (mListener != null)
        mListener.onMoreRequested();
    } else if (mCurrentMode == MODE_LIST) { // Only calling this when in list mode
      doBindViewHolder(holder, position);
    }
  }

  protected void requestFullSpan(RecyclerView.ViewHolder holder) {
    if (holder.itemView.getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams) {
      StaggeredGridLayoutManager.LayoutParams layoutParams = (StaggeredGridLayoutManager.LayoutParams) holder.itemView.getLayoutParams();
      layoutParams.setFullSpan(true);
      holder.itemView.setLayoutParams(layoutParams);
    }
  }

  @Override
  public final int getItemCount() {
    if (mCurrentMode == MODE_EMPTY || mCurrentMode == MODE_PROGRESS) {
      return 1;
    }

    if (mHasMore) {
      return getCount() + 1;
    }

    return getCount();
  }

  @Override
  public final int getItemViewType(int position) {
    if (mCurrentMode == MODE_EMPTY) {
      return VIEW_TYPE_EMPTY;
    }

    if (mCurrentMode == MODE_PROGRESS) {
      return VIEW_TYPE_PROGRESS;
    }

    if (position == getCount() && mHasMore) {
      return (VIEW_TYPE_LOAD_MORE);
    }

    return getViewType(position);
  }

  /**
   * Set display mode of the RecyclerView.
   *
   * @param mode Either List mode, Empty mode, Fullscreen progress mode
   */
  public void setModeAndNotify(int mode) {
    mCurrentMode = mode;
    notifyDataSetChanged();
  }

  public void setEmptyViewText(String title, String subTitle) {
    this.emptyViewSubTitle = subTitle;
    this.emptyViewTitle = title;
  }

  public void setEmptyViewButton(String text, View.OnClickListener listener) {
    this.emptyViewButtonListener = listener;
    this.emptyViewButtonTitle = text;
  }

  public void setEmptyViewSecondaryButton(String text, View.OnClickListener listener) {
    this.emptyViewSecondaryButtonListener = listener;
    this.emptyViewSecondaryButtonTitle = text;
  }

  /**
   * Informs the adapter if there are more items to be loaded
   */
  public void setHasMore(boolean hasMore) {
    this.mHasMore = hasMore;
  }

  /**
   * Implement this to return your specialized view type
   */
  protected abstract int getViewType(int position);

  /**
   * Implement this to return your specialized ViewHolder
   */
  protected abstract RecyclerView.ViewHolder getViewHolder(ViewGroup parent, int viewType);

  /**
   * Implement this to specify your ViewHolder bindings
   */
  protected abstract void doBindViewHolder(RecyclerView.ViewHolder holder, int position);

  /**
   * Implement this to return the number of items represented by this adapter
   */
  protected abstract int getCount();

  /**
   * Called when adapter has reached the end of the data set.
   * Any actions should be performed in a separate thread.
   */
  public interface LoadMoreListener {
    void onMoreRequested();
  }
}
