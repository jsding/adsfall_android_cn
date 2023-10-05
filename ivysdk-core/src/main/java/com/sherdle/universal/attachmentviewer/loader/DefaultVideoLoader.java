package com.sherdle.universal.attachmentviewer.loader;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.os.Build;
import android.view.View;
import android.widget.ImageView;

import com.adsfall.R;
import com.sherdle.universal.attachmentviewer.model.MediaAttachment;
import com.sherdle.universal.attachmentviewer.ui.AttachmentFragment;
import com.sherdle.universal.attachmentviewer.ui.VideoPlayerActivity;

import java.util.HashMap;

/**
 * This file is part of the Universal template
 * For license information, please check the LICENSE
 * file in the root of this project
 *
 * @author Sherdle
 * Copyright 2019
 */
public class DefaultVideoLoader extends MediaLoader {

  public DefaultVideoLoader(MediaAttachment attachment) {
    super(attachment);
  }

  @Override
  public boolean isImage() {
    return false;
  }

  @Override
  public void loadMedia(final AttachmentFragment context, ImageView imageView, View rootView, SuccessCallback callback) {
    new BitmapOperation(imageView).execute(((MediaAttachment) getAttachment()).getUrl());

    View.OnClickListener playClickListener = new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        VideoPlayerActivity.startActivity(context.getContext(), ((MediaAttachment) getAttachment()).getUrl());
      }
    };

    imageView.setImageResource(R.drawable.placeholder_video);
    imageView.setOnClickListener(playClickListener);

    rootView.findViewById(R.id.playButton).setVisibility(View.VISIBLE);
    rootView.findViewById(R.id.playButton).setOnClickListener(playClickListener);
    callback.onSuccess();
  }

  @Override
  public void loadThumbnail(Context context, ImageView thumbnailView, SuccessCallback callback) {
    thumbnailView.setImageResource(R.drawable.ic_action_play);
    callback.onSuccess();
  }

  private class BitmapOperation extends AsyncTask<String, Void, Bitmap> {

    private ImageView imageView;

    public BitmapOperation(ImageView imageView) {
      this.imageView = imageView;
    }

    @Override
    protected Bitmap doInBackground(String... params) {
      Bitmap bitmap = null;
      try (MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever()) {
        if (Build.VERSION.SDK_INT >= 14)
          mediaMetadataRetriever.setDataSource(params[0], new HashMap<String, String>());
        else
          mediaMetadataRetriever.setDataSource(params[0]);
        //   mediaMetadataRetriever.setDataSource(videoPath);
        bitmap = mediaMetadataRetriever.getFrameAtTime();
      } catch (Exception e) {
        e.printStackTrace();

      }
      return bitmap;
    }

    @Override
    protected void onPostExecute(Bitmap result) {
      imageView.setImageBitmap(result);
    }

    @Override
    protected void onPreExecute() {
    }

    @Override
    protected void onProgressUpdate(Void... values) {
    }
  }

}
