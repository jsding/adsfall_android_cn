package com.sherdle.universal.drawer;

import java.util.List;

/**
 * This file is part of the Universal template
 * For license information, please check the LICENSE
 * file in the root of this project
 *
 * @author Sherdle
 * Copyright 2019
 */
public interface MenuItemCallback {

  void menuItemClicked(List<NavItem> action, int menuItemId, boolean requiresPurchase);
}
