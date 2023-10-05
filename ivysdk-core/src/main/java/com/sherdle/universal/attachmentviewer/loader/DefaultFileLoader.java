package com.sherdle.universal.attachmentviewer.loader;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;

import com.adsfall.R;
import com.sherdle.universal.attachmentviewer.model.MediaAttachment;
import com.sherdle.universal.attachmentviewer.ui.AttachmentFragment;
import com.sherdle.universal.util.Helper;

/**
 * This file is part of the Universal template
 * For license information, please check the LICENSE
 * file in the root of this project
 *
 * @author Sherdle
 * Copyright 2019
 */
public class DefaultFileLoader extends MediaLoader {

  public DefaultFileLoader(MediaAttachment attachment) {
    super(attachment);
  }

  @Override
  public boolean isImage() {
    return false;
  }

  @Override
  public void loadMedia(final AttachmentFragment context, ImageView imageView, View rootView, SuccessCallback callback) {
    final String url = ((MediaAttachment) getAttachment()).getUrl();

    rootView.findViewById(R.id.playButton).setVisibility(View.VISIBLE);
    rootView.findViewById(R.id.playButton).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Helper.download(context.getActivity(), url);
      }
    });

    ((ImageView) rootView.findViewById(R.id.playButton)).setImageResource(R.drawable.ic_download);
    callback.onSuccess();
  }

  @Override
  public void loadThumbnail(Context context, ImageView thumbnailView, SuccessCallback callback) {
    thumbnailView.setImageResource(R.drawable.ic_download);
    callback.onSuccess();
  }


}
