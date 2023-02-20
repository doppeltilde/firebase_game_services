import 'package:firebase_game_services/firebase_game_services.dart';
import 'package:firebase_game_services_platform_interface/firebase_game_services_platform_interface.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('FirebaseGameServices', () {
    test('FirebaseGameServices.instance is not null', () {
      expect(FirebaseGameServices.instance, isNotNull);
    });

    test('FirebaseGameServices instance is a singleton', () {
      expect(
          FirebaseGameServices.instance, equals(FirebaseGameServices.instance));
    });

    test('FirebaseGameServices platform is not null', () {
      expect(FirebaseGameServices.platform, isNotNull);
    });
  });

  group('FirebaseGameServicesUser', () {
    test('getPlayerID returns a string', () async {
      final playerId = await FirebaseGameServices.instance.getPlayerId();
      expect(playerId, isA<String>());
    });

    test('getPlayerName returns a string', () async {
      final playerName = await FirebaseGameServices.instance.getPlayerName();
      expect(playerName, isA<String>());
    });
  });

  group('FirebaseGameServicesAchievements', () {
    test('unlock returns a string', () async {
      final achievement = Achievement(
          androidID: 'android_achievement_id', iOSID: 'ios_achievement_id');
      final result =
          await FirebaseGameServices.instance.unlock(achievement: achievement);
      expect(result, isA<String>());
    });

    test('increment returns a string', () async {
      final achievement = Achievement(androidID: 'android_achievement_id');
      final result = await FirebaseGameServices.instance
          .increment(achievement: achievement);
      expect(result, isA<String>());
    });

    test('submitScore returns a string', () async {
      final score =
          Score(androidLeaderboardID: 'android_leaderboard_id', value: 100);
      final result =
          await FirebaseGameServices.instance.submitScore(score: score);
      expect(result, isA<String>());
    });

    test('showAchievements returns a string', () async {
      final result = await FirebaseGameServices.instance.showAchievements();
      expect(result, isA<String>());
    });

    test('showLeaderboards returns a string', () async {
      final result = await FirebaseGameServices.instance.showLeaderboards(
        androidLeaderboardID: 'android_leaderboard_id',
        iOSLeaderboardID: 'ios_leaderboard_id',
      );
      expect(result, isA<String>());
    });

    test('showAccessPoint returns a string', () async {
      final result = await FirebaseGameServices.instance
          .showAccessPoint(AccessPointLocation.topLeading);
      expect(result, isA<String>());
    });

    test('hideAccessPoint returns a string', () async {
      final result = await FirebaseGameServices.instance.hideAccessPoint();
      expect(result, isA<String>());
    });
  });
}
