library firebase_game_services;

import 'package:firebase_game_services_platform_interface/firebase_game_services_platform_interface.dart';

export 'package:firebase_game_services/extensions/achievements.dart';
export 'package:firebase_game_services/extensions/auth.dart';
export 'package:firebase_game_services/extensions/user.dart';
export 'package:firebase_game_services_platform_interface/models/achievement.dart';
export 'package:firebase_game_services_platform_interface/models/access_point.dart';
export 'package:firebase_game_services_platform_interface/models/score.dart';

class FirebaseGameServices {
  static final FirebaseGameServices instance = FirebaseGameServices._();
  FirebaseGameServices._();

  static final platform = FirebaseGameServicesPlatform.instance;
}
