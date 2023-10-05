package com.sherdle.universal.attachmentviewer;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.sherdle.universal.attachmentviewer.loader.MediaLoader;
import com.sherdle.universal.attachmentviewer.ui.AttachmentFragment;

import java.util.List;

/**
 * This file is part of the Universal  template
 * For license information, please check the LICENSE
 * file in the root of this project
 *
 * @author Sherdle
 * Copyright 2019
 */
public class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

  private List<MediaLoader> mListOfMedia;

  private boolean isZoomable = false;

  public ScreenSlidePagerAdapter(FragmentManager fm, List<MediaLoader> listOfMedia,
                                 boolean isZoomable) {
    super(fm);
    this.mListOfMedia = listOfMedia;
    this.isZoomable = isZoomable;
  }

  @Override
  public Fragment getItem(int position) {
    Fragment fragment = null;
    if (position < mListOfMedia.size()) {
      MediaLoader mediaLoader = mListOfMedia.get(position);
      fragment = loadImageFragment(mediaLoader);
    }
    return fragment;
  }

  private Fragment loadImageFragment(MediaLoader mediaLoader) {
    AttachmentFragment fragment = new AttachmentFragment();
    fragment.setMediaLoader(mediaLoader);

    Bundle bundle = new Bundle();
    bundle.putBoolean(AttachmentFragment.ZOOM, isZoomable);
    fragment.setArguments(bundle);

    return fragment;
  }

  @Override
  public int getCount() {
    return mListOfMedia.size();
  }
}
