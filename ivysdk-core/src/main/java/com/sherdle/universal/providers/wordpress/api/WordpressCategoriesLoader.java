package com.sherdle.universal.providers.wordpress.api;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.adsfall.R;
import com.sherdle.universal.Config;
import com.sherdle.universal.HolderActivity;
import com.sherdle.universal.providers.Provider;
import com.sherdle.universal.providers.wordpress.CategoryItem;
import com.sherdle.universal.providers.wordpress.ui.WordpressFragment;
import com.sherdle.universal.util.Helper;

import java.util.ArrayList;

/**
 * Simply loads data from an url (gotten from a provider) and loads it into a list.
 * Various attributes of this list and the way to load are defined in a WordpressGetTaskInfo.
 */
public class WordpressCategoriesLoader implements WordpressGetTaskInfo.ListListener {

  private WordpressGetTaskInfo mInfo;
  private ArrayList<CategoryItem> categoryItems;

  public WordpressCategoriesLoader(WordpressGetTaskInfo info) {
    this.mInfo = info;
  }

  public void load() {
    WordpressCategoriesTask categoriesTask = new WordpressCategoriesTask(mInfo, new WordpressCategoriesTask.WordpressCategoriesCallback() {
      @Override
      public void categoriesLoaded(ArrayList<CategoryItem> result) {
        categoryItems = result;
        if (mInfo.adapter != null && mInfo.adapter.getCount() > 0)
          createSlider(result);
        else
          mInfo.setListener(WordpressCategoriesLoader.this);
      }

      @Override
      public void categoriesFailed() {

      }
    });
    if (Config.WP_CHIPS)
      categoriesTask.execute();
  }

  public void createSlider(ArrayList<CategoryItem> categoryItems) {
    LayoutInflater layoutInflater = LayoutInflater.from(mInfo.context);
    final HorizontalScrollView sliderView = (HorizontalScrollView) layoutInflater.inflate(R.layout.listview_slider, null);
    for (final CategoryItem item : categoryItems) {
      FrameLayout categoryChip = (FrameLayout) layoutInflater.inflate(R.layout.listview_slider_chip, null);

      TextView categoryChipText = categoryChip.findViewById(R.id.category_chip);
      TextView categoryChipNumber = categoryChip.findViewById(R.id.category_chip_number);

      categoryChipText.setText(item.getName());
      categoryChipNumber.setText(Helper.formatValue(item.getPostCount()));
      categoryChipText.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          HolderActivity.startActivity(mInfo.context, WordpressFragment.class, Provider.WORDPRESS, new String[]{mInfo.baseurl, item.getId()});
        }
      });
      ((LinearLayout) sliderView.findViewById(R.id.slider_content)).addView(categoryChip);
    }

    mInfo.adapter.setSlider(sliderView);

    //Animate the appearance
    sliderView.setAlpha(0);
    sliderView.animate().alpha(1).setDuration(500).start();
  }

  @Override
  public void completedWithPosts() {
    createSlider(categoryItems);
  }
}
