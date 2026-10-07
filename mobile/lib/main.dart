import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'config/theme.dart';
import 'screens/home_shell.dart';
import 'state/app_dependencies.dart';
import 'storage/key_value_store.dart';
import 'strings.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final prefs = await SharedPreferences.getInstance();
  final dependencies = AppDependencies.create(SharedPreferencesStore(prefs))..start();
  runApp(DewecsApp(dependencies: dependencies));
}

class DewecsApp extends StatelessWidget {
  const DewecsApp({super.key, required this.dependencies});

  final AppDependencies dependencies;

  @override
  Widget build(BuildContext context) {
    return dependencies.provide(
      child: MaterialApp(
        title: S.appName,
        theme: AppTheme.light(),
        darkTheme: AppTheme.dark(),
        themeMode: ThemeMode.system,
        home: const HomeShell(),
      ),
    );
  }
}
