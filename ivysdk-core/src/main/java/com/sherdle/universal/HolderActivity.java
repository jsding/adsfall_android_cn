package com.sherdle.universal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebViewFragment;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.adsfall.R;
import com.sherdle.universal.inherit.BackPressFragment;
import com.sherdle.universal.inherit.PermissionsFragment;
import com.sherdle.universal.providers.CustomIntent;
import com.sherdle.universal.providers.Provider;
import com.sherdle.universal.providers.web.WebviewFragment;
import com.sherdle.universal.util.Helper;
import com.sherdle.universal.util.Log;
import com.sherdle.universal.util.ThemeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HolderActivity extends AppCompatActivity {

  private Toolbar mToolbar;
  private Class<? extends Fragment> queueItem;
  private String[] queueItemData;
  private String queueProvider;

  /**
   * Show the Holder Activity (for Built-in Proivder)
   *
   * @param mContext from which the new activity is stated.
   * @param fragment fragment to show
   * @param data     data/arguments/parameters to pass to the fragment
   * @param provider name of the provider
   */
  public static void startActivity(Context mContext, Class<? extends Fragment> fragment, String provider, String[] data) {
    Bundle bundle = new Bundle();
    bundle.putStringArray(MainActivity.FRAGMENT_DATA, data);
    bundle.putSerializable(MainActivity.FRAGMENT_CLASS, fragment);
    bundle.putString(MainActivity.FRAGMENT_PROVIDER, provider);

    Intent intent = new Intent(mContext, HolderActivity.class);
    intent.putExtras(bundle);
    mContext.startActivity(intent);
  }

  /**
   * Show the Holder Activity (for custom Fragments)
   *
   * @param mContext from which the new activity is stated.
   * @param fragment fragment to show
   */
  public static void startActivity(Context mContext, Class<? extends Fragment> fragment) {
    startActivity(mContext, fragment, null, null);
  }

  public static void startWebViewActivity(Context context, String url, boolean openExternal, boolean hideNavigation, String withData, int intentFlags) {
    if (openExternal && withData == null) {
      Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
      browserIntent.addFlags(intentFlags);
      if (browserIntent.resolveActivity(context.getPackageManager()) != null) {
        context.startActivity(browserIntent);
      } else {
        Log.v("INFO", "No activity to resolve url: " + url);
        Toast.makeText(context, R.string.no_app, Toast.LENGTH_SHORT).show();
      }
      return;
    }

    Bundle bundle = new Bundle();
    bundle.putStringArray(MainActivity.FRAGMENT_DATA, new String[]{url});
    bundle.putSerializable(MainActivity.FRAGMENT_CLASS, WebViewFragment.class);
    bundle.putString(MainActivity.FRAGMENT_PROVIDER, Provider.WEBVIEW);
    bundle.putBoolean(WebviewFragment.HIDE_NAVIGATION, hideNavigation);
    bundle.putString(WebviewFragment.LOAD_DATA, withData);

    Intent intent = new Intent(context, HolderActivity.class);
    intent.putExtras(bundle);
    intent.addFlags(intentFlags);
    context.startActivity(intent);
  }

  public static void startWebViewActivity(Context context, String url, boolean openExternal, boolean hideNavigation, String withData) {
    startWebViewActivity(context, url, openExternal, hideNavigation, withData, 0);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ThemeUtils.setTheme(this);
    setContentView(R.layout.activity_holder);

    mToolbar = findViewById(R.id.toolbar_actionbar);
    setSupportActionBar(mToolbar);
    getSupportActionBar().setDisplayShowHomeEnabled(true);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    Class<? extends Fragment> fragmentClass = (Class<? extends Fragment>) getIntent().getExtras().getSerializable(MainActivity.FRAGMENT_CLASS);
    String[] args = getIntent().getExtras().getStringArray(MainActivity.FRAGMENT_DATA);
    String provider = getIntent().getExtras().getString(MainActivity.FRAGMENT_PROVIDER, "");

    if (CustomIntent.class.isAssignableFrom(fragmentClass)) {
      CustomIntent.performIntent(HolderActivity.this, args);
      finish();
    } else if (getIntent().hasExtra(WebviewFragment.HIDE_NAVIGATION) || getIntent().hasExtra(WebviewFragment.LOAD_DATA)) {
      openWebFragment(args, getIntent().getExtras().getBoolean(WebviewFragment.HIDE_NAVIGATION), getIntent().getExtras().getString(WebviewFragment.LOAD_DATA));
    } else {
      openFragment(fragmentClass, provider, args);
    }
  }

  private void openFragment(Class<? extends Fragment> fragment, String provider, String[] data) {
    if (!checkPermissionsHandleIfNeeded(fragment, provider, data))
      return;

    try {
      Fragment frag = fragment.newInstance();

      // adding the data
      Bundle bundle = new Bundle();
      bundle.putStringArray(MainActivity.FRAGMENT_DATA, data);
      bundle.putString(MainActivity.FRAGMENT_PROVIDER, provider);
      frag.setArguments(bundle);

      //Changing the fragment
      FragmentManager fragmentManager = getSupportFragmentManager();
      fragmentManager.beginTransaction().replace(R.id.container, frag)
        .commit();

    } catch (InstantiationException | IllegalAccessException e) {
      Log.printStackTrace(e);
    }
  }

  private void openWebFragment(String[] params, boolean hideNavigation, String data) {
    Fragment fragment;
    fragment = new WebviewFragment();

    // adding the data
    Bundle bundle = new Bundle();
    bundle.putStringArray(MainActivity.FRAGMENT_DATA, params);
    bundle.putBoolean(WebviewFragment.HIDE_NAVIGATION, hideNavigation);
    if (data != null)
      bundle.putString(WebviewFragment.LOAD_DATA, data);
    fragment.setArguments(bundle);

    //Changing the fragment
    FragmentManager fragmentManager = getSupportFragmentManager();
    fragmentManager.beginTransaction().replace(R.id.container, fragment)
      .commit();

    //Setting the title
    if (data == null)
      setTitle(getResources().getString(R.string.webview_title));
    else
      setTitle("");
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle item selection
    switch (item.getItemId()) {
      case android.R.id.home:
        finish();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void onBackPressed() {
    Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.container);

    if (fragment instanceof BackPressFragment) {
      boolean handled = ((BackPressFragment) fragment).handleBackPress();
      if (!handled)
        super.onBackPressed();
    } else {
      super.onBackPressed();
    }
  }

  /**
   * Checks if the item can be opened because it has sufficient permissions.
   *
   * @param fragment The item to check
   * @return true if the item is safe to open
   */
  private boolean checkPermissionsHandleIfNeeded(Class<? extends Fragment> fragment, String provider, String[] data) {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) return true;

    List<String> allPermissions = new ArrayList<>();
    if (PermissionsFragment.class.isAssignableFrom(fragment)) {
      try {
        allPermissions.addAll(Arrays.asList(((PermissionsFragment) fragment.newInstance()).requiredPermissions()));
      } catch (Exception e) {
        //Don't really care
      }
    }

    if (allPermissions.size() > 0) {
      boolean allGranted = true;
      for (String permission : allPermissions) {
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED)
          allGranted = false;
      }

      if (!allGranted) {
        //TODO An explanation before asking
        requestPermissions(allPermissions.toArray(new String[0]), 1);
        queueItem = fragment;
        queueItemData = data;
        queueProvider = provider;
        return false;
      }

      return true;
    }

    return true;
  }

  @SuppressLint("NewApi")
  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    switch (requestCode) {
      case 1:
        boolean foundfalse = false;
        for (int i = 0; i < grantResults.length; i++) {
          if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
            foundfalse = true;
          }
        }
        if (!foundfalse) {
          //Retry to open the menu item
          //(we can assume the item is 'purchased' otherwise a permission check would have not occured)
          openFragment(queueItem, queueProvider, queueItemData);
        } else {
          // Permission Denied
          Toast.makeText(HolderActivity.this, getResources().getString(R.string.permissions_required), Toast.LENGTH_SHORT)
            .show();
        }
        break;
      default:
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        break;
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    if (getSupportActionBar() == null) return;
    getSupportActionBar().setDisplayShowHomeEnabled(true);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
  }


  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    List<Fragment> fragments = getSupportFragmentManager().getFragments();
    if (fragments != null)
      for (Fragment frag : fragments)
        if (frag != null)
          frag.onActivityResult(requestCode, resultCode, data);
  }

  /** Implement if methods depend on this (like iaps?) don't work
   @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
   super.onActivityResult(requestCode, resultCode, data);
   List<Fragment> fragments = getSupportFragmentManager().getFragments();
   if (fragments != null)
   for (Fragment frag : fragments)
   if (frag != null)
   frag.onActivityResult(requestCode, resultCode, data);
   }
   */
}