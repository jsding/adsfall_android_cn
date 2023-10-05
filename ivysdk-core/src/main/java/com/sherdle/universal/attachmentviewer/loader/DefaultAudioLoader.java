package com.sherdle.universal.attachmentviewer.loader;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;

import com.adsfall.R;
import com.sherdle.universal.attachmentviewer.model.MediaAttachment;
import com.sherdle.universal.attachmentviewer.ui.AttachmentFragment;

/**
 * This file is part of the Universal template
 * For license information, please check the LICENSE
 * file in the root of this project
 *
 * @author Sherdle
 * Copyright 2019
 */
public class DefaultAudioLoader extends MediaLoader {

  public DefaultAudioLoader(MediaAttachment attachment) {
    super(attachment);
  }

  @Override
  public boolean isImage() {
    return false;
  }

  @Override
  public void loadMedia(final AttachmentFragment context, ImageView imageView, View rootView, SuccessCallback callback) {

    imageView.setImageResource(R.drawable.placeholder_coverart);

    View.OnClickListener playClickListener = new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        String title = getAttachment().getDescription() != null ? getAttachment().getDescription() : "";
        playAudio(context.getContext(), ((MediaAttachment) getAttachment()).getUrl(), title);
      }
    };

    imageView.setOnClickListener(playClickListener);

    rootView.findViewById(R.id.playButton).setVisibility(View.VISIBLE);
    rootView.findViewById(R.id.playButton).setOnClickListener(playClickListener);
    callback.onSuccess();
  }

  @Override
  public void loadThumbnail(Context context, ImageView thumbnailView, SuccessCallback callback) {
    thumbnailView.setImageResource(R.drawable.ic_album_white);
    callback.onSuccess();
  }

  private void playAudio(Context context, String url, String name) {
  }

}
