package com.sherdle.universal.providers.wordpress.api.providers;

import android.text.Html;

import com.sherdle.universal.attachmentviewer.model.MediaAttachment;
import com.sherdle.universal.providers.wordpress.CategoryItem;
import com.sherdle.universal.providers.wordpress.PostItem;
import com.sherdle.universal.providers.wordpress.api.WordpressCategoriesTask;
import com.sherdle.universal.providers.wordpress.api.WordpressGetTaskInfo;
import com.sherdle.universal.providers.wordpress.api.WordpressPostsTask;
import com.sherdle.universal.util.Helper;
import com.sherdle.universal.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

/**
 * This is a provider for the Wordpress Fragment over JetPack API.
 */
public class JetPackProvider implements WordpressProvider {

  //Jetpack
  private static final String JETPACK_BASE = "https://public-api.wordpress.com/rest/v1.1/sites/";
  private static final String JETPACK_FIELDS = "&fields=ID,author,title,URL,content,discussion,featured_image,post_thumbnail,tags,discussion,date,attachments";
  private static final SimpleDateFormat JETPACK_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());

  @Override
  public String getRecentPosts(WordpressGetTaskInfo info) {
    StringBuilder builder = new StringBuilder();
    builder.append(JETPACK_BASE);
    builder.append(info.baseurl);
    builder.append("/posts/?number=");
    if (info.simpleMode)
      builder.append(WordpressPostsTask.PER_PAGE_RELATED);
    else
      builder.append(WordpressPostsTask.PER_PAGE);
    builder.append(JETPACK_FIELDS);
    builder.append("&page=");

    return builder.toString();
  }

  @Override
  public String getTagPosts(WordpressGetTaskInfo info, String tag) {
    StringBuilder builder = new StringBuilder();
    builder.append(JETPACK_BASE);
    builder.append(info.baseurl);
    builder.append("/posts/?number=");
    if (info.simpleMode)
      builder.append(WordpressPostsTask.PER_PAGE_RELATED);
    else
      builder.append(WordpressPostsTask.PER_PAGE);
    builder.append("&tag=");
    builder.append(tag);
    builder.append("&page=");

    return builder.toString();
  }

  @Override
  public String getCategoryPosts(WordpressGetTaskInfo info, String category) {
    StringBuilder builder = new StringBuilder();
    builder.append(JETPACK_BASE);
    builder.append(info.baseurl);
    builder.append("/posts/?number=");
    builder.append(WordpressPostsTask.PER_PAGE);
    builder.append("&category=");
    builder.append(category);
    builder.append("&page=");

    return builder.toString();
  }

  @Override
  public String getSearchPosts(WordpressGetTaskInfo info, String query) {
    StringBuilder builder = new StringBuilder();
    builder.append(JETPACK_BASE);
    builder.append(info.baseurl);
    builder.append("/posts/?number=");
    builder.append(WordpressPostsTask.PER_PAGE);
    builder.append("&search=");
    builder.append(query);
    builder.append("&page=");

    return builder.toString();
  }

  @Override
  public String getPage(WordpressGetTaskInfo info, String pageId) {
    StringBuilder builder = new StringBuilder();
    builder.append(JETPACK_BASE);
    builder.append(info.baseurl);
    builder.append("/posts/");
    builder.append(pageId);

    return builder.toString();
  }

  @Override
  public ArrayList<CategoryItem> getCategories(WordpressGetTaskInfo info) {
    StringBuilder builder = new StringBuilder();
    builder.append(JETPACK_BASE);
    builder.append(info.baseurl);
    builder.append("/categories");
    builder.append("?order_by=count&order=DESC&fields=ID,slug,name,post_count&number=" + WordpressCategoriesTask.NUMBER_OF_CATEGORIES);

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
          Html.fromHtml(category.getString("name")).toString(),
          category.getInt("post_count"));
        result.add(item);
      }
    } catch (JSONException e) {
      Log.printStackTrace(e);
    }

    return result;
  }

  @Override
  public ArrayList<PostItem> parsePostsFromUrl(WordpressGetTaskInfo info, String url) {
    //Get JSON
    JSONObject json = Helper.getJSONObjectFromUrl(url);
    if (json == null) return null;

    ArrayList<PostItem> result = null;
    try {

      result = new ArrayList<PostItem>();
      JSONArray posts;

      //Parsing an array of posts, or a single post
      if (json.has("posts")) {
        info.pages = json.getInt("found") / WordpressPostsTask.PER_PAGE + (json.getInt("found") % WordpressPostsTask.PER_PAGE == 0 ? 0 : 1);
        posts = json.getJSONArray("posts");
      } else {
        info.pages = 0;
        posts = new JSONArray();
        posts.put(json);
      }

      for (int i = 0; i < posts.length(); i++) {
        try {
          JSONObject post = posts.getJSONObject(i);
          PostItem item = itemFromJsonObject(post);

          if (!item.getId().equals(info.ignoreId)) {
            result.add(item);
          }
        } catch (Exception e) {
          Log.v("INFO", "Item " + i + " of " + posts.length()
            + " has been skipped due to exception!");
          Log.printStackTrace(e);
        }
      }

    } catch (Exception e) {
      Log.printStackTrace(e);
    }

    return result;
  }

  public static String getPostCommentsUrl(String baseurl, String postId) {
    StringBuilder builder = new StringBuilder();
    builder.append(JETPACK_BASE);
    builder.append(baseurl);
    builder.append("/posts/");
    builder.append(postId);
    builder.append("/replies?order=ASC");

    return builder.toString();
  }

  public static PostItem itemFromJsonObject(JSONObject post) throws JSONException {
    PostItem item = new PostItem(PostItem.PostType.JETPACK);

    item.setId(post.getLong("ID"));
    item.setAuthor(post.getJSONObject("author").getString("name"));
    try {
      item.setDate(JETPACK_DATE_FORMAT.parse(post.getString("date")));
    } catch (ParseException e) {
      Log.printStackTrace(e);
    }
    item.setTitle(Html.fromHtml(post.getString("title"))
      .toString());
    item.setUrl(post.getString("URL"));
    item.setContent(post.getString("content"));
    item.setCommentCount(post.getJSONObject("discussion").getLong("comment_count"));
    item.setFeaturedImageUrl(post.getString("featured_image"));

    //Set the thumbnail and establish the ID of the post thumbnail
    long thumbId = -1;
    if (!post.isNull("post_thumbnail")) {
      thumbId = post.getJSONObject("post_thumbnail").getLong("ID");
      item.setThumbnailUrl(post.getJSONObject("post_thumbnail").getString("URL"));
    }

    if (post.has("attachments") && post.getJSONObject("attachments").names() != null) {
      JSONObject attachments = post.getJSONObject("attachments");
      for (int i = 0; i < attachments.names().length(); i++) {
        JSONObject attachment = attachments.getJSONObject(attachments.names().getString(i));
        String thumbnail = (attachment.has("thumbnails") &&
          attachment.getJSONObject("thumbnails").has("thumbnail")) ?
          attachment.getJSONObject("thumbnails").getString("thumbnail") : null;

        String title = attachment.has("title") ? attachment.getString("title") : null;
        MediaAttachment mediaAttachment = new MediaAttachment(attachment.getString("URL"), attachment.getString("mime_type"), thumbnail, title);
        item.addAttachment(mediaAttachment);

        //We obtained a thumbnail ID earlier. And set a thumbnail image earlier
        //If a smaller thumbnail is available (thumbnail of thumbnail) we'll use it.
        if (attachment.getLong("ID") == thumbId &&
          attachment.has("thumbnails") &&
          attachment.getJSONObject("thumbnails").has("medium")) {
          item.setThumbnailUrl(attachment.getJSONObject("thumbnails").getString("medium"));
        }
      }

    }

    //If there are tags, save the first one
    JSONObject tags = post.getJSONObject("tags");
    if (tags != null && tags.names() != null && tags.names().length() > 0)
      item.setTag(tags.getJSONObject(tags.names().getString(0)).getString("slug"));

    item.setPostCompleted();

    return item;
  }

}
