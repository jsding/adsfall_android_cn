package com.ivy.firestore;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.android.client.DatabaseChangedListener;
import com.android.client.DatabaseConnectListener;
import com.android.client.DatabaseListener;
import com.android.client.FirebaseAuthError;
import com.android.client.OnPasswordChangedListener;
import com.android.client.OnResultListener;
import com.facebook.AccessToken;
import com.google.android.gms.games.GamesSignInClient;
import com.google.android.gms.games.PlayGames;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PlayGamesAuthProvider;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.ivy.IvySdk;
import com.ivy.util.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreAdapter {
  private static final String TAG = "Firestore";
  private final FirebaseFirestore db;
  private final FirebaseAuth auth;

  public static FirestoreAdapter INSTANCE = null;

  public static FirestoreAdapter getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new FirestoreAdapter();
    }
    return INSTANCE;
  }

  private FirestoreAdapter() {
    JSON.DEFAULT_PARSER_FEATURE &= ~Feature.UseBigDecimal.mask;

    this.db = FirebaseFirestore.getInstance();
    this.auth = FirebaseAuth.getInstance();
  }

  @Nullable
  private String getFirebaseUserId() {
    FirebaseUser currentUser = auth.getCurrentUser();
    return currentUser != null ? currentUser.getUid() : null;
  }

  public void snapshot(String collection, String documentId, @NonNull final DatabaseChangedListener listener) {
    try {
      final DocumentReference docRef = db.collection(collection).document(documentId);
      docRef.addSnapshotListener((snapshot, e) -> {
        if (e != null) {
          Logger.warning(TAG, "Listen failed.", e);
          return;
        }

        if (snapshot != null && snapshot.exists()) {
          Logger.debug(TAG, "Current data: " + snapshot.getData());
          Map<String, Object> resultData = snapshot.getData();
          if (resultData != null && resultData.size() > 0) {
            listener.onData(collection, JSON.toJSONString(resultData));
          }
        }
      });
    } catch (Throwable t) {
      // ignore
      Logger.error(TAG, "snap exception", t);
    }
  }

  public void snapshot(String collection, @NonNull final DatabaseChangedListener listener) {
    String firebaseUserId = getFirebaseUserId();
    if (firebaseUserId == null || this.db == null) {
      return;
    }
    this.snapshot(collection, firebaseUserId, listener);
  }

  public void query(String collection, @NonNull DatabaseListener listener) {
    try {
      this.db.collection(collection).get().addOnSuccessListener(queryDocumentSnapshots -> {
        if (queryDocumentSnapshots == null) {
          listener.onData(collection, "{}");
          return;
        }

        List<DocumentSnapshot> documentSnapshotList = queryDocumentSnapshots.getDocuments();
        List<Map<String, Object>> datas = new ArrayList<>();
        for (DocumentSnapshot documentSnapshot : documentSnapshotList) {
          datas.add(documentSnapshot.getData());
        }
        String resultString = JSON.toJSONString(datas);
        listener.onData(collection, resultString);
      }).addOnFailureListener(e -> {
        listener.onFail(collection);
      });
    } catch (Throwable t) {
      // ignore
    }
  }

  public void read(String collection, @NonNull final DatabaseListener listener) {
    String firebaseUserId = getFirebaseUserId();
    if (firebaseUserId == null || this.db == null) {
      listener.onFail(collection);
      return;
    }
    this.read(collection, firebaseUserId, listener);
  }

  public void read(String collection, String documentId, @NonNull final DatabaseListener listener) {
    Logger.debug(TAG, "Firestore read " + collection + ", document: " + documentId);
    try {
      DocumentReference documentReference = this.db.collection(collection).document(documentId);
      documentReference.get().addOnSuccessListener(documentSnapshot -> {
        Logger.debug(TAG, "Firestore read success");
        if (documentSnapshot == null || !documentSnapshot.exists()) {
          listener.onData(collection, "{}");
          return;
        }
        Map<String, Object> result = documentSnapshot.getData();
        if (result != null) {
          try {
            String resultString = JSON.toJSONString(result);
            listener.onData(collection, resultString);
          } catch (Throwable t) {
            Logger.error(TAG, "convert to json exception", t);
            listener.onData(collection, "{}");
          }
        } else {
          listener.onData(collection, "{}");
        }
      }).addOnFailureListener(e -> {
        Logger.error(TAG, "Firestore read exception", e);
        listener.onFail(collection);
      });
    } catch (Throwable t) {
      Logger.error(TAG, "read exception", t);
      listener.onFail(collection);
    }
  }

  public void merge(String collection, String jsonString, @NonNull final DatabaseListener listener) {
    String firebaseUserId = getFirebaseUserId();

    if (firebaseUserId == null || this.db == null) {
      listener.onFail(collection);
      return;
    }

    try {
      Logger.debug(TAG, "Firestore merge " + collection + ", document: " + firebaseUserId);
      JSONObject jsonObject = JSONObject.parseObject(jsonString);
      if (jsonObject == null) {
        listener.onFail(collection);
        return;
      }
      this.db.collection(collection).document(firebaseUserId).set(jsonObject, SetOptions.merge()).addOnSuccessListener(aVoid -> {
        listener.onSuccess(collection);
      }).addOnFailureListener(e -> {
        listener.onFail(collection);
      });
    } catch (Throwable t) {
      Logger.error(TAG, "merge exception", t);
    }
  }

  public void set(String collection, String jsonString, @NonNull final DatabaseListener listener) {
    String firebaseUserId = getFirebaseUserId();

    if (firebaseUserId == null || this.db == null) {
      listener.onFail(collection);
      return;
    }

    try {
      Logger.debug(TAG, "Firestore set " + collection + ", document: " + firebaseUserId);
      JSONObject jsonObject = JSONObject.parseObject(jsonString);
      if (jsonObject == null) {
        listener.onFail(collection);
        return;
      }
      this.db.collection(collection).document(firebaseUserId).set(jsonObject).addOnSuccessListener(aVoid -> {
        Logger.debug(TAG, "Firestore set success: " + collection);
        listener.onSuccess(collection);
      }).addOnFailureListener(e -> {
        Logger.debug(TAG, "Firestore set exception: " + collection, e);
        listener.onFail(collection);
      });
    } catch (Throwable t) {
      Logger.error(TAG, "set exception", t);
      Logger.debug(TAG, jsonString);
    }
  }

  private static final String COLLECTION_USER_CONFIG = "app_userdata";

  public void updateUserConfig(String key, Object value) {
    try {
      String firebaseUserId = getFirebaseUserId();
      if (firebaseUserId == null || this.db == null) {
        Logger.error(TAG, "user not signed in, update is not possible");
        return;
      }

      this.db.collection(COLLECTION_USER_CONFIG).document(firebaseUserId).update(key, value).addOnSuccessListener(task -> {

      }).addOnFailureListener(e -> {

      });
    } catch(Throwable t) {
      Logger.error(TAG, "updateUserConfig exception", t);
    }
  }

  public void update(String collection, String jsonString, @NonNull final DatabaseListener listener) {
    String firebaseUserId = getFirebaseUserId();
    if (firebaseUserId == null || this.db == null) {
      listener.onFail(collection);
      return;
    }

    try {
      Logger.debug(TAG, "Firestore update " + collection + ", document: " + firebaseUserId);

      JSONObject jsonObject = JSONObject.parseObject(jsonString);
      if (jsonObject == null) {
        listener.onFail(collection);
        return;
      }
      this.db.collection(collection).document(firebaseUserId).update((Map<String, Object>) jsonObject).addOnSuccessListener(aVoid -> {
        Logger.debug(TAG, "Firestore update success: " + collection);
        listener.onSuccess(collection);
      }).addOnFailureListener(e -> {
        Logger.error(TAG, "Firestore update exception: " + collection, e);
        listener.onFail(collection);
      });
    } catch (Throwable t) {
      Logger.error(TAG, "update exception", t);
      Logger.debug(TAG, jsonString);
    }
  }

  public void delete(@NonNull String collection, @NonNull final DatabaseListener listener) {
    String firebaseUserId = getFirebaseUserId();
    if (firebaseUserId == null || this.db == null) {
      listener.onFail(collection);
      return;
    }

    try {
      Logger.debug(TAG, "Firestore delete " + collection + ", document: " + firebaseUserId);
      this.db.collection(collection).document(firebaseUserId).delete().addOnSuccessListener(aVoid -> {
        Logger.debug(TAG, "Firestore delete success");
        listener.onSuccess(collection);
      }).addOnFailureListener(e -> {
        Logger.error(TAG, "Firestore delete exception", e);
        listener.onFail(collection);
      });
    } catch (Throwable t) {
      Logger.error(TAG, "delete exception", t);
    }
  }

  public void initializeAfterSignInPlayGames(@NonNull DatabaseConnectListener databaseConnectListener) {
    this.initializeAfterSignInPlayGames(databaseConnectListener, true);
  }

  public void initializeAfterSignInFacebook(@NonNull DatabaseConnectListener databaseConnectListener) {
    this.initializeAfterSignInFacebook(databaseConnectListener, true);
  }

  public void signInWithEmailAndPassword(@NonNull String email, @NonNull String password, @NonNull DatabaseConnectListener databaseConnectListener) {
    this.signInWithEmailAndPassword(email, password, databaseConnectListener, true);
  }

  /**
   * PlayGames账号登录成功以后，执行登入Firebase的操作。
   *
   * @param databaseConnectListener 回调
   */
  private void initializeAfterSignInPlayGames(@NonNull DatabaseConnectListener databaseConnectListener, boolean reauthentication) {
    String webClientId = IvySdk.getGridConfigString("google_web_client_id", "");
    Activity activity = IvySdk.getActivity();
    if (activity == null) {
      databaseConnectListener.onFail(FirebaseAuthError.ERROR_INVALID_CREDENTIAL.name(), FirebaseAuthError.ERROR_INVALID_CREDENTIAL.getDescription());
      return;
    }

    GamesSignInClient gamesSignInClient = PlayGames.getGamesSignInClient(activity);
    gamesSignInClient.requestServerSideAccess(webClientId, false).addOnCompleteListener(task2 -> {
      if (task2.isSuccessful()) {
        String serverAuthCode = task2.getResult();
        AuthCredential credential = PlayGamesAuthProvider.getCredential(serverAuthCode);
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
          auth.signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
              if (task.isSuccessful()) {
                // Sign in success, update UI with the signed-in user's information
                Logger.debug(TAG, "signInWithCredential:success");
                FirebaseUser user = auth.getCurrentUser();
                if (user != null) {
                  databaseConnectListener.onSuccess();
                }
              } else {
                // If sign in fails, display a message to the user.
                Exception exception = task.getException();
                FirebaseAuthError authError = FirebaseAuthError.fromException(exception);
                if (reauthentication && FirebaseAuthError.ERROR_REQUIRES_RECENT_LOGIN.equals(authError)) {
                  Logger.debug(TAG, "require reauthentication again");
                  reauthentication(new OnResultListener() {
                    @Override
                    public void onSuccess() {
                      Logger.debug(TAG, "reauthentication success, now try to signin with email and password");
                      initializeAfterSignInPlayGames(databaseConnectListener, false);
                    }

                    @Override
                    public void onError() {
                      Logger.debug(TAG, "reauthentication error");
                      databaseConnectListener.onFail(FirebaseAuthError.ERROR_REQUIRES_RECENT_LOGIN.name(), FirebaseAuthError.ERROR_REQUIRES_RECENT_LOGIN.getDescription());
                    }
                  });
                } else {
                  Logger.debug(TAG, "signInWithCredential:failure", exception);
                  databaseConnectListener.onFail(authError.name(), authError.getDescription());
                }
              }
            }
          });
        } else {
          currentUser.linkWithCredential(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
              // Sign in success, update UI with the signed-in user's information
              Logger.debug(TAG, "linkWithCredential:success");
              FirebaseUser user = auth.getCurrentUser();
              if (user != null) {
                databaseConnectListener.onSuccess();
              }
            } else {
              Exception exception = task.getException();
              FirebaseAuthError authError = FirebaseAuthError.fromException(exception);
              if (reauthentication && FirebaseAuthError.ERROR_REQUIRES_RECENT_LOGIN.equals(authError)) {
                Logger.debug(TAG, "require reauthentication again");
                reauthentication(new OnResultListener() {
                  @Override
                  public void onSuccess() {
                    Logger.debug(TAG, "reauthentication success, now try to signin with email and password");
                    initializeAfterSignInPlayGames(databaseConnectListener, false);
                  }

                  @Override
                  public void onError() {
                    Logger.debug(TAG, "reauthentication error");
                    databaseConnectListener.onFail(FirebaseAuthError.ERROR_REQUIRES_RECENT_LOGIN.name(), FirebaseAuthError.ERROR_REQUIRES_RECENT_LOGIN.getDescription());
                  }
                });
              } else {
                Logger.debug(TAG, "signInWithCredential:failure", exception);
                databaseConnectListener.onFail(authError.name(), authError.getDescription());
              }
            }
          });
        }
      } else {
        Logger.debug(TAG, "not able to get server side code");
        Exception exception = task2.getException();
        String message = FirebaseAuthError.ERROR_INVALID_CREDENTIAL.getDescription();
        if (exception != null) {
          message = exception.getLocalizedMessage();
        }
        databaseConnectListener.onFail(FirebaseAuthError.ERROR_INVALID_CREDENTIAL.name(), message);
      }
    });
  }

  /**
   * 初始化并关联到facebook
   * 1. 如果当前有用户，将当前用户直接link到此facebook账号
   */
  private void initializeAfterSignInFacebook(@NonNull DatabaseConnectListener databaseConnectListener, boolean reauthentication) {
    AccessToken accessToken = AccessToken.getCurrentAccessToken();
    if (accessToken == null) {
      Logger.debug(TAG, "accessToken is empty");
      databaseConnectListener.onFail(FirebaseAuthError.ERROR_INVALID_CREDENTIAL.name(), FirebaseAuthError.ERROR_INVALID_CREDENTIAL.getDescription());
      return;
    }

    FirebaseUser currentUser = auth.getCurrentUser();

    AuthCredential credential = FacebookAuthProvider.getCredential(accessToken.getToken());

    if (currentUser == null) {
      auth.signInWithCredential(credential).addOnCompleteListener(task -> {
        if (task.isSuccessful()) {
          // Sign in success, update UI with the signed-in user's information
          Logger.debug(TAG, "signInWithCredential:success");
          FirebaseUser user = auth.getCurrentUser();
          if (user != null) {
            databaseConnectListener.onSuccess();
          }
        } else {
          Exception exception = task.getException();
          FirebaseAuthError authError = FirebaseAuthError.fromException(exception);
          if (reauthentication && FirebaseAuthError.ERROR_REQUIRES_RECENT_LOGIN.equals(authError)) {
            Logger.debug(TAG, "require reauthentication again");
            reauthentication(new OnResultListener() {
              @Override
              public void onSuccess() {
                Logger.debug(TAG, "reauthentication success, now try to signin with email and password");
                initializeAfterSignInFacebook(databaseConnectListener, false);
              }

              @Override
              public void onError() {
                Logger.debug(TAG, "reauthentication error");
                databaseConnectListener.onFail(FirebaseAuthError.ERROR_REQUIRES_RECENT_LOGIN.name(), FirebaseAuthError.ERROR_REQUIRES_RECENT_LOGIN.getDescription());
              }
            });
          } else {
            Logger.debug(TAG, "signInWithCredential:failure", exception);
            databaseConnectListener.onFail(authError.name(), authError.getDescription());
          }
        }
      });
    } else {
      currentUser.linkWithCredential(credential).addOnCompleteListener(task -> {
        if (task.isSuccessful()) {
          // Sign in success, update UI with the signed-in user's information
          Logger.debug(TAG, "linkWithCredential:success");
          FirebaseUser user = auth.getCurrentUser();
          if (user != null) {
            databaseConnectListener.onSuccess();
          }
        } else {
          Exception exception = task.getException();
          Logger.error(TAG, "linkWithCredential:failure", exception);
          FirebaseAuthError authError = FirebaseAuthError.fromException(task.getException());

          if (reauthentication && FirebaseAuthError.ERROR_REQUIRES_RECENT_LOGIN.equals(authError)) {
            Logger.debug(TAG, "require reauthentication again");
            reauthentication(new OnResultListener() {
              @Override
              public void onSuccess() {
                Logger.debug(TAG, "reauthentication success, now try to signin with email and password");
                initializeAfterSignInFacebook(databaseConnectListener);
              }

              @Override
              public void onError() {
                Logger.debug(TAG, "reauthentication error");
                databaseConnectListener.onFail(FirebaseAuthError.ERROR_REQUIRES_RECENT_LOGIN.name(), FirebaseAuthError.ERROR_REQUIRES_RECENT_LOGIN.getDescription());
              }
            });
          } else {
            databaseConnectListener.onFail(authError.name(), authError.getDescription());
          }
        }
      });
    }
  }

  private void reauthenticationEmailAndPassword(@NonNull FirebaseUser currentUser, @NonNull OnResultListener onResultListener) {
    String email = IvySdk.mmGetStringValue("__saved_email", "");
    String password = IvySdk.mmGetStringValue("__saved_password", "");
    if ("".equals(email) || "".equals(password)) {
      onResultListener.onError();
      return;
    }
    AuthCredential authCredential = EmailAuthProvider.getCredential(email, password);
    currentUser.reauthenticate(authCredential).addOnCompleteListener(new OnCompleteListener<Void>() {
      @Override
      public void onComplete(@NonNull Task<Void> task) {
        if (task.isSuccessful()) {
          onResultListener.onSuccess();
        } else {
          onResultListener.onError();
        }
      }
    });
  }

  private void reauthenticationByFacebook(@NonNull FirebaseUser currentUser, @NonNull OnResultListener onResultListener) {
    AccessToken accessToken = AccessToken.getCurrentAccessToken();
    if (accessToken == null) {
      Logger.debug(TAG, "accessToken is empty");
      onResultListener.onError();
      return;
    }

    AuthCredential credential = FacebookAuthProvider.getCredential(accessToken.getToken());
    currentUser.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
      @Override
      public void onComplete(@NonNull Task<Void> task) {
        if (task.isSuccessful()) {
          onResultListener.onSuccess();
        } else {
          onResultListener.onError();
        }
      }
    });
  }

  private void reauthenticationByPlayGames(@NonNull FirebaseUser currentUser, @NonNull OnResultListener onResultListener) {
    String webClientId = IvySdk.getGridConfigString("google_web_client_id", "");
    Activity activity = IvySdk.getActivity();
    if (activity == null) {
      onResultListener.onError();
      return;
    }

    GamesSignInClient gamesSignInClient = PlayGames.getGamesSignInClient(activity);
    gamesSignInClient.requestServerSideAccess(webClientId, false).addOnCompleteListener(task -> {
      if (task.isSuccessful()) {
        String serverCode = task.getResult();
        AuthCredential credential = PlayGamesAuthProvider.getCredential(serverCode);
        currentUser.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
          @Override
          public void onComplete(@NonNull Task<Void> task) {
            if (task.isSuccessful()) {
              onResultListener.onSuccess();
            } else {
              onResultListener.onError();
            }
          }
        });
      } else {
        onResultListener.onError();
      }
    });
  }

  private void reauthentication(@NonNull OnResultListener onResultListener) {
    FirebaseUser user = auth.getCurrentUser();
    if (user == null) {
      onResultListener.onError();
      return;
    }

    List<String> availableProviders = new ArrayList<>();
    List<? extends UserInfo> userInfoList = user.getProviderData();
    for (UserInfo info : userInfoList) {
      if (info != null) {
        availableProviders.add(info.getProviderId());
      }
    }

    if (availableProviders.contains(PlayGamesAuthProvider.PROVIDER_ID)) {
      Logger.debug(TAG, "reauthentication use playgames");
      reauthenticationByPlayGames(user, onResultListener);
    } else if (availableProviders.contains(FacebookAuthProvider.PROVIDER_ID)) {
      Logger.debug(TAG, "reauthentication use facebook");
      reauthenticationByFacebook(user, onResultListener);
    } else if (availableProviders.contains(EmailAuthProvider.PROVIDER_ID)) {
      Logger.debug(TAG, "reauthentication use email");
      reauthenticationEmailAndPassword(user, onResultListener);
    }
  }


  /**
   * 以Email/Password登入系统。
   * 如果当前用户已经有用户，则将当前的账号密码绑定到当前账号。
   * 如果没有登入账号，则直接登入系统
   *
   * @param email                   email账号
   * @param password                email密码
   * @param databaseConnectListener 回调
   */
  private void signInWithEmailAndPassword(@NonNull String email, @NonNull String password, @NonNull DatabaseConnectListener databaseConnectListener, boolean reauthentication) {
    FirebaseUser currentUser = auth.getCurrentUser();
    if (currentUser == null) {
      Logger.debug(TAG, "signInWithEmail and password: " + email + ", password");
      auth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
          @Override
          public void onComplete(@NonNull Task<AuthResult> task) {
            if (task.isSuccessful()) {
              Logger.debug(TAG, "signInWithEmailAndPassword success");
              databaseConnectListener.onSuccess();
            } else {
              Exception e = task.getException();
              FirebaseAuthError authError = FirebaseAuthError.fromException(e);
              if (reauthentication && FirebaseAuthError.ERROR_REQUIRES_RECENT_LOGIN.equals(authError)) {
                Logger.debug(TAG, "require reauthentication again");
                reauthentication(new OnResultListener() {
                  @Override
                  public void onSuccess() {
                    Logger.debug(TAG, "reauthentication success, now try to signin with email and password");
                    signInWithEmailAndPassword(email, password, databaseConnectListener, false);
                  }

                  @Override
                  public void onError() {
                    Logger.debug(TAG, "reauthentication error");
                    databaseConnectListener.onFail(FirebaseAuthError.ERROR_REQUIRES_RECENT_LOGIN.name(), FirebaseAuthError.ERROR_REQUIRES_RECENT_LOGIN.getDescription());
                  }
                });
              } else if (FirebaseAuthError.ERROR_USER_NOT_FOUND.equals(authError)) {
                Logger.debug(TAG, "No email & password, we try to create this user");
                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                  @Override
                  public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                      Logger.debug(TAG, "Email and password signed up, now sign in");
                      auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                          @Override
                          public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                              Logger.debug(TAG, "Email and password signed in success");
                              databaseConnectListener.onSuccess();
                            } else {
                              FirebaseAuthError firebaseAuthError = FirebaseAuthError.fromException(task.getException());
                              Logger.warning(TAG, "Email and password signed in exception", firebaseAuthError);
                              databaseConnectListener.onFail(firebaseAuthError.name(), firebaseAuthError.getDescription());
                            }
                          }
                        });
                    } else {
                      FirebaseAuthError authError1 = FirebaseAuthError.fromException(task.getException());
                      Logger.warning(TAG, "createUserWithEmailAndPassword", authError1);
                      databaseConnectListener.onFail(authError1.name(), authError1.getDescription());
                    }
                  }
                });
              } else {
                Logger.warning(TAG, "signInWithEmailAndPassword >>> ", authError);
                databaseConnectListener.onFail(authError.name(), authError.getDescription());
              }
            }
          }
        });
    } else {
      Logger.debug(TAG, "Already have signed user, try to link the data into that account");
      AuthCredential authCredential = EmailAuthProvider.getCredential(email, password);
      currentUser.linkWithCredential(authCredential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
        @Override
        public void onComplete(@NonNull Task<AuthResult> task) {
          if (task.isSuccessful()) {
            // Sign in success, update UI with the signed-in user's information
            Logger.debug(TAG, "linkWithCredential:success");
            FirebaseUser user = auth.getCurrentUser();
            if (user != null) {
              databaseConnectListener.onSuccess();
            }
          } else {
            Exception e = task.getException();
            FirebaseAuthError authError = FirebaseAuthError.fromException(e);
            Logger.debug(TAG, "linkWithCredential:exception", authError);
            if (reauthentication && FirebaseAuthError.ERROR_REQUIRES_RECENT_LOGIN.equals(authError)) {
              Logger.debug(TAG, "require reauthentication again");
              reauthentication(new OnResultListener() {
                @Override
                public void onSuccess() {
                  Logger.debug(TAG, "reauthentication success, now try to signin with email and password");
                  signInWithEmailAndPassword(email, password, databaseConnectListener, false);
                }

                @Override
                public void onError() {
                  Logger.debug(TAG, "reauthentication error");
                  databaseConnectListener.onFail(FirebaseAuthError.ERROR_REQUIRES_RECENT_LOGIN.name(), FirebaseAuthError.ERROR_REQUIRES_RECENT_LOGIN.getDescription());
                }
              });
            } else if (FirebaseAuthError.ERROR_USER_NOT_FOUND.equals(authError)) {
              Logger.debug(TAG, "Email & password account not exists, try to create one");

              auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                  if (task.isSuccessful()) {
                    Logger.debug(TAG, "Email & password account create success, link again");
                    AuthCredential authCredential = EmailAuthProvider.getCredential(email, password);
                    currentUser.linkWithCredential(authCredential)
                      .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                          if (task.isSuccessful()) {
                            Logger.debug(TAG, "Email & password account link success finally");

                            databaseConnectListener.onSuccess();
                          } else {
                            FirebaseAuthError firebaseAuthError = FirebaseAuthError.fromException(task.getException());
                            Logger.debug(TAG, "Email & password account link exception", firebaseAuthError);
                            databaseConnectListener.onFail(firebaseAuthError.name(), firebaseAuthError.getDescription());
                          }
                        }
                      });
                  } else {
                    FirebaseAuthError authError1 = FirebaseAuthError.fromException(task.getException());
                    databaseConnectListener.onFail(authError1.name(), authError1.getDescription());
                  }
                }
              });
            } else {
              databaseConnectListener.onFail(authError.name(), authError.getDescription());
            }
          }
        }
      });
    }
  }

  public void updatePassword(@NonNull String password, @NonNull OnPasswordChangedListener passwordChangedListener) {
    FirebaseUser user = auth.getCurrentUser();
    if (user == null) {
      passwordChangedListener.onError("ERROR_NOT_SIGNED_IN", "Not signed Inn");
      return;
    }
    user.updatePassword(password).addOnCompleteListener(task -> {
      if (task.isSuccessful()) {
        passwordChangedListener.onSuccess();
        return;
      }
      Exception e = task.getException();
      FirebaseAuthError authError = FirebaseAuthError.fromException(e);
      if (FirebaseAuthError.ERROR_REQUIRES_RECENT_LOGIN.equals(authError)) {
        Logger.debug(TAG, "require reauthentication again");
        reauthentication(new OnResultListener() {
          @Override
          public void onSuccess() {
            Logger.debug(TAG, "reauthentication success, now try to update the passsword");
            updatePassword(password, passwordChangedListener);
          }

          @Override
          public void onError() {
            Logger.debug(TAG, "reauthentication error");
            passwordChangedListener.onError(authError.name(), authError.getDescription());
          }
        });
      } else {
        passwordChangedListener.onError(authError.name(), authError.getDescription());
      }
    });
  }

  public void signOutFirestore() {
    auth.signOut();
  }
}
