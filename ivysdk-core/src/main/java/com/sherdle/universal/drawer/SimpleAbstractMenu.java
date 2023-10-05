package com.sherdle.universal.drawer;

import android.view.Menu;
import android.view.MenuItem;

import com.adsfall.R;

import java.util.ArrayList;
import java.util.List;

/**
 * This file is part of the Universal template
 * For license information, please check the LICENSE
 * file in the root of this project
 *
 * @author Sherdle
 * Copyright 2019
 */
public abstract class SimpleAbstractMenu {
  //Top menu
  protected Menu menu;
  protected MenuItemCallback callback;

  //Keep track of everything in the menu and submenu's
  protected ArrayList<List<NavItem>> menuContent;
  protected ArrayList<MenuItem> menuItems;

  public SimpleAbstractMenu() {
    menuContent = new ArrayList<>();
    menuItems = new ArrayList<>();
  }

  protected MenuItem add(Menu menu, String title, int drawable, final List<NavItem> action) {
    return add(menu, title, drawable, action, false);
  }

  protected MenuItem add(Menu menu, String title, int drawable, final List<NavItem> action, final boolean requiresPurchase) {
    //Add the item to the menu
    MenuItem item = menu.add(R.id.main_group, menuItems.size(), Menu.NONE, title).setCheckable(true).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
      @Override
      public boolean onMenuItemClick(MenuItem menuItem) {
        callback.menuItemClicked(action, menuItem.getItemId(), requiresPurchase);
        return true;
      }
    });

    if (drawable != 0)
      item.setIcon(drawable);

    menuContent.add(action);
    menuItems.add(item);

    return item;
  }

  protected Menu getMenu() {
    return menu;
  }

  protected MenuItemCallback getMenuItemCallback() {
    return callback;
  }

  public List<NavItem> getFirstMenuItem() {
    if (menuContent.size() < 1) {
      return null;
    }

    return menuContent.get(0);
  }

  public List<MenuItem> getMenuItems() {
    return menuItems;
  }

}
