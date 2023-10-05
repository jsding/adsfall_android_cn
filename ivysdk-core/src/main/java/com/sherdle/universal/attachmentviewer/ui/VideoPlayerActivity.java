package com.sherdle.universal.attachmentviewer.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import com.adsfall.R;
import com.devbrackets.android.exomedia.listener.OnErrorListener;
import com.devbrackets.android.exomedia.listener.OnPreparedListener;
import com.devbrackets.android.exomedia.listener.VideoControlsVisibilityListener;
import com.devbrackets.android.exomedia.ui.widget.VideoControls;
import com.devbrackets.android.exomedia.ui.widget.VideoView;
import com.google.android.exoplayer2.source.UnrecognizedInputFormatException;
import com.sherdle.universal.HolderActivity;

/**
 * This file is part of the Universal template
 * For license information, please check the LICENSE
 * file in the root of this project
 *
 * @author Sherdle
 * Copyright 2019
 */
public class VideoPlayerActivity extends Activity implements OnPreparedListener {

  private static String URL = "url";
  private VideoView videoView;

  public static void startActivity(Context fromActivity, String url) {
    Intent intent = new Intent(fromActivity, VideoPlayerActivity.class);
    intent.putExtra(URL, url);
    fromActivity.startActivity(intent);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    String url = getIntent().getExtras().getString(URL);
    setContentView(R.layout.activity_attachment_video);

    videoView = findViewById(R.id.video_view);
    videoView.setOnPreparedListener(this);

    videoView.setVideoURI(Uri.parse(url));

    videoView.setOnErrorListener(new OnErrorListener() {
      @Override
      public boolean onError(Exception e) {
        if (e.getCause() instanceof UnrecognizedInputFormatException) {
          HolderActivity.startWebViewActivity(VideoPlayerActivity.this, url, true, false, null);
          finish();
          return true;
        }
        return false;
      }
    });
    ((VideoControls) videoView.getVideoControlsCore()).setVisibilityListener(new VideoControlsVisibilityListener() {
      @Override
      public void onControlsShown() {
        showSystemUI();
      }

      @Override
      public void onControlsHidden() {
        hideSystemUI();
      }
    });
  }

  private void showSystemUI() {

    if (android.os.Build.VERSION.SDK_INT >= 19)
      getWindow().getDecorView().setSystemUiVisibility(
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
  }

  private void hideSystemUI() {
    if (android.os.Build.VERSION.SDK_INT >= 19) {
      getWindow().getDecorView().setSystemUiVisibility(
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
          View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
          View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
          View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
          View.SYSTEM_UI_FLAG_FULLSCREEN |
          View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
  }

  @Override
  public void onPrepared() {
    videoView.start();
  }
}
