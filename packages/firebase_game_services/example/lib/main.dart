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
              ]),
        ),
      ),
    );
  }
}
