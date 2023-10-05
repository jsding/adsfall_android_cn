package com.sherdle.universal.providers.web;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBar.LayoutParams;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.adsfall.R;
import com.ivy.util.Logger;
import com.sherdle.universal.Config;
import com.sherdle.universal.HolderActivity;
import com.sherdle.universal.MainActivity;
import com.sherdle.universal.inherit.BackPressFragment;
import com.sherdle.universal.inherit.CollapseControllingFragment;
import com.sherdle.universal.inherit.ConfigurationChangeFragment;
import com.sherdle.universal.inherit.PermissionsFragment;
import com.sherdle.universal.util.Helper;
import com.sherdle.universal.util.ThemeUtils;

/**
 * This activity is used to display webpages
 */

public class WebviewFragment extends Fragment implements BackPressFragment,
  CollapseControllingFragment, AdvancedWebView.Listener, ConfigurationChangeFragment, PermissionsFragment {

  //Static
  public static final String HIDE_NAVIGATION = "hide_navigation";
  public static final String LOAD_DATA = "loadwithdata";

  //References
  private Activity mAct;

  //Layout with interaction
  private AdvancedWebView browser;
  private SwipeRefreshLayout mSwipeRefreshLayout;

  //Layouts
  private ImageButton webBackButton;
  private ImageButton webForwButton;
  private FrameLayout ll;

  public WebviewFragment() {
  }

  @SuppressLint("InflateParams")
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);

    ll = (FrameLayout) inflater.inflate(R.layout.fragment_webview,
      container, false);

    setHasOptionsMenu(true);

    browser = ll.findViewById(R.id.webView);
    mSwipeRefreshLayout = ll.findViewById(R.id.refreshlayout);

    browser.setListener(getActivity(), this);
    browser.setGeolocationEnabled(Config.WEBVIEW_GEOLOCATION);
    browser.getSettings().setMediaPlaybackRequiresUserGesture(true);
    browser.setWebViewClient(new WebViewClient() {

      @SuppressWarnings("deprecation")
      @Override
      public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return handleUri(url);
      }

      @TargetApi(Build.VERSION_CODES.N)
      @Override
      public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        return handleUri(request.getUrl().toString());
      }

      @Override
      public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError er) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mAct);
        builder.setMessage(R.string.notification_error_ssl_cert_invalid);
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            handler.proceed();
          }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            handler.cancel();
          }
        });
        final AlertDialog dialog = builder.create();
        dialog.show();
      }

      // Make sure any url clicked is opened in webview
      boolean handleUri(String url) {
        if (urlShouldOpenExternally(url)) {
          try {
            startActivity(
              new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
          } catch (ActivityNotFoundException e) {
            if (url.startsWith("intent://")) {
              startActivity(
                new Intent(Intent.ACTION_VIEW, Uri.parse(url.replace("intent://", "http://"))));
            } else {
              Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.no_app), Toast.LENGTH_LONG).show();
            }
          }

          return true;
        }

        return false;
      }

    });

    mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
      @Override
      public void onRefresh() {
        browser.reload();
      }
    });

    return ll;
  }// of oncreateview

  private static final String HELP_DESK_URL = "https://widget.adsfall.com/assets/twp/index.html";

  private static final String HELP_DESK_URL_TEMPLATE = "https://widget.adsfall.com/assets/twp/index.html?tiledesk_projectid=%s&tiledesk_userFullname=%s&tiledesk_fullscreenMode=true&tiledesk_hideHeaderCloseButton=true&tiledesk_isOpen=true&extra=%s";

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    mAct = getActivity();

    setRetainInstance(true);

    //browser.getSettings().setSupportMultipleWindows(false);

    browser.setWebChromeClient(new FullscreenableChromeClient(mAct));

    String weburl = getArguments().getStringArray(MainActivity.FRAGMENT_DATA)[0];

    // replace the adsfall ulr
    if (weburl.startsWith(HELP_DESK_URL)) {
      Intent intent = mAct.getIntent();
      if (intent != null && intent.hasExtra("projectId")) {
        String project_id = intent.getStringExtra("projectId");
        String customerName = intent.getStringExtra("customerName");

        String customInfo = intent.getStringExtra("customInfo");

        weburl = String.format(HELP_DESK_URL_TEMPLATE, project_id, customerName, customInfo);
      }
    }

    String data = getArguments().containsKey(LOAD_DATA) ? getArguments().getString(LOAD_DATA) : null;
    //if (weburl.startsWith("file:///android_asset/") || hasConnectivity()) {
    //If this is the first time, load the initial url, otherwise restore the view if necessairy
    //If we have HTML data to load, do so, else load the url.
    if (data != null) {
      browser.loadDataWithBaseURL(weburl, data, "text/html", "UTF-8", "");
    } else {
      browser.loadUrl(weburl);
    }
    //}

  }

  @Override
  public void onPause() {
    super.onPause();

    if (browser != null)
      browser.onPause();

//    if (isMenuVisible() || getUserVisibleHint())
    setMenuVisibility(false);
  }

  @Override
  public void setMenuVisibility(final boolean visible) {
    super.setMenuVisibility(visible);
    if (mAct == null) return;

    if (visible) {
      if (navigationIsVisible()) {

        ActionBar actionBar = ((AppCompatActivity) mAct)
          .getSupportActionBar();

        if (actionBar == null) return;

        if (mAct instanceof HolderActivity) {
          actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP);
        } else {
          actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        }

        View view = mAct.getLayoutInflater().inflate(R.layout.fragment_webview_actionbar, null);
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.END | Gravity.CENTER_VERTICAL);
        actionBar.setCustomView(view, lp);

        webBackButton = mAct.findViewById(R.id.goBack);
        webForwButton = mAct.findViewById(R.id.goForward);

        webBackButton.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            if (browser.canGoBack())
              browser.goBack();
          }
        });
        webForwButton.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            if (browser.canGoForward())
              browser.goForward();
          }
        });
      }
    } else {
      if (navigationIsVisible()
        && getActivity() != null) {

        ActionBar actionBar = ((AppCompatActivity) getActivity())
          .getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
      }
    }
  }

  @SuppressLint("RestrictedApi")
  @Override
  public void onResume() {
    super.onResume();
    if (browser != null) {
      browser.onResume();
    } else {
      FragmentTransaction ft = getFragmentManager().beginTransaction();
      ft.detach(this).attach(this).commit();
    }

    if (this.getArguments().containsKey(HIDE_NAVIGATION) &&
      this.getArguments().getBoolean(HIDE_NAVIGATION)) {
      mSwipeRefreshLayout.setEnabled(false);
    }

    if (isMenuVisible() || getUserVisibleHint())
      setMenuVisibility(true);
    adjustControls();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    browser.onDestroy();
  }

  @Override
  public void onHiddenChanged(boolean hidden) {
    super.onHiddenChanged(hidden);
    if (!hidden) {
      setMenuVisibility(true);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.share) {
      shareURL();
      return true;
    } else if (item.getItemId() == R.id.exit_social) {
      this.getActivity().finish();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    // inflater.inflate(R.menu.webview_menu, menu);
    inflater.inflate(R.menu.exit_social_menu, menu);


    ThemeUtils.tintAllIcons(menu, mAct);
  }

  // Checking for an internet connection
  private boolean hasConnectivity() {
    boolean enabled = true;

    ConnectivityManager connectivityManager = (ConnectivityManager) mAct
      .getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo info = connectivityManager.getActiveNetworkInfo();

    if ((info == null || !info.isConnected() || !info.isAvailable())) {
      enabled = false;
      //Helper.noConnection(mAct);
    }

    return enabled;
  }

  public void adjustControls() {
    webBackButton = mAct.findViewById(R.id.goBack);
    webForwButton = mAct.findViewById(R.id.goForward);

    if (webBackButton == null || webForwButton == null || browser == null) return;

    if (ThemeUtils.lightToolbarThemeActive(mAct)) {
      webBackButton.setColorFilter(Color.BLACK);
      webForwButton.setColorFilter(Color.BLACK);
    }

    webBackButton.setAlpha(browser.canGoBack() ? 1.0f : 0.5f);
    webForwButton.setAlpha(browser.canGoForward() ? 1.0f : 0.5f);
  }

  // sharing
  private void shareURL() {
    Intent shareIntent = new Intent(Intent.ACTION_SEND);
    shareIntent.setType("text/plain");
    String appname = getString(R.string.app_name);
    shareIntent.putExtra(Intent.EXTRA_TEXT,
      (getResources().getString(R.string.web_share_begin)) + appname
        + getResources().getString(R.string.web_share_end)
        + browser.getUrl());
    startActivity(Intent.createChooser(shareIntent, getResources()
      .getString(R.string.share)));
  }

  /**
   * Check if an url should load externally and not in the WebView
   *
   * @param url The url that we would like to load
   * @return If it should be loaded inside or outside the WebView
   */
  public static boolean urlShouldOpenExternally(String url) {

    /*
     * If there is a set of urls defined that may only open inside the WebView and
     * the passed url does not match to one of these urls, it should be opened outside the WebView
     */
    if (Config.OPEN_ALL_OUTSIDE_EXCEPT.length > 0) {
      for (String pattern : Config.OPEN_ALL_OUTSIDE_EXCEPT) {
        if (!url.contains(pattern))
          return true;
      }
    }

    /*
     * If there is an url defined that should open outside the WebView, these urls will be loaded outside the webview
     */
    for (String pattern : Config.OPEN_OUTSIDE_WEBVIEW) {
      if (url.contains(pattern))
        return true;
    }

    return false;
  }

  @Override
  public boolean handleBackPress() {
    return !browser.onBackPressed();
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    browser.onActivityResult(requestCode, resultCode, intent);
  }

  @Override
  public boolean supportsCollapse() {
    return true;
  }

  @Override
  public boolean dynamicToolbarElevation() {
    return false;
  }

  /**
   * @return Whether navigation should be visible for this webview or not
   */
  public boolean navigationIsVisible() {
    //If override is on, always hide
    if (Config.FORCE_HIDE_NAVIGATION) return false;

    //Only hide navigation if key is provided and is true
    return (!this.getArguments().containsKey(HIDE_NAVIGATION) ||
      !this.getArguments().getBoolean(HIDE_NAVIGATION)
    );
  }

  @Override
  public void onPageStarted(String url, Bitmap favicon) {
    if (navigationIsVisible())
      mSwipeRefreshLayout.setRefreshing(true);
  }

  @Override
  public void onPageFinished(String url) {
    if (mSwipeRefreshLayout.isRefreshing())
      mSwipeRefreshLayout.setRefreshing(false);

    adjustControls();
    hideErrorScreen();
  }

  @Override
  public void onPageError(int errorCode, String description, String failingUrl) {
    if (mSwipeRefreshLayout.isRefreshing())
      mSwipeRefreshLayout.setRefreshing(false);

    if (failingUrl.startsWith("file:///android_asset/") || hasConnectivity()) {
      //It is a local error, or a we have connectivity
    } else {
      showErrorScreen();
    }
  }

  @Override
  public void onDownloadRequested(String url, String suggestedFilename, String mimeType, long contentLength, String contentDisposition, String userAgent) {
    if (!Helper.hasPermissionToDownload(getActivity()))
      return;

    // we only want to keep the default .bin filetype on the guess if the URL actually has that too
    if (suggestedFilename.endsWith(".bin") && !url.endsWith(".bin")) {
      suggestedFilename = suggestedFilename.replace(".bin", "");
    }

    if (AdvancedWebView.handleDownload(mAct, url, suggestedFilename)) {
      // download successfully handled
    } else {
      Toast.makeText(mAct, R.string.download_failed, Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  public void onExternalPageRequest(String url) {

  }

  @Override
  public String[] requiredPermissions() {
    if (Config.WEBVIEW_GEOLOCATION)
      return new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
    else
      return new String[0];
  }

  public void showErrorScreen() {
    final View stub = ll.findViewById(R.id.empty_view);
    stub.setVisibility(View.VISIBLE);

    stub.findViewById(R.id.retry_button).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        browser.loadUrl("javascript:document.open();document.close();");
        browser.reload();
      }
    });
  }

  public void hideErrorScreen() {
    final View stub = ll.findViewById(R.id.empty_view);
    if (stub.getVisibility() == View.VISIBLE)
      stub.setVisibility(View.GONE);
  }
}
