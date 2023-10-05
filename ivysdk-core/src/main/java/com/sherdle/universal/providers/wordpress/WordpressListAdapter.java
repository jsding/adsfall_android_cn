package com.sherdle.universal.providers.wordpress;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.adsfall.R;
import com.sherdle.universal.providers.wordpress.ui.WordpressFragment;
import com.sherdle.universal.util.Helper;
import com.sherdle.universal.util.InfiniteRecyclerViewAdapter;
import com.sherdle.universal.util.ViewModeUtils;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class WordpressListAdapter extends InfiniteRecyclerViewAdapter {

  private ArrayList<PostItem> listData;
  private Context mContext;
  private AdapterView.OnItemClickListener listener;
  private ViewModeUtils viewModeUtils;

  private boolean simpleMode;
  private View sliderView;

  //Show nice gradients
  private int number;

  //Post types
  private final static int POST_COMPACT = 0;
  private final static int POST = 1;
  private final static int HEADER_IMAGE = 2;
  private final static int HEADER_TEXT = 3;
  private final static int SLIDER = 4;

  public WordpressListAdapter(Context context,
                              ArrayList<PostItem> listData,
                              LoadMoreListener listener,
                              AdapterView.OnItemClickListener clickListener,
                              boolean simpleMode) {
    super(context, listener);
    this.mContext = context;
    this.listener = clickListener;
    this.simpleMode = simpleMode;
    this.listData = listData;

    viewModeUtils = new ViewModeUtils(context, WordpressFragment.class);
  }

  @Override
  public int getCount() {
    return listData.size();
  }

  @Override
  protected int getViewType(int position) {
    if (simpleMode) return POST_COMPACT;

    PostItem newsItem = listData.get(position);
    if (newsItem.getPostType() == PostItem.PostType.SLIDER) {
      return SLIDER;
    } else if (position == 0 || viewModeUtils.getViewMode() == ViewModeUtils.IMMERSIVE) {
      return headerType(newsItem);
    } else if (viewModeUtils.getViewMode() == ViewModeUtils.NORMAL) {
      return POST;
    } else {
      return POST_COMPACT;
    }
  }

  @Override
  protected RecyclerView.ViewHolder getViewHolder(ViewGroup parent, int viewType) {
    switch (viewType) {
      case HEADER_IMAGE:

        View itemViewHeader = LayoutInflater.from(parent.getContext()).inflate(R.layout.listview_highlight, parent, false);
        RecyclerView.ViewHolder holderImage = new HeaderImageViewHolder(itemViewHeader);
        requestFullSpan(holderImage);
        return holderImage;

      case HEADER_TEXT:

        View itemViewHeaderText = LayoutInflater.from(parent.getContext()).inflate(R.layout.listview_highlight_text, parent, false);
        RecyclerView.ViewHolder holderText = new HeaderTextViewHolder(itemViewHeaderText);
        requestFullSpan(holderText);
        return holderText;

      case SLIDER:
        RecyclerView.ViewHolder holderSlider = new SliderViewHolder(sliderView);
        requestFullSpan(holderSlider);
        return holderSlider;

      case POST:
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.listview_row, parent, false);
        return new ItemViewHolder(itemView);

      case POST_COMPACT:
        View itemViewCompact = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_wordpress_list_row, parent, false);
        return new ItemViewHolder(itemViewCompact);

      default:
        return null;
    }
  }

  @Override
  protected void doBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
    PostItem post = listData.get(position);
    if (holder instanceof HeaderImageViewHolder) {
      HeaderImageViewHolder headerHolder = (HeaderImageViewHolder) holder;

      headerHolder.imageView.setImageBitmap(null);
      Picasso.get().load(post.getImageCandidate()).placeholder(R.drawable.placeholder).fit().centerCrop().into(headerHolder.imageView);

      headerHolder.headlineView.setText(post.getTitle());
      headerHolder.dateView.setText(DateUtils.getRelativeDateTimeString(
        mContext, post.getDate().getTime(),
        DateUtils.SECOND_IN_MILLIS,
        DateUtils.WEEK_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_ALL));
    } else if (holder instanceof HeaderTextViewHolder) {
      HeaderTextViewHolder headerHolder = (HeaderTextViewHolder) holder;

      headerHolder.itemView.findViewById(R.id.background).setBackgroundResource(randomGradientResource());

      headerHolder.headlineView.setText(post.getTitle());
      headerHolder.dateView.setText(DateUtils.getRelativeDateTimeString(
        mContext, post.getDate().getTime(),
        DateUtils.SECOND_IN_MILLIS,
        DateUtils.WEEK_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_ALL));
    } else if (holder instanceof SliderViewHolder) {
      //
    } else if (holder instanceof ItemViewHolder) {
      ItemViewHolder itemHolder = (ItemViewHolder) holder;

      itemHolder.imageView.setImageBitmap(null);
      itemHolder.headlineView.setText(post.getTitle());

      //Set date
      if (post.getDate() != null) {
        itemHolder.reportedDateView.setVisibility(View.VISIBLE);
        itemHolder.reportedDateView.setText(
          DateUtils.getRelativeDateTimeString(
            mContext, post.getDate().getTime(),
            DateUtils.SECOND_IN_MILLIS,
            DateUtils.WEEK_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_ALL)
        );
      } else {
        itemHolder.reportedDateView.setVisibility(View.GONE);
      }

      //Set thumbnail image
      itemHolder.imageView.setVisibility(View.GONE);
      boolean largeRowLayout = viewModeUtils.getViewMode() == ViewModeUtils.NORMAL;
      String candidate = largeRowLayout ? post.getImageCandidate() : post.getThumbnailCandidate();
      if (null != candidate && !candidate.equals("")) {
        itemHolder.imageView.setVisibility(View.VISIBLE);
        Picasso.get().load(candidate).fit().centerInside().into(itemHolder.imageView);
      }
    }

    if (!(holder instanceof SliderViewHolder)) {
      holder.itemView.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          listener.onItemClick(null, holder.itemView, position, 0);
        }
      });
    }
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  private int headerType(PostItem newsItem) {
    if (null != newsItem.getImageCandidate() && !newsItem.getImageCandidate().equals(""))
      return HEADER_IMAGE;
    else
      return HEADER_TEXT;
  }

  private int randomGradientResource() {
    number += 1;
    if (number == 6) number = 1;

    return Helper.getGradient(number);
  }

  public void setSlider(View sliderView) {
    //This should only happen the first time setSlider is called
    if (this.sliderView == null
      && viewModeUtils.getViewMode() != ViewModeUtils.IMMERSIVE
      && listData.size() > 0) {
      listData.add(1, new PostItem(PostItem.PostType.SLIDER));
    }

    this.sliderView = sliderView;
    notifyDataSetChanged();
  }

  public void removeSlider() {
    if (this.sliderView != null
      && listData.size() > 0
      && listData.get(1).getPostType() == PostItem.PostType.SLIDER) {
      listData.remove(1);
      this.sliderView = null;
    }
  }

  private static class ItemViewHolder extends RecyclerView.ViewHolder {
    TextView headlineView;
    TextView reportedDateView;
    ImageView imageView;

    ItemViewHolder(View view) {
      super(view);

      this.headlineView = view.findViewById(R.id.title);
      this.reportedDateView = view.findViewById(R.id.date);
      this.imageView = view.findViewById(R.id.thumbImage);

    }
  }

  private static class SliderViewHolder extends RecyclerView.ViewHolder {
    SliderViewHolder(View view) {
      super(view);
    }
  }

  private static class HeaderImageViewHolder extends HeaderViewHolder {
    HeaderImageViewHolder(View view) {
      super(view);
    }
  }

  private static class HeaderTextViewHolder extends HeaderViewHolder {
    HeaderTextViewHolder(View view) {
      super(view);
    }
  }

  private static abstract class HeaderViewHolder extends RecyclerView.ViewHolder {
    TextView headlineView;
    TextView dateView;
    ImageView imageView;

    HeaderViewHolder(View view) {
      super(view);

      this.dateView = view.findViewById(R.id.textViewDate);
      this.headlineView = view.findViewById(R.id.textViewHighlight);
      this.imageView = view.findViewById(R.id.imageViewHighlight);

    }
  }
}
