package com.sherdle.universal.providers.photos;

import java.io.Serializable;

public class PhotoItem implements Serializable {
  private String thumbUrl;
  private String id;
  private String link;
  private String url;
  private String caption;

  public PhotoItem() {
    super();
  }

  public PhotoItem(String id, String link, String url, String caption, String thumbUrl) {
    super();
    this.id = id;
    this.link = link;
    this.url = url;
    this.caption = caption;
    this.thumbUrl = thumbUrl;
  }

  public PhotoItem(String id, String link, String url, String caption) {
    super();
    this.id = id;
    this.link = link;
    this.url = url;
    this.caption = caption;
  }

  public String getUrl() {
    return url;
  }

  public String getId() {
    return id;
  }

  public String getLink() {
    return link;
  }

  public String getThumbUrl() {
    return thumbUrl;
  }

  public String getCaption() {
    return caption;
  }
}