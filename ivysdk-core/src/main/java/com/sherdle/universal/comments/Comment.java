package com.sherdle.universal.comments;

/**
 * Very lazy comment object
 */
public class Comment {
  public String username;
  public String profileUrl;
  public String text;
  public long id;
  public int parentId;

  public int rating = -1;
  public int linesCount;
}
