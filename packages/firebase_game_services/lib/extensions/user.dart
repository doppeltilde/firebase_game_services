import 'package:firebase_game_services/firebase_game_services.dart';

extension FirebaseGameServicesUser on FirebaseGameServices {
  /// Get the player id.
  /// On iOS the player id is unique for your game but not other games.
  Future<String?> getPlayerId() async {
    return await FirebaseGameServices.platform.getPlayerID();
  }

  /// Get the player name.
  /// On iOS the player alias is the name used by the Player visible in the leaderboard
  Future<String?> getPlayerName() async {
    return await FirebaseGameServices.platform.getPlayerName();
  }

  Future<bool?> isUnderage() async {
    return await FirebaseGameServices.platform.isUnderage();
  }

  Future<bool?> isMultiplayerGamingRestricted() async {
    return await FirebaseGameServices.platform.isMultiplayerGamingRestricted();
  }

  Future<bool?> isPersonalizedCommunicationRestricted() async {
    return await FirebaseGameServices.platform
        .isPersonalizedCommunicationRestricted();
  }
}
