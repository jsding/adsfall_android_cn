package com.sherdle.universal.drawer;

import android.content.Context;

import androidx.fragment.app.Fragment;

import java.io.Serializable;

public class NavItem implements Serializable {

  private String mText;
  private int mTextResource;
  private String[] mData;
  private String mProvider;
  private Class<? extends Fragment> mFragment;

  private String categoryImageUrl;
  private int tabIcon;

  //Create a new item with a resource string, resource drawable, type, fragment and data
  public NavItem(int text, Class<? extends Fragment> fragment, String provider, String[] data) {
    this(null, fragment, provider, data);
    mTextResource = text;
  }

  //Create a new item with a text string, resource drawable, type, fragment, data and purchase requirement
  public NavItem(String text, Class<? extends Fragment> fragment, String provider, String[] data) {
    mText = text;
    mFragment = fragment;
    mProvider = provider;
    mData = data;
  }

  public String getText(Context c) {
    if (mText != null) {
      return mText;
    } else {
      return c.getResources().getString(mTextResource);
    }
  }

  public Class<? extends Fragment> getFragment() {
    return mFragment;
  }

  public String[] getData() {
    return mData;
  }

  public void setCategoryImageUrl(String url) {
    this.categoryImageUrl = url;
  }

  public String getCategoryImageUrl() {
    return categoryImageUrl;
  }

  public void setTabIcon(int tabIcon) {
    this.tabIcon = tabIcon;
  }

  public int getTabIcon() {
    return tabIcon;
  }

  public String getProvider() {
    return mProvider;
  }
}
