package com.sherdle.universal.providers.photos;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.adsfall.R;
import com.sherdle.universal.attachmentviewer.model.MediaAttachment;
import com.sherdle.universal.attachmentviewer.ui.AttachmentActivity;
import com.sherdle.universal.util.InfiniteRecyclerViewAdapter;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class PhotoAdapter extends InfiniteRecyclerViewAdapter {

  private ArrayList<PhotoItem> listData;
  private Context mContext;

  /*
   * Show images in a staggered grid (true) or with fixed height (false)
   */
  private static final boolean STAGGERED_HEIGHT = true;

  public PhotoAdapter(Context context, ArrayList<PhotoItem> listData, LoadMoreListener listener) {
    super(context, listener);
    this.listData = listData;
    mContext = context;
  }

  @Override
  protected int getViewType(int position) {
    return 0;
  }

  @Override
  protected RecyclerView.ViewHolder getViewHolder(ViewGroup parent, int viewType) {
    View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_image_row, parent, false);
    return new ItemViewHolder(itemView);
  }

  @Override
  protected void doBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
    if (holder instanceof ItemViewHolder) {
      if (STAGGERED_HEIGHT) {
        Picasso.get()
          .load(listData.get(position).getThumbUrl() != null ?
            listData.get(position).getThumbUrl() :
            listData.get(position).getUrl())
          .placeholder(R.drawable.placeholder)
          .resize(Math.round(mContext.getResources().getDimension(R.dimen.card_width_image)), 0).onlyScaleDown()
          .into(((ItemViewHolder) holder).imageView);
      } else {
        Picasso.get()
          .load(listData.get(position).getThumbUrl() != null ?
            listData.get(position).getThumbUrl() :
            listData.get(position).getUrl())
          .placeholder(R.drawable.placeholder)
          .fit()
          .into(((ItemViewHolder) holder).imageView);
      }

      holder.itemView.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          startImagePagerActivity(position);
        }
      });
    }
  }

  @Override
  public int getCount() {
    return listData.size();
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  private static class ItemViewHolder extends RecyclerView.ViewHolder {
    ImageView imageView;

    private ItemViewHolder(View view) {
      super(view);
      this.imageView = view.findViewById(R.id.image);
    }
  }

  private void startImagePagerActivity(int position) {
    ArrayList<MediaAttachment> list = new ArrayList<>();
    for (PhotoItem image : listData) {
      list.add(new MediaAttachment(image.getUrl(), MediaAttachment.MIME_PATTERN_IMAGE, image.getThumbUrl(), image.getCaption()));
    }
    AttachmentActivity.startActivity(mContext, list, position);
  }
}
