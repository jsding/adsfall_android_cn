package com.sherdle.universal.providers.wordpress.api;

import com.sherdle.universal.attachmentviewer.model.MediaAttachment;
import com.sherdle.universal.providers.wordpress.PostItem;
import com.sherdle.universal.providers.wordpress.api.providers.RestApiProvider;
import com.sherdle.universal.util.Helper;
import com.sherdle.universal.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

public final class RestApiPostLoader extends Thread {
  private PostItem item;
  private String apiBase;
  private JsonApiPostLoader.BackgroundPostCompleterListener listener;

  public RestApiPostLoader(PostItem item, String apiBase, JsonApiPostLoader.BackgroundPostCompleterListener listener) {
    this.item = item;
    this.apiBase = apiBase;
    this.listener = listener;
  }

  @Override
  public void run() {
    String url = RestApiProvider.getPostMediaUrl(apiBase, "" + item.getId());

    // getting JSON string from URL
    JSONArray json = Helper.getJSONArrayFromUrl(url);

    // parsing json data
    try {
      // parsing json object
      if (json != null) {

        for (int i = 0; i < json.length(); i++) {
          JSONObject attachment = json.getJSONObject(i);
          String mime = attachment.getString("mime_type");

          String source = null;
          String thumb = null;
          if (mime.startsWith(MediaAttachment.MIME_PATTERN_IMAGE)) {
            JSONObject sizes = attachment.getJSONObject("media_details").getJSONObject("sizes");
            if (sizes.has("large"))
              source = (sizes.getJSONObject("large").getString("source_url"));
            else
              source = attachment.getString("source_url");

            if (sizes.has("medium"))
              thumb = (sizes.getJSONObject("medium").getString("source_url"));
          } else {
            source = attachment.getString("source_url");
          }
          String title = attachment.getJSONObject("title").getString("rendered");
          MediaAttachment att = new MediaAttachment(source, attachment.getString("mime_type"), thumb, title);

          if (mime.startsWith(MediaAttachment.MIME_PATTERN_AUDIO)) {
            JSONObject details = attachment.getJSONObject("media_details");
            String artist = details.getString("artist");
            String album = details.getString("album");
            long length = details.getLong("length") * 1000;
            att.setAudioMeta(artist, album, length);
          }
          item.addAttachment(att);
        }

        item.setPostCompleted();

        //Make aware that we have completed the item
        if (listener != null) {
          listener.completed(item);
        }
      }
    } catch (Exception e) {
      Log.printStackTrace(e);

      //We weren't able to complete the item, so call the listener with null as an indication of fail
      if (listener != null) {
        listener.completed(null);
      }
    }
  }
}