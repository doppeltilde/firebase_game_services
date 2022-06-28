library firebase_game_services;

import 'dart:async';

import 'package:firebase_game_services_platform_interface/firebase_game_services_platform_interface.dart';

class FirebaseGameServices {
  static final platform = FirebaseGameServicesPlatform.instance;

  /// Unlock an [achievement].
  /// [Achievement] takes three parameters:
  /// [androidID] the achievement id for android.
  /// [iOSID] the achievement id for iOS.
  /// [percentComplete] the completion percent of the achievement, this parameter is
  /// optional in case of iOS.
  static Future<String?> unlock({achievement = Achievement}) async {
    return await platform.unlock(achievement: achievement);
  }

  /// Increment an [achievement].
  /// [Achievement] takes two parameters:
  /// [androidID] the achievement id for android.
  /// [steps] If the achievement is of the incremental type
  /// you can use this method to increment the steps.
  /// * only for Android (see https://developers.google.com/games/services/android/achievements#unlocking_achievements).
  static Future<String?> increment({achievement: Achievement}) async {
    return await platform.increment(achievement: achievement);
  }

  /// Submit a [score] to specific leader board.
  /// [Score] takes three parameters:
  /// [androidLeaderboardID] the leader board id that you want to send the score for in case of android.
  /// [iOSLeaderboardID] the leader board id that you want to send the score for in case of iOS.
  /// [value] the score.
  static Future<String?> submitScore({score: Score}) async {
    return await platform.submitScore(score: score);
  }

  /// It will open the achievements screen.
  static Future<String?> showAchievements() async {
    return await platform.showAchievements();
  }

  /// It will open the leaderboards screen.
  static Future<String?> showLeaderboards(
      {iOSLeaderboardID = "", androidLeaderboardID = ""}) async {
    return await platform.showLeaderboards(
        iOSLeaderboardID: iOSLeaderboardID,
        androidLeaderboardID: androidLeaderboardID);
  }

  /// Show the iOS Access Point.
  static Future<String?> showAccessPoint(AccessPointLocation location) async {
    return await platform.showAccessPoint(location);
  }

  /// Hide the iOS Access Point.
  static Future<String?> hideAccessPoint() async {
    return await platform.hideAccessPoint();
  }

  /// Try to sign in with native Game Service (Play Games on Android and GameCenter on iOS)
  /// Return `true` if success
  /// [clientId] is only for Android if you want to provide a clientId other than the main one in you google-services.json
  static Future<bool> signIn({String? clientId}) async {
    return await platform.signIn(clientId: clientId);
  }

  /// Try to sign link current user with native Game Service (Play Games on Android and GameCenter on iOS)
  /// Return `true` if success
  /// [clientId] is only for Android if you want to provide a clientId other than the main one in you google-services.json
  /// [forceSignInIfCredentialAlreadyUsed] make user force sign in with game services link failed because of ERROR_CREDENTIAL_ALREADY_IN_USE
  static Future<bool> signInLinkedUser(
      {String? clientId,
      bool forceSignInIfCredentialAlreadyUsed = false}) async {
    return await platform.signInLinkedUser(
        clientId: clientId,
        forceSignInIfCredentialAlreadyUsed: forceSignInIfCredentialAlreadyUsed);
  }

  /// Test if a user is already linked to a game service
  /// Advised to be call before linkGameServicesCredentialsToCurrentUser()
  static bool isUserLinkedToGameService() {
    return platform.isUserLinkedToGameService();
  }

  /// Get the player id.
  /// On iOS the player id is unique for your game but not other games.
  static Future<String?> getPlayerID() async {
    return await platform.getPlayerID();
  }

  /// Get the player name.
  /// On iOS the player alias is the name used by the Player visible in the leaderboard
  static Future<String?> getPlayerName() async {
    return await platform.getPlayerName();
  }
}
