package com.sherdle.universal.comments;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.adsfall.R;
import com.sherdle.universal.Config;
import com.sherdle.universal.HolderActivity;
import com.sherdle.universal.util.Helper;
import com.sherdle.universal.util.Log;
import com.sherdle.universal.util.ThemeUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;

public class CommentsActivity extends AppCompatActivity {

  ArrayList<Comment> comments;
  ArrayAdapter<Comment> commentsAdapter;
  private int type;
  private String id;
  private String key;

  private Toolbar mToolbar;

  public static String DATA_PARSEABLE = "parseable";
  public static String DATA_TYPE = "type";
  public static String DATA_ID = "id";
  public static String DATA_KEY = "key";
  public static int INSTAGRAM = 1;
  public static int FACEBOOK = 2;
  public static int YOUTUBE = 3;
  public static int WORDPRESS_JETPACK = 4;
  public static int WORDPRESS_JSON = 5;
  public static int WORDPRESS_REST = 6;
  public static int DISQUS = 7;
  public static int WOOCOMMERCE_REVIEWS = 8;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ThemeUtils.setTheme(this);
    setContentView(R.layout.activity_comments);

    mToolbar = findViewById(R.id.toolbar_actionbar);
    setSupportActionBar(mToolbar);
    getSupportActionBar().setDisplayShowHomeEnabled(true);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    setTitle(getResources().getString(R.string.comments));

    Bundle extras = getIntent().getExtras();
    String parseableString = extras.getString(DATA_PARSEABLE);
    type = extras.getInt(DATA_TYPE);
    id = extras.getString(DATA_ID);
    key = extras.getString(DATA_KEY);

    comments = new ArrayList<>();
    commentsAdapter = new CommentsAdapter(this, comments, type);

    ListView lvComments = findViewById(R.id.listView);
    lvComments.setAdapter(commentsAdapter);
    lvComments.setEmptyView(findViewById(R.id.empty));

    commentsAdapter.notifyDataSetChanged();

    // Fetch other comments
    fetchComments(parseableString);
  }

  private void fetchComments(final String parseableString) {
    if (type == INSTAGRAM) {

      ((TextView) findViewById(R.id.empty)).setText(getResources()
        .getString(R.string.loading));

      new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            comments.clear();

            JSONObject response = new JSONObject(parseableString);

            JSONArray dataJsonArray = response.getJSONArray("data");
            for (int i = 0; i < dataJsonArray.length(); i++) {
              JSONObject commentJson = dataJsonArray
                .getJSONObject(i);
              Comment comment = new Comment();
              comment.text = commentJson.getString("text");
              comment.username = commentJson
                .getString("username");
              comments.add(comment);

            }

          } catch (JSONException | NullPointerException e) {
            Log.printStackTrace(e);
          }

          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              commentsAdapter.notifyDataSetChanged();

              ((TextView) findViewById(R.id.empty))
                .setText(getResources().getString(
                  R.string.no_results));
            }
          });

        }
      }).start();

    } else if (type == FACEBOOK) {
      try {
        JSONObject response = new JSONObject(parseableString);

        JSONArray dataJsonArray = response.getJSONArray("data");
        for (int i = 0; i < dataJsonArray.length(); i++) {
          JSONObject commentJson = dataJsonArray.getJSONObject(i);
          Comment comment = new Comment();
          comment.text = commentJson.getString("message");
          if (commentJson.has("from")) {
            comment.username = commentJson.getJSONObject("from")
              .getString("name");

            comment.profileUrl = "https://graph.facebook.com/"
              + commentJson.getJSONObject("from").getString("id")
              + "/picture?type=large";
          }
          comments.add(comment);

        }
      } catch (JSONException e) {
        Log.printStackTrace(e);
      }
    } else if (type == YOUTUBE) {
      final String url = "https://www.googleapis.com/youtube/v3/commentThreads?part=snippet&maxResults=100&videoId="
        + id
        + "&key="
        + key;
      ((TextView) findViewById(R.id.empty)).setText(getResources()
        .getString(R.string.loading));

      new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            JSONObject response = Helper.getJSONObjectFromUrl(url);
            JSONArray dataJsonArray = response
              .getJSONArray("items");
            for (int i = 0; i < dataJsonArray.length(); i++) {
              JSONObject commentJson = dataJsonArray
                .getJSONObject(i);
              if (commentJson.getJSONObject("snippet").has(
                "topLevelComment")) {
                JSONObject innerSnippet = commentJson
                  .getJSONObject("snippet")
                  .getJSONObject("topLevelComment")
                  .getJSONObject("snippet");
                Comment comment = new Comment();
                comment.text = innerSnippet
                  .getString("textDisplay");
                comment.username = innerSnippet
                  .getString("authorDisplayName");
                comment.profileUrl = innerSnippet
                  .getString("authorProfileImageUrl");
                comments.add(comment);
              }

            }

          } catch (JSONException | NullPointerException e) {
            Log.printStackTrace(e);
          }

          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              commentsAdapter.notifyDataSetChanged();
              ((TextView) findViewById(R.id.empty))
                .setText(getResources().getString(
                  R.string.no_results));
            }
          });

        }
      }).start();

    } else if (type == WORDPRESS_JSON) {

      ((TextView) findViewById(R.id.empty)).setText(getResources()
        .getString(R.string.loading));
      new Thread(new Runnable() {
        @Override
        public void run() {

          try {

            JSONArray dataJsonArray = new JSONArray(parseableString);

            ArrayList<Comment> toBeAddedLater = new ArrayList<>();

            for (int i = 0; i < dataJsonArray.length(); i++) {
              JSONObject commentJson = dataJsonArray
                .getJSONObject(i);
              Comment comment = new Comment();
              comment.text = commentJson.getString("content")
                .trim().replace("<p>", "")
                .replace("</p>", "");
              comment.username = commentJson.getString("name");
              comment.id = commentJson.getInt("id");
              comment.parentId = commentJson.getInt("parent");
              comment.linesCount = 0;
              if (comment.parentId == 0) {
                comments.add(comment);
              } else {
                toBeAddedLater.add(comment);
              }
            }

            orderComments(toBeAddedLater);

          } catch (JSONException | NullPointerException e) {
            Log.printStackTrace(e);
          }

          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              commentsAdapter.notifyDataSetChanged();
              ((TextView) findViewById(R.id.empty))
                .setText(getResources().getString(
                  R.string.no_results));
            }
          });
        }
      }).start();
    } else if (type == WORDPRESS_REST) {
      ((TextView) findViewById(R.id.empty)).setText(getResources()
        .getString(R.string.loading));
      new Thread(new Runnable() {
        @Override
        public void run() {

          try {
            JSONArray dataJsonArray = Helper.getJSONArrayFromUrl(parseableString);

            ArrayList<Comment> toBeAddedLater = new ArrayList<>();

            for (int i = 0; i < dataJsonArray.length(); i++) {
              JSONObject commentJson = dataJsonArray
                .getJSONObject(i);
              Comment comment = new Comment();
              comment.text = commentJson.getJSONObject("content").getString("rendered")
                .trim().replace("<p>", "")
                .replace("</p>", "");
              comment.username = commentJson.getString("author_name");
              comment.id = commentJson.getInt("id");
              if (commentJson.has("author_avatar_urls") && commentJson.getJSONObject("author_avatar_urls").has("96"))
                comment.profileUrl = commentJson.getJSONObject("author_avatar_urls").getString("96");
              comment.parentId = commentJson.getInt("parent");
              comment.linesCount = 0;
              if (comment.parentId == 0) {
                comments.add(comment);
              } else {
                toBeAddedLater.add(comment);
              }
            }

            orderComments(toBeAddedLater);

          } catch (JSONException | NullPointerException e) {
            Log.printStackTrace(e);
          }

          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              commentsAdapter.notifyDataSetChanged();
              ((TextView) findViewById(R.id.empty))
                .setText(getResources().getString(
                  R.string.no_results));
            }
          });
        }
      }).start();
    } else if (type == WORDPRESS_JETPACK) {

      ((TextView) findViewById(R.id.empty)).setText(getResources()
        .getString(R.string.loading));

      new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            JSONObject response = Helper.getJSONObjectFromUrl(parseableString);
            JSONArray dataJsonArray = response
              .getJSONArray("comments");

            ArrayList<Comment> toBeAddedLater = new ArrayList<>();

            for (int i = 0; i < dataJsonArray.length(); i++) {
              JSONObject commentJson = dataJsonArray
                .getJSONObject(i);
              Comment comment = new Comment();
              comment.text = commentJson.getString("content")
                .trim().replace("<p>", "")
                .replace("</p>", "");
              comment.username = commentJson.getJSONObject("author").getString("login");
              comment.profileUrl = commentJson.getJSONObject("author").getString("avatar_URL");
              comment.id = commentJson.getInt("ID");

              JSONObject parentObj = commentJson.optJSONObject("parent");

              if (parentObj != null) {
                comment.parentId = parentObj.getInt("ID");
              } else {
                comment.parentId = 0;
              }

              comment.linesCount = 0;
              if (comment.parentId == 0) {
                comments.add(comment);
              } else {
                toBeAddedLater.add(comment);
              }
            }

            orderComments(toBeAddedLater);

          } catch (JSONException | NullPointerException e) {
            Log.printStackTrace(e);
          }

          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              commentsAdapter.notifyDataSetChanged();
              ((TextView) findViewById(R.id.empty))
                .setText(getResources().getString(
                  R.string.no_results));
            }
          });

        }
      }).start();

    } else if (type == DISQUS) {
      //Split the disqus parseable into: disqus url; identifier patter; shortname.
      String[] components = parseableString.split(";");

      //Insert the ID in the Disqus identifier pattern
      String disqusIdentifier = components[2].replace("%d", id);

      //Generate html to load in the webView based on the identifier and shortname
      String htmlComments = getHtmlComment(disqusIdentifier, components[1]);

      //Start a new WebView with the given data and  disqus (base) url
      HolderActivity.startWebViewActivity(this, components[0], Config.OPEN_INLINE_EXTERNAL, true, htmlComments);

      //We won't be proceeding in this activity
      finish();
    }
    commentsAdapter.notifyDataSetChanged();
  }

  private int checkIfContains(int parentId) {
    for (int a = 0; a < comments.size(); a++) {
      if (comments.get(a).id == parentId) {
        return a;
      }
    }
    return -1;
  }

  private void orderComments(ArrayList<Comment> toBeAddedLater) {
    Collections.reverse(toBeAddedLater);

    do {
      for (int i = 0; i < toBeAddedLater.size(); i++) {

        int index = checkIfContains(toBeAddedLater.get(i).parentId);
        if (index >= 0) {
          toBeAddedLater.get(i).linesCount = comments.get(index).linesCount + 1;
          comments.add(index + 1, toBeAddedLater.get(i));
          toBeAddedLater.remove(i);
        }
      }
    } while (toBeAddedLater.size() > 0);
  }

  //Get html to load in webview for id and shortname.
  //E.g. ("356008 https://www.androidpolice.com/?p=356008", "androidpolice");
  public String getHtmlComment(String idPost, String shortName) {

    return "<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"></head><body><div id='disqus_thread'></div></body>"
      + "<script type='text/javascript'>"
      + "var disqus_identifier = '"
      + idPost
      + "';"
      + "var disqus_shortname = '"
      + shortName
      + "';"
      + " (function() { var dsq = document.createElement('script'); dsq.type = 'text/javascript'; dsq.async = true;"
      + "dsq.src = '/embed.js';"
      + "(document.getElementsByTagName('head')[0] || document.getElementsByTagName('body')[0]).appendChild(dsq); })();"
      + "</script></html>";
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        finish();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

}
