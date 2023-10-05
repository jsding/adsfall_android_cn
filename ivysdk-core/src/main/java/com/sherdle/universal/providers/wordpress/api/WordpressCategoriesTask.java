package com.sherdle.universal.providers.wordpress.api;

import android.os.AsyncTask;

import com.sherdle.universal.providers.wordpress.CategoryItem;

import java.util.ArrayList;

/**
 * Simply loads data from an url (gotten from a provider) and loads it into a list.
 * Various attributes of this list and the way to load are defined in a WordpressGetTaskInfo.
 */
public class WordpressCategoriesTask extends AsyncTask<String, Integer, ArrayList<CategoryItem>> {

  private WordpressGetTaskInfo info;
  private WordpressCategoriesCallback callback;

  public static final int NUMBER_OF_CATEGORIES = 15;

  public WordpressCategoriesTask(WordpressGetTaskInfo info, WordpressCategoriesCallback callback) {
    this.info = info;
    this.callback = callback;
  }

  @Override
  protected void onPreExecute() {
  }

  @Override
  protected ArrayList<CategoryItem> doInBackground(String... params) {
    return info.provider.getCategories(info);
  }

  @Override
  protected void onPostExecute(ArrayList<CategoryItem> result) {
    if (result == null)
      callback.categoriesFailed();
    else
      callback.categoriesLoaded(result);
  }

  public interface WordpressCategoriesCallback {
    void categoriesLoaded(ArrayList<CategoryItem> result);

    void categoriesFailed();
  }

}
