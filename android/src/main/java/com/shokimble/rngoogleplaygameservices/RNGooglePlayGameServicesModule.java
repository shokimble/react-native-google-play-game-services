
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
import android.app.Activity;
import android.content.Intent;
import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.games.AchievementsClient;
import com.google.android.gms.games.AnnotatedData;
import com.google.android.gms.games.EventsClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.LeaderboardsClient;
import com.google.android.gms.games.leaderboard.LeaderboardScore;
import com.google.android.gms.games.leaderboard.LeaderboardVariant;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayersClient;
import com.google.android.gms.games.event.Event;
import com.google.android.gms.games.event.EventBuffer;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadata;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.games.SnapshotsClient;
import com.google.android.gms.games.SnapshotsClient.DataOrConflict;
import com.google.android.gms.games.SnapshotsClient.SnapshotConflict;

/*
TODO implement:

PlayersClient functions so you can retrieve player name - https://developers.google.com/games/services/android/signin

Events, etc
 */


public class RNGooglePlayGameServicesModule extends ReactContextBaseJavaModule {

  private final ReactApplicationContext reactContext;

  private GoogleSignInAccount googleSignInAccount;
  private GoogleSignInClient mGoogleSignInClient;
  private AchievementsClient mAchievementsClient;
  private LeaderboardsClient mLeaderboardsClient;
  private PlayersClient mPlayersClient;
  private SnapshotsClient mSnapshotsClient;
  private Promise signInPromise;
  private Promise achievementPromise;
  private Promise leaderboardPromise;
  private Promise requestPermissionPromise;
  private String requestPermissionFile;
  private Snapshot workingSnapshot;

  //activity result code
  private static final int RC_SIGN_IN = 9001;
  private static final int RC_ACHIEVEMENT_UI = 9003;
  private static final int RC_LEADERBOARD_UI = 9004;
  private static final int RC_REQUEST_PERMISSION_SUCCESS_CONTINUE_FILE_CREATION = 9005;

  // tag for debug logging
  private static final String TAG = "shorngames";

  /////////////////////////////////////////////////////////////////////////////

  private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
      super.onActivityResult(activity, requestCode, resultCode, intent);

      if (requestCode == RC_ACHIEVEMENT_UI){
        if(achievementPromise == null) return;
        achievementPromise.resolve("Achievement dialog complete");
        return;
      }

      if (requestCode == RC_LEADERBOARD_UI){
        if(leaderboardPromise == null) return;
        leaderboardPromise.resolve("Leaderboard dialog complete");
        return;
      }

      if (requestCode == RC_REQUEST_PERMISSION_SUCCESS_CONTINUE_FILE_CREATION){
        if(requestPermissionPromise == null) return;
        if (resultCode == Activity.RESULT_OK) {
          signInSilently(null);
          requestPermissionPromise.reject("Drive.SCOPE_APPFOLDER: Granted");
        }else{
          requestPermissionPromise.reject("Drive.SCOPE_APPFOLDER: Denied");
        }
        requestPermissionFile = null;
        requestPermissionPromise = null;
        return;
      }

      if (requestCode == RC_SIGN_IN) {
        Task<GoogleSignInAccount> task =
                GoogleSignIn.getSignedInAccountFromIntent(intent);
        try {
          GoogleSignInAccount account = task.getResult(ApiException.class);
          onConnected(account);
          if (signInPromise != null) {
            signInPromise.resolve("Signed in");
          }
        } catch (ApiException apiException) {
          onDisconnected();
          if (signInPromise != null) {
            signInPromise.reject("Can't sign in");
          }
        }
        signInPromise = null;
        return;
      }
    }

  };

  /////////////////////////////////////////////////////////////////////////////

  public RNGooglePlayGameServicesModule (ReactApplicationContext reactContext)  {
    super(reactContext);
    this.reactContext = reactContext;
    reactContext.addActivityEventListener(mActivityEventListener);
  }

  /////////////////////////////////////////////////////////////////////////////

  @Override
  public String getName() {
    return "RNGooglePlayGameServices";
  }

  /////////////////////////////////////////////////////////////////////////////

  @ReactMethod
  public void isSignedIn(final Promise promise) {
    if(GoogleSignIn.getLastSignedInAccount(getCurrentActivity()) != null)
      promise.resolve("signed in");
    else
      promise.reject("not signed in");
  }

  /////////////////////////////////////////////////////////////////////////////

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
                  if(promise!=null) promise.resolve("silent sign in successful");
                } else {
                  Log.d(TAG, "signInSilently(): failure", task.getException());
                  onDisconnected();
                  if(promise!=null) promise.reject("silent sign in failed");
                }
              }
            }
    );
  }

  /////////////////////////////////////////////////////////////////////////////

  @ReactMethod
  public void signInIntent(final Promise promise) {
    signInPromise = promise;
    GoogleSignInClient signInClient = GoogleSignIn.getClient(getCurrentActivity(), GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN);
    getCurrentActivity().startActivityForResult( signInClient.getSignInIntent(), RC_SIGN_IN);
  }

  /////////////////////////////////////////////////////////////////////////////

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

  /////////////////////////////////////////////////////////////////////////////

  @ReactMethod
  public void revealAchievement(String id, final Promise promise) {
    if(mAchievementsClient == null) {
      promise.reject("Please sign in first");
      return;
    }
    mAchievementsClient.reveal(id);
    promise.resolve("Revealed achievement");
  }

  /////////////////////////////////////////////////////////////////////////////

  @ReactMethod
  public void getUserId(final Promise promise) {
    mPlayersClient.getCurrentPlayerId().addOnCompleteListener(getCurrentActivity(),
            new OnCompleteListener<String>() {
              @Override
              public void onComplete(@NonNull Task<String> task) {
                if (task.isSuccessful()) {
                  promise.resolve(task.getResult());
                } else {
                  promise.reject("Get ID failed");
                }
              }
            }
    );
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

  /////////////////////////////////////////////////////////////////////////////

  @ReactMethod
  public void incrementAchievement(String id, int inc, final Promise promise) {
    if(mAchievementsClient == null) {
      promise.reject("Please sign in first");
      return;
    }
    mAchievementsClient.increment(id, inc);
    promise.resolve("Incremented achievement");
  }

  /////////////////////////////////////////////////////////////////////////////

  @ReactMethod
  public void setAchievementSteps(String id, int steps, final Promise promise) {
    if(mAchievementsClient == null) {
      promise.reject("Please sign in first");
      return;
    }
    mAchievementsClient.setSteps(id, steps);
    promise.resolve("Achievement setps' set");
  }

  /////////////////////////////////////////////////////////////////////////////

  @ReactMethod
  public void discardAndCloseSnapshot( final Promise promise ) {
    if(mSnapshotsClient == null) {
      promise.reject("Please sign in first");
      return;
    }
    if(workingSnapshot == null) {
      promise.resolve(null);
      return;
    }
    mSnapshotsClient.discardAndClose(workingSnapshot);
    workingSnapshot = null;
    promise.resolve(null);
  }

  /////////////////////////////////////////////////////////////////////////////

  @ReactMethod
  public void commitAndCloseSnapshot(String data, String description, final Promise promise ) {
    if(mSnapshotsClient == null) {
      promise.reject("Please sign in first");
      return;
    }
    if(workingSnapshot == null) {
      promise.resolve(null);
      return;
    }
    try{
      SnapshotMetadataChange.Builder mc = new SnapshotMetadataChange.Builder();
      mc.fromMetadata(workingSnapshot.getMetadata());
      mc.setDescription(description);
      workingSnapshot.getSnapshotContents().writeBytes(data.getBytes());
      mSnapshotsClient.commitAndClose(workingSnapshot, mc.build());
      workingSnapshot = null;
      promise.resolve(null);
    }catch(Exception e){
      promise.reject("Error commiting Snapshot: "+e.getMessage());
    }
  }

  /////////////////////////////////////////////////////////////////////////////

  @ReactMethod
  public void loadSnapshot(String name, final Promise promise ) {

    if(mSnapshotsClient == null) {
      promise.reject("Please sign in first");
      return;
    }

    if (!GoogleSignIn.hasPermissions( googleSignInAccount, Drive.SCOPE_APPFOLDER)) {
      this.requestScopeAppFolder(name, promise);
      return;
    }

    mSnapshotsClient.open(name, true, SnapshotsClient.RESOLUTION_POLICY_MANUAL)
      .addOnSuccessListener(new OnSuccessListener<DataOrConflict<Snapshot>>() {
        @Override
        public void onSuccess(DataOrConflict<Snapshot> result) {
          if (!result.isConflict()) {
            try{
              workingSnapshot = result.getData();
              WritableMap map = Arguments.createMap();
              map.putBoolean("isConflict", false);
              map.putString("data",new String(workingSnapshot.getSnapshotContents().readFully(), "UTF-8"));
              promise.resolve(map);
            }catch(Exception e){
              workingSnapshot = null;
              promise.reject("Error reading snapshot!");
            }
            return;
          }
          try{
            SnapshotConflict conflict = result.getConflict();
            workingSnapshot = conflict.getSnapshot();
            Snapshot conflictSnapshot = conflict.getConflictingSnapshot();
            mSnapshotsClient.resolveConflict(conflict.getConflictId(), workingSnapshot);
            WritableMap map = Arguments.createMap();
            map.putBoolean("isConflict", true);
            map.putString("data",new String(workingSnapshot.getSnapshotContents().readFully(),"UTF-8"));
            map.putString("conflictData",new String(conflictSnapshot.getSnapshotContents().readFully(),"UTF-8"));
            promise.resolve(map);
          }catch(Exception e){
            workingSnapshot = null;
            promise.reject("Error reading snapshot!");
          }
        }
      })
      .addOnFailureListener(new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception e) {
          promise.reject("LoadSnapshot: FAILURE - "+e.getMessage());
        }
      });
  }

  /////////////////////////////////////////////////////////////////////////////

  @ReactMethod
  public void setLeaderboardScore(String id, int score, final Promise promise) {
    if(mLeaderboardsClient == null) {
      promise.reject("Please sign in first");
      return;
    }
    mLeaderboardsClient.submitScore(id, score);
    promise.resolve("Leaderboard score set");
  }

  /////////////////////////////////////////////////////////////////////////////

  @ReactMethod
  public void getLeaderboardScore(String id, final Promise promise) {
    if(mLeaderboardsClient == null) {
      promise.reject("Please sign in first");
      return;
    }
    mLeaderboardsClient.loadCurrentPlayerLeaderboardScore(id, LeaderboardVariant.TIME_SPAN_ALL_TIME, LeaderboardVariant.COLLECTION_PUBLIC)
      .addOnSuccessListener(new OnSuccessListener<AnnotatedData<LeaderboardScore>>() {
        @Override
        public void onSuccess(AnnotatedData<LeaderboardScore> leaderboardScoreAnnotatedData) {
          if (leaderboardScoreAnnotatedData == null) {
            promise.resolve(null);
            return;
          }
          if (leaderboardScoreAnnotatedData.get() == null) {
            promise.resolve(null);
            return;
          }
          promise.resolve(""+leaderboardScoreAnnotatedData.get().getRawScore());
        }
      })
      .addOnFailureListener(new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception e) {
          promise.reject("LeaderBoard: FAILURE");
        }
      });
  }

  /////////////////////////////////////////////////////////////////////////////

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

  /////////////////////////////////////////////////////////////////////////////

  @ReactMethod
  public void showAllLeaderboards(final Promise promise) {
    if(mLeaderboardsClient == null) {
      promise.reject("Please sign in first");
      return;
    }

    leaderboardPromise = promise;

    mLeaderboardsClient.getAllLeaderboardsIntent()
            .addOnSuccessListener(new OnSuccessListener<Intent>() {
              @Override
              public void onSuccess(Intent intent) {
               getCurrentActivity().startActivityForResult(intent, RC_LEADERBOARD_UI);
              }
            })
            .addOnFailureListener(new OnFailureListener() {
              @Override
              public void onFailure(@NonNull Exception e) {
               promise.reject("Could not launch leaderboards intent");
              }
            });
  }

  /////////////////////////////////////////////////////////////////////////////

  @ReactMethod
  public void showLeaderboard(String id, final Promise promise) {
    if(mLeaderboardsClient == null) {
      promise.reject("Please sign in first");
      return;
    }

    leaderboardPromise = promise;

    mLeaderboardsClient.getLeaderboardIntent(id)
            .addOnSuccessListener(new OnSuccessListener<Intent>() {
              @Override
              public void onSuccess(Intent intent) {
               getCurrentActivity().startActivityForResult(intent, RC_LEADERBOARD_UI);
              }
            })
            .addOnFailureListener(new OnFailureListener() {
              @Override
              public void onFailure(@NonNull Exception e) {
               promise.reject("Could not launch leaderboards intent");
              }
            });
  }

  /////////////////////////////////////////////////////////////////////////////

  private void requestScopeAppFolder(String name, final Promise promise){
    requestPermissionPromise = promise;
    requestPermissionFile = name;
    Log.d(TAG, "Drive.SCOPE_APPFOLDER will be requested");
    GoogleSignIn.requestPermissions(
      getCurrentActivity(),
      RC_REQUEST_PERMISSION_SUCCESS_CONTINUE_FILE_CREATION,
      googleSignInAccount,
      Drive.SCOPE_APPFOLDER);
  }

  /////////////////////////////////////////////////////////////////////////////

  private void onConnected(GoogleSignInAccount googleSignInAccount) {
    Log.d(TAG, "onConnected(): connected to Google APIs");
    this.googleSignInAccount = googleSignInAccount;

    Games.getGamesClient(getCurrentActivity(),googleSignInAccount)
         .setViewForPopups(getCurrentActivity().getWindow().getDecorView().findViewById(android.R.id.content));

    mAchievementsClient = Games.getAchievementsClient(getCurrentActivity(), googleSignInAccount);
    mLeaderboardsClient = Games.getLeaderboardsClient(getCurrentActivity(), googleSignInAccount);
    mPlayersClient = Games.getPlayersClient(getCurrentActivity(), googleSignInAccount);
    mSnapshotsClient = Games.getSnapshotsClient(getCurrentActivity(), googleSignInAccount);
    //TODO add these later
    //mEventsClient = Games.getEventsClient(this, googleSignInAccount);
  }

  /////////////////////////////////////////////////////////////////////////////

  private void onDisconnected() {
    Log.d(TAG, "onDisconnected()");

    googleSignInAccount = null;
    mAchievementsClient = null;
    mLeaderboardsClient = null;
    mPlayersClient = null;
    mSnapshotsClient = null;
    //mEventsClient = null;
  }

  /////////////////////////////////////////////////////////////////////////////

}