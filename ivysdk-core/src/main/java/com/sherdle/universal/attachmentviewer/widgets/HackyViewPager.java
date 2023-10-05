package com.sherdle.universal.attachmentviewer.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.viewpager.widget.ViewPager;

/**
 * Found at http://stackoverflow.com/questions/7814017/is-it-possible-to-disable-scrolling-on-a-viewpager.
 * Convenient way to temporarily disable ViewPager navigation while interacting with ImageView.
 * <p>
 * Julia Zudikova
 * <p>
 * Hacky fix for http://code.google.com/p/android/issues/detail?id=18990
 * There's not much I can do in my code for now, but we can mask the result by
 * just catching the problem and ignoring it.
 *
 * @author Chris Banes
 */

public class HackyViewPager extends ViewPager {
  private boolean isLocked;

  public HackyViewPager(Context context) {
    super(context);
    isLocked = false;
  }

  public HackyViewPager(Context context, AttributeSet attrs) {
    super(context, attrs);
    isLocked = false;
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    try {
      return super.onInterceptTouchEvent(ev);
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    }
    return false;
  }

  @Override
  public boolean onTouchEvent(MotionEvent ev) {
    try {
      return super.onTouchEvent(ev);
    } catch (IllegalArgumentException ex) {
      ex.printStackTrace();
    }
    return false;
  }


  public void setLocked(boolean isLocked) {
    this.isLocked = isLocked;
  }

  public boolean isLocked() {
    return isLocked;
  }
}
