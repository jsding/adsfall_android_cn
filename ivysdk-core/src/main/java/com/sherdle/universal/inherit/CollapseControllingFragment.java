package com.sherdle.universal.inherit;

/**
 * This is an interface to implement a backpress in Fragments
 */
public interface CollapseControllingFragment {

  /**
   * @return if the fragment supports a collapsing toolbar
   */
  boolean supportsCollapse();

  /**
   * @return whether elevation of toolbar should be changed depending on scroll state
   */
  boolean dynamicToolbarElevation();
}
