package com.sherdle.universal.providers.web;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.FrameLayout;

import androidx.core.content.ContextCompat;

import com.sherdle.universal.util.ThemeUtils;

public class FullscreenableChromeClient extends WebChromeClient {
  private Activity mActivity = null;

  private View mCustomView;
  private WebChromeClient.CustomViewCallback mCustomViewCallback;
  private int mOriginalOrientation;

  private FrameLayout mFullscreenContainer;

  private static final FrameLayout.LayoutParams COVER_SCREEN_PARAMS = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

  public FullscreenableChromeClient(Activity activity) {
    this.mActivity = activity;
  }

  @Override
  public void onShowCustomView(View view, CustomViewCallback callback) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      if (mCustomView != null) {
        callback.onCustomViewHidden();
        return;
      }

      mOriginalOrientation = mActivity.getRequestedOrientation();
      FrameLayout decor = (FrameLayout) mActivity.getWindow().getDecorView();
      mFullscreenContainer = new FullscreenHolder(mActivity);
      mFullscreenContainer.addView(view, COVER_SCREEN_PARAMS);
      decor.addView(mFullscreenContainer, COVER_SCREEN_PARAMS);
      mCustomView = view;
      setFullscreen(true);
      mCustomViewCallback = callback;
//          mActivity.setRequestedOrientation(requestedOrientation);
    }

    super.onShowCustomView(view, callback);
  }

  @SuppressWarnings("deprecation")
  @Override
  public void onShowCustomView(View view, int requestedOrientation, WebChromeClient.CustomViewCallback callback) {
    this.onShowCustomView(view, callback);
  }

  @Override
  public void onHideCustomView() {
    if (mCustomView == null) {
      return;
    }

    setFullscreen(false);
    FrameLayout decor = (FrameLayout) mActivity.getWindow().getDecorView();
    decor.removeView(mFullscreenContainer);
    mFullscreenContainer = null;
    mCustomView = null;
    mCustomViewCallback.onCustomViewHidden();
    mActivity.setRequestedOrientation(mOriginalOrientation);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ThemeUtils.lightToolbarThemeActive(mActivity)) {
      decor.setSystemUiVisibility(decor.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }
  }

  @Override
  public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
    AdvancedWebView newWebView = new AdvancedWebView(mActivity);
    // myParentLayout.addView(newWebView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
    transport.setWebView(newWebView);
    resultMsg.sendToTarget();

    return true;
  }

  private void setFullscreen(boolean enabled) {
    Window win = mActivity.getWindow();
    WindowManager.LayoutParams winParams = win.getAttributes();
    final int bits = WindowManager.LayoutParams.FLAG_FULLSCREEN;
    if (enabled) {
      winParams.flags |= bits;

      mActivity.getWindow().getDecorView().setSystemUiVisibility(
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
          View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
          View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
          View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
          View.SYSTEM_UI_FLAG_FULLSCREEN |
          View.SYSTEM_UI_FLAG_IMMERSIVE);

    } else {
      winParams.flags &= ~bits;
      if (mCustomView != null) {
        mCustomView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
      }

      mActivity.getWindow().getDecorView().setSystemUiVisibility(
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }
    win.setAttributes(winParams);
  }

  private static class FullscreenHolder extends FrameLayout {
    public FullscreenHolder(Context ctx) {
      super(ctx);
      setBackgroundColor(ContextCompat.getColor(ctx, android.R.color.black));
    }

    @Override
    public boolean onTouchEvent(MotionEvent evt) {
      return true;
    }
  }

}