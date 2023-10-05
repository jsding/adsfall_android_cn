package com.sherdle.universal.attachmentviewer.loader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.widget.ImageView;

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
public class DefaultImageLoader extends MediaLoader {
  private int mId;
  private Bitmap mBitmap;

  public DefaultImageLoader(MediaAttachment attachment, int id) {
    super(attachment);
    mId = id;
  }

  public DefaultImageLoader(MediaAttachment attachment, Bitmap bitmap) {
    super(attachment);
    mBitmap = bitmap;
  }

  @Override
  public boolean isImage() {
    return true;
  }

  @Override
  public void loadMedia(AttachmentFragment fragment, ImageView imageView, View rootView, SuccessCallback callback) {
    //we aren't loading bitmap, because full image loaded on thumbnail step
    imageView.setImageBitmap(mBitmap);
    if (callback != null) {
      callback.onSuccess();
    }
  }

  @Override
  public void loadThumbnail(Context context, ImageView thumbnailView, SuccessCallback callback) {
    loadBitmap(context);
    thumbnailView.setImageBitmap(mBitmap);
    if (callback != null) {
      callback.onSuccess();
    }
  }

  private void loadBitmap(Context context) {
    if (mBitmap == null) {
      mBitmap = ((BitmapDrawable) context.getResources().getDrawable(mId)).getBitmap();
    }
  }

}