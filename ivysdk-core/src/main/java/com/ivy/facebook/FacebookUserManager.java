package com.ivy.facebook;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.client.FriendFinderListener;
import com.android.client.ShareResultListener;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookRequestError;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.LoginStatusCallback;
import com.facebook.Profile;
import com.facebook.gamingservices.FriendFinderDialog;
import com.facebook.internal.Utility;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.share.Sharer;
import com.facebook.share.model.ShareHashtag;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.model.ShareVideo;
import com.facebook.share.widget.MessageDialog;
import com.facebook.share.widget.ShareDialog;
import com.ivy.IvySdk;
import com.ivy.ads.events.EventID;
import com.ivy.util.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FacebookUserManager {
  private static final String TAG = "Facebook";
  private FacebookLoginListener listener;
  private final boolean init = true;
  private final CallbackManager callbackManager;
  private String friends;
  private String me;

  private final boolean requireFriends;

  private ShareDialog shareDialog = null;


  public FacebookUserManager() {
    this.requireFriends = IvySdk.getGridConfigBoolean("requireFriends", false);
    callbackManager = CallbackManager.Factory.create();
  }

  public String getUserId() {
    Profile profile = Profile.getCurrentProfile();

    if (profile != null) {
      return profile.getId();
    }
    return "";
  }

  private void updateMe() {
    Profile profile = Profile.getCurrentProfile();

    if (profile != null) {
      String me = "{\"id\":\"%s\", \"name\":\"%s\", \"picture\":\"%s\"}";
      Uri profilePictureUri = profile.getProfilePictureUri(128, 128);
      this.me = String.format(me, profile.getId(), profile.getName(), profilePictureUri);
    } else {
      Logger.error(TAG, "Facebook profile is null");
      this.me = null;
    }

    Logger.debug(TAG, "Update facebook me to " + (this.me != null ? this.me : " null"));
  }

  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (callbackManager != null) {
      callbackManager.onActivityResult(requestCode, resultCode, data);
    }
  }

  public void logout(Activity activity) {
    LoginManager.getInstance().logOut();
  }

  private boolean checkAlreadyLogin() {
    AccessToken accessToken = AccessToken.getCurrentAccessToken();
    return accessToken != null && !accessToken.isExpired();
  }

  public void login(Activity context, @NonNull FacebookLoginListener loginListener) {
    this.listener = loginListener;

    if (checkAlreadyLogin()) {
      if (listener != null) {
        listener.onReceiveLoginResult(true);
      }
      return;
    }

    LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
      @Override
      public void onSuccess(LoginResult loginResult) {
        if (loginResult == null) {
          Log.e(TAG, "Facebook login success, but loginResult null");
          return;
        }
        Logger.debug(TAG, "Facebook login success" + loginResult);
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if (Profile.getCurrentProfile() == null) {
          Logger.debug(TAG, "Get facebook info");
          Utility.getGraphMeRequestWithCacheAsync(accessToken.getToken(),
            new Utility.GraphMeRequestWithCacheCallback() {
              @Override
              public void onSuccess(JSONObject userInfo) {
                Logger.debug(TAG, "Facebook userinfo: " + userInfo.toString());
                String id = userInfo.optString("id", "");
                if ("".equals(id)) {
                  Logger.error(TAG, "facebook id is null");
                  return;
                }
                String link = userInfo.optString("link");
                Profile profile = new Profile(
                  id,
                  userInfo.optString("first_name"),
                  userInfo.optString("middle_name"),
                  userInfo.optString("last_name"),
                  userInfo.optString("name"),
                  link != null ? Uri.parse(link) : null
                );
                Profile.setCurrentProfile(profile);
                friends = null;
                updateMe();
                if (listener != null) {
                  listener.onReceiveLoginResult(true);
                }
                if (!requestFriends()) {
                  if (listener != null) {
                    listener.onReceiveFriends("[]");
                  }
                }
              }

              @Override
              public void onFailure(FacebookException error) {
                friends = null;
                if (listener != null) {
                  listener.onReceiveLoginResult(false);
                }
              }
            });
        } else {
          Logger.debug(TAG, "Already signed in");
          updateMe();
          if (listener != null) {
            listener.onReceiveLoginResult(true);
          }
          if (!requestFriends()) {
            if (listener != null) {
              listener.onReceiveFriends("[]");
            }
          }
        }
      }

      @Override
      public void onCancel() {
        AccessToken.setCurrentAccessToken(null);
        listener.onReceiveLoginResult(false);
      }

      @Override
      public void onError(@NonNull FacebookException error) {
        IvySdk.showToast(error.getMessage());
        AccessToken.setCurrentAccessToken(null);
        listener.onReceiveLoginResult(false);
      }
    });

    List<String> permissions = getLoginPermissions(requireFriends);
    LoginManager.getInstance().logInWithReadPermissions(context, permissions);
  }

  public List<String> getLoginPermissions(boolean requireFriends) {
    boolean isGamingProfile = IvySdk.getGridConfigBoolean("useFacebookGamingProfile", false);
    if (isGamingProfile) {
      if (requireFriends) {
        return Arrays.asList("gaming_profile", "gaming_user_picture", "user_friends");
      }
      return Arrays.asList("gaming_profile", "gaming_user_picture");
    }

    if (requireFriends) {
      return Arrays.asList("public_profile", "user_friends");
    }
    return Collections.singletonList("public_profile");
  }

  public boolean isLogin() {
    try {
      boolean isInitialized = FacebookSdk.isInitialized();
      if (isInitialized) {
        AccessToken currentAccessToken = AccessToken.getCurrentAccessToken();
        return currentAccessToken != null && !currentAccessToken.isExpired();
      }
    } catch (Throwable t) {
      Logger.error(TAG, "isLogin exception", t);
    }
    return false;
  }

  public void logout() {
    LoginManager instance = LoginManager.getInstance();
    try {
      instance.logOut();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void shareImage(@NonNull Activity activity, @NonNull String title, @NonNull List<String> imageUrls, @NonNull ShareResultListener shareResultListener) {
    SharePhotoContent.Builder builder = new SharePhotoContent.Builder();

    for (String url : imageUrls) {
      SharePhoto photo = new SharePhoto.Builder()
        .setImageUrl(Uri.parse(url))
        .setCaption(title)
        .build();
      builder.addPhoto(photo);
    }

    SharePhotoContent sharePhotoContent = builder.build();
    if (shareDialog == null) {
      shareDialog = new ShareDialog(activity);
      // this part is optional
      shareDialog.registerCallback(callbackManager, new FacebookCallback<Sharer.Result>() {
        @Override
        public void onSuccess(Sharer.Result result) {
          Bundle bundle = new Bundle();

          String postId = result != null ? result.getPostId() : "";
          bundle.putString("label", postId);
          IvySdk.logEvent(EventID.FB_SHARE, bundle);
          shareResultListener.onSuccess(postId);
        }

        @Override
        public void onCancel() {
          shareResultListener.onCancel();
        }

        @Override
        public void onError(@NonNull FacebookException error) {
          shareResultListener.onError(error.getMessage());
        }
      });
    }
    if (ShareDialog.canShow(SharePhotoContent.class)) {
      shareDialog.show(sharePhotoContent);
    }
  }

  private void fallbackToSystemShare(Activity activity, String shareUrl) {
    try {
      Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
      shareIntent.setType("text/plain");
      shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareUrl);
      shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);

      shareIntent.setPackage(null);
      activity.startActivity(Intent.createChooser(shareIntent, "Play game with friends"));
    } catch(Throwable t) {
      Logger.error(TAG, "fallbackToSystemShare exception", t);
    }
  }

  public void loginAndShare(Activity activity, String shareUrl, String tag, String hashtag, ShareResultListener shareResultListener) {
    if (!isLogin()) {
      login(activity, new FacebookLoginListener() {
        @Override
        public void onReceiveLoginResult(boolean success) {
          if (success) {
            share(activity, shareUrl, tag, hashtag, shareResultListener);
          } else {
            fallbackToSystemShare(activity, shareUrl);
          }
        }

        @Override
        public void onReceiveFriends(String friends) {

        }
      });
      return;
    }
    share(activity, shareUrl, tag, hashtag, shareResultListener);
  }

  public void share(Activity activity, String shareUrl, String tag, String hashtag, ShareResultListener shareResultListener) {
    ShareLinkContent.Builder builder = new ShareLinkContent.Builder().setContentUrl(Uri.parse(shareUrl));
    if (hashtag != null && !"".equals(hashtag)) {
      builder.setShareHashtag(new ShareHashtag.Builder().setHashtag(hashtag).build());
    }

    ShareLinkContent content = builder.build();

    if (shareDialog == null) {
      shareDialog = new ShareDialog(activity);
      // this part is optional
      shareDialog.registerCallback(callbackManager, new FacebookCallback<Sharer.Result>() {
        @Override
        public void onSuccess(Sharer.Result result) {
          Bundle bundle = new Bundle();

          String postId = result != null ? result.getPostId() : "";
          if (tag != null) {
            bundle.putString("catalog", tag);
          }
          bundle.putString("label", postId);
          IvySdk.logEvent(EventID.FB_SHARE, bundle);
          if (shareResultListener != null) {
            shareResultListener.onSuccess(postId);
          }
        }

        @Override
        public void onCancel() {
          if (shareResultListener != null) {
            shareResultListener.onCancel();
          }
        }

        @Override
        public void onError(@NonNull FacebookException error) {
          if (shareResultListener != null) {
            shareResultListener.onError(error.getMessage());
          }
        }
      });
    }
    if (ShareDialog.canShow(ShareLinkContent.class)) {
      shareDialog.show(content);
    } else {
      fallbackToSystemShare(activity, shareUrl);
    }
  }

  public String me() {
    if (me == null) {
      Logger.debug(TAG, "Facebook me() is null, will update");
      updateMe();
    }
    return me == null ? "{}" : me;
  }

  private boolean requestFriends() {
    try {
      if (friends == null && requireFriends) {
        Logger.debug(TAG, "request Friends");
        String[] requiredFields = new String[]{
          "id",
          "name",
          "picture.height(128).width(128)"
        };
        try {
          GraphRequest request = new GraphRequest(AccessToken.getCurrentAccessToken(), "me/friends");

          Bundle parameters = request.getParameters();
          parameters.putString("fields", TextUtils.join(",", requiredFields));
          request.setParameters(parameters);
          request.setCallback(new GraphRequest.Callback() {
            @Override
            public void onCompleted(GraphResponse response) {
              Logger.debug(TAG, "Request Friends completed");
              onReceiveFriends(response);
            }
          });
          request.executeAsync();
          return true;
        } catch (Exception | Error e) {
          e.printStackTrace();
          if (listener != null) {
            listener.onReceiveFriends("[]");
          }
          return true;
        }
      } else {
        return false;
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return false;
  }

  private void onReceiveFriends(GraphResponse response) {
    try {
      FacebookRequestError requestError = response.getError();
      FacebookException exception = (requestError == null) ? null : requestError.getException();
      if (response.getJSONObject() == null && exception == null) {
        exception = new FacebookException("GraphObjectPagingLoader received neither a result nor an error.");
      }

      if (exception == null) {
        JSONArray data = response.getJSONObject().optJSONArray("data");
        boolean haveData = data.length() > 0;

        if (haveData) {
          for (int i = data.length() - 1; i >= 0; --i) {
            try {
              JSONObject friend = data.getJSONObject(i);
              JSONObject url = friend.getJSONObject("picture");
              String url_ = url.getJSONObject("data").getString("url");
//                        String cacheName = downloadProfilePicture(url);
              friend.put("picture", url_);
            } catch (JSONException e) {
              e.printStackTrace();
            }
          }
          friends = data.toString();
          if (listener != null) {
            listener.onReceiveFriends(friends);
          }
          Logger.debug(TAG, "ufb#friends " + friends);
        } else {
          Logger.debug(TAG, "ufb#friends 0");
          friends = "[]";
          if (listener != null) {
            listener.onReceiveFriends("[]");
          }
        }
      } else {
        exception.printStackTrace();
        if (listener != null) {
          listener.onReceiveFriends("[]");
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public String friends(FacebookLoginListener loginListener) {
    this.listener = loginListener;
    if (friends == null) {
      requestFriends();
      return "[]";
    } else {
      return friends;
    }
  }

  /**
   * @param activity
   */
  public void quickLogin(Activity activity) {
    LoginManager.getInstance().retrieveLoginStatus(activity, new LoginStatusCallback() {
      @Override
      public void onCompleted(@NonNull AccessToken accessToken) {
        // User was previously logged in, can log them in directly here.
        // If this callback is called, a popup notification appears that says
        // "Logged in as <User Name>"

      }

      @Override
      public void onFailure() {
        // No access token could be retrieved for the user
      }

      @Override
      public void onError(Exception exception) {
        // An error occurred
      }
    });
  }

  private void startFindFriendDialog(@NonNull Activity activity, @NonNull FriendFinderListener listener) {
    FriendFinderDialog dialog = new FriendFinderDialog(activity);
    // if we want to get notified when the dialog is closed
    // we can register a Callback
    dialog.registerCallback(
      this.callbackManager,
      new FacebookCallback<FriendFinderDialog.Result>() {
        @Override
        public void onSuccess(FriendFinderDialog.Result friendFinderResult) {
          Logger.debug(TAG, "Player Finder Dialog closed");
          listener.onSuccess();
        }

        @Override
        public void onCancel() {
          Logger.debug(TAG, "onCancel");
          listener.onCancel();
        }

        @Override
        public void onError(@NonNull FacebookException exception) {
          String error = exception.getMessage();
          if (error == null) {
            error = "invalid error code";
          }
          Logger.error(TAG, error);
          listener.onError(error);
        }
      });
    // open the dialog
    dialog.show();

  }

  public void findFriends(@NonNull Activity activity, @NonNull FriendFinderListener listener) {
    try {
      if (!isLogin()) {
        login(activity, new FacebookLoginListener() {
          @Override
          public void onReceiveLoginResult(boolean success) {
            if (success) {
              startFindFriendDialog(activity, listener);
            } else {
              listener.onError("not login");
            }
          }

          @Override
          public void onReceiveFriends(String friends) {
          }
        });
      } else {
        startFindFriendDialog(activity, listener);
      }
    } catch (Throwable t) {
      // crash protection
      Logger.error(TAG, "findFriends exception", t);
      listener.onError("exception");
    }
  }

}
