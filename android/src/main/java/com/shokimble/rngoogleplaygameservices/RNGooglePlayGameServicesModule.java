
package com.reactlibrary;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;

import android.util.Log;
import android.content.Intent;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
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

  public RNGooglePlayGameServicesModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;

    //initialise client
    this.mGoogleSignInClient = GoogleSignIn.getClient(this,
        new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN).build());

  }

  @Override
  public String getName() {
    return "RNGooglePlayGameServices";
  }

  @ReactMethod
  public void isSignedIn(final Promise promise) {

    if(mGoogleSignIn.getLastSignedInAccount(this) != null)
      promise.resolve("signed in");
    else
      promise.reject("not signed in");
  }

  @ReactMethod
  public void signInSilently(final Promise promise) {
    Log.d(TAG, "signInSilently()");

    mGoogleSignInClient.silentSignIn().addOnCompleteListener(this,
            new OnCompleteListener<GoogleSignInAccount>() {
              @Override
              public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                if (task.isSuccessful()) {
                  Log.d(TAG, "signInSilently(): success");
                  onConnected(task.getResult());
                  promise.resolve("silent sign in successful")
                } else {
                  Log.d(TAG, "signInSilently(): failure", task.getException());
                  onDisconnected();
                  promise.reject("silent sign in failed");
                }
              }
            }
    );
  }

  @ReactMethod
  public void signInIntent(final Promise promise) {
    signInPromise = promise;
    startActivityForResult(mGoogleSignInClient.getSignInIntent(), RC_SIGN_IN);
  }

  @ReactMethod
  public void signOut(final Promise promise) {
    Log.d(TAG, "signOut()");

    if (!isSignedIn()) {
      Log.w(TAG, "signOut() called, but was not signed in!");
      return;
    }

    mGoogleSignInClient.signOut().addOnCompleteListener(this,
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

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);

    if (requestCode == RC_ACHIEVEMENT_UI && achievementPromise != null)
      achievementPromise.resolve("Achievement dialog complete");

    if (requestCode == RC_SIGN_IN) {
      Task<GoogleSignInAccount> task =
              GoogleSignIn.getSignedInAccountFromIntent(intent);

      try {
        GoogleSignInAccount account = task.getResult(ApiException.class);
        onConnected(account);
        if(signInPromise != null)
          signInPromise.resolve("Signed in");
      } catch (ApiException apiException) {
        String message = apiException.getMessage();
        if (message == null || message.isEmpty()) {
          message = getString(R.string.signin_other_error);
        }
        if(signInPromise != null)
          signInPromise.reject("Can't sign in");

        onDisconnected();
      }
    }
  }


  @ReactMethod
  public void unlockAchievement(String id, final Promise promise) {
    mAchievementsClient.unlock(id);
    promise.resolve("Unlocked achievement");
  }

  @ReactMethod
  public void incrementAchievement(String id, int inc, final Promise promise) {
    mAchievementsClient.increment(id, inc);
    promise.resolve("Unlocked incremented");
  }

  @ReactMethod
  public void showAchievements(final Promise promise) {
    achievementPromise = promise;
    startActivityForResult(mAchievementsClient.getAchievementsIntent(), RC_ACHIEVEMENT_UI);
  }



  private void onConnected(GoogleSignInAccount googleSignInAccount) {
    Log.d(TAG, "onConnected(): connected to Google APIs");

    mAchievementsClient = Games.getAchievementsClient(this, googleSignInAccount);
    //TODO add these later
    //mLeaderboardsClient = Games.getLeaderboardsClient(this, googleSignInAccount);
    //mEventsClient = Games.getEventsClient(this, googleSignInAccount);
    mPlayersClient = Games.getPlayersClient(this, googleSignInAccount);

  }


  private void onDisconnected() {
    Log.d(TAG, "onDisconnected()");

    mAchievementsClient = null;
    //LeaderboardsClient = null;
    //PlayersClient = null;

  }
}