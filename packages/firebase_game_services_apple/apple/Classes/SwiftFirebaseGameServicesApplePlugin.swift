import Flutter
import UIKit

public class SwiftFirebaseGameServicesApplePlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "firebase_game_services_apple", binaryMessenger: registrar.messenger())
    let instance = SwiftFirebaseGameServicesApplePlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    result("iOS " + UIDevice.current.systemVersion)
  }
}
