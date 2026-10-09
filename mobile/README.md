# DEWECS Report (mobile app)

Flutter app for citizens, the fifth use case of DEWECS (SE3070 Group 18): report a hazard (flood, landslide, cyclone,
drought) from a phone and follow what the officers do with it on the web dashboard.

It is built for poor connectivity, which is the real situation in a disaster: a report is **saved on the phone first**
and sent when a connection exists. Sending is safe to retry, so a lost response can never create a duplicate.

## What it does

- **Identify once:** NIC, full name, phone number and district. The NIC is sent to the server but never stored on the phone.
- **Report a hazard:** category, description, district, location (GPS, or type latitude and longitude) and an optional photo.
- **Offline first:** Submit stores the report in a queue on the phone and tries to send it. Anything that fails for a
  temporary reason stays queued and is retried; a report the server refuses is shown with the reason so it can be fixed.
- **My reports:** waiting reports first, then the server list with status chips (Waiting for review, Reviewed by
  officers, Action taken, Not accepted, More information needed), the officers' note once action was taken, photo and
  an Open in maps button. The last list is cached, so it still shows offline.
- **Demo mode:** a built-in fake server, so the app can be shown without a backend (Settings).

## Architecture

Provider with `ChangeNotifier`, small files, no code generation, no map or Firebase keys. The app talks to the Spring
backend over HTTP only (contract v1, see `../doc/MOBILE_API.md`); it never contains database credentials.

```
lib/
  config/     theme and default server address
  models/     plain Dart classes with fromJson/toJson; contract rules; Sri Lanka time helper (pure Dart)
  api/        DewecsApi (one method per endpoint), HttpDewecsApi, FakeDewecsApi, ApiException (pure Dart)
  storage/    key-value store (shared_preferences), queue repository, photo store
  sync/       SyncService (the queue engine), back-off, connectivity trigger
  state/      controllers (settings, identity, reference data, reports), device services (GPS, camera, maps)
  screens/    home shell, My reports, report form, detail, identify, settings, sync queue (debug)
  widgets/    status chip, report card, category chips, location and photo sections
  strings.dart  every user-visible string (English only)
tool/smoke.dart   pure-Dart check of the real HTTP layer against a running backend
```

## Setup

```powershell
flutter doctor            # Android toolchain must be green to run on an emulator or phone
cd mobile
flutter pub get
flutter analyze           # no issues
flutter test              # 152 tests
```

Flutter, the Android SDK and the emulator images are large. If you keep them in one folder so they are easy to delete
afterwards, point the caches there for your shell (example for this repo's layout):

```powershell
$dt = "C:\Users\<you>\Downloads\Dilmith\projects\DevTools"
$env:Path = "$dt\flutter\bin;$env:Path"; $env:PUB_CACHE = "$dt\pub-cache"; $env:GRADLE_USER_HOME = "$dt\gradle-home"
```

## Run it against the backend

1. Start the backend with the local profile (in-memory H2, demo data, no database credentials):
   `cd backend` then `java -jar target\dewecs-0.0.1-SNAPSHOT.war --spring.profiles.active=local`
   (build it first with `mvn -B -DskipTests package`).
2. Run the app, then open Settings and check the server address. Use **Test connection** to see the district count.

| Where the app runs | Server address |
|---|---|
| The deployed backend (AWS) | `http://13.201.118.235:8080`, the default of a fresh install (see below) |
| Android emulator, local backend | `http://10.0.2.2:8080`: type it in Settings, or run with `--dart-define=DEWECS_BASE_URL=http://10.0.2.2:8080` |
| Physical phone on the same Wi-Fi | `http://<laptop LAN IP>:8080` (find it with `ipconfig`) and allow Java through the Windows firewall for port 8080 |
| Physical phone over USB | run `adb reverse tcp:8080 tcp:8080`, then use `http://localhost:8080` |

Officers see the reports and photos at http://localhost:8080/ground-reports (local) or http://13.201.118.235:8080/ground-reports (deployed).

**The deployed server is plain http.** The default address is `deployedBaseUrl` in `lib/config/app_config.dart`. Release builds refuse http except for that one IP: `android/app/src/main/res/xml/network_security_config.xml` lists it, and debug builds allow http everywhere (`src/debug/res/xml`). Settings shows an "not encrypted" warning for it. When the server gets https, change `deployedBaseUrl` and delete the `domain-config` block. An installed app keeps the address it saved before: change it in Settings or clear the app data.

## Demo mode

Settings, Demo mode. The app then uses a fake server in memory: four districts, seeded reports in different statuses
(the first citizen to identify owns them, including one with an officers' note), 400 ms latency, replay detection and
the same rules as the real contract. Two switches force failures so offline behaviour can be shown on stage:
"simulate no connection" and "simulate a server error (500)"; "wipe the fake server" imitates a server reset.
Switching Demo mode in either direction asks first and clears the stored identity, queue and caches, because fake ids
must never reach the real backend.

## How offline queueing and retries work

1. **Submit** validates the form, sets `capturedAt` once (Sri Lanka time, UTC+05:30) and stores a queued report on the
   phone at once. It then tries to send, waiting at most 3 seconds, and tells the user "Report sent." or "Saved on this
   phone."
2. **Sync** sends the oldest report first, one run at a time. A report is stored when the server answers 200 or 201;
   then the photo is uploaded; then the item is synced. A photo failure never undoes the report.
3. **Retryable errors** (network, timeout, 5xx, 408, 429) keep the report queued with a back-off of 5 s, 15 s, 60 s,
   5 min, then every 5 min. After a 5xx the run continues with the next report; only a dead connection stops the run.
4. **Permanent errors** (400, 405, 413, 415 and other 4xx) move the report to "Needs attention" with the server's reason.
   The user can edit and send again (a fresh `capturedAt`, safe because nothing was stored) or delete it.
5. **Idempotent:** the same `capturedAt` goes out on every retry. If a response is lost, the retry gets 200 and the
   stored report instead of creating a duplicate. The server does this without any database change.
6. **A 404 on submit** means either the server no longer knows the citizen (its database was reset) or the district
   is gone. The app asks the server for the district list to decide: unknown citizen clears the stored identity and
   opens Identify (the queue is kept and re-pointed to the new citizen); a missing district needs attention.
7. **When it runs:** after Submit, on app start, when the connection returns, when the app returns to the foreground,
   on pull-to-refresh, on Send now, and by an in-app timer for the next due retry.

## Tests

```powershell
cd mobile
flutter test                                  # models, API layer (MockClient), fake server, validators,
                                              # queue state machine, sync engine, controllers, widgets
dart run tool/smoke.dart http://localhost:8092   # real backend (start it on port 8092 with the local profile)
```

## Cleartext HTTP (important for demos)

The dev backend speaks plain `http`. Android blocks that by default, so `usesCleartextTraffic="true"` is set **only in
`android/app/src/debug/AndroidManifest.xml`**. A release build (`flutter build apk`) will refuse `http://` addresses.
For a release APK demo you can either serve the backend over HTTPS, or add `android:usesCleartextTraffic="true"` to
the `<application>` element of `android/app/src/main/AndroidManifest.xml` (a trade-off: it lets the app talk to any
unencrypted server, so remove it again for anything public).

## Limitations

- **No background sync:** reports are only sent while the app is open or when it returns to the foreground. Nothing is
  sent while the app is closed (no WorkManager).
- **No authentication:** anyone who knows a NIC can report as that citizen. After a server reset a stored citizen id may
  point to a different person; the app detects the 404 and asks to identify again, but cannot tell in other cases.
- No map tiles (Open in maps hands over to another app or the browser), English only, no push notifications.
- Photos are copied into the app folder and sent as JPEG, PNG or WebP up to 5 MB (the picker keeps them far below).
- **Check the phone's clock:** the server refuses a capture time more than 5 minutes ahead of Sri Lanka time. A phone
  whose clock or time zone is wrong gets a permanent 400 that Edit and send again cannot fix; the screen shows the
  server's reason ("The capture time is in the future.").
- Only Android was created (`flutter create --platforms android`). Web would need the backend's CORS property and
  would lose queued photos on a reload.
