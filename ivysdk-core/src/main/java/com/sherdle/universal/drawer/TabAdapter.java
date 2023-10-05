package com.sherdle.universal.drawer;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.sherdle.universal.MainActivity;
import com.sherdle.universal.util.Log;

import java.util.List;

/**
 * This file is part of the Universal template
 * For license information, please check the LICENSE
 * file in the root of this project
 *
 * @author Sherdle
 * Copyright 2019
 */
public class TabAdapter extends FragmentStatePagerAdapter {

  private List<NavItem> actions;
  private Context context;
  private Fragment mCurrentFragment;
  private boolean isRtl;

  public TabAdapter(FragmentManager fm, List<NavItem> action, Context context) {
    super(fm);
    this.actions = action;
    this.context = context;

    isRtl = false;
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
      isRtl = context.getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
    }
  }

  /**
   * Return fragment with respect to Position .
   */
  @Override
  public Fragment getItem(int position) {
    return fragmentFromAction(actions.get(position));
  }

  @Override
  public void destroyItem(ViewGroup container, int position, Object object) {
    super.destroyItem(container, position, object);
  }

  @Override
  public int getCount() {
    return actions.size();
  }

  @Override
  public void setPrimaryItem(ViewGroup container, int position, Object object) {
    if (getCurrentFragment() != object) {
      mCurrentFragment = ((Fragment) object);
    }
    super.setPrimaryItem(container, position, object);
  }

  public Fragment getCurrentFragment() {
    return mCurrentFragment;
  }

  /**
   * This method returns the title of the tab according to the position.
   */
  @Override
  public CharSequence getPageTitle(int position) {
    return actions.get(isRtl ? ((actions.size() - 1) - position) : position).getText(context);
  }

  private static Fragment fragmentFromAction(NavItem action) {
    try {
      Fragment fragment = action.getFragment().newInstance();

      Bundle args = new Bundle();
      args.putStringArray(MainActivity.FRAGMENT_DATA, action.getData());
      args.putString(MainActivity.FRAGMENT_PROVIDER, action.getProvider());

      fragment.setArguments(args);

      return fragment;
    } catch (InstantiationException e) {
      Log.printStackTrace(e);
    } catch (IllegalAccessException e) {
      Log.printStackTrace(e);
    }

    return null;
  }

  public List<NavItem> getActions() {
    return actions;
  }
}
