import 'package:firebase_game_services/firebase_game_services.dart';
import 'package:firebase_game_services_platform_interface/firebase_game_services_platform_interface.dart';

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

  /// It will open the leaderboards screen.
  Future<String?> showLeaderboards(
      {iOSLeaderboardID = "", androidLeaderboardID = ""}) async {
    return await FirebaseGameServices.platform.showLeaderboards(
        iOSLeaderboardID: iOSLeaderboardID,
        androidLeaderboardID: androidLeaderboardID);
  }

  /// Show the iOS Access Point.
  Future<String?> showAccessPoint(AccessPointLocation location) async {
    return await FirebaseGameServices.platform.showAccessPoint(location);
  }

  /// Hide the iOS Access Point.
  Future<String?> hideAccessPoint() async {
    return await FirebaseGameServices.platform.hideAccessPoint();
  }
}
