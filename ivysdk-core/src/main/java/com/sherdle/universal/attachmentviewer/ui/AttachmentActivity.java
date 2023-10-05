package com.sherdle.universal.attachmentviewer.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;

import com.adsfall.R;
import com.sherdle.universal.attachmentviewer.loader.DefaultAudioLoader;
import com.sherdle.universal.attachmentviewer.loader.DefaultFileLoader;
import com.sherdle.universal.attachmentviewer.loader.DefaultVideoLoader;
import com.sherdle.universal.attachmentviewer.loader.MediaLoader;
import com.sherdle.universal.attachmentviewer.loader.PicassoImageLoader;
import com.sherdle.universal.attachmentviewer.model.Attachment;
import com.sherdle.universal.attachmentviewer.model.MediaAttachment;
import com.sherdle.universal.attachmentviewer.widgets.ScrollGalleryView;
import com.sherdle.universal.util.Helper;
import com.sherdle.universal.util.ThemeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This file is part of the Universal template
 * For license information, please check the LICENSE
 * file in the root of this project
 *
 * @author Sherdle
 * Copyright 2019
 */

public class AttachmentActivity extends AppCompatActivity {

  private ScrollGalleryView scrollGalleryView;
  private List<MediaLoader> mediaList;

  public static String IMAGES = "images";
  public static String INDEX = "index";

  public static void startActivity(Context source, MediaAttachment image) {
    Intent intent = new Intent(source, AttachmentActivity.class);
    intent.putExtra(IMAGES, new ArrayList<>(Collections.singleton(image)));
    source.startActivity(intent);
  }

  public static void startActivity(Context source, ArrayList<MediaAttachment> images) {
    Intent intent = new Intent(source, AttachmentActivity.class);
    intent.putExtra(IMAGES, images);
    source.startActivity(intent);
  }

  public static void startActivity(Context source, ArrayList<MediaAttachment> images, int defaultPosition) {
    Intent intent = new Intent(source, AttachmentActivity.class);
    intent.putExtra(IMAGES, images);
    intent.putExtra(INDEX, defaultPosition);
    source.startActivity(intent);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ThemeUtils.setTheme(this);
    setContentView(R.layout.activity_attachment);

    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    Helper.setStatusBarColor(this, R.color.black);

    ArrayList<Attachment> images = (ArrayList<Attachment>) getIntent().getSerializableExtra(IMAGES);
    int defaultIndex = getIntent().getIntExtra(INDEX, 0);
    scrollGalleryView = findViewById(R.id.scroll_gallery_view);

    mediaList = easyInitView(scrollGalleryView, images, getSupportFragmentManager());
    scrollGalleryView.setCurrentItem(defaultIndex);
  }

  public static List<MediaLoader> easyInitView(ScrollGalleryView view, ArrayList<Attachment> images, FragmentManager fm) {

    List<MediaLoader> infos = new ArrayList<>(images.size());
    for (Attachment attachment : images) {
      if (attachment instanceof MediaAttachment) {
        MediaAttachment mediaAttachment = ((MediaAttachment) attachment);
        if (mediaAttachment.getMime().contains(MediaAttachment.MIME_PATTERN_IMAGE))
          infos.add(new PicassoImageLoader(mediaAttachment));
        else if (mediaAttachment.getMime().contains(MediaAttachment.MIME_PATTERN_VID))
          infos.add(new DefaultVideoLoader(mediaAttachment));
        else if (mediaAttachment.getMime().contains(MediaAttachment.MIME_PATTERN_AUDIO))
          infos.add(new DefaultAudioLoader(mediaAttachment));
        else
          infos.add(new DefaultFileLoader(mediaAttachment));
      }
    }

    view.setThumbnailSize((int) view.getContext().getResources().getDimension(R.dimen.thumbnail_height))
      .setZoom(true)
      .setFragmentManager(fm)
      .addMedia(infos);

    if (infos.size() == 1) {
      view.hideThumbnails(true);
    }

    return infos;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      // Respond to the action bar's Up/Home button
      case android.R.id.home:
        finish();
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

}
