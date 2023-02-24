import 'package:firebase_game_services/firebase_game_services.dart';

extension FirebaseGameServicesAuth on FirebaseGameServices {
  /// Try to sign in with native Game Service (Play Games on Android and GameCenter on iOS)
  /// Return `true` if success
  /// [clientId] is only for Android if you want to provide a clientId other than the main one in you google-services.json
  Future<bool> signIn({String? clientId}) async {
    return await FirebaseGameServices.platform.signIn(clientId: clientId);
  }

  /// Try to sign link current user with native Game Service (Play Games on Android and GameCenter on iOS)
  /// Return `true` if success
  /// [clientId] is only for Android if you want to provide a clientId other than the main one in you google-services.json
  /// [forceSignInIfCredentialAlreadyUsed] make user force sign in with game services link failed because of ERROR_CREDENTIAL_ALREADY_IN_USE
  Future<bool> signInLinkedUser(
      {String? clientId,
      bool forceSignInIfCredentialAlreadyUsed = false}) async {
    return await FirebaseGameServices.platform.signInLinkedUser(
        clientId: clientId,
        forceSignInIfCredentialAlreadyUsed: forceSignInIfCredentialAlreadyUsed);
  }

  /// Test if a user is already linked to a game service
  /// Advised to be call before linkGameServicesCredentialsToCurrentUser()
  bool isUserLinkedToGameService() {
    return FirebaseGameServices.platform.isUserLinkedToGameService();
  }
}
