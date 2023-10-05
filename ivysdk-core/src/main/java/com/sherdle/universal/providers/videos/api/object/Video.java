package com.sherdle.universal.providers.videos.api.object;

import com.sherdle.universal.providers.wordpress.PostItem;

import java.io.Serializable;
import java.util.Date;

/**
 * Storing information about the video, and formatting the time
 */
@SuppressWarnings("serial")
public class Video implements Serializable {

  private String title;
  private String id;
  private Date updated;
  private String description;
  private String thumbUrl;
  private String image;
  private String channel;
  private String link;
  private String directVideoUrl;

  private PostItem wordpressPost;
  private String[] apiParams;

  public Video(String title, String id, Date updated, String description, String thumbUrl, String image, String channel, String link) {
    super();
    this.title = title;
    this.id = id;
    this.updated = updated;
    this.description = description;
    this.thumbUrl = thumbUrl;
    this.image = image;
    this.channel = channel;
    this.link = link;
  }

  public void setDirectVideoUrl(String url) {
    this.directVideoUrl = url;
  }

  public String getDirectVideoUrl() {
    return directVideoUrl;
  }

  public void setWordpressPost(PostItem post) {
    this.wordpressPost = post;
  }

  public PostItem getWordpressPost() {
    return wordpressPost;
  }

  public void setApiParams(String[] params) {
    this.apiParams = params;
  }

  public String[] getApiParams() {
    return this.apiParams;
  }

  public String getTitle() {
    return title;
  }

  public String getId() {
    return id;
  }

  public Date getUpdated() {
    return updated;
  }

  public String getDescription() {
    return description;
  }

  public String getThumbUrl() {
    return thumbUrl;
  }

  public String getImage() {
    return image;
  }

  public String getChannel() {
    return channel;
  }

  public String getLink() {
    return link;
  }

}