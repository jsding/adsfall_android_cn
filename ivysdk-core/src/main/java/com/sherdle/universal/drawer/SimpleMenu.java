package com.sherdle.universal.drawer;

import android.view.Menu;
import android.view.MenuItem;

import java.util.List;

/**
 * This file is part of the Universal template
 * For license information, please check the LICENSE
 * file in the root of this project
 *
 * @author Sherdle
 * Copyright 2019
 */
public class SimpleMenu extends SimpleAbstractMenu {

  public SimpleMenu(Menu menu, MenuItemCallback callback) {
    super();
    this.menu = menu;
    this.callback = callback;
  }

  public MenuItem add(String title, int drawable, List<NavItem> action) {
    return add(menu, title, drawable, action);
  }

  public MenuItem add(String title, int drawable, List<NavItem> action, boolean requiresPurchase) {
    return add(menu, title, drawable, action, requiresPurchase);
  }

}
