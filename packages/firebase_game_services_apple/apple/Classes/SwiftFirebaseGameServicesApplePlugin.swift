import GameKit
import os
import FirebaseAuth
import SwiftUI

#if os(iOS)
import Flutter
#else
import FlutterMacOS
#endif

public class SwiftFirebaseGameServicesApplePlugin: NSObject, FlutterPlugin {
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
            let arguments = call.arguments as? [String: Any]
            switch call.method {
                case "unlock":
                    let achievementID = (arguments?["achievementID"] as? String) ?? ""
                    let percentComplete = (arguments?["percentComplete"] as? Double) ?? 0.0
                    report(achievementID: achievementID, percentComplete: percentComplete, result: result)
                case "submitScore":
                    let leaderboardID = (arguments?["leaderboardID"] as? String) ?? ""
                    let score = (arguments?["value"] as? Int) ?? 0
                    report(score: Int64(score), leaderboardID: leaderboardID, result: result)
                case "showAchievements":
                    showAchievements()
                    result("success")
                case "showLeaderboards":
                    let leaderboardID = (arguments?["iOSLeaderboardID"] as? String) ?? ""
                    showLeaderboardWith(identifier: leaderboardID)
                    result("success")
                case "hideAccessPoint":
                    hideAccessPoint()
                case "showAccessPoint":
                    let location = (arguments?["location"] as? String) ?? ""
                    showAccessPoint(location: location)
                case "getPlayerID":
                    getGamePlayerID(result: result)
                case "getPlayerName":
                    getGamePlayerAlias(result: result)

                // TODO: Implement
                case "isUnderage":
                    isUnderage(result: result)
                case "isMultiplayerGamingRestricted":
                    isMultiplayerGamingRestricted(result: result)
                case "playerIsPersonalizedCommunicationRestricted":
                    isPersonalizedCommunicationRestricted(result: result)
                // END TODO
                
                case "signIn":
                    authenticateUser() { cred, error in
                        if let error = error {
                            result(error)
                        }
                        result(true)
                    }
                case "signInLinkedUser":
                    var forceSignInIfCredentialAlreadyUsed = false
                
                    let args = call.arguments as? Dictionary<String, Any>
                    
                    if(args != nil) {
                        forceSignInIfCredentialAlreadyUsed = (args!["force_sign_in_credential_already_used"] as? Bool) ?? false
                    }
                    
                    SignInLinkedUser(forceSignInIfCredentialAlreadyUsed: forceSignInIfCredentialAlreadyUsed) { cred, error in
                        if let error = error {
                            result(error)
                        }
                        result(true)
                    }

                default:
                    self.log(message: "Unknown method called")
                    result("unimplemented")
                break
            }
        }

    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "firebase_game_services", binaryMessenger: registrar.messenger())
        let instance = SwiftFirebaseGameServicesApplePlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
    }
            
    private func log(message: StaticString) {
        if #available(iOS 10.0, *) {
            os_log(message)
        }
    }

    var viewController : UIViewController? {
        return UIApplication.shared.keyWindow?.rootViewController
    }

    // Mark - Authentication
    
    private func getCredentialsAndSignIn(result: @escaping (Bool, FlutterError?) -> Void) {
        GameCenterAuthProvider.getCredential { cred, error in
            
            if let error = error {
                result(false, FlutterError.init(code: "get_gamecenter_credentials_failed", message: "Failed to get GameCenter credentials", details:error.localizedDescription))
                return
            }
            
            if(cred == nil) {
                result(false, FlutterError.init(code: "gamecenter_credentials_null", message: "Failed to get GameCenter credentials", details: "Credential are null"))
                return
            }

            Auth.auth().signIn(with:cred!) { (user, error) in
                if let error = error {
                    result(false, FlutterError.init(code: "gamecenter_signin_failed", message: "Failed to sign in with GameCenter", details:error.localizedDescription))
                    return
                } 
                result(true, nil);
                return
            }
        }
    }
    
    private func getCredentialsAndLink(user: User, forceSignInIfCredentialAlreadyUsed: Bool, result: @escaping (Bool, FlutterError?) -> Void) {
        GameCenterAuthProvider.getCredential { cred, error in
            if let error = error {
                result(false, FlutterError.init(code: "get_gamecenter_credentials_failed", message: "Failed to get GameCenter credentials", details:error.localizedDescription))
                return
            }
            
            if(cred == nil) {
                result(false, FlutterError.init(code: "gamecenter_credentials_null", message: "Failed to get GameCenter credentials", details: "Credential are null"))
                return
            }
            
            user.link(with: cred!) { (authResult, error) in
                if let error = error {
                    let err = error as NSError
                    
                    if (err.code == AuthErrorCode.credentialAlreadyInUse.rawValue && forceSignInIfCredentialAlreadyUsed) {
                            try? Auth.auth().signOut();

                            Auth.auth().signIn(with:cred!) { (user, error) in
                                if let error = error {
                                    result(false, FlutterError.init(code: "gamecenter_signin_failed", message: "Failed to sign in with GameCenter", details:error.localizedDescription))
                                    return
                                } 
                                result(true, nil);
                                return
                            }
                    } else {
                            result(false, FlutterError.init(code: "gamecenter_signin_failed", message: "Failed to sign in with GameCenter", details:error.localizedDescription))
                            return
                    }
                } else {
                    result(true, nil);
                    return
                }
            }
        }
    }
    
    private func authenticateUser(result: @escaping (AuthenticationResult) -> Void) {
        guard let player = GKLocalPlayer.local else {
            result(.failure(FlutterError(code: "no_player_detected", message: "No player detected on this phone", details: nil)))
            return
        }
        
        if player.isAuthenticated {
            getCredentialsAndSignIn(result: result)
        } else {
            player.authenticateHandler = { [weak self] vc, error in
                guard let self = self else { return }
                
                if let error = error {
                    result(.failure(FlutterError(code: "authentication_failed", message: error.localizedDescription, details: nil)))
                } else if player.isAuthenticated {
                    self.getCredentialsAndSignIn(result: result)
                } else if let vc = vc {
                    #if os(iOS)
                    self.viewController?.present(vc, animated: true, completion: nil)
                    #else
                    self.viewController.presentAsSheet(vc)
                    #endif
                } else {
                    result(.failure(FlutterError(code: "no_player_detected", message: "No player detected on this phone", details: nil)))
                }
            }
        }
    }
    
    private func SignInLinkedUser(forceSignInIfCredentialAlreadyUsed: Bool, result: @escaping (Bool, FlutterError?) -> Void) {
        let player = GKLocalPlayer.local
        
        let user: User? = Auth.auth().currentUser

        if(user == nil) {
            result(false, FlutterError.init(code: "no_user_sign_in", message: "No User sign in to Firebase, impossible to link any credentials", details:nil))
            return
        }

        for provider in user!.providerData {
            if(provider.providerID == "gc.apple.com") {
                print("User already link to Game Center")
                result(true, nil)
                return
            }
            
        }
        if(player.isAuthenticated) {
            self.getCredentialsAndLink(user: user!, forceSignInIfCredentialAlreadyUsed: forceSignInIfCredentialAlreadyUsed, result: result)
        } else {
            player.authenticateHandler = { vc, error in
                
                if let vc = vc {
                    #if os(iOS)
                    self.viewController?.present(vc, animated: true, completion: nil)
                    #else
                    self.viewController.presentAsSheet(vc)
                    #endif
                } else if player.isAuthenticated {
                    self.getCredentialsAndLink(user: user!, forceSignInIfCredentialAlreadyUsed: forceSignInIfCredentialAlreadyUsed, result: result)
                } else {
                    result(false, FlutterError.init(code: "no_player_detected", message: "No player detected on this phone", details:nil))
                    return
                }
            }
        }
    }
    
    
    // MARK: - Leaderboard

    private func showLeaderboardWith(identifier: String, result: @escaping FlutterResult) {
        let vc = GKGameCenterViewController()
        vc.gameCenterDelegate = self
        vc.viewState = .leaderboards
        vc.leaderboardIdentifier = identifier

        #if os(iOS)
        self.viewController?.present(vc, animated: true, completion: nil)
        #else
        self.viewController.presentAsSheet(vc)
        #endif
    }

    private func report(score: Int64, leaderboardID: String, result: @escaping FlutterResult) {
        let reportedScore = GKScore(leaderboardIdentifier: leaderboardID)
        reportedScore.value = score
        GKScore.report([reportedScore]) { (error) in
        guard error == nil else {
            result(error?.localizedDescription ?? "")
            return
        }
        result("success")
        }
    }

    // MARK: - Achievements

    private func showAchievements() {
        let vc = GKGameCenterViewController()
        vc.gameCenterDelegate = self
        vc.viewState = .achievements
        #if os(iOS)
        self.viewController?.present(vc, animated: true, completion: nil)
        #else
        self.viewController.presentAsSheet(vc)
        #endif
    }

    private func report(achievementID: String, percentComplete: Float, result: @escaping FlutterResult) {
        let achievement = GKAchievement(identifier: achievementID)
        achievement.percentComplete = percentComplete
        achievement.showsCompletionBanner = true
        GKAchievement.report([achievement]) { (error) in
            if let error = error {
                let errorMessage = "Failed to report achievement: \(achievementID)"
                DispatchQueue.main.async {
                    result(errorMessage)
                }
            } else {
                DispatchQueue.main.async {
                    result("success")
                }
            }
        }
    }

    
    // MARK: - AccessPoint

    private func showAccessPoint(location: GKAccessPointLocation) throws {
        guard #available(iOS 14.0, *) else {
            throw GameCenterError.unsupportedVersion
        }
        GKAccessPoint.shared.location = location.gkLocation
        GKAccessPoint.shared.isActive = true
    }
  
    private func hideAccessPoint() throws {
        if #available(iOS 14.0, *) {
            GKAccessPoint.shared.isActive = false
        } else {
            throw GameCenterError.unsupportedVersion
        }
    }

    // MARK: - Game Player

    private func getGamePlayerID(result: @escaping FlutterResult) {
        if #available(iOS 12.4, *) {
        guard let gamePlayerID = GKLocalPlayer.local.gamePlayerID else {
            result(FlutterError(code: "player_not_found", message: "Could not find game player ID.", details: nil))
            return
        }
        result(gamePlayerID)
        } else {
        result(FlutterError(code: "unsupported_version", message: "Game Center not supported on this version of iOS.", details: nil))
        }
    }

    private func getGamePlayerAlias(result: @escaping FlutterResult) {
        if #available(iOS 12.4, *) {
        guard let gamePlayerAlias = GKLocalPlayer.local.alias else {
            result(FlutterError(code: "player_not_found", message: "Could not find game player alias.", details: nil))
            return
        }
        result(gamePlayerAlias)
        } else {
        result(FlutterError(code: "unsupported_version", message: "Game Center not supported on this version of iOS.", details: nil))
        }
    }

    private func isUnderage(result: @escaping FlutterResult) {
        result(currentPlayer.isUnderage)
    }
    
    private func isMultiplayerGamingRestricted(result: @escaping FlutterResult) {
        if #available(iOS 13.0, *) {
        result(currentPlayer.isMultiplayerGamingRestricted)
        } else {
        let errorMessage = "The isMultiplayerGamingRestricted property is not supported on this version of iOS."
        let flutterError = FlutterError(code: "not_supported", message: errorMessage, details: nil)
        result(flutterError)
        }
    }

    func isPersonalizedCommunicationRestricted(result: @escaping FlutterResult) {
        if #available(iOS 14.0, *) {
        result(currentPlayer.isPersonalizedCommunicationRestricted)
        } else {
        result(PluginError.notSupportedForThisOSVersion.flutterError())
        }
    }


    // MARK: - Enums

    private enum AuthenticationResult {
        case success
        case failure(FlutterError)
    }

    private enum GameCenterError: Error {
        case unsupportedVersion
    }

    @available(iOS 14.0, *)
    private enum GKAccessPointLocation {
    case topLeading
    case topTrailing
    case bottomLeading
    case bottomTrailing
    
        var gkLocation: GKAccessPoint.Location {
            switch self {
            case .topLeading:
                return .topLeading
            case .topTrailing:
                return .topTrailing
            case .bottomLeading:
                return .bottomLeading
            case .bottomTrailing:
                return .bottomTrailing
            }
        }
    }


}

// MARK: - GKGameCenterControllerDelegate

extension SwiftFirebaseGameServicesApplePlugin: GKGameCenterControllerDelegate {
  public func gameCenterViewControllerDidFinish(_ gameCenterViewController: GKGameCenterViewController) {
    #if os(iOS)
      self.viewController?.dismiss(animated: true, completion: nil)
    #else
      self.viewController.dismiss(true)
    #endif
  }
}