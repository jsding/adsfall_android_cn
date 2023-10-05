package com.sherdle.universal.attachmentviewer.loader;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;

import com.sherdle.universal.attachmentviewer.model.Attachment;
import com.sherdle.universal.attachmentviewer.ui.AttachmentFragment;

import java.io.Serializable;

/**
 * This file is part of the Universal template
 * For license information, please check the LICENSE
 * file in the root of this project
 *
 * @author Sherdle
 * Copyright 2019
 */
public abstract class MediaLoader implements Serializable {

  private Attachment attachment;

  public MediaLoader(Attachment attachment) {
    this.attachment = attachment;
  }

  public Attachment getAttachment() {
    return attachment;
  }

  /**
   * @return true if implementation load's image, otherwise false
   */
  public abstract boolean isImage();

  public abstract void loadMedia(AttachmentFragment context, ImageView imageView, View rootView, SuccessCallback callback);

  public abstract void loadThumbnail(Context context, ImageView thumbnailView, SuccessCallback callback);

  /**
   * Callback to indicate that the image (or content, but only relevant for images) has been loaded
   */
  public interface SuccessCallback {
    void onSuccess();
  }

}