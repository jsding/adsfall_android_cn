package com.sherdle.universal.providers.videos.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.widget.Toast;

import com.adsfall.R;
import com.sherdle.universal.Config;
import com.sherdle.universal.HolderActivity;
import com.sherdle.universal.attachmentviewer.model.MediaAttachment;
import com.sherdle.universal.attachmentviewer.ui.VideoPlayerActivity;
import com.sherdle.universal.comments.CommentsActivity;
import com.sherdle.universal.providers.videos.api.object.Video;
import com.sherdle.universal.providers.wordpress.PostItem;
import com.sherdle.universal.providers.wordpress.api.JsonApiPostLoader;
import com.sherdle.universal.providers.wordpress.api.RestApiPostLoader;
import com.sherdle.universal.providers.wordpress.api.WordpressGetTaskInfo;
import com.sherdle.universal.providers.wordpress.api.providers.JetPackProvider;
import com.sherdle.universal.providers.wordpress.api.providers.RestApiProvider;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * This is class gets the videos from youtube and parses the result
 */
public class WordpressClient implements VideoProvider {

  private VideosCallback callback;
  private String[] params;
  private WeakReference<Activity> activityReference;

  private int totalPages;
  private int currentPage;

  public WordpressClient(String[] params, Activity activity, VideosCallback callback) {
    this.activityReference = new WeakReference<>(activity);
    this.params = params;
    this.callback = callback;
  }

  @Override
  public void requestVideos(String pageToken, String searchQuery) {

    if (pageToken == null) {
      currentPage = 1;
    } else {
      currentPage = Integer.parseInt(pageToken);
    }

    AsyncTask.execute(new Runnable() {
      @Override
      public void run() {

        final ArrayList<Video> videos = getVideos(searchQuery);

        if (activityReference.get() == null) return;
        activityReference.get().runOnUiThread(new Runnable() {
          public void run() {

            if (videos != null) {
              boolean canLoadMore = currentPage < totalPages;
              callback.completed(videos, canLoadMore, Integer.toString(currentPage + 1));
            } else {
              callback.failed();
            }
          }
        });

      }
    });

  }

  private ArrayList<Video> getVideos(String searchQuery) {
    String apiUrl = params[0];
    String category = params.length > 1 ? params[1] : "";
    WordpressGetTaskInfo info = new WordpressGetTaskInfo(null, null, apiUrl, false);

    ArrayList<PostItem> posts;
    if (searchQuery == null || "".equals(searchQuery)) {
      posts = info.provider.parsePostsFromUrl(info, (category == null || "".equals(category) ?
        info.provider.getRecentPosts(info) :
        info.provider.getCategoryPosts(info, category)) + currentPage);
    } else {
      posts = info.provider.parsePostsFromUrl(info,
        info.provider.getSearchPosts(info, searchQuery) + currentPage);
    }

    if (info.pages == null || posts == null) return null;
    totalPages = info.pages;

    final ArrayList<Video> results = new ArrayList<>();
    for (final PostItem post : posts) {

      String videoUrlInBody = null;
      if (Config.AVOID_SEPERATE_ATTACHMENT_REQUESTS) {
        videoUrlInBody = RestApiProvider.getUrlsWithExtensionFromHtml(post.getContent(),
          new String[]{".mp4", ".webm", ".avi"});
      }

      if (info.provider instanceof RestApiProvider && videoUrlInBody == null)
        new RestApiPostLoader(post, apiUrl, new JsonApiPostLoader.BackgroundPostCompleterListener() {
          @Override
          public void completed(PostItem item) {
            if (post.getAttachments().size() > 0) {
              MediaAttachment videoAtt = null;
              for (MediaAttachment attachment : post.getAttachments()) {
                if (attachment.getMime().contains(MediaAttachment.MIME_PATTERN_VID)) {
                  videoAtt = attachment;
                  break;
                }
              }
              if (videoAtt != null) {
                Video video = new Video(post.getTitle(),
                  post.getId().toString(),
                  post.getDate(),
                  post.getContent(),
                  post.getThumbnailUrl(),
                  post.getFeaturedImageUrl(),
                  post.getAuthor(),
                  post.getUrl());

                video.setWordpressPost(post);
                video.setDirectVideoUrl(videoAtt.getUrl());

                results.add(video);
              }
            }
          }
        }).run();
      else {
        String videoUrl = videoUrlInBody;
        for (MediaAttachment attachment : post.getAttachments()) {
          if (attachment.getMime().contains(MediaAttachment.MIME_PATTERN_VID)) {
            videoUrl = attachment.getUrl();
            break;
          }
        }
        if (videoUrl != null) {

          Video video = new Video(post.getTitle(),
            post.getId().toString(),
            post.getDate(),
            post.getContent(),
            post.getThumbnailUrl(),
            post.getFeaturedImageUrl(),
            post.getAuthor(),
            post.getUrl());

          video.setWordpressPost(post);
          video.setDirectVideoUrl(videoUrl);

          results.add(video);
        }
      }


    }
    return results;
  }

  @Override
  public void requestVideos(String pageToken) {
    requestVideos(pageToken, null);
  }

  @Override
  public boolean supportsSearch() {
    return true;
  }

  @Override
  public boolean isYoutubeLive() {
    return false;
  }

  public static void playVideo(Video video, Context context) {
    VideoPlayerActivity.startActivity(context, video.getDirectVideoUrl());
  }

  public static void openExternally(Video video, Context context) {
    HolderActivity.startWebViewActivity(context, video.getLink(), Config.OPEN_EXPLICIT_EXTERNAL, false, null);
  }

  public static void openComments(Video video, Context context, String[] params) {
    PostItem post = video.getWordpressPost();
    if ((post.getCommentCount() != null &&
      post.getCommentCount() != 0 &&
      post.getCommentsArray() != null) ||
      ((post.getPostType() == PostItem.PostType.JETPACK || post.getPostType() == PostItem.PostType.REST) &&
        post.getCommentCount() != 0)) {

      Intent commentIntent = new Intent(context, CommentsActivity.class);

      if (post.getPostType() == PostItem.PostType.JETPACK) {
        commentIntent.putExtra(CommentsActivity.DATA_PARSEABLE, JetPackProvider.getPostCommentsUrl(params[0], post.getId().toString()));
        commentIntent.putExtra(CommentsActivity.DATA_TYPE, CommentsActivity.WORDPRESS_JETPACK);
      } else if (post.getPostType() == PostItem.PostType.REST) {
        commentIntent.putExtra(CommentsActivity.DATA_PARSEABLE, RestApiProvider.getPostCommentsUrl(params[0], post.getId().toString()));
        commentIntent.putExtra(CommentsActivity.DATA_TYPE, CommentsActivity.WORDPRESS_REST);
      } else if (post.getPostType() == PostItem.PostType.JSON) {
        commentIntent.putExtra(CommentsActivity.DATA_PARSEABLE, post.getCommentsArray().toString());
        commentIntent.putExtra(CommentsActivity.DATA_TYPE, CommentsActivity.WORDPRESS_JSON);
      }

      context.startActivity(commentIntent);
    } else {
      Toast.makeText(context, R.string.no_comments, Toast.LENGTH_SHORT).show();
    }
  }
}