# Firebase Game Services

[![Pub](https://img.shields.io/pub/v/firebase_game_services.svg?style=popout&include_prereleases)](https://pub.dartlang.org/packages/firebase_game_services)

Dart package linking Google's Play Games and Apple's Game Center with Firebase.

---

## Setup

#### iOS Setup
1. [Firebase iOS Setup](https://firebase.google.com/docs/flutter/setup?platform=ios).
2. [Authenticate Using Game Center](https://firebase.google.com/docs/auth/ios/game-center).

- [Apple Developer instructions](https://developer.apple.com/library/archive/documentation/NetworkingInternet/Conceptual/GameKit_Guide/GameCenterOverview/GameCenterOverview.html#//apple_ref/doc/uid/TP40008304-CH5-SW22).
- [Enabling and Configuring Game Center](https://developer.apple.com/documentation/gamekit/enabling_and_configuring_game_center).

#### Android Setup
1. [Firebase Android Setup](https://firebase.google.com/docs/flutter/setup?platform=android).
2. [Authenticate Using Google Play Games Services on Android](https://firebase.google.com/docs/auth/android/play-games).

---

## Usage

#### Sign in
Call this before making any other action.
```dart
await FirebaseGameServices.instance.signIn();
```

#### Sign in linked user
Signs in the currently linked user with native Game Service (Play Games on Android and GameCenter on iOS) to Firebase.
```dart
await FirebaseGameServices.instance.signInLinkedUser();
```

#### Save Game Data to Firebase
This package works in harmony with the Firebase stack.
You can utilize both `Cloud Firestore` and/or `Realtime Database` for storing, syncing, and querying data - whatever suits your project best.

- [Get started with Cloud Firestore](https://firebase.google.com/docs/firestore/quickstart).
- [Get started with Realtime Database](https://firebase.google.com/docs/database/flutter/start).

#####Firestore Example
```dart
    final FirebaseFirestore firestore = FirebaseFirestore.instance;
    final FirebaseAuth auth = FirebaseAuth.instance;

    try {
        if (auth.currentUser != null) {
        if (myValue == null || myValue.isEmpty) {
            throw Exception('myValue cannot be null or empty');
        }
        
        await firestore.collection("my-collection").doc(auth.currentUser!.uid).set(
            {
            "my-field": myValue,
            },
            SetOptions(merge: true),
        );
        } else {
        throw Exception('User is not signed in');
        }
    } catch (e) {
        debugPrint('Error: $e');
    }
```

For static storage, I'd recommend using `Cloud Storage`.

- [Get started with Cloud Storage](https://firebase.google.com/docs/storage/flutter/start).

Of course you can also use your own backend.

#### Show leaderboards
To show the leaderboards screen. It takes the leaderbord id for android and iOS.  
``` dart
await FirebaseGameServices.instance.showLeaderboards(iOSLeaderboardID: 'ios_leaderboard_id', androidLeaderboardID: 'android_leaderboard_id');
```   

#### Submit score  
To submit a ```Score``` to specific leaderboard.  
-The ```Score``` class takes three parameters:  
-```androidLeaderboardID```: the leader board id that you want to send the score for in case of android.  
-```iOSLeaderboardID``` the leader board id that you want to send the score for in case of iOS.  
-```value``` the score.  

``` dart
await FirebaseGameServices.instance.submitScore(
score: Score(
    androidLeaderboardID: 'android_leaderboard_id',
    iOSLeaderboardID: 'ios_leaderboard_id', 
    value: 5,
    )
);
```  

#### Unlock achievement  
To unlock an ```Achievement```.  
The ```Achievement``` takes three parameters:  
-```androidID``` the achievement id for android.  
-```iOSID``` the achievement id for iOS.  
-```percentComplete``` the completion percent of the achievement, this parameter is optional in case of iOS.  
-```steps``` the achievement steps for Android.

``` dart
await FirebaseGameServices.instance.unlock(
achievement: Achievement(
    androidID: 'android_id', iOSID: 'ios_id',
    percentComplete: 100, steps: 2
    ),
);
```

#### Show achievements
```dart
await FirebaseGameServices.instance.showAchievements();
```

#### Player Name
To get the player name:
```dart
final playerName = await FirebaseGameServices.instance.getPlayerName();
```

#### Player Id
To get the player id:
```dart
final playerId = await FirebaseGameServices.instance.getPlayerId();
```

#### Sign out
Signs the user out (not recommended):
```dart
await FirebaseAuth.instance.signOut();
```

## Platform specific
#### Increment (Android Only)  
To increment the steps for android achievement.

```dart
await FirebaseGameServices.instance.increment(achievement: Achievement(androidID: 'android_id', steps: 50));
```  

#### Show AccessPoint (iOS Only)  
To show the access point you can call the following function:  

```dart
await FirebaseGameServices.instance.showAccessPoint(AccessPointLocation.topLeading);
```  

#### Hide AccessPoint (iOS Only)  
To hide the access point.

```dart
await FirebaseGameServices.instance.hideAccessPoint();
```

---

*Notice:*
*This package was initally created to be used in-house, as such the development is first and foremost aligned with the internal requirements.*