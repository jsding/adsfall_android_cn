package com.sherdle.universal.providers.wordpress;

public class CategoryItem {

  private String id;
  private String name;
  private int postCount;

  public CategoryItem(String id, String name, int postCount) {
    this.id = id;
    this.name = name;
    this.postCount = postCount;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getPostCount() {
    return postCount;
  }

  public void setPostCount(int postCount) {
    this.postCount = postCount;
  }

  @Override
  public String toString() {
    return "Id: " + id + " Name: " + name + " Postcount: " + postCount;
  }

}
