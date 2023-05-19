import 'dart:async';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:firebase_game_services_platform_interface/firebase_game_services_exception.dart';
import 'package:firebase_game_services_platform_interface/firebase_game_services_platform_interface.dart';
import 'package:flutter/services.dart';
import 'helpers.dart';

const MethodChannel _channel = MethodChannel("firebase_game_services");

class MethodChannelFirebaseGameServices extends FirebaseGameServicesPlatform {
  @override
  Future<String?> unlock({achievement = Achievement}) async {
    return await _channel.invokeMethod("unlock", {
      "achievementID": achievement.id,
      "percentComplete": achievement.percentComplete,
    });
  }

  @override
  Future<String?> submitScore({score = Score}) async {
    return await _channel.invokeMethod("submitScore", {
      "leaderboardID": score.leaderboardID,
      "value": score.value,
    });
  }

  @override
  Future<String?> increment({achievement = Achievement}) async {
    return await _channel.invokeMethod("increment", {
      "achievementID": achievement.id,
      "steps": achievement.steps,
    });
  }

  @override
  Future<String?> showAchievements() async {
    return await _channel.invokeMethod("showAchievements");
  }

  @override
  Future<String?> showLeaderboards(
      {iOSLeaderboardID = "", androidLeaderboardID = ""}) async {
    return await _channel.invokeMethod("showLeaderboards", {
      "leaderboardID":
          Helpers.isPlatformAndroid ? androidLeaderboardID : iOSLeaderboardID
    });
  }

  @override
  Future<String?> showAccessPoint(
    AccessPointLocation location, {
    bool? showHighlights,
  }) async {
    return await _channel.invokeMethod("showAccessPoint", {
      "location": location.toString().split(".").last,
      "showHighlights": showHighlights,
    });
  }

  @override
  Future<String?> hideAccessPoint() async {
    return await _channel.invokeMethod("hideAccessPoint");
  }

  @override
  Future<bool> signIn({String? clientId}) async {
    try {
      final dynamic result =
          await _channel.invokeMethod('signIn', {'client_id': clientId});

      if (result is bool) {
        return result;
      } else {
        return false;
      }
    } on PlatformException catch (error) {
      String code = 'unknown';

      switch (error.code) {
        case 'ERROR_CREDENTIAL_ALREADY_IN_USE':
          code = 'credential_already_in_use';
          break;
        case 'get_gamecenter_credentials_failed':
        case 'no_player_detected':
        case '12501':
          code = 'game_service_badly_configured_user_side';
          break;
      }
      throw FirebaseGameServicesException(
          code: code, message: error.message, stackTrace: error.stacktrace);
    } catch (error) {
      throw FirebaseGameServicesException(message: error.toString());
    }
  }

  @override
  Future<bool> signInLinkedUser(
      {String? clientId,
      bool forceSignInIfCredentialAlreadyUsed = false}) async {
    try {
      final dynamic result = await _channel.invokeMethod('signInLinkedUser', {
        'client_id': clientId,
        'force_sign_in_credential_already_used':
            forceSignInIfCredentialAlreadyUsed,
      });

      if (result is bool) {
        return result;
      } else {
        return false;
      }
    } on PlatformException catch (error) {
      String code = 'unknown';

      switch (error.code) {
        case 'ERROR_CREDENTIAL_ALREADY_IN_USE':
          code = 'credential_already_in_use';
          break;
        case 'get_gamecenter_credentials_failed':
        case 'no_player_detected':
        case '12501':
          code = 'game_service_badly_configured_user_side';
          break;
      }
      throw FirebaseGameServicesException(
          code: code, message: error.message, stackTrace: error.stacktrace);
    } catch (error) {
      throw FirebaseGameServicesException(message: error.toString());
    }
  }

  @override
  bool isUserLinkedToGameService() {
    final user = FirebaseAuth.instance.currentUser;

    if (user == null) {
      throw Exception('Firebase user is null');
    }

    final isLinked = user.providerData
        .map((userInfo) => userInfo.providerId)
        .contains(Helpers.isPlatformAndroid
            ? 'playgames.google.com'
            : 'gc.apple.com');

    return isLinked;
  }

  @override
  Future<String?> getPlayerID() async {
    return await _channel.invokeMethod("getPlayerID");
  }

  @override
  Future<String?> getPlayerName() async {
    return await _channel.invokeMethod("getPlayerName");
  }

  /// Check if player is underage (always false on Android).
  @override
  Future<bool?> isUnderage() async {
    return await _channel.invokeMethod("isUnderage");
  }

  /// Check if player is restricted from joining multiplayer games (always false on Android).
  @override
  Future<bool?> isMultiplayerGamingRestricted() async {
    return await _channel.invokeMethod("isMultiplayerGamingRestricted");
  }

  /// Check if player is restricted from using personalized communication on
  /// the device (always false on Android).
  @override
  Future<bool?> isPersonalizedCommunicationRestricted() async {
    return await _channel.invokeMethod("isPersonalizedCommunicationRestricted");
  }
}
