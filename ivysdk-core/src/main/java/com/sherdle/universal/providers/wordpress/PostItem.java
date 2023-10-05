package com.sherdle.universal.providers.wordpress;

import com.sherdle.universal.attachmentviewer.model.MediaAttachment;
import com.sherdle.universal.util.SerializableJSONArray;

import org.json.JSONArray;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

public class PostItem implements Serializable {

  private static final long serialVersionUID = 1L;

  public enum PostType {JETPACK, JSON, REST, SLIDER}

  private boolean isCompleted;

  private String title;
  private Date date;
  private ArrayList<MediaAttachment> attachments;
  private String featuredImageUrl;
  private String thumbnailUrl;
  private Long id;
  private String content;
  private String url;
  private String author;
  private String tag;
  private Long commentCount;
  private PostType type;
  private SerializableJSONArray commentsArray;

  /**
   * Constructor
   */

  public PostItem(PostType type) {
    this.isCompleted = false;
    this.type = type;

    this.attachments = new ArrayList<>();
  }

  /**
   * Methods
   */

  //Return a larger image to display for this post
  public String getImageCandidate() {
    //If a featured image has been set, return it
    if (getFeaturedImageUrl() != null && !getFeaturedImageUrl().equals(""))
      return getFeaturedImageUrl();

    //If there is an attachment that is an image, return it
    if (getAttachments().size() > 0 && getAttachments().get(0).getMime().startsWith(MediaAttachment.MIME_PATTERN_IMAGE))
      return getAttachments().get(0).getUrl();
      //If there is an attachment with a thumbnail, return it
    else if (getAttachments().size() > 0 && getAttachments().get(0).getThumbnailUrl() != null)
      return getAttachments().get(0).getThumbnailUrl();

    //If there is a thumbnail, return it
    if (getThumbnailUrl() != null && !getThumbnailUrl().equals("") && !getThumbnailUrl().equals("(null)"))
      return getThumbnailUrl();

    return null;
  }

  //Return a smaller image to display for this post
  public String getThumbnailCandidate() {
    //If there is a thumbnail, return it
    if (getThumbnailUrl() != null && !getThumbnailUrl().equals("") && !getThumbnailUrl().equals("(null)"))
      return getThumbnailUrl();

    //If a featured image has been set, return it
    if (getFeaturedImageUrl() != null && !getFeaturedImageUrl().equals(""))
      return getFeaturedImageUrl();

    //If there is an attachment with a thumbnail, return it
    if (getAttachments().size() > 0 && getAttachments().get(0).getThumbnailUrl() != null)
      return getAttachments().get(0).getThumbnailUrl();
      //If there is an attachment that is an image, return it
    else if (getAttachments().size() > 0 && getAttachments().get(0).getMime().startsWith(MediaAttachment.MIME_PATTERN_IMAGE))
      return getAttachments().get(0).getUrl();

    return null;
  }


  /**
   * Getters and setters
   */

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public String getTag() {
    return tag;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }

  public ArrayList<MediaAttachment> getAttachments() {
    return attachments;
  }

  public void addAttachment(MediaAttachment attachment) {
    this.attachments.add(attachment);
  }

  public String getThumbnailUrl() {
    return thumbnailUrl;
  }

  public void setThumbnailUrl(String thumbnailUrl) {
    this.thumbnailUrl = thumbnailUrl;
  }

  public String getFeaturedImageUrl() {
    return featuredImageUrl;
  }

  public void setFeaturedImageUrl(String featuredImageUrl) {
    this.featuredImageUrl = featuredImageUrl;
  }

  public JSONArray getCommentsArray() {
    if (commentsArray != null)
      return commentsArray.getJSONArray();
    else
      return null;
  }

  public void setCommentsArray(JSONArray commentsArray) {
    this.commentsArray = new SerializableJSONArray(commentsArray);
  }

  public Long getCommentCount() {
    if (commentCount == null) return 0L;
    return commentCount;
  }

  public void setCommentCount(Long commentCount) {
    this.commentCount = commentCount;
  }

  public void setPostCompleted() {
    isCompleted = true;
  }

  public boolean isCompleted() {
    return isCompleted;
  }

  public PostType getPostType() {
    return type;
  }
}

