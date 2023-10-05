package com.sherdle.universal.attachmentviewer.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.adsfall.R;
import com.sherdle.universal.attachmentviewer.loader.DefaultImageLoader;
import com.sherdle.universal.attachmentviewer.loader.MediaLoader;
import com.sherdle.universal.attachmentviewer.loader.PicassoImageLoader;
import com.sherdle.universal.attachmentviewer.model.MediaAttachment;
import com.sherdle.universal.attachmentviewer.widgets.HackyViewPager;
import com.sherdle.universal.attachmentviewer.widgets.ScrollGalleryView;
import com.sherdle.universal.util.Helper;
import com.sherdle.universal.util.ThemeUtils;

/**
 * This file is part of the Universal template
 * For license information, please check the LICENSE
 * file in the root of this project
 *
 * @author Sherdle
 * Copyright 2019
 */
public class AttachmentFragment extends Fragment {

  //Constants
  public static final String MEDIALOADER = "loader";
  public static final String ZOOM = "zoom";

  private boolean systemUIVisible = true;

  //Media to load
  private MediaLoader mMediaLoader;

  //Views
  private HackyViewPager viewPager;
  private ImageView backgroundImage;
  private TextView descriptionView;
  private ScrollGalleryView scrollGalleryView;
  private View rootView;

  private AttachmentActivity activity;

  public void setMediaLoader(MediaLoader mediaLoader) {
    mMediaLoader = mediaLoader;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);

    rootView = inflater.inflate(R.layout.fragment_attachment, container, false);
    backgroundImage = rootView.findViewById(R.id.backgroundImage);
    descriptionView = rootView.findViewById(R.id.description);
    viewPager = getActivity().findViewById(R.id.viewPager);
    scrollGalleryView = getActivity().findViewById(R.id.scroll_gallery_view);

    if (savedInstanceState != null) {
      mMediaLoader = (MediaLoader) savedInstanceState.getSerializable(MEDIALOADER);
    }

    loadMediaToView(rootView, savedInstanceState);

    String description = mMediaLoader.getAttachment().getDescription();
    if (description != null && !description.isEmpty()) {
      descriptionView.setText(Html.fromHtml(description));
      descriptionView.setVisibility(View.VISIBLE);
    }

    if (scrollGalleryView.thumbnailsHidden()) {
      rootView.findViewById(R.id.thumbnail_container_padding).setVisibility(View.GONE);
    }

    return rootView;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    activity = (AttachmentActivity) getActivity();
  }

  private void loadMediaToView(final View rootView, Bundle savedInstanceState) {
    assert mMediaLoader != null;

    mMediaLoader.loadMedia(this, backgroundImage, rootView, new MediaLoader.SuccessCallback() {
      @Override
      public void onSuccess() {
        if (mMediaLoader instanceof PicassoImageLoader || mMediaLoader instanceof DefaultImageLoader)
          createViewAttacher(getArguments());
        rootView.findViewById(R.id.attachmentProgress).setVisibility(View.GONE);
      }
    });
  }

  private void createViewAttacher(Bundle savedInstanceState) {

  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    outState.putSerializable(MEDIALOADER, mMediaLoader);
    super.onSaveInstanceState(outState);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Respond to the action bar's Up/Home button
    if (item.getItemId() == R.id.action_download) {
      Helper.download(getActivity(),
        ((MediaAttachment) mMediaLoader.getAttachment()).getUrl());
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    menu.clear();
    if (mMediaLoader.getAttachment() instanceof MediaAttachment) {
      inflater.inflate(R.menu.menu_download, menu);

      //If this media is an image, also inflate the wallpaper button.
      if (mMediaLoader.getAttachment() instanceof MediaAttachment &&
        ((MediaAttachment) mMediaLoader.getAttachment()).getMime().contains(MediaAttachment.MIME_PATTERN_IMAGE))
        inflater.inflate(R.menu.menu_image, menu);
    }

    if (ThemeUtils.lightToolbarThemeActive(getActivity()))
      ThemeUtils.setToolbarContentColor(getActivity().findViewById(R.id.toolbar), Color.WHITE);

    super.onCreateOptionsMenu(menu, inflater);
  }

  private void showSystemUI() {
    scrollGalleryView.hideThumbnails(false);
    if (!scrollGalleryView.thumbnailsHidden())
      rootView.findViewById(R.id.bottomHolder).setVisibility(View.VISIBLE);

    if (activity.getSupportActionBar() != null) {
      if (!(android.os.Build.VERSION.SDK_INT <= 19)) {
        activity.getSupportActionBar().show();
      }
    }

    if (android.os.Build.VERSION.SDK_INT >= 19)
      activity.getWindow().getDecorView().setSystemUiVisibility(
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

    systemUIVisible = true;
  }

  private void hideSystemUI() {
    scrollGalleryView.hideThumbnails(true);
    rootView.findViewById(R.id.bottomHolder).setVisibility(View.GONE);

    if (activity.getSupportActionBar() != null) {
      if (!(android.os.Build.VERSION.SDK_INT <= 19)) {
        activity.getSupportActionBar().hide();
      }
    }

    if (android.os.Build.VERSION.SDK_INT >= 19) {
      activity.getWindow().getDecorView().setSystemUiVisibility(
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
          View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
          View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
          View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
          View.SYSTEM_UI_FLAG_FULLSCREEN |
          View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    systemUIVisible = false;
  }

}
