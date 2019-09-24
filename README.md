# react-native-google-play-game-services

[![npm version](https://img.shields.io/npm/v/react-native-google-play-game-services.svg?style=flat-square)](https://www.npmjs.com/package/react-native-google-play-game-services)

React Native Google Play Game Services bindings for Android (Google Play Game Services for iOS no longer exists).

Currently implements achievements, leaderboards, snapshots (saved games). Events and more will be added on request.

## Requirements

### Android

* SDK 23+

## Compatibility

This package was built for React Native `0.40` or greater! If you're still on an earlier version you're welcome to check it out and patch the package.json file but there's not going to be anyone to support you.

## Before installing

It is highly recommended that prior to installing this library that you make yourself familiar with Google Play Game Services and [how to get started](https://developers.google.com/games/services/android/quickstart).

This document does not cover those steps but you will need to follow them prior to getting an app up and running.

There are two key steps in the process:
- Add a new game in the Google Play Console and note down your app id
- Link your app - if you don't follow this step correctly this library will silently fail

Also you will need to create some achievements and note down their ids for using the library.

## Installation

Currently there are a number of steps that cannot be automated.

Run `npm install --save react-native-google-play-game-services` to add the package to your app's dependencies.

## react-native cli

Run `react-native link react-native-google-play-game-services` so your project is linked against the Android library

## Manual steps

In your `android/build.gradle` add:

```diff
...
allprojects {
  repositories {
    ...
+   maven { url 'https://maven.google.com' }
  }
}
```

In your `android/app/build.gradle` (note: this is not the same file as above) add:

```diff
...
dependencies {
  ...
+  compile "com.google.android.gms:play-services-games:11.6.0"
+  compile "com.google.android.gms:play-services-auth:11.6.0"
}
...
```

In your `android/app/src/main/AndroidManifest.xml`:

```diff
<application>
        ...
+      <meta-data android:name="com.google.android.gms.games.APP_ID" android:value="@string/app_id" />
+      <meta-data android:name="com.google.android.gms.version" android:value="@integer/google_play_services_version"/>
        ...
</application>
```

In your `android/app/src/main/res/values/strings.xml` (create it if it doesn't exist) you need to add your app id:

```diff
<resources>
    ...
    <string name="app_name">gpsgexample</string>
    <string name="app_id">{your app id from google play console - 10-12 digit number}</string>
</resources>
```


Lastly ensure that you have Google Play Services installed on the target device.

For `Genymotion` you can follow [these instructions](http://stackoverflow.com/questions/20121883/how-to-install-google-play-services-in-a-genymotion-vm-with-no-drag-and-drop-su/20137324#20137324).
For a physical device you need to search on Google for 'Google Play Services'. There will be a link that takes you to the `Play Store` and from there you will see a button to update it (do not search within the `Play Store`).
ndroid Pay [deployment and testing](https://developers.google.com/android-pay/deployment).

## Usage

The API closely follows the Google Play Game Services Android API so if you're not following the below the official documentation should help.

Basically you need to sign in - you can do this silently or through the official UI from Google but you do need to call one of the two sigin in functions before any others will work so calling when your main UI component mounts is probably good practise. You may find if your app has been suspended it will also need to be called. How to deal with this I will leave as an exercise for the reader.

You can then assign achievements and show the achievement table. Simple.

```javascript
import RNGooglePlayGameServices from 'react-native-google-play-game-services';

// SIGN in prior to doing anything - silent sign in works for those who are already logged into google play gaming services
RNGooglePlayGameServices.signInSilently()
	.then((msg) => {

	})
	.catch((msg) => {
		//silent sign in didn't work so show the dialog instead
		//note probably should catch errors here
		RNGooglePlayGameServices.signInIntent();
	});

//unlock an achievement - note the id supplied is obtained by creating a new achievement in the play console
RNGooglePlayGameServices.unlockAchievement("CgkI8oW5sqwOEAIQAQ")
        .then((msg) => { console.log("unlocked achievement -  ",msg)})
        .catch((msg) => { console.log("not signed in - ",msg)});

//reveal an achievement - note the id supplied is obtained by creating a new achievement in the play console
RNGooglePlayGameServices.revealAchievement("CgkI8oW5sqwOEAIQAQ")
        .then((msg) => { console.log("achievement revealed -  ",msg)})
        .catch((msg) => { console.log("not signed in - ",msg)});

//increment an achievement - note the id supplied is obtained by creating a new achievement in the play console
RNGooglePlayGameServices.incrementAchievement("CgkI8oW5sqwOEAIQAw",10)
        .then((msg) => { console.log("incremented achievement -  ",msg)})
		.catch((msg) => { console.log("not signed in - ",msg)});

//set an achievement to specific value - note the id supplied is obtained by creating a new achievement in the play console
RNGooglePlayGameServices.setAchievementSteps("CgkI8oW5sqwOEAIQAw",7)
        .then((msg) => { console.log("set achievement -  ",msg)})
        .catch((msg) => { console.log("not signed in - ",msg)});


//show player the achievements list
//note: should also catch errors here
RNGooglePlayGameServices.showAchievements();


//set the player score for an specifil leaderboard
//note: should also catch errors here
RNGooglePlayGameServices.setLeaderboardScore("AbCI8oW5sqwOEAXXXX",200);

//get the player score for an specifil leaderboard
//note: should also catch errors here
RNGooglePlayGameServices.getLeaderboardScore("AbCI8oW5sqwOEAXXXX")
    .then((score)=>console.log('Score:', score))
    .catch(error=>console.log('Error retrieving score:', error))

//show all leaderboards
//note: should also catch errors here
RNGooglePlayGameServices.showAllLeaderboards();

//show specific leaderboard
//note: should also catch errors here
RNGooglePlayGameServices.showLeaderboard("AbCI8oW5sqwOEAXXXX");

//Load a saved game:
RNGooglePlayGameServices.loadSnapshot("my_game").then(res=>{
    console.log(res);
}).catch(error=>{
    console.log('Error loading game:', error);
});

// Once you've loaded a game, you can close it or update it.
// To close it:
RNGooglePlayGameServices.discardAndCloseSnapshot();

// To write / overwrite it:
RNGooglePlayGameServices.commitAndCloseSnapshot("new saved data", "some description");


// Am I signed in?
RNGooglePlayGameServices.isSignedIn()
        .then((msg) => { console.log("signed in - ",msg)})
        .catch((msg) => { console.log("not signed in - ",msg)});

//sign out
RNGooglePlayGameServices.signOut()
        .then((msg) => { console.log("signed out - ",msg)})
		.catch((msg) => { console.log("not signed out - ",msg)});

// Get current player ID
RNGooglePlayGameServices.getUserId()
        .then(id => { console.log("ID:", id); })
        .catch(err => { console.log("Error:", err); })

```


## Troubleshooting

### It won't compile

Check you followed all the steps in the install guide

### It's not doing anything or failing silently

Use the command adb logcat and grep for OAuth - if you're getting OAuth failures it's because you haven't linked your app properly. You need to follow the [getting started](https://developers.google.com/games/services/android/quickstart) instructions. There will be two apps - one is your debug and one is your release. It's important that you have two because the production OAuth doesn't work with debug and vice versa.


  
