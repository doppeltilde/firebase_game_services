import 'package:firebase_game_services/firebase_game_services.dart';
import 'package:flutter/material.dart';

getSignIn() async {
  try {
    await FirebaseGameServices.instance.signIn();
  } on Exception {
    try {
      await FirebaseGameServices.instance.signInLinkedUser();
    } on Exception catch (e) {
      print(e);
    }
  }
}

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  getSignIn();
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Example'),
        ),
        body: Center(
          child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                ElevatedButton(
                    onPressed: () async {
                      await FirebaseGameServices.instance.showAchievements();
                    },
                    child: const Text("Achievements")),
                ElevatedButton(
                    onPressed: () async {
                      await FirebaseGameServices.instance.showLeaderboards();
                    },
                    child: const Text("Leaderboards")),
                ElevatedButton(
                    onPressed: () async {
                      var id =
                          await FirebaseGameServices.instance.getPlayerId();
                      print(id);
                    },
                    child: const Text("Player Id")),
                ElevatedButton(
                    onPressed: () async {
                      var name =
                          await FirebaseGameServices.instance.getPlayerName();
                      print(name);
                    },
                    child: const Text("Player Name")),
                ElevatedButton(
                    onPressed: () async {
                      await FirebaseGameServices.instance.submitScore(
                          score: Score(
                        androidLeaderboardID: 'android_leaderboard_id',
                        iOSLeaderboardID: 'ios_leaderboard_id',
                        value: 5,
                      ));
                    },
                    child: const Text("Submit Score")),
                ElevatedButton(
                    onPressed: () async {
                      await FirebaseGameServices.instance.unlock(
                        achievement: Achievement(
                            androidID: 'android_id',
                            iOSID: 'ios_id',
                            percentComplete: 100,
                            steps: 2),
                      );
                    },
                    child: const Text("Unlock Achievement")),
                ElevatedButton(
                    onPressed: () async {
                      await FirebaseGameServices.instance.showAccessPoint(
                        AccessPointLocation.topLeading,
                        showHighlights: true,
                      );
                    },
                    child: const Text("Show Accesspoint")),
                ElevatedButton(
                    onPressed: () async {
                      var isUnderage =
                          await FirebaseGameServices.instance.isUnderage();
                      print(isUnderage);
                    },
                    child: const Text("isUnderage")),
                ElevatedButton(
                    onPressed: () async {
                      var isMultiplayerGamingRestricted =
                          await FirebaseGameServices.instance
                              .isMultiplayerGamingRestricted();
                      print(isMultiplayerGamingRestricted);
                    },
                    child: const Text("isMultiplayerGamingRestricted")),
                ElevatedButton(
                    onPressed: () async {
                      var isPersonalizedCommunicationRestricted =
                          await FirebaseGameServices.instance
                              .isPersonalizedCommunicationRestricted();
                      print(isPersonalizedCommunicationRestricted);
                    },
                    child: const Text("isPersonalizedCommunicationRestricted")),
              ]),
        ),
      ),
    );
  }
}
