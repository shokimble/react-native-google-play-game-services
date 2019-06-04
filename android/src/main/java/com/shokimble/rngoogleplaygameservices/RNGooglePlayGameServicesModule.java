package com.shokimble.rngoogleplaygameservices;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ActivityEventListener;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;

import android.util.Log;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.games.*;
import com.google.android.gms.games.AchievementsClient;
import com.google.android.gms.games.AnnotatedData;
import com.google.android.gms.games.EventsClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.LeaderboardsClient;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayersClient;
import com.google.android.gms.games.event.Event;
import com.google.android.gms.games.event.EventBuffer;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

/*
TODO implement:

PlayersClient functions so you can retrieve player name - https://developers.google.com/games/services/android/signin

Events, leaderboards etc
 */


public class RNGooglePlayGameServicesModule extends ReactContextBaseJavaModule {

  private final ReactApplicationContext reactContext;

  private GoogleSignInClient mGoogleSignInClient;
  private AchievementsClient mAchievementsClient;
  private PlayersClient mPlayersClient;

  private Promise signInPromise;
  private Promise achievementPromise;

  //activity result code
  private static final int RC_SIGN_IN = 9001;
  private static final int RC_ACHIEVEMENT_UI = 9003;

  // tag for debug logging
  private static final String TAG = "shorngames";

  private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
      super.onActivityResult(requestCode, resultCode, intent);

      if (requestCode == RC_ACHIEVEMENT_UI && achievementPromise != null)
        achievementPromise.resolve("Achievement dialog complete");

      if (requestCode == RC_SIGN_IN) {
        Log.d(TAG, "RC_SIGN_IN result returned");
        Task<GoogleSignInAccount> task =
                GoogleSignIn.getSignedInAccountFromIntent(intent);

        try {
          GoogleSignInAccount account = task.getResult(ApiException.class);
          onConnected(account);
          if (signInPromise != null)
            handleSignInResult(promise);

        } catch (ApiException apiException) {
          onDisconnected();
          handleSignInResult(promise, true);
        }
      }
    }
  };

  public RNGooglePlayGameServicesModule (ReactApplicationContext reactContext)  {
    super(reactContext);
    this.reactContext = reactContext;

    reactContext.addActivityEventListener(mActivityEventListener);

    //initialise client
    //mGoogleSignInClient = GoogleSignIn.getClient(getCurrentActivity(),
    //    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN).build());

  }

  @Override
  public String getName() {
    return "RNGooglePlayGameServices";
  }

  @ReactMethod
  public void isSignedIn(final Promise promise) {

    if(GoogleSignIn.getLastSignedInAccount(getCurrentActivity()) != null)
      promise.resolve("signed in");
    else
      promise.reject("not signed in");
  }

  // +AS:20190504 TODO: implement normal signin here

  @ReactMethod
  public void signInSilently(final Promise promise) {
    Log.d(TAG, "signInSilently()");
    GoogleSignInClient signInClient = GoogleSignIn.getClient(getCurrentActivity(), GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN);

    signInClient.silentSignIn().addOnCompleteListener(getCurrentActivity(),
            new OnCompleteListener<GoogleSignInAccount>() {
              @Override
              public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                if (task.isSuccessful()) {
                  Log.d(TAG, "signInSilently(): success");
                  onConnected(task.getResult());
                  handleSignInResult(promise);
                } else {
                  Log.d(TAG, "signInSilently(): failure", task.getException());
                  onDisconnected();
                  handleSignInResult(promise, true);
                }
              }
            }
    );
  }

  private void handleSignInResult(final Promise promise, boolean isPreFailed = false)
  {
    if (!isPreFailed)
    {
      mPlayersClient.getCurrentPlayer().addOnSuccessListener(new OnSuccessListener<Player>() {
        @Override
        public void onSuccess(Player player) {

          JSONObject result = new JSONObject();
          JSONObject userInfo = new JSONObject();

          try {
            userInfo.put("google_id", player.getPlayerId());
            userInfo.put("name", player.getDisplayName());
            userInfo.put("avatar_url", player.getIconImageUri());
            result.put("result", true);
            result.put("data", userInfo);

          } catch (JSONException e) {
            e.printStackTrace();
          }

          promise.resolve(result.toString());
        }
      })
      .addOnFailureListener(new OnFailureListener()
      {
        @Override
        public void onFailure(Exception e) {
          onDisconnected();
          handleSignInResult(promise, true);
        }
      });
    }
    else
    {
      if (promise != null)
      {
        JSONObject result = new JSONObject();

        try {
          result.put("result", false);

        } catch (JSONException e) {
          e.printStackTrace();
        }

        promise.reject(result.toString());
      }
    }
  }

  @ReactMethod
  public void signInIntent(final Promise promise) {
    signInPromise = promise;

    GoogleSignInClient signInClient = GoogleSignIn.getClient(getCurrentActivity(), GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN);

    getCurrentActivity().startActivityForResult( signInClient.getSignInIntent(), RC_SIGN_IN);

    /*
    signInClient.getSignInIntent().addOnSuccessListener(new OnSuccessListener<Intent>() {
      @Override
      public void onSuccess(Intent intent) {
        getCurrentActivity().startActivityForResult(intent, RC_SIGN_IN);
      }
    })
    .addOnFailureListener(new OnFailureListener() {
      @Override
      public void onFailure(@NonNull Exception e) {
        promise.reject("Could not launch sign in  intent");
      }
    });
    */
  }

  @ReactMethod
  public void signOut(final Promise promise) {
    Log.d(TAG, "signOut()");

    GoogleSignInClient signInClient = GoogleSignIn.getClient(getCurrentActivity(), GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN);

    signInClient.signOut().addOnCompleteListener(getCurrentActivity(),
            new OnCompleteListener<Void>() {
              @Override
              public void onComplete(@NonNull Task<Void> task) {
                boolean successful = task.isSuccessful();
                Log.d(TAG, "signOut(): " + (successful ? "success" : "failed"));
                onDisconnected();
                promise.resolve("signed out");
              }
            });
  }

  @ReactMethod
  public void unlockAchievement(String id, final Promise promise) {
    if(mAchievementsClient == null) {
      promise.reject("Please sign in first");
      return;
    }
    mAchievementsClient.unlock(id);
    promise.resolve("Unlocked achievement");
  }

  @ReactMethod
  public void incrementAchievement(String id, int inc, final Promise promise) {
    if(mAchievementsClient == null) {
      promise.reject("Please sign in first");
      return;
    }
    mAchievementsClient.increment(id, inc);
    promise.resolve("Unlocked incremented");
  }

  @ReactMethod
  public void showAchievements(final Promise promise) {
    if(mAchievementsClient == null) {
      promise.reject("Please sign in first");
      return;
    }

    achievementPromise = promise;

    mAchievementsClient.getAchievementsIntent()
            .addOnSuccessListener(new OnSuccessListener<Intent>() {
              @Override
              public void onSuccess(Intent intent) {
               getCurrentActivity().startActivityForResult(intent, RC_ACHIEVEMENT_UI);
              }
            })
            .addOnFailureListener(new OnFailureListener() {
              @Override
              public void onFailure(@NonNull Exception e) {
               promise.reject("Could not launch achievements intent");
              }
            });
  }



  private void onConnected(GoogleSignInAccount googleSignInAccount) {
    Log.d(TAG, "onConnected(): connected to Google APIs");

    mAchievementsClient = Games.getAchievementsClient(getCurrentActivity(), googleSignInAccount);
    //TODO add these later
    //mLeaderboardsClient = Games.getLeaderboardsClient(this, googleSignInAccount);
    //mEventsClient = Games.getEventsClient(this, googleSignInAccount);
    mPlayersClient = Games.getPlayersClient(getCurrentActivity(), googleSignInAccount);

  }


  private void onDisconnected() {
    Log.d(TAG, "onDisconnected()");

    mAchievementsClient = null;
    mPlayersClient = null;
    //LeaderboardsClient = null;
    //PlayersClient = null;

  }
}
