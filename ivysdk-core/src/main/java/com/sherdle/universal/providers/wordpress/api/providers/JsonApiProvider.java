package com.sherdle.universal.providers.wordpress.api.providers;

import android.text.Html;

import com.sherdle.universal.Config;
import com.sherdle.universal.attachmentviewer.model.MediaAttachment;
import com.sherdle.universal.providers.wordpress.CategoryItem;
import com.sherdle.universal.providers.wordpress.PostItem;
import com.sherdle.universal.providers.wordpress.api.JsonApiPostLoader;
import com.sherdle.universal.providers.wordpress.api.WordpressCategoriesTask;
import com.sherdle.universal.providers.wordpress.api.WordpressGetTaskInfo;
import com.sherdle.universal.providers.wordpress.api.WordpressPostsTask;
import com.sherdle.universal.util.Helper;
import com.sherdle.universal.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

/**
 * This is a provider for the Wordpress Fragment over JSON API.
 */
public class JsonApiProvider implements WordpressProvider {

  //Whether or not to use perma friendly request urls or not
  public static final boolean USE_WP_FRIENDLY = true;

  //Time in seconds to add (i.e. if it should be 1 hour earlier, type -1 * 60 * 60)
  private static final int TIME_CORRECT = 0 * 60;

  //WP-REST
  private static final String API_LOC = "/?json=";
  private static final String API_LOC_FRIENDLY = "/api/";
  private static final String PARAMS = "date_format=U&exclude=comments,categories,custom_fields";

  @Override
  public String getRecentPosts(WordpressGetTaskInfo info) {
    StringBuilder builder = new StringBuilder();
    builder.append(info.baseurl);
    builder.append(getApiLoc());
    builder.append("get_recent_posts");
    builder.append(getParams(PARAMS));
    builder.append("&count=");
    if (info.simpleMode)
      builder.append(WordpressPostsTask.PER_PAGE_RELATED);
    else
      builder.append(WordpressPostsTask.PER_PAGE);
    builder.append("&page=");

    return builder.toString();
  }

  @Override
  public String getTagPosts(WordpressGetTaskInfo info, String tag) {
    StringBuilder builder = new StringBuilder();
    builder.append(info.baseurl);
    builder.append(getApiLoc());
    builder.append("get_tag_posts");
    builder.append(getParams(PARAMS));
    builder.append("&count=");
    if (info.simpleMode)
      builder.append(WordpressPostsTask.PER_PAGE_RELATED);
    else
      builder.append(WordpressPostsTask.PER_PAGE);
    builder.append("&tag_slug=");
    builder.append(tag);
    builder.append("&page=");

    return builder.toString();
  }

  @Override
  public String getCategoryPosts(WordpressGetTaskInfo info, String category) {
    StringBuilder builder = new StringBuilder();
    builder.append(info.baseurl);
    builder.append(getApiLoc());
    builder.append("get_category_posts");
    builder.append(getParams(PARAMS));
    builder.append("&count=");
    builder.append(WordpressPostsTask.PER_PAGE);
    builder.append("&category_slug=");
    builder.append(category);
    builder.append("&page=");

    return builder.toString();
  }

  @Override
  public String getPage(WordpressGetTaskInfo info, String pageId) {
    throw new RuntimeException("JSON API doesn't support Pages");
  }

  @Override
  public String getSearchPosts(WordpressGetTaskInfo info, String query) {
    StringBuilder builder = new StringBuilder();
    builder.append(info.baseurl);
    builder.append(getApiLoc());
    builder.append("get_search_results");
    builder.append(getParams(PARAMS));
    builder.append("&count=");
    builder.append(WordpressPostsTask.PER_PAGE);
    builder.append("&search=");
    builder.append(query);
    builder.append("&page=");

    return builder.toString();
  }

  @Override
  public ArrayList<CategoryItem> getCategories(WordpressGetTaskInfo info) {
    StringBuilder builder = new StringBuilder();
    builder.append(info.baseurl);
    builder.append(getApiLoc());
    builder.append("GET_CATEGORY_INDEX");

    JSONObject response = Helper.getJSONObjectFromUrl(builder.toString());
    if (response == null || !response.has("categories"))
      return null;

    ArrayList<CategoryItem> result = null;
    try {
      JSONArray categories = response.getJSONArray("categories");
      for (int i = 0; i < categories.length(); i++) {
        if (result == null) result = new ArrayList<>();

        JSONObject category = categories.getJSONObject(i);
        CategoryItem item = new CategoryItem(
          category.getString("slug"),
          Html.fromHtml(category.getString("title")).toString(),
          category.getInt("post_count"));
        result.add(item);
      }
    } catch (JSONException e) {
      Log.printStackTrace(e);
    }

    if (result == null) return null;

    Collections.sort(result, new Comparator<CategoryItem>() {

      public int compare(CategoryItem a, CategoryItem b) {
        Integer postsA = a.getPostCount();
        Integer postsB = b.getPostCount();

        return postsB.compareTo(postsA);
      }
    });

    //Return the only the top posts
    int maxIndexToReturn = Math.min(result.size(), WordpressCategoriesTask.NUMBER_OF_CATEGORIES);
    return new ArrayList<>(result.subList(0, maxIndexToReturn));
  }

  @Override
  public ArrayList<PostItem> parsePostsFromUrl(WordpressGetTaskInfo info, String url) {

    //Get JSON
    JSONObject json = Helper.getJSONObjectFromUrl(url);
    if (json == null) return null;

    ArrayList<PostItem> result = null;

    try {
      info.pages = json.getInt("pages");

      // parsing json object
      if (json.has("posts")) {
        JSONArray posts = json.getJSONArray("posts");

        result = new ArrayList<PostItem>();

        for (int i = 0; i < posts.length(); i++) {
          try {
            JSONObject post = posts.getJSONObject(i);
            PostItem item = itemFromJsonObject(post);

            //Complete the post in the background (if enabled)
            if (!Config.AVOID_SEPERATE_ATTACHMENT_REQUESTS)
              new JsonApiPostLoader(item, info.baseurl, null).start();

            if (!item.getId().equals(info.ignoreId)) {
              result.add(item);
            }
          } catch (Exception e) {
            Log.v("INFO", "Item " + i + " of " + posts.length()
              + " has been skipped due to exception!");
            Log.printStackTrace(e);
          }
        }
      }
    } catch (Exception e) {
      Log.printStackTrace(e);
    }

    return result;
  }


  public static PostItem itemFromJsonObject(JSONObject post) throws JSONException {
    PostItem item = new PostItem(PostItem.PostType.JSON);

    item.setTitle(Html.fromHtml(post.getString("title"))
      .toString());
    item.setDate(new Date((post.getLong("date") + TIME_CORRECT) * 1000));
    item.setId(post.getLong("id"));
    item.setUrl(post.getString("url"));
    item.setContent(post.getString("content"));
    if (post.has("author")) {
      Object author = post.get("author");
      if (author instanceof JSONArray
        && ((JSONArray) author).length() > 0) {
        author = ((JSONArray) author).getJSONObject(0);
      }

      if (author instanceof JSONObject
        && ((JSONObject) author).has("name")) {
        item.setAuthor(((JSONObject) author)
          .getString("name"));
      }
    }

    if (post.has("tags") && post.getJSONArray("tags").length() > 0) {
      item.setTag(((JSONObject) post.getJSONArray("tags").get(0)).getString("slug"));
    }

    try {
      if (post.has("thumbnail")) {
        String thumbnail = post.getString("thumbnail");
        if (!thumbnail.equals("")) {
          item.setThumbnailUrl(thumbnail);
        }
      }

      if (post.has("attachments")) {

        JSONArray attachments = post
          .getJSONArray("attachments");

        // checking how many attachments post has and
        // grabbing the first one
        for (int i = 0; i < attachments.length(); i++) {
          JSONObject attachment = attachments.getJSONObject(i);

          String attachmentThumbnail = null;
          if (attachment.has("images") && attachment.optJSONObject("images") != null) {
            JSONObject thumbnail;
            if (attachment.getJSONObject("images")
              .has("post-thumbnail")) {
              thumbnail = attachment
                .getJSONObject("images")
                .getJSONObject(
                  "post-thumbnail");

              attachmentThumbnail = thumbnail.getString("url");
            } else if (attachment.getJSONObject(
              "images").has("thumbnail")) {
              thumbnail = attachment
                .getJSONObject("images")
                .getJSONObject("thumbnail");

              attachmentThumbnail = thumbnail.getString("url");
            }
          }

          MediaAttachment att = new MediaAttachment(attachment.getString("url"), attachment.getString("mime_type"), attachmentThumbnail, attachment.getString("title"));
          item.addAttachment(att);
        }
      }

    } catch (JSONException e) {
      Log.printStackTrace(e);
    }

    return item;
  }

  public static String getPostUrl(long id, String baseurl) {
    StringBuilder builder = new StringBuilder();
    builder.append(baseurl);
    builder.append(getApiLoc());
    builder.append("get_post");
    builder.append(getParams("post_id="));
    builder.append(id);

    return builder.toString();
  }

  public static String getParams(String params) {
    String query = (USE_WP_FRIENDLY) ? "?" : "&";
    return query + params;
  }

  public static String getApiLoc() {
    return (USE_WP_FRIENDLY) ? API_LOC_FRIENDLY : API_LOC;
  }
}
