package com.sherdle.universal.providers.videos;

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
import com.sherdle.universal.providers.videos.api.object.Video;
import com.sherdle.universal.providers.videos.ui.VideosFragment;
import com.sherdle.universal.util.InfiniteRecyclerViewAdapter;
import com.sherdle.universal.util.ViewModeUtils;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Setting our custom listview rows with the retrieved videos
 */
public class VideosAdapter extends InfiniteRecyclerViewAdapter {

  private List<Video> videos;
  private Context mContext;
  private AdapterView.OnItemClickListener clickListener;
  private ViewModeUtils viewModeUtils;

  private Picasso picasso;

  //Post types
  private final static int VIDEO_COMPACT = 0;
  private final static int VIDEO_NORMAL = 1;
  private final static int HIGHLIGHT_VIDEO = 2;

  public VideosAdapter(Context context, List<Video> videos, LoadMoreListener listener, AdapterView.OnItemClickListener clickListener) {
    super(context, listener);
    this.mContext = context;
    this.videos = videos;
    this.clickListener = clickListener;

    this.viewModeUtils = new ViewModeUtils(context, VideosFragment.class);
    this.picasso = PicassoVideoThumbnailHandler.picassoWithVideoSupport(mContext);
  }

  @Override
  public int getCount() {
    return videos.size();
  }

  @Override
  protected int getViewType(int position) {
    if (position == 0 || viewModeUtils.getViewMode() == ViewModeUtils.IMMERSIVE) {
      return HIGHLIGHT_VIDEO;
    } else if (viewModeUtils.getViewMode() == ViewModeUtils.COMPACT) {
      return VIDEO_COMPACT;
    } else {
      return VIDEO_NORMAL;
    }
  }

  @Override
  protected RecyclerView.ViewHolder getViewHolder(ViewGroup parent, int viewType) {
    if (viewType == VIDEO_COMPACT) {
      View itemView = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.fragment_video_row, parent, false);
      return new VideoCompactViewHolder(itemView);
    } else if (viewType == HIGHLIGHT_VIDEO) {
      View itemView = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.listview_highlight, parent, false);
      RecyclerView.ViewHolder holder = new HighlightViewHolder(itemView);
      requestFullSpan(holder);
      return holder;
    } else if (viewType == VIDEO_NORMAL) {
      View itemView = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.listview_row, parent, false);
      return new VideoNormalViewHolder(itemView);
    }
    return null;
  }

  @Override
  protected void doBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
    final Video video = videos.get(position);
    if (holder instanceof VideoViewHolder) {
      VideoViewHolder videoViewHolder = (VideoViewHolder) holder;

      videoViewHolder.thumb.setImageDrawable(null);
      String thumbnail = (holder instanceof VideoCompactViewHolder) ? video.getThumbUrl() : video.getImage();
      if (thumbnail != null)
        picasso.load(thumbnail).placeholder(R.color.gray).into(videoViewHolder.thumb);
      else
        picasso.load(video.getDirectVideoUrl()).placeholder(R.color.gray).into(videoViewHolder.thumb);

      videoViewHolder.title.setText(video.getTitle());

      String dateString = DateUtils.getRelativeDateTimeString(mContext, video.getUpdated().getTime(), DateUtils.SECOND_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL).toString();

      videoViewHolder.date.setText(dateString);
    }

    holder.itemView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        clickListener.onItemClick(null, holder.itemView, position, 0);
      }
    });
  }

  private abstract static class VideoViewHolder extends RecyclerView.ViewHolder {
    protected TextView title;
    protected TextView date;
    protected ImageView thumb;

    VideoViewHolder(View view) {
      super(view);

    }
  }

  class VideoCompactViewHolder extends VideoViewHolder {
    VideoCompactViewHolder(View view) {
      super(view);

      this.title = view.findViewById(R.id.userVideoTitleTextView);
      this.date = view.findViewById(R.id.userVideoDateTextView);
      this.thumb = view.findViewById(R.id.userVideoThumbImageView);
    }
  }

  private static class VideoNormalViewHolder extends VideoViewHolder {
    VideoNormalViewHolder(View view) {
      super(view);

      this.title = view.findViewById(R.id.title);
      this.date = view.findViewById(R.id.date);
      this.thumb = view.findViewById(R.id.thumbImage);
    }
  }

  private static class HighlightViewHolder extends VideoViewHolder {
    HighlightViewHolder(View view) {
      super(view);

      this.date = view.findViewById(R.id.textViewDate);
      this.title = view.findViewById(R.id.textViewHighlight);
      this.thumb = view.findViewById(R.id.imageViewHighlight);
    }
  }
}