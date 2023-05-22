import 'package:firebase_game_services/firebase_game_services.dart';

extension FirebaseGameServicesAchievements on FirebaseGameServices {
  /// Unlock an [achievement].
  /// [Achievement] takes three parameters:
  /// [androidID] the achievement id for android.
  /// [iOSID] the achievement id for iOS.
  /// [percentComplete] the completion percent of the achievement, this parameter is
  /// optional in case of iOS.
  Future<String?> unlock({achievement = Achievement}) async {
    return await FirebaseGameServices.platform.unlock(achievement: achievement);
  }

  /// Increment an [achievement].
  /// [Achievement] takes two parameters:
  /// [androidID] the achievement id for android.
  /// [steps] If the achievement is of the incremental type
  /// you can use this method to increment the steps.
  /// * only for Android (see https://developers.google.com/games/services/android/achievements#unlocking_achievements).
  Future<String?> increment({achievement = Achievement}) async {
    return await FirebaseGameServices.platform
        .increment(achievement: achievement);
  }

  /// Submit a [score] to specific leader board.
  /// [Score] takes three parameters:
  /// [androidLeaderboardID] the leader board id that you want to send the score for in case of android.
  /// [iOSLeaderboardID] the leader board id that you want to send the score for in case of iOS.
  /// [value] the score.
  Future<String?> submitScore({score = Score}) async {
    return await FirebaseGameServices.platform.submitScore(score: score);
  }

  /// It will open the achievements screen.
  Future<String?> showAchievements() async {
    return await FirebaseGameServices.platform.showAchievements();
  }

  /// Opens a single leaderboard screen.
  Future<String?> showSingleLeaderboard(
      {iOSLeaderboardID = "", androidLeaderboardID = ""}) async {
    return await FirebaseGameServices.platform.showSingleLeaderboard(
        iOSLeaderboardID: iOSLeaderboardID,
        androidLeaderboardID: androidLeaderboardID);
  }

  /// Opens a single leaderboard screen.
  @Deprecated('Use showSingleLeaderboard() instead.')
  Future<String?> showLeaderboards(
      {iOSLeaderboardID = "", androidLeaderboardID = ""}) async {
    return await FirebaseGameServices.platform.showSingleLeaderboard(
        iOSLeaderboardID: iOSLeaderboardID,
        androidLeaderboardID: androidLeaderboardID);
  }

  /// Presents the list of leaderboards.
  Future<String?> showAllLeaderboards() async {
    return await FirebaseGameServices.platform.showAllLeaderboards();
  }

  /// Show the iOS Access Point.
  Future<String?> showAccessPoint(
    AccessPointLocation location, {
    bool showHighlights = false,
  }) async {
    return await FirebaseGameServices.platform
        .showAccessPoint(location, showHighlights: showHighlights);
  }

  /// Hide the iOS Access Point.
  Future<String?> hideAccessPoint() async {
    return await FirebaseGameServices.platform.hideAccessPoint();
  }
}
