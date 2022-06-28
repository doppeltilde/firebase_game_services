#import "FirebaseGameServicesApplePlugin.h"
#if __has_include(<firebase_game_services_apple/firebase_game_services_apple-Swift.h>)
#import <firebase_game_services_apple/firebase_game_services_apple-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "firebase_game_services_apple-Swift.h"
#endif

@implementation FirebaseGameServicesApplePlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftFirebaseGameServicesApplePlugin registerWithRegistrar:registrar];
}
@end
